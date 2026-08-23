package io.signallq.app.ui.screen

import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticReport
import io.signallq.app.core.diagnostico.DiagnosticResult
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.DiagnosticoGuiadoEngine
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticoGuiadoResultadoCopyTest {
    @Test
    fun `resultado de jogos nao promete experiencia garantida`() {
        val titulo = tituloAssistSeguro(ObjetivoDiagnostico.JOGOS_COM_LAG, DiagnosticStatus.info)
        val mensagem = mensagemAssistSegura(ObjetivoDiagnostico.JOGOS_COM_LAG, DiagnosticStatus.info, 106.0, 16.0)

        assertEquals("Sua conexão funciona, mas pode apresentar atraso durante o jogo", titulo)
        assertFalse(titulo.contains("pronta"))
        assertFalse(mensagem.contains("garantida"))
        assertTrue(mensagem.contains("atraso"))
        assertTrue(mensagem.contains("partidas"))
    }

    @Test
    fun `aumento sob carga vira explicacao e proximo passo`() {
        val mensagem = mensagemAssistSegura(ObjetivoDiagnostico.JOGOS_COM_LAG, DiagnosticStatus.info, 106.0, 16.0)
        val resultado =
            DiagnosticoGuiadoEngine.avaliar(
                objetivo = ObjetivoDiagnostico.JOGOS_COM_LAG,
                respostas = listOf(0, 0),
                input = null,
            )
        val passos = passosAssistSeguro(resultado, emptyList(), 106.0, 16.0)

        assertTrue(mensagem.contains("90 ms"))
        assertTrue(passos.isNotEmpty())
        assertTrue(passos.any { it.contains("rede livre") })
    }

    @Test
    fun `dados ausentes nao expoem nomes do contrato`() {
        val copy =
            dadosAusentesEmLinguagemHumana(
                listOf("dns.latencyMs", "quality.packetLossPercent", "gateway.connectedDevices"),
            )

        assertTrue(copy.contains("resposta do DNS"))
        assertTrue(copy.contains("perda de pacotes"))
        assertTrue(copy.contains("dispositivos conectados"))
        assertFalse(copy.contains("latencyMs"))
        assertFalse(copy.contains("connectedDevices"))
    }

    @Test
    fun `confianca alta cai para media quando faltam dados`() {
        val report =
            DiagnosticReport(
                wifiResultados = emptyList(),
                internetResultados = emptyList(),
                fibraResultados = emptyList(),
                decisao =
                    DiagnosticResult(
                        id = "test",
                        titulo = "Teste",
                        status = DiagnosticStatus.info,
                        evidencia = null,
                        mensagemUsuario = "Teste",
                        recomendacao = null,
                        categoria = "test",
                        podeConcluir = true,
                    ),
                dadosAusentes = listOf("quality.jitterMs"),
                evaluationSource = DiagnosticEvaluationSource.REMOTE,
                geradoEmMs = 0L,
            )

        assertEquals("média", confiancaAssist(report))
    }
}
