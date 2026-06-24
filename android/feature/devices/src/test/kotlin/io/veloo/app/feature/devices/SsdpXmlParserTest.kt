package io.veloo.app.feature.devices

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Testes unitários para o parser de XML de descrição UPnP ([XmlDescricaoUpnpParser])
 * e para a lógica de prioridade/merge de nome e fabricante ([NamingPrioridade]).
 *
 * Todos os testes são puramente funcionais — sem rede, sem Context, sem Android runtime.
 */
class SsdpXmlParserTest {

    // ── Instância reutilizável (sem Context) ────────────────────────────────────
    private val parser = XmlDescricaoUpnpParser

    // ── Testes do parser XML ────────────────────────────────────────────────────

    @Test
    fun `parsear xml completo extrai friendlyName manufacturer e modelName`() {
        val xml = """
            <?xml version="1.0"?>
            <root xmlns="urn:schemas-upnp-org:device-1-0">
                <device>
                    <friendlyName>TV da Sala</friendlyName>
                    <manufacturer>Samsung</manufacturer>
                    <modelName>UE55NU7100</modelName>
                    <deviceType>urn:schemas-upnp-org:device:MediaRenderer:1</deviceType>
                </device>
            </root>
        """.trimIndent()

        val resultado = parser.parsear(xml)

        assertNotNull(resultado)
        assertEquals("TV da Sala", resultado!!.friendlyName)
        assertEquals("Samsung", resultado.manufacturer)
        assertEquals("UE55NU7100", resultado.modelName)
    }

    @Test
    fun `parsear xml sem friendlyName retorna null`() {
        val xml = """
            <?xml version="1.0"?>
            <root>
                <device>
                    <manufacturer>Samsung</manufacturer>
                    <modelName>UE55NU7100</modelName>
                </device>
            </root>
        """.trimIndent()

        val resultado = parser.parsear(xml)

        assertNull(resultado)
    }

    @Test
    fun `parsear xml com friendlyName vazio retorna null`() {
        val xml = """
            <root><device><friendlyName>   </friendlyName><manufacturer>LG</manufacturer></device></root>
        """.trimIndent()

        val resultado = parser.parsear(xml)

        assertNull(resultado)
    }

    @Test
    fun `parsear xml sem manufacturer retorna friendlyName com manufacturer vazio`() {
        val xml = """
            <root><device><friendlyName>Roteador TP-Link</friendlyName><modelName>Archer C6</modelName></device></root>
        """.trimIndent()

        val resultado = parser.parsear(xml)

        assertNotNull(resultado)
        assertEquals("Roteador TP-Link", resultado!!.friendlyName)
        assertEquals("", resultado.manufacturer)
        assertEquals("Archer C6", resultado.modelName)
    }

    @Test
    fun `parsear xml sem modelName retorna modelName vazio`() {
        val xml = """
            <root><device><friendlyName>Chromecast</friendlyName><manufacturer>Google</manufacturer></device></root>
        """.trimIndent()

        val resultado = parser.parsear(xml)

        assertNotNull(resultado)
        assertEquals("Chromecast", resultado!!.friendlyName)
        assertEquals("Google", resultado.manufacturer)
        assertEquals("", resultado.modelName)
    }

    @Test
    fun `parsear xml com tags em uppercase por firmware embarcado`() {
        val xml = """
            <ROOT><DEVICE><FriendlyName>Impressora HP</FriendlyName><Manufacturer>HP</Manufacturer><ModelName>LaserJet Pro M404</ModelName></DEVICE></ROOT>
        """.trimIndent()

        val resultado = parser.parsear(xml)

        assertNotNull(resultado)
        assertEquals("Impressora HP", resultado!!.friendlyName)
        assertEquals("HP", resultado.manufacturer)
        assertEquals("LaserJet Pro M404", resultado.modelName)
    }

    @Test
    fun `parsear xml com atributos nas tags extrai conteudo corretamente`() {
        val xml = """
            <root>
                <device>
                    <friendlyName lang="pt">Smart TV LG</friendlyName>
                    <manufacturer>LG Electronics</manufacturer>
                    <modelName>OLED55C1</modelName>
                </device>
            </root>
        """.trimIndent()

        val resultado = parser.parsear(xml)

        assertNotNull(resultado)
        assertEquals("Smart TV LG", resultado!!.friendlyName)
        assertEquals("LG Electronics", resultado.manufacturer)
    }

