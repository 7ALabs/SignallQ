package io.signallq.app.ui.screen

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import io.signallq.app.core.network.contracts.localdevice.ClientSnapshot
import io.signallq.app.core.network.contracts.localdevice.DataFreshness
import io.signallq.app.core.network.contracts.localdevice.DeviceCapabilities
import io.signallq.app.core.network.contracts.localdevice.DeviceType
import io.signallq.app.core.network.contracts.localdevice.FiberSnapshot
import io.signallq.app.core.network.contracts.localdevice.LanSnapshot
import io.signallq.app.core.network.contracts.localdevice.LocalNetworkDeviceSnapshot
import io.signallq.app.core.network.contracts.localdevice.SupportLevel
import io.signallq.app.core.network.contracts.localdevice.TipoConexaoFisica
import io.signallq.app.core.network.contracts.localdevice.WifiRadioSnapshot
import io.signallq.app.core.network.contracts.localdevice.WifiSnapshot
import io.signallq.app.feature.fibra.EstadoFibra
import io.signallq.app.feature.fibra.SnapshotFibra
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Teste de caracterização do bug #6 (redesign de distribuição de cards,
 * spec Lia 2026-07-18) — protege a extração de `EquipamentoInternetScreen.kt`
 * (dívida crítica) em 6 componentes novos (`Equipamento*Card.kt`) e a nova
 * ordem narrativa: identidade → status (absorve saúde óptica) → disponibilidade
 * → uso → alerta → aviso → topologia → módulos técnicos (Wi-Fi full-width, com
 * as duas bandas como linhas — revisão Lia 2026-07-18) → dispositivos → info
 * técnica → ações.
 *
 * Escrito ANTES da extração (`.claude/rules/higiene-e-padronizacao-repositorio.md`
 * seção 4.6, "crie testes de caracterização antes de extrações com risco de
 * comportamento de dados ou estado").
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class EquipamentoInternetScreenLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** Roteador com fibra, Wi-Fi 2,4/5GHz, LAN e 2 clientes — cobre o par 2-col
     *  de Disponibilidade/Uso, o card único full-width de Wi-Fi (com as duas
     *  bandas como linhas) e o módulo full de LAN. */
    private fun localDeviceCompleto() =
        LocalNetworkDeviceSnapshot(
            deviceType = DeviceType.ROUTER,
            supportLevel = SupportLevel.LAB_VALIDATED,
            capabilities =
                DeviceCapabilities(
                    suportaFibra = true,
                    suportaWan = false,
                    suportaWifi = true,
                    suportaLan = true,
                    suportaClientes = true,
                    suportaDiagnosticoNativo = false,
                    suportaGerenciamento = false,
                ),
            vendor = "TP-Link",
            modelo = "Archer C6",
            firmwareVersion = "1.2.3",
            fiber =
                FiberSnapshot(
                    linkAtivo = true,
                    rxPowerDbm = -19.8,
                    txPowerDbm = 2.1,
                    temperaturaCelsius = 45.0,
                    tensaoV = 3.3,
                    correnteLaserMa = 10.0,
                    serialOnt = "ABC123",
                ),
            wan = null,
            wifi =
                WifiSnapshot(
                    radios =
                        listOf(
                            WifiRadioSnapshot(
                                banda = "2.4GHz",
                                ssid = "Casa_24",
                                canal = 6,
                                larguraCanal = "40MHz",
                                potenciaTx = "high",
                                criptografia = "wpa2",
                                habilitado = true,
                            ),
                            WifiRadioSnapshot(
                                banda = "5GHz",
                                ssid = "Casa_5G",
                                canal = 44,
                                larguraCanal = "80MHz",
                                potenciaTx = "high",
                                criptografia = "wpa2",
                                habilitado = true,
                            ),
                        ),
                ),
            lan =
                LanSnapshot(
                    ipRoteador = "192.168.1.1",
                    mascara = "255.255.255.0",
                    dhcpHabilitado = true,
                    faixaDhcpInicio = "192.168.1.100",
                    faixaDhcpFim = "192.168.1.200",
                ),
            clientes =
                listOf(
                    ClientSnapshot(
                        mac = null,
                        ip = "192.168.1.50",
                        hostname = "Notebook",
                        tipoConexao = "wifi_2g",
                        tipoConexaoFisica = TipoConexaoFisica.WIFI,
                    ),
                    ClientSnapshot(
                        mac = null,
                        ip = "192.168.1.51",
                        hostname = "Desktop",
                        tipoConexao = "wired",
                        tipoConexaoFisica = TipoConexaoFisica.ETHERNET,
                    ),
                ),
            warnings = emptyList(),
            freshness = DataFreshness(capturadoEmEpochMs = System.currentTimeMillis(), expirado = false),
        )

    private fun snapshotFibraConcluido() =
        SnapshotFibra(estado = EstadoFibra.concluido, gpon = null, wan = null, ppp = null, deviceInfo = null, erroMensagem = null)

    @Test
    fun `nova distribuicao segue a ordem narrativa identidade-status-topologia-wifi-por-banda-lan-dispositivos-acoes`() {
        composeRule.setContent {
            SignallQTheme {
                EquipamentoInternetScreen(
                    snapshotFibra = snapshotFibraConcluido(),
                    localDevice = localDeviceCompleto(),
                    natStatus = null,
                    modemHost = "192.168.1.1",
                    modemUsername = "admin",
                    modemPassword = "admin",
                    onVoltar = {},
                    onRetentar = {},
                    onAbrirAjustes = {},
                    onReiniciarEquipamento = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Regressão do bug #6: grid 2x2 antigo sumiu do card de status, saúde
        // óptica virou linha absorvida ("Sinal óptico: <veredito>"), sem a pill
        // solta de antes ("Sinal óptico bom", sem dois pontos).
        composeRule.onNodeWithText("Sinal óptico:", substring = true).assertExists()

        // Par 2-col Disponibilidade: Fibra e Wi-Fi renderizam em cards próprios,
        // os dois com "Disponível" (a fixture habilita as duas capacidades).
        // Esse par NÃO mudou nesta revisão (só o par de bandas Wi-Fi virou full).
        composeRule.onAllNodesWithText("Disponível").assertCountEquals(2)

        // "Wi-Fi" aparece 2x: o label do par Disponibilidade (item 4) e o
        // título do card técnico full-width (item 9-11) — não virou 2 cards
        // "Wi-Fi 2,4GHz"/"Wi-Fi 5/6GHz" como antes da revisão da Lia.
        composeRule.onAllNodesWithText("Wi-Fi").assertCountEquals(2)
        composeRule.onNodeWithText("Wi-Fi 2,4GHz").assertDoesNotExist()
        composeRule.onNodeWithText("Wi-Fi 5/6GHz").assertDoesNotExist()

        // As duas bandas viram linhas (label = SSID) dentro do MESMO card,
        // 2,4GHz antes de 5/6GHz — ordem preservada pelo mapper.
        composeRule.onNodeWithText("Casa_24").assertExists()
        composeRule.onNodeWithText("Casa_5G").assertExists()

        val arvore = composeRule.onRoot().printToString()
        val idxIdentidade = arvore.indexOf("TP-Link Archer C6")
        val idxStatus = arvore.indexOf("Conectado ao equipamento")
        val idxTopologia = arvore.indexOf("Como sua rede está conectada")
        val idxWifi24 = arvore.indexOf("Casa_24")
        val idxWifi56 = arvore.indexOf("Casa_5G")
        val idxLan = arvore.indexOf("Rede local (LAN)")
        val idxDispositivos = arvore.indexOf("Dispositivos conectados")
        // LkSectionOverline aplica .uppercase() de verdade (design system) — o
        // texto renderizado não é "Ações disponíveis", é "AÇÕES DISPONÍVEIS".
        val idxAcoes = arvore.indexOf("AÇÕES DISPONÍVEIS")

        listOf(
            "identidade" to idxIdentidade,
            "status" to idxStatus,
            "topologia" to idxTopologia,
            "wifi 2,4GHz (linha Casa_24)" to idxWifi24,
            "wifi 5/6GHz (linha Casa_5G)" to idxWifi56,
            "LAN" to idxLan,
            "dispositivos" to idxDispositivos,
            "ações" to idxAcoes,
        ).forEach { (nome, idx) -> assert(idx >= 0) { "Marcador '$nome' não encontrado na árvore renderizada." } }

        assert(idxIdentidade < idxStatus) { "Identidade deveria vir antes do Status." }
        assert(idxStatus < idxTopologia) { "Status (com Disponibilidade/Uso/Alerta/Aviso) deveria vir antes da Topologia." }
        assert(idxTopologia < idxWifi24) { "Topologia deveria vir antes dos módulos técnicos (Wi-Fi)." }
        assert(idxWifi24 < idxWifi56) { "Linha da banda 2,4GHz deveria vir antes da linha 5/6GHz, dentro do mesmo card Wi-Fi." }
        assert(idxWifi56 < idxLan) { "Wi-Fi deveria vir antes do módulo full de LAN." }
        assert(idxLan < idxDispositivos) { "Módulos técnicos deveriam vir antes do resumo de Dispositivos conectados." }
        assert(idxDispositivos < idxAcoes) { "Dispositivos conectados deveria vir antes de Ações disponíveis." }
    }
}
