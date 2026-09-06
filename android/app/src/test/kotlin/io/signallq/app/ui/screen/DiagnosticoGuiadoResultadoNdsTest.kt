package io.signallq.app.ui.screen

import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.core.diagnostico.ResultadoDiagnosticoGuiado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticoGuiadoResultadoNdsTest {
    private val resultadoLocal =
        ResultadoDiagnosticoGuiado(
            objetivo = ObjetivoDiagnostico.LENTIDAO_GERAL,
            status = DiagnosticStatus.attention,
            mensagemMotor = "A conexão pode estar lenta.",
            evidencias = emptyList(),
            acoes = listOf("Ação determinada localmente."),
            dadosInsuficientes = false,
        )

    @Test
    fun `titulo e explicacao remotos sao protagonistas no Assist`() {
        assertEquals(
            "Pico de latência no horário de maior uso",
            tituloAssistVindoDoNds(
                tituloNds = "Pico de latência no horário de maior uso",
                objetivo = resultadoLocal.objetivo,
                status = resultadoLocal.status,
            ),
        )
        assertEquals(
            "A rede fica congestionada entre 19h e 22h.",
            mensagemAssistVindaDoNds(
                mensagemNds = "A rede fica congestionada entre 19h e 22h.",
                objetivo = resultadoLocal.objetivo,
                status = resultadoLocal.status,
                atrasoSobCarga = null,
                latenciaLivre = null,
            ),
        )
    }

    @Test
    fun `Assist remoto nao inventa acao local quando NDS nao recomenda uma`() {
        assertEquals(
            emptyList<String>(),
            passosAssistSeguro(
                resultado = resultadoLocal,
                recomendacaoNds = emptyList(),
                atrasoSobCarga = null,
                latenciaLivre = null,
                usarApenasRecomendacaoNds = true,
            ),
        )
    }

    @Test
    fun `Assist remoto nao exibe CTA para ferramenta local`() {
        assertFalse(deveExibirCtaMelhoriaLocal(veioDoNds = true))
        assertTrue(deveExibirCtaMelhoriaLocal(veioDoNds = false))
    }

    @Test
    fun `resposta remota sem texto usa fallback seguro`() {
        assertEquals(
            "Sua conexão apresenta sinais de instabilidade",
            tituloAssistVindoDoNds(
                tituloNds = null,
                objetivo = resultadoLocal.objetivo,
                status = resultadoLocal.status,
            ),
        )
    }
}
