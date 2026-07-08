package io.signallq.app.feature.devices

import org.junit.Assert.assertEquals
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
