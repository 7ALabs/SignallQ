package io.signallq.app.ui.component

import io.signallq.app.core.network.contracts.gateway.EquipmentClassification
import io.signallq.app.core.network.contracts.localdevice.ClientSnapshot
import io.signallq.app.core.network.contracts.localdevice.DataFreshness
import io.signallq.app.core.network.contracts.localdevice.DeviceCapabilities
import io.signallq.app.core.network.contracts.localdevice.DeviceType
import io.signallq.app.core.network.contracts.localdevice.DeviceWarning
import io.signallq.app.core.network.contracts.localdevice.DeviceWarningType
import io.signallq.app.core.network.contracts.localdevice.FiberSnapshot
import io.signallq.app.core.network.contracts.localdevice.LanSnapshot
import io.signallq.app.core.network.contracts.localdevice.LocalNetworkDeviceSnapshot
import io.signallq.app.core.network.contracts.localdevice.SupportLevel
import io.signallq.app.core.network.contracts.localdevice.WanSnapshot
import io.signallq.app.core.network.contracts.localdevice.WifiRadioSnapshot
import io.signallq.app.core.network.contracts.localdevice.WifiSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import io.signallq.app.core.network.contracts.gateway.DeviceType as GatewayDeviceType
import io.signallq.app.core.network.contracts.gateway.SupportLevel as GatewaySupportLevel

/** GH#544 — cobre os 7 estados obrigatorios da secao "Equipamento local" e a
 *  regra critica de capabilities: Nokia mostra fibra, TP-Link/roteador nunca. */
class LocalDeviceSectionUiStateTest {
    private fun freshness() = DataFreshness(capturadoEmEpochMs = 1_000L)

    @Test
    fun `sem snapshot e sem descoberta vira NenhumEncontrado`() {
        val estado = mapLocalDeviceSectionUiState(snapshot = null)
        assertEquals(LocalDeviceSectionUiState.NenhumEncontrado, estado)
    }

    @Test
    fun `sem snapshot mas com fingerprint vira EncontradoNaoConectado`() {
        val descoberta =
            EquipmentClassification(
                deviceType = GatewayDeviceType.ROUTER,
                supportLevel = GatewaySupportLevel.LAB_VALIDATED,
                driverId = "tplink-archer-c20",
                fibraCapable = false,
                confidenceScore = 0.55,
            )
        val estado = mapLocalDeviceSectionUiState(snapshot = null, descoberta = descoberta)
        assertEquals(
            LocalDeviceSectionUiState.EncontradoNaoConectado(DeviceType.ROUTER, SupportLevel.LAB_VALIDATED),
            estado,
        )
    }

    @Test
    fun `warning LOGIN_FALHOU vira estado LoginFalhou com a mensagem do driver`() {
        val snapshot =
            LocalNetworkDeviceSnapshot(
                deviceType = DeviceType.ROUTER,
                supportLevel = SupportLevel.LAB_VALIDATED,
                capabilities = DeviceCapabilities(suportaWan = true),
                vendor = "TP-Link",
                modelo = "Archer C20",
                firmwareVersion = null,
                fiber = null,
                wan = null,
                wifi = null,
                lan = null,
                warnings = listOf(DeviceWarning(DeviceWarningType.LOGIN_FALHOU, "Usuário ou senha incorretos.")),
                freshness = freshness(),
            )
        val estado = mapLocalDeviceSectionUiState(snapshot)
        assertTrue(estado is LocalDeviceSectionUiState.LoginFalhou)
        assertEquals("Usuário ou senha incorretos.", (estado as LocalDeviceSectionUiState.LoginFalhou).mensagem)
    }

    @Test
    fun `deviceType UNKNOWN_UNSUPPORTED vira SemSuporte`() {
        val snapshot =
            LocalNetworkDeviceSnapshot(
                deviceType = DeviceType.UNKNOWN_UNSUPPORTED,
                supportLevel = SupportLevel.UNKNOWN,
                capabilities = DeviceCapabilities(),
                vendor = null,
                modelo = null,
                firmwareVersion = null,
                fiber = null,
                wan = null,
                wifi = null,
                lan = null,
                freshness = freshness(),
            )
        val estado = mapLocalDeviceSectionUiState(snapshot)
        assertEquals(LocalDeviceSectionUiState.SemSuporte(DeviceType.UNKNOWN_UNSUPPORTED), estado)
    }

    @Test
    fun `Nokia ONT_GPON com fibra suportada mostra secao de fibra`() {
        val snapshot = nokiaSnapshotCompleto()
        val estado = mapLocalDeviceSectionUiState(snapshot)
        assertTrue(estado is LocalDeviceSectionUiState.Conectado)
        val conectado = estado as LocalDeviceSectionUiState.Conectado
        assertTrue(conectado.secoes.any { it.titulo == "Fibra óptica" })
        assertTrue(conectado.completo)
        assertFalse(conectado.experimental)
    }

