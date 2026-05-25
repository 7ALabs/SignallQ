package io.linka.app.kotlin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.linka.app.kotlin.core.network.EstadoConexao
import io.linka.app.kotlin.ui.LinkaTheme
import io.linka.app.kotlin.ui.screen.AppShell
import io.linka.app.kotlin.ui.screen.OnboardingScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val solicitacaoPermissoes =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            verificarEPedirPermissoes()
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

    private var temPermissaoTelefonia by mutableStateOf(false)
    private var temPermissaoLocalizacao by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
            val orbitUiState = viewModel.orbitUiStateFlow.collectAsStateWithLifecycle().value
            val movelSnapshot = viewModel.movelSnapshot.collectAsStateWithLifecycle().value
            val gemmaAvailable = viewModel.gemmaAvailable.collectAsStateWithLifecycle().value
            val onboardingConcluido = viewModel.onboardingConcluido.collectAsStateWithLifecycle().value

            val gatewayIpDetectado = gateways.firstOrNull()?.ip
            val darkTheme =
                when (temaSelecionado) {
                    "claro" -> false
                    "escuro" -> true
                    else -> isSystemInDarkTheme()
                }

            val connectedBssid = snapshotRede.wifiLinkSnapshot?.bssid
            val connectedNetwork =
                if (connectedBssid != null) {
                    snapshotWifi.redes.find { it.bssid == connectedBssid }
                } else {
                    null
                }

            LinkaTheme(darkTheme = darkTheme) {
                if (!onboardingConcluido) {
                    OnboardingScreen(
                        onConcluir = { viewModel.marcarOnboardingConcluido() },
                    )
                } else {
                    AppShell(
                        snapshotRede = snapshotRede,
                        snapshotSpeedtest = snapshotSpeedtest,
                        snapshotDns = snapshotDns,
                        snapshotDevices = snapshotDevices,
                        snapshotDiagnostico = snapshotDiagnostico,
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
                        connectedNetwork = connectedNetwork,
                        snapshotWifi = snapshotWifi,
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
                        onNovoTeste = { modo -> viewModel.reiniciarSuite(modo) },
                        onCancelarTeste = { viewModel.executorSpeedtest.cancelar() },
                        onDispararBenchmarkDns = { viewModel.dispararBenchmarkDns() },
                        onRefreshDispositivos = { viewModel.refreshDispositivos() },
                        apelidos = apelidos,
                        onSalvarApelido = { mac, apelido -> viewModel.salvarApelido(mac, apelido) },
                        onRefreshSinal = { viewModel.refreshSinal() },
                        onReconectarFibra = { host, user, pass -> viewModel.reconectarFibra(host, user, pass) },
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
                        speedtestPendenteModoMovel = speedtestPendenteModoMovel,
                        onConfirmarSpeedtestMovel = { viewModel.confirmarSpeedtestEmMovel() },
                        onCancelarSpeedtestMovel = { viewModel.cancelarSpeedtestMovel() },
                        speedtestPermiteHeavyMovel = speedtestPermiteHeavyMovel,
                        onSetSpeedtestPermiteHeavyMovel = { valor -> viewModel.setSpeedtestPermiteHeavyMovel(valor) },
                        speedtestMbConsumidosMes = speedtestMbConsumidosMes,
                        onIniciarDiagnostico = {
                            solicitarPermissaoTelefoniaSeNecessario()
                            viewModel.iniciarDiagnostico()
                        },
                        temPermissaoTelefonia = temPermissaoTelefonia,
                        onSolicitarPermissaoTelefonia = { solicitarPermissaoTelefoniaContextual() },
                        temPermissaoLocalizacao = temPermissaoLocalizacao,
                        onSolicitarPermissaoLocalizacao = { solicitarPermissaoLocalizacaoContextual() },
                        orbitUiState = orbitUiState,
                        movelSnapshot = movelSnapshot,
                        onIniciarOrbit = { foco ->
                            solicitarPermissaoTelefoniaSeNecessario()
                            viewModel.iniciarOrbit(foco)
                        },
                        onResetOrbit = { viewModel.resetOrbit() },
                        onSelecionarChip = { chip -> viewModel.selecionarChipOrbit(chip) },
                        onResponderPergunta = { opcao -> viewModel.responderPerguntaOrbit(opcao) },
                        gemmaAvailable = gemmaAvailable,
                        onVerificarGemma = { viewModel.verificarDisponibilidadeGemma() },
                        onIniciarOrbitComResultado = { resultado, foco ->
                            solicitarPermissaoTelefoniaSeNecessario()
                            viewModel.iniciarOrbitComResultado(resultado, foco)
                        },
                        onEnviarMensagemTexto = { texto -> viewModel.enviarMensagemTextoOrbit(texto) },
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
        val emWifi = viewModel.monitorRede.snapshotFlow.value.estadoConexao == EstadoConexao.wifi
        if (emWifi) viewModel.verificarDispositivosNovos(this)
    }

    override fun onStop() {
        viewModel.encerrarMonitorRede()
        super.onStop()
    }

    private fun verificarEPedirPermissoes() {
        val pendentes = viewModel.gerenciadorPermissoes.listarPermissoesPendentes()
        if (pendentes.isNotEmpty()) {
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
