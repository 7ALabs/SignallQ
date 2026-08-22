package io.signallq.app.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre o gate de navegacao dos 9 modulos feature do Consumer (Epico #1347, F4/#1480) --
 * funcoes puras extraidas de `AppShell.kt` (ver `AppShellFeatureGating.kt`). Nao testa
 * Compose/UI: so a decisao de habilitar/bloquear rota e o efeito colateral de analytics,
 * que e o que o criterio de aceite da issue #1480 pede ("modulo desligado bloqueia a
 * rota/navegacao").
 */
class AppShellFeatureGatingTest {
    @Test
    fun `tabHabilitada mapeia cada indice de tab fixa pro campo certo`() {
        val flags =
            AppShellFeatureFlagsState(
                homeEnabled = false,
                speedtestEnabled = true,
                historyEnabled = true,
            )

        assertFalse(flags.tabHabilitada(0)) // Home
        assertTrue(flags.tabHabilitada(1)) // Velocidade
        assertTrue(flags.tabHabilitada(2)) // Historico
        assertTrue(flags.tabHabilitada(3)) // Ferramentas -- nunca gateada
    }

    @Test
    fun `tabModuleId retorna null para Ferramentas -- hub nao pertence a um modulo`() {
        assertEquals("home", tabModuleId(0))
        assertEquals("speedtest", tabModuleId(1))
        assertEquals("history", tabModuleId(2))
        assertEquals(null, tabModuleId(3))
    }

    @Test
    fun `primeiraTabHabilitada prioriza Inicio quando tudo ligado`() {
        assertEquals(0, AppShellFeatureFlagsState().primeiraTabHabilitada())
    }

    @Test
    fun `primeiraTabHabilitada pula Inicio e Velocidade`() {
        val flags = AppShellFeatureFlagsState(speedtestEnabled = false, homeEnabled = false)
        assertEquals(2, flags.primeiraTabHabilitada())
    }

    @Test
    fun `primeiraTabHabilitada cai em Ferramentas quando as raizes gateadas estao desligadas`() {
        val flags =
            AppShellFeatureFlagsState(
                homeEnabled = false,
                speedtestEnabled = false,
                historyEnabled = false,
            )
        assertEquals(3, flags.primeiraTabHabilitada())
    }

    @Test
    fun `permitirOuBloquear com flag ligada nao dispara onFeatureBlocked e retorna true`() {
        var bloqueado: String? = null
        val flags = AppShellFeatureFlagsState(onFeatureBlocked = { bloqueado = it })

        val permitido = flags.permitirOuBloquear(habilitada = true, moduleId = "dns")

        assertTrue(permitido)
        assertEquals(null, bloqueado)
    }

    @Test
    fun `permitirOuBloquear com flag desligada dispara onFeatureBlocked com o moduleId e retorna false`() {
        var bloqueado: String? = null
        val flags = AppShellFeatureFlagsState(onFeatureBlocked = { bloqueado = it })

        val permitido = flags.permitirOuBloquear(habilitada = false, moduleId = "fibra")

        assertFalse(permitido)
        assertEquals("fibra", bloqueado)
    }
}
