package io.signallq.app.feature.diagnostico.remote

import io.signallq.app.core.diagnostico.DiagnosticArea
import io.signallq.app.core.diagnostico.DiagnosticDivergenceClassifier
import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticReport
import io.signallq.app.core.diagnostico.DiagnosticRunner
import io.signallq.app.core.diagnostico.GameReadinessClassifier
import io.signallq.app.core.diagnostico.UsageProfileClassifier
import io.signallq.app.feature.diagnostico.RecommendationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.security.MessageDigest
import java.util.UUID
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
 *
 * ## Shadow mode (GH#1444, parte de #952) — [evaluateShadow], NAO [evaluate]
 * [evaluate] (acima) e remoto-primeiro: quando o worker responde, o resultado
 * REMOTO e o que a UI mostra. Isso e o que #952 chama de "avaliacao remota
 * autoritativa" — so deveria acontecer DEPOIS de uma fase de shadow mode medir
 * equivalencia entre os dois motores (a paridade documentada em GH#1442 mostra
 * varias regras PARCIAL/PENDENTE, longe de validada). O orquestrador de
 * producao ([io.signallq.app.feature.diagnostico.DiagnosticOrchestrator]) usa
 * [evaluateShadow], nao [evaluate], exatamente por isso: motor LOCAL sempre
 * autoritativo, avaliacao remota roda so em paralelo, para comparacao. [evaluate]
 * continua existindo (testado, funcional) como a estrategia remoto-primeiro que
 * um rollout futuro controlado (#1445, apos meta de equivalencia validada com
 * dados reais deste shadow mode) pode voltar a acionar — não removido, so não é
 * mais o caminho de producao hoje.
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
    private val divergenceReporter: DiagnosticDivergenceReporter? = null,
    private val shadowScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
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
     * Shadow mode (GH#1444, parte de #952). Motor LOCAL roda primeiro e e o
     * UNICO resultado retornado — nunca espera a rede, nunca e sobrescrito
     * depois. Quando [divergenceReporter] esta configurado, uma avaliacao
     * remota roda em paralelo em [shadowScope] (fire-and-forget, nunca
     * aguardada por este metodo) so para comparar/reportar — falha nela nunca
     * propaga nem altera o retorno.
     */
    suspend fun evaluateShadow(
        input: DiagnosticInput,
        enabledAreas: Set<DiagnosticArea> = DiagnosticArea.entries.toSet(),
    ): DiagnosticReport {
        val startedAtMs = System.currentTimeMillis()
        val localReport = DiagnosticRunner.run(input, enabledAreas, gerarRecomendacoes = RecommendationEngine::recomendar)
        val localDurationMs = System.currentTimeMillis() - startedAtMs

        val reporter = divergenceReporter
        if (reporter != null) {
            shadowScope.launch {
                runCatching { runShadowComparison(input, localReport, localDurationMs, reporter) }
                    .onFailure { t -> Timber.w(t, "RemoteDiagnosticRepository: shadow comparison falhou (ignorando)") }
            }
        }

        return localReport
    }

    /** Avalia remoto, classifica contra [localReport] e reporta — tudo em
     *  best-effort, chamado so a partir de [shadowScope] (nunca no caminho
     *  principal de [evaluateShadow]). */
    private suspend fun runShadowComparison(
        input: DiagnosticInput,
        localReport: DiagnosticReport,
        localDurationMs: Long,
        reporter: DiagnosticDivergenceReporter,
    ) {
        if (!reporter.isEligibleForShadowComparison()) return

        val startedAtMs = System.currentTimeMillis()
        val remotePayload = evaluateRemote(input)
        val remoteDurationMs = System.currentTimeMillis() - startedAtMs

        val remoteReport = remotePayload?.let {
            try {
                // RemoteDiagnosticReportMapper nao preenche evaluationSource (fica no
                // default BUNDLED_LOCAL da data class) -- mesmo ajuste que evaluate()
                // ja faz apos mapear, necessario aqui tambem.
                RemoteDiagnosticReportMapper.toDiagnosticReport(payload = it, geradoEmMs = System.currentTimeMillis())
                    .copy(evaluationSource = DiagnosticEvaluationSource.REMOTE)
            } catch (t: Throwable) {
                Timber.w(t, "RemoteDiagnosticRepository: falha ao mapear resposta remota no shadow mode")
                null
            }
        }

        val comparison = DiagnosticDivergenceClassifier.classify(localReport, remoteReport)

        reporter.report(
            executionId = UUID.randomUUID().toString(),
            snapshotHash = sha256(DiagnosticSnapshotMapper.toJson(input).toString()),
            comparison = comparison,
            localSource = localReport.evaluationSource,
            remoteSource = remoteReport?.evaluationSource,
            localDurationMs = localDurationMs,
            remoteDurationMs = remoteDurationMs,
        )
    }

    private fun sha256(content: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(content.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

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
