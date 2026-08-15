package io.signallq.app.ui.ads

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.android.gms.ads.nativead.NativeAd
import io.mockk.mockk
import io.mockk.verify
import io.signallq.app.ads.AdSlot
import io.signallq.app.ads.NativeAdContentSignal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NativeAdLoaderLifecycleTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val eligibility =
        NativeAdEligibility(AdSlot.VELOCIDADE, flagEnabled = true, canRequestAds = true, online = true)
    private val signal = NativeAdContentSignal("https://signallq.com/velocidade")

    @Test
    fun `recomposition does not duplicate request and disposal destroys fill once`() {
        val requester = FakeNativeAdRequester()
        var unrelatedState by mutableIntStateOf(0)
        var visible by mutableStateOf(true)

        composeRule.setContent {
            unrelatedState
            if (visible) {
                rememberNativeAdState("test-unit", signal, eligibility, requester)
            }
        }
        composeRule.runOnIdle { unrelatedState++ }
        composeRule.runOnIdle { unrelatedState++ }
        assertEquals(1, requester.loadCount)

        val ad = mockk<NativeAd>(relaxed = true)
        composeRule.runOnIdle { requester.fill(ad) }
        composeRule.runOnIdle { visible = false }
        composeRule.waitForIdle()

        assertEquals(1, requester.cancelCount)
        verify(exactly = 1) { ad.destroy() }
    }

    @Test
    fun `late fill after navigation is destroyed and never adopted`() {
        val requester = FakeNativeAdRequester()
        var visible by mutableStateOf(true)

        composeRule.setContent {
            if (visible) rememberNativeAdState("test-unit", signal, eligibility, requester)
        }
        composeRule.runOnIdle { visible = false }
        composeRule.waitForIdle()

        val lateAd = mockk<NativeAd>(relaxed = true)
        composeRule.runOnIdle { requester.fill(lateAd) }

        verify(exactly = 1) { lateAd.destroy() }
    }

    @Test
    fun `navigation key starts new session and releases previous fill`() {
        val requester = FakeNativeAdRequester()
        var unitId by mutableStateOf("slot-a")

        composeRule.setContent {
            rememberNativeAdState(unitId, signal, eligibility, requester)
        }
        val firstAd = mockk<NativeAd>(relaxed = true)
        composeRule.runOnIdle { requester.fill(firstAd) }
        composeRule.runOnIdle { unitId = "slot-b" }
        composeRule.waitForIdle()

        assertEquals(2, requester.loadCount)
        assertEquals(1, requester.cancelCount)
        verify(exactly = 1) { firstAd.destroy() }
    }
}

private class FakeNativeAdRequester : NativeAdRequester {
    var loadCount = 0
    var cancelCount = 0
    private var onFill: ((NativeAd) -> Unit)? = null

    override fun load(
        adUnitId: String,
        contentSignal: NativeAdContentSignal,
        onFill: (NativeAd) -> Unit,
        onFailure: (Int) -> Unit,
    ): NativeAdRequestHandle {
        loadCount++
        this.onFill = onFill
        return NativeAdRequestHandle { cancelCount++ }
    }

    fun fill(ad: NativeAd) {
        checkNotNull(onFill).invoke(ad)
    }
}
