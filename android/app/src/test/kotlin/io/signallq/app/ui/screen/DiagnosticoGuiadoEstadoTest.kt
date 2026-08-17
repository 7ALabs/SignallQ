package io.signallq.app.ui.screen

import androidx.compose.runtime.saveable.SaverScope
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.core.diagnostico.PerguntasDiagnosticoGuiado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Estado do fluxo guiado — issue #1704, correção do bloqueio B3 da PR #1708.
 *
 * A PR anterior foi reprovada por persistir **quatro campos independentes**: o saver do objetivo
 * devolvia `null` para nome de enum desconhecido enquanto passo, respostas e a flag de resultado
 * restauravam intactos, quebrando o invariante e produzindo ou `IndexOutOfBounds` em
 * `perguntas[passo]`, ou resultado calculado com respostas de outra jornada.
 *
 * Aqui o estado é **um objeto com um saver**. Estes testes travam as duas propriedades que fazem
 * isso valer a pena: o ciclo salvar/restaurar preserva, e restauração incoerente **sana** em vez
 * de propagar lixo.
 */
class DiagnosticoGuiadoEstadoTest {
    private val scope = SaverScope { true }

    private fun salvar(estado: DiagnosticoGuiadoEstado): List<String> =
        with(DiagnosticoGuiadoEstado.Saver) { scope.save(estado) }!!

    private fun cicloCompleto(estado: DiagnosticoGuiadoEstado): DiagnosticoGuiadoEstado? =
        DiagnosticoGuiadoEstado.Saver.restore(salvar(estado))

    private val jogos = ObjetivoDiagnostico.JOGOS_COM_LAG
    private val perguntasJogos = PerguntasDiagnosticoGuiado.perguntas(jogos).size

    // ─── Ciclo salvar/restaurar ────────────────────────────────────────────────

    @Test
    fun `estado inicial sobrevive`() {
        assertEquals(DiagnosticoGuiadoEstado(), cicloCompleto(DiagnosticoGuiadoEstado()))
    }

    @Test
    fun `estado completo sobrevive com objetivo passo respostas e pilha`() {
        val estado =
            DiagnosticoGuiadoEstado(
                objetivo = jogos,
                passo = 1,
                respostas = listOf(0, null),
                pilha = listOf(DiagnosticoGuiadoRota.Analise, DiagnosticoGuiadoRota.Resultado),
            )
        assertEquals(estado, cicloCompleto(estado))
    }

    @Test
    fun `objetivo e persistido pelo nome e nao pelo ordinal`() {
        // Mutante: `estado.objetivo?.ordinal?.toString()`. Ordinal quebra em silêncio quando
        // alguém reordena ou acrescenta um objetivo no enum — a análise salva antes da
        // atualização volta apontando para OUTRO problema, sem erro nenhum.
        assertEquals("JOGOS_COM_LAG", salvar(DiagnosticoGuiadoEstado(objetivo = jogos))[0])
    }

    @Test
    fun `resposta nao respondida sobrevive como nulo e nao como zero`() {
        // Mutante: sentinela `0` no lugar de `-1`. Zero é índice de opção VÁLIDO (a primeira
        // alternativa), então confundir os dois faria uma pergunta em branco reaparecer respondida
        // com a primeira opção — resposta que o usuário nunca deu, alimentando o motor.
        val estado = DiagnosticoGuiadoEstado(objetivo = jogos, passo = 1, respostas = listOf(0, null))
        assertEquals(listOf(0, null), cicloCompleto(estado)?.respostas)
    }

    @Test
    fun `pilha ciclica sobrevive com rota repetida`() {
        // O fluxo é cíclico por natureza (Resultado -> Orientacao -> Reteste -> Comparacao ->
        // Orientacao de novo). É exatamente o que a pilha set-like de AppShellOverlay não
        // representaria — motivo pelo qual estas rotas não entraram naquele enum.
        val estado =
            DiagnosticoGuiadoEstado(
                objetivo = jogos,
                passo = perguntasJogos,
                pilha =
                    listOf(
                        DiagnosticoGuiadoRota.Resultado,
                        DiagnosticoGuiadoRota.Orientacao,
                        DiagnosticoGuiadoRota.Reteste,
                        DiagnosticoGuiadoRota.Comparacao,
                        DiagnosticoGuiadoRota.Orientacao,
                    ),
            )
        assertEquals(estado.pilha, cicloCompleto(estado)?.pilha)
    }

    // ─── Saneamento: o bloqueio B3 da PR #1708 ─────────────────────────────────

    @Test
    fun `objetivo desconhecido sana o estado inteiro em vez de restaurar pedacos`() {
        // ESTE é o teste que representa o B3. Cenário: app atualizado, objetivo removido do enum,
        // estado salvo aponta para nome inexistente. Antes, com 4 slots, o objetivo voltava null e
        // passo/respostas/resultado voltavam intactos — e escolher um objetivo novo caía direto no
        // resultado com respostas de outra jornada, ou estourava em `perguntas[passo]`.
        val salvoComObjetivoExtinto = listOf("OBJETIVO_QUE_NAO_EXISTE_MAIS", "2", "0,1", "Resultado")
        val restaurado = DiagnosticoGuiadoEstado.Saver.restore(salvoComObjetivoExtinto)

        assertEquals(
            "estado inteiro tem que voltar ao inicio, nao so o objetivo",
            DiagnosticoGuiadoEstado(),
            restaurado,
        )
    }

