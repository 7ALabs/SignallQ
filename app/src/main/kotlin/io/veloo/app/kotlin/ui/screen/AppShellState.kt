package io.veloo.app.ui.screen

import androidx.compose.runtime.Stable
import io.veloo.app.feature.devices.SnapshotScanDispositivos
import io.veloo.app.feature.diagnostico.SnapshotDiagnostico
import io.veloo.app.feature.diagnostico.ai.DiagChatEntry
import io.veloo.app.feature.diagnostico.chat.TipoDiagnostico
import io.veloo.app.feature.diagnostico.pulse.OpcaoResposta
import io.veloo.app.feature.speedtest.ModoSpeedtest
import io.veloo.app.feature.speedtest.ResultadoSpeedtest
import io.veloo.app.feature.speedtest.SnapshotExecucaoSpeedtest
import io.veloo.app.feature.wifi.RedeVizinha
import io.veloo.app.feature.wifi.SnapshotScanWifi
import io.veloo.app.ui.viewmodel.ChatDiagUiState

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
 * Agrupa parametros do chat de diagnostico IA (sessoes persistidas).
 */
@Stable
data class AppShellChatDiagState(
    val chatDiagUiState: ChatDiagUiState = ChatDiagUiState(),
    val onEnviarMensagem: (String) -> Unit = {},
    val onAtualizarDraft: (String) -> Unit = {},
    val onEscolherOpcao: (TipoDiagnostico) -> Unit = {},
    val onAbrirSessao: (String) -> Unit = {},
    val onApagarSessao: (String) -> Unit = {},
    val onRenomearSessao: (String, String) -> Unit = { _, _ -> },
    val onNovaSessao: () -> Unit = {},
    val onToggleDrawer: () -> Unit = {},
    val onCancelarAcaoAtual: () -> Unit = {},
)
