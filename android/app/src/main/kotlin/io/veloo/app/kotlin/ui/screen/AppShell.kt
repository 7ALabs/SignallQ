package io.signallq.app.ui.screen

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.BuildConfig
import io.signallq.app.FeatureFlags
import io.signallq.app.R
import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.core.telephony.MovelSimSnapshot
import io.signallq.app.core.telephony.MovelSnapshot
import io.signallq.app.feature.diagnostico.EstadoDiagnostico
import io.signallq.app.feature.dns.SnapshotBenchmarkDns
import io.signallq.app.feature.fibra.SnapshotFibra
import io.signallq.app.feature.history.ResumoHistorico
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.ModoSpeedtest
import io.signallq.app.ui.FiltroConexaoHistorico
import io.signallq.app.ui.GatewayInfo
import io.signallq.app.ui.HistoryPoint
import io.signallq.app.ui.IspInfo
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.state.UiState
import kotlinx.coroutines.delay

private enum class Overlay {
    Laudo,
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
    speedtest: AppShellSpeedtestState,
    wifi: AppShellWifiState,
    diagnostico: AppShellDiagnosticoState,
    signallQ: AppShellSignallQState,
    chatDiag: AppShellChatDiagState,
    snapshotDns: SnapshotBenchmarkDns,
    history: List<HistoryPoint>,
    localIp: UiState<String>,
    publicIp: UiState<String>,
    ispInfo: UiState<IspInfo>,
    gateways: List<GatewayInfo>,
    deviceName: String,
    dnsResolverIp: String?,
    historico: List<MedicaoEntity>,
    blocoUptime: List<io.signallq.app.feature.history.BlocoUptime> = emptyList(),
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
    onDispararBenchmarkDns: () -> Unit,
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
    movelSnapshot: MovelSnapshot?,
    simsAtivos: List<MovelSimSnapshot> = emptyList(),
    temPermissaoTelefonia: Boolean = false,
    onSolicitarPermissaoTelefonia: () -> Unit = {},
    temPermissaoLocalizacao: Boolean = true,
    localizacaoBloqueadaPermanentemente: Boolean = false,
    onSolicitarPermissaoLocalizacao: () -> Unit = {},
    // Issue #85 — Minha Conexão
    velocidadeContratadaDownMbps: Int = 0,
    velocidadeContratadaUpMbps: Int = 0,
    onSalvarVelocidadeContratada: (downMbps: Int, upMbps: Int) -> Unit = { _, _ -> },
    onSalvarConexaoDadosCompletos: (
        operadora: String,
        estadoUf: String,
        cidadeNome: String,
        downMbps: Int,
        upMbps: Int,
    ) -> Unit = { _, _, _, _, _ -> },
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
    onScreenView: (screenName: String) -> Unit = {},
) {
    // Desempacota os grupos de estado para variaveis locais — mantém compatibilidade com
    // o corpo interno sem precisar propagar o prefixo `speedtest.x` por toda a funcao.
    val snapshotSpeedtest = speedtest.snapshotSpeedtest
    val speedtestPendenteModoMovel = speedtest.speedtestPendenteModoMovel
    val speedtestPermiteHeavyMovel = speedtest.speedtestPermiteHeavyMovel
    val speedtestMbConsumidosMes = speedtest.speedtestMbConsumidosMes
    val onNovoTeste = speedtest.onNovoTeste
    val onCancelarTeste = speedtest.onCancelarTeste
    val onConfirmarSpeedtestMovel = speedtest.onConfirmarSpeedtestMovel
    val onCancelarSpeedtestMovel = speedtest.onCancelarSpeedtestMovel
    val onSetSpeedtestPermiteHeavyMovel = speedtest.onSetSpeedtestPermiteHeavyMovel

    val snapshotWifi = wifi.snapshotWifi
    val connectedNetwork = wifi.connectedNetwork
    val snapshotDevices = wifi.snapshotDevices
    val apelidos = wifi.apelidos
    val onRefreshDispositivos = wifi.onRefreshDispositivos
    val onRefreshSinal = wifi.onRefreshSinal
    val onSalvarApelido = wifi.onSalvarApelido

    val snapshotDiagnostico = diagnostico.snapshotDiagnostico
    val onIniciarDiagnostico = diagnostico.onIniciarDiagnostico
    val analisadorState = diagnostico.analisadorState
    val onAnalisarProblema = diagnostico.onAnalisarProblema
    val onResetarAnalisador = diagnostico.onResetarAnalisador

    val operadoraMovel = signallQ.operadoraMovel
    val onVerificarGemma = signallQ.onVerificarGemma

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
    var showPerfilSheet by remember { mutableStateOf(false) }
    var testeAtivo by remember { mutableStateOf(false) }
    var mostrarConcluido by remember { mutableStateOf(false) }
    val primeiraHistoria = remember(historico) { historico.firstOrNull() }
    val tabScreenNames = listOf("home", "speedtest", "sinal_wifi", "historico", "ajustes")

    // NAV-D: verifica IA ao entrar na tab Velocidade (índice 1)
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) onVerificarGemma()
        tabScreenNames.getOrNull(selectedTab)?.let { onScreenView(it) }
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

    // Abre LaudoScreen automaticamente ao concluir qualquer diagnóstico.
    LaunchedEffect(snapshotDiagnostico.estado) {
        if (snapshotDiagnostico.estado == EstadoDiagnostico.concluido) {
            if (Overlay.Laudo !in overlayStack) overlayStack.add(Overlay.Laudo)
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

    // #374: tela de erro do speedtest (overlay VelocidadeScreen) não tinha BackHandler
    // próprio — o back físico do sistema saía direto do app em vez de descartar o erro.
    BackHandler(enabled = snapshotSpeedtest.estado == EstadoExecucaoSpeedtest.erro) {
        onCancelarTeste()
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
                                if (Overlay.Laudo !in overlayStack) {
                                    overlayStack.add(Overlay.Laudo)
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
                            onAbrirPing = { if (Overlay.Ping !in overlayStack) overlayStack.add(Overlay.Ping) },
                            onVerResultado = {
                                if (Overlay.ResultadoVelocidade !in
                                    overlayStack
                                ) {
                                    overlayStack.add(Overlay.ResultadoVelocidade)
                                }
                            },
                            onAbrirHistorico = { selectedTab = 3 },
                            onAbrirAjustes = { selectedTab = 4 },
                            nomeUsuario = nomeUsuario,
                            fotoUri = fotoUriUsuario,
                            onAbrirPerfil = { showPerfilSheet = true },
                            planoInternet = planoInternet,
                            speedtestPendenteModoMovel = speedtestPendenteModoMovel,
                            onConfirmarSpeedtestMovel = onConfirmarSpeedtestMovel,
                            onCancelarSpeedtestMovel = onCancelarSpeedtestMovel,
                            movelSnapshot = movelSnapshot,
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
                            snapshotDispositivos = snapshotDevices,
                            apelidos = apelidos,
                            onSalvarApelido = onSalvarApelido,
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
                        contentDescription = stringResource(R.string.appshell_cd_concluido),
                        tint = LkColors.success,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.appshell_concluido),
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
                    ispInfo = ispInfoData,
                    operadoraMovel = operadoraMovel,
                    anatelBannerDismissed = anatelBannerDismissed,
                    onDismissAnatelBanner = onDispensarBannerAnatel,
                    analisadorState = analisadorState,
                    onAnalisarProblema = onAnalisarProblema,
                    onResetarAnalisador = onResetarAnalisador,
                )
            }
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
        title = { Text(stringResource(R.string.appshell_sem_wifi), fontWeight = FontWeight.W600) },
        text = {
            Text(
                "Você está usando dados móveis. Fazer um teste de velocidade pode consumir uma quantidade significativa do seu plano de dados.\n\nDeseja continuar mesmo assim?",
                fontSize = 14.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onContinuar) {
                Text(stringResource(R.string.appshell_continuar_mesmo_assim), color = LkColors.warning)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(stringResource(R.string.global_btn_cancelar)) }
        },
    )
}
