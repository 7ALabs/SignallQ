package io.signallq.app.ui.screen

import io.signallq.app.ads.AdSlot
import io.signallq.app.core.diagnostico.DiagnosticResult
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.MetricClassifier
import io.signallq.app.core.diagnostico.MetricStatus
import io.signallq.app.core.recommendation.RecommendationType
import io.signallq.app.ui.ads.NativeAdIneligibleReason
import io.signallq.app.ui.ads.NativeAdLoadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

    // GH#1521 (P0-1 da auditoria #1228) — card de metrica e banner desta tela nao podem
    // mais divergir pra latencia/upload. Cenarios 3 e 4 da tabela de caracterizacao
    // (docs_ai/ARQUITETURA/AUDITORIA_1228_FASE0_INVENTARIO_COMPLETO.md).

    private fun achadoAtivo(
        id: String,
        status: DiagnosticStatus,
    ) = DiagnosticResult(
        id = id,
        titulo = "titulo",
        status = status,
        evidencia = null,
        mensagemUsuario = "mensagem",
        recomendacao = null,
        categoria = "internet",
    )

    @Test
    fun `latencia 120ms nao mostra veredito melhor que o achado IN-NORMAL-05`() {
        // MetricClassifier isolado classificaria 120ms como "bom" (ate 150ms) — era
        // exatamente esse o bug: card "Bom" enquanto o banner (achado abaixo) diz
        // "demorando para responder".
        val statusIsolado = MetricClassifier.classificarLatencia(120.0)
        assertEquals(MetricStatus.bom, statusIsolado)

        val achados = listOf(achadoAtivo("IN-NORMAL-05", DiagnosticStatus.attention))
        val statusConciliado = statusIsolado.comSeveridadeConciliada(achados, idPrefix = "IN-NORMAL-05")

        assertNotEquals(MetricStatus.bom, statusConciliado)
        assertNotEquals(MetricStatus.excelente, statusConciliado)
        assertEquals(MetricStatus.regular, statusConciliado)
    }

    @Test
    fun `latencia sem achado ativo mantem veredito do MetricClassifier`() {
        val statusIsolado = MetricClassifier.classificarLatencia(50.0)
        val statusConciliado = statusIsolado.comSeveridadeConciliada(emptyList(), idPrefix = "IN-NORMAL-05")
        assertEquals(statusIsolado, statusConciliado)
    }

    @Test
    fun `upload entre 3 e 10 Mbps nao mostra veredito melhor que o achado IN-NORMAL-04`() {
        // Faixa citada na auditoria como divergente (issue #1466): dentro dela,
        // MetricClassifier ja da "regular" (nao "bom"/"excelente"), entao o piso do
        // achado (attention -> regular) nao rebaixa nada — o teste trava que a
        // conciliacao nunca deixa o card "melhor" que o achado do motor.
        listOf(3.0, 4.0, 4.9).forEach { uploadMbps ->
            val statusIsolado = MetricClassifier.classificarUpload(uploadMbps)
            val achados = listOf(achadoAtivo("IN-NORMAL-04", DiagnosticStatus.attention))
            val statusConciliado = statusIsolado.comSeveridadeConciliada(achados, idPrefix = "IN-NORMAL-04")

            assertNotEquals("uploadMbps=$uploadMbps", MetricStatus.bom, statusConciliado)
            assertNotEquals("uploadMbps=$uploadMbps", MetricStatus.excelente, statusConciliado)
        }
    }

    @Test
    fun `upload zerado com achado critico IN-NORMAL-04Z conserva severidade critica`() {
        val statusIsolado = MetricClassifier.classificarUpload(0.0)
        assertEquals(MetricStatus.critico, statusIsolado)

        val achados = listOf(achadoAtivo("IN-NORMAL-04Z", DiagnosticStatus.critical))
        val statusConciliado = statusIsolado.comSeveridadeConciliada(achados, idPrefix = "IN-NORMAL-04")

        assertEquals(MetricStatus.critico, statusConciliado)
    }

    @Test
    fun `conciliacao nunca abranda um MetricClassifier ja mais severo que o achado`() {
        // lat=250ms: MetricClassifier ja da "ruim" (mais severo que o piso "regular" do
        // achado attention) — a conciliacao nao pode suavizar isso.
        val statusIsolado = MetricClassifier.classificarLatencia(250.0)
        assertEquals(MetricStatus.ruim, statusIsolado)

        val achados = listOf(achadoAtivo("IN-NORMAL-05", DiagnosticStatus.attention))
        val statusConciliado = statusIsolado.comSeveridadeConciliada(achados, idPrefix = "IN-NORMAL-05")

        assertEquals(MetricStatus.ruim, statusConciliado)
    }

    @Test
    fun `veredito inconclusivo nunca e sobrescrito pela conciliacao`() {
        val achados = listOf(achadoAtivo("IN-NORMAL-04", DiagnosticStatus.critical))
        val statusConciliado = MetricStatus.inconclusivo.comSeveridadeConciliada(achados, idPrefix = "IN-NORMAL-04")
        assertEquals(MetricStatus.inconclusivo, statusConciliado)
    }

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
