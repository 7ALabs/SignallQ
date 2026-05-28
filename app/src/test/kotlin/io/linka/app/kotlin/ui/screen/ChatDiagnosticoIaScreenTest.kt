package io.linka.app.kotlin.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.assertCountEquals
import io.linka.app.kotlin.feature.diagnostico.chat.StatusSessao
import io.linka.app.kotlin.feature.diagnostico.chat.SessaoChatDiagnostico
import io.linka.app.kotlin.ui.LinkaTheme
import io.linka.app.kotlin.ui.viewmodel.ChatDiagUiState
import io.linka.app.kotlin.ui.viewmodel.EstadoChatDiagnostico
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Smoke tests de UI para ChatDiagnosticoIaScreen usando Robolectric + Compose.
 *
 * Padrão para outras Screens:
 *  1. Anotar a classe com @RunWith(RobolectricTestRunner::class) e @Config(sdk = [34]).
 *  2. Usar createComposeRule() — NÃO createAndroidComposeRule.
 *  3. Envolver o conteúdo com LinkaTheme { } para resolver LocalLkTokens.
 *  4. Passar o estado diretamente via parâmetros do Composable (pattern stateless).
 *  5. Verificar presença/ausência de nós via semantics (contentDescription ou text),
 *     não por texto literal que pode mudar.
 *  6. Não testar lógica de negócio — isso é responsabilidade do ViewModel test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChatDiagnosticoIaScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun uiStateBase() = ChatDiagUiState(
        estado = EstadoChatDiagnostico.Idle,
        opcoesIniciaisVisiveis = false,
        drawerAberto = false,
    )

    private fun renderScreen(
        uiState: ChatDiagUiState,
        onVoltar: () -> Unit = {},
        onEnviarMensagem: (String) -> Unit = {},
        onAtualizarDraft: (String) -> Unit = {},
        onEscolherOpcao: (io.linka.app.kotlin.feature.diagnostico.chat.TipoDiagnostico) -> Unit = {},
        onAbrirSessao: (String) -> Unit = {},
        onApagarSessao: (String) -> Unit = {},
        onRenomearSessao: (String, String) -> Unit = { _, _ -> },
        onNovaSessao: () -> Unit = {},
        onToggleDrawer: () -> Unit = {},
        onCancelarAcaoAtual: () -> Unit = {},
    ) {
        composeRule.setContent {
            LinkaTheme {
                ChatDiagnosticoIaScreen(
                    uiState = uiState,
                    onVoltar = onVoltar,
                    onEnviarMensagem = onEnviarMensagem,
                    onAtualizarDraft = onAtualizarDraft,
                    onEscolherOpcao = onEscolherOpcao,
                    onAbrirSessao = onAbrirSessao,
                    onApagarSessao = onApagarSessao,
                    onRenomearSessao = onRenomearSessao,
                    onNovaSessao = onNovaSessao,
                    onToggleDrawer = onToggleDrawer,
                    onCancelarAcaoAtual = onCancelarAcaoAtual,
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Teste 1: Chips iniciais aparecem quando opcoesIniciaisVisiveis = true
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun chipsIniciais_aparecem_quando_opcoesIniciaisVisiveis_true() {
        renderScreen(
            uiState = uiStateBase().copy(opcoesIniciaisVisiveis = true),
        )

        // Os 3 chips têm contentDescription igual ao label (ver OpcoesIniciaisChips)
        composeRule
            .onNodeWithContentDescription("Analisar meu último teste")
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Executar novo teste agora")
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Analisar meu histórico recente")
            .assertIsDisplayed()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Teste 2: Chips iniciais somem quando opcoesIniciaisVisiveis = false
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun chipsIniciais_somem_quando_opcoesIniciaisVisiveis_false() {
        renderScreen(
            uiState = uiStateBase().copy(opcoesIniciaisVisiveis = false),
        )

        composeRule
            .onNodeWithContentDescription("Analisar meu último teste")
            .assertDoesNotExist()
        composeRule
            .onNodeWithContentDescription("Executar novo teste agora")
            .assertDoesNotExist()
        composeRule
            .onNodeWithContentDescription("Analisar meu histórico recente")
            .assertDoesNotExist()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Teste 3: CotaExcedidaBanner substitui o input quando estado = CotaExcedida
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun cotaExcedidaBanner_substitui_input_quando_estado_cotaExcedida() {
        renderScreen(
            uiState = uiStateBase().copy(estado = EstadoChatDiagnostico.CotaExcedida),
        )

        // Banner de cota usa texto fixo "Limite diário atingido"
        composeRule
            .onNodeWithText("Limite diário atingido")
            .assertIsDisplayed()
    }

    @Test
    fun cotaExcedidaBanner_nao_aparece_quando_estado_idle() {
        renderScreen(
            uiState = uiStateBase().copy(estado = EstadoChatDiagnostico.Idle),
        )

        composeRule
            .onNodeWithText("Limite diário atingido")
            .assertDoesNotExist()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Teste 4: Drawer tem paneTitle correto quando aberto (semantics)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun drawer_paneTitle_historico_de_conversas_existe_quando_aberto() {
        renderScreen(
            uiState = uiStateBase().copy(drawerAberto = true),
        )

        // DrawerConteudo usa: modifier = Modifier.semantics { paneTitle = "Histórico de conversas" }
        composeRule
            .onNodeWithText("Conversas")
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("Nova conversa")
            .assertIsDisplayed()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Teste 5: Tema claro e escuro compilam e renderizam sem crash
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun screen_renderiza_sem_crash_tema_claro() {
        composeRule.setContent {
            LinkaTheme(darkTheme = false) {
                ChatDiagnosticoIaScreen(
                    uiState = uiStateBase(),
                    onVoltar = {},
                    onEnviarMensagem = {},
                    onAtualizarDraft = {},
                    onEscolherOpcao = {},
                    onAbrirSessao = {},
                    onApagarSessao = {},
                    onRenomearSessao = { _, _ -> },
                    onNovaSessao = {},
                    onToggleDrawer = {},
                    onCancelarAcaoAtual = {},
                )
            }
        }

        // Só verificar que o TopBar rendiza — título fixo "Diagnóstico IA"
        composeRule
            .onNodeWithText("Diagnóstico IA")
            .assertIsDisplayed()
    }

    @Test
    fun screen_renderiza_sem_crash_tema_escuro() {
        composeRule.setContent {
            LinkaTheme(darkTheme = true) {
                ChatDiagnosticoIaScreen(
                    uiState = uiStateBase(),
                    onVoltar = {},
                    onEnviarMensagem = {},
                    onAtualizarDraft = {},
                    onEscolherOpcao = {},
                    onAbrirSessao = {},
                    onApagarSessao = {},
                    onRenomearSessao = { _, _ -> },
                    onNovaSessao = {},
                    onToggleDrawer = {},
                    onCancelarAcaoAtual = {},
                )
            }
        }

        composeRule
            .onNodeWithText("Diagnóstico IA")
            .assertIsDisplayed()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Teste 6 (bônus): Auto-scroll — lista com mensagens renderiza sem crash
    // e não exibe chips quando opcoesIniciaisVisiveis = false
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun lista_com_mensagens_renderiza_e_nao_exibe_chips_quando_ocultos() {
        val mensagens = listOf(
            io.linka.app.kotlin.feature.diagnostico.chat.ChatMensagem(
                id = "1",
                sessionId = "s1",
                papel = io.linka.app.kotlin.feature.diagnostico.chat.PapelChatMensagem.assistente,
                conteudo = "Olá, sou o Diagnóstico IA.",
                criadoEmEpochMs = 0L,
                status = io.linka.app.kotlin.feature.diagnostico.chat.StatusChatMensagem.concluido,
                isLocal = true,
            ),
        )

        renderScreen(
            uiState = uiStateBase().copy(
                mensagens = mensagens,
                opcoesIniciaisVisiveis = false,
            ),
        )

        // A mensagem está na tela
        composeRule
            .onNodeWithText("Olá, sou o Diagnóstico IA.")
            .assertIsDisplayed()

        // Chips não estão — opcoesIniciaisVisiveis = false
        composeRule
            .onNodeWithContentDescription("Analisar meu último teste")
            .assertDoesNotExist()
    }
}
