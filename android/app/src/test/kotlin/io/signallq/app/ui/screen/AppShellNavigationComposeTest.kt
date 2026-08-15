package io.signallq.app.ui.screen

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.lightTokens
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppShellNavigationComposeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `rememberSaveable restores root and overlay through registry recreation`() {
        val restoration = StateRestorationTester(composeRule)
        lateinit var navigator: AppShellNavigator
        restoration.setContent {
            navigator = rememberAppShellNavigator(AppShellMode.Guided2)
            Text(navigator.selectedRoot.name)
        }
        composeRule.runOnIdle {
            navigator.select(AppShellRoot.Tools)
            navigator.open(AppShellOverlay.Dns)
        }

        restoration.emulateSavedInstanceStateRestore()

        composeRule.runOnIdle {
            assertEquals(AppShellRoot.Tools, navigator.selectedRoot)
            assertEquals(AppShellOverlay.Dns, navigator.overlayStack.single())
        }
    }

    @Test
    fun `system back pops overlay then History returns Home`() {
        lateinit var navigator: AppShellNavigator
        composeRule.setContent {
            navigator = rememberAppShellNavigator(AppShellMode.Guided2)
            AppShellBackHandlers(navigator)
            Text(navigator.selectedRoot.name + navigator.overlayStack.joinToString())
        }
        composeRule.runOnIdle {
            navigator.select(AppShellRoot.History)
            navigator.open(AppShellOverlay.Perfil)
        }

        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.runOnIdle { assertEquals(emptyList<AppShellOverlay>(), navigator.overlayStack) }
        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.runOnIdle { assertEquals(AppShellRoot.Home, navigator.selectedRoot) }
    }

    @Test
    fun `bottom bar exposes mode roots and respects feature flags`() {
        composeRule.setContent {
            CompositionLocalProvider(LocalLkTokens provides lightTokens()) {
                AppShellBottomBar(
                    c = lightTokens(),
                    mode = AppShellMode.Guided2,
                    selectedTab = 0,
                    featureFlags = AppShellFeatureFlagsState(historyEnabled = false),
                    onRootSelected = {},
                )
            }
        }

        composeRule.onNodeWithText("Início").assertExists()
        composeRule.onNodeWithText("Velocidade").assertExists()
        composeRule.onNodeWithText("Histórico").assertExists()
        composeRule.onNodeWithText("Histórico").assertIsNotEnabled()
        composeRule.onNodeWithText("Ferramentas").assertExists()
        composeRule.onNodeWithText("Sinal").assertDoesNotExist()
    }
}
