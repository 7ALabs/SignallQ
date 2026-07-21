package io.signallq.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** GH#1226 item 9/RF-I — cobertura declarada no catálogo, não lista hardcoded na UI. */
class OperatorScopeTest {
    @Test
    fun `as 4 operadoras nacionais tem scope NATIONAL`() {
        val nacionaisEsperadas = setOf("vivo_fibra", "claro_net", "tim_live", "nio")
        val nacionaisReais =
            BancoOperadoras.lista
                .filter { it.scope == OperatorScope.NATIONAL }
                .map { it.id }
                .toSet()
        assertEquals(nacionaisEsperadas, nacionaisReais)
    }

    @Test
    fun `demais operadoras do catalogo sao regionais por padrao`() {
        val regionais = BancoOperadoras.lista.filter { it.scope == OperatorScope.REGIONAL }
        assertTrue(regionais.size >= 10)
        assertTrue(regionais.none { it.id in setOf("vivo_fibra", "claro_net", "tim_live", "nio") })
    }
}
