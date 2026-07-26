package io.signallq.app.feature.diagnostico.remote

import io.signallq.app.core.diagnostico.DiagnosticDivergenceClassifier
import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.network.FeatureFlagProvider
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Shadow mode (GH#1444, parte de #952) — envia o registro JA CLASSIFICADO
 * ([DiagnosticDivergenceClassifier.Comparison], calculado no cliente) para o
 * worker `signallq-diagnostic` (`POST /ingest/diagnostic-divergence`).
 *
 * ## Decisao: comparacao no cliente, nao no worker (corpo da issue, item 4)
 * O worker NUNCA recebe o [io.signallq.app.core.diagnostico.DiagnosticReport]
 * local completo nem o remoto — so o resumo ja comparado (status/score/origem
 * dos dois lados + a classificacao). Duas razoes: (1) o motor local roda em
 * Kotlin/JVM, o worker so enxergaria o snapshot de novo (nao o resultado local
 * de fato produzido pelo [io.signallq.app.core.diagnostico.DiagnosticRunner]) —
 * comparar no worker forcaria reimplementar a decisao local em TypeScript, uma
 * segunda fonte de verdade; (2) menos superficie nova de payload trafegando —
 * o relatorio completo (achados/recomendacoes/textos) nunca sai do aparelho so
 * para fins de telemetria de shadow mode.
 *
 * Fire-and-forget, mesmo padrao de [io.signallq.app.feature.diagnostico.ingest.AdminIngestRepository]:
 * nunca lanca excecao, nunca bloqueia o chamador, qualquer falha e so logada.
 *
 * Kill switch: [FeatureFlagProvider] com a chave `feature_diagnostic_shadow_mode`
 * (mecanismo remoto ja existente — `signallq-admin-worker` `GET /flags` — reusado
 * em vez de duplicar; ver kdoc de [FeatureFlagProvider]). #1347 (Firebase Remote
 * Config) ainda nao esta pronto para governar isto — quando estiver, este flag
 * migra para la, sem novo mecanismo paralelo nesse meio tempo.
 */
class DiagnosticDivergenceReporter(
    private val baseUrl: String,
    private val client: OkHttpClient,
    private val featureFlagProvider: FeatureFlagProvider,
    private val appVersion: String,
    private val versionCode: Int,
    // null-safe: default false garante que sem consentimento LGPD nada e enviado,
    // mesmo padrao de AdminIngestRepository.
    private val consentimentoProvider: suspend () -> Boolean = { false },
) {
    fun isEnabled(): Boolean = featureFlagProvider.isDiagnosticShadowModeEnabled()

    /**
     * Envia um registro de divergencia. Fire-and-forget: nunca lanca excecao,
     * nunca bloqueia. Silencioso quando desligado por flag, sem consentimento
     * LGPD ou sem baseUrl configurada.
     */
    suspend fun report(
        executionId: String,
        snapshotHash: String,
        comparison: DiagnosticDivergenceClassifier.Comparison,
        localSource: DiagnosticEvaluationSource,
        remoteSource: DiagnosticEvaluationSource?,
        localDurationMs: Long,
        remoteDurationMs: Long?,
    ) {
        if (!isEnabled()) return
        if (!consentimentoProvider()) return
        if (baseUrl.isBlank()) {
            Timber.w("DiagnosticDivergenceReporter: baseUrl nao configurada, ignorando")
            return
        }

        runCatching {
            withContext(Dispatchers.IO) {
                val body = toJson(executionId, snapshotHash, comparison, localSource, remoteSource, localDurationMs, remoteDurationMs)
                    .toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(baseUrl.trimEnd('/') + "/ingest/diagnostic-divergence")
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Timber.w("DiagnosticDivergenceReporter: worker HTTP ${response.code} — execution=$executionId")
                    }
                }
            }
        }.onFailure { t ->
            Timber.w(t, "DiagnosticDivergenceReporter: falha ao reportar divergencia (ignorando) — execution=$executionId")
        }
    }

    private fun toJson(
        executionId: String,
        snapshotHash: String,
        comparison: DiagnosticDivergenceClassifier.Comparison,
        localSource: DiagnosticEvaluationSource,
        remoteSource: DiagnosticEvaluationSource?,
        localDurationMs: Long,
        remoteDurationMs: Long?,
    ): JSONObject {
        val o = JSONObject()
        o.put("executionId", executionId)
        o.put("snapshotHash", snapshotHash)
        o.put("classification", comparison.classification.wireValue)
        comparison.localStatus?.let { o.put("localStatus", it.name) }
        comparison.remoteStatus?.let { o.put("remoteStatus", it.name) }
        comparison.localScore?.let { o.put("localScore", it) }
        comparison.remoteScore?.let { o.put("remoteScore", it) }
        comparison.localFlow?.let { o.put("localFlow", it) }
        comparison.remoteFlow?.let { o.put("remoteFlow", it) }
        o.put("localSource", localSource.wireValue)
        remoteSource?.let { o.put("remoteSource", it.wireValue) }
        o.put("localDurationMs", localDurationMs)
        remoteDurationMs?.let { o.put("remoteDurationMs", it) }
        o.put("appVersion", appVersion)
        o.put("versionCode", versionCode)
        return o
    }
}
