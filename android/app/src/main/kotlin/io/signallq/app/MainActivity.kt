package io.signallq.app

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint
import io.signallq.app.ads.AdsFlags
import io.signallq.app.ads.AdsFlagsManager
import io.signallq.app.ads.ConsentManager
import io.signallq.app.core.network.AnalyticsHelper
import io.signallq.app.core.network.AnalyticsTracker
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.feature.devices.DevicesViewModel
import io.signallq.app.feature.speedtest.SpeedtestViewModel
import io.signallq.app.review.InAppReviewManager
import io.signallq.app.ui.SignallQTheme
import io.signallq.app.ui.component.LgpdConsentDialog
import io.signallq.app.ui.screen.AppShell
import io.signallq.app.ui.screen.OnboardingScreen
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    @Inject
    lateinit var inAppReviewManager: InAppReviewManager

    @Inject
    lateinit var adsFlagsManager: AdsFlagsManager

    // GH#970 — resolve identidade/contato de operadora local -> diretorio remoto do
    // worker signallq-diagnostic -> fallback generico. Injetado aqui (nao dentro de
    // Composable) porque AppShell/Inicio2Screen/ResultadoVelocidadeScreen sao 100%
    // data-driven (sem hiltViewModel() em Composables leaf neste app).
    @Inject
    lateinit var operadoraDirectoryResolver: io.signallq.app.ui.OperadoraDirectoryResolver

    private val viewModel: MainViewModel by viewModels()

    // ViewModels por feature — extraidos do MainViewModel (Passo 6 do plano de migracao).
    // Fase atual: instanciados e conectados; o MainViewModel ainda contem logica legada
    // para compatibilidade de build. Cleanup do MainViewModel em PR subsequente.
    // NOTA: DiagnosticoViewModel removido — era codigo morto (instanciado, nunca referenciado).
    // Callbacks de diagnostico continuam delegando para MainViewModel intencionalmente
    // por compatibilidade legada; migracao completa em PR subsequente.
    private val devicesViewModel: DevicesViewModel by viewModels()
    private val speedtestViewModel: SpeedtestViewModel by viewModels()

    private var temPermissaoTelefonia by mutableStateOf(false)
    private var temPermissaoLocalizacao by mutableStateOf(false)

    // #155/9.3: permissão negada permanentemente (shouldShowRequestPermissionRationale = false E não concedida)
    private var localizacaoBloqueadaPermanentemente by mutableStateOf(false)

    // Issue #1671 -- mesma logica de #155/9.3, agora tambem para telefonia e notificacao:
    // as 3 permissoes opcionais tornaram-se contextuais (pedidas so no ponto de uso), entao
    // cada uma precisa da propria distincao "nunca pedida" x "negada permanentemente" pra
    // decidir entre reabrir o dialogo do sistema ou mandar o usuario pra Ajustes do app.
    private var telefoniaBloqueadaPermanentemente by mutableStateOf(false)
    private var notificacaoBloqueadaPermanentemente by mutableStateOf(false)

    // Issue #1671 -- launchers reais de permissao contextual. Antes, a UNICA tela que de fato
    // solicitava essas permissoes ao SO era o onboarding (pedido em lote); com o onboarding
    // reduzido a 1 tela (boas-vindas + termos/LGPD, sem pedido de permissao), cada ponto de uso
    // (aba Wi-Fi/Sinal, aba Movel, toggle de monitoramento) precisa do proprio disparo real.
    private val solicitarLocalizacaoLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultado ->
            viewModel.marcarLocalizacaoPermissaoJaSolicitada()
            val concedida =
                resultado[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    resultado[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            temPermissaoLocalizacao = concedida
            localizacaoBloqueadaPermanentemente =
                !concedida &&
                !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
            if (concedida) viewModel.iniciarRotinasNaoSpeedtest()
        }

    private val solicitarTelefoniaLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
            temPermissaoTelefonia = concedida
            telefoniaBloqueadaPermanentemente =
                !concedida &&
                !shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE)
            if (concedida) viewModel.iniciarMonitorTelefoniaSeMovel()
        }

    private val solicitarNotificacoesLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
            notificacaoBloqueadaPermanentemente =
                !concedida &&
                !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        }

    // Issue #555 -- gate de consentimento UMP para anuncio nativo AdMob. Comeca false:
    // nenhuma tela pede anuncio ate a UMP responder (mesmo que a resposta seja "nao
    // exigido nesta regiao", ainda precisa do callback pra saber disso).
    private var podeRequisitarAnuncio by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        analyticsTracker.registrarSessionStart()
        registrarBatterySnapshotInicial()

        // Issue #555 -- gate de consentimento UMP antes de qualquer AdRequest, mesmo
        // so contextual. MobileAds.initialize so roda depois do consentimento resolvido
        // (ordem recomendada pelo proprio guia UMP+AdMob do Google).
        if (BuildConfig.ADS_ENABLED) {
            ConsentManager.atualizarEMostrarSeNecessario(this) { podeRequisitar ->
                podeRequisitarAnuncio = podeRequisitar
                if (podeRequisitar) {
                    MobileAds.initialize(this) {}
                }
            }
        }
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
            // GH#865 Fase 1 — snapshot normalizado do equipamento local (ONT Nokia).
            val localDeviceSnapshot =
                viewModel.localDeviceSnapshot
                    .collectAsStateWithLifecycle()
                    .value
            // GH#934 — Fase 5: NAT/CGNAT (SIG-279), reusado pela EquipamentoInternetScreen.
            val natStatus =
                viewModel.natStatusFlow
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
            val resumoHistorico = viewModel.resumoHistorico.collectAsStateWithLifecycle().value
            // #95 — Filtros do Historico
            val historicoFiltrado = viewModel.historicoFiltrado.collectAsStateWithLifecycle().value
            val filtroConexaoHistorico = viewModel.filtroConexaoHistorico.collectAsStateWithLifecycle().value
            val filtroOperadoraHistorico = viewModel.filtroOperadoraHistorico.collectAsStateWithLifecycle().value
            val operadorasDisponiveisHistorico = viewModel.operadorasDisponiveisHistorico.collectAsStateWithLifecycle().value
            // #1666/#1520 — grid de uptime de 7 dias, religado apos ficar orfao.
            val blocosUptimeHistorico = viewModel.uptimeBlocos.collectAsStateWithLifecycle().value

            // --- Preferencias combinadas (1 subscricao por grupo) ---
            val preferenciasModem = viewModel.preferenciasModem.collectAsStateWithLifecycle().value
            val modemHost = preferenciasModem.host
            val modemUsername = preferenciasModem.username
            val modemPassword = preferenciasModem.password
            val modemPermanecerConectado = preferenciasModem.permanecerConectado
            val gatewaySessionBssid = preferenciasModem.gatewaySessionBssid

            val preferenciasNotificacao = viewModel.preferenciasNotificacao.collectAsStateWithLifecycle().value
            val notificacaoLatenciaAtiva = preferenciasNotificacao.latenciaAtiva
            val notificacaoDnsAtiva = preferenciasNotificacao.dnsAtiva
            val notificacaoRssiAtiva = preferenciasNotificacao.rssiAtiva
            val notificacaoSemInternetAtiva = preferenciasNotificacao.semInternetAtiva

            val preferenciasUi = viewModel.preferenciasUi.collectAsStateWithLifecycle().value
            val temaSelecionado = preferenciasUi.temaSelecionado
            val analiseAvancada = preferenciasUi.analiseAvancada

            // Modo gamer (Feature #550, issue #1476) — combinação jogo+device salva como padrão.
            val modoGamerPadrao = viewModel.modoGamerPadrao.collectAsStateWithLifecycle().value

            val perfilProvedor = viewModel.preferenciasPerfilProvedor.collectAsStateWithLifecycle().value
            val nomeUsuario = perfilProvedor.nomeUsuario
            val fotoUriUsuario = perfilProvedor.fotoUriUsuario
            val operadora = perfilProvedor.operadora
            val planoInternet = perfilProvedor.planoInternet
            val regiao = perfilProvedor.regiao
            val limiteAlertaMbps = perfilProvedor.limiteAlertaMbps
            // GH#1249 -- perfil de conexao por rede (substitui estadoUf/cidadeNome/
            // velocidadeContratadaDown-UpMbps/ispConfirmado globais na tela de Ajustes).
            val connectionProfileAtual = viewModel.connectionProfileAtual.collectAsStateWithLifecycle().value
            // Issue #1670 — estado observável (EmAndamento/Sucesso/Falha) de limpar
            // histórico/apagar dados/resetar app, disparados pela DadosLocaisSheet.
            val dadosLocaisAcaoEstado = viewModel.dadosLocaisAcaoEstado.collectAsStateWithLifecycle().value

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
            // GH#1512 — conclusao do diagnostico local quando o Speedtest e interrompido
            // por Wi-Fi conectado sem internet.
            val diagnosticoConectividade =
                viewModel.diagnosticoConectividade
                    .collectAsStateWithLifecycle()
                    .value
            val apelidos = viewModel.apelidos.collectAsStateWithLifecycle().value
            val correlacoesTopologia = viewModel.correlacoesTopologia.collectAsStateWithLifecycle().value
            val snapshotDiagnostico =
                viewModel.diagnosticOrchestrator.snapshotFlow
                    .collectAsStateWithLifecycle()
                    .value
            val medicaoBaseModoGamer by viewModel.medicaoBaseModoGamer.collectAsStateWithLifecycle()
            val networkIdAtual by viewModel.networkIdAtual.collectAsStateWithLifecycle()
            val movelSnapshot = viewModel.movelSnapshot.collectAsStateWithLifecycle().value
            val simsAtivos = viewModel.simsAtivos.collectAsStateWithLifecycle().value
            val gemmaAvailable = viewModel.gemmaAvailable.collectAsStateWithLifecycle().value
            val onboardingConcluido = viewModel.onboardingConcluido.collectAsStateWithLifecycle().value
            val consentimentoLgpd = viewModel.consentimentoLgpd.collectAsStateWithLifecycle().value
            val analisadorState by viewModel.analisadorState.collectAsStateWithLifecycle()
            // GH#1707 (Task 2.0.09e, parte 2/2) — estado do reteste vinculado (spec §8.8/§14.6).
            val comparacaoRetesteState by viewModel.comparacaoRetesteState.collectAsStateWithLifecycle()
            val recommendationDecision by viewModel.recommendationDecision.collectAsStateWithLifecycle()
            val recommendationFeedback by viewModel.recommendationFeedback.collectAsStateWithLifecycle()
            // Issue #555 -- toggle remoto (Firebase Remote Config) de anuncios nativos.
            val adsFlags by adsFlagsManager.flags.collectAsStateWithLifecycle()
            // GH#1480 (Epico #1347, F4) -- gate de navegacao dos 9 modulos feature do Consumer.
            val featureFlagsState by viewModel.featureFlagsState.collectAsStateWithLifecycle()

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

            // SIG-173/#664 — avaliacao nativa Google Play sem atrito. A elegibilidade
            // (ReviewPromptPolicy) e decidida no MainViewModel; aqui so disparamos o
            // fluxo nativo, que exige uma Activity e nunca deve ser retido pelo ViewModel.
            LaunchedEffect(Unit) {
                viewModel.solicitarAvaliacaoPlayEvent.collect {
                    analyticsTracker.registrarFeatureUsada("review_prompt_google_play")
                    inAppReviewManager.solicitarFluxoAvaliacao(this@MainActivity)
                }
            }

            // GH#1707 (Task 2.0.09e, parte 2/2) — funil do reteste vinculado (spec #1657, passos
            // 8/9). O cálculo do payload é do ViewModel (rede/DB/orchestrator); o disparo pro
            // Firebase fica aqui, mesmo padrão do funil do diagnóstico guiado (#1706) logo abaixo.
            LaunchedEffect(Unit) {
                viewModel.retesteIniciadoEvent.collect { evento ->
                    analyticsTracker.registrarDiagnosticoRetesteIniciado(evento)
                }
            }
            LaunchedEffect(Unit) {
                viewModel.comparacaoConcluidaEvent.collect { evento ->
                    analyticsTracker.registrarDiagnosticoComparacaoConcluida(evento)
                }
            }

            // GH#1265 — onStart() confere `viewModel.onboardingConcluido.value == true` de
            // forma SINCRONA, mas esse StateFlow comeca em `null` (#895) ate o DataStore emitir
            // o valor real (assincrono, so apos o primeiro coletor inscrever). Numa cold-start
            // genuina (processo recem-criado pelo SO, nao so reativado do background) o onStart()
            // roda antes desse valor real chegar -- reproduzido em device/emulador: o card
            // "Caminho da sua internet" ficava preso em "Buscando.../Conectando..." indefinidamente
            // (nao so mais lento), porque `iniciarRotinasNaoSpeedtest()` nunca era chamado por
            // nenhum dos 3 gatilhos (onStart, callback de permissao do onboarding, termino de
            // speedtest). Este efeito reage ao MESMO valor de `onboardingConcluido` (coletado
            // acima via collectAsStateWithLifecycle) assim que ele chega a `true` de verdade --
            // seguro chamar de novo mesmo se onStart() ja tiver disparado, pois cada rotina
            // interna ja tem guard proprio (scannerDispositivosDisparado, ispInfoColetada etc).
            LaunchedEffect(onboardingConcluido) {
                if (onboardingConcluido == true) viewModel.iniciarRotinasNaoSpeedtest()
            }

            val connectedBssid = snapshotRede.wifiLinkSnapshot?.bssid
            val connectedNetwork =
                if (connectedBssid != null) {
                    snapshotWifi.redes.find { it.bssid == connectedBssid }
                } else {
                    null
                }

            SignallQTheme(darkTheme = darkTheme) {
                // #895/#1671: rota inicial extraida para funcao pura testavel (RotaInicialApp.kt)
                // -- `onboardingConcluido == null` e um 3o estado real ("DataStore ainda nao
                // respondeu"), distinto de `false` ("usuario novo"). Sem ele a tela de Onboarding
                // (e, na sequencia, o dialog de LGPD) "piscava" por um instante em TODO cold
                // start, mesmo pra quem ja concluiu ambos.
                when (rotaInicialApp(onboardingConcluido, consentimentoLgpd)) {
                    RotaInicialApp.Carregando ->
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background),
                        )
                    RotaInicialApp.Onboarding ->
                        // Issue #1671 (Task 2.0.23, epico #1647) -- onboarding virou 1 tela so
                        // (boas-vindas + termos/LGPD). Nao pede mais nenhuma permissao em lote:
                        // cada permissao opcional e solicitada de forma contextual, no ponto de
                        // uso (ver solicitarPermissao*Contextual() e onAtivarMonitoramento abaixo).
                        OnboardingScreen(
                            onConcluir = { viewModel.marcarOnboardingConcluido() },
                        )
                    RotaInicialApp.ConsentimentoLgpd ->
                        LgpdConsentDialog(
                            onAceitar = { viewModel.definirConsentimentoLgpd(true) },
                            onRecusar = { viewModel.definirConsentimentoLgpd(false) },
                        )
                    RotaInicialApp.Home ->
                        AppShell(
                            snapshotRede = snapshotRede,
                            speedtest =
                                io.signallq.app.ui.screen.AppShellSpeedtestState(
                                    snapshotSpeedtest = snapshotSpeedtest,
                                    speedtestPendenteModoMovel = speedtestPendenteModoMovel,
                                    speedtestPermiteHeavyMovel = speedtestPermiteHeavyMovel,
                                    speedtestMbConsumidosMes = speedtestMbConsumidosMes,
                                    onNovoTeste = { modo -> viewModel.reiniciarSuite(modo) },
                                    onNovoTesteJaConfirmadoMovel = { modo ->
                                        viewModel.reiniciarSuite(modo, jaConfirmadoRedeMovel = true)
                                    },
                                    onCancelarTeste = { viewModel.executorSpeedtest.cancelar() },
                                    onConfirmarSpeedtestMovel = { viewModel.confirmarSpeedtestEmMovel() },
                                    onCancelarSpeedtestMovel = { viewModel.cancelarSpeedtestMovel() },
                                    onSetSpeedtestPermiteHeavyMovel = { valor -> viewModel.setSpeedtestPermiteHeavyMovel(valor) },
                                    diagnosticoConectividade = diagnosticoConectividade,
                                    onLimparDiagnosticoConectividade = { viewModel.limparDiagnosticoConectividade() },
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
                                    correlacoesTopologia = correlacoesTopologia,
                                ),
                            diagnostico =
                                io.signallq.app.ui.screen.AppShellDiagnosticoState(
                                    snapshotDiagnostico = snapshotDiagnostico,
                                    medicaoBaseModoGamer = medicaoBaseModoGamer,
                                    networkIdAtual = networkIdAtual,
                                    onIniciarDiagnostico = {
                                        // GH#919 — feature_used("diagnostico") era disparado dentro do
                                        // SignallQOrchestrator (motor SignallQ Pulse), correlacionado com
                                        // diagnostic_sessions.id/ai_usage.session_id. O motor foi removido
                                        // por ser codigo morto sem consumidor de UI (GH#1682) e nada
                                        // retomou esse disparo — feature_used("diagnostico") com
                                        // correlacao real fica pendente de decisao de produto/analytics
                                        // (nao adicionado aqui para nao emitir com session_id generico
                                        // e sem correlacao, que era exatamente o problema original).
                                        viewModel.iniciarDiagnostico()
                                    },
                                    onSolicitarDiagnostico = { viewModel.solicitarDiagnostico() },
                                    analisadorState = analisadorState,
                                    onAnalisarProblema = { problema -> viewModel.analisarProblema(problema) },
                                    onResetarAnalisador = { viewModel.resetarAnalisador() },
                                    onLaudoFechado = { viewModel.onLaudoFechado() },
                                    recommendationDecision = recommendationDecision,
                                    recommendationFeedback = recommendationFeedback,
                                    onRecommendationShown = { viewModel.registrarRecomendacaoMostrada() },
                                    onRecommendationClicked = { viewModel.registrarRecomendacaoClicada() },
                                    onRecommendationFeedback = { feedback -> viewModel.registrarFeedbackRecomendacao(feedback) },
                                    onTestarNovamenteVinculado = { analiseId, acaoAnteriorId ->
                                        viewModel.testarNovamenteVinculado(analiseId, acaoAnteriorId)
                                    },
                                    comparacaoRetesteState = comparacaoRetesteState,
                                ),
                            signallQ =
                                io.signallq.app.ui.screen.AppShellSignallQState(
                                    gemmaAvailable = gemmaAvailable,
                                    operadoraMovel =
                                        simsAtivos.firstOrNull { it.isDefaultData }?.operadora
                                            ?: simsAtivos.firstOrNull()?.operadora,
                                    onVerificarGemma = { viewModel.verificarDisponibilidadeGemma() },
                                ),
                            ads =
                                io.signallq.app.ui.screen.AppShellAdsState(
                                    flags = if (BuildConfig.ADS_ENABLED) adsFlags else AdsFlags.DESLIGADO,
                                    podeRequisitarAnuncio = BuildConfig.ADS_ENABLED && podeRequisitarAnuncio,
                                ),
                            featureFlags = featureFlagsState,
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
                            connectionProfileAtual = connectionProfileAtual,
                            onSalvarConnectionProfile = { providerFixed, down, up, cidade, uf, userConfirmed ->
                                viewModel.salvarConnectionProfileAtual(providerFixed, down, up, cidade, uf, userConfirmed)
                            },
                            limiteAlertaMbps = limiteAlertaMbps,
                            dnsResolverIp = snapshotRede.dnsServidores.firstOrNull(),
                            historico = historico,
                            snapshotFibra = snapshotFibra,
                            localDevice = localDeviceSnapshot,
                            natStatus = natStatus,
                            modemHost = modemHost,
                            modemUsername = modemUsername,
                            modemPassword = modemPassword,
                            modemPermanecerConectado = modemPermanecerConectado,
                            gatewaySessionBssid = gatewaySessionBssid,
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
                            onReiniciarEquipamento = {
                                viewModel.reiniciarEquipamento()
                                analyticsTracker.registrarFeatureUsada("fibra")
                            },
                            onSalvarConfiguracaoModem = { host, user, pass, perm ->
                                viewModel.salvarConfiguracaoModem(host, user, pass, perm)
                            },
                            onRegistrarConexaoGateway = { ip, usuario, senha, lembrarSenha, manterConectado, bssidAtual ->
                                viewModel.registrarConexaoGateway(ip, usuario, senha, lembrarSenha, manterConectado, bssidAtual)
                                analyticsTracker.registrarFeatureUsada("fibra")
                            },
                            temaSelecionado = temaSelecionado,
                            analiseAvancada = analiseAvancada,
                            onDefinirTemaSelecionado = { tema -> viewModel.definirTemaSelecionado(tema) },
                            onDefinirAnaliseAvancada = { ativa -> viewModel.definirAnaliseAvancada(ativa) },
                            onLimparHistorico = { viewModel.limparHistorico() },
                            onApagarDadosLocais = { viewModel.apagarDadosLocais() },
                            onResetarApp = { viewModel.resetarApp() },
                            dadosLocaisAcaoEstado = dadosLocaisAcaoEstado,
                            onConsumirDadosLocaisAcaoEstado = { viewModel.consumirDadosLocaisAcaoEstado() },
                            monitoramentoAtivo = monitoramentoAtivo,
                            onAtivarMonitoramento = { ativo ->
                                // Issue #1671 -- permissao de notificacao e contextual: so e
                                // pedida aqui, no momento em que o usuario liga o monitoramento
                                // (a funcionalidade que de fato precisa dela), nunca no onboarding.
                                if (ativo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val notificacaoConcedida =
                                        ContextCompat.checkSelfPermission(
                                            this@MainActivity,
                                            Manifest.permission.POST_NOTIFICATIONS,
                                        ) == PackageManager.PERMISSION_GRANTED
                                    when (decidirPermissaoContextual(notificacaoConcedida, notificacaoBloqueadaPermanentemente)) {
                                        DecisaoPermissaoContextual.JA_CONCEDIDA -> Unit
                                        DecisaoPermissaoContextual.SOLICITAR ->
                                            solicitarNotificacoesLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        DecisaoPermissaoContextual.ABRIR_AJUSTES -> abrirAjustesDoApp()
                                    }
                                }
                                viewModel.atualizarMonitoramento(ativo)
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
                            onSalvarLimiteAlerta = { limite -> viewModel.salvarLimiteAlerta(limite) },
                            movelSnapshot = movelSnapshot,
                            simsAtivos = simsAtivos,
                            temPermissaoTelefonia = temPermissaoTelefonia,
                            onSolicitarPermissaoTelefonia = { solicitarPermissaoTelefoniaContextual() },
                            temPermissaoLocalizacao = temPermissaoLocalizacao,
                            localizacaoBloqueadaPermanentemente = localizacaoBloqueadaPermanentemente,
                            onSolicitarPermissaoLocalizacao = { solicitarPermissaoLocalizacaoContextual() },
                            historicoTela =
                                io.signallq.app.ui.screen.AppShellHistoricoState(
                                    historicoFiltrado = historicoFiltrado,
                                    resumoHistorico = resumoHistorico,
                                    filtroConexao = filtroConexaoHistorico,
                                    onFiltroConexaoChange = {
                                        viewModel.setFiltroConexaoHistorico(it)
                                        analyticsTracker.registrarFeatureUsada("historico")
                                    },
                                    filtroOperadora = filtroOperadoraHistorico,
                                    onFiltroOperadoraChange = {
                                        viewModel.setFiltroOperadoraHistorico(it)
                                        analyticsTracker.registrarFeatureUsada("historico")
                                    },
                                    operadorasDisponiveis = operadorasDisponiveisHistorico,
                                    onExcluirMedicao = viewModel::deletarMedicao,
                                    blocosUptime = blocosUptimeHistorico,
                                ),
                            onScreenView = { screenName -> analyticsTracker.registrarScreenView(screenName) },
                            // GH#1706 — funil do diagnostico guiado (spec §12, passos 3 e 4).
                            onDiagnosticoPlanoIniciado = analyticsTracker::registrarDiagnosticoPlanoIniciado,
                            onAvaliarAssist = viewModel::avaliarAssist,
                            onCompartilharResultadoVelocidade = {
                                analyticsTracker.registrarFeatureUsada("speedtest_compartilhou")
                            },
                            // GH#970 — cadeia local -> diretorio remoto -> fallback generico
                            // (io.signallq.app.ui.OperadoraDirectoryResolver, injetado via Hilt).
                            operadoraResolvers =
                                io.signallq.app.ui.screen.AppShellOperadoraResolvers(
                                    identidadeLocal = operadoraDirectoryResolver::resolveLocalIdentity,
                                    contatoLocal = operadoraDirectoryResolver::resolveLocalContact,
                                    identidadeRemota = operadoraDirectoryResolver::resolveIdentity,
                                    contatoRemoto = operadoraDirectoryResolver::resolveContact,
                                ),
                            modoGamerPadrao = modoGamerPadrao,
                            onSalvarModoGamerPadrao = viewModel::salvarModoGamerPadrao,
                        )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        analyticsTracker.registrarSessionStart()
        viewModel.iniciarMonitorRede()
        if (viewModel.onboardingConcluido.value == true) {
            viewModel.iniciarRotinasNaoSpeedtest()
        }
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
        // #155/9.3, corrigido em #1182: shouldShowRequestPermissionRationale() sozinho retorna
        // false tanto para "nunca pedida" quanto para "negada permanentemente" -- so trata como
        // bloqueio permanente quando ja existe um pedido real registrado (marcado no callback do
        // RequestMultiplePermissions do onboarding, unica tela que de fato solicita esta permissao).
        localizacaoBloqueadaPermanentemente = !temPermissaoLocalizacao &&
            !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) &&
            viewModel.localizacaoPermissaoJaSolicitada.value
        val emWifi = viewModel.monitorRede.snapshotFlow.value.estadoConexao == EstadoConexao.wifi
        // Usa DevicesViewModel para verificar novos dispositivos (etapa A do refactor).
        // O MainViewModel.verificarDispositivosNovos() ainda existe mas nao e mais chamado aqui.
        if (emWifi) devicesViewModel.verificarDispositivosNovos()
    }

    override fun onStop() {
        // Fecha a sessão de foreground antes de pausar a atividade. O envio ao Admin
        // permanece best-effort e idempotente pelo id do evento; nunca bloqueia UI.
        analyticsTracker.registrarSessionEnd()
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
        // Issue #1671 -- contextual de verdade: pede o dialogo real do SO na primeira vez
        // (ou enquanto ainda faz sentido reperguntar); so manda pra Ajustes quando o SO ja
        // negou permanentemente nesta sessao.
        if (telefoniaBloqueadaPermanentemente) {
            abrirAjustesDoApp()
        } else {
            solicitarTelefoniaLauncher.launch(Manifest.permission.READ_PHONE_STATE)
        }
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
            viewModel.iniciarRotinasNaoSpeedtest()
            return
        }
        // Issue #1671 -- contextual de verdade: pede o dialogo real do SO na primeira vez.
        // "Dispositivos na rede" (NEARBY_WIFI_DEVICES, API 33+) e pedida junto porque serve a
        // mesma funcionalidade de escaneamento de Wi-Fi que localizacao habilita aqui -- nao
        // faz sentido dois dialogos separados para a mesma entrada de tela.
        if (localizacaoBloqueadaPermanentemente) {
            abrirAjustesDoApp()
        } else {
            val permissoes =
                mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissoes += Manifest.permission.NEARBY_WIFI_DEVICES
            }
            solicitarLocalizacaoLauncher.launch(permissoes.toTypedArray())
        }
    }

    private fun abrirAjustesDoApp() {
        startActivity(
            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            },
        )
    }
}
