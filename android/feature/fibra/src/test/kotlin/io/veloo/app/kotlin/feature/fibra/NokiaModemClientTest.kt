package io.signallq.app.feature.fibra

import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * GH#1213 item 2 — o cliente nunca deve ficar de pé com um host público/inválido: a
 * validação acontece no construtor, antes de qualquer chamada de rede (login/fetchPage).
 */
class NokiaModemClientTest {

    @Test
    fun `construir com host publico lanca IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            NokiaModemClient("8.8.8.8")
        }
    }

    @Test
    fun `construir com host privado nao lanca excecao`() {
        NokiaModemClient("192.168.1.1")
    }
}
