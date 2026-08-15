package io.signallq.app.feature.dns

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** GH#1212 item 10 — cobre IPv4 (comportamento preservado) e IPv6 (novo). */
class DetectorEnderecoIpPrivadoTest {

    @Test
    fun `ipv4 RFC-1918 e detectado como privado`() {
        assertTrue(DetectorEnderecoIpPrivado.ehPrivadoOuLocal("192.168.1.1"))
        assertTrue(DetectorEnderecoIpPrivado.ehPrivadoOuLocal("10.0.0.1"))
        assertTrue(DetectorEnderecoIpPrivado.ehPrivadoOuLocal("172.16.0.1"))
    }

    @Test
    fun `ipv4 loopback e link-local sao detectados como privados`() {
        assertTrue(DetectorEnderecoIpPrivado.ehPrivadoOuLocal("127.0.0.1"))
        assertTrue(DetectorEnderecoIpPrivado.ehPrivadoOuLocal("169.254.1.1"))
    }

    @Test
    fun `ipv4 publico nao e detectado como privado`() {
        assertFalse(DetectorEnderecoIpPrivado.ehPrivadoOuLocal("1.1.1.1"))
        assertFalse(DetectorEnderecoIpPrivado.ehPrivadoOuLocal("8.8.8.8"))
    }

    @Test
    fun `ipv6 loopback e detectado como privado`() {
        assertTrue(DetectorEnderecoIpPrivado.ehPrivadoOuLocal("::1"))
    }

    @Test
    fun `ipv6 link-local fe80 e detectado como privado`() {
        assertTrue(DetectorEnderecoIpPrivado.ehPrivadoOuLocal("fe80::1"))
    }

    @Test
    fun `ipv6 ULA fc00 e fd00 sao detectados como privados`() {
        assertTrue(DetectorEnderecoIpPrivado.ehPrivadoOuLocal("fc00::1"))
        assertTrue(DetectorEnderecoIpPrivado.ehPrivadoOuLocal("fd12:3456:789a::1"))
    }

    @Test
    fun `ipv6 publico nao e detectado como privado`() {
        // Cloudflare (2606:4700:4700::1111) e Google (2001:4860:4860::8888) publicos.
        assertFalse(DetectorEnderecoIpPrivado.ehPrivadoOuLocal("2606:4700:4700::1111"))
        assertFalse(DetectorEnderecoIpPrivado.ehPrivadoOuLocal("2001:4860:4860::8888"))
    }
}
