package io.signallq.app.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class SinalWifiScreenTest {
    @Test
    fun `rotulo de recursos nao afirma suporte quando a capacidade nao foi informada`() {
        assertEquals("MU-MIMO", recursosWifiLabel(true))
        assertEquals("MU-MIMO não suportado", recursosWifiLabel(false))
        assertEquals("Não informado", recursosWifiLabel(null))
    }
}
