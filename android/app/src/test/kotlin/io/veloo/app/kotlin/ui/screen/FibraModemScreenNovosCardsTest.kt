package io.signallq.app.ui.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import io.signallq.app.feature.fibra.DeviceInfoFibra
import io.signallq.app.feature.fibra.EstadoFibra
import io.signallq.app.feature.fibra.GponStatus
import io.signallq.app.feature.fibra.SnapshotFibra
import io.signallq.app.feature.fibra.WanStatus
import io.signallq.app.feature.fibra.WifiRadioStatus
import io.signallq.app.feature.fibra.WifiStatus
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes de UI dos 3 cards novos do FibraModemScreen (GH#893): Tempo ligado,
 * Rede Wi-Fi (por rádio) e DNS. Segue o padrão Robolectric+Compose.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FibraModemScreenNovosCardsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val gponSaudavel =
        GponStatus(
            status = "up",
            mode = "GPON",
            rxPowerDbm = -18.0,
            txPowerDbm = 2.0,
            temperatureCelsius = 45.0,
            serial = "ABCD1234",
            voltageV = 3.3,
            laserCurrentMa = 10.0,
        )

    private fun renderScreen(
        deviceInfo: DeviceInfoFibra? = null,
        wifi: WifiStatus? = null,
        wan: WanStatus? = null,
    ) {
        val snapshot =
            SnapshotFibra(
                estado = EstadoFibra.concluido,
                gpon = gponSaudavel,
                wan = wan,
                ppp = null,
                deviceInfo = deviceInfo,
                erroMensagem = null,
                wifi = wifi,
            )
        composeRule.setContent {
            SignallQTheme {
                FibraModemScreen(
                    snapshotFibra = snapshot,
                    onVoltar = {},
                    onRetentar = {},
                    onAbrirAjustes = {},
                )
            }
        }
    }

    // ── Tempo ligado ────────────────────────────────────────────────────────

    @Test
    fun `card tempo ligado mostra uptime formatado`() {
        renderScreen(deviceInfo = deviceInfoComUptime(uptimeSeconds = 3 * 86400 + 4 * 3600))

        composeRule.onNodeWithText("Tempo ligado").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("3d 04h").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `card tempo ligado nao aparece sem deviceInfo`() {
        renderScreen(deviceInfo = null)

        composeRule.onNodeWithText("Tempo ligado").assertDoesNotExist()
    }

    @Test
    fun `card tempo ligado nao aparece com uptime zerado`() {
        renderScreen(deviceInfo = deviceInfoComUptime(uptimeSeconds = 0))

        composeRule.onNodeWithText("Tempo ligado").assertDoesNotExist()
    }

    // ── Rede Wi-Fi ──────────────────────────────────────────────────────────

    @Test
    fun `card wifi protegido mostra badge Ativa e descricao traduzida`() {
        renderScreen(
            wifi =
                WifiStatus(
                    radios =
                        listOf(
                            WifiRadioStatus(
                                banda = "2.4GHz",
                                ssid = "CasaWifi",
                                canal = 6,
                                habilitado = true,
                                criptografia = "WPAand11i",
                                potenciaTx = "100%",
                            ),
                        ),
                ),
        )

        composeRule.onNodeWithText("CasaWifi").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2.4GHz · Canal 6 · Wi-Fi protegida").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Ativa").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `card wifi aberto usa badge de atencao Sem senha`() {
        renderScreen(
            wifi =
                WifiStatus(
                    radios =
                        listOf(
                            WifiRadioStatus(
                                banda = "5GHz",
                                ssid = "RedeAberta",
                                canal = 44,
                                habilitado = true,
                                criptografia = "None",
                                potenciaTx = "80%",
                            ),
                        ),
                ),
        )

        composeRule.onNodeWithText("5GHz · Canal 44 · Rede aberta, sem senha").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sem senha").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `um card por radio quando 2_4 e 5GHz presentes`() {
        renderScreen(
            wifi =
                WifiStatus(
                    radios =
                        listOf(
                            WifiRadioStatus("2.4GHz", "CasaWifi", 6, true, "11i", "100%"),
                            WifiRadioStatus("5GHz", "CasaWifi_5G", 44, true, "11i", "80%"),
                        ),
                ),
        )

        composeRule.onNodeWithText("CasaWifi").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("CasaWifi_5G").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `secao wifi nao aparece sem dado`() {
        renderScreen(wifi = null)

        composeRule.onNodeWithText("Servidor de nomes").assertDoesNotExist()
    }

    // ── DNS ─────────────────────────────────────────────────────────────────

    @Test
    fun `card dns configurado mostra badge e ips na legenda`() {
        renderScreen(
            wan =
                WanStatus(
                    externalIp = "200.1.2.3",
                    gateway = "200.1.2.1",
                    primaryDns = "8.8.8.8",
                    secondaryDns = "8.8.4.4",
                    vlanId = "100",
                    interfaceName = "eth0",
                    pppoeConcentrator = "—",
                    connectionType = "PPPoE",
                    connectionUptimeSeconds = 3600,
                ),
        )

        composeRule.onNodeWithText("Servidor de nomes").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Configurado").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("8.8.8.8 · 8.8.4.4").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `card dns nao configurado quando ips vazios`() {
        renderScreen(
            wan =
                WanStatus(
                    externalIp = "200.1.2.3",
                    gateway = "200.1.2.1",
                    primaryDns = "",
                    secondaryDns = "",
                    vlanId = "100",
                    interfaceName = "eth0",
                    pppoeConcentrator = "—",
                    connectionType = "PPPoE",
                    connectionUptimeSeconds = 3600,
                ),
        )

        composeRule.onNodeWithText("Servidor de nomes").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Não configurado").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `card dns nao aparece sem wan`() {
        renderScreen(wan = null)

        composeRule.onNodeWithText("Servidor de nomes").assertDoesNotExist()
    }

    private fun deviceInfoComUptime(uptimeSeconds: Int) =
        DeviceInfoFibra(
            model = "G-1425G-B",
            manufacturer = "Nokia",
            serialNumber = "SN123",
            firmwareVersion = "1.0",
            hardwareVersion = "1.0",
            uptimeSeconds = uptimeSeconds,
        )
}
