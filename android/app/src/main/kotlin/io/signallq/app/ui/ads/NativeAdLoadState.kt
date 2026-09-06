package io.signallq.app.ui.ads

import androidx.compose.runtime.Composable
import com.google.android.gms.ads.nativead.NativeAd
import io.signallq.app.ads.AdSlot

sealed interface NativeAdIneligibleReason {
    data object FlagDisabled : NativeAdIneligibleReason

    data object ConsentUnavailable : NativeAdIneligibleReason
}

sealed interface NativeAdLoadState {
    data class Ineligible(
        val reason: NativeAdIneligibleReason,
    ) : NativeAdLoadState

    data object Loading : NativeAdLoadState

    data class Fill(
        val ad: NativeAd,
    ) : NativeAdLoadState

    data object NoFill : NativeAdLoadState

    data class RecoverableError(
        val code: Int,
    ) : NativeAdLoadState

    data object Offline : NativeAdLoadState
}

data class NativeAdEligibility(
    val slot: AdSlot? = null,
    val flagEnabled: Boolean,
    val canRequestAds: Boolean,
    val online: Boolean,
) {
    fun initialState(): NativeAdLoadState =
        when {
            !flagEnabled -> NativeAdLoadState.Ineligible(NativeAdIneligibleReason.FlagDisabled)
            !canRequestAds -> NativeAdLoadState.Ineligible(NativeAdIneligibleReason.ConsentUnavailable)
            !online -> NativeAdLoadState.Offline
            else -> NativeAdLoadState.Loading
        }

    val canLoad: Boolean
        get() = flagEnabled && canRequestAds && online
}

internal const val ADMOB_NO_FILL_ERROR_CODE = 3

/** Renderiza exclusivamente fill; todos os demais estados ocupam zero espaço. */
@Composable
fun NativeAdStateContent(
    state: NativeAdLoadState,
    content: @Composable (NativeAd) -> Unit,
) {
    (state as? NativeAdLoadState.Fill)?.let { content(it.ad) }
}