    @Test
    fun `parsear xml vazio retorna null`() {
        val resultado = parser.parsear("")
        assertNull(resultado)
    }

    @Test
    fun `parsear xml com caracteres especiais no friendlyName`() {
        val xml = """
            <root><device><friendlyName>João's AirPort Express</friendlyName><manufacturer>Apple</manufacturer></device></root>
        """.trimIndent()

        val resultado = parser.parsear(xml)

        assertNotNull(resultado)
        assertEquals("João's AirPort Express", resultado!!.friendlyName)
    }

    // ── Testes de prioridade/merge de nome e fabricante ────────────────────────

    @Test
    fun `resolverNome prioriza ssdpXml sobre mdnsJmDns`() {
        val nome = NamingPrioridade.resolverNome(
            nomeSsdpXml = "TV da Sala",
            nomeMdns = "Samsung-TV-XYZ",
            nomeHostname = null,
        )
        assertEquals("TV da Sala", nome)
    }

    @Test
    fun `resolverNome usa mdns quando ssdpXml e nulo`() {
        val nome = NamingPrioridade.resolverNome(
            nomeSsdpXml = null,
            nomeMdns = "João's iPhone",
            nomeHostname = null,
        )
        assertEquals("João's iPhone", nome)
    }

    @Test
    fun `resolverNome usa hostname quando ssdpXml e mdns sao nulos`() {
        val nome = NamingPrioridade.resolverNome(
            nomeSsdpXml = null,
            nomeMdns = null,
            nomeHostname = "desktop-joao.local",
        )
        assertEquals("desktop-joao.local", nome)
    }

    @Test
    fun `resolverNome ignora ssdpXml generico e usa mdns`() {
        val nome = NamingPrioridade.resolverNome(
            nomeSsdpXml = "Dispositivo SSDP",
            nomeMdns = "MacBook Pro de Maria",
            nomeHostname = null,
        )
        assertEquals("MacBook Pro de Maria", nome)
    }

    @Test
    fun `resolverNome retorna fallback quando todos sao nulos`() {
        val nome = NamingPrioridade.resolverNome(
            nomeSsdpXml = null,
            nomeMdns = null,
            nomeHostname = null,
            fallback = "Host ativo",
        )
        assertEquals("Host ativo", nome)
    }

    @Test
    fun `resolverFabricante prioriza upnpXml sobre mdns e oui`() {
        val fab = NamingPrioridade.resolverFabricante(
            fabricanteUpnpXml = "Samsung Electronics",
            fabricanteMdns = "Apple",
            fabricanteOui = "Google LLC",
        )
        assertEquals("Samsung Electronics", fab)
    }

    @Test
    fun `resolverFabricante usa mdns quando upnpXml e nulo`() {
        val fab = NamingPrioridade.resolverFabricante(
            fabricanteUpnpXml = null,
            fabricanteMdns = "Google LLC",
            fabricanteOui = "TP-Link",
        )
        assertEquals("Google LLC", fab)
    }

    @Test
    fun `resolverFabricante usa oui quando upnpXml e mdns sao nulos`() {
        val fab = NamingPrioridade.resolverFabricante(
            fabricanteUpnpXml = null,
            fabricanteMdns = null,
            fabricanteOui = "Intelbras",
        )
        assertEquals("Intelbras", fab)
    }

    @Test
    fun `resolverFabricante retorna null quando todos sao nulos`() {
        val fab = NamingPrioridade.resolverFabricante(
            fabricanteUpnpXml = null,
            fabricanteMdns = null,
            fabricanteOui = null,
        )
        assertNull(fab)
    }

    @Test
    fun `resolverFabricante ignora fabricante vazio e usa proximo`() {
        val fab = NamingPrioridade.resolverFabricante(
            fabricanteUpnpXml = "  ",
            fabricanteMdns = "Epson",
            fabricanteOui = null,
        )
        assertEquals("Epson", fab)
    }
}
