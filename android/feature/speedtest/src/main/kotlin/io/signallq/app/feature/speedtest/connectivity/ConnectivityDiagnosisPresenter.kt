package io.signallq.app.feature.speedtest.connectivity

import io.signallq.app.core.network.contracts.connectivity.ConnectivityDiagnosis
import io.signallq.app.core.network.contracts.connectivity.ConnectivityStatus
import io.signallq.app.core.network.contracts.topologia.NivelConfianca

/** Ação sugerida ao usuário a partir do diagnóstico de conectividade (GH#1512) — a UI
 *  decide o texto/ícone do botão; aqui só a intenção, relacionada ao estado concluído. */
enum class ConnectivityAction {
    ABRIR_PORTAL_LOGIN,
    RECONECTAR_WIFI,
    TESTAR_OUTRO_APARELHO,
    VERIFICAR_LUZES_EQUIPAMENTO,
    REINICIAR_EQUIPAMENTO,
    TESTAR_DNS_ALTERNATIVO,
    CONTATAR_OPERADORA,
}

/** Texto mínimo e honesto + ações ordenadas para apresentar um [ConnectivityDiagnosis]
 *  (GH#1512). Não é a reescrita da experiência de resultado (#1502) — apenas o suficiente
 *  para o usuário entender e agir quando o Wi-Fi está sem internet. */
data class ConnectivityDiagnosisMensagem(
    val titulo: String,
    val mensagem: String,
    val acoes: List<ConnectivityAction>,
)

/**
 * Traduz [ConnectivityDiagnosis] em texto/ações simples — usa linguagem de probabilidade
 * e evidência (GH#1512): nunca afirma que a operadora está fora do ar apenas porque o
 * gateway responde e a internet externa não.
 */
object ConnectivityDiagnosisPresenter {

