package io.signallq.app.ui.screen

import io.signallq.app.ads.AdSlot
import io.signallq.app.ui.ads.NativeAdIneligibleReason
import io.signallq.app.ui.ads.NativeAdLoadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GH#1785 — migração do call site de anúncio de `DispositivosLista.kt` de rememberNativeAd()
 * (legado) pra rememberNativeAdState() (contrato tipado). eligibilidadeAnuncioDispositivos é o
 * mapeamento puro entre o único sinal que a tela recebe de fora (adsEnabled) e
 * NativeAdEligibility -- mesmo padrão de eligibilidadeAnuncioResultado
 * (ResultadoVelocidadeScreenTest).
 */
class DispositivosListaNativeAdTest {
    @Test
    fun `eligibilidadeAnuncioDispositivos com adsEnabled true habilita flag e consentimento e assume online`() {
        val eligibility = eligibilidadeAnuncioDispositivos(adsEnabled = true)

        assertEquals(AdSlot.DISPOSITIVOS, eligibility.slot)
        assertTrue(eligibility.flagEnabled)
        assertTrue(eligibility.canRequestAds)
        assertTrue(eligibility.online)
        assertTrue(eligibility.canLoad)
        assertEquals(NativeAdLoadState.Loading, eligibility.initialState())
    }

    @Test
    fun `eligibilidadeAnuncioDispositivos com adsEnabled false desabilita flag e consentimento`() {
        val eligibility = eligibilidadeAnuncioDispositivos(adsEnabled = false)

        assertFalse(eligibility.flagEnabled)
        assertFalse(eligibility.canRequestAds)
        assertFalse(eligibility.canLoad)
        assertEquals(
            NativeAdLoadState.Ineligible(NativeAdIneligibleReason.FlagDisabled),
            eligibility.initialState(),
        )
    }

    @Test
    fun `eligibilidadeAnuncioDispositivos sempre usa o slot DISPOSITIVOS`() {
        assertEquals(AdSlot.DISPOSITIVOS, eligibilidadeAnuncioDispositivos(adsEnabled = true).slot)
        assertEquals(AdSlot.DISPOSITIVOS, eligibilidadeAnuncioDispositivos(adsEnabled = false).slot)
    }
}
