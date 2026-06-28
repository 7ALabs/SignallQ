package io.signallq.app.feature.diagnostico.topology.lan

import io.signallq.app.feature.diagnostico.topology.model.SsdpResponse
import io.signallq.app.feature.diagnostico.topology.model.UpnpDeviceInfo
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

internal object UpnpParser {

    // Parse de uma resposta SSDP completa (HTTP/1.1 200 OK\r\n...)
    fun parseSsdpResponse(raw: String): SsdpResponse? {
        val headers = raw.lines().drop(1) // pula a status line
            .mapNotNull { line ->
                val colon = line.indexOf(':')
                if (colon < 0) null else line.substring(0, colon).trim().uppercase() to line.substring(colon + 1).trim()
            }.toMap()
        val location = headers["LOCATION"] ?: return null
        return SsdpResponse(
            location = location,
            usn = headers["USN"],
            server = headers["SERVER"]
        )
    }

    // Parse do XML de descrição UPnP. baseUrl é usado para resolver controlUrl relativo.
    fun parseUpnpDescription(xml: String, baseUrl: String): UpnpDeviceInfo? = try {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        doc.documentElement.normalize()

        fun text(tag: String): String? =
            doc.getElementsByTagName(tag).item(0)?.textContent?.trim()?.takeIf { it.isNotEmpty() }

        val friendlyName = text("friendlyName")
        val manufacturer = text("manufacturer")
        val modelName = text("modelName")
        val modelNumber = text("modelNumber")

        // Procura WANIPConnection:1 ou WANPPPConnection:1
        val serviceTypes = listOf("WANIPConnection:1", "WANPPPConnection:1")
        var controlUrl: String? = null

        val serviceList = doc.getElementsByTagName("service")
        outer@ for (i in 0 until serviceList.length) {
            val el = serviceList.item(i) as? Element ?: continue
            val type = el.getElementsByTagName("serviceType").item(0)?.textContent ?: continue
            for (st in serviceTypes) {
                if (type.contains(st, ignoreCase = true)) {
                    val rel = el.getElementsByTagName("controlURL").item(0)?.textContent?.trim() ?: break
                    controlUrl = resolveUrl(baseUrl, rel)
                    break@outer
                }
            }
        }

        UpnpDeviceInfo(friendlyName, manufacturer, modelName, modelNumber, controlUrl)
    } catch (_: Exception) { null }

    // Parse do XML de resposta SOAP GetExternalIPAddress
    fun parseSoapGetExternalIpResponse(xml: String): String? = try {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        doc.getElementsByTagName("NewExternalIPAddress").item(0)
            ?.textContent?.trim()?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) { null }

    private fun resolveUrl(base: String, relative: String): String = try {
        URL(URL(base), relative).toString()
    } catch (_: Exception) { relative }
}
