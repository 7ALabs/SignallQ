package io.signallq.app.core.nds

import io.signallq.app.core.diagnostico.ConnectionType
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DnsDiagnosticInput
import io.signallq.app.core.diagnostico.FibraDiagnosticInput
import io.signallq.app.core.diagnostico.InternetDiagnosticInput
import io.signallq.app.core.diagnostico.WifiDiagnosticInput
import io.signallq.app.core.diagnostico.WifiScanDiagnosticInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobertura da ponte `DiagnosticInput -> NdsDiagnosticsRequest` (NDS-02k, issue
 * #1759, item 4).
 */
class NdsDiagnosticsRequestMapperTest {

    @Test
    fun `input vazio gera request minimo, sem blocos opcionais`() {
        val input = DiagnosticInput(connectionType = ConnectionType.desconhecido, executionId = "exec-1")

        val request = input.toNdsDiagnosticsRequest(appVersion = "2.4.0")

        assertEquals("exec-1", request.requestId)
        assertEquals("io.signallq.app", request.app.id)
        assertEquals("2.4.0", request.app.version)
        assertNull(request.profile)
        assertEquals(listOf("scoring", "ai"), request.capabilities)
        assertEquals("UNKNOWN", request.connection?.type)
        assertNull(request.wifi)
        assertNull(request.wifiScan)
        assertNull(request.speed)
        assertNull(request.dns)
        assertNull(request.fiber)
    }

    @Test
    fun `executionId em branco gera um requestId novo (UUID), nunca vazio`() {
        val input = DiagnosticInput(executionId = "")

        val request = input.toNdsDiagnosticsRequest(appVersion = "1.0.0")

        assertTrue(request.requestId.isNotBlank())
        assertTrue("deveria parecer um UUID (36 chars, 4 hifens)", request.requestId.count { it == '-' } == 4)
    }

    @Test
    fun `wifi presente preenche bloco wifi e capabilities inclui wifi`() {
        val input = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            executionId = "exec-2",
            wifi = WifiDiagnosticInput(
                rssiDbm = -65,
                linkSpeedMbps = 433,
                frequenciaMhz = 5180,
                ssid = "MinhaRede_5G",
                bssidMascarado = "00:14:22:01:23:45",
                canal = 36,
                wifiStandard = "802.11ac",
            ),
        )

        val request = input.toNdsDiagnosticsRequest(appVersion = "1.0.0")

        assertEquals("WIFI", request.connection?.type)
        assertEquals("MinhaRede_5G", request.connection?.ssid)
        assertEquals("00:14:22:01:23:45", request.connection?.bssid)
        assertEquals(-65, request.wifi?.rssi)
        assertEquals("5GHz", request.wifi?.band)
        assertEquals(36, request.wifi?.channel)
        assertEquals(433, request.wifi?.linkSpeed)
        assertEquals("802.11ac", request.wifi?.standard)
        assertTrue(request.capabilities.contains("wifi"))
    }

    @Test
    fun `fibra presente preenche bloco fiber e capabilities inclui fiber`() {
        val input = DiagnosticInput(
            fibra = FibraDiagnosticInput(rxPowerDbm = -22.0, txPowerDbm = 2.5, temperatureCelsius = 45.0, isUp = true),
        )

        val request = input.toNdsDiagnosticsRequest(appVersion = "1.0.0")

        assertEquals(-22.0, request.fiber?.rxPowerDbm)
        assertEquals(2.5, request.fiber?.txPowerDbm)
        assertEquals(45.0, request.fiber?.temperatureC)
        assertNull("voltage nao existe em FibraDiagnosticInput -- gap conhecido", request.fiber?.voltageV)
        assertTrue(request.capabilities.contains("fiber"))
    }

    @Test
    fun `internet presente preenche bloco speed`() {
        val input = DiagnosticInput(
            internet = InternetDiagnosticInput(
                downloadMbps = 300.0,
                uploadMbps = 150.0,
                latencyMs = 12.0,
                jitterMs = 2.0,
                perdaPercentual = 0.5,
            ),
        )

        val request = input.toNdsDiagnosticsRequest(appVersion = "1.0.0")

        assertEquals(12.0, request.speed?.pingMs)
        assertEquals(2.0, request.speed?.jitterMs)
        assertEquals(300.0, request.speed?.downloadMbps)
        assertEquals(150.0, request.speed?.uploadMbps)
        assertEquals(0.5, request.speed?.packetLossPercent)
    }

    @Test
    fun `dns presente preenche bloco dns com hijacked sempre nulo (gap de coleta)`() {
        val input = DiagnosticInput(
            dns = DnsDiagnosticInput(currentDnsIp = "8.8.8.8", currentDnsLatencyMs = 35),
        )

        val request = input.toNdsDiagnosticsRequest(appVersion = "1.0.0")

        assertEquals("8.8.8.8", request.dns?.primary)
        assertEquals(35, request.dns?.responseTimeMs)
        assertNull(request.dns?.hijacked)
    }

    @Test
    fun `perfilGamer true preenche profile gamer, false mantem omitido`() {
        val input = DiagnosticInput()

        assertEquals("gamer", input.toNdsDiagnosticsRequest(appVersion = "1.0.0", perfilGamer = true).profile)
        assertNull(input.toNdsDiagnosticsRequest(appVersion = "1.0.0", perfilGamer = false).profile)
    }

    @Test
    fun `wifiScan sempre nulo -- gap documentado, este mapper nao roda o ChannelEvaluator`() {
        val input = DiagnosticInput(wifiScan = WifiScanDiagnosticInput())

        val request = input.toNdsDiagnosticsRequest(appVersion = "1.0.0")

        assertNull(request.wifiScan)
    }

    @Test
    fun `tipos de conexao mapeiam para o vocabulario esperado`() {
        assertEquals("WIFI", DiagnosticInput(connectionType = ConnectionType.wifi).toNdsDiagnosticsRequest("1.0.0").connection?.type)
        assertEquals("MOBILE", DiagnosticInput(connectionType = ConnectionType.mobile).toNdsDiagnosticsRequest("1.0.0").connection?.type)
        assertEquals("ETHERNET", DiagnosticInput(connectionType = ConnectionType.ethernet).toNdsDiagnosticsRequest("1.0.0").connection?.type)
        assertEquals("DISCONNECTED", DiagnosticInput(connectionType = ConnectionType.desconectado).toNdsDiagnosticsRequest("1.0.0").connection?.type)
        assertEquals("UNKNOWN", DiagnosticInput(connectionType = ConnectionType.desconhecido).toNdsDiagnosticsRequest("1.0.0").connection?.type)
    }
}
