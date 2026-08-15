package io.signallq.app.feature.diagnostico.remote

import io.signallq.app.core.diagnostico.DiagnosticDivergenceClassifier
import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticRolloutStatus
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.featureflags.FeatureFlagKey
import io.signallq.app.core.featureflags.FeatureFlagProvider
import io.signallq.app.core.featureflags.FeatureFlagRawValue
import io.signallq.app.core.featureflags.FeatureFlagRefreshResult
import io.signallq.app.core.featureflags.FeatureFlagSource
import io.signallq.app.core.featureflags.FeatureFlagValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * GH#1444 (parte de #952) — shadow mode: [DiagnosticDivergenceReporter] nunca
 * lanca excecao, respeita o kill switch (feature flag) e o consentimento LGPD
 * antes de enviar qualquer coisa, e serializa o payload sanitizado esperado
 * pelo worker (`POST /ingest/diagnostic-divergence`).
 */
class DiagnosticDivergenceReporterTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun flagProvider(enabled: Boolean): FeatureFlagProvider = object : FeatureFlagProvider {
        override fun observe(key: FeatureFlagKey): Flow<FeatureFlagValue> =
            flowOf(FeatureFlagValue(key = key, raw = FeatureFlagRawValue.BooleanValue(enabled), source = FeatureFlagSource.STATIC))

        override fun isEnabled(key: FeatureFlagKey): Boolean = enabled

