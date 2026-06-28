package io.signallq.app

import io.signallq.app.ui.screen.isDnsIpPrivado
import io.signallq.app.ui.screen.resolveDnsName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsScreenTest {
    @Test
    fun `resolveDnsName retorna Cloudflare para IPs primario e secundario`() {
        assertEquals("Cloudflare", resolveDnsName("1.1.1.1"))
        assertEquals("Cloudflare", resolveDnsName("1.0.0.1"))
    }

    @Test
    fun `resolveDnsName retorna Google DNS para IPs do Google`() {
        assertEquals("Google DNS", resolveDnsName("8.8.8.8"))
        assertEquals("Google DNS", resolveDnsName("8.8.4.4"))
    }

    @Test
    fun `resolveDnsName retorna Quad9 para IPs do Quad9`() {
        assertEquals("Quad9", resolveDnsName("9.9.9.9"))
        assertEquals("Quad9", resolveDnsName("149.112.112.112"))
    }

    @Test
    fun `resolveDnsName retorna OpenDNS para IPs do OpenDNS`() {
        assertEquals("OpenDNS", resolveDnsName("208.67.222.222"))
        assertEquals("OpenDNS", resolveDnsName("208.67.220.220"))
    }

    @Test
    fun `resolveDnsName retorna AdGuard para IPs do AdGuard`() {
        assertEquals("AdGuard", resolveDnsName("94.140.14.14"))
        assertEquals("AdGuard", resolveDnsName("94.140.15.15"))
    }

    @Test
    fun `resolveDnsName retorna Roteador da rede para IPs privados`() {
        assertEquals("Roteador da rede", resolveDnsName("192.168.1.1"))
        assertEquals("Roteador da rede", resolveDnsName("192.168.1.254"))
        assertEquals("Roteador da rede", resolveDnsName("10.0.0.1"))
        assertEquals("Roteador da rede", resolveDnsName("172.16.0.1"))
        assertEquals("Roteador da rede", resolveDnsName("172.31.255.255"))
        assertEquals("Roteador da rede", resolveDnsName("169.254.1.1"))
    }

    @Test
    fun `resolveDnsName retorna DNS do Provedor para IP publico desconhecido`() {
        assertEquals("DNS do Provedor", resolveDnsName("200.200.200.200"))
        assertEquals("DNS do Provedor", resolveDnsName("1.2.3.4"))
    }

    @Test
    fun `resolveDnsName retorna DNS do Provedor para null`() {
        assertEquals("DNS do Provedor", resolveDnsName(null))
    }

    @Test
    fun `isDnsIpPrivado detecta corretamente IPs RFC-1918 e link-local`() {
        assertTrue(isDnsIpPrivado("192.168.0.1"))
        assertTrue(isDnsIpPrivado("192.168.1.254"))
        assertTrue(isDnsIpPrivado("10.0.0.1"))
        assertTrue(isDnsIpPrivado("10.255.255.255"))
        assertTrue(isDnsIpPrivado("172.16.0.1"))
        assertTrue(isDnsIpPrivado("172.31.255.255"))
        assertTrue(isDnsIpPrivado("169.254.0.1"))
        assertTrue(isDnsIpPrivado("127.0.0.1"))
    }

    @Test
    fun `isDnsIpPrivado retorna false para IPs publicos`() {
        assertFalse(isDnsIpPrivado("1.1.1.1"))
        assertFalse(isDnsIpPrivado("8.8.8.8"))
        assertFalse(isDnsIpPrivado("172.15.255.255"))
        assertFalse(isDnsIpPrivado("172.32.0.0"))
        assertFalse(isDnsIpPrivado("192.167.1.1"))
        assertFalse(isDnsIpPrivado("11.0.0.1"))
    }
}
