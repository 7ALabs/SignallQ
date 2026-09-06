package io.signallq.app.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.feature.history.BlocoUptime
import io.signallq.app.feature.history.StatusUptime
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.util.UUID

/**
 * Caracterização da decisão atual do Histórico: o bloco de estabilidade dos últimos 7 dias não
 * faz parte da tela, mesmo quando o estado antigo ainda chega pelo shell por compatibilidade.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HistoricoUptimeWiringCaracterizacaoTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun medicao(connectionType: String = "wifi") =
        MedicaoEntity(
            id = UUID.randomUUID().toString(),
            timestampEpochMs = System.currentTimeMillis(),
            connectionType = connectionType,
            connectionTypeStart = null,
            connectionTypeEnd = null,
            contaminado = false,
            speedtestMode = null,
            specVersion = null,
            downloadMbps = 50.0,
            uploadMbps = 20.0,
            latencyMs = 20.0,
            jitterMs = null,
            perdaPercentual = null,
            bufferbloatMs = null,
            packetLossSource = null,
            vereditoStreaming = null,
            vereditoGamer = null,
            vereditoVideoChamada = null,
            gargaloPrimario = null,
            fonte = null,
            operadoraMovel = null,
        )

    private fun blocoOk(dt: LocalDateTime = LocalDateTime.now()) =
        BlocoUptime(dataHora = dt, status = StatusUptime.OK, latencyMs = 40, latencyMediaMs = 40)

    private fun blocoSemDado(dt: LocalDateTime = LocalDateTime.now()) =
        BlocoUptime(dataHora = dt, status = StatusUptime.SEM_DADO, latencyMs = null, latencyMediaMs = null)

    private fun blocoOffline(dt: LocalDateTime = LocalDateTime.now()) =
        BlocoUptime(dataHora = dt, status = StatusUptime.OFFLINE, latencyMs = null, latencyMediaMs = null)

    private val overlineUptime = "Estabilidade da conexão · últimos 7 dias"

    @Test
    fun `blocosUptime nao informado mantem comportamento antigo sem secao de uptime`() {
        composeRule.setContent {
            SignallQTheme {
                HistoricoScreen(historico = listOf(medicao()))
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(overlineUptime, ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun `blocosUptime com dados reais nao renderiza mais a secao`() {
        val blocos = List(336) { blocoOk() }
        composeRule.setContent {
            SignallQTheme {
                HistoricoScreen(historico = listOf(medicao()), blocosUptime = blocos)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(overlineUptime, ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun `blocosUptime com quedas reais nao renderiza narrativa`() {
        val blocos = List(336) { idx -> if (idx < 40) blocoOffline() else blocoOk() }
        composeRule.setContent {
            SignallQTheme {
                HistoricoScreen(historico = listOf(medicao()), blocosUptime = blocos)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(overlineUptime, ignoreCase = true).assertDoesNotExist()
        composeRule.onNodeWithText("offline", substring = true).assertDoesNotExist()
    }

    @Test
    fun `sem historico manual mas com dado real de uptime nao cai no estado totalmente vazio`() {
        val blocos = List(335) { blocoSemDado() } + blocoOk()
        composeRule.setContent {
            SignallQTheme {
                HistoricoScreen(historico = emptyList(), blocosUptime = blocos)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(overlineUptime, ignoreCase = true).assertDoesNotExist()
        composeRule.onNodeWithText("Nenhum teste para este filtro").assertDoesNotExist()
        composeRule.onNodeWithText("Nenhum teste realizado ainda").assertExists()
    }

    @Test
    fun `sem historico manual e sem dado real de uptime mantem estado totalmente vazio`() {
        val blocos = List(336) { blocoSemDado() }
        composeRule.setContent {
            SignallQTheme {
                HistoricoScreen(historico = emptyList(), blocosUptime = blocos)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Nenhum teste realizado ainda").assertExists()
        composeRule.onNodeWithText(overlineUptime, ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun `filtro sem resultado continua mostrando mensagem especifica de filtro`() {
        // Modo controlado: HistoricoScreen confia que o chamador (ViewModel) ja filtrou
        // `historico` para o filtro selecionado -- nao refiltra internamente. Para simular
        // "filtro MOVEL sem nenhum resultado" nesse modo, o historico pre-filtrado chega vazio.
        composeRule.setContent {
            SignallQTheme {
                HistoricoScreen(
                    historico = emptyList(),
                    filtroConexao = io.signallq.app.ui.FiltroConexaoHistorico.MOVEL,
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Nenhum teste para este filtro").assertExists()
    }
}
