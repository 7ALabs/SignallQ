package io.signallq.app.ui.screen

import io.signallq.app.ads.AdSlot
import io.signallq.app.ui.ads.NativeAdIneligibleReason
import io.signallq.app.ui.ads.NativeAdLoadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GH#1785 — migração do call site de anúncio de `ModoGamerConfigResultadoSection.kt` de
 * rememberNativeAd() (legado) pra rememberNativeAdState() (contrato tipado).
 * eligibilidadeAnuncioModoGamer é o mapeamento puro entre o único sinal que a tela recebe de fora
 * (adsEnabled) e NativeAdEligibility -- mesmo padrão de eligibilidadeAnuncioResultado
 * (ResultadoVelocidadeScreenTest).
 */
class ModoGamerConfigResultadoSectionNativeAdTest {
    @Test
    fun `eligibilidadeAnuncioModoGamer com adsEnabled true habilita flag e consentimento e assume online`() {
        val eligibility = eligibilidadeAnuncioModoGamer(adsEnabled = true)

        assertEquals(AdSlot.JOGOS, eligibility.slot)
        assertTrue(eligibility.flagEnabled)
        assertTrue(eligibility.canRequestAds)
        assertTrue(eligibility.online)
        assertTrue(eligibility.canLoad)
        assertEquals(NativeAdLoadState.Loading, eligibility.initialState())
    }

    @Test
    fun `eligibilidadeAnuncioModoGamer com adsEnabled false desabilita flag e consentimento`() {
        val eligibility = eligibilidadeAnuncioModoGamer(adsEnabled = false)

        assertFalse(eligibility.flagEnabled)
        assertFalse(eligibility.canRequestAds)
        assertFalse(eligibility.canLoad)
        assertEquals(
            NativeAdLoadState.Ineligible(NativeAdIneligibleReason.FlagDisabled),
            eligibility.initialState(),
        )
    }

    @Test
    fun `eligibilidadeAnuncioModoGamer sempre usa o slot JOGOS`() {
        assertEquals(AdSlot.JOGOS, eligibilidadeAnuncioModoGamer(adsEnabled = true).slot)
        assertEquals(AdSlot.JOGOS, eligibilidadeAnuncioModoGamer(adsEnabled = false).slot)
    }
}
