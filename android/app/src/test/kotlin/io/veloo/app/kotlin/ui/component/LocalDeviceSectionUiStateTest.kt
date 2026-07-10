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
import io.signallq.app.feature.diagnostico.DiagnosticStatus
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

    private fun wifiItemDoRadio(radio: WifiRadioSnapshot): EquipamentoItemTecnico {
        val snapshot = tplinkSnapshotCompleto().copy(wifi = WifiSnapshot(radios = listOf(radio)))
        val estado = mapLocalDeviceSectionUiState(snapshot) as LocalDeviceSectionUiState.Conectado
        return estado.secoes
            .first { it.titulo == "Wi-Fi" }
            .itens
            .first()
    }

    @Test
    fun `radio com todos os campos monta a linha completa traduzida sem cor especial`() {
        val item =
            wifiItemDoRadio(
                WifiRadioSnapshot(
                    banda = "5 GHz",
                    ssid = "Casa_5G",
                    canal = 44,
                    larguraCanal = "80MHz",
                    potenciaTx = "alta",
                    criptografia = "WPA2",
                    habilitado = true,
                ),
            )
        assertEquals("5 GHz · canal 44 · WPA2 · 80 MHz · Potência alta", item.valor)
        assertEquals(null, item.statusValor)
    }

    @Test
    fun `rede aberta vira Sem senha com status critico`() {
        val item =
            wifiItemDoRadio(
                WifiRadioSnapshot(
                    banda = "2.4 GHz",
                    ssid = "Casa_2G",
                    canal = 6,
                    larguraCanal = null,
                    potenciaTx = null,
                    criptografia = "none",
                    habilitado = true,
                ),
            )
        assertTrue(item.valor.contains("Sem senha"))
        assertEquals(DiagnosticStatus.critical, item.statusValor)
    }

    @Test
    fun `WEP vira status de atencao`() {
        val item =
            wifiItemDoRadio(
                WifiRadioSnapshot(
                    banda = "2.4 GHz",
                    ssid = "Casa_2G",
                    canal = 6,
                    larguraCanal = null,
                    potenciaTx = null,
                    criptografia = "WEP",
                    habilitado = true,
                ),
            )
        assertTrue(item.valor.endsWith("WEP"))
        assertEquals(DiagnosticStatus.attention, item.statusValor)
    }

    @Test
    fun `psk isolado sem wpa no texto vira WPA2 neutro`() {
        val item =
            wifiItemDoRadio(
                WifiRadioSnapshot(
                    banda = "5 GHz",
                    ssid = "Casa_5G",
                    canal = 44,
                    larguraCanal = null,
                    potenciaTx = null,
                    criptografia = "psk",
                    habilitado = true,
                ),
            )
        assertTrue(item.valor.endsWith("WPA2"))
        assertEquals(null, item.statusValor)
    }

    @Test
    fun `criptografia nao reconhecida mostra rotulo generico sem cor`() {
        val item =
            wifiItemDoRadio(
                WifiRadioSnapshot(
                    banda = "5 GHz",
                    ssid = "Casa_5G",
                    canal = 44,
                    larguraCanal = null,
                    potenciaTx = null,
                    criptografia = "vendor-proprietary-xyz",
                    habilitado = true,
                ),
            )
        assertTrue(item.valor.endsWith("Segurança não identificada"))
        assertEquals(null, item.statusValor)
    }

    @Test
    fun `radio desligado suprime seguranca largura e potencia`() {
        val item =
            wifiItemDoRadio(
                WifiRadioSnapshot(
                    banda = "5 GHz",
                    ssid = "Casa_5G",
                    canal = 44,
                    larguraCanal = "80MHz",
                    potenciaTx = "alta",
                    criptografia = "WPA2",
                    habilitado = false,
                ),
            )
        assertEquals("5 GHz · canal 44 · desligado", item.valor)
        assertEquals(null, item.statusValor)
    }

    @Test
    fun `campos ausentes sao omitidos sem traco`() {
        val item =
            wifiItemDoRadio(
                WifiRadioSnapshot(
                    banda = "5 GHz",
                    ssid = "Casa_5G",
                    canal = 44,
                    larguraCanal = null,
                    potenciaTx = null,
                    criptografia = null,
                    habilitado = true,
                ),
            )
        assertEquals("5 GHz · canal 44", item.valor)
        assertFalse(item.valor.contains("—"))
    }

    @Test
    fun `largura de canal em formato inesperado mostra valor bruto`() {
        val item =
            wifiItemDoRadio(
                WifiRadioSnapshot(
                    banda = "5 GHz",
                    ssid = "Casa_5G",
                    canal = 44,
                    larguraCanal = "faixa-dupla",
                    potenciaTx = null,
                    criptografia = null,
                    habilitado = true,
                ),
            )
        assertTrue(item.valor.endsWith("faixa-dupla"))
    }

    @Test
    fun `potencia com percentual e dBm sao traduzidas com unidade`() {
        val itemPercentual =
            wifiItemDoRadio(
                WifiRadioSnapshot(
                    banda = "5 GHz",
                    ssid = "Casa_5G",
                    canal = 44,
                    larguraCanal = null,
                    potenciaTx = "75%",
                    criptografia = null,
                    habilitado = true,
                ),
            )
        assertTrue(itemPercentual.valor.endsWith("Potência: 75% do máximo"))

        val itemDbm =
            wifiItemDoRadio(
                WifiRadioSnapshot(
                    banda = "5 GHz",
                    ssid = "Casa_5G",
                    canal = 44,
                    larguraCanal = null,
                    potenciaTx = "20dBm",
                    criptografia = null,
                    habilitado = true,
                ),
            )
        assertTrue(itemDbm.valor.endsWith("Potência: 20 dBm"))
    }

    private fun lanItens(lan: LanSnapshot?): List<EquipamentoItemTecnico> {
        val snapshot = tplinkSnapshotCompleto().copy(lan = lan)
        val estado = mapLocalDeviceSectionUiState(snapshot) as LocalDeviceSectionUiState.Conectado
        return estado.secoes.first { it.titulo == "Rede local (LAN)" }.itens
    }

    @Test
    fun `mascara de sub-rede 255-255-255-0 vira barra 24`() {
        val itens = lanItens(tplinkSnapshotCompleto().lan)
        val item = itens.first { it.label == "Máscara de sub-rede" }
        assertEquals("255.255.255.0 · /24", item.valor)
    }

    @Test
    fun `mascara de sub-rede malformada mostra valor bruto sem sufixo quebrado`() {
        val lan =
            LanSnapshot(
                ipRoteador = "192.168.1.1",
                mascara = "algo-invalido",
                dhcpHabilitado = true,
                faixaDhcpInicio = "192.168.1.100",
                faixaDhcpFim = "192.168.1.200",
            )
        val item = lanItens(lan).first { it.label == "Máscara de sub-rede" }
        assertEquals("algo-invalido", item.valor)
        assertFalse(item.valor.contains("null"))
        assertFalse(item.valor.contains("NaN"))
    }

    @Test
    fun `faixa de dhcp ativa mostra inicio e fim sem mascaramento`() {
        val itens = lanItens(tplinkSnapshotCompleto().lan)
        val item = itens.first { it.label == "Faixa de DHCP" }
        assertEquals("192.168.1.100 – 192.168.1.200", item.valor)
    }

    @Test
    fun `faixa de dhcp desligado nao mostra a linha mesmo com valores residuais`() {
        val lan =
            LanSnapshot(
                ipRoteador = "192.168.1.1",
                mascara = "255.255.255.0",
                dhcpHabilitado = false,
                faixaDhcpInicio = "192.168.1.100",
                faixaDhcpFim = "192.168.1.200",
            )
        val itens = lanItens(lan)
        assertFalse(itens.any { it.label == "Faixa de DHCP" })
    }

    @Test
    fun `faixa de dhcp parcial e omitida`() {
        val lan =
            LanSnapshot(
                ipRoteador = "192.168.1.1",
                mascara = "255.255.255.0",
                dhcpHabilitado = true,
                faixaDhcpInicio = "192.168.1.100",
                faixaDhcpFim = null,
            )
        val itens = lanItens(lan)
        assertFalse(itens.any { it.label == "Faixa de DHCP" })
    }

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

    private fun clientesSecaoDe(clientes: List<ClientSnapshot>): EquipamentoSecaoTecnica {
        val snapshot = tplinkSnapshotCompleto().copy(clientes = clientes)
        val estado = mapLocalDeviceSectionUiState(snapshot) as LocalDeviceSectionUiState.Conectado
        return estado.secoes.first { it.titulo == "Dispositivos conectados" }
    }

    @Test
    fun `cliente com hostname mostra hostname como titulo e IP mascarado como subtitulo`() {
        val secao =
            clientesSecaoDe(
                listOf(ClientSnapshot(mac = "AA:BB:CC:DD:EE:FF", ip = "192.168.1.50", hostname = "Notebook da Sala", tipoConexao = "wifi_5g")),
            )
        val cliente = secao.clientes.single()
        assertEquals("Notebook da Sala", cliente.titulo)
        assertEquals("192.168.1.*", cliente.ip)
        assertEquals("Wi-Fi 5 GHz", cliente.tipoLabel)
    }

    @Test
    fun `cliente sem hostname usa IP mascarado como titulo e omite subtitulo repetido`() {
        val secao =
            clientesSecaoDe(
                listOf(ClientSnapshot(mac = "AA:BB:CC:DD:EE:FF", ip = "192.168.1.77", hostname = null, tipoConexao = "wired")),
            )
        val cliente = secao.clientes.single()
        assertEquals("192.168.1.*", cliente.titulo)
        assertEquals(null, cliente.ip)
        assertEquals("Cabo (Ethernet)", cliente.tipoLabel)
    }

    @Test
    fun `cliente sem hostname e sem IP vira Dispositivo sem nome, nunca expoe o MAC cru`() {
        val secao =
            clientesSecaoDe(
                listOf(ClientSnapshot(mac = "AA:BB:CC:DD:EE:FF", ip = null, hostname = null, tipoConexao = null)),
            )
        val cliente = secao.clientes.single()
        assertEquals("Dispositivo sem nome", cliente.titulo)
        assertFalse(cliente.titulo.contains("AA:BB:CC"))
        assertEquals(null, cliente.tipoLabel)
    }

    @Test
    fun `tipoConexao nao reconhecido omite o texto do badge`() {
        val secao =
            clientesSecaoDe(
                listOf(ClientSnapshot(mac = "AA:BB:CC:DD:EE:FF", ip = "192.168.1.9", hostname = "TV", tipoConexao = "powerline")),
            )
        assertEquals(null, secao.clientes.single().tipoLabel)
    }

    @Test
    fun `overline usa singular para 1 dispositivo e plural para mais de 1`() {
        val umCliente = clientesSecaoDe(listOf(ClientSnapshot(null, "192.168.1.2", "PC", "wifi")))
        assertEquals("1 dispositivo conectado", umCliente.overline)

        val doisClientes =
            clientesSecaoDe(
                listOf(
                    ClientSnapshot(null, "192.168.1.2", "PC", "wifi"),
                    ClientSnapshot(null, "192.168.1.3", "TV", "wired"),
                ),
            )
        assertEquals("2 dispositivos conectados", doisClientes.overline)
    }

    @Test
    fun `lista vazia de clientes mostra Sem leitura nesta captura e nao afirma zero dispositivos`() {
        val secao = clientesSecaoDe(emptyList())
        assertEquals(null, secao.overline)
        assertTrue(secao.clientes.isEmpty())
        assertEquals(1, secao.itens.size)
        assertEquals("Sem leitura nesta captura", secao.itens.single().valor)
    }

    @Test
    fun `mais de 8 clientes trunca em 8 e mostra contador de excedente`() {
        val clientes = (1..11).map { ClientSnapshot(null, "192.168.1.$it", "Dispositivo $it", "wifi") }
        val secao = clientesSecaoDe(clientes)
        assertEquals(8, secao.clientes.size)
        assertEquals("+ 3 outros dispositivos", secao.trailing)
    }

    @Test
    fun `8 clientes ou menos nao mostra contador de excedente`() {
        val clientes = (1..8).map { ClientSnapshot(null, "192.168.1.$it", "Dispositivo $it", "wifi") }
        val secao = clientesSecaoDe(clientes)
        assertEquals(8, secao.clientes.size)
        assertEquals(null, secao.trailing)
    }
}
