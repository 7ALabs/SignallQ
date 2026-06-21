package io.veloo.app.feature.diagnostico.topology

import io.veloo.app.feature.diagnostico.topology.lan.UpnpParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UpnpParserTest {

    // --- Fixtures ---

    private val ssdpResponseFixture = """HTTP/1.1 200 OK
CACHE-CONTROL: max-age=1800
DATE: Thu, 01 Jan 2026 00:00:00 GMT
EXT:
LOCATION: http://192.168.1.1:49152/rootDesc.xml
SERVER: Linux/3.10 UPnP/1.1 MiniUPnPd/2.1
ST: urn:schemas-upnp-org:device:InternetGatewayDevice:1
USN: uuid:12345678-1234-1234-1234-123456789abc::urn:schemas-upnp-org:device:InternetGatewayDevice:1"""

    private val upnpDescriptionXml = """<?xml version="1.0"?>
<root xmlns="urn:schemas-upnp-org:device-1-0">
  <specVersion><major>1</major><minor>0</minor></specVersion>
  <device>
    <deviceType>urn:schemas-upnp-org:device:InternetGatewayDevice:1</deviceType>
    <friendlyName>TP-Link Router AC1200</friendlyName>
    <manufacturer>TP-Link</manufacturer>
    <modelName>Archer C6</modelName>
    <modelNumber>V3.0</modelNumber>
    <deviceList>
      <device>
        <serviceList>
          <service>
            <serviceType>urn:schemas-upnp-org:service:WANIPConnection:1</serviceType>
            <serviceId>urn:upnp-org:serviceId:WANIPConn1</serviceId>
            <controlURL>/ctl/IPConn</controlURL>
          </service>
        </serviceList>
      </device>
    </deviceList>
  </device>
</root>"""

    private val soapResponseXml = """<?xml version="1.0"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
  <s:Body>
    <u:GetExternalIPAddressResponse xmlns:u="urn:schemas-upnp-org:service:WANIPConnection:1">
      <NewExternalIPAddress>203.0.113.45</NewExternalIPAddress>
    </u:GetExternalIPAddressResponse>
  </s:Body>
</s:Envelope>"""

    // --- parseSsdpResponse ---

    @Test
    fun `parseSsdpResponse extrai LOCATION corretamente`() {
        val result = UpnpParser.parseSsdpResponse(ssdpResponseFixture)
        assertNotNull(result)
        assertEquals("http://192.168.1.1:49152/rootDesc.xml", result!!.location)
    }

    @Test
    fun `parseSsdpResponse extrai USN corretamente`() {
        val result = UpnpParser.parseSsdpResponse(ssdpResponseFixture)
        assertNotNull(result)
        assertEquals(
            "uuid:12345678-1234-1234-1234-123456789abc::urn:schemas-upnp-org:device:InternetGatewayDevice:1",
            result!!.usn
        )
    }

    @Test
    fun `parseSsdpResponse extrai SERVER corretamente`() {
        val result = UpnpParser.parseSsdpResponse(ssdpResponseFixture)
        assertNotNull(result)
        assertEquals("Linux/3.10 UPnP/1.1 MiniUPnPd/2.1", result!!.server)
    }

    @Test
    fun `parseSsdpResponse retorna null quando LOCATION esta ausente`() {
        val semLocation = """HTTP/1.1 200 OK
CACHE-CONTROL: max-age=1800
SERVER: Linux/3.10 UPnP/1.1 MiniUPnPd/2.1
ST: urn:schemas-upnp-org:device:InternetGatewayDevice:1"""
        val result = UpnpParser.parseSsdpResponse(semLocation)
        assertNull(result)
    }

    // --- parseUpnpDescription ---

    @Test
    fun `parseUpnpDescription extrai friendlyName corretamente`() {
        val result = UpnpParser.parseUpnpDescription(upnpDescriptionXml, "http://192.168.1.1:49152/rootDesc.xml")
        assertNotNull(result)
        assertEquals("TP-Link Router AC1200", result!!.friendlyName)
    }

    @Test
    fun `parseUpnpDescription extrai manufacturer corretamente`() {
        val result = UpnpParser.parseUpnpDescription(upnpDescriptionXml, "http://192.168.1.1:49152/rootDesc.xml")
        assertNotNull(result)
        assertEquals("TP-Link", result!!.manufacturer)
    }

    @Test
    fun `parseUpnpDescription extrai modelName corretamente`() {
        val result = UpnpParser.parseUpnpDescription(upnpDescriptionXml, "http://192.168.1.1:49152/rootDesc.xml")
        assertNotNull(result)
        assertEquals("Archer C6", result!!.modelName)
    }

    @Test
    fun `parseUpnpDescription resolve controlUrl relativa contra baseUrl`() {
        val result = UpnpParser.parseUpnpDescription(upnpDescriptionXml, "http://192.168.1.1:49152/rootDesc.xml")
        assertNotNull(result)
        assertEquals("http://192.168.1.1:49152/ctl/IPConn", result!!.controlUrl)
    }

    // --- parseSoapGetExternalIpResponse ---

    @Test
    fun `parseSoapGetExternalIpResponse extrai NewExternalIPAddress corretamente`() {
        val result = UpnpParser.parseSoapGetExternalIpResponse(soapResponseXml)
        assertEquals("203.0.113.45", result)
    }

    @Test
    fun `parseSoapGetExternalIpResponse retorna null para XML invalido`() {
        val result = UpnpParser.parseSoapGetExternalIpResponse("nao eh xml valido")
        assertNull(result)
    }
}
