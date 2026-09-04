package io.signallq.app.core.nds

import io.signallq.app.core.diagnostico.ConnectionType
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticContext
import io.signallq.app.core.diagnostico.DnsDiagnosticInput
import io.signallq.app.core.diagnostico.FibraDiagnosticInput
import io.signallq.app.core.diagnostico.HistoricalDiagnosticInput
import io.signallq.app.core.diagnostico.InternetDiagnosticInput
import io.signallq.app.core.diagnostico.RedeWifiVizinha
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
        assertNull(request.quality)
        assertNull(request.dns)
        assertNull(request.gateway)
        assertNull(request.fiber)
        assertNull(request.historical)
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
                bufferbloatMs = 65.0,
                rttGatewayMs = 4,
            ),
            wifi = WifiDiagnosticInput(rssiDbm = -55, linkSpeedMbps = 433, frequenciaMhz = 5180, dispositivosNaRede = 6),
        )

        val request = input.toNdsDiagnosticsRequest(appVersion = "1.0.0")

        assertEquals(12.0, request.speed?.pingMs)
        assertEquals(2.0, request.speed?.jitterMs)
        assertEquals(300.0, request.speed?.downloadMbps)
        assertEquals(150.0, request.speed?.uploadMbps)
        assertEquals(0.5, request.speed?.packetLossPercent)
        assertEquals(12.0, request.quality?.latencyMs)
        assertEquals(2.0, request.quality?.jitterMs)
        assertEquals(0.5, request.quality?.packetLossPercent)
        assertEquals(77.0, request.quality?.loadedLatencyMs)
        assertEquals(65.0, request.quality?.bufferbloatMs)
        assertEquals(4, request.gateway?.rttGatewayMs)
        assertEquals(6, request.gateway?.connectedDevices)
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
    fun `wifiScan ausente quando DiagnosticInput nao carrega scan`() {
        val input = DiagnosticInput()

        val request = input.toNdsDiagnosticsRequest(appVersion = "1.0.0")

        assertNull(request.wifiScan)
    }

    @Test
    fun `wifiScan ausente quando scan nao tem nenhuma evidencia util`() {
        val input = DiagnosticInput(wifiScan = WifiScanDiagnosticInput())

        val request = input.toNdsDiagnosticsRequest(appVersion = "1.0.0")

        assertNull(request.wifiScan)
    }

    @Test
    fun `wifiScan com redes vizinhas chega ao payload com evidencia e entra em capabilities`() {
        val input = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            wifi = WifiDiagnosticInput(rssiDbm = -60, linkSpeedMbps = 433, frequenciaMhz = 5180, canal = 36),
            wifiScan = WifiScanDiagnosticInput(
                conectadoCanal = 36,
                conectadoBanda = io.signallq.app.core.diagnostico.BandaWifi.ghz5,
                redes = listOf(
                    RedeWifiVizinha(canal = 40, rssiDbm = -55, frequenciaMhz = 5200, bssid = "AA:BB:CC:00:00:01"),
                    RedeWifiVizinha(canal = 36, rssiDbm = -70, frequenciaMhz = 5180, bssid = "AA:BB:CC:00:00:02"),
                ),
            ),
        )

        val request = input.toNdsDiagnosticsRequest(appVersion = "1.0.0")

        assertEquals(36, request.wifiScan?.connectedChannel)
        assertEquals(2, request.wifiScan?.neighborCount)
        assertEquals(2, request.wifiScan?.neighbors?.size)
        assertTrue(request.capabilities.contains("wifi_scan"))
    }

    @Test
    fun `tipos de conexao mapeiam para o vocabulario esperado`() {
        assertEquals("WIFI", DiagnosticInput(connectionType = ConnectionType.wifi).toNdsDiagnosticsRequest("1.0.0").connection?.type)
        assertEquals("MOBILE", DiagnosticInput(connectionType = ConnectionType.mobile).toNdsDiagnosticsRequest("1.0.0").connection?.type)
        assertEquals("ETHERNET", DiagnosticInput(connectionType = ConnectionType.ethernet).toNdsDiagnosticsRequest("1.0.0").connection?.type)
        assertEquals("DISCONNECTED", DiagnosticInput(connectionType = ConnectionType.desconectado).toNdsDiagnosticsRequest("1.0.0").connection?.type)
        assertEquals("UNKNOWN", DiagnosticInput(connectionType = ConnectionType.desconhecido).toNdsDiagnosticsRequest("1.0.0").connection?.type)
    }

    @Test
    fun `caracterizacao -- wifiScan preenchido nao muda serializacao dos outros blocos`() {
        val input = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            executionId = "exec-caracterizacao",
            wifi = WifiDiagnosticInput(
                rssiDbm = -65,
                linkSpeedMbps = 433,
                frequenciaMhz = 5180,
                ssid = "MinhaRede_5G",
                bssidMascarado = "00:14:22:01:23:45",
                canal = 36,
                wifiStandard = "802.11ac",
                dispositivosNaRede = 6,
            ),
            wifiScan = WifiScanDiagnosticInput(
                conectadoCanal = 36,
                redes = listOf(RedeWifiVizinha(canal = 40, rssiDbm = -55, frequenciaMhz = 5200, bssid = "AA:BB:CC:00:00:01")),
            ),
            internet = InternetDiagnosticInput(
                downloadMbps = 300.0,
                uploadMbps = 150.0,
                latencyMs = 12.0,
                jitterMs = 2.0,
                perdaPercentual = 0.5,
                bufferbloatMs = 65.0,
                rttGatewayMs = 4,
            ),
            dns = DnsDiagnosticInput(currentDnsIp = "8.8.8.8", currentDnsLatencyMs = 35),
            fibra = FibraDiagnosticInput(rxPowerDbm = -22.0, txPowerDbm = 2.5, temperatureCelsius = 45.0, isUp = true),
        )

        val json = input.toNdsDiagnosticsRequest(appVersion = "1.0.0").toJson()

        // Blocos que nao pertencem a esta fatia (#1834) — mesmos valores que os
        // testes dedicados de cada bloco (acima) ja verificam, aqui conferidos via
        // JSON serializado para provar que a introducao do wifiScan real nao
        // alterou o shape ou o valor de nenhum outro bloco.
        val wifiJson = json.getJSONObject("wifi")
        assertEquals(-65, wifiJson.getInt("rssi"))
        assertEquals("5GHz", wifiJson.getString("band"))
        assertEquals(36, wifiJson.getInt("channel"))
        assertEquals(433, wifiJson.getInt("linkSpeed"))
        assertEquals("802.11ac", wifiJson.getString("standard"))

        val speedJson = json.getJSONObject("speed")
        assertEquals(12.0, speedJson.getDouble("ping_ms"), 0.0)
        assertEquals(300.0, speedJson.getDouble("download_mbps"), 0.0)
        assertEquals(150.0, speedJson.getDouble("upload_mbps"), 0.0)
        assertEquals(0.5, speedJson.getDouble("packet_loss_percent"), 0.0)

        val qualityJson = json.getJSONObject("quality")
        assertEquals(77.0, qualityJson.getDouble("loadedLatencyMs"), 0.0)
        assertEquals(65.0, qualityJson.getDouble("bufferbloatMs"), 0.0)

        val dnsJson = json.getJSONObject("dns")
        assertEquals("8.8.8.8", dnsJson.getString("primary"))
        assertEquals(35, dnsJson.getInt("latencyMs"))
        assertTrue("hijacked segue sem coleta -- nunca deve virar chave", !dnsJson.has("hijacked"))

        val gatewayJson = json.getJSONObject("gateway")
        assertEquals(4, gatewayJson.getInt("rttGatewayMs"))
        assertEquals(6, gatewayJson.getInt("connectedDevices"))

        val fiberJson = json.getJSONObject("fiber")
        assertEquals(-22.0, fiberJson.getDouble("rxPower_dbm"), 0.0)
        assertEquals(2.5, fiberJson.getDouble("txPower_dbm"), 0.0)
        assertEquals(45.0, fiberJson.getDouble("temperature_c"), 0.0)
        assertTrue("voltage segue sem coleta -- gap conhecido", !fiberJson.has("voltage_v"))

        // Bloco desta fatia: presente, com evidencia bruta e resultado calculado.
        val wifiScanJson = json.getJSONObject("wifiScan")
        assertEquals(36, wifiScanJson.getInt("connectedChannel"))
        assertEquals(1, wifiScanJson.getInt("neighborCount"))
        assertEquals(1, wifiScanJson.getJSONArray("neighbors").length())
    }

    @Test
    fun `historico ausente quando DiagnosticInput nao carrega nenhum teste`() {
        val input = DiagnosticInput(historico = HistoricalDiagnosticInput(testsCount7d = 0, testsCount30d = 0))

        val request = input.toNdsDiagnosticsRequest(appVersion = "1.0.0")

        assertNull(request.historical)
    }

    @Test
    fun `historico presente preenche bloco historical e entra em capabilities`() {
        val input = DiagnosticInput(
            historico = HistoricalDiagnosticInput(
                avgDownload7d = 287.4,
                avgUpload7d = 141.2,
                avgPing7d = 19.2,
                testsCount7d = 8,
                avgDownload30d = 301.8,
                avgUpload30d = 149.1,
                avgPing30d = 17.5,
                testsCount30d = 31,
                degradationDetected = true,
                degradationPercent = 18.3,
            ),
        )

        val request = input.toNdsDiagnosticsRequest(appVersion = "1.0.0")

        assertEquals(8, request.historical?.testsCount7d)
        assertEquals(287.4, request.historical?.avgDownload7d)
        assertEquals(31, request.historical?.testsCount30d)
        assertEquals(301.8, request.historical?.avgDownload30d)
        assertEquals(true, request.historical?.degradationDetected)
        assertEquals(18.3, request.historical?.degradationPercent)
        assertTrue(request.capabilities.contains("historical"))
    }

    @Test
    fun `contexto guiado preserva relato objetivo subcategoria e respostas estruturadas`() {
        val request = DiagnosticInput(
            executionId = "exec-context",
            context = DiagnosticContext(
                reportedProblem = "A conexão cai à noite.",
                objective = "JOGOS_COM_LAG",
                subcategory = "lag_horario_pico",
                answers = mapOf("pergunta_0" to "resposta_1"),
            ),
        ).toNdsDiagnosticsRequest(appVersion = "1.0.0")

        assertEquals("gamer", request.profile)
        assertTrue(request.capabilities.contains("usage_profiles"))
        assertEquals("A conexão cai à noite.", request.context?.reportedProblem)
        assertEquals("JOGOS_COM_LAG", request.context?.objective)
        assertEquals("lag_horario_pico", request.context?.subcategory)
        assertEquals(mapOf("pergunta_0" to "resposta_1"), request.context?.answers)
    }
}
