package io.veloo.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.veloo.app.BuildConfig
import io.veloo.app.FeatureFlags
import io.veloo.app.core.database.MedicaoEntity
import io.veloo.app.core.network.EstadoConexao
import io.veloo.app.core.network.SnapshotRede
import io.veloo.app.core.telephony.MovelSimSnapshot
import io.veloo.app.core.telephony.MovelSnapshot
import io.veloo.app.feature.devices.SnapshotScanDispositivos
import io.veloo.app.feature.diagnostico.ConnectionType
import io.veloo.app.feature.diagnostico.SnapshotDiagnostico
import io.veloo.app.feature.diagnostico.ai.AiDiagnosisRepository
import io.veloo.app.feature.diagnostico.ai.AiDiagnosisState
import io.veloo.app.feature.diagnostico.ai.AiMetricasAtuais
import io.veloo.app.feature.diagnostico.ai.DiagChatEntry
import io.veloo.app.feature.diagnostico.ai.DiagnosisAiContext
import io.veloo.app.feature.diagnostico.chat.ChatMensagem
import io.veloo.app.feature.diagnostico.chat.PapelChatMensagem
import io.veloo.app.feature.diagnostico.chat.StatusChatMensagem
import io.veloo.app.feature.diagnostico.chat.TipoDiagnostico
import io.veloo.app.feature.diagnostico.pulse.OpcaoResposta
import io.veloo.app.feature.dns.SnapshotBenchmarkDns
import io.veloo.app.feature.fibra.SnapshotFibra
import io.veloo.app.feature.history.ResumoHistorico
import io.veloo.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.veloo.app.feature.speedtest.ModoSpeedtest
import io.veloo.app.feature.speedtest.ResultadoSpeedtest
import io.veloo.app.feature.speedtest.SnapshotExecucaoSpeedtest
import io.veloo.app.feature.wifi.RedeVizinha
import io.veloo.app.feature.wifi.SnapshotScanWifi
import io.veloo.app.ui.FiltroConexaoHistorico
import io.veloo.app.ui.GatewayInfo
import io.veloo.app.ui.HistoryPoint
import io.veloo.app.ui.IspInfo
import io.veloo.app.ui.LkColors
import io.veloo.app.ui.LkTokens
import io.veloo.app.ui.LocalLkTokens
import io.veloo.app.ui.state.UiState
import io.veloo.app.ui.viewmodel.ChatDiagUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Overlay {
    Laudo,
    Chat,

    /** @deprecated Substituído por [ChatDiagnosticoIa]. Mantido para fallback. Remover na próxima major. */
    DiagnosticoInteligente,
    ChatDiagnosticoIa,
    LLMChat,
    Ping,
    Privacidade,
    Novidades,
    ResultadoVelocidade,
    Fibra,
    MinhaConexao,
    Dispositivos,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(
    snapshotRede: SnapshotRede,
    snapshotSpeedtest: SnapshotExecucaoSpeedtest,
    snapshotDns: SnapshotBenchmarkDns,
    snapshotDevices: SnapshotScanDispositivos,
    history: List<HistoryPoint>,
    localIp: UiState<String>,
    publicIp: UiState<String>,
    ispInfo: UiState<IspInfo>,
    gateways: List<GatewayInfo>,
    deviceName: String,
    dnsResolverIp: String?,
    connectedNetwork: RedeVizinha?,
    snapshotDiagnostico: SnapshotDiagnostico,
    snapshotWifi: SnapshotScanWifi,
    historico: List<MedicaoEntity>,
    blocoUptime: List<io.veloo.app.feature.history.BlocoUptime> = emptyList(),
    narrativaUptime: String = "",
    resumoHistorico: ResumoHistorico? = null,
    snapshotFibra: SnapshotFibra,
    modemHost: String?,
    modemUsername: String,
    modemPassword: String,
    modemPermanecerConectado: Boolean,
    gatewayIpDetectado: String?,
    localizacaoServidor: UiState<String>,
    temaSelecionado: String,
    analiseAvancada: Boolean,
    onNovoTeste: (ModoSpeedtest) -> Unit,
    onCancelarTeste: () -> Unit,
    onDispararBenchmarkDns: () -> Unit,
    onRefreshDispositivos: () -> Unit,
    apelidos: Map<String, String>,
    onSalvarApelido: (mac: String, apelido: String) -> Unit,
    onRefreshSinal: () -> Unit,
    onReconectarFibra: (host: String, username: String, password: String) -> Unit,
    onSalvarConfiguracaoModem: (host: String, username: String, password: String, permanecer: Boolean) -> Unit,
    onDefinirTemaSelecionado: (String) -> Unit,
    onDefinirAnaliseAvancada: (Boolean) -> Unit,
    nomeUsuario: String,
    fotoUriUsuario: String?,
    operadora: String,
    planoInternet: String,
    regiao: String,
    estadoUf: String,
    cidadeNome: String,
    ispConfirmado: Boolean,
    limiteAlertaMbps: Int,
    onLimparHistorico: () -> Unit,
    onApagarDadosLocais: () -> Unit,
    onResetarApp: () -> Unit,
    monitoramentoAtivo: Boolean,
    onAtivarMonitoramento: (Boolean) -> Unit,
    notificacaoLatenciaAtiva: Boolean,
    notificacaoDnsAtiva: Boolean,
    notificacaoRssiAtiva: Boolean,
    notificacaoSemInternetAtiva: Boolean,
    onDefinirNotificacaoLatenciaAtiva: (Boolean) -> Unit,
    onDefinirNotificacaoDnsAtiva: (Boolean) -> Unit,
    onDefinirNotificacaoRssiAtiva: (Boolean) -> Unit,
    onDefinirNotificacaoSemInternetAtiva: (Boolean) -> Unit,
    onSalvarPerfil: (nome: String, fotoUri: String?) -> Unit,
    onSalvarDadosProvedor: (operadora: String, plano: String, regiao: String) -> Unit,
    onSalvarEstadoCidade: (estadoUf: String, cidadeNome: String) -> Unit,
    onConfirmarIsp: (operadora: String) -> Unit,
    onDispensarBannerIsp: () -> Unit,
    onSalvarLimiteAlerta: (Int) -> Unit,
    onIniciarDiagnostico: () -> Unit,
    signallQUiState: SignallQUiState,
    movelSnapshot: MovelSnapshot?,
    simsAtivos: List<MovelSimSnapshot> = emptyList(),
    temPermissaoTelefonia: Boolean = false,
    onSolicitarPermissaoTelefonia: () -> Unit = {},
    temPermissaoLocalizacao: Boolean = true,
    localizacaoBloqueadaPermanentemente: Boolean = false,
    onSolicitarPermissaoLocalizacao: () -> Unit = {},
    onIniciarSignallQ: (foco: String?) -> Unit,
    onResetSignallQ: () -> Unit,
    onSelecionarChip: (OpcaoResposta) -> Unit,
    onResponderPergunta: (OpcaoResposta) -> Unit,
    gemmaAvailable: Boolean = false,
    operadoraMovel: String? = null,
    onVerificarGemma: () -> Unit = {},
    onIniciarSignallQComResultado: (ResultadoSpeedtest, String?) -> Unit = { _, _ -> },
    // T6.2/T6.5: mensagens digitadas têm fluxo separado do selecionarChip
    onEnviarMensagemTexto: (String) -> Unit = {},
    // Task 4 — confirmação de speedtest em rede medida
    speedtestPendenteModoMovel: ModoSpeedtest? = null,
    onConfirmarSpeedtestMovel: () -> Unit = {},
    onCancelarSpeedtestMovel: () -> Unit = {},
    // Task 5 — preferências de dados móveis para AjustesScreen
    speedtestPermiteHeavyMovel: Boolean = false,
    onSetSpeedtestPermiteHeavyMovel: (Boolean) -> Unit = {},
    speedtestMbConsumidosMes: Long = 0L,
    // Issue #66 — chat inline DiagnosticoScreen
    diagChatHistorico: List<DiagChatEntry> = emptyList(),
    diagChatCarregando: Boolean = false,
    onEnviarPerguntaDiagnostico: (String) -> Unit = {},
    onLimparDiagChat: () -> Unit = {},
    // Issue #85 — Minha Conexão
    velocidadeContratadaDownMbps: Int = 0,
    velocidadeContratadaUpMbps: Int = 0,
    onSalvarVelocidadeContratada: (downMbps: Int, upMbps: Int) -> Unit = { _, _ -> },
    onSalvarConexaoDadosCompletos: (operadora: String, estadoUf: String, cidadeNome: String, downMbps: Int, upMbps: Int) -> Unit = { _, _, _, _, _ -> },
    // #82 — Banner Anatel dismissível
    anatelBannerDismissed: Boolean = false,
    onDispensarBannerAnatel: () -> Unit = {},
    // #95 — Filtros do Historico
    historicoFiltrado: List<MedicaoEntity> = emptyList(),
    filtroConexaoHistorico: FiltroConexaoHistorico = FiltroConexaoHistorico.TODOS,
    onFiltroConexaoHistoricoChange: (FiltroConexaoHistorico) -> Unit = {},
    filtroOperadoraHistorico: String? = null,
    onFiltroOperadoraHistoricoChange: (String?) -> Unit = {},
    operadorasDisponiveisHistorico: List<String> = emptyList(),
    // Chat Diagnóstico IA
    chatDiagUiState: ChatDiagUiState = ChatDiagUiState(),
    onChatDiagEnviarMensagem: (String) -> Unit = {},
    onChatDiagAtualizarDraft: (String) -> Unit = {},
    onChatDiagEscolherOpcao: (TipoDiagnostico) -> Unit = {},
    onChatDiagAbrirSessao: (String) -> Unit = {},
    onChatDiagApagarSessao: (String) -> Unit = {},
    onChatDiagRenomearSessao: (String, String) -> Unit = { _, _ -> },
    onChatDiagNovaSessao: () -> Unit = {},
    onChatDiagToggleDrawer: () -> Unit = {},
    onChatDiagCancelarAcaoAtual: () -> Unit = {},
    /** AiDiagnosisRepository injetada pelo Hilt como @Singleton — compartilhada por todos
     *  os Composables filhos. Evita instancias duplicadas via remember {} em telas. */
    aiRepository: AiDiagnosisRepository,
) {
    val c = LocalLkTokens.current
    // Desempacota UiState<T> → tipos opcionais para as telas filhas que ainda recebem primitivos.
    // Loading e Error resultam em null — as telas exibem fallback textual próprio.
    val localizacaoServidorStr: String? = (localizacaoServidor as? UiState.Success)?.data
    val localIpStr: String? = (localIp as? UiState.Success)?.data
    val publicIpStr: String? = (publicIp as? UiState.Success)?.data
    val ispInfoData: IspInfo? = (ispInfo as? UiState.Success)?.data
    val isIspInfoLoading = publicIp is UiState.Loading
    var selectedTab by remember { mutableIntStateOf(0) }
    var modoSelecionado by remember { mutableStateOf(ModoSpeedtest.complete) }
    val overlayStack = remember { mutableStateListOf<Overlay>() }
    var showDnsSheet by remember { mutableStateOf(false) }
    var showForaDoWifiDialog by remember { mutableStateOf(false) }
    var diagInteligenteAnaliseSolicitada by remember { mutableStateOf(false) }
    var diagInteligenteAiState: AiDiagnosisState by remember { mutableStateOf(AiDiagnosisState.idle) }
    var showPerfilSheet by remember { mutableStateOf(false) }
    var testeAtivo by remember { mutableStateOf(false) }
    var mostrarConcluido by remember { mutableStateOf(false) }
    val primeiraHistoria = remember(historico) { historico.firstOrNull() }
    var llmChatDraft by remember { mutableStateOf("") }
    val llmChatMensagens = remember { mutableStateListOf<ChatMensagem>() }
    var llmIsStreaming by remember { mutableStateOf(false) }
    val llmCoroutineScope = rememberCoroutineScope()
    // llmAiRepository: alias local para legibilidade — usa a instancia @Singleton injetada.
    val llmAiRepository = aiRepository

    // NAV-D: verifica IA ao entrar na tab Velocidade (índice 1)
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) onVerificarGemma()
    }

    LaunchedEffect(showDnsSheet) {
        if (showDnsSheet) onDispararBenchmarkDns()
    }

    LaunchedEffect(snapshotSpeedtest.estado) {
        when (snapshotSpeedtest.estado) {
            EstadoExecucaoSpeedtest.executando -> testeAtivo = true
            EstadoExecucaoSpeedtest.concluido -> {
                if (testeAtivo) {
                    onIniciarDiagnostico()
                    mostrarConcluido = true
                    delay(400)
                    mostrarConcluido = false
                    if (Overlay.ResultadoVelocidade !in overlayStack) overlayStack.add(Overlay.ResultadoVelocidade)
                    testeAtivo = false
                }
            }
            else -> {}
        }
    }

    // Back em overlay: desfaz último overlay empilhado.
    BackHandler(enabled = overlayStack.isNotEmpty()) {
        overlayStack.removeLastOrNull()
    }

    // Back na tab Histórico (índice 3): volta para Home em vez de sair do app.
    // Sem este handler, o back gesture do sistema fecha o app enquanto o usuário está
    // navegando pelo histórico — comportamento confuso reportado como "trava".
    BackHandler(enabled = overlayStack.isEmpty() && selectedTab == 3) {
        selectedTab = 0
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = c.bgPrimary,
            bottomBar = {
                if (snapshotSpeedtest.estado != EstadoExecucaoSpeedtest.executando) {
                    AppBottomNavBar(
                        c = c,
                        selectedTab = selectedTab,
                        testeAtivo = testeAtivo,
                        onTabSelected = { selectedTab = it },
                    )
                }
            },
        ) { padding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
            ) {
                when (selectedTab) {
                    // NAV-E: Tab 0 — Home
                    0 ->
                        HomeScreen(
                            snapshotRede = snapshotRede,
                            snapshotSpeedtest = snapshotSpeedtest,
                            history = history,
                            ultimaMedicao = primeiraHistoria,
                            localIp = localIpStr,
                            publicIp = publicIpStr,
                            ispInfo = ispInfoData,
                            isIspInfoLoading = isIspInfoLoading,
                            gateways = gateways,
                            deviceName = deviceName,
                            nomeUsuario = nomeUsuario,
                            fotoUriUsuario = fotoUriUsuario,
                            connectedNetwork = connectedNetwork,
                            movelSnapshot = movelSnapshot,
                            simsAtivos = simsAtivos,
                            onIniciarTeste = { modo ->
                                if (snapshotRede.estadoConexao == EstadoConexao.movel) {
                                    // AppShell decide: em rede móvel mostra ForaDoWifiDialog
                                    // O modo fica registrado no modoSelecionado para uso posterior
                                    modoSelecionado = modo
                                    showForaDoWifiDialog = true
                                } else {
                                    modoSelecionado = modo
                                    onNovoTeste(modo)
                                }
                            },
                            onAbrirUltimoResultado = {
                                if (Overlay.ResultadoVelocidade !in overlayStack && snapshotSpeedtest.resultado != null) {
                                    overlayStack.add(Overlay.ResultadoVelocidade)
                                }
                            },
                            onAbrirHistorico = { selectedTab = 3 },
                            onAbrirPerfil = { showPerfilSheet = true },
                            // NAV-B: Sinal agora é tab 2 — navega por tab em vez de overlay
                            onAbrirRedes = { selectedTab = 2 },
                            anatelBannerDismissed = anatelBannerDismissed,
                            onDismissAnatelBanner = onDispensarBannerAnatel,
                            onAbrirDns = { showDnsSheet = true },
                            onAbrirPing = {
                                if (Overlay.Ping !in overlayStack) overlayStack.add(Overlay.Ping)
                            },
                            onAbrirDiagnostico = {
                                if (Overlay.DiagnosticoInteligente !in overlayStack) {
                                    overlayStack.add(Overlay.DiagnosticoInteligente)
                                }
                            },
                            snapshotDispositivos = snapshotDevices,
                            onAbrirDispositivos = {
                                if (Overlay.Dispositivos !in overlayStack) overlayStack.add(Overlay.Dispositivos)
                            },
                        )
                    // NAV-E: Tab 1 — Velocidade (SpeedTestScreen como tab fixa)
                    1 ->
                        SpeedTestScreen(
                            snapshotSpeedtest = snapshotSpeedtest,
                            snapshotRede = snapshotRede,
                            ispInfo = ispInfoData,
                            localizacaoServidor = localizacaoServidorStr,
                            modoSelecionado = modoSelecionado,
                            onModoSelecionado = { modoSelecionado = it },
                            onIniciarTeste = { onNovoTeste(modoSelecionado) },
                            onCancelarTeste = onCancelarTeste,
                            onAbrirDnsBenchmark = {
                                if (FeatureFlags.DNS_SCREEN) showDnsSheet = true
                            },
                            onAbrirDiagnostico = {
                                if (FeatureFlags.DIAGNOSTICO_CHAT) {
                                    onIniciarSignallQ(null)
                                    if (Overlay.Chat !in overlayStack) overlayStack.add(Overlay.Chat)
                                }
                            },
                            onAbrirPing = { if (Overlay.Ping !in overlayStack) overlayStack.add(Overlay.Ping) },
                            onVerResultado = { if (Overlay.ResultadoVelocidade !in overlayStack) overlayStack.add(Overlay.ResultadoVelocidade) },
                            onAbrirHistorico = { selectedTab = 3 },
                            onAbrirAjustes = { selectedTab = 4 },
                            nomeUsuario = nomeUsuario,
                            fotoUri = fotoUriUsuario,
                            onAbrirPerfil = { showPerfilSheet = true },
                            planoInternet = planoInternet,
                            speedtestPendenteModoMovel = speedtestPendenteModoMovel,
                            onConfirmarSpeedtestMovel = onConfirmarSpeedtestMovel,
                            onCancelarSpeedtestMovel = onCancelarSpeedtestMovel,
                        )
                    // NAV-B: Tab 2 — Sinal (SinalScreen como tab fixa, sem botão voltar)
                    2 ->
                        SinalScreen(
                            snapshotWifi = snapshotWifi,
                            connectedNetwork = connectedNetwork,
                            estadoConexao = snapshotRede.estadoConexao,
                            conectado = snapshotRede.conectado,
                            movelSnapshot = movelSnapshot,
                            simsAtivos = simsAtivos,
                            localIp = localIpStr,
                            temPermissaoTelefonia = temPermissaoTelefonia,
                            onSolicitarPermissaoTelefonia = onSolicitarPermissaoTelefonia,
                            temPermissaoLocalizacao = temPermissaoLocalizacao,
                            localizacaoBloqueadaPermanentemente = localizacaoBloqueadaPermanentemente,
                            onSolicitarPermissaoLocalizacao = onSolicitarPermissaoLocalizacao,
                            onRefresh = onRefreshSinal,
                            onVoltar = { selectedTab = 0 },
                            nomeUsuario = nomeUsuario,
                            fotoUri = fotoUriUsuario,
                            onAbrirPerfil = { showPerfilSheet = true },
                            wifiLinkSnapshot = snapshotRede.wifiLinkSnapshot,
                            onAbrirDispositivos = {
                                if (Overlay.Dispositivos !in overlayStack) overlayStack.add(Overlay.Dispositivos)
                            },
                        )
                    // Tab 3 — Historico (indice mantido conforme spec)
                    3 ->
                        HistoricoScreen(
                            historico = historicoFiltrado,
                            blocoUptime = blocoUptime,
                            narrativaUptime = narrativaUptime,
                            resumoHistorico = resumoHistorico,
                            nomeUsuario = nomeUsuario,
                            fotoUri = fotoUriUsuario,
                            onAbrirPerfil = { showPerfilSheet = true },
                            onIniciarTeste = { selectedTab = 1 },
                            filtroConexao = filtroConexaoHistorico,
                            onFiltroConexaoChange = onFiltroConexaoHistoricoChange,
                            filtroOperadora = filtroOperadoraHistorico,
                            onFiltroOperadoraChange = onFiltroOperadoraHistoricoChange,
                            operadorasDisponiveis = operadorasDisponiveisHistorico,
                        )
                    // Tab 4 — Ajustes
                    else ->
                        AjustesScreen(
                            perfil =
                                AjustesPerfilState(
                                    nomeUsuario = nomeUsuario,
                                    fotoUriUsuario = fotoUriUsuario,
                                    deviceName = deviceName,
                                    appVersion = BuildConfig.VERSION_NAME,
                                    onSalvarPerfil = onSalvarPerfil,
                                ),
                            provedor =
                                AjustesProvedorState(
                                    operadora = operadora,
                                    planoInternet = planoInternet,
                                    regiao = regiao,
                                    estadoUf = estadoUf,
                                    cidadeNome = cidadeNome,
                                    ispDetectado = ispInfoData?.isp,
                                    ispConfirmado = ispConfirmado,
                                    onSalvarDadosProvedor = onSalvarDadosProvedor,
                                    onSalvarEstadoCidade = onSalvarEstadoCidade,
                                    onConfirmarIsp = onConfirmarIsp,
                                    onDispensarBannerIsp = onDispensarBannerIsp,
                                ),
                            monitoramento =
                                AjustesMonitoramentoState(
                                    monitoramentoAtivo = monitoramentoAtivo,
                                    analiseAvancada = analiseAvancada,
                                    notificacaoLatenciaAtiva = notificacaoLatenciaAtiva,
                                    notificacaoDnsAtiva = notificacaoDnsAtiva,
                                    notificacaoRssiAtiva = notificacaoRssiAtiva,
                                    notificacaoSemInternetAtiva = notificacaoSemInternetAtiva,
                                    onAtivarMonitoramento = onAtivarMonitoramento,
                                    onDefinirAnaliseAvancada = onDefinirAnaliseAvancada,
                                    onDefinirNotificacaoLatenciaAtiva = onDefinirNotificacaoLatenciaAtiva,
                                    onDefinirNotificacaoDnsAtiva = onDefinirNotificacaoDnsAtiva,
                                    onDefinirNotificacaoRssiAtiva = onDefinirNotificacaoRssiAtiva,
                                    onDefinirNotificacaoSemInternetAtiva = onDefinirNotificacaoSemInternetAtiva,
                                ),
                            modem =
                                AjustesModemState(
                                    modemHost = modemHost,
                                    modemUsername = modemUsername,
                                    modemPassword = modemPassword,
                                    modemPermanecerConectado = modemPermanecerConectado,
                                    gatewayIpDetectado = gatewayIpDetectado,
                                    onSalvarConfiguracaoModem = onSalvarConfiguracaoModem,
                                    onConectarFibra = { host, user, pass -> onReconectarFibra(host, user, pass) },
                                ),
                            temaSelecionado = temaSelecionado,
                            onDefinirTemaSelecionado = onDefinirTemaSelecionado,
                            limiteAlertaMbps = limiteAlertaMbps,
                            onSalvarLimiteAlerta = onSalvarLimiteAlerta,
                            onLimparHistorico = onLimparHistorico,
                            onApagarDadosLocais = onApagarDadosLocais,
                            onResetarApp = onResetarApp,
                            onAbrirHistorico = { selectedTab = 3 },
                            onAbrirLaudo = { if (Overlay.Laudo !in overlayStack) overlayStack.add(Overlay.Laudo) },
                            onAbrirPerfil = { showPerfilSheet = true },
                            onAbrirPrivacidade = { if (Overlay.Privacidade !in overlayStack) overlayStack.add(Overlay.Privacidade) },
                            onAbrirNovidades = { if (Overlay.Novidades !in overlayStack) overlayStack.add(Overlay.Novidades) },
                            onAbrirMinhaConexao = { if (Overlay.MinhaConexao !in overlayStack) overlayStack.add(Overlay.MinhaConexao) },
                            dadosMoveis =
                                AjustesDadosMoveisState(
                                    speedtestPermiteHeavyMovel = speedtestPermiteHeavyMovel,
                                    speedtestMbConsumidosMes = speedtestMbConsumidosMes,
                                    onSetSpeedtestPermiteHeavyMovel = onSetSpeedtestPermiteHeavyMovel,
                                ),
                        )
                }
            }
        }

        // Overlay de execução do speedtest — cobre toda a tela durante o teste
        AnimatedVisibility(
            visible =
                snapshotSpeedtest.estado == EstadoExecucaoSpeedtest.executando ||
                    snapshotSpeedtest.estado == EstadoExecucaoSpeedtest.erro,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            VelocidadeScreen(
                snapshot = snapshotSpeedtest,
                localizacaoServidor = localizacaoServidorStr,
                ispInfo = ispInfoData,
                onCancelar = onCancelarTeste,
                onReiniciar = { onNovoTeste(modoSelecionado) },
                onVoltar = onCancelarTeste,
            )
        }

        AnimatedVisibility(
            visible = mostrarConcluido,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            val cLocal = LocalLkTokens.current
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(cLocal.bgPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Concluído",
                        tint = LkColors.success,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Concluído",
                        style = MaterialTheme.typography.titleLarge,
                        color = LkColors.success,
                        fontWeight = FontWeight.W600,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = Overlay.ResultadoVelocidade in overlayStack && snapshotSpeedtest.resultado != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            snapshotSpeedtest.resultado?.let { resultado ->
                ResultadoVelocidadeScreen(
                    resultado = resultado,
                    snapshotDiagnostico = snapshotDiagnostico,
                    onTestarNovamente = { overlayStack.remove(Overlay.ResultadoVelocidade) },
                    onIrParaHome = {
                        overlayStack.remove(Overlay.ResultadoVelocidade)
                        selectedTab = 0
                    },
                    onVoltar = { overlayStack.remove(Overlay.ResultadoVelocidade) },
                    localizacaoServidor = localizacaoServidorStr,
                    onAbrirChat = {
                        if (Overlay.LLMChat !in overlayStack) {
                            overlayStack.add(Overlay.LLMChat)
                        }
                    },
                    ispInfo = ispInfoData,
                    operadoraMovel = operadoraMovel,
                    anatelBannerDismissed = anatelBannerDismissed,
                    onDismissAnatelBanner = onDispensarBannerAnatel,
                )
            }
        }

        AnimatedVisibility(
            visible = Overlay.Chat in overlayStack && FeatureFlags.DIAGNOSTICO_CHAT,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            ChatScreen(
                uiState = signallQUiState,
                onNavigateBack = { overlayStack.remove(Overlay.Chat) },
                onIniciarSignallQ = onIniciarSignallQ,
                onResetSignallQ = {
                    onResetSignallQ()
                    overlayStack.remove(Overlay.Chat)
                },
                onSelecionarChip = onSelecionarChip,
                onResponderPergunta = onResponderPergunta,
                onEnviarMensagemTexto = onEnviarMensagemTexto,
            )
        }

        AnimatedVisibility(
            visible = Overlay.Laudo in overlayStack,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            LaudoScreen(
                snapshotDiagnostico = snapshotDiagnostico,
                ultimaMedicao = primeiraHistoria,
                nomeUsuario = nomeUsuario,
                operadora = operadora,
                ssid = connectedNetwork?.ssid,
                ipLocal = localIpStr,
                ipPublico = publicIpStr,
                onVoltar = { overlayStack.remove(Overlay.Laudo) },
                velocidadeContratadaMbps = planoInternet.filter { it.isDigit() }.toIntOrNull(),
            )
        }

        AnimatedVisibility(
            visible = Overlay.Privacidade in overlayStack,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            PrivacidadeScreen(
                onVoltar = { overlayStack.remove(Overlay.Privacidade) },
                onApagarDadosLocais = onApagarDadosLocais,
                onResetarApp = onResetarApp,
            )
        }

        AnimatedVisibility(
            visible = Overlay.Novidades in overlayStack,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            NovidadesScreen(
                appVersion = BuildConfig.VERSION_NAME,
                onVoltar = { overlayStack.remove(Overlay.Novidades) },
            )
        }

        @Suppress("DEPRECATION")
        AnimatedVisibility(
            visible = Overlay.DiagnosticoInteligente in overlayStack,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            @Suppress("DEPRECATION")
            DiagnosticoScreen(
                snapshotDiagnostico = snapshotDiagnostico,
                onAbrirRedes = { selectedTab = 2 },
                onIniciarDiagnostico = onIniciarDiagnostico,
                analiseSolicitada = diagInteligenteAnaliseSolicitada,
                onAnaliseSolicitadaChange = { diagInteligenteAnaliseSolicitada = it },
                aiState = diagInteligenteAiState,
                onAiStateChange = { diagInteligenteAiState = it },
                onVoltar = {
                    overlayStack.remove(Overlay.DiagnosticoInteligente)
                    onLimparDiagChat()
                },
                onRefazer = {
                    overlayStack.remove(Overlay.DiagnosticoInteligente)
                },
                onAbrirChat = {
                    if (Overlay.LLMChat !in overlayStack) {
                        overlayStack.add(Overlay.LLMChat)
                    }
                },
                chatHistorico = diagChatHistorico,
                chatCarregando = diagChatCarregando,
                onEnviarChat = onEnviarPerguntaDiagnostico,
                aiRepository = aiRepository,
            )
        }

        AnimatedVisibility(
            visible = Overlay.ChatDiagnosticoIa in overlayStack,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            ChatDiagnosticoIaScreen(
                uiState = chatDiagUiState,
                onVoltar = { overlayStack.remove(Overlay.ChatDiagnosticoIa) },
                onEnviarMensagem = onChatDiagEnviarMensagem,
                onAtualizarDraft = onChatDiagAtualizarDraft,
                onEscolherOpcao = onChatDiagEscolherOpcao,
                onAbrirSessao = onChatDiagAbrirSessao,
                onApagarSessao = onChatDiagApagarSessao,
                onRenomearSessao = onChatDiagRenomearSessao,
                onNovaSessao = onChatDiagNovaSessao,
                onToggleDrawer = onChatDiagToggleDrawer,
                onCancelarAcaoAtual = onChatDiagCancelarAcaoAtual,
            )
        }

        AnimatedVisibility(
            visible = Overlay.LLMChat in overlayStack,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            LLMChatScreen(
                mensagens = llmChatMensagens,
                draft = llmChatDraft,
                isStreaming = llmIsStreaming,
                chips = listOf("Como troco o canal do Wi-Fi?", "Vale a pena 5 GHz?"),
                onEnviarMensagem = onEnviarMensagem@{ texto ->
                    if (llmIsStreaming) return@onEnviarMensagem
                    val userMsgId =
                        java.util.UUID
                            .randomUUID()
                            .toString()
                    llmChatMensagens.add(
                        ChatMensagem(
                            id = userMsgId,
                            sessionId = "llm-session",
                            papel = PapelChatMensagem.usuario,
                            conteudo = texto,
                            criadoEmEpochMs = System.currentTimeMillis(),
                            status = StatusChatMensagem.concluido,
                        ),
                    )
                    llmChatDraft = ""

                    val aiMsgId =
                        java.util.UUID
                            .randomUUID()
                            .toString()
                    llmChatMensagens.add(
                        ChatMensagem(
                            id = aiMsgId,
                            sessionId = "llm-session",
                            papel = PapelChatMensagem.assistente,
                            conteudo = "",
                            criadoEmEpochMs = System.currentTimeMillis(),
                            status = StatusChatMensagem.streaming,
                        ),
                    )

                    llmIsStreaming = true
                    llmCoroutineScope.launch(Dispatchers.IO) {
                        try {
                            val resultado = snapshotSpeedtest.resultado
                            val contexto =
                                if (resultado != null) {
                                    DiagnosisAiContext(
                                        generatedAtEpochMs = resultado.timestampEpochMs,
                                        connectionType = ConnectionType.wifi,
                                        metricasAtuais =
                                            AiMetricasAtuais(
                                                downloadMbps = resultado.downloadMbps,
                                                uploadMbps = resultado.uploadMbps,
                                                latenciaMs = resultado.latenciaMs,
                                                jitterMs = resultado.jitterMs,
                                                perdaPacotesPercentual = resultado.perdaPercentual,
                                                bufferbloatMs = resultado.bufferbloatMs,
                                                severidadeBufferbloat = resultado.severidadeBufferbloat.name,
                                                stabilityScore = resultado.stabilityScore,
                                                peakDownloadMbps = resultado.peakDownloadMbps,
                                                peakUploadMbps = resultado.peakUploadMbps,
                                                latencyDownloadMs = resultado.latencyDownloadMs,
                                                latencyUploadMs = resultado.latencyUploadMs,
                                                packetLossSource = resultado.packetLossSource,
                                            ),
                                        feedbackUsuario = texto,
                                        evidencias = emptyList(),
                                    )
                                } else {
                                    DiagnosisAiContext(
                                        generatedAtEpochMs = System.currentTimeMillis(),
                                        connectionType = ConnectionType.wifi,
                                        feedbackUsuario = texto,
                                        evidencias = emptyList(),
                                    )
                                }

                            var acumulado = ""
                            llmAiRepository.explainDiagnosisStream(contexto).collect { token ->
                                acumulado += token
                                val idx = llmChatMensagens.indexOfFirst { it.id == aiMsgId }
                                if (idx >= 0) {
                                    llmChatMensagens[idx] = llmChatMensagens[idx].copy(conteudo = acumulado)
                                }
                            }

                            val idx = llmChatMensagens.indexOfFirst { it.id == aiMsgId }
                            if (idx >= 0) {
                                llmChatMensagens[idx] =
                                    llmChatMensagens[idx].copy(
                                        conteudo = acumulado.ifBlank { "Não recebi uma resposta completa. Tente novamente." },
                                        status = StatusChatMensagem.concluido,
                                    )
                            }
                        } catch (e: Exception) {
                            val idx = llmChatMensagens.indexOfFirst { it.id == aiMsgId }
                            if (idx >= 0) {
                                llmChatMensagens[idx] =
                                    llmChatMensagens[idx].copy(
                                        conteudo = "Não consegui processar sua pergunta. Tente novamente.",
                                        status = StatusChatMensagem.concluido,
                                    )
                            }
                        } finally {
                            llmIsStreaming = false
                        }
                    }
                },
                onAtualizarDraft = { llmChatDraft = it },
                onSelecionarChip = { chip -> llmChatDraft = chip },
                onNovaSessao = {
                    llmChatMensagens.clear()
                    llmChatDraft = ""
                },
                onVoltar = { overlayStack.remove(Overlay.LLMChat) },
            )
        }

        if (Overlay.Ping in overlayStack) {
            PingScreen(onDismiss = { overlayStack.remove(Overlay.Ping) })
        }

        AnimatedVisibility(
            visible = Overlay.Fibra in overlayStack,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            FibraModemScreen(
                snapshotFibra = snapshotFibra,
                onVoltar = { overlayStack.remove(Overlay.Fibra) },
                onRetentar = { onReconectarFibra(modemHost ?: "", modemUsername, modemPassword) },
                onAbrirAjustes = { selectedTab = 4 },
            )
        }

        AnimatedVisibility(
            visible = Overlay.Dispositivos in overlayStack,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            DispositivosScreen(
                snapshotDevices = snapshotDevices,
                snapshotRede = snapshotRede,
                onRefresh = {
                    onRefreshDispositivos()
                },
                apelidos = apelidos,
                onSalvarApelido = onSalvarApelido,
                onVoltar = { overlayStack.remove(Overlay.Dispositivos) },
            )
        }

        AnimatedVisibility(
            visible = Overlay.MinhaConexao in overlayStack,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            MinhaConexaoScreen(
                operadora = operadora,
                estadoUf = estadoUf,
                cidadeNome = cidadeNome,
                velocidadeContratadaDownMbps = velocidadeContratadaDownMbps,
                velocidadeContratadaUpMbps = velocidadeContratadaUpMbps,
                operadoraAutodetectada = movelSnapshot?.operadora,
                onSalvar = { op, uf, cidade, down, up ->
                    onSalvarDadosProvedor(op, planoInternet, regiao)
                    onSalvarEstadoCidade(uf, cidade)
                    onSalvarVelocidadeContratada(down, up)
                    overlayStack.remove(Overlay.MinhaConexao)
                },
                onVoltar = { overlayStack.remove(Overlay.MinhaConexao) },
            )
        }

        if (showDnsSheet && FeatureFlags.DNS_SCREEN) {
            ModalBottomSheet(
                onDismissRequest = { showDnsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = c.bgSecondary,
            ) {
                DnsSheetContent(
                    snapshotDns = snapshotDns,
                    dnsResolverIp = dnsResolverIp,
                    snapshotRede = snapshotRede,
                    c = c,
                )
            }
        }

        if (showPerfilSheet) {
            PerfilEditSheet(
                c = c,
                nomeAtual = nomeUsuario,
                fotoUriAtual = fotoUriUsuario,
                deviceName = deviceName,
                appVersion = BuildConfig.VERSION_NAME,
                ispInfo = ispInfoData,
                estadoConexao = snapshotRede.estadoConexao,
                onDismiss = { showPerfilSheet = false },
                onSalvar = { nome, fotoUri ->
                    onSalvarPerfil(nome, fotoUri)
                    showPerfilSheet = false
                },
            )
        }

        if (showForaDoWifiDialog) {
            ForaDoWifiDialog(
                onContinuar = {
                    showForaDoWifiDialog = false
                    onNovoTeste(modoSelecionado)
                },
                onCancelar = { showForaDoWifiDialog = false },
            )
        }
    }
}