    @Test
    fun `passo alem do roteiro do objetivo sana`() {
        // O outro caminho para `perguntas[passo]` estourar: passo maior que o número de perguntas.
        val salvo = listOf(jogos.name, (perguntasJogos + 5).toString(), "", "")
        assertEquals(DiagnosticoGuiadoEstado(), DiagnosticoGuiadoEstado.Saver.restore(salvo))
    }

    @Test
    fun `pilha sem objetivo sana`() {
        // Rota empilhada sem objetivo escolhido é incoerente por definição — não há roteiro.
        val salvo = listOf("", "0", "", "Resultado")
        assertEquals(DiagnosticoGuiadoEstado(), DiagnosticoGuiadoEstado.Saver.restore(salvo))
    }

    @Test
    fun `passo no fim do roteiro e coerente e nao sana`() {
        // `passo == perguntas.size` significa "roteiro terminado" — estado transitório legítimo
        // entre responder a última pergunta e empilhar o Resultado. Um mutante que usasse `<` no
        // lugar de `<=` apagaria a jornada exatamente no momento de concluir.
        val estado = DiagnosticoGuiadoEstado(objetivo = jogos, passo = perguntasJogos)
        assertTrue(estado.coerente)
        assertEquals(estado, cicloCompleto(estado))
    }

    @Test
    fun `rota desconhecida na pilha e descartada sem derrubar o resto`() {
        // Rota removida numa versão futura não pode estourar na restauração. O que sobra ainda
        // precisa ser coerente — aqui sobra `Resultado`, com objetivo válido.
        val salvo = listOf(jogos.name, "0", "", "Resultado,ROTA_EXTINTA")
        assertEquals(
            listOf(DiagnosticoGuiadoRota.Resultado),
            DiagnosticoGuiadoEstado.Saver.restore(salvo)?.pilha,
        )
    }

    // ─── Coerência ─────────────────────────────────────────────────────────────

    @Test
    fun `sem objetivo tudo mais tem que estar zerado`() {
        assertTrue(DiagnosticoGuiadoEstado().coerente)
        assertFalse(DiagnosticoGuiadoEstado(passo = 1).coerente)
        assertFalse(DiagnosticoGuiadoEstado(respostas = listOf(0)).coerente)
        assertFalse(DiagnosticoGuiadoEstado(pilha = listOf(DiagnosticoGuiadoRota.Resultado)).coerente)
    }

    // ─── Navegação interna ─────────────────────────────────────────────────────

    @Test
    fun `recuar desempilha a rota antes de mexer no passo`() {
        val estado =
            DiagnosticoGuiadoEstado(
                objetivo = jogos,
                passo = 1,
                pilha = listOf(DiagnosticoGuiadoRota.Resultado),
            )
        val recuado = estado.recuar()
        assertEquals(emptyList<DiagnosticoGuiadoRota>(), recuado?.pilha)
        assertEquals("passo nao pode mudar enquanto ha rota para desempilhar", 1, recuado?.passo)
    }

    @Test
    fun `recuar sem pilha volta um passo do roteiro`() {
        val estado = DiagnosticoGuiadoEstado(objetivo = jogos, passo = 2)
        assertEquals(1, estado.recuar()?.passo)
    }

    @Test
    fun `recuar do primeiro passo volta a lista de objetivos zerando tudo`() {
        // Preserva o comportamento do `voltarUmPasso()` atual: soltar o objetivo zera passo e
        // respostas junto — que é justamente o invariante por construção que a PR #1708 quebrou
        // ao persistir os campos separados.
        val estado = DiagnosticoGuiadoEstado(objetivo = jogos, passo = 0, respostas = listOf(0))
        assertEquals(DiagnosticoGuiadoEstado(), estado.recuar())
    }

    @Test
    fun `recuar do estado inicial devolve nulo para o shell fechar o overlay`() {
        // `null` é o sinal que `RegistrarBackDoOverlay` traduz em `false` — "não consumi, pode
        // fechar o overlay inteiro". Sem isso o usuário ficaria preso dentro do fluxo.
        assertNull(DiagnosticoGuiadoEstado().recuar())
    }

    @Test
    fun `empilhar permite repeticao porque o fluxo e ciclico`() {
        val estado =
            DiagnosticoGuiadoEstado(objetivo = jogos)
                .empilhar(DiagnosticoGuiadoRota.Orientacao)
                .empilhar(DiagnosticoGuiadoRota.Reteste)
                .empilhar(DiagnosticoGuiadoRota.Orientacao)
        assertEquals(3, estado.pilha.size)
        assertEquals(DiagnosticoGuiadoRota.Orientacao, estado.rotaAtual)
    }

    @Test
    fun `sao cinco rotas e nao sete`() {
        // Trava a decisão de escopo: `insufficient` e `recoverable-error` são variações dentro das
        // telas, não destinos (spec §9 + COVERAGE.md do protótipo). Se alguém acrescentar uma
        // delas como rota, este teste obriga a rediscutir em vez de deixar passar.
        assertEquals(5, DiagnosticoGuiadoRota.entries.size)
        assertEquals(
            listOf("Analise", "Resultado", "Orientacao", "Reteste", "Comparacao"),
            DiagnosticoGuiadoRota.entries.map { it.name },
        )
    }
}
