package io.signallq.app.ui.screen

import androidx.compose.runtime.saveable.SaverScope
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Testes dos savers do fluxo guiado — issue #1704 (2.0.09b), épico #1647.
 *
 * A persistência entrou para que girar o aparelho ou sofrer process death no meio de uma análise
 * pare de apagar objetivo, passo e respostas (spec §4.3/§8.1, "continuar análise"). Um saver
 * silenciosamente errado é pior que ausência de saver: em vez de perder a análise, ela **volta
 * errada** — e nada na UI denuncia.
 *
 * Estes testes são puros (sem Compose, sem Robolectric): o `SaverScope` é a única dependência e é
 * um `fun interface` de uma linha.
 */
class DiagnosticoGuiadoSaversTest {
    /** `canBeSaved` só importa para o autoSaver; nossos savers reduzem a String/List<Int>. */
    private val scope = SaverScope { true }

    private fun salvarObjetivo(valor: ObjetivoDiagnostico?): String = with(SaverObjetivoDiagnostico) { scope.save(valor) }!!

    private fun salvarRespostas(valor: List<Int?>): List<Int> = with(SaverRespostas) { scope.save(valor) }!!

    // ─── Objetivo ──────────────────────────────────────────────────────────────

    @Test
    fun `todos os objetivos sobrevivem ao ciclo salvar-restaurar`() {
        ObjetivoDiagnostico.entries.forEach { objetivo ->
            val restaurado = SaverObjetivoDiagnostico.restore(salvarObjetivo(objetivo))
            assertEquals("objetivo $objetivo nao sobreviveu ao ciclo", objetivo, restaurado)
        }
    }

    @Test
    fun `objetivo e persistido pelo nome e nao pelo ordinal`() {
        // Mutante que este teste mata: `save = { it?.ordinal?.toString() ?: "" }`.
        // Ordinal quebra em silêncio quando alguém acrescenta ou reordena um objetivo no enum —
        // a análise salva antes da atualização volta apontando para OUTRO problema, sem erro
        // nenhum. Asserir o nome literal trava o contrato de serialização.
        assertEquals("JOGOS_COM_LAG", salvarObjetivo(ObjetivoDiagnostico.JOGOS_COM_LAG))
        assertEquals(
            ObjetivoDiagnostico.JOGOS_COM_LAG,
            SaverObjetivoDiagnostico.restore("JOGOS_COM_LAG"),
        )
    }

    @Test
    fun `objetivo nulo vai e volta como nulo`() {
        // "Nenhum objetivo escolhido ainda" é estado legítimo do fluxo (tela de lista), não
        // ausência de dado — precisa sobreviver à rotação como nulo, não virar o primeiro enum.
        assertEquals("", salvarObjetivo(null))
        assertNull(SaverObjetivoDiagnostico.restore(""))
    }

    @Test
    fun `objetivo desconhecido restaura como nulo em vez de estourar`() {
        // Cenário real: o usuário atualiza o app com uma análise salva cujo objetivo foi removido
        // do enum. Voltar `null` devolve o usuário à lista de objetivos; lançar derrubaria o app
        // na restauração, que é o pior momento possível.
        assertNull(SaverObjetivoDiagnostico.restore("OBJETIVO_QUE_NAO_EXISTE_MAIS"))
    }

    // ─── Respostas ─────────────────────────────────────────────────────────────

    @Test
    fun `respostas preenchidas sobrevivem preservando ordem`() {
        val respostas = listOf<Int?>(2, 0, 1)
        assertEquals(respostas, SaverRespostas.restore(salvarRespostas(respostas)))
    }

    @Test
    fun `resposta nao respondida sobrevive como nulo e nao como zero`() {
        // Mutante que este teste mata: usar 0 como sentinela em vez de -1. Zero é um índice de
        // opção VÁLIDO (a primeira alternativa de qualquer pergunta), então confundir os dois
        // faria uma pergunta em branco reaparecer respondida com a primeira opção — resposta que
        // o usuário nunca deu, alimentando o motor de diagnóstico.
        val comLacuna = listOf(2, null, 0)
        val salvo = salvarRespostas(comLacuna)
        assertEquals(listOf(2, RESPOSTA_NAO_RESPONDIDA, 0), salvo)
        assertEquals(comLacuna, SaverRespostas.restore(salvo))
    }

    @Test
    fun `sentinela e negativa e nao colide com indice de opcao valido`() {
        // Índice de opção é sempre >= 0; a sentinela precisa estar fora desse domínio.
        assert(RESPOSTA_NAO_RESPONDIDA < 0) { "sentinela precisa ser negativa para nao colidir" }
    }

    @Test
    fun `lista vazia sobrevive`() {
        assertEquals(emptyList<Int?>(), SaverRespostas.restore(salvarRespostas(emptyList())))
    }

    @Test
    fun `lista so de nulos sobrevive inteira`() {
        val soNulos = listOf<Int?>(null, null, null)
        assertEquals(soNulos, SaverRespostas.restore(salvarRespostas(soNulos)))
    }
}
