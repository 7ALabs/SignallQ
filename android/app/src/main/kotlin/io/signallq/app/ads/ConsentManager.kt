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
                    val podeRequisitar = consentInformation.canRequestAds()
                    // GH#1330 -- log de diagnostico: sem isso, "nenhum anuncio aparece" nao dava
                    // pra distinguir entre UMP OK/consentimento negado e falha de rede/config,
                    // so via debugger anexado. Filtrar logcat por "ConsentManager".
                    Timber.i(
                        "UMP: consentInfoUpdate OK -- status=${consentInformation.consentStatus}, " +
                            "podeRequisitarAnuncio=$podeRequisitar",
                    )
                    onResultado(podeRequisitar)
                }
            },
            { requestError ->
                Timber.w(
                    "UMP: falha ao atualizar info de consentimento: " +
                        "codigo=${requestError.errorCode}, mensagem=${requestError.message}",
                )
                // Falha na atualizacao nao apaga consentimento ja obtido em sessao anterior.
                val podeRequisitar = consentInformation.canRequestAds()
                Timber.w("UMP: apos falha, status=${consentInformation.consentStatus}, podeRequisitarAnuncio=$podeRequisitar")
                onResultado(podeRequisitar)
            },
        )
    }

    fun podeRequisitarAnuncioAgora(activity: Activity): Boolean =
        UserMessagingPlatform.getConsentInformation(activity).canRequestAds()

    /**
     * A UMP exige que exista uma entrada **permanente** para o usuário revisar a escolha de
     * consentimento depois de já tê-la feito (GH#1703). Até esta issue o app só sabia
     * *coletar* o consentimento, via [atualizarEMostrarSeNecessario] — não havia caminho de
     * volta, o que é exigência do Google em regiões sob GDPR, não preferência nossa.
     *
     * `REQUIRED` é o único valor que obriga a mostrar a entrada. `NOT_REQUIRED` (fora da
     * região) e `UNKNOWN` (antes do primeiro `requestConsentInfoUpdate` da sessão) mantêm a
     * entrada oculta — mostrar um item que abre um formulário vazio seria pior que não mostrar.
     */
    fun precisaOferecerOpcoesPrivacidade(activity: Activity): Boolean =
        precisaOferecer(UserMessagingPlatform.getConsentInformation(activity).privacyOptionsRequirementStatus)

    /**
     * A regra em si, separada do acesso ao SDK — achado da revisão de Caio na PR #1709.
     *
     * Enquanto isto vivia embutido na chamada acima, o predicado **não era testado por nenhum
     * meio**: em máquina não, porque exige `Activity` e o SDK da UMP; em aparelho tampouco,
     * porque o caminho `REQUIRED` só ocorre sob GDPR e não temos *debug geography* configurado.
     * Duas mutações sobreviviam à suíte inteira, e são exatamente as duas formas de errar a
     * obrigação regulatória: fixar `true` (entrada aparece no Brasil e abre formulário vazio) e
     * comparar com `NOT_REQUIRED` (entrada some sob GDPR — o descumprimento que a issue corrige,
     * de volta intacto).
     *
     * Separado, vira tabela de três casos sem `Activity`, sem SDK e sem VPN. Não é abstração
     * nova: é uma função pura, zero interface.
     */
    internal fun precisaOferecer(status: ConsentInformation.PrivacyOptionsRequirementStatus): Boolean =
        status == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Abre o formulário de opções de privacidade da própria UMP — não construímos UI de
     * consentimento, e não devemos: o formulário é gerado pelo Google a partir da configuração
     * do AdMob, e reimplementá-lo desalinharia do que foi efetivamente consentido.
     *
     * [onFechado] recebe `null` em sucesso ou a mensagem de erro. Nunca lança para o chamador,
     * mesmo padrão de [atualizarEMostrarSeNecessario].
     */
    fun mostrarOpcoesPrivacidade(
        activity: Activity,
        onFechado: (erro: String?) -> Unit = {},
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Timber.w("UMP: erro ao exibir opcoes de privacidade: ${formError.message}")
            } else {
                Timber.i(
                    "UMP: opcoes de privacidade fechadas -- status=" +
                        "${UserMessagingPlatform.getConsentInformation(activity).consentStatus}",
                )
            }
            onFechado(formError?.message)
        }
    }

    /** Estado bruto da UMP, exposto so para telemetria/debug -- nunca usado para decisao de UI. */
    fun statusConsentimento(activity: Activity): Int =
        UserMessagingPlatform.getConsentInformation(activity).consentStatus

    const val STATUS_DESCONHECIDO = ConsentInformation.ConsentStatus.UNKNOWN
}
