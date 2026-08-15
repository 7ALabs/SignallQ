package io.signallq.app.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.wifi.EstadoScanWifi
import io.signallq.app.core.network.wifi.SnapshotScanWifi
import io.signallq.app.ui.SignallQTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

/**
 * Testes do ciclo de auto-refresh da SinalScreen (#893): reescaneia sozinho
 * enquanto a aba Wi-Fi/Canal está visível, para quando não está em Wi-Fi, e
 * não duplica o loop numa mesma composição.
 *
 * Usa `autoRefreshIntervalMs` (seam de teste, só usado aqui — produção sempre
 * usa o default de 30s) pra não depender de esperar tempo real longo.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SinalScreenAutoRefreshTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val snapshotWifiVazio =
        SnapshotScanWifi(estado = EstadoScanWifi.concluido, redes = emptyList(), erroMensagem = null)

    @Test
    fun `chama onRefresh automaticamente enquanto conectado em wifi`() {
        val contador = AtomicInteger(0)

        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi = snapshotWifiVazio,
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.wifi,
                    onRefresh = { contador.incrementAndGet() },
                    onVoltar = {},
                    autoRefreshIntervalMs = 100L,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.mainClock.advanceTimeBy(350L)
        composeRule.waitForIdle()

        // Chamada imediata ao entrar + repetições no intervalo — nunca zero.
        assertTrue("esperava ao menos 2 chamadas, teve ${contador.get()}", contador.get() >= 2)
    }

    @Test
    fun `nao chama onRefresh quando nao esta em wifi`() {
        val contador = AtomicInteger(0)

        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi = snapshotWifiVazio,
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.movel,
                    onRefresh = { contador.incrementAndGet() },
                    onVoltar = {},
                    autoRefreshIntervalMs = 100L,
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.mainClock.advanceTimeBy(350L)
        composeRule.waitForIdle()

        assertEquals(0, contador.get())
    }

    @Test
    fun `nao duplica o loop de auto-refresh numa mesma composicao`() {
        val contador = AtomicInteger(0)

        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi = snapshotWifiVazio,
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.wifi,
                    onRefresh = { contador.incrementAndGet() },
                    onVoltar = {},
                    autoRefreshIntervalMs = 100L,
                )
            }
        }
        composeRule.waitForIdle()

        // Janela de 320ms com intervalo de 100ms: no maximo ~4 chamadas
        // (imediata + 3 ciclos). Se houvesse 2 loops rodando em paralelo,
        // esse numero dobraria.
        composeRule.mainClock.advanceTimeBy(320L)
        composeRule.waitForIdle()

        assertTrue("esperava no maximo 5 chamadas (loop unico), teve ${contador.get()}", contador.get() <= 5)
    }
}
