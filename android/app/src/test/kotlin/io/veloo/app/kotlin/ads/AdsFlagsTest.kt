package io.signallq.app.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Issue #555 -- elegibilidade do slot de anuncio: chave mestra + chave por tela. */
class AdsFlagsTest {
    @Test
    fun `default e tudo desligado -- fallback seguro`() {
        assertFalse(AdsFlags.DESLIGADO.masterEnabled)
        AdSlot.entries.forEach { assertFalse(AdsFlags.DESLIGADO.habilitadoPara(it)) }
    }

    @Test
    fun `master desligada bloqueia mesmo com a flag da tela ligada`() {
        val flags = AdsFlags(masterEnabled = false, velocidade = true, resultado = true, dispositivos = true, historico = true)
        AdSlot.entries.forEach { assertFalse(flags.habilitadoPara(it)) }
    }

    @Test
    fun `master ligada mas tela especifica desligada bloqueia so aquela tela`() {
        val flags =
            AdsFlags(
                masterEnabled = true,
                velocidade = true,
                resultado = false,
                dispositivos = true,
                historico = true,
            )
        assertTrue(flags.habilitadoPara(AdSlot.VELOCIDADE))
        assertFalse(flags.habilitadoPara(AdSlot.RESULTADO))
        assertTrue(flags.habilitadoPara(AdSlot.DISPOSITIVOS))
        assertTrue(flags.habilitadoPara(AdSlot.HISTORICO))
    }

    @Test
    fun `master e tela ligadas habilita`() {
        val flags = AdsFlags(masterEnabled = true, historico = true)
        assertTrue(flags.habilitadoPara(AdSlot.HISTORICO))
        assertFalse(flags.habilitadoPara(AdSlot.VELOCIDADE))
    }
}
