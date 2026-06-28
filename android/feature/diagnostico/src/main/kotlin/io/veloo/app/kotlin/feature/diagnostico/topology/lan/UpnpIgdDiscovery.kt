package io.signallq.app.feature.diagnostico.topology.lan

import android.content.Context
import android.net.wifi.WifiManager
import io.signallq.app.feature.diagnostico.topology.model.UpnpDeviceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UpnpIgdDiscovery(
    context: Context,
    private val httpClient: OkHttpClient,
) {
    private val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    suspend fun discover(): UpnpDeviceInfo? = withContext(Dispatchers.IO) {
        val location = discoverLocation() ?: return@withContext null
        fetchDescription(location)
    }

    private fun discoverLocation(): String? {
        val lock = wm.createMulticastLock("signallq_topology_ssdp").apply { acquire() }
        return try {
            val msearch = buildString {
                appendLine("M-SEARCH * HTTP/1.1")
                appendLine("HOST: 239.255.255.250:1900")
                appendLine("MAN: \"ssdp:discover\"")
                appendLine("MX: 2")
                appendLine("ST: urn:schemas-upnp-org:device:InternetGatewayDevice:1")
                appendLine()
            }.toByteArray(Charsets.UTF_8)

            DatagramSocket().use { socket ->
                socket.soTimeout = 2000
                val group = InetAddress.getByName("239.255.255.250")
                socket.send(DatagramPacket(msearch, msearch.size, group, 1900))

                val buf = ByteArray(4096)
                val deadline = System.currentTimeMillis() + 2000L
                var found: String? = null
                while (System.currentTimeMillis() < deadline && found == null) {
                    try {
                        val recv = DatagramPacket(buf, buf.size)
                        socket.receive(recv)
                        val raw = String(recv.data, 0, recv.length, Charsets.UTF_8)
                        found = UpnpParser.parseSsdpResponse(raw)?.location
                    } catch (_: java.net.SocketTimeoutException) { break }
                }
                found
            }
        } catch (_: Exception) { null }
        finally { runCatching { lock.release() } }
    }

    private fun fetchDescription(locationUrl: String): UpnpDeviceInfo? = try {
        val req = Request.Builder().url(locationUrl).build()
        val xml = httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            resp.body?.string() ?: return null
        }
        UpnpParser.parseUpnpDescription(xml, locationUrl)
    } catch (_: Exception) { null }
}
