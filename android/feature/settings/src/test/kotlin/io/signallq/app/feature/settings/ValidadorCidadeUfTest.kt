package io.signallq.app.feature.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidadorCidadeUfTest {

    @Test
    fun `ambos vazios sao validos (nada a validar)`() {
        assertTrue(ValidadorCidadeUf.ehCombinacaoValida(null, null))
        assertTrue(ValidadorCidadeUf.ehCombinacaoValida("", ""))
        assertTrue(ValidadorCidadeUf.ehCombinacaoValida("   ", null))
    }

    @Test
    fun `ambos preenchidos com UF real sao validos`() {
        assertTrue(ValidadorCidadeUf.ehCombinacaoValida("São Paulo", "SP"))
        assertTrue(ValidadorCidadeUf.ehCombinacaoValida("Rio de Janeiro", "rj"))
    }

    @Test
    fun `cidade preenchida sem UF e invalida`() {
        assertFalse(ValidadorCidadeUf.ehCombinacaoValida("São Paulo", null))
        assertFalse(ValidadorCidadeUf.ehCombinacaoValida("São Paulo", ""))
    }

    @Test
    fun `UF preenchida sem cidade e invalida`() {
        assertFalse(ValidadorCidadeUf.ehCombinacaoValida(null, "SP"))
        assertFalse(ValidadorCidadeUf.ehCombinacaoValida("", "SP"))
    }

    @Test
    fun `UF que nao existe e invalida mesmo com cidade preenchida`() {
        assertFalse(ValidadorCidadeUf.ehCombinacaoValida("Cidade Fictícia", "XX"))
    }
}
