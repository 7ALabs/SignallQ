package io.signallq.app.core.network.contracts.localdevice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes de [LocalDeviceSafeFilter] — cobre os critérios de aceite de GH#541:
 * IA/analytics/logs só podem receber o payload filtrado, nunca o snapshot
 * bruto com MAC/IP completos ou lista crua de clientes.
 */
class LocalDeviceSafeFilterTest {

    private fun ontComFibraAtiva() = LocalNetworkDeviceSnapshot(
        deviceType = DeviceType.ONT_GPON,
        supportLevel = SupportLevel.LAB_VALIDATED,
        capabilities = DeviceCapabilities(
            suportaFibra = true,
            suportaWan = true,
            suportaLan = true,
        ),
        vendor = "Nokia",
        modelo = "G-1425G-B",
        firmwareVersion = "3FE49362BLGH31",
        fiber = FiberSnapshot(
            linkAtivo = true,
            rxPowerDbm = -18.5,
            txPowerDbm = 2.1,
            temperaturaCelsius = 45.0,
            tensaoV = 3.3,
            correnteLaserMa = 12.0,
            serialOnt = "ALCLXXXXXXXX",
        ),
        wan = WanSnapshot(
            ipExterno = "203.0.113.10",
            gateway = "192.168.1.254",
            dnsPrimario = "8.8.8.8",
            dnsSecundario = "8.8.4.4",
            tipoConexao = "pppoe",
            nomeInterface = "ppp0",
            uptimeSegundos = 3600,
        ),
        wifi = null,
        lan = LanSnapshot(
            ipRoteador = "192.168.1.254",
            mascara = "255.255.255.0",
            dhcpHabilitado = true,
            faixaDhcpInicio = "192.168.1.100",
            faixaDhcpFim = "192.168.1.200",
        ),
        clientes = emptyList(),
        warnings = emptyList(),
        freshness = DataFreshness(capturadoEmEpochMs = 1_000_000L),
    )

    private fun roteadorComClientes() = LocalNetworkDeviceSnapshot(
        deviceType = DeviceType.ROUTER,
        supportLevel = SupportLevel.LAB_VALIDATED,
        capabilities = DeviceCapabilities(
            suportaFibra = false,
            suportaWan = true,
            suportaWifi = true,
            suportaLan = true,
            suportaClientes = true,
        ),
        vendor = "TP-Link",
        modelo = "Archer C6",
        firmwareVersion = "1.1.10 Build 20230830",
        fiber = null,
        wan = WanSnapshot(
            ipExterno = null,
            gateway = "192.168.1.254",
            dnsPrimario = null,
            dnsSecundario = null,
            tipoConexao = "dhcp",
            nomeInterface = null,
            uptimeSegundos = null,
        ),
        wifi = WifiSnapshot(
            radios = listOf(
                WifiRadioSnapshot(
                    banda = "2.4GHz",
                    ssid = "MinhaRede_2G",
                    canal = 10,
                    larguraCanal = "20MHz",
                    potenciaTx = "high",
                    criptografia = "psk",
                    habilitado = true,
                ),
            ),
        ),
        lan = LanSnapshot(
            ipRoteador = "192.168.0.1",
            mascara = "255.255.255.0",
            dhcpHabilitado = true,
            faixaDhcpInicio = "192.168.0.100",
            faixaDhcpFim = "192.168.0.200",
        ),
        clientes = listOf(
            ClientSnapshot(mac = "AA:BB:CC:DD:EE:FF", ip = "192.168.0.50", hostname = "notebook", tipoConexao = "wifi_5g"),
            ClientSnapshot(mac = "11:22:33:44:55:66", ip = "192.168.0.51", hostname = "celular", tipoConexao = "wifi_2g"),
        ),
        warnings = emptyList(),
        freshness = DataFreshness(capturadoEmEpochMs = 2_000_000L),
    )

