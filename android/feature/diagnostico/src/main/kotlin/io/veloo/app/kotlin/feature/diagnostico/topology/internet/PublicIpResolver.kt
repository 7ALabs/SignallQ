package io.signallq.app.feature.diagnostico.topology.internet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class PublicIpResolver(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build(),
    private val endpoints: List<String> = DEFAULT_ENDPOINTS
) {
    companion object {
        val DEFAULT_ENDPOINTS = listOf(
            "https://api.ipify.org",
            "https://ifconfig.me/ip",
            "https://icanhazip.com"
        )
    }

    suspend fun resolve(): String? = withContext(Dispatchers.IO) {
        for (url in endpoints) {
            val ip = tryFetch(url)
            if (ip != null) return@withContext ip
        }
        null
    }

    private fun tryFetch(url: String): String? = try {
        val req = Request.Builder().url(url).addHeader("User-Agent", "curl/7.0").build()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) null
            else resp.body?.string()?.trim()?.takeIf { isValidIp(it) }
        }
    } catch (_: Exception) { null }

    private fun isValidIp(s: String): Boolean =
        s.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""))
}
