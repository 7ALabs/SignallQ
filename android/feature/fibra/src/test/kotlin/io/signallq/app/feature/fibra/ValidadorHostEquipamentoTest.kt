package io.signallq.app.feature.fibra

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** GH#1213 item 2 — host/IP do NokiaModemClient precisa ser privado/local, nunca público. */
class ValidadorHostEquipamentoTest {

    @Test
    fun `ipv4 RFC-1918 e valido`() {
        assertTrue(ValidadorHostEquipamento.ehIpPrivadoValido("192.168.1.1"))
        assertTrue(ValidadorHostEquipamento.ehIpPrivadoValido("10.0.0.1"))
        assertTrue(ValidadorHostEquipamento.ehIpPrivadoValido("172.16.0.1"))
    }

    @Test
    fun `ipv4 loopback e link-local sao validos`() {
        assertTrue(ValidadorHostEquipamento.ehIpPrivadoValido("127.0.0.1"))
        assertTrue(ValidadorHostEquipamento.ehIpPrivadoValido("169.254.1.1"))
    }

    @Test
    fun `ipv4 publico e invalido`() {
        assertFalse(ValidadorHostEquipamento.ehIpPrivadoValido("1.1.1.1"))
        assertFalse(ValidadorHostEquipamento.ehIpPrivadoValido("8.8.8.8"))
    }

    @Test
    fun `ipv6 loopback link-local e ULA sao validos`() {
        assertTrue(ValidadorHostEquipamento.ehIpPrivadoValido("::1"))
        assertTrue(ValidadorHostEquipamento.ehIpPrivadoValido("fe80::1"))
        assertTrue(ValidadorHostEquipamento.ehIpPrivadoValido("fd12:3456:789a::1"))
    }

    @Test
    fun `ipv6 publico e invalido`() {
        assertFalse(ValidadorHostEquipamento.ehIpPrivadoValido("2606:4700:4700::1111"))
    }

    @Test
    fun `hostname nao-ip e invalido -- nunca resolve DNS pra validar`() {
        assertFalse(ValidadorHostEquipamento.ehIpPrivadoValido("modem.attacker.example"))
        assertFalse(ValidadorHostEquipamento.ehIpPrivadoValido("localhost"))
    }

    @Test
    fun `string vazia ou em branco e invalida`() {
        assertFalse(ValidadorHostEquipamento.ehIpPrivadoValido(""))
        assertFalse(ValidadorHostEquipamento.ehIpPrivadoValido("   "))
    }

    @Test
    fun `octeto fora de faixa e invalido`() {
        assertFalse(ValidadorHostEquipamento.ehIpPrivadoValido("192.168.1.999"))
    }
}
