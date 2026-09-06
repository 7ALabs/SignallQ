package io.signallq.app.core.diagnostico

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** GH#1445 (parte de #952) — segmentacao (versao minima + canal) combinada com o bucket de rollout. */
class DiagnosticRolloutEligibilityTest {
    private val installationDentroDoBucket = "installation-in"
    private val installationForaDoBucket = "installation-out"

    @Test
    fun `sem restricao de versao ou canal, so o percentual decide`() {
        val status = DiagnosticRolloutStatus(rulesetVersion = 1, rolloutPercent = 100)
        assertTrue(
            DiagnosticRolloutEligibility.isEligible(
                status = status,
                installationId = "qualquer-instalacao",
                appVersionCode = 100,
                appChannel = "play_store",
            ),
        )
    }

    @Test
    fun `rolloutPercent zero nunca elegivel mesmo sem restricao de segmentacao`() {
        val status = DiagnosticRolloutStatus(rulesetVersion = 1, rolloutPercent = 0)
        assertFalse(
            DiagnosticRolloutEligibility.isEligible(
                status = status,
                installationId = "qualquer-instalacao",
                appVersionCode = 999,
                appChannel = "play_store",
            ),
        )
    }

    @Test
    fun `appVersionCode abaixo de rolloutMinVersionCode nunca elegivel`() {
        val status = DiagnosticRolloutStatus(rulesetVersion = 1, rolloutPercent = 100, rolloutMinVersionCode = 320)
        assertFalse(
            DiagnosticRolloutEligibility.isEligible(
                status = status,
                installationId = "qualquer-instalacao",
                appVersionCode = 319,
                appChannel = "play_store",
            ),
        )
    }

    @Test
    fun `appVersionCode igual ou acima de rolloutMinVersionCode passa a restricao de versao`() {
        val status = DiagnosticRolloutStatus(rulesetVersion = 1, rolloutPercent = 100, rolloutMinVersionCode = 320)
        assertTrue(
            DiagnosticRolloutEligibility.isEligible(
                status = status,
                installationId = "qualquer-instalacao",
                appVersionCode = 320,
                appChannel = "play_store",
            ),
        )
    }

    @Test
    fun `canal fora da lista de rolloutChannels nunca elegivel`() {
        val status = DiagnosticRolloutStatus(rulesetVersion = 1, rolloutPercent = 100, rolloutChannels = listOf("play_store"))
        assertFalse(
            DiagnosticRolloutEligibility.isEligible(
                status = status,
                installationId = "qualquer-instalacao",
                appVersionCode = 100,
                appChannel = "sideload",
            ),
        )
    }

    @Test
    fun `canal dentro de rolloutChannels passa a restricao de canal`() {
        val status = DiagnosticRolloutStatus(rulesetVersion = 1, rolloutPercent = 100, rolloutChannels = listOf("play_store", "sideload"))
        assertTrue(
            DiagnosticRolloutEligibility.isEligible(
                status = status,
                installationId = "qualquer-instalacao",
                appVersionCode = 100,
                appChannel = "sideload",
            ),
        )
    }

    @Test
    fun `rolloutChannels vazio equivale a sem restricao (mesmo comportamento de null)`() {
        val status = DiagnosticRolloutStatus(rulesetVersion = 1, rolloutPercent = 100, rolloutChannels = emptyList())
        assertTrue(
            DiagnosticRolloutEligibility.isEligible(
                status = status,
                installationId = "qualquer-instalacao",
                appVersionCode = 100,
                appChannel = "unknown",
            ),
        )
    }

    @Test
    fun `passa versao e canal mas fica de fora pelo bucket de percentual`() {
        val bucket = RolloutBucketCalculator.bucketOf(installationForaDoBucket)
        val status = DiagnosticRolloutStatus(rulesetVersion = 1, rolloutPercent = bucket, rolloutMinVersionCode = 1, rolloutChannels = listOf("play_store"))
        assertFalse(
            DiagnosticRolloutEligibility.isEligible(
                status = status,
                installationId = installationForaDoBucket,
                appVersionCode = 999,
                appChannel = "play_store",
            ),
        )
    }

    @Test
    fun `passa versao, canal e bucket -- elegivel`() {
        val bucket = RolloutBucketCalculator.bucketOf(installationDentroDoBucket)
        val status = DiagnosticRolloutStatus(rulesetVersion = 1, rolloutPercent = bucket + 1, rolloutMinVersionCode = 1, rolloutChannels = listOf("play_store"))
        assertTrue(
            DiagnosticRolloutEligibility.isEligible(
                status = status,
                installationId = installationDentroDoBucket,
                appVersionCode = 999,
                appChannel = "play_store",
            ),
        )
    }
}
