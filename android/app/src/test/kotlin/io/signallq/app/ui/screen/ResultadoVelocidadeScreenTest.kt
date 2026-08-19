package io.signallq.app.ui.screen

import io.signallq.app.ads.AdSlot
import io.signallq.app.core.recommendation.RecommendationType
import io.signallq.app.ui.ads.NativeAdIneligibleReason
import io.signallq.app.ui.ads.NativeAdLoadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GH#536 — orientação por tipo de rede exibida no diagnóstico detalhado deve
 * diferenciar Wi-Fi, móvel (com tecnologia) e caso não identificado.
 */
class ResultadoVelocidadeScreenTest {
    @Test
    fun `wifi menciona Wi-Fi na orientacao`() {
        val texto = orientacaoPorTipoDeRede("wifi", null)
        assertTrue(texto.contains("Wi-Fi"))
    }

    @Test
    fun `movel com 5G menciona 5G na orientacao`() {
        val texto = orientacaoPorTipoDeRede("movel", "5G NSA")
        assertTrue(texto.contains("5G"))
    }

    @Test
    fun `movel com 4G LTE menciona 4G na orientacao`() {
        val texto = orientacaoPorTipoDeRede("movel", "4G LTE")
        assertTrue(texto.contains("4G"))
    }

    @Test
    fun `movel sem tecnologia informada usa rede movel generica`() {
        val texto = orientacaoPorTipoDeRede("movel", null)
        assertTrue(texto.contains("rede móvel"))
    }

    @Test
    fun `tipo desconhecido pede para repetir o teste`() {
        val texto = orientacaoPorTipoDeRede(null, null)
        assertTrue(texto.contains("não identificado"))
    }

    // GH#813 — badge de tipo da recomendacao do Recommendation Engine (RecommendationType).
    @Test
    fun `free tip mostra rotulo dica gratuita`() {
        assertEquals("DICA", recommendationTypeLabel(RecommendationType.FREE_TIP))
    }

    @Test
    fun `tutorial mostra rotulo tutorial`() {
        assertEquals("TUTORIAL", recommendationTypeLabel(RecommendationType.TUTORIAL))
    }

    @Test
    fun `configuration mostra rotulo configuracao`() {
        assertEquals("AJUSTE RECOMENDADO", recommendationTypeLabel(RecommendationType.CONFIGURATION))
    }

    @Test
    fun `todos os tipos monetizados tem rotulo distinto e nao vazio`() {
        val monetizados =
            listOf(
                RecommendationType.AFFILIATE_PRODUCT,
                RecommendationType.PARTNER_OFFER,
                RecommendationType.OPERATOR_OFFER,
                RecommendationType.NATIVE_AD_FALLBACK,
            )
        val rotulos = monetizados.map { recommendationTypeLabel(it) }
        assertTrue(rotulos.all { it.isNotBlank() })
        assertEquals(rotulos.distinct().size, rotulos.size)
    }

    // GH#1521 (P0-1 da auditoria #1228) tinha introduzido comSeveridadeConciliada() pra
    // reconciliar o card de latencia/upload com o achado do InternetDiagnosticEngine — a
    // NDS-02d (#1752, ADR-017) removeu essa mecanica por completo (dependia de duas reguas
    // numericas paralelas e de uma taxonomia de ID que o NDS nao reproduz, decisao
    // registrada no inventario de #1746). Os testes que caracterizavam a conciliacao
    // saíram junto — a classificacao dos 6 cards agora é só a convergência com
    // MetricClassifier, coberta em ClassificacaoMetricaLocalTest (ui/component).

    // =========================================================================
    // GH#1659a — migração AdMob: rememberNativeAd() (colapsa tudo em NativeAd? nulo) para o
    // contrato tipado NativeAdLoadState via rememberNativeAdState(). eligibilidadeAnuncioResultado
    // é o mapeamento puro entre o único sinal que a tela recebe de fora (adsEnabled) e
    // NativeAdEligibility -- a parte determinística e testável sem SDK/rede real do AdMob (mesmo
    // limite documentado em AppShellRootRegistryTest: sob Robolectric não há fill real, então não
    // existe asserção de UI capaz de distinguir eligible=true de eligible=false).
    // =========================================================================

    @Test
    fun `eligibilidadeAnuncioResultado com adsEnabled true habilita flag e consentimento e assume online`() {
        val eligibility = eligibilidadeAnuncioResultado(adsEnabled = true)

        assertEquals(AdSlot.RESULTADO, eligibility.slot)
        assertTrue(eligibility.flagEnabled)
        assertTrue(eligibility.canRequestAds)
        assertTrue(eligibility.online)
        assertTrue(eligibility.canLoad)
        assertEquals(NativeAdLoadState.Loading, eligibility.initialState())
    }

    @Test
    fun `eligibilidadeAnuncioResultado com adsEnabled false desabilita flag e consentimento`() {
        val eligibility = eligibilidadeAnuncioResultado(adsEnabled = false)

        assertFalse(eligibility.flagEnabled)
        assertFalse(eligibility.canRequestAds)
        assertFalse(eligibility.canLoad)
        assertEquals(
            NativeAdLoadState.Ineligible(NativeAdIneligibleReason.FlagDisabled),
            eligibility.initialState(),
        )
    }

    @Test
    fun `eligibilidadeAnuncioResultado sempre usa o slot RESULTADO`() {
        assertEquals(AdSlot.RESULTADO, eligibilidadeAnuncioResultado(adsEnabled = true).slot)
        assertEquals(AdSlot.RESULTADO, eligibilidadeAnuncioResultado(adsEnabled = false).slot)
    }

    // =========================================================================
    // GH#1659a — try/catch no compartilhamento: antes, uma IllegalStateException de
    // ResultadoPdfGenerator.gerarECompartilhar (dentro de scope.launch{}) travava o spinner
    // "compartilhando = true" pra sempre, sem nada visível pro usuário. mensagemErroCompartilhamento
    // Resultado é a mensagem que agora fica visível no lugar (mesmo padrão de
    // LaudoScreen.compartilharLaudo).
    // =========================================================================

    @Test
    fun `mensagem de erro de compartilhamento nunca fica vazia e cita o motivo original`() {
        val mensagem = mensagemErroCompartilhamentoResultado(IllegalStateException("Falha ao gerar PDF via WebView"))

        assertTrue(mensagem.isNotBlank())
        assertTrue(mensagem.contains("compartilhar", ignoreCase = true))
        assertTrue(mensagem.contains("Falha ao gerar PDF via WebView"))
    }

    @Test
    fun `mensagem de erro de compartilhamento nao quebra quando a excecao nao tem detalhe`() {
        val mensagem = mensagemErroCompartilhamentoResultado(IllegalStateException())

        assertTrue(mensagem.isNotBlank())
        assertTrue(mensagem.contains("compartilhar", ignoreCase = true))
    }
}
