package io.signallq.app.ui.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import io.signallq.app.ads.NativeAdContentSignal
import kotlinx.coroutines.awaitCancellation
import timber.log.Timber

/**
 * Carrega um [NativeAd] real do AdMob para um slot -- issue #555.
 *
 * `eligible = false` (Remote Config desligado ou consentimento UMP pendente/negado)
 * nunca chega a chamar [AdLoader.loadAd] -- nem a fazer request de rede a toa. O
 * anuncio anterior e sempre destruido (`NativeAd.destroy()`) antes de um novo e ao
 * sair de composicao -- exigencia do SDK para nao vazar memoria/recursos de midia.
 */
@Composable
fun rememberNativeAd(
    adUnitId: String,
    contentSignal: NativeAdContentSignal,
    eligible: Boolean,
): State<NativeAd?> {
    val context = LocalContext.current
    return produceState<NativeAd?>(initialValue = null, adUnitId, contentSignal, eligible) {
        if (!eligible) {
            value = null
            return@produceState
        }

        var adCarregado: NativeAd? = null
        val loader =
            AdLoader
                .Builder(context, adUnitId)
                .forNativeAd { nativeAd ->
                    adCarregado?.destroy()
                    adCarregado = nativeAd
                    value = nativeAd
                }.withAdListener(
                    object : AdListener() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            Timber.w("NativeAd falhou ao carregar: ${adError.message} (${adError.code})")
                            value = null
                        }
                    },
                ).build()

        loader.loadAd(buildAdRequest(contentSignal))

        try {
            awaitCancellation()
        } finally {
            adCarregado?.destroy()
        }
    }
}

private fun buildAdRequest(signal: NativeAdContentSignal): AdRequest {
    val builder = AdRequest.Builder().setContentUrl(signal.contentUrl)
    if (signal.neighboringContentUrls.isNotEmpty()) {
        builder.setNeighboringContentUrls(signal.neighboringContentUrls)
    }
    return builder.build()
}
