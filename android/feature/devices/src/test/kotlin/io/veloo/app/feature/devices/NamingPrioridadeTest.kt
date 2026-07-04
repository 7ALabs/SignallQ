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
    fun `rotuloFallbackGenerico sem fabricante retorna Dispositivo generico`() {
        assertEquals("Dispositivo", NamingPrioridade.rotuloFallbackGenerico(null))
    }

    @Test
    fun `rotuloFallbackGenerico com fabricante em branco retorna Dispositivo generico`() {
        assertEquals("Dispositivo", NamingPrioridade.rotuloFallbackGenerico("   "))
    }
}
