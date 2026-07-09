package io.signallq.app.core.network.contracts.localdevice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes do contrato normalizado de equipamento local (GH#546).
 *
 * Não testa parser/driver nenhum (isso é escopo de outra issue) — só garante
 * que o contrato compila e representa corretamente os dois casos exigidos
 * pelos critérios de aceite: Nokia com fibra e TP-Link sem fibra.
 */
class LocalNetworkDeviceSnapshotTest {

    @Test
    fun `ONT Nokia produz snapshot com fibra preenchida`() {
        val snapshot = LocalNetworkDeviceSnapshot(
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
            lan = null,
            freshness = DataFreshness(capturadoEmEpochMs = 1_000_000L),
        )

        assertTrue(snapshot.capabilities.suportaFibra)
        assertEquals(DeviceType.ONT_GPON, snapshot.deviceType)
        val fiber = requireNotNull(snapshot.fiber)
        assertEquals(-18.5, fiber.rxPowerDbm)
    }

    @Test
    fun `roteador TP-Link produz snapshot com fiber nulo`() {
        val snapshot = LocalNetworkDeviceSnapshot(
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
            ),
            warnings = emptyList(),
            freshness = DataFreshness(capturadoEmEpochMs = 2_000_000L),
        )

        assertNull("TP-Link nunca produz dados de fibra", snapshot.fiber)
        assertEquals(DeviceType.ROUTER, snapshot.deviceType)
        assertTrue(snapshot.capabilities.suportaWifi)
        assertTrue(snapshot.wifi!!.radios.isNotEmpty())
    }

    @Test
    fun `dados parciais viram warning explicito, nao excecao`() {
        val snapshot = LocalNetworkDeviceSnapshot(
            deviceType = DeviceType.ROUTER,
            supportLevel = SupportLevel.PARSER_IMPORTED,
            capabilities = DeviceCapabilities(suportaWan = true),
            vendor = "TP-Link",
            modelo = null,
            firmwareVersion = null,
            fiber = null,
            wan = null,
            wifi = null,
            lan = null,
            warnings = listOf(
                DeviceWarning(
                    type = DeviceWarningType.LOGIN_FALHOU,
                    mensagem = "Não foi possível conectar ao equipamento. Você pode continuar sem esses dados.",
                ),
            ),
            freshness = DataFreshness(capturadoEmEpochMs = 3_000_000L, expirado = true),
        )

        assertEquals(1, snapshot.warnings.size)
        assertEquals(DeviceWarningType.LOGIN_FALHOU, snapshot.warnings.first().type)
        assertTrue(snapshot.freshness.expirado)
    }

    @Test
    fun `contrato nao expoe campos de credencial em nenhuma secao`() {
        // Verificação estrutural: os nomes de propriedade de cada seção não
        // podem conter senha/cookie/token — reforça a regra de segurança do
        // contrato via reflection Java (sem depender de kotlin-reflect), sem
        // depender de revisão manual futura.
        val termosProibidos = listOf("senha", "password", "cookie", "token", "sessao", "stok", "sysauth", "psk")

        val classes = listOf(
            LocalNetworkDeviceSnapshot::class.java,
            FiberSnapshot::class.java,
            WanSnapshot::class.java,
            WifiSnapshot::class.java,
            WifiRadioSnapshot::class.java,
            LanSnapshot::class.java,
            ClientSnapshot::class.java,
            DeviceWarning::class.java,
            DataFreshness::class.java,
            DeviceCapabilities::class.java,
        )

        classes.forEach { klass ->
            klass.declaredFields.map { it.name.lowercase() }.forEach { nome ->
                termosProibidos.forEach { termo ->
                    assertTrue(
                        "${klass.simpleName}.$nome parece carregar credencial ($termo)",
                        !nome.contains(termo),
                    )
                }
            }
        }
    }
}