    fun apresentar(diagnostico: ConnectivityDiagnosis): ConnectivityDiagnosisMensagem = when (diagnostico.status) {
        ConnectivityStatus.INTERNET_AVAILABLE -> ConnectivityDiagnosisMensagem(
            titulo = "Internet disponível",
            mensagem = "A conexão Wi-Fi está funcionando normalmente.",
            acoes = emptyList(),
        )

        ConnectivityStatus.WIFI_WITHOUT_INTERNET -> ConnectivityDiagnosisMensagem(
            titulo = "Wi-Fi sem internet",
            mensagem = "Você está conectado ao Wi-Fi, mas essa rede não está conseguindo acessar a internet.",
            acoes = listOf(
                ConnectivityAction.RECONECTAR_WIFI,
                ConnectivityAction.VERIFICAR_LUZES_EQUIPAMENTO,
                ConnectivityAction.TESTAR_OUTRO_APARELHO,
                ConnectivityAction.REINICIAR_EQUIPAMENTO,
                ConnectivityAction.CONTATAR_OPERADORA,
            ),
        )

        ConnectivityStatus.GATEWAY_UNREACHABLE -> ConnectivityDiagnosisMensagem(
            titulo = "Não alcançamos o roteador",
            mensagem = when (diagnostico.confidence) {
                NivelConfianca.ALTA -> "Confirmamos: seu celular está conectado ao Wi-Fi, mas não " +
                    "consegue se comunicar com o roteador."
                NivelConfianca.MEDIA -> "Há sinais de que seu celular está conectado ao Wi-Fi, mas " +
                    "não consegue se comunicar com o roteador."
                NivelConfianca.BAIXA -> "O problema parece estar entre o celular e o roteador, mas " +
                    "algumas verificações não puderam ser concluídas."
            },
            acoes = listOf(
                ConnectivityAction.RECONECTAR_WIFI,
                ConnectivityAction.TESTAR_OUTRO_APARELHO,
                ConnectivityAction.VERIFICAR_LUZES_EQUIPAMENTO,
            ),
        )

        ConnectivityStatus.DNS_FAILURE -> ConnectivityDiagnosisMensagem(
            titulo = "Falha no DNS",
            mensagem = when (diagnostico.confidence) {
                NivelConfianca.ALTA -> "O teste mostrou que a conexão está ativa, mas o serviço que " +
                    "localiza os sites não está respondendo."
                NivelConfianca.MEDIA, NivelConfianca.BAIXA -> "A conexão parece estar ativa, mas há " +
                    "sinais de que o serviço que localiza os sites não está respondendo."
            },
            acoes = listOf(
                ConnectivityAction.TESTAR_DNS_ALTERNATIVO,
                ConnectivityAction.REINICIAR_EQUIPAMENTO,
                ConnectivityAction.CONTATAR_OPERADORA,
            ),
        )

        ConnectivityStatus.EXTERNAL_ROUTE_FAILURE -> ConnectivityDiagnosisMensagem(
            titulo = "Sem rota para a internet",
            mensagem = when (diagnostico.confidence) {
                NivelConfianca.ALTA -> "Confirmamos: o sinal chega até o roteador e o DNS responde, " +
                    "mas esta rede não está conseguindo alcançar a internet além disso."
                NivelConfianca.MEDIA, NivelConfianca.BAIXA -> "A causa mais provável é que o sinal " +
                    "chega até o roteador e o DNS responde, mas esta rede não está conseguindo " +
                    "alcançar a internet além disso."
            },
            acoes = listOf(
                ConnectivityAction.REINICIAR_EQUIPAMENTO,
                ConnectivityAction.VERIFICAR_LUZES_EQUIPAMENTO,
                ConnectivityAction.CONTATAR_OPERADORA,
            ),
        )

        ConnectivityStatus.CAPTIVE_PORTAL -> ConnectivityDiagnosisMensagem(
            titulo = "Login necessário",
            mensagem = "Esta rede precisa de login ou confirmação antes de liberar a internet.",
            acoes = listOf(ConnectivityAction.ABRIR_PORTAL_LOGIN),
        )

        ConnectivityStatus.PARTIAL_CONNECTIVITY -> ConnectivityDiagnosisMensagem(
            titulo = "Conexão instável",
            mensagem = "Há sinais de instabilidade nesta rede: parte das conexões funciona e parte " +
                "não — diferente de uma internet totalmente indisponível.",
            acoes = listOf(
                ConnectivityAction.TESTAR_OUTRO_APARELHO,
                ConnectivityAction.REINICIAR_EQUIPAMENTO,
            ),
        )

        ConnectivityStatus.WIFI_DISCONNECTED -> ConnectivityDiagnosisMensagem(
            titulo = "Wi-Fi desconectado",
            mensagem = "Seu aparelho não está conectado a nenhuma rede Wi-Fi no momento.",
            acoes = listOf(ConnectivityAction.RECONECTAR_WIFI),
        )

        ConnectivityStatus.INCONCLUSIVE -> apresentarInconclusivo()
    }

    /**
     * Fallback honesto quando o diagnóstico em si falha inesperadamente (ex.: erro de I/O
     * ao persistir, exceção não prevista numa sondagem) — GH#1512, achado de revisão:
     * nunca deixar uma exceção do novo caminho de diagnóstico propagar como crash do app;
     * degrada para a mesma conclusão honesta de [ConnectivityStatus.INCONCLUSIVE].
     */
    fun apresentarInconclusivo(): ConnectivityDiagnosisMensagem = ConnectivityDiagnosisMensagem(
        titulo = "Diagnóstico inconclusivo",
        mensagem = "Confirmamos que esta rede está sem acesso normal à internet, mas não foi " +
            "possível identificar a causa com segurança.",
        acoes = listOf(
            ConnectivityAction.TESTAR_OUTRO_APARELHO,
            ConnectivityAction.REINICIAR_EQUIPAMENTO,
        ),
    )
}
