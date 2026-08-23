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
                passo = perguntasJogos - 1,
                respostas = List(perguntasJogos) { 0 },
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
    fun `passo igual ao tamanho do roteiro e INCOERENTE e sana`() {
        // Correção do B4: a versão anterior desta PR afirmava que `passo == perguntas.size` era
        // "roteiro terminado, estado transitório" e o aceitava. Era invenção — a tela faz
        // `if (passo < perguntas.size - 1) passo += 1 else mostrarResultado = true`
        // (DiagnosticoGuiadoScreen.kt:300), então o passo TETO em `size - 1` e esse estado nunca
        // é produzido. Sancioná-lo era perigoso: `recuar()` desempilha a rota sem mexer no passo,
        // e um consumidor confiando naquele limite cairia em `perguntas[perguntas.size]`.
        val estado = DiagnosticoGuiadoEstado(objetivo = jogos, passo = perguntasJogos)
        assertFalse(estado.coerente)
        assertEquals(DiagnosticoGuiadoEstado(), cicloCompleto(estado))
    }

    @Test
    fun `ultimo passo real do roteiro e coerente`() {
        // O teto verdadeiro: `perguntas.size - 1`, com a lista de respostas cheia.
        val estado =
            DiagnosticoGuiadoEstado(
                objetivo = jogos,
                passo = perguntasJogos - 1,
                respostas = List(perguntasJogos) { 0 },
            )
        assertTrue(estado.coerente)
        assertEquals(estado, cicloCompleto(estado))
    }

    // ─── B1: clauses de `coerente` que não tinham cobertura ────────────────────

    @Test
    fun `passo negativo sana, inclusive no valor de fronteira`() {
        // Mutante que este teste mata: remover `passo >= 0 &&` de `coerente`. O caso é alcançável
        // pelo próprio restore — `toIntOrNull()` aceita "-1".
        //
        // `-1` é o valor que importa, e descobri isso rodando o mutante: com `-3`, o clause
        // `respostas.size <= passo + 1` já reprova sozinho (`0 <= -2` é falso), então o mutante
        // sobrevivia. Em `-1` os dois outros clauses passam (`-1 < 2` e `0 <= 0`) e só a guarda de
        // não-negatividade segura. Testar longe da fronteira dava a impressão de cobertura sem ter.
        assertFalse("passo = -1 e a fronteira", DiagnosticoGuiadoEstado(objetivo = jogos, passo = -1).coerente)
        assertFalse(DiagnosticoGuiadoEstado(objetivo = jogos, passo = -3).coerente)
        assertEquals(
            DiagnosticoGuiadoEstado(),
            DiagnosticoGuiadoEstado.Saver.restore(listOf(jogos.name, "-1", "", "")),
        )
    }

    @Test
    fun `respostas acima do roteiro sanam`() {
        // Cobre o teto REVERTIDO. Sem isto, o mutante `respostas.size <= 99` volta a sobreviver e
        // o bloqueio B1 reabre no mesmo lugar.
        assertFalse(DiagnosticoGuiadoEstado(objetivo = jogos, passo = 1, respostas = listOf(0, 1, 2)).coerente)
        assertEquals(
            DiagnosticoGuiadoEstado(),
            DiagnosticoGuiadoEstado.Saver.restore(listOf(jogos.name, "1", "0,1,2", "")),
        )
    }

    @Test
    fun `voltar uma pergunta com o roteiro respondido preserva a jornada`() {
        // O teste que faltava desde a primeira rodada, e que pegou a regressão do B5.
        //
        // A resposta selecionada é preservada ao voltar para uma pergunta anterior.
        // (DiagnosticoGuiadoScreen.kt:478), então o usuário só chega ao último passo tendo
        // respondido — e voltar PRESERVA as respostas de propósito
        // (`respostas.getOrNull(passo)` re-seleciona a anterior).
        //
        // Uma versão intermediária desta PR usou `respostas.size <= passo + 1` como invariante.
        // Com ele, este cenário virava incoerente e `saneado()` apagava a jornada inteira se o
        // processo morresse logo depois: o usuário responde tudo, volta uma para revisar, o app
        // vai para segundo plano e perde o diagnóstico. Nada impede reintroduzir um bound cruzado
        // exceto este teste.
        val respondido = DiagnosticoGuiadoEstado(objetivo = jogos, passo = 1, respostas = listOf(0, 1))
        assertTrue("roteiro respondido e coerente", respondido.coerente)

        val voltouUma = respondido.recuar()
        assertEquals(0, voltouUma?.passo)
        assertEquals("respostas preservadas ao voltar", listOf(0, 1), voltouUma?.respostas)
        assertTrue("recuar a partir de coerente tem que continuar coerente", voltouUma!!.coerente)
        assertEquals("e tem que sobreviver ao ciclo de save", voltouUma, cicloCompleto(voltouUma))
    }

    @Test
    fun `respostas ate o passo corrente sao coerentes`() {
        // O lado positivo do mesmo invariante, para o clause não virar bloqueio universal.
        assertTrue(DiagnosticoGuiadoEstado(objetivo = jogos, passo = 0, respostas = listOf(0)).coerente)
        assertTrue(DiagnosticoGuiadoEstado(objetivo = jogos, passo = 1, respostas = listOf(0, 1)).coerente)
    }

    @Test
    fun `indice de resposta negativo vira nulo e nao indice valido`() {
        // Subconjunto grátis da validação de conteúdo (B2): `takeIf { it >= 0 }` cobre o sentinela
        // e qualquer negativo. Antes, só `-1` era tratado; `-7` atravessaria como índice de opção.
        val restaurado = DiagnosticoGuiadoEstado.Saver.restore(listOf(jogos.name, "1", "0,-7", ""))
        assertEquals(listOf(0, null), restaurado?.respostas)
    }

    @Test
    fun `rota desconhecida na pilha sana o estado inteiro`() {
        // Correção do B3: a versão anterior descartava a rota desconhecida em silêncio e mantinha
        // o resto — política OPOSTA à do objetivo, no mesmo saver, para o mesmo evento (evolução
        // de enum entre versões). E a permissiva era a errada: `"Analise,Resultado"` com
        // `Resultado` renomeado restaurava `[Analise]`, e o usuário que estava lendo o resultado
        // acordava na medição, que redispara sozinha. Agora é fail-closed, igual ao objetivo.
        val salvo = listOf(jogos.name, "0", "", "Resultado,ROTA_EXTINTA")
        assertEquals(DiagnosticoGuiadoEstado(), DiagnosticoGuiadoEstado.Saver.restore(salvo))
    }

    @Test
    fun `rota desconhecida no MEIO da pilha tambem sana`() {
        // O caso pior do B3: descarte silencioso no meio produzia um histórico de navegação que
        // nunca existiu, com `recuar()` andando por ele.
        val salvo = listOf(jogos.name, "1", "0,1", "Resultado,X,Comparacao")
        assertEquals(DiagnosticoGuiadoEstado(), DiagnosticoGuiadoEstado.Saver.restore(salvo))
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
        assertEquals(6, DiagnosticoGuiadoRota.entries.size)
        assertEquals(
            listOf("Processando", "Analise", "Resultado", "Orientacao", "Reteste", "Comparacao"),
            DiagnosticoGuiadoRota.entries.map { it.name },
        )
    }
}
