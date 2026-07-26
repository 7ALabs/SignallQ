package io.signallq.app.feature.diagnostico.remote

import io.signallq.app.core.diagnostico.DiagnosticArea
import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticReport
import io.signallq.app.core.diagnostico.DiagnosticRunner
import io.signallq.app.core.diagnostico.GameReadinessClassifier
import io.signallq.app.core.diagnostico.UsageProfileClassifier
import io.signallq.app.feature.diagnostico.RecommendationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Client/repository que conecta o app ao motor de diagnostico remoto do worker
 * `signallq-diagnostic` (`POST /api/diagnostic/evaluate`) — GH#962.
 *
 * ## Estrategia local vs. remoto (decisao desta issue, documentada)
 * 1. Tenta o worker remoto com timeout de conexao curto (connect 3s / read 4s /
 *    write 3s no OkHttpClient), com teto adicional de 42s via [withTimeoutOrNull]
 *    (decisao de produto — Luiz, 2026-07-16 — ampliado a partir dos 5s sugeridos
 *    na spec original da tela "1a · Analise detalhada" pra reduzir fallback
 *    prematuro para o motor local em rede lenta).
 * 2. Se o worker responder 2xx com JSON valido: mapeia via
 *    [RemoteDiagnosticReportMapper] e retorna. `perfisUso`/`gameReadiness` sao
 *    SEMPRE calculados localmente (puro, determinístico, nao precisa de rede —
 *    ver kdoc de [RemoteDiagnosticReportMapper]), nunca vem do worker.
 * 3. Em QUALQUER falha (sem rede, timeout, !2xx, corpo vazio, JSON invalido,
 *    excecao) -> fallback total para [DiagnosticRunner.run] (motor local
 *    100% offline, ja usado hoje por [io.signallq.app.feature.diagnostico.DiagnosticOrchestrator]).
 *    O usuario NUNCA fica esperando rede alem do teto de timeout acima —
 *    nunca trava a UI.
 *
 * ## Adocao pelo orquestrador principal (GH#969)
 * Wireada em [io.signallq.app.feature.diagnostico.DiagnosticOrchestrator.executar], que
 * agora e `suspend` e delega inteiramente a estrategia remoto-primeiro/fallback-local
 * definida aqui — o orquestrador nao duplica a decisao, so chama [evaluate]. Todos os
 * call sites de `executar` ja rodavam dentro de coroutine (`viewModelScope.launch` /
 * `Flow.collect` / `withContext`), entao a mudanca para `suspend` nao exigiu alterar
 * nenhum chamador.
 *
 * ## Fallback local de 3 niveis (GH#1450, secao "Fallback local" de #952)
 * [cacheStore] guarda o ultimo JSON valido devolvido pelo worker. Ordem de tentativa
 * em [evaluate]: `REMOTE` (worker responde e mapeia com sucesso) -> `CACHED_LOCAL`
 * (worker falhou, mas ha ruleset remoto cacheado e valido) -> `BUNDLED_LOCAL`
 * ([DiagnosticRunner], motor 100% embarcado, fallback final -- nunca falha). A origem
 * efetivamente usada fica em [DiagnosticReport.evaluationSource]. Sem [cacheStore]
 * configurado (default [NoOpRulesetCacheStore]), o comportamento degrada para o
 * fallback de 2 niveis anterior (REMOTE -> BUNDLED_LOCAL).
 */