@Composable
private fun AppBottomNavBar(
    c: LkTokens,
    selectedTab: Int,
    testeAtivo: Boolean = false,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        HorizontalDivider(color = c.border, thickness = 1.dp)
        NavigationBar(
            containerColor = c.bgSecondary,
            tonalElevation = 0.dp,
        ) {
            AppNavItem(c, selectedTab, 0, "Início", Icons.Outlined.Home, Icons.Filled.Home, onTabSelected)
            AppNavItem(c, selectedTab, 1, "Velocidade", Icons.Outlined.Speed, Icons.Filled.Speed, onTabSelected, showBadge = testeAtivo)
            AppNavItem(c, selectedTab, 2, "Sinal", Icons.Outlined.Wifi, Icons.Filled.Wifi, onTabSelected)
            AppNavItem(c, selectedTab, 3, "Histórico", Icons.Outlined.History, Icons.Filled.History, onTabSelected)
            AppNavItem(c, selectedTab, 4, "Ajustes", Icons.Outlined.Settings, Icons.Filled.Settings, onTabSelected)
        }
    }
}

@Composable
private fun RowScope.AppNavItem(
    c: LkTokens,
    selectedTab: Int,
    index: Int,
    label: String,
    outlinedIcon: ImageVector,
    filledIcon: ImageVector,
    onTabSelected: (Int) -> Unit,
    showBadge: Boolean = false,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val badgePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(700, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "badgeAlpha",
    )
    NavigationBarItem(
        selected = selectedTab == index,
        onClick = { onTabSelected(index) },
        icon = {
            BadgedBox(badge = {
                if (showBadge) Badge(modifier = Modifier.graphicsLayer { alpha = badgePulseAlpha })
            }) {
                Icon(
                    imageVector = if (selectedTab == index) filledIcon else outlinedIcon,
                    contentDescription = label,
                )
            }
        },
        label = { Text(label, fontWeight = FontWeight.W600) },
        colors =
            NavigationBarItemDefaults.colors(
                selectedIconColor = LkColors.accent,
                unselectedIconColor = c.textTertiary,
                selectedTextColor = LkColors.accent,
                unselectedTextColor = c.textTertiary,
                indicatorColor = LkColors.accent.copy(alpha = 0.12f),
            ),
    )
}

// ─── Dialog: fora do Wi-Fi ────────────────────────────────────────────────────

@Composable
private fun ForaDoWifiDialog(
    onContinuar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Sem Wi-Fi", fontWeight = FontWeight.W600) },
        text = {
            Text(
                "Você está usando dados móveis. Fazer um teste de velocidade pode consumir uma quantidade significativa do seu plano de dados.\n\nDeseja continuar mesmo assim?",
                fontSize = 14.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onContinuar) {
                Text("Continuar mesmo assim", color = LkColors.warning)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        },
    )
}
