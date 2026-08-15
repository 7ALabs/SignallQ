package io.signallq.app.sinalwifi

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `calcularSuportaMuMimo` mapeia o rótulo de padrão Wi-Fi (já formatado por [calcularPadraoWifi])
 * para suporte a MU-MIMO -- GH#1201. Wi-Fi 4 (802.11n) não tem MU-MIMO; 5/6/6E/7 têm.
 */
class CalcularSuportaMuMimoTest {
    @Test
    fun `Wi-Fi 4 (n) nao suporta MU-MIMO`() {
        assertEquals(false, calcularSuportaMuMimo("Wi-Fi 4 (n)"))
    }

    @Test
    fun `Wi-Fi 5 (ac) suporta MU-MIMO`() {
        assertEquals(true, calcularSuportaMuMimo("Wi-Fi 5 (ac)"))
    }

    @Test
    fun `Wi-Fi 6 (ax) suporta MU-MIMO`() {
        assertEquals(true, calcularSuportaMuMimo("Wi-Fi 6 (ax)"))
    }

    @Test
    fun `Wi-Fi 6E (ax) suporta MU-MIMO`() {
        assertEquals(true, calcularSuportaMuMimo("Wi-Fi 6E (ax)"))
    }

    @Test
    fun `Wi-Fi 7 (be) suporta MU-MIMO`() {
        assertEquals(true, calcularSuportaMuMimo("Wi-Fi 7 (be)"))
    }

    @Test
    fun `padrao nulo retorna nulo`() {
        assertEquals(null, calcularSuportaMuMimo(null))
    }
}
