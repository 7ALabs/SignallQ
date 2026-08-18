package io.signallq.app.ads

import android.app.Activity
import android.content.Intent
import android.provider.Settings
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
     * `REQUIRED` é o único valor em que a UMP tem formulário para mostrar. `NOT_REQUIRED` (fora
     * da região) e `UNKNOWN` (antes do primeiro `requestConsentInfoUpdate` da sessão) não têm.
     *
     * Isto **não decide mais se a entrada aparece** — decidia até a GH#1717, e o efeito era que
     * quem está fora do GDPR recebia anúncio personalizado sem controle nenhum dentro do app.
     * Hoje decide para onde ela leva; ver [destinoDasOpcoes]. Continua valendo que abrir um
     * formulário vazio é pior que não abrir nada — o que mudou é existir outro destino.
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
     * Para onde a entrada "Preferências de anúncios" leva — GH#1717.
     *
     * Até esta issue a entrada só existia sob GDPR, então quem está no Brasil recebia anúncio
     * personalizado sem nenhum controle dentro do app. Agora ela existe para todos, e o destino
     * depende de haver ou não formulário da UMP:
     *
     * - [FORMULARIO_UMP] — a UMP tem o que mostrar (`REQUIRED`);
     * - [CONFIGURACOES_DO_ANDROID] — não tem. Abrir o formulário vazio seria pior que não abrir
     *   nada, mas existe controle real fora do app: a tela de anúncios do Google Play services, onde dá para limitar a
     *   personalização e apagar o identificador de publicidade.
     */
    enum class DestinoOpcoesAnuncios { FORMULARIO_UMP, CONFIGURACOES_DO_ANDROID }

    internal fun destinoDasOpcoes(status: ConsentInformation.PrivacyOptionsRequirementStatus): DestinoOpcoesAnuncios =
        if (precisaOferecer(status)) {
            DestinoOpcoesAnuncios.FORMULARIO_UMP
        } else {
            DestinoOpcoesAnuncios.CONFIGURACOES_DO_ANDROID
        }

    /**
     * Ações que abrem a tela de anúncios do sistema, da mais específica para a mais genérica.
     *
     * A primeira é a tela de privacidade de anúncios do Play services. A segunda é a tela de
     * privacidade do próprio Android — constante de plataforma, e não string de pacote de
     * terceiro. A versão anterior desta lista usava `com.google.android.gms.settings.ADS`, que
     * **não consegui confirmar** como ação registrada; um item que nunca resolve é um `forEach`
     * que finge cobertura (bloqueio B2 de Caio na PR #1717).
     */
    private val ACOES_ANUNCIOS_DO_SISTEMA =
        listOf(
            "com.google.android.gms.settings.ADS_PRIVACY",
            Settings.ACTION_PRIVACY_SETTINGS,
        )

    /**
     * Abre a tela de anúncios do sistema. `true` se alguma abriu.
     *
     * **Sem `resolveActivity`**, e isso não é descuido — bloqueio B2. Com `targetSdk = 36`,
     * `resolveActivity` passa pelo filtro de visibilidade de pacotes da API 30+, e o manifesto
     * mesclado deste app não declara `<queries>` que torne `com.google.android.gms` visível para
     * essas ações. O filtro devolveria `null` mesmo com a tela existindo, e **todo brasileiro**
     * que tocasse em "Preferências de anúncios" receberia "este aparelho não tem a tela" — com a
     * política publicada afirmando o contrário.
     *
     * `startActivity` não depende de visibilidade de pacote; quem não existe lança
     * `ActivityNotFoundException`, que é o sinal honesto e é o que se trata aqui.
     */
    fun abrirConfiguracoesDeAnuncios(activity: Activity): Boolean {
        ACOES_ANUNCIOS_DO_SISTEMA.forEach { acao ->
            val intent = Intent(acao).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { activity.startActivity(intent) }
                .onSuccess { return true }
                .onFailure { Timber.w("UMP: nao abriu $acao: ${it.message}") }
        }
        Timber.w("UMP: nenhuma tela de anuncios do sistema disponivel neste aparelho")
        return false
    }

    /**
     * Executa o roteamento da entrada "Preferências de anúncios" — GH#1717, bloqueio B3.
     *
     * Existe como função porque, enquanto o `if` vivia dentro de `AppShellPrivacidadeOverlay`,
     * trocá-lo por `if (true)` passava na suíte inteira do `:app`: todo mundo fora do GDPR cairia
     * no formulário vazio da UMP — a regressão que esta issue existe para impedir — e nada
     * quebrava. `destinoDasOpcoes` estava testada, mas não tinha chamador de produção, então o
     * mutante que a citava como prova matava só o teste da própria função morta.
     *
     * Devolve `false` quando **nada** pôde ser aberto, para o chamador avisar em vez de deixar o
     * toque mudo — mesmo princípio da ressalva R2 da PR #1709.
     */
    internal fun executarOpcoesDeAnuncios(
        destino: DestinoOpcoesAnuncios,
        abrirFormulario: () -> Unit,
        abrirSistema: () -> Boolean,
    ): Boolean =
        when (destino) {
            DestinoOpcoesAnuncios.FORMULARIO_UMP -> {
                abrirFormulario()
                true
            }
            DestinoOpcoesAnuncios.CONFIGURACOES_DO_ANDROID -> abrirSistema()
        }

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
