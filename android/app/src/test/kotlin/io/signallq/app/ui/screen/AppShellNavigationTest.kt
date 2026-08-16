package io.signallq.app.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class AppShellNavigationTest {
    @Test
    fun `guided shell starts on Home and exposes four roots`() {
        val navigator = AppShellNavigator(initialTab = AppShellRoot.Home.legacyIndex)

        assertEquals(AppShellRoot.Home, navigator.selectedRoot)
        assertEquals(
            listOf(
                AppShellRoot.Home,
                AppShellRoot.Speed,
                AppShellRoot.History,
                AppShellRoot.Tools,
            ),
            AppShellMode.Guided2.roots(),
        )
    }

    @Test
    fun `legacy shell remains available with Wifi root`() {
        assertEquals(AppShellRoot.entries, AppShellMode.Legacy.roots())
    }

    @Test
    fun `switching roots preserves each independent overlay stack`() {
        val navigator = AppShellNavigator(initialTab = AppShellRoot.Home.legacyIndex)
        navigator.open(AppShellOverlay.SinalWifi)

        navigator.select(AppShellRoot.Tools)
        navigator.open(AppShellOverlay.Dns)
        assertEquals(listOf(AppShellOverlay.Dns), navigator.overlayStack)

        navigator.select(AppShellRoot.Home)
        assertEquals(listOf(AppShellOverlay.SinalWifi), navigator.overlayStack)
    }

    @Test
    fun `back removes only the top overlay from current root`() {
        val navigator = AppShellNavigator(initialTab = AppShellRoot.Speed.legacyIndex)
        navigator.open(AppShellOverlay.ResultadoVelocidade)
        navigator.open(AppShellOverlay.DetalhesTecnicos)

        assertEquals(AppShellOverlay.DetalhesTecnicos, navigator.pop())
        assertEquals(listOf(AppShellOverlay.ResultadoVelocidade), navigator.overlayStack)
    }

    @Test
    fun `saver restores selected root and all root stacks`() {
        val navigator = AppShellNavigator(initialTab = AppShellRoot.Home.legacyIndex)
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
        val navigator = AppShellNavigator(initialTab = AppShellRoot.Home.legacyIndex)

        navigator.open(AppShellOverlay.Perfil)
        navigator.open(AppShellOverlay.Perfil)

        assertEquals(1, navigator.overlayStack.size)
        assertEquals(AppShellOverlay.Perfil, navigator.overlayStack.single())
    }

    @Test
    fun `perfil ajustes preserves back stack and restoration`() {
        val navigator = AppShellNavigator(initialTab = AppShellRoot.Home.legacyIndex)
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
        val navigator = AppShellNavigator(initialTab = AppShellRoot.Tools.legacyIndex)
        navigator.open(AppShellOverlay.SinalCanais)

        val restored = AppShellNavigator.restoreState(AppShellNavigator.saveState(navigator))

        assertEquals(listOf(AppShellOverlay.SinalCanais), restored.overlayStack)
        assertEquals(AppShellOverlay.SinalCanais, restored.pop())
        assertEquals(true, restored.isAtRoot)
    }

    @Test
    fun `bar visibility follows root overlay and running speedtest contracts`() {
        assertEquals(true, shouldShowAppShellBottomBar(AppShellMode.Guided2, isAtRoot = true, speedtestRunning = false))
        assertEquals(false, shouldShowAppShellBottomBar(AppShellMode.Guided2, isAtRoot = false, speedtestRunning = false))
        assertEquals(false, shouldShowAppShellBottomBar(AppShellMode.Guided2, isAtRoot = true, speedtestRunning = true))
        assertEquals(true, shouldShowAppShellBottomBar(AppShellMode.Legacy, isAtRoot = false, speedtestRunning = false))
    }

    @Test
    fun `screen view mapping remains stable without positional lookup`() {
        assertEquals(
            listOf("home", "speedtest", "sinal_wifi", "historico", "ferramentas"),
            AppShellRoot.entries.map(AppShellRoot::screenName),
        )
    }

    @Test
    fun `rollout flag defaults Legacy and enables Guided2 explicitly`() {
        assertEquals(AppShellMode.Legacy, AppShellFeatureFlagsState().shellMode)
        assertEquals(AppShellMode.Guided2, AppShellFeatureFlagsState(guidedShell2Enabled = true).shellMode)
    }
}
