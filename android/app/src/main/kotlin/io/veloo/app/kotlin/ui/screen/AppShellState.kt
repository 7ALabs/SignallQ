package io.signallq.app.ui.screen

import androidx.compose.runtime.Stable
import io.signallq.app.ads.AdsFlags
import io.signallq.app.core.recommendation.RecommendationDecision
import io.signallq.app.core.recommendation.RecommendationFeedbackType
import io.signallq.app.feature.devices.SnapshotScanDispositivos
import io.signallq.app.feature.diagnostico.SnapshotDiagnostico
import io.signallq.app.feature.diagnostico.ai.AiAcaoRecomendada
import io.signallq.app.feature.diagnostico.ai.DiagChatEntry
import io.signallq.app.feature.diagnostico.pulse.OpcaoResposta
import io.signallq.app.feature.speedtest.ModoSpeedtest
import io.signallq.app.feature.speedtest.ResultadoSpeedtest
import io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest
import io.signallq.app.feature.wifi.RedeVizinha
import io.signallq.app.feature.wifi.SnapshotScanWifi

/**
 * Agrupa parametros do speedtest para reduzir a assinatura do AppShell.
 * @Stable garante que o Compose nao recompoe a arvore inteira quando apenas
 * um campo muda — desde que o conteudo semantico dos campos nao mude.
 */
@Stable
data class AppShellSpeedtestState(
    val snapshotSpeedtest: SnapshotExecucaoSpeedtest,
    val speedtestPendenteModoMovel: ModoSpeedtest? = null,
    val speedtestPermiteHeavyMovel: Boolean = false,
    val speedtestMbConsumidosMes: Long = 0L,
    val onNovoTeste: (ModoSpeedtest) -> Unit,
    /**
     * Igual a [onNovoTeste], mas para quando o usuario ja confirmou o aviso de dados moveis
     * (ForaDoWifiDialog, Home) — pula o segundo gate de confirmacao em rede medida, que nao
     * tem UI fora da tab Velocidade (#516).
     */
    val onNovoTesteJaConfirmadoMovel: (ModoSpeedtest) -> Unit = onNovoTeste,
    val onCancelarTeste: () -> Unit,
    val onConfirmarSpeedtestMovel: () -> Unit = {},
    val onCancelarSpeedtestMovel: () -> Unit = {},
    val onSetSpeedtestPermiteHeavyMovel: (Boolean) -> Unit = {},
)

/**
 * Agrupa parametros de Wi-Fi e dispositivos para reduzir a assinatura do AppShell.
 */
@Stable
data class AppShellWifiState(
    val snapshotWifi: SnapshotScanWifi,
    val connectedNetwork: RedeVizinha?,
    val snapshotDevices: SnapshotScanDispositivos,
    val apelidos: Map<String, String>,
    val onRefreshDispositivos: () -> Unit,
    val onRefreshSinal: () -> Unit,
    val onSalvarApelido: (mac: String, apelido: String) -> Unit,
)

sealed class AnalisadorState {
    data object Inativo : AnalisadorState()

    data object Analisando : AnalisadorState()

    data class Resultado(
        val texto: String,
        val origem: String,
        val acoes: List<AiAcaoRecomendada> = emptyList(),
    ) : AnalisadorState()

    data class Erro(
        val mensagem: String,
    ) : AnalisadorState()
}

/**
 * Agrupa parametros do diagnostico e do chat inline de diagnostico.
 */
@Stable
data class AppShellDiagnosticoState(
    val snapshotDiagnostico: SnapshotDiagnostico,
    val onIniciarDiagnostico: () -> Unit,
    val diagChatHistorico: List<DiagChatEntry> = emptyList(),
    val diagChatCarregando: Boolean = false,
    val onEnviarPerguntaDiagnostico: (String) -> Unit = {},
    val onLimparDiagChat: () -> Unit = {},
    val analisadorState: AnalisadorState = AnalisadorState.Inativo,
    val onAnalisarProblema: (String) -> Unit = {},
    val onResetarAnalisador: () -> Unit = {},
    /** SIG-173/#664 — chamado quando o usuario fecha o LaudoScreen. Avalia elegibilidade
     *  para o prompt nativo de avaliacao do Google Play (nunca bloqueia o fechamento). */
    val onLaudoFechado: () -> Unit = {},
    /** Recomendacao do Recommendation Engine (#790/#811/#812) para o diagnostico atual --
     *  null quando `RecommendationEngine.choose` nao encontrou nada elegivel, ou depois que
     *  o usuario deu feedback "ocultar" (#813). */
    val recommendationDecision: RecommendationDecision? = null,
    /** Feedback ja dado pelo usuario para [recommendationDecision] nesta sessao, ou null. */
    val recommendationFeedback: RecommendationFeedbackType? = null,
    val onRecommendationShown: () -> Unit = {},
    val onRecommendationClicked: () -> Unit = {},
    val onRecommendationFeedback: (RecommendationFeedbackType) -> Unit = {},
    val onRecommendationDismissed: () -> Unit = {},
)

/**
 * Agrupa parametros do fluxo SignallQ (IA conversacional).
 */
@Stable
data class AppShellSignallQState(
    val signallQUiState: SignallQUiState,
    val gemmaAvailable: Boolean = false,
    val operadoraMovel: String? = null,
    val onIniciarSignallQ: (foco: String?) -> Unit,
    val onResetSignallQ: () -> Unit,
    val onSelecionarChip: (OpcaoResposta) -> Unit,
    val onResponderPergunta: (OpcaoResposta) -> Unit,
    val onEnviarMensagemTexto: (String) -> Unit = {},
    val onVerificarGemma: () -> Unit = {},
    val onIniciarSignallQComResultado: (ResultadoSpeedtest, String?) -> Unit = { _, _ -> },
)

/**
 * Agrupa o estado de monetizacao nativa (issue #555): flags remotas por tela + gate
 * de consentimento UMP. [podeRequisitarAnuncio] cobre tanto "UMP ainda nao respondeu"
 * quanto "usuario recusou personalizacao" -- em ambos os casos as telas nao chamam
 * `AdLoader.loadAd`, nao so nao mostram o card.
 */
@Stable
data class AppShellAdsState(
    val flags: AdsFlags = AdsFlags.DESLIGADO,
    val podeRequisitarAnuncio: Boolean = false,
)
