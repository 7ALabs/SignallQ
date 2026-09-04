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
        assertFalse(json.has("mobile"))
        assertFalse(json.has("historical"))
        assertFalse(json.has("localEquipment"))
        assertFalse(json.has("plan"))
    }

    @Test
    fun `toJson serializa connection natStatus e bloco plan (ADR-018, NDS-Snapshot-09, issue #1841)`() {
        val request = NdsDiagnosticsRequest(
            requestId = "req-nat-plan",
            app = NdsAppInfo(id = "io.signallq.app", version = "1.0.0"),
            connection = NdsConnectionInfo(type = "WIFI", natStatus = "CGNAT"),
            plan = NdsPlanInfo(contractedSpeedMbps = 500),
        )

        val json = request.toJson()

        assertEquals("CGNAT", json.getJSONObject("connection").getString("natStatus"))
        assertEquals(500, json.getJSONObject("plan").getInt("contractedSpeedMbps"))
    }

    @Test
    fun `toJson omite connection natStatus quando null, sem quebrar os demais campos de connection`() {
        val request = NdsDiagnosticsRequest(
            requestId = "req-nat-null",
            app = NdsAppInfo(id = "io.signallq.app", version = "1.0.0"),
            connection = NdsConnectionInfo(type = "WIFI", ssid = "MinhaRede", natStatus = null),
        )

        val connectionJson = request.toJson().getJSONObject("connection")

        assertEquals("WIFI", connectionJson.getString("type"))
        assertEquals("MinhaRede", connectionJson.getString("ssid"))
        assertFalse("natStatus null nunca vira chave", connectionJson.has("natStatus"))
    }

    @Test
    fun `toJson omite plan contractedSpeedMbps quando null, mas nao omite o bloco se ja foi construido`() {
        val request = NdsDiagnosticsRequest(
            requestId = "req-plan-vazio",
            app = NdsAppInfo(id = "io.signallq.app", version = "1.0.0"),
            plan = NdsPlanInfo(contractedSpeedMbps = null),
        )

        val planJson = request.toJson().getJSONObject("plan")

        assertFalse("contractedSpeedMbps null nunca vira chave", planJson.has("contractedSpeedMbps"))
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
    fun `toJson serializa bloco mobile em snake_case e sem identificadores de celula`() {
        val request = NdsDiagnosticsRequest(
            requestId = "req-mobile",
            app = NdsAppInfo(id = "io.signallq.app", version = "1.0.0"),
            mobile = NdsMobileInfo(
                operator = "TIM",
                technology = "5G",
                rsrpDbm = -101,
                rsrqDb = -14,
                sinrDb = 7,
                band = "n78",
            ),
        )

        val mobile = request.toJson().getJSONObject("mobile")

        assertEquals("TIM", mobile.getString("operator"))
        assertEquals("5G", mobile.getString("technology"))
        assertEquals(-101, mobile.getInt("rsrp_dbm"))
        assertEquals(-14, mobile.getInt("rsrq_db"))
        assertEquals(7, mobile.getInt("sinr_db"))
        assertEquals("n78", mobile.getString("band"))
        assertFalse("Cell ID nunca deve entrar no payload", mobile.has("cell_id"))
        assertFalse("TAC nunca deve entrar no payload", mobile.has("tac"))
        assertFalse("MCC nunca deve entrar no payload", mobile.has("mcc"))
        assertFalse("MNC nunca deve entrar no payload", mobile.has("mnc"))
    }

    @Test
    fun `toJson omite campos individuais nulos do bloco mobile`() {
        val request = NdsDiagnosticsRequest(
            requestId = "req-mobile-parcial",
            app = NdsAppInfo(id = "io.signallq.app", version = "1.0.0"),
            mobile = NdsMobileInfo(rsrpDbm = -95, rsrqDb = -12, sinrDb = 3),
        )

        val mobile = request.toJson().getJSONObject("mobile")

        assertFalse("tecnologia desconhecida nao deve virar chave", mobile.has("technology"))
        assertFalse(mobile.has("operator"))
        assertFalse(mobile.has("band"))
        assertEquals(-95, mobile.getInt("rsrp_dbm"))
    }

    @Test
    fun `toJson usa snake_case para historical e sempre serializa as contagens de teste, mesmo zero`() {
        val request =
            NdsDiagnosticsRequest(
                requestId = "req-hist",
                app = NdsAppInfo(id = "io.signallq.app", version = "1.0.0"),
                historical =
                    NdsHistoricalInfo(
                        testsCount7d = 0,
                        avgDownload30d = 301.8,
                        avgUpload30d = 149.1,
                        avgPing30d = 17.5,
                        testsCount30d = 12,
                        degradationDetected = false,
                        degradationPercent = -3.2,
                    ),
            )

        val historical = request.toJson().getJSONObject("historical")

        assertEquals(0, historical.getInt("tests_7d"))
        assertFalse("media de 7d ausente nao pode virar zero", historical.has("avg_download_7d"))
        assertEquals(12, historical.getInt("tests_30d"))
        assertEquals(301.8, historical.getDouble("avg_download_30d"), 0.001)
        assertEquals(149.1, historical.getDouble("avg_upload_30d"), 0.001)
        assertEquals(17.5, historical.getDouble("avg_ping_30d"), 0.001)
        assertFalse(historical.getBoolean("degradation_detected"))
        assertEquals(-3.2, historical.getDouble("degradation_percent"), 0.001)
    }

    @Test
    fun `toJson serializa bloco dns expandido (ADR-018, issue #1840)`() {
        val request = NdsDiagnosticsRequest(
            requestId = "req-dns",
            app = NdsAppInfo(id = "io.signallq.app", version = "1.0.0"),
            dns = NdsDnsInfo(
                primary = "1.1.1.1",
                responseTimeMs = 12,
                hijacked = null,
                providerName = "Cloudflare",
                bestName = "Cloudflare",
                bestLatencyMs = 12,
                grade = "A",
                comparisonAvailable = true,
                coherenceAlertLevel = "none",
                coherenceConsecutiveDivergences = 0,
                coherenceDivergenceRatePercent = 0.0,
                privateDnsActive = true,
                privateDnsHostname = "dns.google",
            ),
        )

        val dns = request.toJson().getJSONObject("dns")

        assertEquals("1.1.1.1", dns.getString("primary"))
        assertEquals(12, dns.getInt("latencyMs"))
        assertFalse("hijacked null nunca vira chave", dns.has("hijacked"))
        assertEquals("Cloudflare", dns.getString("providerName"))
        assertEquals("Cloudflare", dns.getString("bestName"))
        assertEquals(12, dns.getInt("bestLatencyMs"))
        assertEquals("A", dns.getString("grade"))
        assertTrue(dns.getBoolean("comparisonAvailable"))
        assertEquals("none", dns.getString("coherenceAlertLevel"))
        assertEquals(0, dns.getInt("coherenceConsecutiveDivergences"))
        assertEquals(0.0, dns.getDouble("coherenceDivergenceRatePercent"), 0.001)
        assertTrue(dns.getBoolean("privateDnsActive"))
        assertEquals("dns.google", dns.getString("privateDnsHostname"))
    }

    @Test
    fun `toJson sempre serializa comparisonAvailable mesmo false, nunca omite`() {
        val request = NdsDiagnosticsRequest(
            requestId = "req-dns-sem-comparacao",
            app = NdsAppInfo(id = "io.signallq.app", version = "1.0.0"),
            dns = NdsDnsInfo(primary = "8.8.8.8", comparisonAvailable = false),
        )

        val dns = request.toJson().getJSONObject("dns")

        assertTrue("comparisonAvailable=false e valor legitimo, deve estar sempre presente", dns.has("comparisonAvailable"))
        assertFalse(dns.getBoolean("comparisonAvailable"))
        assertFalse(dns.has("bestName"))
        assertFalse(dns.has("privateDnsActive"))
        assertFalse(dns.has("privateDnsHostname"))
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
