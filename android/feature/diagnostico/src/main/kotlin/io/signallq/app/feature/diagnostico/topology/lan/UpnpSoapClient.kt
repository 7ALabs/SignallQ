package io.signallq.app.feature.diagnostico.topology.lan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class UpnpSoapClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) {
    suspend fun getExternalIpAddress(controlUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val body = """<?xml version="1.0"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
 <s:Body>
  <u:GetExternalIPAddress xmlns:u="urn:schemas-upnp-org:service:WANIPConnection:1"/>
 </s:Body>
</s:Envelope>""".trimIndent()

            val req = Request.Builder()
                .url(controlUrl)
                .addHeader("SOAPAction", "\"urn:schemas-upnp-org:service:WANIPConnection:1#GetExternalIPAddress\"")
                .addHeader("Content-Type", "text/xml; charset=utf-8")
                .post(body.toRequestBody("text/xml; charset=utf-8".toMediaType()))
                .build()

            val xml = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                resp.body?.string() ?: return@withContext null
            }
            UpnpParser.parseSoapGetExternalIpResponse(xml)
        } catch (_: Exception) { null }
    }
}
