package io.signallq.app.ui.ads

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import com.google.android.gms.ads.nativead.NativeAd
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NativeAdStateContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `states without fill occupy no semantic or layout node`() {
        var currentState by mutableStateOf<NativeAdLoadState>(NativeAdLoadState.Loading)
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                NativeAdStateContent(currentState) { Text("publicidade") }
            }
        }
        listOf(
            NativeAdLoadState.Loading,
            NativeAdLoadState.NoFill,
            NativeAdLoadState.Offline,
            NativeAdLoadState.RecoverableError(1),
            NativeAdLoadState.Ineligible(NativeAdIneligibleReason.ConsentUnavailable),
        ).forEach { state ->
            composeRule.runOnIdle { currentState = state }
            composeRule.onNodeWithText("publicidade").assertDoesNotExist()
        }
    }

    @Test
    fun `fill delegates rendering to native ad component`() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                NativeAdStateContent(NativeAdLoadState.Fill(mockk<NativeAd>())) {
                    Text("publicidade")
                }
            }
        }

        composeRule.onNodeWithText("publicidade").assertExists()
    }
}
