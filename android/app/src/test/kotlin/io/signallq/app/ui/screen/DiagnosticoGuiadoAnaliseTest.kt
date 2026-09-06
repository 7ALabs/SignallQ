package io.signallq.app.ui.screen

import io.mockk.mockk
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.FaseSpeedtest
import io.signallq.app.feature.speedtest.ResultadoSpeedtest
import io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes de [estadoAnaliseGuiada] — GH#1704 parte 4/4, rota `Analise` (spec 2.0 §8.5).
 *
 * Os testes que **afirmam** matar um mutante trazem o mutante no comentário, e cada um desses foi
 * rodado de fato antes de a afirmação entrar no arquivo — padrão que esta issue adotou depois dos
 * bloqueios das PRs #1708–#1713. Um dos mutantes propostos sobreviveu e a afirmação
 * correspondente foi apagada, não reescrita (ver `executando vence...` abaixo). Os demais testes
 * são de mapeamento simples e não afirmam nada sobre mutação.
 *
 * O `resultado` é um `mockk` sem stub de propriedade nenhuma de propósito: a função só o testa
 * contra `null` e nunca lê um campo. Um mock relaxado provaria menos — se alguém passar a ler
 * `resultado.status` aqui dentro, este teste estoura com `MockKException` em vez de passar em
 * silêncio, que é exatamente o aviso que se quer.
 */
class DiagnosticoGuiadoAnaliseTest {
    private val resultado = mockk<ResultadoSpeedtest>()

    private fun snapshot(
        estado: EstadoExecucaoSpeedtest,
        resultado: ResultadoSpeedtest? = null,
        erroMensagem: String? = null,
        fase: FaseSpeedtest = FaseSpeedtest.idle,
        progressoGlobal: Float = 0f,
    ) = SnapshotExecucaoSpeedtest(
        estado = estado,
        progressoPercentual = (progressoGlobal * 100).toInt(),
        resultado = resultado,
        erroMensagem = erroMensagem,
        faseAtual = fase,
        progressoGlobal = progressoGlobal,
    )

    // Mutante: mover a cláusula do `resultado` para DEPOIS da cláusula de `erro`.
    // Rodado — o teste falha (devolve `Falhou`), e a falha é a de produção: uma análise concluída
    // com erro residual voltaria a medir sozinha em vez de mostrar a conclusão.
    @Test
    fun `resultado presente vence estado de erro`() {
        val estado = estadoAnaliseGuiada(snapshot(EstadoExecucaoSpeedtest.erro, resultado, "falhou"))

        assertEquals(EstadoAnaliseGuiada.Concluida, estado)
    }

    // Mutante: remover a cláusula do `resultado` inteira.
    // Rodado — falha com `NaoIniciada` (e derruba junto o teste acima). Este é o caso que o KDoc
    // cita: `cancelar()` republica `estado = idle` preservando o `resultado`, e sem a cláusula o
    // fluxo dispararia uma segunda medição em cima de uma já concluída.
    @Test
    fun `resultado presente com estado idle e conclusao, nao inicio`() {
        val estado = estadoAnaliseGuiada(snapshot(EstadoExecucaoSpeedtest.idle, resultado))

        assertEquals(EstadoAnaliseGuiada.Concluida, estado)
    }

    // Este teste cobre o MAPEAMENTO de `executando` (progresso + etapa), não uma ordem de
    // cláusula. A primeira versão dele afirmava matar o mutante "trocar `executando` com `erro`";
    // rodei o mutante e ele **sobreviveu** — as duas cláusulas testam `estado` com valores
    // mutuamente exclusivos, então a ordem entre elas não é observável por teste nenhum. A
    // afirmação foi removida daqui e do KDoc da função em vez de o teste ser "consertado": não
    // havia defeito, havia uma garantia inventada.
    //
    // O `erroMensagem` residual continua no cenário porque descreve o snapshot real que o
    // executor publica ao reiniciar depois de uma falha — só não prova nada sobre ordenação.
    @Test
    fun `executando vence mensagem de erro residual`() {
        val estado =
            estadoAnaliseGuiada(
                snapshot(
                    estado = EstadoExecucaoSpeedtest.executando,
                    erroMensagem = "residual de antes",
                    fase = FaseSpeedtest.download,
                    progressoGlobal = 0.5f,
                ),
            )

        assertEquals(EstadoAnaliseGuiada.EmAndamento(0.5f, "Medindo a velocidade de recebimento"), estado)
    }

    // Mutante: trocar `snapshot.erroMensagem ?: MENSAGEM_FALHA_GENERICA` por `snapshot.erroMensagem!!`.
    // Rodado — falha com NPE. O executor publica `estado = erro` com `erroMensagem = null` em pelo
    // menos um caminho (o reset de `cancelar()` zera a mensagem), então o `!!` é alcançável.
    @Test
    fun `erro sem mensagem cai no texto generico`() {
        val estado = estadoAnaliseGuiada(snapshot(EstadoExecucaoSpeedtest.erro))

        assertEquals(EstadoAnaliseGuiada.Falhou(MENSAGEM_FALHA_GENERICA), estado)
    }

    @Test
    fun `erro com mensagem preserva o texto do executor`() {
        val estado = estadoAnaliseGuiada(snapshot(EstadoExecucaoSpeedtest.erro, erroMensagem = "sem conexão"))

        assertEquals(EstadoAnaliseGuiada.Falhou("sem conexão"), estado)
    }

    @Test
    fun `idle sem resultado pede o inicio da medicao`() {
        val estado = estadoAnaliseGuiada(snapshot(EstadoExecucaoSpeedtest.idle))

        assertEquals(EstadoAnaliseGuiada.NaoIniciada, estado)
    }

    // Mutante: remover o `.coerceIn(0f, 1f)`.
    // Rodado — falha com `progresso = 1.4f`. O executor calcula `progresso / 100f` a partir de um
    // Int já limitado, então hoje não estoura; o `coerceIn` protege a rota de uma mudança lá
    // dentro, e sem o teste ninguém saberia que a proteção sumiu.
    @Test
    fun `progresso fora da faixa e limitado`() {
        val estado =
            estadoAnaliseGuiada(
                snapshot(EstadoExecucaoSpeedtest.executando, progressoGlobal = 1.4f),
            )

        assertEquals(1f, (estado as EstadoAnaliseGuiada.EmAndamento).progresso, 0.0001f)
    }

    // §8.5: "etapa atual em linguagem humana". Um `when` que devolvesse o `fase.name` passaria em
    // qualquer teste que só checasse "o texto não é vazio" — por isso a asserção é sobre a
    // AUSÊNCIA do jargão, que é o que a regra de fato exige.
    @Test
    fun `nenhuma etapa expoe jargao de protocolo`() {
        val jargao = listOf("ping", "download", "upload", "idle")

        FaseSpeedtest.entries.forEach { fase ->
            val texto = etapaEmLinguagemHumana(fase).lowercase()
            jargao.forEach { termo ->
                assertTrue("fase $fase vazou o termo técnico '$termo': \"$texto\"", !texto.contains(termo))
            }
        }
    }

    @Test
    fun `cada fase tem um texto proprio`() {
        val textos = FaseSpeedtest.entries.map { etapaEmLinguagemHumana(it) }

        assertEquals(FaseSpeedtest.entries.size, textos.toSet().size)
    }
}
