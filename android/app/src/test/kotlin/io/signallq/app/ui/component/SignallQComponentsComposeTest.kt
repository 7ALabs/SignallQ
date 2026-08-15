package io.signallq.app.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.SignallQTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
class SignallQComponentsComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `expandable exposes one action and coherent expanded state`() {
        composeRule.setThemedContent {
            SignallQExpandableDetails("Detalhes técnicos") { androidx.compose.material3.Text("28 ms") }
        }

        composeRule.onAllNodesWithText("Detalhes técnicos", useUnmergedTree = true).assertCountEquals(1)
        val details = composeRule.onNodeWithText("Detalhes técnicos")
        details.assertHasClickAction()
        assertEquals("Recolhido", details.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertEquals(ToggleableState.Off, details.fetchSemanticsNode().config[SemanticsProperties.ToggleableState])
        details.performClick()
        assertEquals("Expandido", details.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        assertEquals(ToggleableState.On, details.fetchSemanticsNode().config[SemanticsProperties.ToggleableState])
        composeRule.onNodeWithText("28 ms").assertIsDisplayed()
    }

    @Test
    fun `loading button keeps bounds and exposes one accessible loading state`() {
        composeRule.setThemedContent {
            Column {
                SignallQButton("Analisar conexão", {}, Modifier.testTag("normal"))
                SignallQButton("Analisar conexão", {}, Modifier.testTag("loading"), loading = true)
            }
        }

        val normal = composeRule.onNodeWithTag("normal").fetchSemanticsNode().boundsInRoot
        val loadingNode = composeRule.onNodeWithTag("loading")
        val loading = loadingNode.fetchSemanticsNode().boundsInRoot
        assertEquals(normal.width, loading.width, 0.5f)
        assertEquals(normal.height, loading.height, 0.5f)
        loadingNode.assertIsNotEnabled()
        assertEquals("Carregando", loadingNode.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
    }

    @Test
    fun `interactive controls keep 48 dp target and chip semantics`() {
        composeRule.setThemedContent {
            Column {
                SignallQButton("Continuar", {}, Modifier.testTag("button"))
                SignallQChoiceChip("Wi-Fi", selected = true, onClick = {}, modifier = Modifier.testTag("chip"))
            }
        }

        composeRule.onNodeWithTag("button").assertHeightIsAtLeast(48.dp).assertHasClickAction()
        composeRule.onNodeWithTag("chip").assertHeightIsAtLeast(48.dp).assertHasClickAction()
    }

    @Test
    fun `banner keeps CTA as a separate action and progress exposes value`() {
        composeRule.setThemedContent {
            Column {
                SignallQBanner("Sem conexão", actionLabel = "Tentar novamente", onAction = {})
                SignallQProgress("Progresso da análise", progress = 0.5f)
            }
        }

        composeRule.onNodeWithText("Sem conexão").assertIsDisplayed()
        composeRule.onNodeWithText("Tentar novamente").assertHasClickAction()
        composeRule
            .onNode(
                hasProgressBarRangeInfo(
                    androidx.compose.ui.semantics
                        .ProgressBarRangeInfo(0.5f, 0f..1f),
                ),
            ).assertIsDisplayed()
    }

    @Test
    fun `full screen state remains reachable with font scale 200 percent`() {
        composeRule.setContent {
            val density = LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                SignallQTheme {
                    SignallQStatefulScreen<Unit>(
                        state =
                            SignallQScreenState.RecoverableError(
                                "Uma conclusão longa para explicar o problema sem cortar o conteúdo",
                                "Uma explicação igualmente longa que continua legível quando o texto está ampliado.",
                            ),
                        modifier = Modifier.height(320.dp),
                        actionLabel = "Tentar novamente",
                        onAction = {},
                        content = {},
                    )
                }
            }
        }

        composeRule
            .onNodeWithText("Tentar novamente")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.setThemedContent(
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) = setContent { SignallQTheme(content = content) }
}
