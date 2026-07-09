package io.signallq.app.core.recommendation

import io.signallq.app.core.recommendation.catalog.LocalRecommendationCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {

    private val fixedNow = 1_700_000_000_000L
    private val engine = RecommendationEngine(
        catalog = LocalRecommendationCatalog(),
        now = { fixedNow },
    )

    @Test
    fun `diagnostico com wifi fraco escolhe recomendacao gratuita`() {
        val request = RecommendationRequest(
            tags = setOf(DiagnosticTag.WIFI_FRACO),
            network = NetworkContextType.WIFI,
        )

        val decision = engine.choose(request)

        assertNotNull(decision)
        assertEquals(RecommendationType.FREE_TIP, decision!!.type)
        assertEquals("free_tip_reposicionar_roteador", decision.recommendation.id)
        assertTrue(!decision.monetized)
    }

    @Test
    fun `diagnostico com sinal baixo forte relacao escolhe produto afiliado`() {
        val request = RecommendationRequest(
            tags = setOf(DiagnosticTag.SINAL_BAIXO),
            network = NetworkContextType.WIFI,
        )

        val decision = engine.choose(request)

        assertNotNull(decision)
        assertEquals(RecommendationType.AFFILIATE_PRODUCT, decision!!.type)
        assertEquals("affiliate_repetidor_wifi", decision.recommendation.id)
        assertTrue(decision.monetized)
    }

    @Test
    fun `afiliado nao elegivel quando relacao com diagnostico e fraca`() {
        // sinal_baixo casa com affiliate_repetidor_wifi, mas junto de tags irrelevantes
        // o match ratio da recomendacao continua 1 de 1 -- entao forcamos o cenario fraco
        // via minAffiliateMatchRatio alto, simulando exigencia remota mais rigorosa.
        val request = RecommendationRequest(
            tags = setOf(DiagnosticTag.SINAL_BAIXO),
            network = NetworkContextType.WIFI,
            flags = RecommendationFlags(minAffiliateMatchRatio = 1.5, nativeAdFallbackEnabled = false),
        )

        val decision = engine.choose(request)

        assertNull(decision)
    }

    @Test
    fun `sem recomendacao contextual elegivel cai no fallback de admob nativo`() {
        val request = RecommendationRequest(
            tags = setOf(DiagnosticTag.PERDA_PACOTES_ALTA),
            network = NetworkContextType.WIFI,
        )

        val decision = engine.choose(request)

        assertNotNull(decision)
        assertEquals(RecommendationType.NATIVE_AD_FALLBACK, decision!!.type)
        assertEquals("native_ad_fallback_default", decision.recommendation.id)
        assertTrue(decision.monetized)
    }

    @Test
    fun `fallback de admob nao e retornado quando existe afiliado elegivel`() {
        val request = RecommendationRequest(
            tags = setOf(DiagnosticTag.SINAL_BAIXO),
            network = NetworkContextType.WIFI,
        )

        val ranked = engine.rank(request)

        // apenas um card monetizado por diagnostico -- nunca afiliado + admob juntos
        val monetizedTypes = ranked.map { it.type }.filter { it != RecommendationType.FREE_TIP }
        assertEquals(1, ranked.size)
        assertEquals(listOf(RecommendationType.AFFILIATE_PRODUCT), monetizedTypes)
        assertTrue(ranked.none { it.type == RecommendationType.NATIVE_AD_FALLBACK })
    }

    @Test
    fun `nao repete a mesma recomendacao em sequencia mesmo sem cooldown vencido`() {
        val request = RecommendationRequest(
            tags = setOf(DiagnosticTag.DNS_LENTO),
            network = NetworkContextType.WIFI,
            history = listOf(
                RecommendationHistoryEntry(
                    recommendationId = "configuration_trocar_dns",
                    shownAt = fixedNow - 10_000L,
                ),
            ),
            flags = RecommendationFlags(nativeAdFallbackEnabled = false),
        )

        val decision = engine.choose(request)

        assertNull(decision)
    }

    @Test
    fun `respeita cooldown da recomendacao afiliada e nao a repete antes do prazo`() {
        val history = listOf(
            RecommendationHistoryEntry(
                recommendationId = "affiliate_repetidor_wifi",
                shownAt = fixedNow - HOUR_MS, // cooldown da recomendacao e 72h
            ),
        )
        val request = RecommendationRequest(
            tags = setOf(DiagnosticTag.SINAL_BAIXO),
            network = NetworkContextType.WIFI,
            history = history,
            flags = RecommendationFlags(nativeAdFallbackEnabled = false),
        )

        val decision = engine.choose(request)

        assertNull(decision)
    }

    @Test
    fun `libera recomendacao novamente apos cooldown expirar`() {
        val cooldownMs = 72 * HOUR_MS
        val history = listOf(
            RecommendationHistoryEntry(
                recommendationId = "affiliate_repetidor_wifi",
                shownAt = fixedNow - cooldownMs - HOUR_MS,
            ),
            // entrada mais recente e de outra recomendacao para nao cair na regra de nao-repeticao em sequencia
            RecommendationHistoryEntry(
                recommendationId = "free_tip_reposicionar_roteador",
                shownAt = fixedNow - HOUR_MS,
            ),
        )
        val request = RecommendationRequest(
            tags = setOf(DiagnosticTag.SINAL_BAIXO),
            network = NetworkContextType.WIFI,
            history = history,
        )

        val decision = engine.choose(request)

        assertNotNull(decision)
        assertEquals("affiliate_repetidor_wifi", decision!!.recommendation.id)
    }

    @Test
    fun `recomendacao marcada como hide pelo usuario nunca mais e elegivel`() {
        val request = RecommendationRequest(
            tags = setOf(DiagnosticTag.WIFI_FRACO),
            network = NetworkContextType.WIFI,
            history = listOf(
                RecommendationHistoryEntry(
                    recommendationId = "free_tip_reposicionar_roteador",
                    shownAt = fixedNow - HOUR_MS,
                    feedback = RecommendationFeedbackType.HIDE,
                    feedbackAt = fixedNow - HOUR_MS,
                ),
            ),
            flags = RecommendationFlags(nativeAdFallbackEnabled = false),
        )

        val decision = engine.choose(request)

        assertNull(decision)
    }

    private companion object {
        const val HOUR_MS = 3_600_000L
    }
}
