package io.signallq.app.feature.diagnostico.remote

import io.signallq.app.feature.diagnostico.ConnectionType
import io.signallq.app.feature.diagnostico.DiagnosticInput
import io.signallq.app.feature.diagnostico.DnsDiagnosticInput
import io.signallq.app.feature.diagnostico.FibraDiagnosticInput
import io.signallq.app.feature.diagnostico.HistoricalDiagnosticInput
import io.signallq.app.feature.diagnostico.InternetDiagnosticInput
import io.signallq.app.feature.diagnostico.MobileDiagnosticInput
import io.signallq.app.feature.diagnostico.WifiDiagnosticInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DiagnosticSnapshotMapperTest {

    @Test
    fun `snapshot vazio ainda manda schemaVersion e connection`() {
        val json = DiagnosticSnapshotMapper.toJson(DiagnosticInput())

        assertEquals(6, json.getInt("schemaVersion"))
        assertEquals("UNKNOWN", json.getJSONObject("connection").getString("type"))
        assertFalse(json.has("wifi"))
        assertFalse(json.has("speed"))
        assertFalse(json.has("quality"))
    }

    @Test
    fun `wifi 2_4Ghz mapeia banda no formato do worker`() {
        val input = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            wifi = WifiDiagnosticInput(rssiDbm = -70, linkSpeedMbps = 72, frequenciaMhz = 2412),
        )
        val json = DiagnosticSnapshotMapper.toJson(input)

        assertEquals("WIFI", json.getJSONObject("connection").getString("type"))
        val wifi = json.getJSONObject("wifi")
        assertEquals("2_4_GHZ", wifi.getString("band"))
        assertEquals(-70, wifi.getInt("rssiDbm"))
        assertEquals(72, wifi.getInt("linkSpeedMbps"))
    }

    @Test
    fun `wifi 5Ghz mapeia banda 5_GHZ`() {
        val input = DiagnosticInput(
            wifi = WifiDiagnosticInput(rssiDbm = -55, linkSpeedMbps = 400, frequenciaMhz = 5180),
        )
        val json = DiagnosticSnapshotMapper.toJson(input)
        assertEquals("5_GHZ", json.getJSONObject("wifi").getString("band"))
    }

    @Test
    fun `speed e quality mapeiam metricas de internet`() {
        val input = DiagnosticInput(
            internet = InternetDiagnosticInput(
                downloadMbps = 85.0,
                uploadMbps = 20.0,
                latencyMs = 24.0,
                jitterMs = 4.0,
                perdaPercentual = 0.5,
                bufferbloatMs = 12.0,
            ),
        )
        val json = DiagnosticSnapshotMapper.toJson(input)

        val speed = json.getJSONObject("speed")
        assertEquals(85.0, speed.getDouble("downloadMbps"), 0.001)
        assertEquals(20.0, speed.getDouble("uploadMbps"), 0.001)

        val quality = json.getJSONObject("quality")
        assertEquals(24.0, quality.getDouble("latencyMs"), 0.001)
        assertEquals(4.0, quality.getDouble("jitterMs"), 0.001)
        assertEquals(0.5, quality.getDouble("packetLossPercent"), 0.001)
        // loadedLatencyMs = latencyMs + bufferbloatMs (derivado, contrato remoto nao tem bufferbloatMs direto)
        assertEquals(36.0, quality.getDouble("loadedLatencyMs"), 0.001)
    }

    @Test
    fun `internet sem nenhuma metrica de velocidade marca hasInternet false e nao manda speed`() {
        val input = DiagnosticInput(
            internet = InternetDiagnosticInput(
                downloadMbps = null,
                uploadMbps = null,
                latencyMs = null,
                jitterMs = null,
                perdaPercentual = null,
            ),
        )
        val json = DiagnosticSnapshotMapper.toJson(input)
        assertFalse(json.getJSONObject("connection").getBoolean("hasInternet"))
        assertFalse(json.has("speed"))
    }

    @Test
    fun `dns fibra mobile e historico mapeiam quando presentes`() {
        val input = DiagnosticInput(
            dns = DnsDiagnosticInput(currentDnsLatencyMs = 45, currentDnsName = "Cloudflare"),
            fibra = FibraDiagnosticInput(rxPowerDbm = -18.5, txPowerDbm = 2.1, temperatureCelsius = 45.0, isUp = true),
            mobile = MobileDiagnosticInput(mobileTechnology = "5G", rsrpDbm = -95, rsrqDb = -10, sinrDb = 12, carrierName = "Vivo"),
            historico = HistoricalDiagnosticInput(avgDownload7d = 80.0, testsCount7d = 5, testsCount30d = 20),
        )
        val json = DiagnosticSnapshotMapper.toJson(input)

        assertEquals(45, json.getJSONObject("dns").getInt("latencyMs"))
        assertEquals("Cloudflare", json.getJSONObject("dns").getString("currentProvider"))

        assertEquals(-18.5, json.getJSONObject("fiber").getDouble("rxPowerDbm"), 0.001)

        assertEquals("5G", json.getJSONObject("mobile").getString("technology"))
        assertEquals(-95, json.getJSONObject("mobile").getInt("rsrpDbm"))

        assertEquals(80.0, json.getJSONObject("historical").getDouble("avgDownload7d"), 0.001)
        assertEquals(5, json.getJSONObject("historical").getInt("testsCount7d"))
    }

    @Test
    fun `gateway rtt mapeia quando presente no internet input`() {
        val input = DiagnosticInput(
            internet = InternetDiagnosticInput(
                downloadMbps = 50.0, uploadMbps = 10.0, latencyMs = 20.0, jitterMs = 2.0,
                perdaPercentual = 0.0, rttGatewayMs = 8,
            ),
        )
        val json = DiagnosticSnapshotMapper.toJson(input)
        assertEquals(8, json.getJSONObject("gateway").getInt("rttMs"))
    }

    @Test
    fun `fibra down sem nenhuma metrica optica nao e enviada`() {
        val input = DiagnosticInput(fibra = FibraDiagnosticInput(isUp = false))
        val json = DiagnosticSnapshotMapper.toJson(input)
        assertFalse(json.has("fiber"))
    }
}
