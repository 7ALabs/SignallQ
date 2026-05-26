package io.linka.app.kotlin.ui.screen

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.linka.app.kotlin.BuildConfig
import io.linka.app.kotlin.FeatureFlags
import io.linka.app.kotlin.core.database.MedicaoEntity
import io.linka.app.kotlin.core.network.EstadoConexao
import io.linka.app.kotlin.core.network.SnapshotRede
import io.linka.app.kotlin.core.telephony.MovelSnapshot
import io.linka.app.kotlin.feature.devices.SnapshotScanDispositivos
import io.linka.app.kotlin.feature.diagnostico.SnapshotDiagnostico
import io.linka.app.kotlin.feature.diagnostico.ai.AiDiagnosisState
import io.linka.app.kotlin.feature.diagnostico.ai.DiagChatEntry
import io.linka.app.kotlin.feature.diagnostico.pulse.OpcaoResposta
import io.linka.app.kotlin.feature.dns.EstadoBenchmarkDns
import io.linka.app.kotlin.feature.dns.ResultadoBenchmarkDns
import io.linka.app.kotlin.feature.dns.SnapshotBenchmarkDns
import io.linka.app.kotlin.feature.fibra.EstadoFibra
import io.linka.app.kotlin.feature.fibra.SnapshotFibra
import io.linka.app.kotlin.feature.history.ResumoHistorico
import io.linka.app.kotlin.feature.speedtest.EstadoExecucaoSpeedtest
import io.linka.app.kotlin.feature.speedtest.ModoSpeedtest
import io.linka.app.kotlin.feature.speedtest.ResultadoSpeedtest
import io.linka.app.kotlin.feature.speedtest.SnapshotExecucaoSpeedtest
import io.linka.app.kotlin.feature.wifi.RedeVizinha
import io.linka.app.kotlin.feature.wifi.SnapshotScanWifi
import io.linka.app.kotlin.ui.GatewayInfo
import io.linka.app.kotlin.ui.HistoryPoint
import io.linka.app.kotlin.ui.IspInfo
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkRadius
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LkTokens
import io.linka.app.kotlin.ui.LocalLkTokens
import io.linka.app.kotlin.ui.state.UiState
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private enum class Overlay {
    Laudo,
    Chat,
    DiagnosticoInteligente,
    Ping,
    Privacidade,
    Novidades,
    ResultadoVelocidade,
    Fibra,
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
    blocoUptime: List<io.linka.app.kotlin.feature.history.BlocoUptime> = emptyList(),
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
    orbitUiState: OrbitUiState,
    movelSnapshot: MovelSnapshot?,
    temPermissaoTelefonia: Boolean = false,
    onSolicitarPermissaoTelefonia: () -> Unit = {},
    temPermissaoLocalizacao: Boolean = true,
    onSolicitarPermissaoLocalizacao: () -> Unit = {},
    onIniciarOrbit: (foco: String?) -> Unit,
    onResetOrbit: () -> Unit,
    onSelecionarChip: (OpcaoResposta) -> Unit,
    onResponderPergunta: (OpcaoResposta) -> Unit,
    gemmaAvailable: Boolean = false,
    onVerificarGemma: () -> Unit = {},
    onIniciarOrbitComResultado: (ResultadoSpeedtest, String?) -> Unit = { _, _ -> },
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
) {
    val c = LocalLkTokens.current
    // Desempacota UiState<T> → tipos opcionais para as telas filhas que ainda recebem primitivos.
    // Loading e Error resultam em null — as telas exibem fallback textual próprio.
    val localizacaoServidorStr: String? = (localizacaoServidor as? UiState.Success)?.data
    val localIpStr: String? = (localIp as? UiState.Success)?.data
    val publicIpStr: String? = (publicIp as? UiState.Success)?.data
    val ispInfoData: IspInfo? = (ispInfo as? UiState.Success)?.data
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

    LaunchedEffect(snapshotFibra.estado) {
        if (snapshotFibra.estado != EstadoFibra.idle) {
            if (Overlay.Fibra !in overlayStack) overlayStack.add(Overlay.Fibra)
        }
    }

    BackHandler(enabled = overlayStack.isNotEmpty()) {
        overlayStack.removeLastOrNull()
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
                            gateways = gateways,
                            deviceName = deviceName,
                            nomeUsuario = nomeUsuario,
                            fotoUriUsuario = fotoUriUsuario,
                            connectedNetwork = connectedNetwork,
                            movelSnapshot = movelSnapshot,
                            onNovoTeste = {
                                if (snapshotRede.estadoConexao == EstadoConexao.movel) {
                                    showForaDoWifiDialog = true
                                } else {
                                    selectedTab = 1
                                }
                            },
                            onAbrirHistorico = { selectedTab = 3 },
                            onAbrirPerfil = { showPerfilSheet = true },
                            // NAV-B: Sinal agora é tab 2 — navega por tab em vez de overlay
                            onAbrirRedes = { selectedTab = 2 },
                            onAbrirDiagnostico = {
                                diagInteligenteAnaliseSolicitada = true
                                diagInteligenteAiState = AiDiagnosisState.idle
                                if (Overlay.DiagnosticoInteligente !in overlayStack) {
                                    overlayStack.add(Overlay.DiagnosticoInteligente)
                                }
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
                                    onIniciarOrbit(null)
                                    if (Overlay.Chat !in overlayStack) overlayStack.add(Overlay.Chat)
                                }
                            },
                            onAbrirPing = { if (Overlay.Ping !in overlayStack) overlayStack.add(Overlay.Ping) },
                            onVerResultado = { if (Overlay.ResultadoVelocidade !in overlayStack) overlayStack.add(Overlay.ResultadoVelocidade) },
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
                            localIp = localIpStr,
                            temPermissaoTelefonia = temPermissaoTelefonia,
                            onSolicitarPermissaoTelefonia = onSolicitarPermissaoTelefonia,
                            temPermissaoLocalizacao = temPermissaoLocalizacao,
                            onSolicitarPermissaoLocalizacao = onSolicitarPermissaoLocalizacao,
                            onRefresh = onRefreshSinal,
                            onVoltar = { selectedTab = 0 },
                            nomeUsuario = nomeUsuario,
                            fotoUri = fotoUriUsuario,
                            onAbrirPerfil = { showPerfilSheet = true },
                            wifiLinkSnapshot = snapshotRede.wifiLinkSnapshot,
                        )
                    // Tab 3 — Histórico (índice mantido conforme spec)
                    3 ->
                        HistoricoScreen(
                            historico = historico,
                            blocoUptime = blocoUptime,
                            narrativaUptime = narrativaUptime,
                            resumoHistorico = resumoHistorico,
                            nomeUsuario = nomeUsuario,
                            fotoUri = fotoUriUsuario,
                            onAbrirPerfil = { showPerfilSheet = true },
                            onIniciarTeste = { selectedTab = 1 },
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
                    gemmaAvailable = gemmaAvailable,
                    onAbrirChat = {
                        onIniciarOrbitComResultado(resultado, null)
                        if (Overlay.Chat !in overlayStack) overlayStack.add(Overlay.Chat)
                    },
                    ispInfo = ispInfoData,
                )
            }
        }

        AnimatedVisibility(
            visible = Overlay.Chat in overlayStack && FeatureFlags.DIAGNOSTICO_CHAT,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            ChatScreen(
                uiState = orbitUiState,
                onNavigateBack = { overlayStack.remove(Overlay.Chat) },
                onIniciarOrbit = onIniciarOrbit,
                onResetOrbit = {
                    onResetOrbit()
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
            PrivacidadeScreen(onVoltar = { overlayStack.remove(Overlay.Privacidade) })
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

        AnimatedVisibility(
            visible = Overlay.DiagnosticoInteligente in overlayStack,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            DiagnosticoScreen(
                snapshotDiagnostico = snapshotDiagnostico,
                onAbrirRedes = { selectedTab = 2 },
                onIniciarDiagnostico = onIniciarDiagnostico,
                analiseSolicitada = diagInteligenteAnaliseSolicitada,
                onAnaliseSolicitadaChange = { diagInteligenteAnaliseSolicitada = it },
                aiState = diagInteligenteAiState,
                onAiStateChange = { diagInteligenteAiState = it },
                onVoltar = { overlayStack.remove(Overlay.DiagnosticoInteligente) },
                chatHistorico = diagChatHistorico,
                chatCarregando = diagChatCarregando,
                onEnviarChat = onEnviarPerguntaDiagnostico,
            )
        }

        if (Overlay.Ping in overlayStack) {
            PingScreen(onDismiss = { overlayStack.remove(Overlay.Ping) })
        }

        if (Overlay.Fibra in overlayStack) {
            FibraStatusOverlay(
                snapshotFibra = snapshotFibra,
                onDismiss = { overlayStack.remove(Overlay.Fibra) },
            )
        }

        if (showDnsSheet && FeatureFlags.DNS_SCREEN) {
            ModalBottomSheet(
                onDismissRequest = { showDnsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = c.bgSecondary,
            ) {
                DnsComparisonSheetContent(
                    snapshotDns = snapshotDns,
                    dnsResolverIp = dnsResolverIp,
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
                    selectedTab = 1
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

// ─── DNS comparison sheet ─────────────────────────────────────────────────────

private fun resolveDnsName(dnsIp: String?): String =
    when (dnsIp) {
        "1.1.1.1", "1.0.0.1" -> "Cloudflare"
        "8.8.8.8", "8.8.4.4" -> "Google DNS"
        "9.9.9.9", "149.112.112.112" -> "Quad9"
        "208.67.222.222", "208.67.220.220" -> "OpenDNS"
        "94.140.14.14", "94.140.15.15" -> "AdGuard"
        else -> "DNS do Provedor"
    }

@Composable
private fun DnsComparisonSheetContent(
    snapshotDns: SnapshotBenchmarkDns,
    dnsResolverIp: String?,
    c: LkTokens,
) {
    val currentDnsName = resolveDnsName(dnsResolverIp)
    val isLoading = snapshotDns.estado == EstadoBenchmarkDns.executando
    var showGuia by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(c.border),
            )
        }
        Spacer(Modifier.height(20.dp))

        if (showGuia) {
            DnsGuideView(c = c, onVoltar = { showGuia = false })
        } else {
            Text("Comparativo de DNS", fontSize = 18.sp, fontWeight = FontWeight.W700, color = c.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Latência via DoH · menor é melhor", fontSize = 12.sp, color = c.textSecondary)
            Spacer(Modifier.height(16.dp))

            if (snapshotDns.estado == EstadoBenchmarkDns.erro) {
                Text(
                    text = "Falha ao executar o benchmark. Tente novamente.",
                    fontSize = 13.sp,
                    color = LkColors.error,
                )
            } else if (isLoading && snapshotDns.resultados.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LkColors.accent)
                    Spacer(Modifier.width(10.dp))
                    Text("Medindo servidores...", fontSize = 13.sp, color = c.textSecondary)
                }
            } else {
                val recomendadoNome = snapshotDns.resultados.firstOrNull { it.nomeProvedor != currentDnsName }?.nomeProvedor
                HorizontalDivider(color = c.border, thickness = 1.dp)
                snapshotDns.resultados.forEach { server ->
                    DnsRowSheet(
                        result = server,
                        isCurrent = server.nomeProvedor == currentDnsName,
                        isRecomendado = server.nomeProvedor == recomendadoNome,
                        c = c,
                    )
                    HorizontalDivider(color = c.border, thickness = 1.dp)
                }
                if (isLoading) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = LkColors.accent.copy(alpha = 0.6f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Medindo...", fontSize = 11.sp, color = c.textTertiary)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(
                modifier =
                    Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showGuia = true }
                        .padding(vertical = LkSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Como alterar meu DNS", fontSize = 13.sp, color = LkColors.accent, fontWeight = FontWeight.W500)
            }
        }
    }
}

@Composable
private fun DnsRowSheet(
    result: ResultadoBenchmarkDns,
    isCurrent: Boolean,
    isRecomendado: Boolean,
    c: LkTokens,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = LkSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    result.nomeProvedor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W600,
                    color = if (isRecomendado) LkColors.success else c.textPrimary,
                )
                if (isCurrent) {
                    Spacer(Modifier.width(LkSpacing.sm))
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(LkColors.accent.copy(alpha = 0.12f))
                                .padding(horizontal = LkSpacing.sm, vertical = 4.dp),
                    ) {
                        Text("atual", fontSize = 10.sp, fontWeight = FontWeight.W600, color = LkColors.accent)
                    }
                }
                if (isRecomendado) {
                    Spacer(Modifier.width(LkSpacing.sm))
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(LkColors.success.copy(alpha = 0.12f))
                                .padding(horizontal = LkSpacing.sm, vertical = 4.dp),
                    ) {
                        Text("recomendado", fontSize = 10.sp, fontWeight = FontWeight.W600, color = LkColors.success)
                    }
                }
            }
        }
        val tempoMs = result.tempoMs
        if (result.erroMensagem != null && tempoMs == null) {
            Text("Falhou", fontSize = 12.sp, color = LkColors.error)
        } else if (tempoMs != null) {
            Text("${tempoMs.roundToInt()} ms", fontSize = 13.sp, fontWeight = FontWeight.W500, color = c.textSecondary)
            Spacer(Modifier.width(8.dp))
            result.gradeRapidez?.let { DnsGradeBadge(grade = it, c = c) }
        }
    }
}

