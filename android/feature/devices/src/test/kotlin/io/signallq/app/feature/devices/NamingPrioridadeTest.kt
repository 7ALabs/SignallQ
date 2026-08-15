package io.signallq.app.feature.devices

import io.signallq.app.core.network.contracts.localdevice.ClientSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Testes unitários para [NamingPrioridade].
 * Cobre resolução de nome/fabricante e o fallback de rótulo genérico via OUI (issue #394).
 */
class NamingPrioridadeTest {

    @Test
    fun `resolverNome prefere ssdp sobre mdns e hostname`() {
        val nome = NamingPrioridade.resolverNome(
            nomeSsdpXml = "Smart TV Samsung",
            nomeMdns = "nome-mdns",
            nomeHostname = "host.lan",
        )
        assertEquals("Smart TV Samsung", nome)
    }

    @Test
    fun `resolverNome ignora nomes genericos e cai para hostname`() {
        val nome = NamingPrioridade.resolverNome(
            nomeSsdpXml = "Dispositivo não identificado",
            nomeMdns = null,
            nomeHostname = "notebook.lan",
        )
        assertEquals("notebook.lan", nome)
    }

    @Test
    fun `resolverNome usa fallback quando tudo ausente ou generico`() {
        val nome = NamingPrioridade.resolverNome(
            nomeSsdpXml = null,
            nomeMdns = "Host ativo",
            nomeHostname = null,
            fallback = "Dispositivo",
        )
        assertEquals("Dispositivo", nome)
    }

    @Test
    fun `FONTE_NOME_ROUTER_ACTIVE e distinta das fontes passivas existentes`() {
        val fontesPassivas = setOf("ssdp", "ssdpXml", "mdnsJmDns", "subnetMdns", "arp", "subnet", "tcpProbe", "gateway")
        assertEquals("routerActive", NamingPrioridade.FONTE_NOME_ROUTER_ACTIVE)
        assert(NamingPrioridade.FONTE_NOME_ROUTER_ACTIVE !in fontesPassivas)
    }

    @Test
    fun `resolverNomeRouterActive retorna hostname e fonte confirmada quando MAC bate e hostname e valido`() {
        val clientes = listOf(ClientSnapshot(mac = "AA:BB:CC:DD:EE:FF", ip = "192.168.1.10", hostname = "notebook-camilo", tipoConexao = "wired"))
        val resultado = NamingPrioridade.resolverNomeRouterActive("aa:bb:cc:dd:ee:ff", clientes)
        assertEquals("notebook-camilo", resultado?.nome)
        assertEquals(NamingPrioridade.FONTE_NOME_ROUTER_ACTIVE, resultado?.fonte)
    }

    @Test
    fun `resolverNomeRouterActive normaliza MAC ignorando case e separador`() {
        val clientes = listOf(ClientSnapshot(mac = "aabbccddeeff", ip = "192.168.1.10", hostname = "tablet-cozinha", tipoConexao = "wifi"))
        val resultado = NamingPrioridade.resolverNomeRouterActive("AA-BB-CC-DD-EE-FF", clientes)
        assertEquals("tablet-cozinha", resultado?.nome)
    }

    @Test
    fun `resolverNomeRouterActive retorna null quando MAC nao bate com nenhum cliente`() {
        val clientes = listOf(ClientSnapshot(mac = "11:22:33:44:55:66", ip = "192.168.1.5", hostname = "outro-device", tipoConexao = "wired"))
        val resultado = NamingPrioridade.resolverNomeRouterActive("aa:bb:cc:dd:ee:ff", clientes)
        assertNull(resultado)
    }

    @Test
    fun `resolverNomeRouterActive retorna null quando hostname do cliente e nulo`() {
        val clientes = listOf(ClientSnapshot(mac = "aa:bb:cc:dd:ee:ff", ip = "192.168.1.10", hostname = null, tipoConexao = "wired"))
        val resultado = NamingPrioridade.resolverNomeRouterActive("aa:bb:cc:dd:ee:ff", clientes)
        assertNull(resultado)
    }

    @Test
    fun `resolverNomeRouterActive retorna null quando hostname do cliente e em branco`() {
        val clientes = listOf(ClientSnapshot(mac = "aa:bb:cc:dd:ee:ff", ip = "192.168.1.10", hostname = "   ", tipoConexao = "wired"))
        val resultado = NamingPrioridade.resolverNomeRouterActive("aa:bb:cc:dd:ee:ff", clientes)
        assertNull(resultado)
    }

    @Test
    fun `resolverNomeRouterActive retorna null quando hostname do cliente e generico`() {
        val clientes = listOf(ClientSnapshot(mac = "aa:bb:cc:dd:ee:ff", ip = "192.168.1.10", hostname = "Host ativo", tipoConexao = "wired"))
        val resultado = NamingPrioridade.resolverNomeRouterActive("aa:bb:cc:dd:ee:ff", clientes)
        assertNull(resultado)
    }

    @Test
    fun `resolverNomeRouterActive retorna null quando MAC do dispositivo e nulo`() {
        val clientes = listOf(ClientSnapshot(mac = "aa:bb:cc:dd:ee:ff", ip = "192.168.1.10", hostname = "notebook", tipoConexao = "wired"))
        val resultado = NamingPrioridade.resolverNomeRouterActive(null, clientes)
        assertNull(resultado)
    }

    @Test
    fun `resolverNomeRouterActive retorna null quando lista de clientes esta vazia`() {
        val resultado = NamingPrioridade.resolverNomeRouterActive("aa:bb:cc:dd:ee:ff", emptyList())
        assertNull(resultado)
    }

    @Test
    fun `resolverNomeRouterActive casa por IP quando MAC do dispositivo e nulo -- fonte e provavel, nao confirmada`() {
        val clientes = listOf(ClientSnapshot(mac = "aa:bb:cc:dd:ee:ff", ip = "192.168.1.66", hostname = "ESP_7FE810", tipoConexao = "wifi"))
        val resultado = NamingPrioridade.resolverNomeRouterActive(macDispositivo = null, clientesGateway = clientes, ipDispositivo = "192.168.1.66")
        assertEquals("ESP_7FE810", resultado?.nome)
        // GH#1217 item 2 -- match só por IP nunca é "confirmado", só "provável".
        assertEquals(NamingPrioridade.FONTE_NOME_ROUTER_ACTIVE_IP, resultado?.fonte)
    }

    @Test
    fun `resolverNomeRouterActive prefere match por MAC mesmo com IP tambem disponivel`() {
        val clientes = listOf(
            ClientSnapshot(mac = "aa:bb:cc:dd:ee:ff", ip = "192.168.1.66", hostname = "por-mac", tipoConexao = "wifi"),
            ClientSnapshot(mac = "11:22:33:44:55:66", ip = "192.168.1.67", hostname = "por-ip", tipoConexao = "wifi"),
        )
        val resultado = NamingPrioridade.resolverNomeRouterActive(
            macDispositivo = "aa:bb:cc:dd:ee:ff",
            clientesGateway = clientes,
            ipDispositivo = "192.168.1.67",
        )
        assertEquals("por-mac", resultado?.nome)
        // Match por MAC vence e é confirmado, mesmo com um IP também disponível na lista.
        assertEquals(NamingPrioridade.FONTE_NOME_ROUTER_ACTIVE, resultado?.fonte)
    }

    @Test
    fun `resolverNomeRouterActive retorna null quando IP nao bate com nenhum cliente`() {
        val clientes = listOf(ClientSnapshot(mac = null, ip = "192.168.1.10", hostname = "notebook", tipoConexao = "wired"))
        val resultado = NamingPrioridade.resolverNomeRouterActive(macDispositivo = null, clientesGateway = clientes, ipDispositivo = "192.168.1.99")
        assertNull(resultado)
    }

    @Test
    fun `resolverNomeRouterActive trata hostname sintetico Unknown_MAC do roteador como generico`() {
        val clientes = listOf(ClientSnapshot(mac = "aa:bb:cc:dd:ee:ff", ip = "192.168.1.10", hostname = "Unknown_aa:bb:cc:dd:ee:ff", tipoConexao = "wifi"))
        val resultado = NamingPrioridade.resolverNomeRouterActive("aa:bb:cc:dd:ee:ff", clientes)
        assertNull(resultado)
    }

    @Test
    fun `rotuloFallbackGenerico com fabricante retorna Dispositivo mais fabricante`() {
        assertEquals("Dispositivo Samsung", NamingPrioridade.rotuloFallbackGenerico("Samsung"))
        assertEquals("Dispositivo Apple", NamingPrioridade.rotuloFallbackGenerico("Apple"))
    }

    @Test
    fun `rotuloFallbackGenerico sem fabricante retorna Dispositivo desconhecido`() {
        assertEquals("Dispositivo desconhecido", NamingPrioridade.rotuloFallbackGenerico(null))
    }

    @Test
    fun `rotuloFallbackGenerico com fabricante em branco retorna Dispositivo desconhecido`() {
        assertEquals("Dispositivo desconhecido", NamingPrioridade.rotuloFallbackGenerico("   "))
    }

    @Test
    fun `capitalizarFabricante capitaliza primeira letra de manufacturer lowercase`() {
        assertEquals("Samsung", NamingPrioridade.capitalizarFabricante("samsung"))
        assertEquals("Xiaomi", NamingPrioridade.capitalizarFabricante("xiaomi"))
    }

    @Test
    fun `capitalizarFabricante retorna null para manufacturer nulo ou em branco`() {
        assertEquals(null, NamingPrioridade.capitalizarFabricante(null))
        assertEquals(null, NamingPrioridade.capitalizarFabricante("   "))
    }

    @Test
    fun `nomeAmigavelDoDevice combina fabricante e modelo quando modelo nao repete fabricante`() {
        assertEquals(
            "Samsung SM-A256E",
            NamingPrioridade.nomeAmigavelDoDevice(modelo = "SM-A256E", fabricante = "Samsung"),
        )
    }

    @Test
    fun `nomeAmigavelDoDevice nao duplica fabricante quando modelo ja comeca com ele`() {
        assertEquals(
            "Samsung Galaxy S23",
            NamingPrioridade.nomeAmigavelDoDevice(modelo = "Samsung Galaxy S23", fabricante = "Samsung"),
        )
    }

    @Test
    fun `nomeAmigavelDoDevice usa so o modelo quando fabricante ausente`() {
        assertEquals("SM-A256E", NamingPrioridade.nomeAmigavelDoDevice(modelo = "SM-A256E", fabricante = null))
    }

    @Test
    fun `nomeAmigavelDoDevice usa so o fabricante quando modelo ausente`() {
        assertEquals("Samsung", NamingPrioridade.nomeAmigavelDoDevice(modelo = null, fabricante = "Samsung"))
    }

    @Test
    fun `nomeAmigavelDoDevice cai para Este aparelho quando ambos ausentes`() {
        assertEquals("Este aparelho", NamingPrioridade.nomeAmigavelDoDevice(modelo = null, fabricante = null))
    }
}
