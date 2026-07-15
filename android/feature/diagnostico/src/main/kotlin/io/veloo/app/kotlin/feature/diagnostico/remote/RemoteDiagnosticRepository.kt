package io.signallq.app.feature.diagnostico.remote

import io.signallq.app.feature.diagnostico.DiagnosticArea
import io.signallq.app.feature.diagnostico.DiagnosticInput
import io.signallq.app.feature.diagnostico.DiagnosticReport
import io.signallq.app.feature.diagnostico.DiagnosticRunner
import io.signallq.app.feature.diagnostico.GameReadinessClassifier
import io.signallq.app.feature.diagnostico.UsageProfileClassifier
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
 * 1. Tenta o worker remoto com timeout CURTO (connect 3s / read 4s / write 3s,
 *    com teto adicional de 5s via [withTimeoutOrNull] — mesmo padrao ja usado
 *    por [io.signallq.app.feature.diagnostico.ai.AiDiagnosisRepository]).
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
 */
class RemoteDiagnosticRepository(
    private val baseUrl: String,
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build(),
) {

    /**
     * Avalia o diagnostico: tenta remoto primeiro, cai para o motor local em
     * qualquer falha. Nunca lanca excecao — sempre devolve um [DiagnosticReport]
     * valido (remoto ou local).
     */
    suspend fun evaluate(
        input: DiagnosticInput,
        enabledAreas: Set<DiagnosticArea> = DiagnosticArea.entries.toSet(),
    ): DiagnosticReport {
        val remotePayload = evaluateRemote(input)
        if (remotePayload == null) {
            Timber.i("RemoteDiagnosticRepository: remoto indisponivel, usando motor local")
            return DiagnosticRunner.run(input, enabledAreas)
        }

        return try {
            val remoteReport = RemoteDiagnosticReportMapper.toDiagnosticReport(
                payload = remotePayload,
                geradoEmMs = System.currentTimeMillis(),
            )
            // perfisUso/gameReadiness: sempre local, ver kdoc de RemoteDiagnosticReportMapper.
            remoteReport.copy(
                perfisUso = UsageProfileClassifier.classificarTodos(input),
                gameReadiness = GameReadinessClassifier.classificarTodos(input),
            )
        } catch (t: Throwable) {
            Timber.w(t, "RemoteDiagnosticRepository: falha ao mapear resposta remota, usando motor local")
            DiagnosticRunner.run(input, enabledAreas)
        }
    }

    /**
     * So a chamada remota (sem fallback), para uso interno/testes. Retorna
     * `null` em qualquer falha (sem rede, timeout, !2xx, corpo vazio/invalido).
     */
    internal suspend fun evaluateRemote(input: DiagnosticInput): JSONObject? {
        return withContext(Dispatchers.IO) {
            withTimeoutOrNull(5_000L) {
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
}
