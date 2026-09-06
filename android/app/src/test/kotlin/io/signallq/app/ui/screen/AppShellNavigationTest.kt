package io.signallq.app.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class AppShellNavigationTest {
    @Test
    fun `jornada unica inicia em Inicio e expoe quatro raizes`() {
        val navigator = AppShellNavigator(initialTab = AppShellRoot.Home.index)

        assertEquals(AppShellRoot.Home, navigator.selectedRoot)
        assertEquals(
            listOf(
                AppShellRoot.Home,
                AppShellRoot.Speed,
                AppShellRoot.History,
                AppShellRoot.Tools,
            ),
            AppShellRoot.entries.toList(),
        )
    }

    @Test
    fun `switching roots preserves each independent overlay stack`() {
        val navigator = AppShellNavigator(initialTab = AppShellRoot.Home.index)
        navigator.open(AppShellOverlay.SinalWifi)

        navigator.select(AppShellRoot.Tools)
        navigator.open(AppShellOverlay.Dns)
        assertEquals(listOf(AppShellOverlay.Dns), navigator.overlayStack)

        navigator.select(AppShellRoot.Home)
        assertEquals(listOf(AppShellOverlay.SinalWifi), navigator.overlayStack)
    }

    @Test
    fun `back removes only the top overlay from current root`() {
        val navigator = AppShellNavigator(initialTab = AppShellRoot.Speed.index)
        navigator.open(AppShellOverlay.ResultadoVelocidade)
        navigator.open(AppShellOverlay.DetalhesTecnicos)

        assertEquals(AppShellOverlay.DetalhesTecnicos, navigator.pop())
        assertEquals(listOf(AppShellOverlay.ResultadoVelocidade), navigator.overlayStack)
    }

    @Test
    fun `saver restores selected root and all root stacks`() {
        val navigator = AppShellNavigator(initialTab = AppShellRoot.Home.index)
        navigator.open(AppShellOverlay.SinalWifi)
        navigator.select(AppShellRoot.Tools)
        navigator.open(AppShellOverlay.Dns)

        val restored = AppShellNavigator.restoreState(AppShellNavigator.saveState(navigator))

        assertEquals(AppShellRoot.Tools, restored.selectedRoot)
        assertEquals(listOf(AppShellOverlay.Dns), restored.overlayStack)
        restored.select(AppShellRoot.Home)
        assertEquals(listOf(AppShellOverlay.SinalWifi), restored.overlayStack)
    }

    @Test
    fun `duplicate overlay is not pushed twice`() {
        val navigator = AppShellNavigator(initialTab = AppShellRoot.Home.index)

        navigator.open(AppShellOverlay.Perfil)
        navigator.open(AppShellOverlay.Perfil)

        assertEquals(1, navigator.overlayStack.size)
        assertEquals(AppShellOverlay.Perfil, navigator.overlayStack.single())
    }

    @Test
    fun `perfil ajustes preserves back stack and restoration`() {
        val navigator = AppShellNavigator(initialTab = AppShellRoot.Home.index)
        navigator.open(AppShellOverlay.Perfil)
        navigator.open(AppShellOverlay.Ajustes)

        val restored = AppShellNavigator.restoreState(AppShellNavigator.saveState(navigator))

        assertEquals(listOf(AppShellOverlay.Perfil, AppShellOverlay.Ajustes), restored.overlayStack)
        assertEquals(AppShellOverlay.Ajustes, restored.pop())
        assertEquals(listOf(AppShellOverlay.Perfil), restored.overlayStack)
        assertEquals(AppShellOverlay.Perfil, restored.pop())
        assertEquals(true, restored.isAtRoot)
    }

    @Test
    fun `sinal canais is a deep restorable route from tools`() {
        val navigator = AppShellNavigator(initialTab = AppShellRoot.Tools.index)
        navigator.open(AppShellOverlay.SinalCanais)

        val restored = AppShellNavigator.restoreState(AppShellNavigator.saveState(navigator))

        assertEquals(listOf(AppShellOverlay.SinalCanais), restored.overlayStack)
        assertEquals(AppShellOverlay.SinalCanais, restored.pop())
        assertEquals(true, restored.isAtRoot)
    }

    @Test
    fun `destinos aplicaveis da trilha voltam para Inicio`() {
        val navigator = AppShellNavigator(initialTab = AppShellRoot.Home.index)

        listOf(
            AppShellOverlay.EquipamentoInternet,
            AppShellOverlay.SinalWifi,
            AppShellOverlay.SinalCanais,
        ).forEach { destination ->
            navigator.open(destination)
            assertEquals(destination, navigator.pop())
            assertEquals(AppShellRoot.Home, navigator.selectedRoot)
            assertEquals(true, navigator.isAtRoot)
        }
    }

    @Test
    fun `bar visibility follows root overlay and running speedtest contracts`() {
        assertEquals(true, shouldShowAppShellBottomBar(isAtRoot = true, speedtestRunning = false))
        assertEquals(false, shouldShowAppShellBottomBar(isAtRoot = false, speedtestRunning = false))
        assertEquals(false, shouldShowAppShellBottomBar(isAtRoot = true, speedtestRunning = true))
    }

    @Test
    fun `screen view mapping remains stable without positional lookup`() {
        assertEquals(
            listOf("home", "speedtest", "historico", "ferramentas"),
            AppShellRoot.entries.map(AppShellRoot::screenName),
        )
    }
}
