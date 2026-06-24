package io.veloo.app.feature.diagnostico.ai

import io.veloo.app.feature.diagnostico.DiagnosticReport
import io.veloo.app.feature.diagnostico.DiagnosticResult
import io.veloo.app.feature.diagnostico.DiagnosticStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cenario 5 do briefing — fallback local.
 *
 * Quando a IA falha (sem auth, timeout, !2xx, JSON invalido) o app continua
 * mostrando um diagnostico. O rodape DEVE indicar "Diagnostico local do SignallQ",
 * nunca "SignallQ IA — Llama 3.3 70B" (seria mentira).
 */
class AiFallbackFactoryTest {

    private fun fakeReport(status: DiagnosticStatus): DiagnosticReport {
        val decisao = DiagnosticResult(
            id = "dec-1",
            titulo = "Decisao local",
            status = status,
            evidencia = null,
            mensagemUsuario = "Mensagem local de fallback.",
            recomendacao = "Verifique seu roteador.",
            categoria = "isp",
            podeConcluir = true,
        )
        return DiagnosticReport(
            wifiResultados = emptyList(),
            internetResultados = emptyList(),
            mobileResultados = emptyList(),
            fibraResultados = emptyList(),
            dnsResultados = emptyList(),
            historicoResultados = emptyList(),
            wifiCanalResultados = emptyList(),
            decisao = decisao,
            perfisUsoSpeedtest = null,
            geradoEmMs = 1700000000000L,
        )
    }

    @Test
    fun cenario5_fallbackLocal_rodapeDiagnosticoLocal() {
        val report = fakeReport(DiagnosticStatus.attention)
        val result = AiFallbackFactory.fromLocal(report)

        // Schema v3 mesmo no fallback (alinhado com AI_PROMPT_VERSION = "diagnostico_v3_raw")
        assertEquals("3", result.schemaVersion)
        assertEquals("local", result.source)

        // ModeloIa nao mente: provedor "local", familia "Local"
        assertEquals("local", result.modeloIa.provedor)
        assertEquals("Local", result.modeloIa.familia)
        assertEquals("Diagnóstico local", result.modeloIa.nomeExibicao)
        assertEquals("Diagnóstico local do SignallQ", result.modeloIa.nomeCompletoComercial)
        assertEquals("Motor de análise: Diagnóstico local do SignallQ", result.modeloIa.textoRodape)

        // NUNCA pode dizer "SignallQ IA" no fallback (sem IA real)
        assertFalse(
            "Fallback nao deve usar nome comercial 'SignallQ IA'",
            result.modeloIa.textoRodape.contains("SignallQ IA"),
        )
        assertFalse(result.modeloIa.nomeExibicao.contains("Llama"))
        assertFalse(result.modeloIa.nomeExibicao.contains("Gemma"))

        // Status mapeado de attention para regular
        assertEquals("regular", result.status)

        // Acoes recomendadas vem da decisao local
        assertTrue(result.acoesRecomendadas.isNotEmpty())
        assertEquals("observacao", result.acoesRecomendadas.first().tipo)
    }

    @Test
    fun fallbackLocal_statusCritical_mapeiaParaCritico() {
        val result = AiFallbackFactory.fromLocal(fakeReport(DiagnosticStatus.critical))
        assertEquals("critico", result.status)
    }

    @Test
    fun fallbackLocal_statusOk_mapeiaParaBom() {
        val result = AiFallbackFactory.fromLocal(fakeReport(DiagnosticStatus.ok))
        assertEquals("bom", result.status)
    }
}
