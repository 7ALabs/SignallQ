package io.signallq.app.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import io.signallq.app.ui.FiltroConexaoHistorico
import io.signallq.app.ui.SignallQTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes de caracterização do ponto de extensão de **root content** criado pela issue #1698
 * (épico #1647), irmão de `AppShellOverlayRegistryTest`.
 *
 * Mesma estrutura de duas camadas deliberadamente redundantes que a revisão de Caio exigiu na
 * PR #1697:
 * - **por raiz** (`AppShellXxxRoot` chamado direto) — cobre que a raiz repassa corretamente o que
 *   recebe (estado, callbacks, regra de disponibilidade);
 * - **por registro** (`AppShellRootRegistry` chamado inteiro) — cobre que a entrada daquela raiz
 *   realmente existe dentro do agregador. Sem esta camada, apagar a linha
 *   `AppShellRoot.History -> AppShellHistoricoRoot(...)` de dentro do registro deixaria a suíte
 *   verde e a tela sumiria do app — foi exatamente o achado que reprovou a primeira rodada da
 *   PR #1697 (4 de 7 entradas do registro de overlay sem cobertura efetiva).
 *
 * A terceira camada é específica desta issue e não existia no registro de overlays: o slot
 * [AppShellRootRegistry] `naoMigradas`. Ele precisa ser chamado para as 3 raízes ainda inline em
 * `AppShell.kt` e **nunca** para as 2 migradas — um `when` que caísse no slot por engano
 * renderizaria a tela antiga silenciosamente.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppShellRootRegistryTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun historicoEntry(
        state: AppShellHistoricoState = AppShellHistoricoState(),
        onIniciarTeste: () -> Unit = {},
    ) = AppShellHistoricoRootEntry(
        state = state,
        adsEnabled = false,
        onAbrirMenu = {},
        onIniciarTeste = onIniciarTeste,
    )

    private fun ferramentasEntry(
        acoes: AppShellFerramentasAcoes = acoesVazias(),
        disponibilidade: (TipoFerramenta) -> FerramentaDisponibilidade = { FerramentaDisponibilidade.Disponivel },
        onRegistrarAbertura: (TipoFerramenta) -> Unit = {},
    ) = AppShellFerramentasRootEntry(
        acoes = acoes,
        disponibilidade = disponibilidade,
        onAbrirMenu = {},
        onRegistrarAbertura = onRegistrarAbertura,
    )

    private fun acoesVazias(registro: MutableList<String>? = null) =
        AppShellFerramentasAcoes(
            onAbrirSinalCanais = { registro?.add("sinal_canais") },
            onAbrirDispositivos = { registro?.add("dispositivos") },
            onAbrirEquipamentoInternet = { registro?.add("equipamento") },
            onAbrirPing = { registro?.add("ping") },
            onAbrirDns = { registro?.add("dns") },
            onAbrirLaudo = { registro?.add("laudo") },
            onAbrirMonitoramento = { registro?.add("monitoramento") },
            onAbrirModoGamer = { registro?.add("modo_gamer") },
            onAbrirSinalWifi = { registro?.add("sinal_wifi") },
        )

    /** Wiring padrão do registro; o teste customiza só o que está exercitando. */
    @Composable
    private fun RegistroDeTeste(
        root: AppShellRoot,
        historico: AppShellHistoricoRootEntry = historicoEntry(),
        ferramentas: AppShellFerramentasRootEntry = ferramentasEntry(),
        naoMigradas: @Composable (AppShellRoot) -> Unit = {},
    ) {
        SignallQTheme {
            AppShellRootRegistry(
                root = root,
                historico = historico,
                ferramentas = ferramentas,
                naoMigradas = naoMigradas,
            )
        }
    }

    // ─── Por raiz (a raiz repassa o que recebe) ─────────────────────────────────

    @Test
    fun `historico root compoe a tela e repassa onIniciarTeste`() {
        var iniciou = false
        composeRule.setContent {
            SignallQTheme {
                AppShellHistoricoRoot(
                    state = AppShellHistoricoState(),
                    adsEnabled = false,
                    onAbrirMenu = {},
                    onIniciarTeste = { iniciou = true },
                )
            }
        }
        composeRule.onNodeWithText("Histórico").assertExists()
        // Lista vazia + filtro TODOS = estado vazio inicial.
        composeRule.onNodeWithText("Fazer primeiro teste").performClick()
        composeRule.runOnIdle { assertTrue(iniciou) }
    }

    @Test
    fun `historico root repassa o filtro de conexao e nao cai no modo nao-controlado`() {
        // Mutante que este teste mata: remover `filtroConexao = state.filtroConexao` do repasse.
        // Sem ele, `HistoricoScreen` entra em modo NÃO-controlado (filtro nulo) e volta a filtrar
        // a lista internamente — double-filter que a própria tela documenta como bug. A diferença
        // é observável: com filtro ativo e lista vazia, a copy do estado vazio é "Medir agora";
        // sem filtro, é "Fazer primeiro teste".
        composeRule.setContent {
            SignallQTheme {
                AppShellHistoricoRoot(
                    state = AppShellHistoricoState(filtroConexao = FiltroConexaoHistorico.MOVEL),
                    adsEnabled = false,
                    onAbrirMenu = {},
                    onIniciarTeste = {},
                )
            }
        }
        composeRule.onNodeWithText("Medir agora").assertExists()
        composeRule.onNodeWithText("Fazer primeiro teste").assertDoesNotExist()
    }

    @Test
    fun `ferramentas root repassa as nove acoes para os cards certos`() {
        // Mutante que este teste mata: trocar dois campos de lugar em AppShellFerramentasAcoes
        // (ex.: onAbrirPing recebendo onAbrirDns). Compila, e o usuário abre a tela errada.
        val abertas = mutableListOf<String>()
        composeRule.setContent {
            SignallQTheme {
                AppShellFerramentasRoot(
                    acoes = acoesVazias(abertas),
                    disponibilidade = { FerramentaDisponibilidade.Disponivel },
                    onAbrirMenu = {},
                    onRegistrarAbertura = {},
                )
            }
        }
        listOf(
            "Sinal e canais" to "sinal_canais",
            "Sinal Wi-Fi ao vivo" to "sinal_wifi",
            "Dispositivos" to "dispositivos",
            "Equipamento de internet" to "equipamento",
            "Ping" to "ping",
            "DNS" to "dns",
            "Laudo" to "laudo",
            "Monitoramento" to "monitoramento",
            "Modo gamer" to "modo_gamer",
        ).forEach { (rotulo, _) ->
            composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(rotulo))
            composeRule.onNodeWithText(rotulo).performClick()
        }
        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    "sinal_canais",
                    "sinal_wifi",
                    "dispositivos",
                    "equipamento",
                    "ping",
                    "dns",
                    "laudo",
                    "monitoramento",
                    "modo_gamer",
                ),
                abertas,
            )
        }
    }

    @Test
    fun `ferramentas root repassa a regra de disponibilidade em vez de assumir tudo disponivel`() {
        // Mutante: trocar `disponibilidade = disponibilidade` por um `{ Disponivel }` fixo.
        // Sem esta asserção, a ferramenta bloqueada abriria normalmente.
        //
        // `Offline` é o estado escolhido de propósito: é o único dos três que BLOQUEIA a
        // navegação. `IndisponivelRemotamente` e `PermissaoNecessaria` mostram o próximo passo e
        // ainda assim navegam (comportamento já travado por `FerramentasScreenTest`) — usá-los
        // aqui não distinguiria "regra repassada" de "regra ignorada".
        val abertas = mutableListOf<String>()
        composeRule.setContent {
            SignallQTheme {
                AppShellFerramentasRoot(
                    acoes = acoesVazias(abertas),
                    disponibilidade = { tipo ->
                        if (tipo == TipoFerramenta.PING) {
                            FerramentaDisponibilidade.Offline("Reconecte-se.")
                        } else {
                            FerramentaDisponibilidade.Disponivel
                        }
                    },
                    onAbrirMenu = {},
                    onRegistrarAbertura = {},
                )
            }
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Ping"))
        composeRule.onNodeWithText("Ping").performClick()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Reconecte-se."))
        composeRule.onNodeWithText("Reconecte-se.").assertExists()
        composeRule.runOnIdle { assertTrue("Ping offline nao pode ter navegado", abertas.isEmpty()) }
    }

    // ─── Por registro (a entrada existe dentro do agregador) ────────────────────

    @Test
    fun `registro compoe historico quando a raiz e History`() {
        composeRule.setContent { RegistroDeTeste(root = AppShellRoot.History) }
        composeRule.onNodeWithText("Histórico").assertExists()
    }

    @Test
    fun `registro compoe ferramentas quando a raiz e Tools`() {
        composeRule.setContent { RegistroDeTeste(root = AppShellRoot.Tools) }
        composeRule.onNodeWithText("Sinal e canais").assertExists()
    }

    @Test
    fun `registro repassa onIniciarTeste do historico ate a tela`() {
        // Cobre a cadeia inteira registro -> raiz -> tela, não só a raiz isolada.
        var iniciou = false
        composeRule.setContent {
            RegistroDeTeste(
                root = AppShellRoot.History,
                historico = historicoEntry(onIniciarTeste = { iniciou = true }),
            )
        }
        composeRule.onNodeWithText("Fazer primeiro teste").performClick()
        composeRule.runOnIdle { assertTrue(iniciou) }
    }

    @Test
    fun `registro repassa a regra de disponibilidade do hub ate a tela`() {
        composeRule.setContent {
            RegistroDeTeste(
                root = AppShellRoot.Tools,
                ferramentas =
                    ferramentasEntry(
                        disponibilidade = { tipo ->
                            if (tipo == TipoFerramenta.PING) {
                                FerramentaDisponibilidade.Offline("Reconecte-se.")
                            } else {
                                FerramentaDisponibilidade.Disponivel
                            }
                        },
                    ),
            )
        }
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Ping"))
        composeRule.onNodeWithText("Ping").performClick()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Reconecte-se."))
        composeRule.onNodeWithText("Reconecte-se.").assertExists()
    }

    // ─── Slot das raízes ainda não migradas ────────────────────────────────────

    @Test
    fun `registro delega ao slot exatamente as tres raizes ainda inline no AppShell`() {
        // Trava os dois lados da fronteira de migração: quem deve cair no slot cai, e a raiz
        // recebida é a mesma que entrou (um `naoMigradas(AppShellRoot.Home)` fixo passaria
        // despercebido sem a comparação por valor).
        //
        // A raiz é dirigida por estado em vez de um `setContent` por iteração: `ComposeTestRule`
        // aceita `setContent` uma única vez por teste. Começa em `History` (migrada) para que a
        // primeira transição já seja uma troca real de valor.
        var recebida: AppShellRoot? = null
        val raizAtual = mutableStateOf(AppShellRoot.History)
        composeRule.setContent {
            RegistroDeTeste(root = raizAtual.value, naoMigradas = { recebida = it })
        }
        listOf(AppShellRoot.Home, AppShellRoot.Speed, AppShellRoot.Wifi).forEach { raiz ->
            composeRule.runOnIdle {
                recebida = null
                raizAtual.value = raiz
            }
            composeRule.waitForIdle()
            composeRule.runOnIdle { assertEquals("slot deveria receber $raiz", raiz, recebida) }
        }
    }

    @Test
    fun `registro nunca cai no slot para as raizes ja migradas`() {
        // Mutante que este teste mata: mover History ou Tools de volta para o ramo
        // `naoMigradas` do `when`. O app continuaria funcionando (o slot ainda desenha a tela
        // antiga em `AppShell.kt`), mas a migração teria sido revertida em silêncio.
        var recebida: AppShellRoot? = null
        val raizAtual = mutableStateOf(AppShellRoot.Home)
        composeRule.setContent {
            RegistroDeTeste(root = raizAtual.value, naoMigradas = { recebida = it })
        }
        listOf(AppShellRoot.History, AppShellRoot.Tools).forEach { raiz ->
            composeRule.runOnIdle {
                recebida = null
                raizAtual.value = raiz
            }
            composeRule.waitForIdle()
            composeRule.runOnIdle { assertNull("$raiz nao pode cair no slot de nao migradas", recebida) }
        }
    }
}