    @Test
    fun `TP-Link roteador nunca mostra secao de fibra mesmo conectado`() {
        val snapshot = tplinkSnapshotCompleto()
        val estado = mapLocalDeviceSectionUiState(snapshot)
        assertTrue(estado is LocalDeviceSectionUiState.Conectado)
        val conectado = estado as LocalDeviceSectionUiState.Conectado
        assertFalse(conectado.secoes.any { it.titulo == "Fibra óptica" })
        assertTrue(conectado.secoes.any { it.titulo == "Internet (WAN)" })
        assertTrue(conectado.secoes.any { it.titulo == "Wi-Fi" })
    }

    @Test
    fun `driver PARSER_IMPORTED marca estado como experimental`() {
        val snapshot = tplinkSnapshotCompleto().copy(supportLevel = SupportLevel.PARSER_IMPORTED)
        val estado = mapLocalDeviceSectionUiState(snapshot) as LocalDeviceSectionUiState.Conectado
        assertTrue(estado.experimental)
    }

    @Test
    fun `capability declarada sem secao lida conta como dados parciais`() {
        val snapshot = tplinkSnapshotCompleto().copy(lan = null)
        val estado = mapLocalDeviceSectionUiState(snapshot) as LocalDeviceSectionUiState.Conectado
        assertFalse(estado.completo)
    }

    @Test
    fun `fibra com link inativo gera resumo critico`() {
        val snapshot =
            nokiaSnapshotCompleto().copy(
                fiber =
                    FiberSnapshot(
                        linkAtivo = false,
                        rxPowerDbm = null,
                        txPowerDbm = null,
                        temperaturaCelsius = null,
                        tensaoV = null,
                        correnteLaserMa = null,
                        serialOnt = null,
                    ),
            )
        val estado = mapLocalDeviceSectionUiState(snapshot) as LocalDeviceSectionUiState.Conectado
        assertEquals("Fibra desconectada", estado.resumoTitulo)
    }

    private fun nokiaSnapshotCompleto() =
        LocalNetworkDeviceSnapshot(
            deviceType = DeviceType.ONT_GPON,
            supportLevel = SupportLevel.LAB_VALIDATED,
            capabilities = DeviceCapabilities(suportaFibra = true, suportaWan = true),
            vendor = "Nokia",
            modelo = "G-1425G-B",
            firmwareVersion = "1.0.0",
            fiber =
                FiberSnapshot(
                    linkAtivo = true,
                    rxPowerDbm = -19.8,
                    txPowerDbm = 2.1,
                    temperaturaCelsius = 45.0,
                    tensaoV = null,
                    correnteLaserMa = null,
                    serialOnt = "ALCLXXXX",
                ),
            wan =
                WanSnapshot(
                    ipExterno = "203.0.113.10",
                    gateway = "203.0.113.1",
                    dnsPrimario = "8.8.8.8",
                    dnsSecundario = null,
                    tipoConexao = "DHCP",
                    nomeInterface = "eth_wan",
                    uptimeSegundos = 3600,
                ),
            wifi = null,
            lan = null,
            freshness = freshness(),
        )

    private fun tplinkSnapshotCompleto() =
        LocalNetworkDeviceSnapshot(
            deviceType = DeviceType.ROUTER,
            supportLevel = SupportLevel.LAB_VALIDATED,
            capabilities =
                DeviceCapabilities(
                    suportaFibra = false,
                    suportaWan = true,
                    suportaWifi = true,
                    suportaLan = true,
                    suportaClientes = true,
                ),
            vendor = "TP-Link",
            modelo = "Archer C20",
            firmwareVersion = "1.2.3",
            fiber = null,
            wan =
                WanSnapshot(
                    ipExterno = "192.0.2.5",
                    gateway = "192.0.2.1",
                    dnsPrimario = "1.1.1.1",
                    dnsSecundario = null,
                    tipoConexao = "DHCP",
                    nomeInterface = "wan",
                    uptimeSegundos = 7200,
                ),
            wifi =
                WifiSnapshot(
                    radios =
                        listOf(
                            WifiRadioSnapshot(
                                banda = "5 GHz",
                                ssid = "Casa_5G",
                                canal = 44,
                                larguraCanal = "80MHz",
                                potenciaTx = "alta",
                                criptografia = "WPA2",
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
            clientes = listOf(ClientSnapshot(mac = "AA:BB:CC:DD:EE:FF", ip = "192.168.1.50", hostname = "notebook", tipoConexao = "wifi")),
            freshness = freshness(),
        )
}
