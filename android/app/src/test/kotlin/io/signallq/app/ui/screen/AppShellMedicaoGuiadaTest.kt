package io.signallq.app.ui.screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import io.signallq.app.core.network.connectivity.ConnectivityDiagnosisEngine
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.FaseSpeedtest
import io.signallq.app.feature.speedtest.ModoSpeedtest
import io.signallq.app.feature.speedtest.ResultadoSpeedtest
import io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes de [rememberMedicaoGuiada] — GH#1704, rodada 2 da PR #1719.
 *
 * Existem porque dois mutantes sobreviviam à suíte **inteira** do `:app` enquanto este estado
 * morava dentro de `AppShell.kt` (bloqueio B3 de Caio): remover o reset em `onCancelar`, e remover
 * a supressão do `VelocidadeScreen`. Não era teste difícil — era código no lugar errado.
 *
 * O snapshot é dirigido por `mutableStateOf` e **muda ao longo do teste**. Essa é a diferença que
 * o bloqueio B1 apontou: estado estático não exercita nada que exista para tratar transição.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppShellMedicaoGuiadaTest {
    @get:Rule val composeRule = createComposeRule()

    private val resultado = mockk<ResultadoSpeedtest>()

    private fun snapshot(
        estado: EstadoExecucaoSpeedtest,
        resultado: ResultadoSpeedtest? = null,
        fase: FaseSpeedtest = FaseSpeedtest.idle,
        progressoGlobal: Float = 0f,
    ) = SnapshotExecucaoSpeedtest(
        estado = estado,
        progressoPercentual = (progressoGlobal * 100).toInt(),
        resultado = resultado,
        erroMensagem = null,
        faseAtual = fase,
        progressoGlobal = progressoGlobal,
    )

    private class Cenario {
        var snapshot by
            mutableStateOf(
                SnapshotExecucaoSpeedtest(EstadoExecucaoSpeedtest.idle, 0, null, null),
            )
        var ui: MedicaoGuiadaUi? = null
        val modosPedidos = mutableListOf<ModoSpeedtest>()
        var cancelamentos = 0
    }

    private fun montar(limiteMs: Long = LIMITE_PARA_INICIAR_MS): Cenario {
        composeRule.mainClock.autoAdvance = false
        val cenario = Cenario()
        composeRule.setContent {
            cenario.ui =
                rememberMedicaoGuiada(
                    snapshot = cenario.snapshot,
                    onNovoTeste = { cenario.modosPedidos += it },
                    onCancelarTeste = { cenario.cancelamentos += 1 },
                    limiteParaIniciarMs = limiteMs,
                )
        }
        return cenario
    }

    /**
     * [MedicaoGuiadaUi] é imutável e reconstruída a cada recomposição — ler `cenario.ui` sem antes
     * deixar a recomposição acontecer devolve o valor do frame anterior. Com `autoAdvance = false`
     * o `runOnIdle` não bombeia frame nenhum. Foi exatamente esse o erro da primeira versão destes
     * testes: três deles falharam lendo estado velho, não porque o código estivesse errado.
     */
    private fun Cenario.atual(): MedicaoGuiadaUi {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        return ui!!
    }

    private fun Cenario.invocar(acao: (MedicaoGuiadaUi) -> Unit) {
        composeRule.runOnUiThread { acao(ui!!) }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
    }

    private fun Cenario.publicar(novo: SnapshotExecucaoSpeedtest) {
        composeRule.runOnUiThread { snapshot = novo }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
    }

    @Test
    fun `sem pedido o shell reage normalmente e o estado nao e da analise`() {
        val cenario = montar()

        assertFalse(cenario.atual().suprimeReacoesDoShell)
        assertEquals(EstadoAnaliseGuiada.NaoIniciada, cenario.atual().contrato.estado)
    }

    // Mutante que este teste mata: `suprimeReacoesDoShell` deixar de refletir `emCurso`.
    //
    // Ressalva honesta sobre o M5 do parecer: ele tem duas metades, e este teste mata **uma**.
    // A metade do ESTADO (o holder parar de sinalizar supressão) morre aqui. A metade do USO —
    // apagar `!medicaoGuiada.suprimeReacoesDoShell &&` da linha do `AnimatedVisibility` em
    // `AppShell.kt` — **continua sobrevivendo**: apliquei e rodei `:app:testDebugUnitTest`
    // completo, BUILD SUCCESSFUL. Cobrir isso exigiria compor o `AppShell` inteiro, que tem 30+
    // parâmetros e nenhum harness. Está declarado na PR como resíduo, não como resolvido.
    @Test
    fun `pedir medicao suprime as reacoes do shell e dispara o modo fast`() {
        val cenario = montar()

        cenario.invocar { it.contrato.onIniciar() }

        assertTrue(cenario.atual().suprimeReacoesDoShell)
        assertEquals(listOf(ModoSpeedtest.fast), cenario.modosPedidos)
    }

    // Mutante que este teste mata: remover `emCurso = false` de `MedicaoGuiada.cancelar` (M4 do
    // parecer). Sem isso o speedtest do shell fica silenciado para sempre após um cancelamento.
    @Test
    fun `cancelar devolve as reacoes ao shell e chama o executor`() {
        val cenario = montar()
        cenario.invocar { it.contrato.onIniciar() }

        cenario.invocar { it.contrato.onCancelar() }

        assertFalse(cenario.atual().suprimeReacoesDoShell)
        assertEquals(1, cenario.cancelamentos)
    }

    // Bloqueio B2. `onNovoTeste` não garante medição: `MainViewModel.reiniciarSuite` tem dois
    // `return` silenciosos alcançáveis daqui (`:960` execução em andamento, `:1003` Wi-Fi sem
    // internet). Sem limite, a rota exibia "Estou medindo sua conexão" para sempre.
    @Test
    fun `disparo recusado em silencio vira falha, nao espera eterna`() {
        val cenario = montar(limiteMs = 1_000L)
        cenario.invocar { it.contrato.onIniciar() }

        // O executor nunca sai de `idle` — exatamente o que os dois `return` produzem.
        composeRule.mainClock.advanceTimeBy(1_500L)

        assertEquals(EstadoAnaliseGuiada.Falhou(MENSAGEM_NAO_INICIOU), cenario.atual().contrato.estado)
    }

    // Mutante que este teste mata: remover a chamada a `registrarInicioObservado()`. Aí o limite
    // dispararia em cima de uma medição que ESTÁ correndo, e a pessoa veria "não consegui começar"
    // no meio de uma análise saudável.
    @Test
    fun `medicao que comeca a tempo nao cai no limite`() {
        val cenario = montar(limiteMs = 1_000L)
        cenario.invocar { it.contrato.onIniciar() }

        composeRule.mainClock.advanceTimeBy(200L)
        cenario.publicar(
            snapshot(EstadoExecucaoSpeedtest.executando, fase = FaseSpeedtest.ping, progressoGlobal = 0.3f),
        )
        composeRule.mainClock.advanceTimeBy(2_000L)

        assertEquals(
            EstadoAnaliseGuiada.EmAndamento(0.3f, "Verificando o tempo de resposta"),
            cenario.atual().contrato.estado,
        )
    }

    // A corrida do `Concluida` velho, agora fechada no holder e não na tela. Mutante: remover a
    // cláusula `medicao.aguardandoInicio ->` do `when` de `rememberMedicaoGuiada`.
    @Test
    fun `resultado anterior nao vaza como conclusao enquanto a medicao nova nao comeca`() {
        val cenario = montar(limiteMs = 10_000L)
        cenario.publicar(snapshot(EstadoExecucaoSpeedtest.concluido, resultado = resultado))
        assertEquals(EstadoAnaliseGuiada.Concluida, cenario.atual().contrato.estado)

        cenario.invocar { it.contrato.onIniciar() }

        assertEquals(
            EstadoAnaliseGuiada.EmAndamento(0f, "Preparando a análise"),
            cenario.atual().contrato.estado,
        )
    }

    // Mutante que este teste mata: `consumirConclusao()` devolver `true` sem checar `emCurso`.
    // O shell deixaria de empilhar `Overlay.ResultadoVelocidade` em teste NENHUM — a tela de
    // resultado do speedtest sumiria do app para todo mundo.
    @Test
    fun `conclusao de teste que nao e do fluxo guiado nao e consumida`() {
        val cenario = montar()

        assertFalse(cenario.atual().consumirConclusao())
    }

    @Test
    fun `conclusao do fluxo guiado e consumida uma vez so`() {
        val cenario = montar()
        cenario.invocar { it.contrato.onIniciar() }

        assertTrue(cenario.atual().consumirConclusao())
        assertFalse("segunda chamada não pode consumir de novo", cenario.atual().consumirConclusao())
        assertFalse(cenario.atual().suprimeReacoesDoShell)
    }

    // BLOQUEIO B7. `LaunchedEffect(snapshot.estado)` era um detector de TRANSIÇÃO fazendo papel de
    // detector de ESTADO: com o executor já `executando` no momento do pedido, a chave não mudava,
    // `registrarInicioObservado()` nunca era chamado, e o limite disparava em cima de uma medição
    // correndo. Caio executou o cenário e viu medição a 50% com a tela dizendo "não consegui
    // começar".
    //
    // Alcançável pelo cancelar-e-refazer rápido: `cancelar()` marca uma flag, o loop só desenrola
    // na fronteira de fase, e `execucaoSpeedtestEmAndamento` só libera no `finally` — então
    // `reiniciarSuite` retorna em `:960` E o snapshot já está `executando`.
    @Test
    fun `executor ja executando no momento do pedido nao cai no limite`() {
        val cenario = montar(limiteMs = 1_000L)
        cenario.publicar(
            snapshot(EstadoExecucaoSpeedtest.executando, fase = FaseSpeedtest.download, progressoGlobal = 0.5f),
        )

        cenario.invocar { it.contrato.onIniciar() }
        composeRule.mainClock.advanceTimeBy(2_000L)

        assertEquals(
            EstadoAnaliseGuiada.EmAndamento(0.5f, "Medindo a velocidade de recebimento"),
            cenario.atual().contrato.estado,
        )
    }

    // BLOQUEIO B8. `consumirConclusao()` não limpava `naoIniciou`, quebrando o invariante
    // `naoIniciou ⇒ emCurso`. Com a falsa falha ativa, a medição concluía de verdade, o shell
    // ENGOLIA o `Overlay.ResultadoVelocidade` (porque `consumirConclusao` devolvia `true`), e a
    // rota ficava travada em "Não consegui começar a análise" — resultado completo em mãos e
    // nenhuma tela para mostrá-lo.
    @Test
    fun `conclusao que chega depois da falha destrava a rota`() {
        val cenario = montar(limiteMs = 1_000L)
        cenario.invocar { it.contrato.onIniciar() }
        composeRule.mainClock.advanceTimeBy(1_500L)
        assertEquals(EstadoAnaliseGuiada.Falhou(MENSAGEM_NAO_INICIOU), cenario.atual().contrato.estado)

        cenario.publicar(snapshot(EstadoExecucaoSpeedtest.concluido, resultado = resultado))
        var consumiu = false
        cenario.invocar { consumiu = it.consumirConclusao() }

        assertTrue("a conclusão pertence ao fluxo guiado", consumiu)
        assertEquals(EstadoAnaliseGuiada.Concluida, cenario.atual().contrato.estado)
        assertFalse(cenario.atual().suprimeReacoesDoShell)
    }

    // BLOQUEIO B6. O limite empatava com `GLOBAL_TIMEOUT_MS_DEFAULT` do pré-voo que ele deveria
    // estar esperando — dois literais `8_000L` iguais em módulos diferentes, por acidente. Este
    // teste trava a derivação: se alguém voltar a escolher um número solto, ele reprova.
    @Test
    fun `o limite de inicio e maior que o teto do pre-voo`() {
        assertTrue(
            "o limite ($LIMITE_PARA_INICIAR_MS ms) precisa exceder o timeout global do " +
                "ConnectivityDiagnosisEngine (${ConnectivityDiagnosisEngine.GLOBAL_TIMEOUT_MS_DEFAULT} ms), " +
                "que roda ANTES de o executor publicar `executando`",
            LIMITE_PARA_INICIAR_MS > ConnectivityDiagnosisEngine.GLOBAL_TIMEOUT_MS_DEFAULT,
        )
    }

    // RESSALVA RS7. As REGRAS das leituras do shell, testáveis sem compor o `AppShell`. O call site
    // continua descoberto — isso está declarado, não escondido.
    @Test
    fun `medicao guiada suprime o overlay de velocidade e o back de erro`() {
        EstadoExecucaoSpeedtest.entries.forEach { estado ->
            assertFalse(
                "estado $estado não pode mostrar o overlay durante medição guiada",
                deveMostrarOverlayVelocidade(suprimeReacoesDoShell = true, estado = estado),
            )
            assertFalse(
                "estado $estado não pode tratar o back de erro durante medição guiada",
                deveTratarBackDeErroDoSpeedtest(suprimeReacoesDoShell = true, estado = estado),
            )
        }
    }

    @Test
    fun `sem medicao guiada o overlay de velocidade segue a regra original`() {
        assertTrue(deveMostrarOverlayVelocidade(false, EstadoExecucaoSpeedtest.executando))
        assertTrue(deveMostrarOverlayVelocidade(false, EstadoExecucaoSpeedtest.erro))
        assertFalse(deveMostrarOverlayVelocidade(false, EstadoExecucaoSpeedtest.idle))
        assertFalse(deveMostrarOverlayVelocidade(false, EstadoExecucaoSpeedtest.concluido))

        assertTrue(deveTratarBackDeErroDoSpeedtest(false, EstadoExecucaoSpeedtest.erro))
        assertFalse(deveTratarBackDeErroDoSpeedtest(false, EstadoExecucaoSpeedtest.executando))
    }

    @Test
    fun `tentar de novo depois da falha limpa o estado de falha`() {
        val cenario = montar(limiteMs = 1_000L)
        cenario.invocar { it.contrato.onIniciar() }
        composeRule.mainClock.advanceTimeBy(1_500L)
        assertEquals(EstadoAnaliseGuiada.Falhou(MENSAGEM_NAO_INICIOU), cenario.atual().contrato.estado)

        cenario.invocar { it.contrato.onIniciar() }

        assertEquals(
            EstadoAnaliseGuiada.EmAndamento(0f, "Preparando a análise"),
            cenario.atual().contrato.estado,
        )
        assertEquals(listOf(ModoSpeedtest.fast, ModoSpeedtest.fast), cenario.modosPedidos)
    }
}
