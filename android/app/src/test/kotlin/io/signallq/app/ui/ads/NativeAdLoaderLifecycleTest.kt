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
import org.junit.Assert.assertSame
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
    private val signal = NativeAdContentSignal.forSlot(AdSlot.VELOCIDADE)

    @Test
    fun `flag consent and connectivity gate requests before loader`() {
        val requester = FakeNativeAdRequester()
        var currentEligibility by
            mutableStateOf(
                NativeAdEligibility(AdSlot.VELOCIDADE, flagEnabled = false, canRequestAds = true, online = true),
            )
        var observedState: NativeAdLoadState? = null

        composeRule.setContent {
            observedState = rememberNativeAdState("test-unit", signal, currentEligibility, requester).value
        }
        composeRule.runOnIdle {
            assertEquals(NativeAdLoadState.Ineligible(NativeAdIneligibleReason.FlagDisabled), observedState)
            assertEquals(0, requester.loadCount)
            currentEligibility =
                NativeAdEligibility(AdSlot.VELOCIDADE, flagEnabled = true, canRequestAds = false, online = true)
        }
        composeRule.runOnIdle {
            assertEquals(NativeAdLoadState.Ineligible(NativeAdIneligibleReason.ConsentUnavailable), observedState)
            assertEquals(0, requester.loadCount)
            currentEligibility =
                NativeAdEligibility(AdSlot.VELOCIDADE, flagEnabled = true, canRequestAds = true, online = false)
        }
        composeRule.runOnIdle {
            assertEquals(NativeAdLoadState.Offline, observedState)
            assertEquals(0, requester.loadCount)
        }
    }

    @Test
    fun `failure code maps no fill separately from recoverable error`() {
        val requester = FakeNativeAdRequester()
        var unitId by mutableStateOf("no-fill")
        var observedState: NativeAdLoadState? = null

        composeRule.setContent {
            observedState = rememberNativeAdState(unitId, signal, eligibility, requester).value
        }
        composeRule.runOnIdle { requester.session(0).fail(ADMOB_NO_FILL_ERROR_CODE) }
        composeRule.runOnIdle {
            assertEquals(NativeAdLoadState.NoFill, observedState)
            unitId = "recoverable-error"
        }
        composeRule.runOnIdle { requester.session(1).fail(42) }
        composeRule.runOnIdle {
            assertEquals(NativeAdLoadState.RecoverableError(42), observedState)
            assertEquals(2, requester.loadCount)
        }
    }

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
        composeRule.runOnIdle { requester.session(0).fill(ad) }
        composeRule.runOnIdle { visible = false }
        composeRule.waitForIdle()

        assertEquals(1, requester.cancelCount)
        verify(exactly = 1) { ad.destroy() }
    }

    @Test
    fun `new signal instance with same slot does not restart request`() {
        val requester = FakeNativeAdRequester()
        var unrelatedState by mutableIntStateOf(0)

        composeRule.setContent {
            unrelatedState
            rememberNativeAdState(
                adUnitId = "test-unit",
                contentSignal = NativeAdContentSignal.forSlot(AdSlot.VELOCIDADE),
                eligibility = eligibility,
                requester = requester,
            )
        }

        composeRule.runOnIdle { unrelatedState++ }
        composeRule.runOnIdle { unrelatedState++ }

        assertEquals("recomposição não pode cancelar a carga ativa", 1, requester.loadCount)
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
        composeRule.runOnIdle { requester.session(0).fill(lateAd) }

        verify(exactly = 1) { lateAd.destroy() }
    }

    @Test
    fun `navigation key starts new session and releases previous fill`() {
        val requester = FakeNativeAdRequester()
        var unitId by mutableStateOf("slot-a")
        var observedState: NativeAdLoadState? = null

        composeRule.setContent {
            observedState = rememberNativeAdState(unitId, signal, eligibility, requester).value
        }
        val firstAd = mockk<NativeAd>(relaxed = true)
        composeRule.runOnIdle { requester.session(0).fill(firstAd) }
        composeRule.runOnIdle { unitId = "slot-b" }
        composeRule.waitForIdle()

        assertEquals(2, requester.loadCount)
        assertEquals(1, requester.cancelCount)
        verify(exactly = 1) { firstAd.destroy() }

        val staleAd = mockk<NativeAd>(relaxed = true)
        composeRule.runOnIdle {
            requester.session(0).fail(99)
            requester.session(0).fill(staleAd)
        }
        composeRule.runOnIdle { assertEquals(NativeAdLoadState.Loading, observedState) }
        verify(exactly = 1) { staleAd.destroy() }

        val currentAd = mockk<NativeAd>(relaxed = true)
        composeRule.runOnIdle { requester.session(1).fill(currentAd) }
        composeRule.runOnIdle {
            val fill = observedState as NativeAdLoadState.Fill
            assertSame(currentAd, fill.ad)
        }
        verify(exactly = 0) { currentAd.destroy() }
    }
}

private class FakeNativeAdRequester : NativeAdRequester {
    private val sessions = mutableListOf<Session>()

    val loadCount: Int
        get() = sessions.size

    val cancelCount: Int
        get() = sessions.count { it.cancelled }

    override fun load(
        adUnitId: String,
        contentSignal: NativeAdContentSignal,
        onFill: (NativeAd) -> Unit,
        onFailure: (Int) -> Unit,
    ): NativeAdRequestHandle {
        val session = Session(onFill, onFailure)
        sessions += session
        return NativeAdRequestHandle { session.cancelled = true }
    }

    fun session(index: Int): Session = sessions[index]

    class Session(
        private val onFill: (NativeAd) -> Unit,
        private val onFailure: (Int) -> Unit,
    ) {
        var cancelled: Boolean = false

        fun fill(ad: NativeAd) = onFill(ad)

        fun fail(code: Int) = onFailure(code)
    }
}
