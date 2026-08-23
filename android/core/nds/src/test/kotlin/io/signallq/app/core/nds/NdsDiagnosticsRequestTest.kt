package io.signallq.app.core.nds

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NdsDiagnosticsRequestTest {

    @Test
    fun `toJson omite blocos opcionais nulos em vez de mandar chave vazia ou zero`() {
        val request =
            NdsDiagnosticsRequest(
                requestId = "req-1",
                app = NdsAppInfo(id = "io.signallq.app", version = "1.0.0"),
            )

        val json = request.toJson()

        assertEquals("req-1", json.getString("request_id"))
        assertEquals("pt-BR", json.getString("locale"))
        assertFalse("profile null não deve virar chave", json.has("profile"))
        assertFalse("capabilities vazia não deve virar chave", json.has("capabilities"))
        assertFalse(json.has("connection"))
        assertFalse(json.has("wifi"))
        assertFalse(json.has("wifiScan"))
        assertFalse(json.has("speed"))
        assertFalse(json.has("quality"))
        assertFalse(json.has("dns"))
        assertFalse(json.has("gateway"))
        assertFalse(json.has("fiber"))
    }

    @Test
    fun `toJson usa o formato canonico do NDS para qualidade, dns e gateway`() {
        val request =
            NdsDiagnosticsRequest(
                requestId = "req-2",
                app = NdsAppInfo(id = "io.signallq.app", version = "1.0.0"),
                speed =
                    NdsSpeedInfo(
                        pingMs = 151.0,
                        jitterMs = 40.0,
                        downloadMbps = 300.0,
                        uploadMbps = 150.0,
                        packetLossPercent = 2.0,
                    ),
                quality =
                    NdsQualityInfo(
                        latencyMs = 151.0,
                        jitterMs = 40.0,
                        packetLossPercent = 2.0,
                        loadedLatencyMs = 251.0,
                        bufferbloatMs = 100.0,
                    ),
                dns = NdsDnsInfo(primary = "8.8.8.8", responseTimeMs = 35, hijacked = false),
                gateway = NdsGatewayInfo(rttGatewayMs = 4, connectedDevices = 6),
                fiber =
                    NdsFiberInfo(
                        rxPowerDbm = -22.0,
                        txPowerDbm = 2.5,
                        temperatureC = 45.0,
                        voltageV = 3.3,
                    ),
            )

        val json = request.toJson()

        val speed = json.getJSONObject("speed")
        assertEquals(151.0, speed.getDouble("ping_ms"), 0.001)
        assertEquals(40.0, speed.getDouble("jitter_ms"), 0.001)
        assertEquals(300.0, speed.getDouble("download_mbps"), 0.001)
        assertEquals(150.0, speed.getDouble("upload_mbps"), 0.001)
        assertEquals(2.0, speed.getDouble("packet_loss_percent"), 0.001)

        val quality = json.getJSONObject("quality")
        assertEquals(151.0, quality.getDouble("latencyMs"), 0.001)
        assertEquals(40.0, quality.getDouble("jitterMs"), 0.001)
        assertEquals(2.0, quality.getDouble("packetLossPercent"), 0.001)
        assertEquals(251.0, quality.getDouble("loadedLatencyMs"), 0.001)
        assertEquals(100.0, quality.getDouble("bufferbloatMs"), 0.001)

        val dns = json.getJSONObject("dns")
        assertEquals("8.8.8.8", dns.getString("primary"))
        assertEquals(35, dns.getInt("latencyMs"))
        assertFalse(dns.getBoolean("hijacked"))

        val gateway = json.getJSONObject("gateway")
        assertEquals(4, gateway.getInt("rttGatewayMs"))
        assertEquals(6, gateway.getInt("connectedDevices"))

        val fiber = json.getJSONObject("fiber")
        assertEquals(-22.0, fiber.getDouble("rxPower_dbm"), 0.001)
        assertEquals(2.5, fiber.getDouble("txPower_dbm"), 0.001)
        assertEquals(45.0, fiber.getDouble("temperature_c"), 0.001)
        assertEquals(3.3, fiber.getDouble("voltage_v"), 0.001)
    }

    @Test
    fun `toJson inclui capabilities quando presentes`() {
        val request =
            NdsDiagnosticsRequest(
                requestId = "req-3",
                app = NdsAppInfo(id = "io.signallq.app", version = "1.0.0"),
                capabilities = listOf("scoring", "ai"),
            )

        val json = request.toJson()

        assertTrue(json.has("capabilities"))
        val capabilities = json.getJSONArray("capabilities")
        assertEquals(2, capabilities.length())
        assertEquals("scoring", capabilities.getString(0))
        assertEquals("ai", capabilities.getString(1))
    }

    @Test
    fun `toJson serializa contexto sem PII e omite relato nulo`() {
        val request = NdsDiagnosticsRequest(
            requestId = "req-context",
            app = NdsAppInfo(id = "io.signallq.app", version = "1.0.0"),
            context = NdsDiagnosticContext(
                objective = "SITES_DEMORAM",
                answers = mapOf("pergunta_0" to "resposta_2"),
            ),
        )

        val context = request.toJson().getJSONObject("context")
        assertFalse(context.has("reported_problem"))
        assertEquals("SITES_DEMORAM", context.getString("objective"))
        assertEquals("resposta_2", context.getJSONObject("answers").getString("pergunta_0"))
    }
}