class RemoteDiagnosticRepository(
    private val baseUrl: String,
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build(),
    private val cacheStore: RulesetCacheStore = NoOpRulesetCacheStore,
) {

    /**
     * Avalia o diagnostico com fallback local de 3 niveis: tenta remoto, depois o
     * ultimo ruleset remoto cacheado, depois o motor embarcado. Nunca lanca excecao —
     * sempre devolve um [DiagnosticReport] valido, com [DiagnosticReport.evaluationSource]
     * indicando qual dos tres realmente foi usado.
     */
    suspend fun evaluate(
        input: DiagnosticInput,
        enabledAreas: Set<DiagnosticArea> = DiagnosticArea.entries.toSet(),
    ): DiagnosticReport {
        val remotePayload = evaluateRemote(input)
        if (remotePayload != null) {
            try {
                val remoteReport = RemoteDiagnosticReportMapper.toDiagnosticReport(
                    payload = remotePayload,
                    geradoEmMs = System.currentTimeMillis(),
                )
                persistarCache(remotePayload)
                // perfisUso/gameReadiness: sempre local, ver kdoc de RemoteDiagnosticReportMapper.
                return remoteReport.copy(
                    evaluationSource = DiagnosticEvaluationSource.REMOTE,
                    perfisUso = UsageProfileClassifier.classificarTodos(input),
                    gameReadiness = GameReadinessClassifier.classificarTodos(input),
                )
            } catch (t: Throwable) {
                Timber.w(t, "RemoteDiagnosticRepository: falha ao mapear resposta remota, tentando cache local")
            }
        } else {
            Timber.i("RemoteDiagnosticRepository: remoto indisponivel, tentando cache local")
        }

        return evaluateFromCacheOrBundled(input, enabledAreas)
    }

    /** Nivel 2 (`CACHED_LOCAL`) e nivel 3 (`BUNDLED_LOCAL`) do fallback -- chamado
     *  quando o remoto falhou (indisponivel ou resposta nao mapeavel). */
    private suspend fun evaluateFromCacheOrBundled(
        input: DiagnosticInput,
        enabledAreas: Set<DiagnosticArea>,
    ): DiagnosticReport {
        val cached = try {
            cacheStore.load()
        } catch (t: Throwable) {
            Timber.w(t, "RemoteDiagnosticRepository: falha ao ler cache local")
            null
        }

        if (cached != null) {
            try {
                val cachedReport = RemoteDiagnosticReportMapper.toDiagnosticReport(
                    payload = JSONObject(cached.content),
                    geradoEmMs = System.currentTimeMillis(),
                )
                Timber.i("RemoteDiagnosticRepository: usando CACHED_LOCAL (sincronizado em ${cached.syncedAtMs})")
                return cachedReport.copy(
                    evaluationSource = DiagnosticEvaluationSource.CACHED_LOCAL,
                    perfisUso = UsageProfileClassifier.classificarTodos(input),
                    gameReadiness = GameReadinessClassifier.classificarTodos(input),
                )
            } catch (t: Throwable) {
                Timber.w(t, "RemoteDiagnosticRepository: cache local corrompido, usando motor embarcado")
            }
        }

        Timber.i("RemoteDiagnosticRepository: usando motor embarcado (BUNDLED_LOCAL)")
        return DiagnosticRunner.run(input, enabledAreas, gerarRecomendacoes = RecommendationEngine::recomendar)
            .copy(evaluationSource = DiagnosticEvaluationSource.BUNDLED_LOCAL)
    }

    /** Persiste o payload remoto recem-validado como novo `CACHED_LOCAL`. Melhor
     *  esforco: falha ao persistir nao derruba a avaliacao REMOTE que ja foi obtida
     *  com sucesso, so significa que o proximo fallback nao tera um cache mais novo. */
    private fun persistarCache(payload: JSONObject) {
        try {
            cacheStore.save(
                content = payload.toString(),
                rulesetVersion = if (payload.has("rulesetVersion") && !payload.isNull("rulesetVersion")) {
                    payload.optInt("rulesetVersion")
                } else {
                    null
                },
                syncedAtMs = System.currentTimeMillis(),
            )
        } catch (t: Throwable) {
            Timber.w(t, "RemoteDiagnosticRepository: falha ao persistir ruleset remoto para CACHED_LOCAL")
        }
    }

    /**
     * So a chamada remota (sem fallback), para uso interno/testes. Retorna
     * `null` em qualquer falha (sem rede, timeout, !2xx, corpo vazio/invalido).
     */
    internal suspend fun evaluateRemote(input: DiagnosticInput): JSONObject? {
        return withContext(Dispatchers.IO) {
            withTimeoutOrNull(TIMEOUT_TETO_MS) {
                try {
                    val url = baseUrl.trimEnd('/') + "/api/diagnostic/evaluate"
                    val json = DiagnosticSnapshotMapper.toJson(input).toString()
                    val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder().url(url).post(body).build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            Timber.w("RemoteDiagnosticRepository: worker HTTP ${response.code}")
                            return@use null
                        }
                        val text = response.body?.string()
                        if (text.isNullOrBlank()) {
                            Timber.w("RemoteDiagnosticRepository: worker retornou corpo vazio")
                            return@use null
                        }
                        try {
                            JSONObject(text)
                        } catch (t: Throwable) {
                            Timber.w(t, "RemoteDiagnosticRepository: JSON invalido do worker")
                            null
                        }
                    }
                } catch (t: Throwable) {
                    Timber.w(t, "RemoteDiagnosticRepository: falha de rede — ${t::class.simpleName}")
                    null
                }
            }
        }
    }

    private companion object {
        /** Teto total de espera pelo worker remoto antes do fallback local — ver kdoc da classe. */
        const val TIMEOUT_TETO_MS = 42_000L
    }
}
