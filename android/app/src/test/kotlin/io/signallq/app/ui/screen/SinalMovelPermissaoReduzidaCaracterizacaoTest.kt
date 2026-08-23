package io.signallq.app.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.wifi.EstadoScanWifi
import io.signallq.app.core.network.wifi.SnapshotScanWifi
import io.signallq.app.core.telephony.MovelSimSnapshot
import io.signallq.app.core.telephony.MovelSnapshot
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * GH#1662 (Task 2.0.14, épico #1647) — cobre as decisões de produto de 2026-08-19 na aba Móvel:
 *
 * 1. Sem permissão de telefone, a tela continua útil de forma reduzida quando há dado (snapshot
 *    ou SIM ativo) — não bloqueia mais com o empty state cheio (esse comportamento, quando NÃO
 *    há dado nenhum, já está coberto por [SinalScreenExtracaoAbaCaracterizacaoTest] e continua
 *    valendo sem alteração).
 * 2. Operadora não identificada no catálogo local não esconde nem desabilita o botão de contato
 *    — mostra um fallback genérico.
 * 3. Detalhes técnicos (RSRP/RSRQ/SINR) só aparecem depois dos três cards de conclusão.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SinalMovelPermissaoReduzidaCaracterizacaoTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val snapshotWifiVazio =
        SnapshotScanWifi(estado = EstadoScanWifi.concluido, redes = emptyList(), erroMensagem = null)

    private fun snapshotMovel(
        operadora: String? = "Vivo",
        tecnologia: String? = "4G",
        rsrpDbm: Int? = -85,
        capturaReduzida: Boolean = false,
    ) = MovelSnapshot(
        operadora = operadora,
        tecnologia = tecnologia,
        rsrpDbm = rsrpDbm,
        rsrqDb = if (rsrpDbm != null) -11 else null,
        sinrDb = if (rsrpDbm != null) 8 else null,
        ecnoDb = null,
        bandaMovel = null,
        cellId = null,
        mcc = null,
        mnc = null,
        tac = null,
        roaming = false,
        capturaReduzida = capturaReduzida,
        timestampMs = 0L,
    )

    @Test
    fun `sem permissao mas com snapshot reduzido a aba continua util em vez de bloquear`() {
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi = snapshotWifiVazio,
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.movel,
                    movelSnapshot = snapshotMovel(operadora = "Vivo", tecnologia = null, rsrpDbm = null, capturaReduzida = true),
                    temPermissaoTelefonia = false,
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Não é mais o empty state de bloqueio total.
        composeRule.onNodeWithText("Permissão necessária").assertDoesNotExist()
        // Explica o que falta e por quê, sem esconder o resto do conteúdo.
        composeRule.onNodeWithText("Mostrando o que dá para saber sem a permissão de telefone").assertExists()
        composeRule.onNodeWithText("Vivo").assertExists()
        composeRule.onNodeWithText("Qualidade do sinal").assertExists()
    }

    @Test
    fun `sem permissao e sem nenhum dado o empty state de permissao continua aparecendo`() {
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi = snapshotWifiVazio,
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.movel,
                    movelSnapshot = null,
                    temPermissaoTelefonia = false,
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Nada pra mostrar de fato (nem operadora) -- fallback continua sendo o empty state.
        composeRule.onNodeWithText("Permissão necessária").assertExists()
    }

    @Test
    fun `com permissao concedida o banner de permissao reduzida nao aparece`() {
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi = snapshotWifiVazio,
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.movel,
                    movelSnapshot = snapshotMovel(),
                    temPermissaoTelefonia = true,
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Mostrando o que dá para saber sem a permissão de telefone").assertDoesNotExist()
    }

    @Test
    fun `operadora nao cadastrada no catalogo local ainda mostra botao de contato com fallback`() {
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi = snapshotWifiVazio,
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.movel,
                    movelSnapshot = snapshotMovel(operadora = "Operadora Fora Do Catalogo"),
                    temPermissaoTelefonia = true,
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Falar com a Operadora Fora Do Catalogo").assertExists()
    }

    @Test
    fun `operadora nao identificada pelo Android mostra rotulo generico de contato`() {
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi = snapshotWifiVazio,
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.movel,
                    movelSnapshot = snapshotMovel(operadora = null),
                    temPermissaoTelefonia = true,
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Falar com sua operadora").assertExists()
    }

    @Test
    fun `detalhes tecnicos com RSRP aparecem depois dos cards de conclusao`() {
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi = snapshotWifiVazio,
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.movel,
                    movelSnapshot = snapshotMovel(rsrpDbm = -85, tecnologia = "4G"),
                    temPermissaoTelefonia = true,
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Qualidade do sinal").assertExists()
        composeRule.onNodeWithText("Detalhes técnicos").assertExists()
        composeRule.onNodeWithText("RSRP").assertExists()
        composeRule.onNodeWithText("-85 dBm").assertExists()
    }

    @Test
    fun `dual-SIM continua distinguindo SIM ativa da SIM de dados sem permissao`() {
        val simDados =
            MovelSimSnapshot(
                subId = 1,
                simIndex = 1,
                operadora = "Vivo",
                tecnologiaRede = "4G",
                rsrpDbm = -85,
                emRoaming = false,
                isDefaultData = true,
                radioDesligado = false,
                rsrqDb = -11,
                sinrDb = 8,
            )
        val simSecundario =
            MovelSimSnapshot(
                subId = 2,
                simIndex = 2,
                operadora = "TIM",
                tecnologiaRede = "3G",
                rsrpDbm = -100,
                emRoaming = false,
                isDefaultData = false,
                radioDesligado = false,
                rsrqDb = null,
                sinrDb = null,
            )
        composeRule.setContent {
            SignallQTheme {
                SinalScreen(
                    snapshotWifi = snapshotWifiVazio,
                    connectedNetwork = null,
                    estadoConexao = EstadoConexao.movel,
                    simsAtivos = listOf(simDados, simSecundario),
                    temPermissaoTelefonia = true,
                    onRefresh = {},
                    onVoltar = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Chip 1").assertExists()
        composeRule.onNodeWithText("Chip 2").assertExists()
        composeRule.onNodeWithText("Vivo · 4G").assertExists()
        composeRule.onNodeWithText("TIM · 3G").assertExists()
        composeRule.onNodeWithText("-85 dBm").assertExists()
        composeRule.onNodeWithText("-100 dBm").assertExists()
    }
}
