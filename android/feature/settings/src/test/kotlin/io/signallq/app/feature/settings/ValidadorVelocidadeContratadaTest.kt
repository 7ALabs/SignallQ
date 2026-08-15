package io.signallq.app.feature.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** GH#1227 item 5/RF-D — zero, negativo e valores absurdos não podem ser aceitos. */
class ValidadorVelocidadeContratadaTest {

    @Test
    fun `nulo (campo vazio) e sempre valido -- estado explicito de sem dado`() {
        assertTrue(ValidadorVelocidadeContratada.ehValida(null))
    }

    @Test
    fun `zero e invalido -- nao pode significar desligado e velocidade real ao mesmo tempo`() {
        assertFalse(ValidadorVelocidadeContratada.ehValida(0))
    }

    @Test
    fun `negativo e invalido`() {
        assertFalse(ValidadorVelocidadeContratada.ehValida(-100))
    }

    @Test
    fun `valor tipico de plano residencial e valido`() {
        assertTrue(ValidadorVelocidadeContratada.ehValida(500))
    }

    @Test
    fun `valor absurdo acima do teto e invalido`() {
        assertFalse(ValidadorVelocidadeContratada.ehValida(1_000_000))
    }

    @Test
    fun `valor exatamente no teto e valido`() {
        assertTrue(ValidadorVelocidadeContratada.ehValida(ValidadorVelocidadeContratada.LIMITE_SUPERIOR_MBPS))
    }
}
