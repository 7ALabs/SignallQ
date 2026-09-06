package io.signallq.app.ui.ads

import io.signallq.app.ads.AdSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAdLoadStateTest {
    @Test
    fun `flag disabled is ineligible before consent and network`() {
        val eligibility = NativeAdEligibility(AdSlot.HISTORICO, flagEnabled = false, canRequestAds = true, online = true)

        assertEquals(
            NativeAdLoadState.Ineligible(NativeAdIneligibleReason.FlagDisabled),
            eligibility.initialState(),
        )
        assertFalse(eligibility.canLoad)
    }

    @Test
    fun `missing UMP consent never permits request`() {
        val eligibility = NativeAdEligibility(AdSlot.JOGOS, flagEnabled = true, canRequestAds = false, online = true)

        assertEquals(
            NativeAdLoadState.Ineligible(NativeAdIneligibleReason.ConsentUnavailable),
            eligibility.initialState(),
        )
        assertFalse(eligibility.canLoad)
    }

    @Test
    fun `offline is explicit and does not permit request`() {
        val eligibility = NativeAdEligibility(AdSlot.DISPOSITIVOS, flagEnabled = true, canRequestAds = true, online = false)

        assertEquals(NativeAdLoadState.Offline, eligibility.initialState())
        assertFalse(eligibility.canLoad)
    }

    @Test
    fun `eligible online state starts one controlled loading session`() {
        val eligibility = NativeAdEligibility(AdSlot.RESULTADO, flagEnabled = true, canRequestAds = true, online = true)

        assertEquals(NativeAdLoadState.Loading, eligibility.initialState())
        assertTrue(eligibility.canLoad)
    }
}
