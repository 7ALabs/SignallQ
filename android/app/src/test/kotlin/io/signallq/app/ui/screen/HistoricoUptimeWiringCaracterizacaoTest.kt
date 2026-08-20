package io.signallq.app.ui.screen

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
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
 * Testes de caracterização das issues #1666 (Task 2.0.18, épico #1647) e #1520 — religam o
 * wiring de `UptimeGridChart` em `HistoricoScreen.kt`, órfão desde uma refatoração anterior
 * (GH#495 original, regressão confirmada em #1518/#1520).
 *
 * Decisão de produto (Luiz, 2026-08-19, comentário final na #1520): restaurar o wiring de
 * `UptimeGridChart`/`UptimeChartUseCase`/`UptimeNarrativaEngine` a partir de `HistoricoScreen.kt`,
 * com teste cobrindo a renderização real — é este arquivo.
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

    // LkSectionOverline aplica `.uppercase()` ao texto (BaseComponents.kt) -- os asserts abaixo
    // usam ignoreCase para nao acoplar o teste a essa transformacao visual.
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
    fun `blocosUptime com dados reais renderiza secao e narrativa de rede estavel`() {
        val blocos = List(336) { blocoOk() }
        composeRule.setContent {
            SignallQTheme {
                HistoricoScreen(historico = listOf(medicao()), blocosUptime = blocos)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(overlineUptime, ignoreCase = true).assertExists()
        composeRule
            .onNodeWithText("Sua rede esteve estável nos últimos 7 dias. Nenhuma lentidão ou queda detectada.")
            .assertExists()
    }

    @Test
    fun `blocosUptime com quedas reais distingue offline na narrativa`() {
        val blocos = List(336) { idx -> if (idx < 40) blocoOffline() else blocoOk() }
        composeRule.setContent {
            SignallQTheme {
                HistoricoScreen(historico = listOf(medicao()), blocosUptime = blocos)
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(overlineUptime, ignoreCase = true).assertExists()
        // Narrativa deve mencionar offline (rede indisponivel), nao apenas lentidao -- aparece
        // tanto no card de narrativa quanto no resumo por dia, por isso pega o primeiro match.
        composeRule.onAllNodesWithText("offline", substring = true)[0].assertExists()
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
        // Estado totalmente vazio (EmptyHistorico em tela cheia) nao renderiza a secao de
        // uptime -- a presenca dela prova que caiu no ramo do LazyColumn, nao no ramo de tela
        // cheia vazia (a lista manual continua vazia, mas nao e o UNICO conteudo da tela).
        composeRule.onNodeWithText(overlineUptime, ignoreCase = true).assertExists()
        // #1666: sem filtro ativo, o texto de lista manual vazia deve ser o generico, nunca
        // "para este filtro" (regressao corrigida nesta mesma fatia -- antes vinha hardcoded).
        // O card de uptime empurra esse item para fora da viewport inicial do teste --
        // rola a LazyColumn ate ele antes de checar (LazyColumn so compoe itens visiveis).
        composeRule
            .onNodeWithTag("historico_lista")
            .performScrollToNode(hasText("Nenhum teste realizado ainda"))
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