@Composable
private fun DnsGradeBadge(
    grade: String,
    c: LkTokens,
) {
    val color =
        when (grade) {
            "A" -> LkColors.success
            "B" -> LkColors.accent
            "C" -> LkColors.warning
            else -> LkColors.error
        }
    Box(
        modifier =
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(grade, fontSize = 12.sp, fontWeight = FontWeight.W700, color = color)
    }
}

// ─── DNS guide ────────────────────────────────────────────────────────────────

@Composable
private fun DnsGuideView(
    c: LkTokens,
    onVoltar: () -> Unit,
) {
    var tabSelecionada by remember { mutableIntStateOf(0) }
    Column {
        TextButton(
            onClick = onVoltar,
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = c.textSecondary,
            )
            Spacer(Modifier.width(4.dp))
            Text("Voltar ao comparativo", fontSize = 12.sp, color = c.textSecondary)
        }
        Spacer(Modifier.height(12.dp))
        Text("Como alterar meu DNS", fontSize = 16.sp, fontWeight = FontWeight.W600, color = c.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text("Escolha onde prefere alterar:", fontSize = 12.sp, color = c.textSecondary)
        Spacer(Modifier.height(16.dp))

        val tabs = listOf("Dispositivo", "Roteador")
        TabRow(
            selectedTabIndex = tabSelecionada,
            containerColor = c.bgPrimary,
            contentColor = LkColors.accent,
            divider = { HorizontalDivider(color = c.border, thickness = 1.dp) },
        ) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = tabSelecionada == idx,
                    onClick = { tabSelecionada = idx },
                    text = {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = if (tabSelecionada == idx) FontWeight.W600 else FontWeight.W400,
                            color = if (tabSelecionada == idx) LkColors.accent else c.textSecondary,
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        if (tabSelecionada == 0) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Android · DNS Privado", fontSize = 13.sp, fontWeight = FontWeight.W600, color = c.textPrimary)
                DnsGuideStep(1, "Abra as Configurações do sistema", c)
                DnsGuideStep(2, "Vá em Rede e internet → DNS privado", c)
                DnsGuideStep(3, "Selecione \"Nome do host do DNS privado\"", c)
                DnsGuideStep(4, "Digite o hostname do servidor DNS desejado", c)
                DnsGuideStep(5, "Toque em Salvar", c)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Esta configuração afeta apenas este dispositivo.",
                    fontSize = 11.sp,
                    color = c.textTertiary,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Configurações do Roteador", fontSize = 13.sp, fontWeight = FontWeight.W600, color = c.textPrimary)
                DnsGuideStep(1, "Acesse o painel admin do roteador (geralmente 192.168.0.1 ou 192.168.1.1)", c)
                DnsGuideStep(2, "Faça login com as credenciais (veja na etiqueta do roteador)", c)
                DnsGuideStep(3, "Localize as configurações de Rede ou WAN", c)
                DnsGuideStep(4, "Encontre o campo DNS primário e DNS secundário", c)
                DnsGuideStep(5, "Insira os endereços do servidor DNS desejado", c)
                DnsGuideStep(6, "Salve e aguarde o roteador reiniciar", c)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Esta configuração afeta todos os dispositivos conectados à rede.",
                    fontSize = 11.sp,
                    color = c.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun DnsGuideStep(
    numero: Int,
    texto: String,
    c: LkTokens,
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(LkColors.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("$numero", fontSize = 11.sp, fontWeight = FontWeight.W600, color = LkColors.accent)
        }
        Spacer(Modifier.width(LkSpacing.sm))
        Text(texto, fontSize = 13.sp, color = c.textSecondary, modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FibraStatusOverlay(
    snapshotFibra: SnapshotFibra,
    onDismiss: () -> Unit,
) {
    val c = LocalLkTokens.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        scrimColor =
            androidx.compose.ui.graphics.Color.Black
                .copy(alpha = 0.32f),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LkSpacing.md, vertical = LkSpacing.lg)
                    .navigationBarsPadding(),
        ) {
            Text(
                "Status Fibra Óptica",
                fontSize = 18.sp,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
                modifier = Modifier.padding(bottom = LkSpacing.md),
            )

            when (snapshotFibra.estado) {
                EstadoFibra.conectando -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(LkSpacing.md))
                        Text("Conectando ao modem...", fontSize = 14.sp, color = c.textSecondary)
                    }
                }
                EstadoFibra.concluido -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        snapshotFibra.gpon?.let {
                            FibraStatusField("GPON Status", it.status, c)
                            it.rxPowerDbm?.let { rxPower ->
                                FibraStatusField("RX Power", "$rxPower dBm", c)
                            }
                        }
                        snapshotFibra.wan?.let {
                            FibraStatusField("IP Externo", it.externalIp ?: "N/A", c)
                        }
                        snapshotFibra.ppp?.let {
                            FibraStatusField("PPP Status", it.connectionStatus, c)
                        }
                        snapshotFibra.deviceInfo?.let {
                            FibraStatusField("Modelo", it.model ?: "N/A", c)
                        }
                    }
                }
                EstadoFibra.erro -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LkColors.error.copy(alpha = 0.1f))
                                .padding(LkSpacing.md),
                    ) {
                        Text(
                            "Erro de conexão",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W600,
                            color = LkColors.error,
                        )
                        snapshotFibra.erroMensagem?.let {
                            Spacer(Modifier.height(LkSpacing.sm))
                            Text(
                                it,
                                fontSize = 12.sp,
                                color = c.textSecondary,
                            )
                        }
                    }
                }
                EstadoFibra.idle -> {}
            }

            Spacer(Modifier.height(LkSpacing.lg))
            Button(
                onClick = onDismiss,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                Text("Fechar", fontSize = 14.sp, fontWeight = FontWeight.W600)
            }
        }
    }
}

@Composable
private fun FibraStatusField(
    label: String,
    value: String,
    c: LkTokens,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = LkSpacing.md)) {
        Text(
            label,
            fontSize = 12.sp,
            color = c.textSecondary,
            fontWeight = FontWeight.W500,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            fontSize = 14.sp,
            color = c.textPrimary,
            fontWeight = FontWeight.W500,
        )
    }
}
