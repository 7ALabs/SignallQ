package io.signallq.app.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.signallq.app.monitoramento.MonitoramentoScheduler
import io.signallq.app.ui.SignallQTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes de caracterização da issue #1666 (Task 2.0.18, épico #1647) — decisão de produto
 * (Luiz, 2026-08-19): a tela deve explicar a frequência real de checagem ("verificamos a cada
 * X minutos") em vez de linguagem vaga tipo "acompanhamos sempre", respeitando a limitação real
 * de bateria/WorkManager do Android.
 *
 * `MonitoramentoScheduler.INTERVALO_MINUTOS` é a mesma constante usada para agendar o
 * `PeriodicWorkRequest` real (`MonitoramentoScheduler.agendar`) — este teste também funciona
 * como guarda de regressão: se o intervalo real mudar sem atualizar a UI, o teste quebra.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MonitoramentoSheetFrequenciaRealTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun montarSheet(
        monitoramentoAtivo: Boolean,
        onAtivarMonitoramento: (Boolean) -> Unit = {},
    ) {
        composeRule.setContent {
            SignallQTheme {
                MonitoramentoSheet(
                    c = io.signallq.app.ui.LocalLkTokens.current,
                    analiseAvancada = false,
                    monitoramentoAtivo = monitoramentoAtivo,
                    notificacaoLatenciaAtiva = true,
                    notificacaoDnsAtiva = true,
                    notificacaoRssiAtiva = true,
                    notificacaoSemInternetAtiva = true,
                    onDismiss = {},
                    onDefinirAnaliseAvancada = {},
                    onAtivarMonitoramento = onAtivarMonitoramento,
                    onDefinirNotificacaoLatenciaAtiva = {},
                    onDefinirNotificacaoDnsAtiva = {},
                    onDefinirNotificacaoRssiAtiva = {},
                    onDefinirNotificacaoSemInternetAtiva = {},
                )
            }
        }
    }

    @Test
    fun `constante de intervalo real e 30 minutos -- mesma usada pelo agendamento do WorkManager`() {
        // Guarda de regressao: se este valor mudar, a comunicacao na UI (abaixo) precisa mudar junto.
        assertEquals(30, MonitoramentoScheduler.INTERVALO_MINUTOS)
    }

    @Test
    fun `monitoramento ativo comunica a frequencia real em minutos, nao linguagem vaga`() {
        montarSheet(monitoramentoAtivo = true)
        composeRule.waitForIdle()
        composeRule
            .onNodeWithText(
                "Ativo · verifica a cada ${MonitoramentoScheduler.INTERVALO_MINUTOS} minutos e pode enviar alertas",
            ).assertExists()
    }

    @Test
    fun `monitoramento desativado nao promete frequencia nenhuma`() {
        montarSheet(monitoramentoAtivo = false)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Desativado").assertExists()
    }
}
