package io.signallq.app.ui.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.signallq.app.diagnosticooffline.DiagnosticoOfflineEstado
import io.signallq.app.diagnosticooffline.EtapaDiagnosticoOffline
import io.signallq.app.diagnosticooffline.ResultadoEtapaDiagnosticoOffline
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Issue #1818 — cobre `DiagnosticoOfflineConteudo` (o Composable interno de
 * `DiagnosticoOfflineDialog.kt`) contra os estados reais de [DiagnosticoOfflineEstado], sem
 * depender do `DiagnosticoOfflineViewModel`/`DiagnosticoOfflineViewModelFactory` (que exigem
 * `Context`/rede real) — os 3 estados exigidos pelo critério de aceite: inicial, etapa com falha
 * (com retry) e conclusão com sucesso total.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DiagnosticoOfflineDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `estado inicial mostra as 4 etapas pendentes e nenhum resumo`() {
        composeRule.setContent {
            SignallQTheme {
                DiagnosticoOfflineConteudo(
                    estado = DiagnosticoOfflineEstado.Idle,
                    onRetry = {},
                    onDismiss = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Diagnóstico guiado").assertExists()
        composeRule.onNodeWithText("Gateway").assertExists()
        composeRule.onNodeWithText("DNS").assertExists()
        composeRule.onNodeWithText("Rota externa").assertExists()
        composeRule.onNodeWithText("Hostname / captive portal").assertExists()
        composeRule.onNodeWithText("Diagnóstico concluído").assertDoesNotExist()
    }

    @Test
    fun `etapa com falha mostra motivo e oferece um unico retry, que aciona onRetry`() {
        var chamadas = 0
        val historico =
            listOf(
                ResultadoEtapaDiagnosticoOffline.Sucesso(EtapaDiagnosticoOffline.GATEWAY),
                ResultadoEtapaDiagnosticoOffline.Falha(EtapaDiagnosticoOffline.DNS, motivo = "Servidor DNS não respondeu"),
            )
        composeRule.setContent {
            SignallQTheme {
                DiagnosticoOfflineConteudo(
                    estado =
                        DiagnosticoOfflineEstado.DiagnosticoConcluido(
                            historico = historico,
                            etapaComFalha = EtapaDiagnosticoOffline.DNS,
                        ),
                    onRetry = { chamadas++ },
                    onDismiss = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Servidor DNS não respondeu").assertExists()
        composeRule.onNodeWithText("Diagnóstico concluído com falha").assertExists()

        // Só existe um botão "Tentar novamente" na tela (rodapé fixo) — onNodeWithText já falha
        // se houver mais de um nó com esse texto, então a unicidade em si já é a asserção contra
        // o achado de a11y da revisão do Caio na PR #1821 (dois botões idênticos).
        composeRule.onNodeWithText("Tentar novamente").performClick()
        composeRule.waitForIdle()

        assert(chamadas == 1) { "esperava 1 chamada a onRetry, houve $chamadas" }
    }

    @Test
    fun `conclusao com sucesso total mostra resumo positivo sem botao de retry`() {
        val historico = EtapaDiagnosticoOffline.ORDEM.map { ResultadoEtapaDiagnosticoOffline.Sucesso(it) }
        var dismissChamado = false
        composeRule.setContent {
            SignallQTheme {
                DiagnosticoOfflineConteudo(
                    estado = DiagnosticoOfflineEstado.DiagnosticoConcluido(historico = historico, etapaComFalha = null),
                    onRetry = {},
                    onDismiss = { dismissChamado = true },
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Diagnóstico concluído").assertExists()
        composeRule.onNodeWithText("Tentar novamente").assertDoesNotExist()

        // Rodapé fixo fora do scroll (achado de revisão do Caio na PR #1821) — não precisa mais
        // de performScrollTo() pra alcançar o botão.
        composeRule.onNodeWithText("Concluir").performClick()
        composeRule.waitForIdle()

        assert(dismissChamado) { "esperava onDismiss chamado ao tocar em Concluir" }
    }
}
