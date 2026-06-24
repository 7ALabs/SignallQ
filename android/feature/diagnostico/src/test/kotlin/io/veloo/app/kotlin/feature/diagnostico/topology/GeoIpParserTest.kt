package io.veloo.app.feature.diagnostico.topology

import io.veloo.app.feature.diagnostico.topology.internet.GeoIpResolver
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GeoIpParserTest {

    private val resolver = GeoIpResolver()

    // --- Fixtures ---

    private val ipInfoJson = JSONObject(
        """{"ip":"203.0.113.45","city":"São Paulo","region":"São Paulo","country":"BR","org":"AS28573 Claro S.A.","loc":"-23.5505,-46.6333"}"""
    )

    private val ipApiSuccessJson = JSONObject(
        """{"status":"success","isp":"Claro S.A.","city":"São Paulo","regionName":"São Paulo","country":"Brazil","lat":-23.5505,"lon":-46.6333}"""
    )

    private val ipApiFailJson = JSONObject(
        """{"status":"fail","message":"private range","query":"192.168.1.1"}"""
    )

    // --- parseIpInfoResponse ---

    @Test
    fun `parseIpInfoResponse extrai org como isp`() {
        val result = resolver.parseIpInfoResponse(ipInfoJson)
        assertEquals("AS28573 Claro S.A.", result.isp)
    }

    @Test
    fun `parseIpInfoResponse combina city, region e country na region`() {
        val result = resolver.parseIpInfoResponse(ipInfoJson)
        assertEquals("São Paulo, São Paulo, BR", result.region)
    }

    @Test
    fun `parseIpInfoResponse retorna resultado nao nulo`() {
        val result = resolver.parseIpInfoResponse(ipInfoJson)
        assertNotNull(result)
    }

    // --- parseIpApiResponse ---

    @Test
    fun `parseIpApiResponse com status success extrai isp`() {
        val result = resolver.parseIpApiResponse(ipApiSuccessJson)
        assertNotNull(result)
        assertEquals("Claro S.A.", result!!.isp)
    }

    @Test
    fun `parseIpApiResponse com status success combina city, regionName e country`() {
        val result = resolver.parseIpApiResponse(ipApiSuccessJson)
        assertNotNull(result)
        assertEquals("São Paulo, São Paulo, Brazil", result!!.region)
    }

    @Test
    fun `parseIpApiResponse com status fail retorna null`() {
        val result = resolver.parseIpApiResponse(ipApiFailJson)
        assertNull(result)
    }
}