        override suspend fun refresh(force: Boolean): FeatureFlagRefreshResult =
            FeatureFlagRefreshResult.Success(activated = false, fetchTimeMillis = null)
    }

    private fun sampleComparison() = DiagnosticDivergenceClassifier.Comparison(
        classification = DiagnosticDivergenceClassifier.Classification.MINOR_DIVERGENCE,
        localStatus = DiagnosticStatus.ok,
        remoteStatus = DiagnosticStatus.attention,
        localScore = 90,
        remoteScore = 60,
        localFlow = "wifi",
        remoteFlow = "wifi",
    )

    private fun reporter(enabled: Boolean, consentimento: Boolean): DiagnosticDivergenceReporter =
        DiagnosticDivergenceReporter(
            baseUrl = server.url("/").toString(),
            client = okhttp3.OkHttpClient(),
            featureFlagProvider = flagProvider(enabled),
            appVersion = "0.31.0",
            versionCode = 312,
            consentimentoProvider = { consentimento },
        )

    @Test
    fun `isEnabled reflete a flag feature_diagnostic_shadow_mode`() {
        assertEquals(true, reporter(enabled = true, consentimento = true).isEnabled())
        assertEquals(false, reporter(enabled = false, consentimento = true).isEnabled())
    }

    // --- GH#1445 (parte de #952) — rollout gradual + segmentacao do shadow mode ---

    private fun reporterComRollout(
        enabled: Boolean = true,
        installationId: String = "installation-1",
        rolloutStatus: DiagnosticRolloutStatus? = DiagnosticRolloutStatus(rulesetVersion = 1, rolloutPercent = 100),
        appChannel: String = "play_store",
        versionCode: Int = 312,
    ): DiagnosticDivergenceReporter =
        DiagnosticDivergenceReporter(
            baseUrl = server.url("/").toString(),
            client = okhttp3.OkHttpClient(),
            featureFlagProvider = flagProvider(enabled),
            appVersion = "0.31.0",
            versionCode = versionCode,
            consentimentoProvider = { true },
            installationIdProvider = { installationId },
            rolloutStatusProvider = { rolloutStatus },
            appChannel = appChannel,
        )

    @Test
    fun `isEligibleForShadowComparison falso quando o kill switch esta desligado, mesmo com rollout 100 por cento`() = runTest {
        val rep = reporterComRollout(enabled = false)
        assertEquals(false, rep.isEligibleForShadowComparison())
    }

    @Test
    fun `isEligibleForShadowComparison falso quando o status de rollout nao esta disponivel (fail closed)`() = runTest {
        val rep = reporterComRollout(rolloutStatus = null)
        assertEquals(false, rep.isEligibleForShadowComparison())
    }

    @Test
    fun `isEligibleForShadowComparison falso com rolloutPercent 0`() = runTest {
        val rep = reporterComRollout(rolloutStatus = DiagnosticRolloutStatus(rulesetVersion = 1, rolloutPercent = 0))
        assertEquals(false, rep.isEligibleForShadowComparison())
    }

    @Test
    fun `isEligibleForShadowComparison verdadeiro com kill switch ligado, rollout 100 por cento e instalacao valida`() = runTest {
        val rep = reporterComRollout()
        assertEquals(true, rep.isEligibleForShadowComparison())
    }

    @Test
    fun `isEligibleForShadowComparison respeita segmentacao por canal`() = runTest {
        val rep = reporterComRollout(
            rolloutStatus = DiagnosticRolloutStatus(rulesetVersion = 1, rolloutPercent = 100, rolloutChannels = listOf("play_store")),
            appChannel = "sideload",
        )
        assertEquals(false, rep.isEligibleForShadowComparison())
    }

    @Test
    fun `isEligibleForShadowComparison respeita segmentacao por versao minima`() = runTest {
        val rep = reporterComRollout(
            rolloutStatus = DiagnosticRolloutStatus(rulesetVersion = 1, rolloutPercent = 100, rolloutMinVersionCode = 400),
            versionCode = 312,
        )
        assertEquals(false, rep.isEligibleForShadowComparison())
    }

    @Test
    fun `report nao envia nada quando a flag esta desligada`() = runTest {
        val rep = reporter(enabled = false, consentimento = true)
        rep.report(
            executionId = "exec-1",
            snapshotHash = "hash-1",
            comparison = sampleComparison(),
            localSource = DiagnosticEvaluationSource.BUNDLED_LOCAL,
            remoteSource = DiagnosticEvaluationSource.REMOTE,
            localDurationMs = 10,
            remoteDurationMs = 500,
        )
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `report nao envia nada sem consentimento LGPD`() = runTest {
        val rep = reporter(enabled = true, consentimento = false)
        rep.report(
            executionId = "exec-1",
            snapshotHash = "hash-1",
            comparison = sampleComparison(),
            localSource = DiagnosticEvaluationSource.BUNDLED_LOCAL,
            remoteSource = DiagnosticEvaluationSource.REMOTE,
            localDurationMs = 10,
            remoteDurationMs = 500,
        )
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `report envia payload sanitizado quando habilitado e com consentimento`() = runTest {
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"ok":true,"stored":true,"id":"x"}"""))
        val rep = reporter(enabled = true, consentimento = true)

        rep.report(
            executionId = "exec-1",
            snapshotHash = "hash-1",
            comparison = sampleComparison(),
            localSource = DiagnosticEvaluationSource.BUNDLED_LOCAL,
            remoteSource = DiagnosticEvaluationSource.REMOTE,
            localDurationMs = 10,
            remoteDurationMs = 500,
        )

        val recorded = server.takeRequest()
        assertEquals("/ingest/diagnostic-divergence", recorded.path)
        assertEquals("POST", recorded.method)

        val body = JSONObject(recorded.body.readUtf8())
        assertEquals("exec-1", body.getString("executionId"))
        assertEquals("hash-1", body.getString("snapshotHash"))
        assertEquals("MINOR_DIVERGENCE", body.getString("classification"))
        assertEquals("ok", body.getString("localStatus"))
        assertEquals("attention", body.getString("remoteStatus"))
        assertEquals(90, body.getInt("localScore"))
        assertEquals(60, body.getInt("remoteScore"))
        assertEquals("wifi", body.getString("localFlow"))
        assertEquals("BUNDLED_LOCAL", body.getString("localSource"))
        assertEquals("REMOTE", body.getString("remoteSource"))
        assertEquals(10, body.getLong("localDurationMs"))
        assertEquals(500, body.getLong("remoteDurationMs"))
        assertEquals("0.31.0", body.getString("appVersion"))
        assertEquals(312, body.getInt("versionCode"))
    }

    @Test
    fun `report nunca lanca excecao quando o worker esta indisponivel`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val rep = reporter(enabled = true, consentimento = true)

        // Nao deve lancar -- fire-and-forget, falha so logada.
        rep.report(
            executionId = "exec-1",
            snapshotHash = "hash-1",
            comparison = sampleComparison(),
            localSource = DiagnosticEvaluationSource.BUNDLED_LOCAL,
            remoteSource = null,
            localDurationMs = 10,
            remoteDurationMs = null,
        )
    }

    @Test
    fun `report omite campos nulos opcionais (remoteStatus, remoteScore, remoteSource)`() = runTest {
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"ok":true}"""))
        val rep = reporter(enabled = true, consentimento = true)

        rep.report(
            executionId = "exec-2",
            snapshotHash = "hash-2",
            comparison = DiagnosticDivergenceClassifier.Comparison(
                classification = DiagnosticDivergenceClassifier.Classification.REMOTE_ERROR,
                localStatus = DiagnosticStatus.ok,
                remoteStatus = null,
                localScore = 90,
                remoteScore = null,
                localFlow = "wifi",
                remoteFlow = null,
            ),
            localSource = DiagnosticEvaluationSource.BUNDLED_LOCAL,
            remoteSource = null,
            localDurationMs = 10,
            remoteDurationMs = null,
        )

        val body = JSONObject(server.takeRequest().body.readUtf8())
        assertEquals("REMOTE_ERROR", body.getString("classification"))
        assertNull(body.opt("remoteStatus"))
        assertNull(body.opt("remoteScore"))
        assertNull(body.opt("remoteSource"))
        assertNull(body.opt("remoteDurationMs"))
    }
}
