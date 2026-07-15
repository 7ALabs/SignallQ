package io.signallq.app.feature.fibra

import io.signallq.app.core.network.contracts.localdevice.DeviceType
import io.signallq.app.core.network.contracts.localdevice.SupportLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NokiaLocalDeviceMapperTest {

    private val gpon = GponStatus(
        status = "up",
        mode = "VlanMuxMode",
        rxPowerDbm = -19.8,
        txPowerDbm = 2.1,
        temperatureCelsius = 48.5,
        serial = "ALCL12345678",
        voltageV = 3.25,
        laserCurrentMa = 13.99,
    )

    private val wan = WanStatus(
        externalIp = "203.0.113.44",
        gateway = "10.0.0.1",
        primaryDns = "8.8.8.8",
        secondaryDns = "8.8.4.4",
        vlanId = "1002",
        interfaceName = "ipoe_eth0",
        pppoeConcentrator = "—",
        connectionType = "IP_Routed",
        connectionUptimeSeconds = 3600,
    )

    private val deviceInfo = DeviceInfoFibra(
        model = "G-1425G-B",
        manufacturer = "ALCL",
        serialNumber = "ALCL12345678",
        firmwareVersion = "3FE49568IJJJ09",
        hardwareVersion = "3FE49937ADAA",
        uptimeSeconds = 500_000,
    )

    private val wifi = WifiStatus(
        radios = listOf(
            WifiRadioStatus(
                banda = "2.4GHz",
                ssid = "CasaWifi",
                canal = 6,
                habilitado = true,
                criptografia = "WPAand11i",
                potenciaTx = "100%",
            ),
        ),
    )

    private val lan = LanStatus(
        routerIp = "192.168.1.254",
        subnetMask = "255.255.255.0",
        dhcpHabilitado = true,
        dhcpFaixaInicio = "192.168.1.100",
        dhcpFaixaFim = "192.168.1.200",
    )

    @Test
    fun `map converte snapshot completo com wifi e lan presentes`() {
        val snapshot = SnapshotFibra(
            estado = EstadoFibra.concluido,
            gpon = gpon,
            wan = wan,
            ppp = null,
            deviceInfo = deviceInfo,
            erroMensagem = null,
            wifi = wifi,
            lan = lan,
        )

        val resultado = NokiaLocalDeviceMapper.map(snapshot, capturadoEmEpochMs = 1_720_000_000_000L)

        requireNotNull(resultado)
        assertEquals(DeviceType.ONT_GPON, resultado.deviceType)
        assertEquals(SupportLevel.LAB_VALIDATED, resultado.supportLevel)
        assertEquals("Nokia", resultado.vendor)
        assertEquals("G-1425G-B", resultado.modelo)

        assertTrue(resultado.capabilities.suportaFibra)
        assertTrue(resultado.capabilities.suportaWan)
        assertTrue(resultado.capabilities.suportaWifi)
        assertTrue(resultado.capabilities.suportaLan)
        assertTrue(resultado.capabilities.suportaClientes)
        assertTrue(resultado.capabilities.suportaGerenciamento)

        requireNotNull(resultado.fiber)
        assertEquals(true, resultado.fiber?.linkAtivo)
        assertEquals(-19.8, resultado.fiber?.rxPowerDbm)

        requireNotNull(resultado.wan)
        assertEquals("203.0.113.44", resultado.wan?.ipExterno)

        requireNotNull(resultado.wifi)
        assertEquals(1, resultado.wifi?.radios?.size)
        assertEquals("CasaWifi", resultado.wifi?.radios?.first()?.ssid)
        assertEquals("2.4GHz", resultado.wifi?.radios?.first()?.banda)
        assertNull(resultado.wifi?.radios?.first()?.larguraCanal)

        requireNotNull(resultado.lan)
        assertEquals("192.168.1.254", resultado.lan?.ipRoteador)
        assertEquals(true, resultado.lan?.dhcpHabilitado)

        assertTrue(resultado.clientes.isEmpty())
        assertEquals(1_720_000_000_000L, resultado.freshness.capturadoEmEpochMs)
        assertEquals(false, resultado.freshness.expirado)
    }

    @Test
    fun `map produz wifi e lan nulos quando a leitura nao trouxe esses dados`() {
        val snapshot = SnapshotFibra(
            estado = EstadoFibra.concluido,
            gpon = gpon,
            wan = wan,
            ppp = null,
            deviceInfo = deviceInfo,
            erroMensagem = null,
            wifi = null,
            lan = null,
        )

        val resultado = NokiaLocalDeviceMapper.map(snapshot, capturadoEmEpochMs = 1_720_000_000_000L)

        requireNotNull(resultado)
        // Capability continua true (o driver Nokia sabe ler Wi-Fi/LAN) mesmo
        // que esta leitura especifica nao tenha trazido o dado — a UI trata
        // capability=true + secao=null como leitura parcial, nao como erro.
        assertTrue(resultado.capabilities.suportaWifi)
        assertTrue(resultado.capabilities.suportaLan)
        assertNull(resultado.wifi)
        assertNull(resultado.lan)
    }

    @Test
    fun `map retorna null quando o estado nao e concluido`() {
        val snapshot = SnapshotFibra(
            estado = EstadoFibra.erro,
            gpon = null,
            wan = null,
            ppp = null,
            deviceInfo = null,
            erroMensagem = "erroModemInacessivel",
        )

        assertNull(NokiaLocalDeviceMapper.map(snapshot, capturadoEmEpochMs = 0L))
    }

    @Test
    fun `map retorna null quando concluido mas sem dado optico`() {
        val snapshot = SnapshotFibra(
            estado = EstadoFibra.concluido,
            gpon = null,
            wan = wan,
            ppp = null,
            deviceInfo = deviceInfo,
            erroMensagem = null,
        )

        assertNull(NokiaLocalDeviceMapper.map(snapshot, capturadoEmEpochMs = 0L))
    }
}
