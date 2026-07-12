package io.signallq.app.ads

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import timber.log.Timber

/**
 * Gate de consentimento (UMP -- User Messaging Platform) exigido pelo proprio Google
 * antes de qualquer [com.google.android.gms.ads.AdRequest], mesmo anuncio so
 * contextual/nao-personalizado (issue #555, passo 1 do plano).
 *
 * Nao decide LGPD do restante do app (isso e o consentimentoLgpdFlow existente em
 * PreferenciasAppRepository) -- e uma camada adicional, especifica de ads, exigida
 * pela politica do AdMob/UMP independente da nossa propria tela de privacidade.
 */
object ConsentManager {
    /**
     * Atualiza info de consentimento e mostra o formulario da UMP se necessario.
     * [onResultado] e sempre chamado exatamente uma vez, com `true` quando o app pode
     * pedir anuncio (consentimento obtido ou nao exigido nesta regiao) e `false` caso
     * contrario -- nunca lanca excecao para o chamador, so loga e reporta `false`.
     */
    fun atualizarEMostrarSeNecessario(
        activity: Activity,
        onResultado: (podeRequisitarAnuncio: Boolean) -> Unit,
    ) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Timber.w("UMP: erro ao exibir formulario de consentimento: ${formError.message}")
                    }
                    onResultado(consentInformation.canRequestAds())
                }
            },
            { requestError ->
                Timber.w("UMP: falha ao atualizar info de consentimento: ${requestError.message}")
                // Falha na atualizacao nao apaga consentimento ja obtido em sessao anterior.
                onResultado(consentInformation.canRequestAds())
            },
        )
    }

    fun podeRequisitarAnuncioAgora(activity: Activity): Boolean =
        UserMessagingPlatform.getConsentInformation(activity).canRequestAds()

    /** Estado bruto da UMP, exposto so para telemetria/debug -- nunca usado para decisao de UI. */
    fun statusConsentimento(activity: Activity): Int =
        UserMessagingPlatform.getConsentInformation(activity).consentStatus

    const val STATUS_DESCONHECIDO = ConsentInformation.ConsentStatus.UNKNOWN
}
