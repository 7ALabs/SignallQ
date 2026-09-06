package io.signallq.app.feature.diagnostico.remote

import io.signallq.app.core.diagnostico.DiagnosticDivergenceClassifier
import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticRolloutEligibility
import io.signallq.app.core.diagnostico.DiagnosticRolloutStatus
import io.signallq.app.core.featureflags.FeatureFlagKeys
import io.signallq.app.core.featureflags.FeatureFlagProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber

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
 * Kill switch: [FeatureFlagProvider] com a chave
 * [FeatureFlagKeys.CONSUMER_DIAGNOSTICO_SHADOW_MODE_ENABLED] (Firebase Remote Config,
 * Epico #1347/`:core:featureflags`). Migrado do mecanismo legado SIG-13
 * (`signallq-admin-worker` `GET /flags`, chave `feature_diagnostic_shadow_mode`) em
 * 2026-08-01 (issue #1497) — era o unico consumidor real restante do sistema legado.
 *
 * ## Rollout gradual + segmentacao (GH#1445, parte de #952)
 * [isEnabled] continua sendo SO o kill switch booleano (flag ligada/desligada
 * para todo mundo) — [isEligibleForShadowComparison] e o metodo usado de fato
 * pelo caminho de producao ([RemoteDiagnosticRepository.evaluateShadow]),
 * combinando o kill switch com o percentual/segmentacao publicados no worker
 * (`GET /diagnostic/rollout-status`, via [rolloutStatusProvider]) e o
 * identificador estavel de instalacao ja existente no app
 * ([installationIdProvider] — `PreferenciasAppRepository.buscarOuGerarAnonDeviceId()`,
 * o mesmo `anon_device_id` ja usado por `AdminSyncWorker`/`CompositeAnalyticsTracker`,
 * nunca um identificador novo criado so pra isto). Qualquer falha em obter o
 * status de rollout (`null`) resulta em NAO participar (fail closed) — nunca
 * assume 100%.
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
    // GH#1445 — identificador estavel de instalacao (sem PII) para o bucketing
    // de rollout. Default vazio: sem provider configurado, RolloutBucketCalculator
    // trata string em branco como fail closed (nunca participa).
    private val installationIdProvider: suspend () -> String = { "" },
    // GH#1445 — status de rollout do ruleset PUBLISHED atual. Default null:
    // sem provider configurado, tratado como "nao participa" (fail closed).
    private val rolloutStatusProvider: suspend () -> DiagnosticRolloutStatus? = { null },
    // GH#1445 — canal de distribuicao desta instalacao (ex.: "play_store",
    // "sideload"), para a dimensao de segmentacao por canal.
    private val appChannel: String = "unknown",
) {
    fun isEnabled(): Boolean = featureFlagProvider.isEnabled(FeatureFlagKeys.CONSUMER_DIAGNOSTICO_SHADOW_MODE_ENABLED)

    /**
     * GH#1445 — decide se esta instalacao participa do shadow mode AGORA:
     * kill switch ligado + status de rollout disponivel + dentro do
     * percentual/segmentacao publicados. Usado pelo caminho de producao
     * ([RemoteDiagnosticRepository.evaluateShadow]) em vez de [isEnabled]
     * sozinho.
     */
    suspend fun isEligibleForShadowComparison(): Boolean {
        if (!isEnabled()) return false
        val status = rolloutStatusProvider() ?: return false
        val installationId = installationIdProvider()
        return DiagnosticRolloutEligibility.isEligible(
            status = status,
            installationId = installationId,
            appVersionCode = versionCode,
            appChannel = appChannel,
        )
    }

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
                val body =
                    toJson(executionId, snapshotHash, comparison, localSource, remoteSource, localDurationMs, remoteDurationMs)
                        .toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())
                val request =
                    Request
                        .Builder()
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
