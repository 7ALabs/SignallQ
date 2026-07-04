package io.signallq.app

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.signallq.app.core.network.AnalyticsHelper
import io.signallq.app.core.network.AnalyticsTracker
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.feature.devices.DevicesViewModel
import io.signallq.app.feature.speedtest.SpeedtestViewModel
import io.signallq.app.ui.SignallQTheme
import io.signallq.app.ui.component.LgpdConsentDialog
import io.signallq.app.ui.screen.AppShell
import io.signallq.app.ui.screen.OnboardingScreen
import io.signallq.app.ui.viewmodel.ChatDiagnosticoIaViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    private val viewModel: MainViewModel by viewModels()
    private val chatDiagViewModel: ChatDiagnosticoIaViewModel by viewModels()

    // ViewModels por feature — extraidos do MainViewModel (Passo 6 do plano de migracao).
    // Fase atual: instanciados e conectados; o MainViewModel ainda contem logica legada
    // para compatibilidade de build. Cleanup do MainViewModel em PR subsequente.
    // NOTA: DiagnosticoViewModel removido — era codigo morto (instanciado, nunca referenciado).
    // Callbacks de diagnostico continuam delegando para MainViewModel intencionalmente
    // por compatibilidade legada; migracao completa em PR subsequente.
    private val devicesViewModel: DevicesViewModel by viewModels()
    private val speedtestViewModel: SpeedtestViewModel by viewModels()

    private val solicitacaoPermissoes =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultados ->
            aguardandoRespostaPermissoes = false
            val todasConcedidas = resultados.values.all { it }
            if (todasConcedidas) viewModel.iniciarRotinasNaoSpeedtest()
        }

    /**
     * Solicitacao LAZY de READ_PHONE_STATE — disparada apenas quando o usuario
     * roda diagnostico em rede movel pela primeira vez. Justificativa:
     * "Para analise de sinal 4G/5G". Se negar, snapshot movel fica null e a
     * IA recebe `connectionType: mobile` sem o bloco `movel` (gracioso).
     */
    private val solicitacaoPermissaoTelefonia =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
            temPermissaoTelefonia = concedida
            if (concedida) viewModel.iniciarMonitorTelefoniaSeMovel()
        }

    private val solicitacaoPermissaoLocalizacao =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
            temPermissaoLocalizacao = concedida
            if (concedida) viewModel.iniciarRotinasNaoSpeedtest()
        }

    private val solicitacaoPermissaoNotificacao =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Worker agenda mesmo se permissao negada — notificacoes ficam silenciosas
            viewModel.atualizarMonitoramento(true)
        }

    private var jaSolicitouTelefoniaNestaSessao = false
    private var aguardandoRespostaPermissoes = false

    private var temPermissaoTelefonia by mutableStateOf(false)
    private var temPermissaoLocalizacao by mutableStateOf(false)

    // #155/9.3: permissão negada permanentemente (shouldShowRequestPermissionRationale = false E não concedida)
    private var localizacaoBloqueadaPermanentemente by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        analyticsTracker.registrarSessionStart()
        registrarBatterySnapshotInicial()
        val estadoConexaoInicial = viewModel.monitorRede.snapshotFlow.value.estadoConexao
        analyticsHelper.registrarAppAberto(tipoConexao = estadoConexaoInicial.paraTipoConexaoAnalytics())

        // Conecta o SpeedtestViewModel ao MainViewModel: apos cada speedtest, dispara
        // as rotinas nao-speedtest (scan de dispositivos, diagnostico, etc.).
        speedtestViewModel.onSpeedtestConcluido = {
            viewModel.iniciarRotinasNaoSpeedtest()
            analyticsTracker.registrarFeatureUsada("speedtest")
        }

        // Assina o SharedFlow de dispositivos novos do DevicesViewModel e exibe notificacao.
        // A notificacao ocorre no :app (que tem SignallQNotificationHelper) para respeitar
        // a lei de dependencias: featureDevices nao pode depender de :app.
        lifecycleScope.launch {
            devicesViewModel.dispositivosNovos.collect { identificador ->
                io.signallq.app.notificacao.SignallQNotificationHelper.notificarDispositivoNovo(
                    this@MainActivity,
                    identificador,
                )
            }
        }

        setContent {
            // --- Snapshots de features (ciclos de vida independentes — NAO combinar) ---
            val snapshotRede =
                viewModel.monitorRede.snapshotFlow
                    .collectAsStateWithLifecycle()
                    .value
            val snapshotSpeedtest =
                viewModel.executorSpeedtest.snapshotFlow
                    .collectAsStateWithLifecycle()
                    .value
            val snapshotDns =
                viewModel.benchmarkDns.snapshotFlow
                    .collectAsStateWithLifecycle()
                    .value
            val snapshotDevices =
                viewModel.scannerDispositivos.snapshotFlow
                    .collectAsStateWithLifecycle()
                    .value
            val snapshotWifi =
                viewModel.scannerRedesWifi.snapshotFlow
                    .collectAsStateWithLifecycle()
                    .value
            val snapshotFibra =
                viewModel.executorFibra.snapshotFlow
                    .collectAsStateWithLifecycle()
                    .value

            // --- Estado de rede e ISP (atualizam em momentos distintos — NAO combinar) ---
            val localIpUiState = viewModel.localIp.collectAsStateWithLifecycle().value
            val publicIpUiState = viewModel.publicIp.collectAsStateWithLifecycle().value
            val ispInfoUiState = viewModel.ispInfo.collectAsStateWithLifecycle().value
            val gateways = viewModel.gateways.collectAsStateWithLifecycle().value
            val localizacaoServidorUiState = viewModel.localizacaoServidor.collectAsStateWithLifecycle().value

            // --- Historico ---
            val history = viewModel.history.collectAsStateWithLifecycle().value
            val historico = viewModel.historico.collectAsStateWithLifecycle().value
            val blocoUptime = viewModel.blocoUptime.collectAsStateWithLifecycle().value
            val narrativaUptime = viewModel.narrativaUptime.collectAsStateWithLifecycle().value
            val resumoHistorico = viewModel.resumoHistorico.collectAsStateWithLifecycle().value
            // #95 — Filtros do Historico
            val historicoFiltrado = viewModel.historicoFiltrado.collectAsStateWithLifecycle().value
            val filtroConexaoHistorico = viewModel.filtroConexaoHistorico.collectAsStateWithLifecycle().value
            val filtroOperadoraHistorico = viewModel.filtroOperadoraHistorico.collectAsStateWithLifecycle().value
            val operadorasDisponiveisHistorico = viewModel.operadorasDisponiveisHistorico.collectAsStateWithLifecycle().value

            // --- Preferencias combinadas (1 subscricao por grupo) ---
            val preferenciasModem = viewModel.preferenciasModem.collectAsStateWithLifecycle().value
            val modemHost = preferenciasModem.host
            val modemUsername = preferenciasModem.username
            val modemPassword = preferenciasModem.password
            val modemPermanecerConectado = preferenciasModem.permanecerConectado

            val preferenciasNotificacao = viewModel.preferenciasNotificacao.collectAsStateWithLifecycle().value
            val notificacaoLatenciaAtiva = preferenciasNotificacao.latenciaAtiva
            val notificacaoDnsAtiva = preferenciasNotificacao.dnsAtiva
            val notificacaoRssiAtiva = preferenciasNotificacao.rssiAtiva
            val notificacaoSemInternetAtiva = preferenciasNotificacao.semInternetAtiva

            val preferenciasUi = viewModel.preferenciasUi.collectAsStateWithLifecycle().value
            val temaSelecionado = preferenciasUi.temaSelecionado
            val analiseAvancada = preferenciasUi.analiseAvancada

            val perfilProvedor = viewModel.preferenciasPerfilProvedor.collectAsStateWithLifecycle().value
            val nomeUsuario = perfilProvedor.nomeUsuario
            val fotoUriUsuario = perfilProvedor.fotoUriUsuario
            val operadora = perfilProvedor.operadora
            val planoInternet = perfilProvedor.planoInternet
            val regiao = perfilProvedor.regiao
            val estadoUf = perfilProvedor.estadoUf
            val cidadeNome = perfilProvedor.cidadeNome
            val ispConfirmado = perfilProvedor.ispConfirmado
            val limiteAlertaMbps = perfilProvedor.limiteAlertaMbps

            val speedtestMovel = viewModel.preferenciasSpeedtestMovel.collectAsStateWithLifecycle().value
            val speedtestPermiteHeavyMovel = speedtestMovel.permiteHeavy
            val speedtestMbConsumidosMes = speedtestMovel.mbConsumidosMes

            // --- Flows individuais com distinctUntilChanged no ViewModel ---
            val monitoramentoAtivo = viewModel.monitoramentoAtivo.collectAsStateWithLifecycle().value

            // --- Outros flows de estado ---
            val speedtestPendenteModoMovel =
                viewModel.speedtestPendenteModoMovel
                    .collectAsStateWithLifecycle()
                    .value
            val apelidos = viewModel.apelidos.collectAsStateWithLifecycle().value
            val snapshotDiagnostico =
                viewModel.diagnosticOrchestrator.snapshotFlow
                    .collectAsStateWithLifecycle()
                    .value
            val signallQUiState = viewModel.signallQUiStateFlow.collectAsStateWithLifecycle().value
            val movelSnapshot = viewModel.movelSnapshot.collectAsStateWithLifecycle().value
            val simsAtivos = viewModel.simsAtivos.collectAsStateWithLifecycle().value
            val gemmaAvailable = viewModel.gemmaAvailable.collectAsStateWithLifecycle().value
            val onboardingConcluido = viewModel.onboardingConcluido.collectAsStateWithLifecycle().value
            val consentimentoLgpd = viewModel.consentimentoLgpd.collectAsStateWithLifecycle().value
            val diagChatHistorico by viewModel.diagChatHistorico.collectAsStateWithLifecycle()
            val diagChatCarregando by viewModel.diagChatCarregando.collectAsStateWithLifecycle()
            val analisadorState by viewModel.analisadorState.collectAsStateWithLifecycle()
            // #82 — Banner Anatel dismissível
            val anatelBannerDismissed = viewModel.anatelBannerDismissed.collectAsStateWithLifecycle().value
            // Chat Diagnóstico IA
            val chatDiagUiState by chatDiagViewModel.uiState.collectAsStateWithLifecycle()

            val gatewayIpDetectado = gateways.firstOrNull()?.ip
            val darkTheme =
                when (temaSelecionado) {
                    "claro" -> false
                    "escuro" -> true
                    else -> isSystemInDarkTheme()
                }

            SideEffect {
                enableEdgeToEdge(
                    statusBarStyle =
                        if (darkTheme) {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT,
                            )
                        },
                )
            }

            val connectedBssid = snapshotRede.wifiLinkSnapshot?.bssid
            val connectedNetwork =
                if (connectedBssid != null) {
                    snapshotWifi.redes.find { it.bssid == connectedBssid }
                } else {
                    null
                }

            SignallQTheme(darkTheme = darkTheme) {
                if (!onboardingConcluido) {
                    OnboardingScreen(
                        onConcluir = { viewModel.marcarOnboardingConcluido() },
                        // #128: solicitar permissões no slide 3 (localização + dispositivos próximos)
                        onSolicitarPermissaoLocalizacao = { solicitarPermissaoLocalizacaoContextual() },
                        onSolicitarPermissaoDispositivosProximos = { solicitarPermissaoDispositivosProximosContextual() },
                    )
                } else if (consentimentoLgpd == null) {
                    LgpdConsentDialog(
                        onAceitar = { viewModel.definirConsentimentoLgpd(true) },
                        onRecusar = { viewModel.definirConsentimentoLgpd(false) },
                    )
                } else {
                    AppShell(
                        snapshotRede = snapshotRede,
                        speedtest =
                            io.signallq.app.ui.screen.AppShellSpeedtestState(
                                snapshotSpeedtest = snapshotSpeedtest,
                                speedtestPendenteModoMovel = speedtestPendenteModoMovel,
                                speedtestPermiteHeavyMovel = speedtestPermiteHeavyMovel,
                                speedtestMbConsumidosMes = speedtestMbConsumidosMes,
                                onNovoTeste = { modo -> viewModel.reiniciarSuite(modo) },
                                onCancelarTeste = { viewModel.executorSpeedtest.cancelar() },
                                onConfirmarSpeedtestMovel = { viewModel.confirmarSpeedtestEmMovel() },
                                onCancelarSpeedtestMovel = { viewModel.cancelarSpeedtestMovel() },
                                onSetSpeedtestPermiteHeavyMovel = { valor -> viewModel.setSpeedtestPermiteHeavyMovel(valor) },
                            ),
                        wifi =
                            io.signallq.app.ui.screen.AppShellWifiState(
                                snapshotWifi = snapshotWifi,
                                connectedNetwork = connectedNetwork,
                                snapshotDevices = snapshotDevices,
                                apelidos = apelidos,
                                onRefreshDispositivos = { viewModel.refreshDispositivos() },
                                onRefreshSinal = {
                                    viewModel.refreshSinal()
                                    analyticsTracker.registrarFeatureUsada("wifi")
                                },
                                onSalvarApelido = { mac, apelido -> viewModel.salvarApelido(mac, apelido) },
                            ),
                        diagnostico =
                            io.signallq.app.ui.screen.AppShellDiagnosticoState(
                                snapshotDiagnostico = snapshotDiagnostico,
                                onIniciarDiagnostico = {
                                    solicitarPermissaoTelefoniaSeNecessario()
                                    viewModel.iniciarDiagnostico()
                                    analyticsTracker.registrarFeatureUsada("diagnostico")
                                },
                                diagChatHistorico = diagChatHistorico,
                                diagChatCarregando = diagChatCarregando,
                                onEnviarPerguntaDiagnostico = { pergunta -> viewModel.enviarPerguntaDiagnostico(pergunta) },
                                onLimparDiagChat = { viewModel.limparDiagChat() },
                                analisadorState = analisadorState,
                                onAnalisarProblema = { problema -> viewModel.analisarProblema(problema) },
                                onResetarAnalisador = { viewModel.resetarAnalisador() },
                            ),
                        signallQ =
                            io.signallq.app.ui.screen.AppShellSignallQState(
                                signallQUiState = signallQUiState,
                                gemmaAvailable = gemmaAvailable,
                                operadoraMovel =
                                    simsAtivos.firstOrNull { it.isDefaultData }?.operadora
                                        ?: simsAtivos.firstOrNull()?.operadora,
                                onIniciarSignallQ = { foco ->
                                    solicitarPermissaoTelefoniaSeNecessario()
                                    viewModel.iniciarSignallQ(foco)
                                },
                                onResetSignallQ = { viewModel.resetSignallQ() },
                                onSelecionarChip = { chip -> viewModel.selecionarChipSignallQ(chip) },
                                onResponderPergunta = { opcao -> viewModel.responderPerguntaSignallQ(opcao) },
                                onEnviarMensagemTexto = { texto -> viewModel.enviarMensagemTextoSignallQ(texto) },
                                onVerificarGemma = { viewModel.verificarDisponibilidadeGemma() },
                                onIniciarSignallQComResultado = { resultado, foco ->
                                    solicitarPermissaoTelefoniaSeNecessario()
                                    viewModel.iniciarSignallQComResultado(resultado, foco)
                                },
                            ),
                        chatDiag =
                            io.signallq.app.ui.screen.AppShellChatDiagState(
                                chatDiagUiState = chatDiagUiState,
                                onEnviarMensagem = chatDiagViewModel::onEnviarMensagem,
                                onAtualizarDraft = chatDiagViewModel::onAtualizarDraft,
                                onEscolherOpcao = chatDiagViewModel::onEscolherOpcao,
                                onAbrirSessao = chatDiagViewModel::onAbrirSessao,
                                onApagarSessao = chatDiagViewModel::onApagarSessao,
                                onRenomearSessao = chatDiagViewModel::onRenomearSessao,
                                onNovaSessao = chatDiagViewModel::onNovaSessao,
                                onToggleDrawer = chatDiagViewModel::onToggleDrawer,
                                onCancelarAcaoAtual = chatDiagViewModel::onCancelarAcaoAtual,
                            ),
                        snapshotDns = snapshotDns,
                        history = history,
                        localIp = localIpUiState,
                        publicIp = publicIpUiState,
                        ispInfo = ispInfoUiState,
                        gateways = gateways,
                        deviceName = Build.MODEL,
                        nomeUsuario = nomeUsuario,
                        fotoUriUsuario = fotoUriUsuario,
                        operadora = operadora,
                        planoInternet = planoInternet,
                        regiao = regiao,
                        estadoUf = estadoUf,
                        cidadeNome = cidadeNome,
                        ispConfirmado = ispConfirmado,
                        limiteAlertaMbps = limiteAlertaMbps,
                        dnsResolverIp = snapshotRede.dnsServidores.firstOrNull(),
                        historico = historico,
                        blocoUptime = blocoUptime,
                        narrativaUptime = narrativaUptime,
                        resumoHistorico = resumoHistorico,
                        snapshotFibra = snapshotFibra,
                        modemHost = modemHost,
                        modemUsername = modemUsername,
                        modemPassword = modemPassword,
                        modemPermanecerConectado = modemPermanecerConectado,
                        gatewayIpDetectado = gatewayIpDetectado,
                        localizacaoServidor = localizacaoServidorUiState,
                        onDispararBenchmarkDns = {
                            viewModel.dispararBenchmarkDns()
                            analyticsTracker.registrarFeatureUsada("dns")
                        },
                        onReconectarFibra = { host, user, pass ->
                            viewModel.reconectarFibra(host, user, pass)
                            analyticsTracker.registrarFeatureUsada("fibra")
                        },
                        onSalvarConfiguracaoModem = { host, user, pass, perm ->
                            viewModel.salvarConfiguracaoModem(host, user, pass, perm)
                        },
                        temaSelecionado = temaSelecionado,
                        analiseAvancada = analiseAvancada,
                        onDefinirTemaSelecionado = { tema -> viewModel.definirTemaSelecionado(tema) },
                        onDefinirAnaliseAvancada = { ativa -> viewModel.definirAnaliseAvancada(ativa) },
                        onLimparHistorico = { viewModel.limparHistorico() },
                        onApagarDadosLocais = { viewModel.apagarDadosLocais() },
                        onResetarApp = { viewModel.resetarApp() },
                        monitoramentoAtivo = monitoramentoAtivo,
                        onAtivarMonitoramento = { ativo ->
                            if (ativo) {
                                solicitarPermissaoNotificacaoSeNecessario { viewModel.atualizarMonitoramento(true) }
                            } else {
                                viewModel.atualizarMonitoramento(false)
                            }
                        },
                        notificacaoLatenciaAtiva = notificacaoLatenciaAtiva,
                        notificacaoDnsAtiva = notificacaoDnsAtiva,
                        notificacaoRssiAtiva = notificacaoRssiAtiva,
                        notificacaoSemInternetAtiva = notificacaoSemInternetAtiva,
                        onDefinirNotificacaoLatenciaAtiva = { viewModel.definirNotificacaoLatenciaAtiva(it) },
                        onDefinirNotificacaoDnsAtiva = { viewModel.definirNotificacaoDnsAtiva(it) },
                        onDefinirNotificacaoRssiAtiva = { viewModel.definirNotificacaoRssiAtiva(it) },
                        onDefinirNotificacaoSemInternetAtiva = { viewModel.definirNotificacaoSemInternetAtiva(it) },
                        onSalvarPerfil = { nome, fotoUri -> viewModel.salvarPerfil(nome, fotoUri) },
                        onSalvarDadosProvedor = { op, plano, reg -> viewModel.salvarDadosProvedor(op, plano, reg) },
                        onSalvarEstadoCidade = { uf, cidade -> viewModel.salvarEstadoCidade(uf, cidade) },
                        onConfirmarIsp = { op -> viewModel.confirmarIspDetectado(op) },
                        onDispensarBannerIsp = { viewModel.dispensarBannerIsp() },
                        onSalvarLimiteAlerta = { limite -> viewModel.salvarLimiteAlerta(limite) },
                        movelSnapshot = movelSnapshot,
                        simsAtivos = simsAtivos,
                        temPermissaoTelefonia = temPermissaoTelefonia,
                        onSolicitarPermissaoTelefonia = { solicitarPermissaoTelefoniaContextual() },
                        temPermissaoLocalizacao = temPermissaoLocalizacao,
                        localizacaoBloqueadaPermanentemente = localizacaoBloqueadaPermanentemente,
                        onSolicitarPermissaoLocalizacao = { solicitarPermissaoLocalizacaoContextual() },
                        velocidadeContratadaDownMbps = perfilProvedor.velocidadeContratadaDownMbps,
                        velocidadeContratadaUpMbps = perfilProvedor.velocidadeContratadaUpMbps,
                        onSalvarVelocidadeContratada = { down, up ->
                            viewModel.salvarVelocidadeContratada(down, up)
                        },
                        onSalvarConexaoDadosCompletos = { op, uf, cidade, down, up ->
                            viewModel.salvarDadosProvedor(op, "", "")
                            viewModel.salvarEstadoCidade(uf, cidade)
                            viewModel.salvarVelocidadeContratada(down, up)
                        },
                        anatelBannerDismissed = anatelBannerDismissed,
                        onDispensarBannerAnatel = { viewModel.dispensarBannerAnatel() },
                        historicoFiltrado = historicoFiltrado,
                        filtroConexaoHistorico = filtroConexaoHistorico,
                        onFiltroConexaoHistoricoChange = {
                            viewModel.setFiltroConexaoHistorico(it)
                            analyticsTracker.registrarFeatureUsada("historico")
                        },
                        filtroOperadoraHistorico = filtroOperadoraHistorico,
                        onFiltroOperadoraHistoricoChange = {
                            viewModel.setFiltroOperadoraHistorico(it)
                            analyticsTracker.registrarFeatureUsada("historico")
                        },
                        operadorasDisponiveisHistorico = operadorasDisponiveisHistorico,
                        onScreenView = { screenName -> analyticsTracker.registrarScreenView(screenName) },
                    )
                } // else onboardingConcluido
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.iniciarMonitorRede()
        verificarEPedirPermissoes()
    }

    override fun onResume() {
        super.onResume()
        temPermissaoTelefonia = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE,
        ) == PackageManager.PERMISSION_GRANTED
        temPermissaoLocalizacao = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        // #155/9.3: bloqueada = não concedida E não pode mais mostrar rationale
        localizacaoBloqueadaPermanentemente = !temPermissaoLocalizacao &&
            !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
        val emWifi = viewModel.monitorRede.snapshotFlow.value.estadoConexao == EstadoConexao.wifi
        // Usa DevicesViewModel para verificar novos dispositivos (etapa A do refactor).
        // O MainViewModel.verificarDispositivosNovos() ainda existe mas nao e mais chamado aqui.
        if (emWifi) devicesViewModel.verificarDispositivosNovos()
    }

    override fun onStop() {
        viewModel.encerrarMonitorRede()
        super.onStop()
    }

    override fun onDestroy() {
        speedtestViewModel.onSpeedtestConcluido = null
        super.onDestroy()
    }

    private fun registrarBatterySnapshotInicial() {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return
        val levelPercent = (level * 100 / scale)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging =
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        analyticsTracker.registrarBatterySnapshot(levelPercent, charging)
    }

    private fun verificarEPedirPermissoes() {
        if (aguardandoRespostaPermissoes) return
        val pendentes = viewModel.gerenciadorPermissoes.listarPermissoesPendentes()
        if (pendentes.isNotEmpty()) {
            aguardandoRespostaPermissoes = true
            solicitacaoPermissoes.launch(pendentes.toTypedArray())
        } else {
            viewModel.iniciarRotinasNaoSpeedtest()
        }
    }

    /**
     * Lazy: so solicita READ_PHONE_STATE quando o usuario esta em rede movel
     * E ainda nao tentamos pedir nesta sessao. Em Wi-Fi/Ethernet, nao pede.
     * Se ja concedida, apenas garante que o monitor esta iniciado.
     */
    private fun solicitarPermissaoNotificacaoSeNecessario(onProsseguir: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val concedida =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            if (!concedida) {
                solicitacaoPermissaoNotificacao.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        onProsseguir()
    }

    private fun solicitarPermissaoTelefoniaSeNecessario() {
        val emRedeMovel = viewModel.monitorRede.snapshotFlow.value.estadoConexao == EstadoConexao.movel
        if (!emRedeMovel) return
        val concedida =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE,
            ) == PackageManager.PERMISSION_GRANTED
        if (concedida) {
            temPermissaoTelefonia = true
            viewModel.iniciarMonitorTelefoniaSeMovel()
            return
        }
        if (jaSolicitouTelefoniaNestaSessao) return
        jaSolicitouTelefoniaNestaSessao = true
        solicitacaoPermissaoTelefonia.launch(Manifest.permission.READ_PHONE_STATE)
    }

    private fun solicitarPermissaoTelefoniaContextual() {
        val concedida =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE,
            ) == PackageManager.PERMISSION_GRANTED
        if (concedida) {
            temPermissaoTelefonia = true
            viewModel.iniciarMonitorTelefoniaSeMovel()
            return
        }
        solicitacaoPermissaoTelefonia.launch(Manifest.permission.READ_PHONE_STATE)
    }

    // #128: solicitar permissão de dispositivos próximos (NEARBY_WIFI_DEVICES) no onboarding
    private fun solicitarPermissaoDispositivosProximosContextual() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val concedida =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.NEARBY_WIFI_DEVICES,
                ) == PackageManager.PERMISSION_GRANTED
            if (!concedida) {
                // Usar launcher genérico (múltiplas permissões) para não corromper o
                // launcher de localização (solicitacaoPermissaoLocalizacao)
                solicitacaoPermissoes.launch(arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES))
            }
        }
        // Abaixo do API 33, NEARBY_WIFI_DEVICES não existe — no-op; localização cobre o caso
    }

    // Analytics (SIG-155): EstadoConexao.movel vira "mobile" no schema do funil.
    // Os demais nomes (wifi/ethernet/desconectado/desconhecido) ja batem com o schema.
    private fun EstadoConexao.paraTipoConexaoAnalytics(): String =
        if (this == EstadoConexao.movel) "mobile" else name

    private fun solicitarPermissaoLocalizacaoContextual() {
        val concedida =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        if (concedida) {
            temPermissaoLocalizacao = true
            return
        }
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            solicitacaoPermissaoLocalizacao.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            startActivity(
                android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", packageName, null)
                },
            )
        }
    }
}
