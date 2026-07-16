package io.signallq.app.ui.component

import io.signallq.app.ui.screen.AnalisadorState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Follow-up Lia (PR #1013) — o header do sheet aninhado "Analisar meu problema com IA" precisa
 * diferenciar o laudo disparado automaticamente pela tela 1a (`problemaRelatado == null`) da
 * análise que o usuário pediu por sintoma (`problemaRelatado != null`), sem convidar de novo a
 * descrever um problema que já tem resposta.
 */
class AnaliseDetalhadaBottomSheetTest {
    @Test
    fun `resultado sem problema relatado usa copy de diagnostico geral`() {
        val estado = AnalisadorState.Resultado(texto = "laudo", origem = "ia", problemaRelatado = null)

        val (titulo, subtitulo) = headerAnaliseDetalhada(estado)

        assertEquals("Diagnóstico geral da sua conexão", titulo)
        assertEquals(
            "Baseado no teste que você acabou de rodar. Quer detalhar um problema específico?",
            subtitulo,
        )
    }

    @Test
    fun `resultado com problema relatado usa copy especifica com o sintoma`() {
        val estado = AnalisadorState.Resultado(texto = "laudo", origem = "ia", problemaRelatado = "Quedas constantes")

        val (titulo, subtitulo) = headerAnaliseDetalhada(estado)

        assertEquals("Análise do seu problema", titulo)
        assertEquals("Diagnóstico específico para \"Quedas constantes\".", subtitulo)
    }

    @Test
    fun `estados fora de resultado mantem copy original de convite a descrever o problema`() {
        val (tituloInativo, subtituloInativo) = headerAnaliseDetalhada(AnalisadorState.Inativo)
        val (tituloAnalisando, subtituloAnalisando) = headerAnaliseDetalhada(AnalisadorState.Analisando)
        val (tituloErro, subtituloErro) = headerAnaliseDetalhada(AnalisadorState.Erro("falhou"))

        val esperadoTitulo = "Analisar meu problema com IA"
        val esperadoSubtitulo = "Descreva o que está acontecendo pra receber um diagnóstico específico."

        assertEquals(esperadoTitulo, tituloInativo)
        assertEquals(esperadoSubtitulo, subtituloInativo)
        assertEquals(esperadoTitulo, tituloAnalisando)
        assertEquals(esperadoSubtitulo, subtituloAnalisando)
        assertEquals(esperadoTitulo, tituloErro)
        assertEquals(esperadoSubtitulo, subtituloErro)
    }
}
