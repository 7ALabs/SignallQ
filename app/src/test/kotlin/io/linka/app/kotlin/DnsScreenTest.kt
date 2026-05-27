package io.linka.app.kotlin

import io.linka.app.kotlin.ui.screen.resolveDnsName
import org.junit.Assert.assertEquals
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
    fun `resolveDnsName retorna DNS do Provedor para IP desconhecido`() {
        assertEquals("DNS do Provedor", resolveDnsName("192.168.1.1"))
        assertEquals("DNS do Provedor", resolveDnsName("10.0.0.1"))
    }

    @Test
    fun `resolveDnsName retorna DNS do Provedor para null`() {
        assertEquals("DNS do Provedor", resolveDnsName(null))
    }
}
