package io.signallq.app.feature.diagnostico.topology.internet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeoIpResult(val isp: String?, val region: String?)

class GeoIpResolver(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) {
    suspend fun resolve(): GeoIpResult? = withContext(Dispatchers.IO) {
        tryIpInfo() ?: tryIpApi()
    }

    private fun tryIpInfo(): GeoIpResult? {
        return try {
            val req = Request.Builder().url("https://ipinfo.io/json").build()
            val json = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                JSONObject(resp.body?.string() ?: return null)
            }
            val isp = json.optString("org").takeIf { it.isNotBlank() }
            val city = json.optString("city")
            val region = json.optString("region")
            val country = json.optString("country")
            val regionStr = listOf(city, region, country).filter { it.isNotBlank() }.joinToString(", ").takeIf { it.isNotBlank() }
            GeoIpResult(isp = isp, region = regionStr)
        } catch (_: Exception) { null }
    }

    private fun tryIpApi(): GeoIpResult? {
        return try {
            val req = Request.Builder().url("http://ip-api.com/json").build()
            val json = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                JSONObject(resp.body?.string() ?: return null)
            }
            if (json.optString("status") != "success") return null
            val isp = json.optString("isp").takeIf { it.isNotBlank() }
            val city = json.optString("city")
            val regionName = json.optString("regionName")
            val country = json.optString("country")
            val regionStr = listOf(city, regionName, country).filter { it.isNotBlank() }.joinToString(", ").takeIf { it.isNotBlank() }
            GeoIpResult(isp = isp, region = regionStr)
        } catch (_: Exception) { null }
    }

    // Função pura para parsing — testável sem rede
    internal fun parseIpInfoResponse(json: JSONObject): GeoIpResult {
        val isp = json.optString("org").takeIf { it.isNotBlank() }
        val city = json.optString("city")
        val region = json.optString("region")
        val country = json.optString("country")
        val regionStr = listOf(city, region, country).filter { it.isNotBlank() }.joinToString(", ").takeIf { it.isNotBlank() }
        return GeoIpResult(isp = isp, region = regionStr)
    }

    internal fun parseIpApiResponse(json: JSONObject): GeoIpResult? {
        if (json.optString("status") != "success") return null
        val isp = json.optString("isp").takeIf { it.isNotBlank() }
        val city = json.optString("city")
        val regionName = json.optString("regionName")
        val country = json.optString("country")
        val regionStr = listOf(city, regionName, country).filter { it.isNotBlank() }.joinToString(", ").takeIf { it.isNotBlank() }
        return GeoIpResult(isp = isp, region = regionStr)
    }
}