    @Test
    fun `payload filtrado mantem apenas campos da allowlist`() {
        val safe = LocalDeviceSafeFilter.filtrar(ontComFibraAtiva())

        assertEquals("Nokia", safe.vendor)
        assertEquals("G-1425G-B", safe.modelo)
        assertEquals("3FE49362BLGH31", safe.firmwareVersion)
        assertEquals(DeviceType.ONT_GPON, safe.deviceType)
        assertEquals(SupportLevel.LAB_VALIDATED, safe.supportLevel)
        assertEquals(1_000_000L, safe.coletadoEmEpochMs)
        assertEquals(LocalDeviceSectionStatus.OK, safe.statusFibra)
        assertEquals(LocalDeviceSectionStatus.OK, safe.statusWan)
        assertEquals(LocalDeviceSectionStatus.NAO_SUPORTADO, safe.statusWifi)
        assertEquals(LocalDeviceSectionStatus.OK, safe.statusLan)
        assertEquals(LocalDeviceSectionStatus.OK, safe.connectionStatus)
    }

    @Test
    fun `payload filtrado nunca expoe MAC ou IP completo dos clientes`() {
        val safe = LocalDeviceSafeFilter.filtrar(roteadorComClientes())

        assertEquals(2, safe.quantidadeClientes)

        val camposDoPayload = SafeLocalDeviceContext::class.java.declaredFields
        assertFalse(
            "payload seguro nao deve ter lista crua de ClientSnapshot (com MAC/IP)",
            camposDoPayload.any { it.genericType.toString().contains("ClientSnapshot") },
        )
        val nomesDosCampos = camposDoPayload.map { it.name.lowercase() }
        assertFalse("payload seguro nao deve ter campo mac", nomesDosCampos.any { it.contains("mac") })
        assertFalse("payload seguro nao deve ter campo de ip", nomesDosCampos.any { it == "ip" || it.contains("ipexterno") || it.contains("iproteador") })
    }

    @Test
    fun `payload filtrado nao expoe serial da ont nem dados brutos de fibra`() {
        val safe = LocalDeviceSafeFilter.filtrar(ontComFibraAtiva())

        val camposDoPayload = SafeLocalDeviceContext::class.java.declaredFields.map { it.name.lowercase() }
        val termosProibidos = listOf(
            "serial", "senha", "password", "cookie", "token", "sessao",
            "rxpower", "txpower", "temperatura", "tensao", "corrente", "dns", "gateway",
        )
        camposDoPayload.forEach { nome ->
            termosProibidos.forEach { termo ->
                assertFalse("SafeLocalDeviceContext.$nome parece expor dado bruto/sensivel ($termo)", nome.contains(termo))
            }
        }
    }

    @Test
    fun `fibra sem leitura vira INDISPONIVEL, nao excecao`() {
        val snapshot = ontComFibraAtiva().copy(fiber = null)
        val safe = LocalDeviceSafeFilter.filtrar(snapshot)
        assertEquals(LocalDeviceSectionStatus.INDISPONIVEL, safe.statusFibra)
    }

    @Test
    fun `fibra com link inativo vira ATENCAO`() {
        val snapshot = ontComFibraAtiva().let {
            it.copy(fiber = it.fiber!!.copy(linkAtivo = false))
        }
        val safe = LocalDeviceSafeFilter.filtrar(snapshot)
        assertEquals(LocalDeviceSectionStatus.ATENCAO, safe.statusFibra)
    }

    @Test
    fun `erro de comunicacao no snapshot vira connectionStatus INDISPONIVEL`() {
        val snapshot = roteadorComClientes().copy(
            warnings = listOf(
                DeviceWarning(
                    type = DeviceWarningType.ERRO_COMUNICACAO,
                    mensagem = "Não foi possível conectar ao equipamento. Você pode continuar sem esses dados.",
                ),
            ),
        )
        val safe = LocalDeviceSafeFilter.filtrar(snapshot)
        assertEquals(LocalDeviceSectionStatus.INDISPONIVEL, safe.connectionStatus)
        assertEquals(1, safe.warnings.size)
    }

    @Test
    fun `secao nao suportada pelo equipamento vira NAO_SUPORTADO`() {
        val safe = LocalDeviceSafeFilter.filtrar(roteadorComClientes())
        assertEquals(LocalDeviceSectionStatus.NAO_SUPORTADO, safe.statusFibra)
    }
}
