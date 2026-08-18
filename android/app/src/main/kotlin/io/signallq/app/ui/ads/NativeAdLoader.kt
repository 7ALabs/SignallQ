package io.signallq.app.ui.ads

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
    val state =
        rememberNativeAdState(
            adUnitId = adUnitId,
            contentSignal = contentSignal,
            eligibility =
                NativeAdEligibility(
                    flagEnabled = eligible,
                    canRequestAds = eligible,
                    online = true,
                ),
        )
    return remember(state) { derivedStateOf { (state.value as? NativeAdLoadState.Fill)?.ad } }
}

/**
 * Contrato tipado para as migrações 2.0. A chave estável da coroutine impede request duplicada
 * por recomposição; troca de slot/configuração cancela a sessão e destrói exatamente o fill
 * anterior. Estados sem fill não devem ser renderizados pelos consumidores.
 */
@Composable
fun rememberNativeAdState(
    adUnitId: String,
    contentSignal: NativeAdContentSignal,
    eligibility: NativeAdEligibility,
): State<NativeAdLoadState> {
    val context = LocalContext.current
    val requester = remember(context) { GoogleNativeAdRequester(context) }
    return rememberNativeAdState(adUnitId, contentSignal, eligibility, requester)
}

@Composable
internal fun rememberNativeAdState(
    adUnitId: String,
    contentSignal: NativeAdContentSignal,
    eligibility: NativeAdEligibility,
    requester: NativeAdRequester,
): State<NativeAdLoadState> {
    return produceState<NativeAdLoadState>(
        initialValue = eligibility.initialState(),
        adUnitId,
        contentSignal,
        eligibility,
    ) {
        value = eligibility.initialState()
        if (!eligibility.canLoad) {
            return@produceState
        }

        var adCarregado: NativeAd? = null
        var sessionActive = true
        val requestHandle =
            requester.load(
                adUnitId = adUnitId,
                contentSignal = contentSignal,
                onFill = { nativeAd ->
                    if (!sessionActive) {
                        nativeAd.destroy()
                        return@load
                    }
                    adCarregado?.destroy()
                    adCarregado = nativeAd
                    value = NativeAdLoadState.Fill(nativeAd)
                },
                onFailure = { errorCode ->
                    if (!sessionActive) return@load
                    value =
                        if (errorCode == ADMOB_NO_FILL_ERROR_CODE) {
                            NativeAdLoadState.NoFill
                        } else {
                            NativeAdLoadState.RecoverableError(errorCode)
                        }
                },
            )

        try {
            awaitCancellation()
        } finally {
            sessionActive = false
            requestHandle.cancel()
            adCarregado?.destroy()
        }
    }
}

internal fun interface NativeAdRequestHandle {
    fun cancel()
}

internal interface NativeAdRequester {
    fun load(
        adUnitId: String,
        contentSignal: NativeAdContentSignal,
        onFill: (NativeAd) -> Unit,
        onFailure: (Int) -> Unit,
    ): NativeAdRequestHandle
}

private class GoogleNativeAdRequester(
    private val context: Context,
) : NativeAdRequester {
    override fun load(
        adUnitId: String,
        contentSignal: NativeAdContentSignal,
        onFill: (NativeAd) -> Unit,
        onFailure: (Int) -> Unit,
    ): NativeAdRequestHandle {
        val loader =
            AdLoader
                .Builder(context, adUnitId)
                .forNativeAd(onFill)
                .withAdListener(
                    object : AdListener() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            Timber.w("NativeAd falhou ao carregar: ${adError.message} (${adError.code})")
                            onFailure(adError.code)
                        }
                    },
                ).build()
        loader.loadAd(buildAdRequest(contentSignal))
        // O SDK nao oferece cancelamento da request em voo; o handle invalida a sessao Compose,
        // enquanto qualquer fill pertencente a ela e destruido no descarte da composicao.
        return NativeAdRequestHandle {}
    }
}

private fun buildAdRequest(signal: NativeAdContentSignal): AdRequest {
    // GH#1717 — `setNeighboringContentUrls` saiu junto com os marcadores de diagnóstico. O único
    // sinal enviado é o tópico da tela; ver o KDoc de `NativeAdContentSignal`.
    val builder = AdRequest.Builder().setContentUrl(signal.contentUrl)
    return builder.build()
}
