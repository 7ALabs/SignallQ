package io.veloo.app

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.veloo.app.core.database.ApelidoDispositivoEntity
import io.veloo.app.core.database.SignallQDatabase
import io.veloo.app.core.database.MedicaoEntity
import io.veloo.app.core.datastore.PreferenciasAppRepository
import io.veloo.app.core.network.DispatcherProvider
import io.veloo.app.core.network.EstadoConexao
import io.veloo.app.core.network.MonitorRede
import io.veloo.app.core.network.NetworkCapabilitiesProvider
import io.veloo.app.core.permissions.GerenciadorPermissoesRede
import io.veloo.app.core.telephony.MonitorTelephony
import io.veloo.app.core.telephony.MovelSimSnapshot
import io.veloo.app.core.telephony.MovelSnapshot
import io.veloo.app.feature.devices.ScannerDispositivos
import io.veloo.app.feature.devices.SnapshotScanDispositivos
import io.veloo.app.feature.diagnostico.DiagnosticOrchestrator
import io.veloo.app.feature.diagnostico.FibraDiagnosticInput
import io.veloo.app.feature.diagnostico.InternetDiagnosticInput
import io.veloo.app.feature.diagnostico.WifiDiagnosticInput
import io.veloo.app.feature.diagnostico.ai.AdditionalAiContext
import io.veloo.app.feature.diagnostico.ai.AiDiagnosisRepository
import io.veloo.app.feature.diagnostico.ai.AiDiagnosisState
import io.veloo.app.feature.diagnostico.ai.AiDispositivosInfo
import io.veloo.app.feature.diagnostico.ai.AiFallbackFactory
import io.veloo.app.feature.diagnostico.ai.AiMovelInfo
import io.veloo.app.feature.diagnostico.ai.AiRedeVizinha
import io.veloo.app.feature.diagnostico.ai.AiTesteHistorico
import io.veloo.app.feature.diagnostico.ai.DiagChatAutor
import io.veloo.app.feature.diagnostico.ai.DiagChatEntry
import io.veloo.app.feature.diagnostico.ai.DiagnosisAiContext
import io.veloo.app.feature.diagnostico.ai.DiagnosisAiContextFactory
import io.veloo.app.feature.diagnostico.pulse.OpcaoResposta
import io.veloo.app.feature.dns.AvaliadorCoerenciaDns
import io.veloo.app.feature.dns.BenchmarkDns
import io.veloo.app.feature.dns.EstadoBenchmarkDns
import io.veloo.app.feature.dns.OrientadorConfiguracaoDns
import io.veloo.app.feature.fibra.EstadoFibra
import io.veloo.app.feature.fibra.ExecutorFibra
import io.veloo.app.feature.history.BlocoUptime
import io.veloo.app.feature.history.ObservadorHistoricoRoom
import io.veloo.app.feature.history.ResumoHistorico
import io.veloo.app.feature.history.UptimeChartUseCase
import io.veloo.app.feature.history.UptimeNarrativaEngine
import io.veloo.app.feature.speedtest.ExecutorSpeedtest
import io.veloo.app.feature.speedtest.ModoSpeedtest
import io.veloo.app.feature.wifi.ScannerRedesWifi
import io.veloo.app.monitoramento.MonitoramentoScheduler
import io.veloo.app.notificacao.SignallQNotificationHelper
import io.veloo.app.feature.diagnostico.pulse.SignallQOrchestrator
import io.veloo.app.pulse.SignallQUiStateMapper
import io.veloo.app.ui.ConnectionNodeType
import io.veloo.app.ui.FiltroConexaoHistorico
import io.veloo.app.ui.GatewayInfo
import io.veloo.app.ui.HistoryPoint
import io.veloo.app.ui.IspInfo
import io.veloo.app.ui.screen.SignallQUiState
import io.veloo.app.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        application: Application,
        val preferenciasAppRepository: PreferenciasAppRepository,
        val monitorRede: MonitorRede,
        val networkCapabilitiesProvider: NetworkCapabilitiesProvider,
        val gerenciadorPermissoes: GerenciadorPermissoesRede,
        val scannerDispositivos: ScannerDispositivos,
        val benchmarkDns: BenchmarkDns,
        val executorSpeedtest: ExecutorSpeedtest,
        val scannerRedesWifi: ScannerRedesWifi,
        val executorFibra: ExecutorFibra,
        /** Monitor de telefonia movel — instanciado pelo Hilt. NAO inicia automaticamente:
         *  o start so acontece quando [iniciarMonitorTelefoniaSeMovel] e chamado
         *  (rede movel ativa + permissao concedida). Em Wi-Fi/Ethernet, o monitor
         *  fica idle e nao consome bateria com callbacks de TelephonyManager. */
        val monitorTelephony: MonitorTelephony,
        private val bancoDados: SignallQDatabase,
        private val dispatchers: DispatcherProvider,
        /** AiDiagnosisRepository injetada pelo Hilt como @Singleton (DiagnosticoModule).
         *  Antes era instanciada manualmente via lazy (segunda instancia alem da do Orchestrator). */
        val diagAiRepository: AiDiagnosisRepository,
        /** DiagnosticOrchestrator injetado pelo Hilt como @Singleton (DiagnosticoModule).
         *  Antes era instanciado via lazy: `by lazy { DiagnosticOrchestrator() }`.
         *  Agora e singleton compartilhado com DiagnosticoViewModel. */
        _diagnosticOrchestrator: DiagnosticOrchestrator,
    ) : AndroidViewModel(application) {
        private companion object {
            const val LOG_TAG = "SignallQSpeedtestSuite"
            const val DNS_CACHE_TTL_MS = 15 * 60 * 1_000L
        }

        private val avaliadorCoerenciaDns by lazy { AvaliadorCoerenciaDns() }

        @Suppress("unused")
        private val orientadorConfiguracaoDns by lazy { OrientadorConfiguracaoDns() }

        // DiagnosticOrchestrator injetado via Hilt como @Singleton — compartilhado com DiagnosticoViewModel.
        // O underscore no construtor e convencao para parametros que viram val publico.
        val diagnosticOrchestrator: DiagnosticOrchestrator = _diagnosticOrchestrator
        val movelSnapshot: StateFlow<MovelSnapshot?> get() = monitorTelephony.snapshotFlow

        // #179 Task C — Dual SIM: lista de SIMs ativos, atualizada sempre que o monitor de
        // telefonia emite novo snapshot (mudanca de rede/sinal). Inicializado com emptyList()
        // para nao crashar a UI antes da captura. Nao chama startScan() — usa dados cacheados.
        private val _simsAtivos = MutableStateFlow<List<MovelSimSnapshot>>(emptyList())
        val simsAtivos: StateFlow<List<MovelSimSnapshot>> = _simsAtivos
        val signallQOrchestrator by lazy {
            SignallQOrchestrator(
                executorSpeedtest = executorSpeedtest,
                diagnosticOrchestrator = diagnosticOrchestrator,
                monitorRede = monitorRede,
                medicaoDao = bancoDados.medicaoDao(),
                scope = viewModelScope,
                additionalContextProvider = { coletarContextoAdicionalIa() },
                networkCapabilitiesProvider = networkCapabilitiesProvider,
                aiRepository = diagAiRepository,
            )
        }
        val signallQUiStateFlow by lazy {
            signallQOrchestrator.snapshotFlow
                .map { SignallQUiStateMapper.from(it) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SignallQUiState.Idle)
        }

        val onboardingConcluido: StateFlow<Boolean> by lazy {
            preferenciasAppRepository.onboardingConcluidoFlow
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
        }

        // #82 — Banner Anatel dismissível
        val anatelBannerDismissed: StateFlow<Boolean> by lazy {
            preferenciasAppRepository.anatelBannerDismissedFlow
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
        }

        fun dispensarBannerAnatel() {
            viewModelScope.launch { preferenciasAppRepository.definirAnatelBannerDismissed(true) }
        }

        fun marcarOnboardingConcluido() {
            viewModelScope.launch { preferenciasAppRepository.definirOnboardingConcluido(true) }
        }

        fun concluirOnboarding() {
            viewModelScope.launch { preferenciasAppRepository.definirOnboardingConcluido(true) }
        }

        val gemmaAvailable = MutableStateFlow(false)

        // ── DiagChat ──────────────────────────────────────────────────────────────
        private val _diagChatHistorico = MutableStateFlow<List<DiagChatEntry>>(emptyList())
        val diagChatHistorico: StateFlow<List<DiagChatEntry>> = _diagChatHistorico

        private val _diagChatCarregando = MutableStateFlow(false)
        val diagChatCarregando: StateFlow<Boolean> = _diagChatCarregando

        private var diagAiContext: DiagnosisAiContext? = null

        // -------------------------------------------------------------------------
        // Flows combinados — agrupam preferencias do mesmo dominio para reduzir o
        // numero de subscricoes na Activity e evitar recomposicoes em cascata.
        // distinctUntilChanged() em flows que podem oscilar para o mesmo valor.
        // -------------------------------------------------------------------------

        /** Preferencias de configuracao do modem: 4 flows para 1 subscricao. */
        val preferenciasModem: StateFlow<PreferenciasModemUiState> by lazy {
            combine(
                preferenciasAppRepository.modemHostFlow,
                preferenciasAppRepository.modemUsernameFlow,
                preferenciasAppRepository.modemPasswordFlow,
                preferenciasAppRepository.modemPermanecerConectadoFlow,
            ) { host, username, password, permanecerConectado ->
                PreferenciasModemUiState(
                    host = host,
                    username = username,
                    password = password,
                    permanecerConectado = permanecerConectado,
                )
            }.distinctUntilChanged()
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    PreferenciasModemUiState(),
                )
        }

        /** Controles granulares de notificacao: 4 flows para 1 subscricao. */
        val preferenciasNotificacao: StateFlow<PreferenciasNotificacaoUiState> by lazy {
            combine(
                preferenciasAppRepository.notificacaoLatenciaAtivaFlow,
                preferenciasAppRepository.notificacaoDnsAtivaFlow,
                preferenciasAppRepository.notificacaoRssiAtivaFlow,
                preferenciasAppRepository.notificacaoSemInternetAtivaFlow,
            ) { latencia, dns, rssi, semInternet ->
                PreferenciasNotificacaoUiState(
                    latenciaAtiva = latencia,
                    dnsAtiva = dns,
                    rssiAtiva = rssi,
                    semInternetAtiva = semInternet,
                )
            }.distinctUntilChanged()
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    PreferenciasNotificacaoUiState(),
                )
        }

        /** Preferencias de UI (tema e analise avancada): 2 flows para 1 subscricao. */
        val preferenciasUi: StateFlow<PreferenciasUiUiState> by lazy {
            combine(
                preferenciasAppRepository.temaSelecionadoFlow,
                preferenciasAppRepository.analiseAvancadaFlow,
            ) { tema, analise ->
                PreferenciasUiUiState(temaSelecionado = tema, analiseAvancada = analise)
            }.distinctUntilChanged()
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    PreferenciasUiUiState(),
                )
        }

        /**
         * Dados de perfil do usuario e provedor: 9 flows para 1 subscricao.
         * Usa combine aninhado para superar o limite de 5 parametros do combine padrao.
         */
        val preferenciasPerfilProvedor: StateFlow<PreferenciasPerfilProvedorUiState> by lazy {
            combine(
                combine(
                    preferenciasAppRepository.nomeUsuarioFlow,
                    preferenciasAppRepository.fotoUriUsuarioFlow,
                    preferenciasAppRepository.operadoraFlow,
                    preferenciasAppRepository.planoInternetFlow,
                ) { nome, foto, op, plano ->
                    listOf<Any?>(nome, foto, op, plano)
                },
                combine(
                    preferenciasAppRepository.regiaoFlow,
                    preferenciasAppRepository.estadoUfFlow,
                    preferenciasAppRepository.cidadeNomeFlow,
                    preferenciasAppRepository.ispConfirmadoFlow,
                    preferenciasAppRepository.limiteAlertaMbpsFlow,
                ) { regiao, uf, cidade, isp, limite ->
                    listOf<Any?>(regiao, uf, cidade, isp, limite)
                },
                combine(
                    preferenciasAppRepository.velocidadeContratadaDownMbpsFlow,
                    preferenciasAppRepository.velocidadeContratadaUpMbpsFlow,
                ) { down, up -> listOf<Any?>(down, up) },
            ) { primeiro, segundo, terceiro ->
                PreferenciasPerfilProvedorUiState(
                    nomeUsuario = primeiro[0] as String,
                    fotoUriUsuario = primeiro[1] as String?,
                    operadora = primeiro[2] as String,
                    planoInternet = primeiro[3] as String,
                    regiao = segundo[0] as String,
                    estadoUf = segundo[1] as String,
                    cidadeNome = segundo[2] as String,
                    ispConfirmado = segundo[3] as Boolean,
                    limiteAlertaMbps = segundo[4] as Int,
                    velocidadeContratadaDownMbps = terceiro[0] as Int,
                    velocidadeContratadaUpMbps = terceiro[1] as Int,
                )
            }.distinctUntilChanged()
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    PreferenciasPerfilProvedorUiState(),
                )
        }

        /** Controles de speedtest em rede movel: 2 flows para 1 subscricao. */
        val preferenciasSpeedtestMovel: StateFlow<PreferenciasSpeedtestMovelUiState> by lazy {
            combine(
                preferenciasAppRepository.speedtestPermiteHeavyMovel,
                preferenciasAppRepository.speedtestMbConsumidosMes,
            ) { permite, mb ->
                PreferenciasSpeedtestMovelUiState(permiteHeavy = permite, mbConsumidosMes = mb)
            }.distinctUntilChanged()
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    PreferenciasSpeedtestMovelUiState(),
                )
        }

        /** distinctUntilChanged: toggle boolean que pode oscilar para o mesmo valor. */
        val monitoramentoAtivo: StateFlow<Boolean> by lazy {
            preferenciasAppRepository.monitoramentoAtivoFlow
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
        }

        // Speedtest em rede medida — emite o modo aguardando confirmacao do usuario, null caso contrario
        private val _speedtestPendenteModoMovel = MutableStateFlow<ModoSpeedtest?>(null)
        val speedtestPendenteModoMovel: StateFlow<ModoSpeedtest?> = _speedtestPendenteModoMovel

        fun verificarDisponibilidadeGemma() {
            viewModelScope.launch {
                gemmaAvailable.value = signallQOrchestrator.checkAiAvailability()
            }
        }

        val apelidos by lazy {
            bancoDados
                .apelidoDispositivoDao()
                .observarTodos()
                // Filtra entidades sem apelido (registradas silenciosamente para supressao de
                // notificacao de dispositivo novo). So inclui no mapa dispositivos com apelido definido.
                .map { list -> list.mapNotNull { e -> e.apelido?.let { ap -> e.mac to ap } }.toMap() }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
        }

        val localIp = MutableStateFlow<UiState<String>>(UiState.Loading)
        val publicIp = MutableStateFlow<UiState<String>>(UiState.Loading)
        val ispInfo = MutableStateFlow<UiState<IspInfo>>(UiState.Loading)
        val gateways = MutableStateFlow<List<GatewayInfo>>(emptyList())
        val history = MutableStateFlow<List<HistoryPoint>>(emptyList())
        val historico = MutableStateFlow<List<MedicaoEntity>>(emptyList())

        // ── Filtros do Histórico (#95) ─────────────────────────────────────────
        private val _filtroConexaoHistorico = MutableStateFlow(FiltroConexaoHistorico.TODOS)
        val filtroConexaoHistorico: StateFlow<FiltroConexaoHistorico> = _filtroConexaoHistorico

        private val _filtroOperadoraHistorico = MutableStateFlow<String?>(null)
        val filtroOperadoraHistorico: StateFlow<String?> = _filtroOperadoraHistorico

        val historicoFiltrado: StateFlow<List<MedicaoEntity>> =
            combine(
                historico,
                _filtroConexaoHistorico,
                _filtroOperadoraHistorico,
            ) { lista, filtroConexao, filtroOp ->
                lista
                    .filter { m ->
                        when (filtroConexao) {
                            FiltroConexaoHistorico.TODOS -> true
                            FiltroConexaoHistorico.WIFI -> m.connectionType == "wifi"
                            FiltroConexaoHistorico.MOVEL -> m.connectionType == EstadoConexao.movel.name
                        }
                    }.filter { m -> filtroOp == null || m.operadoraMovel == filtroOp }
            }.distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        val operadorasDisponiveisHistorico: StateFlow<List<String>> =
            historico
                .map { lista ->
                    lista
                        .filter { it.connectionType == EstadoConexao.movel.name }
                        .mapNotNull { it.operadoraMovel?.trim()?.ifBlank { null } }
                        .distinct()
                        .sorted()
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        fun setFiltroConexaoHistorico(filtro: FiltroConexaoHistorico) {
            _filtroConexaoHistorico.value = filtro
            if (filtro != FiltroConexaoHistorico.MOVEL) _filtroOperadoraHistorico.value = null
        }

        fun setFiltroOperadoraHistorico(operadora: String?) {
            _filtroOperadoraHistorico.value = operadora
        }

        val localizacaoServidor = MutableStateFlow<UiState<String>>(UiState.Loading)
        val blocoUptime = MutableStateFlow<List<BlocoUptime>>(emptyList())
        val narrativaUptime = MutableStateFlow<String>("")

        private val observadorHistorico by lazy { ObservadorHistoricoRoom(bancoDados.medicaoDao(), dispatchers.io) }

        val resumoHistorico: StateFlow<ResumoHistorico?> =
            observadorHistorico.resumoFlow
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        private val uptimeChartUseCase by lazy { UptimeChartUseCase(bancoDados.medicaoDao(), dispatchers.io) }

        private var scannerDispositivosDisparado = false
        private var scanWifiDisparado = false
        private var benchmarkDnsDisparado = false
        private var diagnosticoDisparado = false
        private var fibraDisparada = false
        private var infoLocalRedeColetada = false
        private var ispInfoColetada = false
        private var localizacaoServidorColetada = false
        private var ultimoBenchmarkDnsEpochMs: Long? = null
        private var ssidAoDispararDns: String? = null
        private var estadoConexaoAoDispararDns: EstadoConexao? = null

        init {
            iniciarObservadores()
            iniciarObservadorSimsAtivos()
        }

        private fun iniciarObservadores() {
            // Persistência do speedtest delegada ao SpeedtestPersistenceCoordinator (issues #184/#185).
            viewModelScope.launch {
                executorFibra.snapshotFlow.collect { snapshot ->
                    if (snapshot.estado != EstadoFibra.concluido) return@collect
                    val gpon = snapshot.gpon ?: return@collect
                    val fibraInput =
                        FibraDiagnosticInput(
                            rxPowerDbm = gpon.rxPowerDbm,
                            txPowerDbm = gpon.txPowerDbm,
                            temperatureCelsius = gpon.temperatureCelsius,
                            isUp = gpon.isUp,
                        )
                    val ultimaMedicao =
                        bancoDados
                            .medicaoDao()
                            .observarUltimas(1)
                            .first()
                            .firstOrNull()
                    val wifiSnapshot = monitorRede.snapshotFlow.value.wifiLinkSnapshot
                    val internetInput =
                        ultimaMedicao?.let {
                            InternetDiagnosticInput(
                                downloadMbps = it.downloadMbps,
                                uploadMbps = it.uploadMbps,
                                latencyMs = it.latencyMs,
                                jitterMs = it.jitterMs,
                                perdaPercentual = it.perdaPercentual,
                                bufferbloatMs = it.bufferbloatMs,
                            )
                        }
                    val wifiInput =
                        wifiSnapshot?.let { ws ->
                            WifiDiagnosticInput(
                                rssiDbm = ws.rssiDbm,
                                linkSpeedMbps = ws.linkSpeedMbps,
                                frequenciaMhz = ws.frequenciaMhz,
                            )
                        }
                    diagnosticOrchestrator.executar(internetInput, wifiInput, fibraInput)
                }
            }

            viewModelScope.launch {
                benchmarkDns.snapshotFlow.collect { snapshot ->
                    if (snapshot.estado != EstadoBenchmarkDns.concluido) return@collect
                    val melhor =
                        snapshot.resultados
                            .filter { it.tempoMs != null }
                            .minByOrNull { it.tempoMs ?: Double.MAX_VALUE }
                            ?: return@collect
                    val rede = monitorRede.snapshotFlow.value
                    val provedorAtivo = inferirProvedorAtivoDns(rede.privateDnsHostname, rede.dnsServidores)
                    val coerencia = classificarCoerenciaDns(melhor.nomeProvedor, provedorAtivo)
                    avaliadorCoerenciaDns.registrarCoerencia(coerencia)
                }
            }

            viewModelScope.launch {
                bancoDados.medicaoDao().observarUltimas(20).collect { medicoes ->
                    history.value =
                        medicoes
                            .filter { it.connectionType == EstadoConexao.wifi.name }
                            .map { HistoryPoint(it.timestampEpochMs, it.downloadMbps, it.uploadMbps) }
                }
            }

            viewModelScope.launch {
                bancoDados.medicaoDao().observarUltimas(100).collect { medicoes ->
                    historico.value = medicoes
                }
            }

            viewModelScope.launch {
                // Recarrega o grid de uptime quando chegam novas medicoes.
                // observarUltimas(1) serve como trigger — qualquer nova medicao
                // invalida o cache e gera novamente os 336 blocos.
                bancoDados.medicaoDao().observarUltimas(1).collect {
                    val blocos = withContext(dispatchers.io) { uptimeChartUseCase.gerar7dias() }
                    blocoUptime.value = blocos
                    narrativaUptime.value = UptimeNarrativaEngine.gerarNarrativa(blocos)
                }
            }

            viewModelScope.launch {
                var estadoAnterior: EstadoConexao? = null
                monitorRede.snapshotFlow.collect { snapshot ->
                    val estadoAtual = snapshot.estadoConexao
                    val ssidAtual = snapshot.wifiLinkSnapshot?.ssid
                    // Mantem monitor de telefonia sempre ativo para exibir info
                    // de chips na tela de Sinal mesmo quando conectado em Wi-Fi.
                    iniciarMonitorTelefoniaSempre()
                    if (estadoAnterior != null && estadoAtual != estadoAnterior) {
                        infoLocalRedeColetada = false
                        ispInfoColetada = false
                        gateways.value = emptyList()
                        localIp.value = UiState.Loading
                        publicIp.value = UiState.Loading
                        ispInfo.value = UiState.Loading
                        coletarInfoLocalRede()
                        launch { coletarIspInfo() }
                        ultimoBenchmarkDnsEpochMs = null
                    } else if (ssidAtual != ssidAoDispararDns && ultimoBenchmarkDnsEpochMs != null) {
                        ultimoBenchmarkDnsEpochMs = null
                    }
                    estadoAnterior = estadoAtual
                }
            }
        }

        /**
         * Atualiza [simsAtivos] com os SIMs atualmente ativos no dispositivo.
         * Chamado uma vez ao iniciar monitoramento e sempre que o snapshot movel muda.
         * Seguro: captureSimsAtivos e envolto em runCatching internamente.
         */
        private fun atualizarSimsAtivos() {
            _simsAtivos.value = monitorTelephony.captureSimsAtivos(getApplication())
        }

        /**
         * Observa mudancas no snapshot movel para manter [simsAtivos] atualizado.
         * Atualiza sempre que houver coleta — inclusive em Wi-Fi, pois a tela
         * de Sinal agora exibe info de chips independentemente do tipo de conexao.
         */
        private fun iniciarObservadorSimsAtivos() {
            viewModelScope.launch {
                monitorTelephony.snapshotFlow.collect {
                    atualizarSimsAtivos()
                }
            }
        }

        fun iniciarMonitorRede() = monitorRede.iniciar()

        fun encerrarMonitorRede() {
            monitorRede.encerrar()
            monitorTelephony.encerrar()
        }

        /**
         * Inicia o coletor de telefonia movel SOMENTE se o estado atual e movel.
         * Idempotente. Chamado pela MainActivity logo apos o usuario conceder
         * a permissao ou ja ter ela concedida.
         */
        fun iniciarMonitorTelefoniaSeMovel() {
            if (monitorRede.snapshotFlow.value.estadoConexao == EstadoConexao.movel) {
                monitorTelephony.iniciar()
            }
        }

        /**
         * Inicia o monitor de telefonia independentemente do tipo de conexao.
         * Necessario para exibir info de chips na tela de Sinal em Wi-Fi.
         */
        private fun iniciarMonitorTelefoniaSempre() {
            monitorTelephony.iniciar()
        }

        // -------------------------------------------------------------------------
        // LEGADO — Compatibilidade: reiniciarSuite, confirmarSpeedtestEmMovel,
        // cancelarSpeedtestMovel e setSpeedtestPermiteHeavyMovel duplicam logica
        // que agora vive em SpeedtestViewModel (feat/viewmodels-por-funcionalidade).
        // Mantidos aqui pois a AppShell ainda recebe lambdas via MainActivity que
        // passam por este ViewModel. Remover em PR subsequente apos migrar AppShell
        // para consumir SpeedtestViewModel diretamente.
        // Tambem remover: executarSpeedtest(), acumularMbConsumidos() e
        // _speedtestPendenteModoMovel deste ViewModel quando a migracao for concluida.
        // -------------------------------------------------------------------------

        fun reiniciarSuite(modo: ModoSpeedtest) {
            scannerDispositivosDisparado = false
            scanWifiDisparado = false
            benchmarkDnsDisparado = false
            diagnosticoDisparado = false
            fibraDisparada = false
            infoLocalRedeColetada = false
            ispInfoColetada = false
            localizacaoServidorColetada = false
            viewModelScope.launch {
                // Guarda de rede medida: se movel, modo pesado e usuario nao autorizou,
                // suspende e aguarda confirmacao via dialog (Task 4). Sem dialog agora.
                if (modo != ModoSpeedtest.fast && networkCapabilitiesProvider.isMeteredNetwork()) {
                    val permiteHeavy = preferenciasAppRepository.speedtestPermiteHeavyMovel.first()
                    if (!permiteHeavy) {
                        _speedtestPendenteModoMovel.value = modo
                        return@launch
                    }
                }
                try {
                    executarSpeedtest(modo)
                } finally {
                    acumularMbConsumidos(modo)
                    iniciarRotinasNaoSpeedtest()
                }
            }
        }

        /** Chamada pelo dialog de confirmacao (Task 4 — Lia) quando usuario aceita usar dados moveis. */
        fun confirmarSpeedtestEmMovel() {
            val modo = _speedtestPendenteModoMovel.value ?: return
            _speedtestPendenteModoMovel.value = null
            viewModelScope.launch {
                try {
                    executarSpeedtest(modo)
                } finally {
                    acumularMbConsumidos(modo)
                    iniciarRotinasNaoSpeedtest()
                }
            }
        }

        /** Chamada pelo dialog quando o usuario cancela. */
        fun cancelarSpeedtestMovel() {
            _speedtestPendenteModoMovel.value = null
        }

        /** Persiste a preferencia de permitir testes pesados em rede medida (Task 5). */
        fun setSpeedtestPermiteHeavyMovel(valor: Boolean) {
            viewModelScope.launch { preferenciasAppRepository.setSpeedtestPermiteHeavyMovel(valor) }
        }

        /**
         * Acumula MB estimados consumidos no mes corrente.
         * Reset automatico quando o mes muda em relacao ao valor salvo em [speedtestMesReferencia].
         * Estimativas: fast=10 MB, complete=25 MB, triplo=30 MB.
         */
        private fun acumularMbConsumidos(modo: ModoSpeedtest) {
            val mbEstimado =
                when (modo) {
                    ModoSpeedtest.fast -> 10L
                    ModoSpeedtest.complete -> 25L
                    ModoSpeedtest.triplo -> 30L
                }
            // Usa Calendar para compatibilidade com minSdk 24 (java.time requer API 26+ ou desugaring)
            val cal = java.util.Calendar.getInstance()
            val mesAtual = "%04d-%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1)
            viewModelScope.launch {
                // NonCancellable garante que a escrita no DataStore completa mesmo se o ViewModel
                // for destruido entre o .first() e o set — evita race condition de contagem perdida.
                withContext(kotlinx.coroutines.NonCancellable) {
                    val mesReferencia = preferenciasAppRepository.speedtestMesReferencia.first()
                    val mbAcumulados =
                        if (mesReferencia == mesAtual) {
                            preferenciasAppRepository.speedtestMbConsumidosMes.first()
                        } else {
                            preferenciasAppRepository.setSpeedtestMesReferencia(mesAtual)
                            0L
                        }
                    preferenciasAppRepository.setSpeedtestMbConsumidosMes(mbAcumulados + mbEstimado)
                }
            }
        }

        fun iniciarRotinasNaoSpeedtest() {
            if (!scannerDispositivosDisparado) {
                scannerDispositivosDisparado = true
                viewModelScope.launch { scannerDispositivos.iniciarScan(profundo = false) }
            }
            if (!scanWifiDisparado) {
                scanWifiDisparado = true
                viewModelScope.launch { scannerRedesWifi.escanear() }
            }
            if (!diagnosticoDisparado) {
                diagnosticoDisparado = true
                viewModelScope.launch {
                    // Usa o resultado em memoria do speedtest como fonte primaria.
                    // O snapshotFlow e atualizado imediatamente quando o speedtest termina.
                    // O BD e o fallback para sessoes sem speedtest novo (ex.: app reaberto).
                    // Ler do BD aqui causava race condition: o save acontece em outra
                    // coroutine e pode nao ter terminado quando este bloco executa.
                    val internetInput = speedtestResultToInternetInput()
                    val wifiSnapshot = monitorRede.snapshotFlow.value.wifiLinkSnapshot
                    val wifiInput =
                        wifiSnapshot?.let {
                            WifiDiagnosticInput(
                                rssiDbm = it.rssiDbm,
                                linkSpeedMbps = it.linkSpeedMbps,
                                frequenciaMhz = it.frequenciaMhz,
                            )
                        }
                    diagnosticOrchestrator.executar(internetInput, wifiInput)
                }
            }
            if (!fibraDisparada) {
                fibraDisparada = true
                viewModelScope.launch {
                    // #127: guard de rede — fibra só faz sentido em Wi-Fi/Ethernet.
                    // Em rede móvel pura não há modem local para consultar.
                    val estadoAtual = monitorRede.snapshotFlow.value.estadoConexao
                    if (estadoAtual == EstadoConexao.movel) {
                        executorFibra.marcarSemRede()
                        return@launch
                    }
                    if (estadoAtual == EstadoConexao.desconectado) {
                        executorFibra.marcarSemRede()
                        return@launch
                    }
                    val permanecerConectado = preferenciasAppRepository.modemPermanecerConectadoFlow.first()
                    if (!permanecerConectado) return@launch
                    val host =
                        preferenciasAppRepository.modemHostFlow.first()
                            ?: gateways.value.firstOrNull()?.ip
                            ?: return@launch
                    val username = preferenciasAppRepository.modemUsernameFlow.first()
                    val password = preferenciasAppRepository.modemPasswordFlow.first()
                    executorFibra.executar(host, username, password)
                }
            }
            if (!infoLocalRedeColetada) {
                infoLocalRedeColetada = true
                coletarInfoLocalRede()
            }
            if (!ispInfoColetada) {
                ispInfoColetada = true
                viewModelScope.launch { coletarIspInfo() }
            }
            if (!localizacaoServidorColetada) {
                localizacaoServidorColetada = true
                viewModelScope.launch { buscarLocalizacaoServidor() }
            }
        }

        fun dispararBenchmarkDns() {
            val agora = System.currentTimeMillis()
            val expirado =
                ultimoBenchmarkDnsEpochMs == null ||
                    (agora - (ultimoBenchmarkDnsEpochMs ?: 0L)) > DNS_CACHE_TTL_MS
            if (!expirado) return
            val rede = monitorRede.snapshotFlow.value
            ultimoBenchmarkDnsEpochMs = agora
            ssidAoDispararDns = rede.wifiLinkSnapshot?.ssid
            estadoConexaoAoDispararDns = rede.estadoConexao
            viewModelScope.launch {
                benchmarkDns.executar(
                    resolvedoresAtivos = rede.dnsServidores,
                    privateDnsHostname = rede.privateDnsHostname,
                )
            }
        }

        fun enviarPerguntaDiagnostico(pergunta: String) {
            val historicoAtual = _diagChatHistorico.value
            val perguntasUsuario = historicoAtual.count { it.autor == DiagChatAutor.Usuario }
            if (perguntasUsuario >= 5) return
            if (_diagChatCarregando.value) return

            viewModelScope.launch {
                _diagChatHistorico.value = historicoAtual +
                    DiagChatEntry(autor = DiagChatAutor.Usuario, texto = pergunta)
                _diagChatCarregando.value = true

                val ctx =
                    diagAiContext ?: run {
                        val snap = diagnosticOrchestrator.snapshotFlow.value
                        val relatorio =
                            snap.relatorio ?: run {
                                _diagChatCarregando.value = false
                                return@launch
                            }
                        val connectionType =
                            snap.input?.connectionType
                                ?: io.veloo.app.feature.diagnostico.ConnectionType.desconhecido
                        DiagnosisAiContextFactory
                            .from(relatorio, snap.input, connectionType)
                            .also { diagAiContext = it }
                    }

                // Inclui histórico recente para dar contexto conversacional ao Worker.
                // O Worker detecta feedbackUsuario e muda para modo chat (resposta direta).
                val historicoContexto =
                    historicoAtual.takeLast(6).joinToString("\n") { entry ->
                        if (entry.autor == DiagChatAutor.Usuario) {
                            "Usuário: ${entry.texto.take(200)}"
                        } else {
                            "IA: ${entry.texto.take(300)}"
                        }
                    }
                val feedbackComHistorico =
                    if (historicoContexto.isNotBlank()) {
                        "Histórico:\n$historicoContexto\n\nPergunta atual: $pergunta"
                    } else {
                        pergunta
                    }
                val ctxComPergunta = ctx.copy(feedbackUsuario = feedbackComHistorico.take(1000))
                val snapAtual = diagnosticOrchestrator.snapshotFlow.value
                val relatorio = snapAtual.relatorio

                // Cria entrada parcial da IA ANTES de receber dados
                val tsEntradaIa = System.currentTimeMillis()
                val entradaIa =
                    DiagChatEntry(
                        autor = DiagChatAutor.Ia,
                        texto = "",
                        nomeModelo = "SignallQ IA",
                        isParcial = true,
                        timestamp = tsEntradaIa,
                    )
                _diagChatHistorico.value = _diagChatHistorico.value + entradaIa

                var textoAcumulado = ""
                var primeiroChunk = true

                try {
                    diagAiRepository.explainDiagnosisStream(ctxComPergunta).collect { token ->
                        textoAcumulado += token
                        if (primeiroChunk) {
                            _diagChatCarregando.value = false // dots pulsantes somem no 1o chunk
                            primeiroChunk = false
                        }
                        _diagChatHistorico.value = _diagChatHistorico.value.dropLast(1) +
                            entradaIa.copy(texto = textoAcumulado, isParcial = true)
                    }
                    // Stream completo — marcar como nao-parcial
                    if (textoAcumulado.isNotBlank()) {
                        _diagChatHistorico.value = _diagChatHistorico.value.dropLast(1) +
                            entradaIa.copy(texto = textoAcumulado, isParcial = false)
                    } else {
                        // Stream vazio (Worker nao suporta SSE) — fallback para resposta completa
                        _diagChatHistorico.value = _diagChatHistorico.value.dropLast(1)
                        val resultado =
                            if (relatorio != null) {
                                diagAiRepository.explainDiagnosis(ctxComPergunta) {
                                    AiFallbackFactory.fromLocal(relatorio)
                                }
                            } else {
                                AiDiagnosisState.error("sem_relatorio")
                            }
                        val (textoResposta, nomeModelo, isErro) =
                            when (resultado) {
                                is AiDiagnosisState.success ->
                                    Triple(
                                        resultado.result.textoLaudo.ifBlank { resultado.result.resumo },
                                        resultado.result.modeloIa.nomeExibicao
                                            .ifBlank { "SignallQ IA" },
                                        false,
                                    )
                                else -> Triple("", null, true)
                            }
                        _diagChatHistorico.value = _diagChatHistorico.value +
                            DiagChatEntry(
                                autor = DiagChatAutor.Ia,
                                texto = textoResposta,
                                nomeModelo = nomeModelo,
                                isErro = isErro,
                            )
                    }
                } catch (e: Exception) {
                    // Fallback: stream falhou — tenta resposta completa via explainDiagnosis()
                    _diagChatHistorico.value = _diagChatHistorico.value.dropLast(1)
                    try {
                        val resultado =
                            if (relatorio != null) {
                                diagAiRepository.explainDiagnosis(ctxComPergunta) {
                                    AiFallbackFactory.fromLocal(relatorio)
                                }
                            } else {
                                AiDiagnosisState.error("sem_relatorio")
                            }
                        val (textoResposta, nomeModelo, isErro) =
                            when (resultado) {
                                is AiDiagnosisState.success ->
                                    Triple(
                                        resultado.result.textoLaudo.ifBlank { resultado.result.resumo },
                                        resultado.result.modeloIa.nomeExibicao
                                            .ifBlank { "SignallQ IA" },
                                        false,
                                    )
                                else -> Triple("", null, true)
                            }
                        _diagChatHistorico.value = _diagChatHistorico.value +
                            DiagChatEntry(
                                autor = DiagChatAutor.Ia,
                                texto = textoResposta,
                                nomeModelo = nomeModelo,
                                isErro = isErro,
                            )
                    } catch (_: Exception) {
                        _diagChatHistorico.value = _diagChatHistorico.value +
                            DiagChatEntry(autor = DiagChatAutor.Ia, texto = "", isErro = true)
                    }
                } finally {
                    _diagChatCarregando.value = false
                }
            }
        }

        fun limparDiagChat() {
            _diagChatHistorico.value = emptyList()
            _diagChatCarregando.value = false
            diagAiContext = null
        }

        fun iniciarDiagnostico() {
            limparDiagChat()
            viewModelScope.launch {
                val internetInput = speedtestResultToInternetInput()
                val wifiSnapshot = monitorRede.snapshotFlow.value.wifiLinkSnapshot
                val wifiInput =
                    wifiSnapshot?.let { ws ->
                        WifiDiagnosticInput(
                            rssiDbm = ws.rssiDbm,
                            linkSpeedMbps = ws.linkSpeedMbps,
                            frequenciaMhz = ws.frequenciaMhz,
                        )
                    }
                diagnosticOrchestrator.executar(internetInput, wifiInput)
            }
        }

        fun reconectarFibra(
            host: String,
            username: String,
            password: String,
        ) {
            viewModelScope.launch {
                val resolvedHost =
                    host.ifBlank {
                        preferenciasAppRepository.modemHostFlow.first()
                            ?: gateways.value.firstOrNull()?.ip
                            ?: return@launch
                    }
                executorFibra.executar(resolvedHost, username, password)
            }
        }

        fun salvarConfiguracaoModem(
            host: String,
            user: String,
            pass: String,
            perm: Boolean,
        ) {
            viewModelScope.launch {
                preferenciasAppRepository.definirModemHost(host.ifBlank { null })
                preferenciasAppRepository.definirModemUsername(user)
                preferenciasAppRepository.definirModemPassword(pass)
                preferenciasAppRepository.definirModemPermanecerConectado(perm)
            }
        }

        fun definirTemaSelecionado(tema: String) {
            viewModelScope.launch { preferenciasAppRepository.definirTemaSelecionado(tema) }
        }

        fun definirAnaliseAvancada(ativa: Boolean) {
            viewModelScope.launch { preferenciasAppRepository.definirAnaliseAvancada(ativa) }
        }

        fun atualizarMonitoramento(ativo: Boolean) {
            viewModelScope.launch {
                preferenciasAppRepository.definirMonitoramentoAtivo(ativo)
                if (ativo) {
                    MonitoramentoScheduler.agendar(getApplication())
                } else {
                    MonitoramentoScheduler.cancelar(getApplication())
                }
            }
        }

        fun definirNotificacaoLatenciaAtiva(ativa: Boolean) {
            viewModelScope.launch { preferenciasAppRepository.definirNotificacaoLatenciaAtiva(ativa) }
        }

        fun definirNotificacaoDnsAtiva(ativa: Boolean) {
            viewModelScope.launch { preferenciasAppRepository.definirNotificacaoDnsAtiva(ativa) }
        }

        fun definirNotificacaoRssiAtiva(ativa: Boolean) {
            viewModelScope.launch { preferenciasAppRepository.definirNotificacaoRssiAtiva(ativa) }
        }

        fun definirNotificacaoSemInternetAtiva(ativa: Boolean) {
            viewModelScope.launch { preferenciasAppRepository.definirNotificacaoSemInternetAtiva(ativa) }
        }

        fun limparHistorico() {
            viewModelScope.launch(dispatchers.io) { bancoDados.medicaoDao().deletarTodos() }
        }

        fun apagarDadosLocais() {
            viewModelScope.launch { preferenciasAppRepository.limparTodasPreferencias() }
        }

        fun resetarApp() {
            viewModelScope.launch(dispatchers.io) {
                bancoDados.medicaoDao().deletarTodos()
                preferenciasAppRepository.limparTodasPreferencias()
            }
        }

        fun salvarPerfil(
            nome: String,
            fotoUri: String?,
        ) {
            viewModelScope.launch {
                preferenciasAppRepository.definirNomeUsuario(nome)
                preferenciasAppRepository.definirFotoUriUsuario(fotoUri)
            }
        }

        fun salvarDadosProvedor(
            op: String,
            plano: String,
            reg: String,
        ) {
            viewModelScope.launch {
                preferenciasAppRepository.definirOperadora(op)
                preferenciasAppRepository.definirPlanoInternet(plano)
                preferenciasAppRepository.definirRegiao(reg)
            }
        }

        fun salvarUltimaVersaoVista(versao: String) {
            viewModelScope.launch {
                preferenciasAppRepository.definirUltimaVersaoVista(versao)
            }
        }

        fun salvarEstadoCidade(
            estadoUf: String,
            cidadeNome: String,
        ) {
            viewModelScope.launch {
                preferenciasAppRepository.definirEstadoUf(estadoUf)
                preferenciasAppRepository.definirCidadeNome(cidadeNome)
            }
        }

        fun salvarVelocidadeContratada(
            downMbps: Int,
            upMbps: Int,
        ) {
            viewModelScope.launch {
                preferenciasAppRepository.definirVelocidadeContratadaDownMbps(downMbps)
                preferenciasAppRepository.definirVelocidadeContratadaUpMbps(upMbps)
            }
        }

        fun confirmarIspDetectado(operadora: String) {
            viewModelScope.launch {
                preferenciasAppRepository.definirOperadora(operadora)
                preferenciasAppRepository.definirIspConfirmado(true)
            }
        }

        fun dispensarBannerIsp() {
            viewModelScope.launch {
                preferenciasAppRepository.definirIspConfirmado(true)
            }
        }

        fun salvarLimiteAlerta(limite: Int) {
            viewModelScope.launch { preferenciasAppRepository.definirLimiteAlertaMbps(limite) }
        }

        fun salvarApelido(
            mac: String,
            apelido: String,
        ) {
            viewModelScope.launch {
                bancoDados.apelidoDispositivoDao().salvar(
                    ApelidoDispositivoEntity(mac = mac, apelido = apelido),
                )
            }
        }

        /**
         * Snapshot do scan de dispositivos exposto publicamente.
         * A UI pode observar este flow para exibir contagem no card Wi-Fi da Home
         * e alimentar a DispositivosScreen sem passar pelo orquestrador.
         */
        val snapshotDispositivos: StateFlow<SnapshotScanDispositivos> by lazy {
            scannerDispositivos.snapshotFlow
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), scannerDispositivos.snapshotFlow.value)
        }

        fun refreshDispositivos() {
            viewModelScope.launch { scannerDispositivos.iniciarScan() }
        }

        /**
         * Verifica se ha dispositivos novos na rede e notifica o usuario.
         *
         * Executa um scan leve (profundo=false) e compara as identidades estáveis dos dispositivos
         * encontrados com as identidades já conhecidas. Identidade estável:
         *  - Se houver MAC: "mac:<MAC em lowercase>" → persiste na tabela Room (fluxo original).
         *  - Sem MAC: "ipnome:<IP>:<nomeNormalizado>" → persiste no DataStore (sem poluir Room).
         *
         * LIMITAÇÃO DOCUMENTADA: dispositivos sem MAC têm identidade derivada de ip+nome.
         * Se o IP mudar por DHCP ou o nome mudar (ex.: reboot muda hostname), o dispositivo
         * pode ser notificado novamente como "novo". Comportamento aceitável dado que MACs
         * randomizados no Android 10+ tornam a alternativa (só MAC) pior — detectaria nada.
         *
         * Chamado no onResume da MainActivity — scan leve, sem WorkManager.
         */
        fun verificarDispositivosNovos(context: android.content.Context) {
            viewModelScope.launch(dispatchers.io) {
                try {
                    // Scan leve — nao bloqueia UI, resultado rapido via ARP + SubnetDevices
                    scannerDispositivos.iniciarScan(profundo = false)

                    val dispositivosAtuais = scannerDispositivos.snapshotFlow.value.dispositivos

                    // Identidades conhecidas: MACs do Room + identidades ip+nome do DataStore
                    val macsConhecidosRoom =
                        bancoDados.apelidoDispositivoDao().buscarTodos().map { it.mac }.toSet()
                    val identidadesConhecidas =
                        preferenciasAppRepository.buscarDispositivosConhecidos().toMutableSet()

                    val novasIdentidades = mutableSetOf<String>()

                    dispositivosAtuais.forEach { dispositivo ->
                        val identidade = identidadeEstavelDispositivo(dispositivo)
                        val mac = dispositivo.mac // val local para smart cast cross-module
                        when {
                            // Dispositivo com MAC: fluxo original via Room
                            mac != null -> {
                                val macNorm = mac.lowercase()
                                if (macNorm !in macsConhecidosRoom) {
                                    SignallQNotificationHelper.notificarDispositivoNovo(
                                        context,
                                        mac,
                                    )
                                    bancoDados.apelidoDispositivoDao().inserirSilencioso(
                                        ApelidoDispositivoEntity(mac = macNorm, apelido = null),
                                    )
                                }
                            }
                            // Dispositivo sem MAC: identidade ip+nome via DataStore
                            identidade != null && identidade !in identidadesConhecidas -> {
                                SignallQNotificationHelper.notificarDispositivoNovo(
                                    context,
                                    dispositivo.ip ?: dispositivo.nomeExibicao,
                                )
                                novasIdentidades.add(identidade)
                            }
                        }
                    }

                    // Persiste novas identidades sem MAC de uma só vez (batch)
                    if (novasIdentidades.isNotEmpty()) {
                        identidadesConhecidas.addAll(novasIdentidades)
                        preferenciasAppRepository.salvarDispositivosConhecidos(identidadesConhecidas)
                    }
                } catch (e: Exception) {
                    Timber.w("verificarDispositivosNovos falhou: ${e.message}")
                }
            }
        }

        /**
         * Retorna uma identidade estável para rastrear dispositivos entre scans.
         *
         * - Com MAC disponível: retorna null (o fluxo via Room já cobre esse caso).
         * - Sem MAC: retorna "ipnome:<IP>:<nome normalizado>" como fallback.
         *   LIMITAÇÃO: pode gerar falso-novo se IP mudar por DHCP ou nome mudar por reboot.
         */
        internal fun identidadeEstavelDispositivo(dispositivo: io.veloo.app.feature.devices.DispositivoRede): String? {
            if (dispositivo.mac != null) return null // MAC presente → fluxo Room, não precisa de identidade DataStore
            val ip = dispositivo.ip ?: return null
            val nome = dispositivo.nomeExibicao.trim().lowercase()
            return "ipnome:$ip:$nome"
        }

        fun refreshSinal() {
            viewModelScope.launch { scannerRedesWifi.escanear() }
        }

        fun iniciarSignallQ(
            foco: String? = null,
            forcarNovoSpeedtest: Boolean = false,
        ) {
            viewModelScope.launch { signallQOrchestrator.iniciarDiagnostico(foco, forcarNovoSpeedtest) }
        }

        fun iniciarSignallQComResultado(
            resultado: io.veloo.app.feature.speedtest.ResultadoSpeedtest,
            foco: String? = null,
        ) {
            viewModelScope.launch { signallQOrchestrator.iniciarDiagnosticoComResultado(resultado, foco) }
        }

        /** Volta ao intent picker (Idle) sem iniciar diagnostico. */
        fun resetSignallQ() {
            signallQOrchestrator.reset()
        }

        fun selecionarChipSignallQ(chip: OpcaoResposta) {
            viewModelScope.launch { signallQOrchestrator.selecionarChip(chip) }
        }

        fun responderPerguntaSignallQ(opcao: OpcaoResposta) {
            viewModelScope.launch { signallQOrchestrator.responderPergunta(opcao) }
        }

        /** Processa mensagem digitada livremente pelo usuario no chat.
         *  Aplica guard off-topic e incrementa [userTurnCount] apenas se aprovada. */
        fun enviarMensagemTextoSignallQ(texto: String) {
            viewModelScope.launch { signallQOrchestrator.enviarMensagemTexto(texto) }
        }

        fun coletarInfoLocalRede() {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return
            val props = cm.getLinkProperties(network) ?: return
            val ipLocal =
                props.linkAddresses
                    .mapNotNull { it.address.hostAddress }
                    .firstOrNull { it.contains('.') && !it.startsWith("127.") && !it.startsWith("169.254.") }
            localIp.value = if (ipLocal != null) UiState.Success(ipLocal) else UiState.Loading

            val snapshotRede = monitorRede.snapshotFlow.value
            if (snapshotRede.estadoConexao == EstadoConexao.movel) {
                gateways.value = listOf(GatewayInfo(ip = null, name = "Antena movel", type = ConnectionNodeType.Mobile))
                return
            }

            val ssid =
                snapshotRede.wifiLinkSnapshot
                    ?.ssid
                    ?.trim('"')
                    .orEmpty()
            val redesVizinhas = scannerRedesWifi.snapshotFlow.value.redes
            val gatewayType = inferirTipoGatewayPorScan(ssid, redesVizinhas)
            val gatewayName =
                ssid.ifBlank {
                    when (gatewayType) {
                        ConnectionNodeType.WifiMesh -> "Rede Mesh"
                        ConnectionNodeType.WifiExtender -> "Repetidor"
                        else -> "Roteador"
                    }
                }
            val gatewayIps =
                props.routes
                    .mapNotNull { it.gateway?.hostAddress }
                    .filter { ip -> !ip.startsWith("0.") && !ip.startsWith("127.") && ip.contains('.') }
                    .distinct()
            gateways.value =
                if (
                    gatewayType == ConnectionNodeType.WifiMesh ||
                    gatewayType == ConnectionNodeType.WifiExtender
                ) {
                    val meshIp = gatewayIps.getOrNull(0)
                    val routerIp = gatewayIps.getOrNull(1)
                    // O Android normalmente expõe apenas uma rota default (o nó mesh ao qual o
                    // dispositivo está conectado). O roteador central por trás do mesh não tem IP
                    // visível, então só criamos o nó "Roteador" quando há de fato um segundo gateway.
                    buildList {
                        add(GatewayInfo(ip = meshIp, name = gatewayName, type = gatewayType))
                        if (routerIp != null) {
                            add(GatewayInfo(ip = routerIp, name = "Roteador", type = ConnectionNodeType.WifiRouter))
                        }
                    }
                } else {
                    gatewayIps
                        .map { ip ->
                            GatewayInfo(ip = ip, name = gatewayName, type = gatewayType)
                        }.ifEmpty {
                            listOf(GatewayInfo(ip = null, name = gatewayName, type = gatewayType))
                        }
                }
        }

        private suspend fun coletarIspInfo() =
            withContext(dispatchers.io) {
                try {
                    val connection =
                        URL("https://ipapi.co/json/")
                            .openConnection() as HttpURLConnection
                    connection.connectTimeout = 6_000
                    connection.readTimeout = 6_000
                    connection.setRequestProperty("User-Agent", "SignallQ/1.0")
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()
                    val json = JSONObject(body)
                    val ip = json.optString("ip").ifBlank { null }
                    val operadora = json.optString("org").ifBlank { null }
                    val info =
                        IspInfo(
                            ip = ip,
                            isp = operadora,
                            asn = json.optString("asn").ifBlank { null },
                            country = json.optString("country_name").ifBlank { null },
                            region = json.optString("region").ifBlank { null },
                        )
                    publicIp.value = if (ip != null) UiState.Success(ip) else UiState.Error("IP indisponivel")
                    ispInfo.value = UiState.Success(info)
                    if (monitorRede.snapshotFlow.value.estadoConexao == EstadoConexao.movel) {
                        gateways.value =
                            listOf(
                                GatewayInfo(ip = null, name = operadora ?: "Operadora", type = ConnectionNodeType.Mobile),
                            )
                    }
                } catch (e: Exception) {
                    publicIp.value = UiState.Error("Falha ao obter IP publico")
                    ispInfo.value = UiState.Error("ISP indisponivel")
                    Timber.w("coletarIspInfo falhou: ${e.message}")
                }
            }

        private suspend fun executarSpeedtest(modo: ModoSpeedtest) {
            val connectionType = monitorRede.snapshotFlow.value.estadoConexao.name
            Timber.i("iniciando modo=${modo.name} connectionType=$connectionType")
            executorSpeedtest.executar(
                modo = modo,
                connectionType = connectionType,
                connectionTypeProvider = { monitorRede.snapshotFlow.value.estadoConexao.name },
                tecnologiaProvider = { monitorTelephony.snapshotFlow.value?.tecnologia },
            )
            Timber.i("finalizado modo=${modo.name}")
        }

        /**
         * Coleta TODOS os dados brutos disponiveis no app que possam ajudar a IA a
         * diagnosticar. Chamada pelo SignallQOrchestrator antes de cada explainDiagnosis.
         *
         * Politica: dado que nao existe -> null (omitido do payload). Nao inventa.
         * NAO inclui analise local, classificacao ou rotulos. So dados crus.
         */
        private suspend fun coletarContextoAdicionalIa(): AdditionalAiContext {
            val rede = monitorRede.snapshotFlow.value
            val wifi = rede.wifiLinkSnapshot
            val isp = (ispInfo.value as? UiState.Success)?.data

            // Wi-Fi: BSSID, padrao (Wi-Fi 5/6/...) e link speed
            val wifiBssid = wifi?.bssid
            val wifiPadrao = wifi?.padraoWifi
            val wifiLinkSpeedMbps = wifi?.linkSpeedMbps

            // DNS resolver primario (IP + provedor inferido por hostname/IP)
            val dnsResolverIp = rede.dnsServidores.firstOrNull()
            val dnsResolverProvider = inferirProvedorAtivoDns(rede.privateDnsHostname, rede.dnsServidores)

            // Historico cru — ultimas 5 medicoes (sem rotulo, so numeros)
            val ultimosTestes =
                try {
                    bancoDados.medicaoDao().observarUltimas(5).first().map { m ->
                        AiTesteHistorico(
                            timestampEpochMs = m.timestampEpochMs,
                            downloadMbps = m.downloadMbps,
                            uploadMbps = m.uploadMbps,
                            latenciaMs = m.latencyMs,
                            jitterMs = m.jitterMs,
                            perdaPercentual = m.perdaPercentual,
                            connectionType = m.connectionType,
                        )
                    }
                } catch (_: Throwable) {
                    emptyList()
                }

            // Redes Wi-Fi vizinhas (scan). Pega as 15 mais fortes (RSSI maior).
            val redesProximas =
                try {
                    scannerRedesWifi.snapshotFlow.value.redes
                        .sortedByDescending { it.rssiDbm }
                        .take(15)
                        .map { rv ->
                            AiRedeVizinha(
                                ssid = rv.ssid,
                                bssid = rv.bssid,
                                rssiDbm = rv.rssiDbm,
                                frequenciaMhz = rv.frequenciaMhz,
                                canal = rv.canal,
                                seguranca = rv.seguranca.name,
                            )
                        }
                } catch (_: Throwable) {
                    emptyList()
                }

            // Dispositivo do usuario
            val dispositivos =
                AiDispositivosInfo(
                    fabricante = android.os.Build.MANUFACTURER,
                    modelo = android.os.Build.MODEL,
                    sistema = "Android",
                    versaoSO = android.os.Build.VERSION.RELEASE,
                    quantidadeNaRede =
                        scannerDispositivos.snapshotFlow.value.dispositivos.size
                            .takeIf { it > 0 },
                )

            // Telefonia movel: SO populado quando connectionType=mobile (economia
            // de bateria — em Wi-Fi/Ethernet o monitor sequer e iniciado). Quando
            // a permissao READ_PHONE_STATE foi negada, snapshot vai null e a IA
            // recebe movel: null (gracioso).
            val movel: AiMovelInfo? =
                if (rede.estadoConexao == EstadoConexao.movel) {
                    // Garante que o monitor esta rodando — idempotente.
                    monitorTelephony.iniciar()
                    monitorTelephony.snapshotFlow.value?.let { snap -> mapMovelSnapshotToAi(snap) }
                } else {
                    null
                }

            return AdditionalAiContext(
                ispNome = isp?.isp,
                ispAsn = isp?.asn,
                ipPublico = isp?.ip ?: (publicIp.value as? UiState.Success)?.data,
                ipLocal = (localIp.value as? UiState.Success)?.data,
                pais = isp?.country,
                regiao = isp?.region,
                gatewayIp = gateways.value.firstOrNull()?.ip,
                dnsResolverIp = dnsResolverIp,
                dnsResolverProvider = dnsResolverProvider,
                dnsLatenciaMs = null,
                servidorTesteCidade = (localizacaoServidor.value as? UiState.Success)?.data,
                ultimosTestesHistorico = ultimosTestes,
                redesProximas = redesProximas,
                movel = movel,
                dispositivos = dispositivos,
                privateDnsAtivo = rede.privateDnsAtivo,
                privateDnsHostname = rede.privateDnsHostname,
                wifiBssid = wifiBssid,
                wifiPadrao = wifiPadrao,
                wifiLinkSpeedMbps = wifiLinkSpeedMbps,
                speedtestExtras = null,
            )
        }

        /**
         * Converte MovelSnapshot (coreTelephony) em AiMovelInfo (featureDiagnostico).
         * Mapeamento direto; cellId/tac viram String (JSON-safe; Long pode estourar
         * Number.MAX_SAFE_INTEGER em runtimes JS — Cloudflare Worker e JS).
         */
        private fun mapMovelSnapshotToAi(snap: MovelSnapshot): AiMovelInfo =
            AiMovelInfo(
                operadora = snap.operadora,
                tecnologia = snap.tecnologia,
                rsrpDbm = snap.rsrpDbm,
                rsrqDb = snap.rsrqDb,
                sinrDb = snap.sinrDb,
                ecnoDb = snap.ecnoDb,
                bandaMovel = snap.bandaMovel,
                cellId = snap.cellId?.toString(),
                mcc = snap.mcc,
                mnc = snap.mnc,
                tac = snap.tac?.toString(),
                roaming = snap.roaming,
            )

        private fun classificarCoerenciaDns(
            melhorProvedor: String?,
            provedorAtivo: String?,
        ): String {
            if (melhorProvedor.isNullOrBlank()) return "indeterminado"
            if (provedorAtivo.isNullOrBlank()) return "semReferencia"
            return if (melhorProvedor.equals(provedorAtivo, ignoreCase = true)) "coerente" else "divergente"
        }

        private fun inferirProvedorAtivoDns(
            privateDnsHostname: String?,
            dnsServidores: List<String>,
        ): String? {
            val hostname = privateDnsHostname?.lowercase().orEmpty()
            if (hostname.contains("cloudflare")) return "cloudflare"
            if (hostname.contains("dns.google") || hostname.contains("google")) return "google"
            if (hostname.contains("quad9")) return "quad9"
            if (hostname.contains("opendns")) return "opendns"
            if (hostname.contains("adguard")) return "adguard"
            val ips = dnsServidores.map { it.trim() }.filter { it.isNotBlank() }
            if (ips.any { it == "1.1.1.1" || it == "1.0.0.1" }) return "cloudflare"
            if (ips.any { it == "8.8.8.8" || it == "8.8.4.4" }) return "google"
            if (ips.any { it == "9.9.9.9" || it == "149.112.112.112" }) return "quad9"
            if (ips.any { it == "208.67.222.222" || it == "208.67.220.220" }) return "opendns"
            if (ips.any { it == "94.140.14.14" || it == "94.140.15.15" }) return "adguard"
            return null
        }

        private suspend fun buscarLocalizacaoServidor() =
            withContext(dispatchers.io) {
                localizacaoServidor.value = UiState.Loading
                try {
                    val connection =
                        URL("https://speed.cloudflare.com/meta")
                            .openConnection() as HttpURLConnection
                    connection.connectTimeout = 6_000
                    connection.readTimeout = 6_000
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()
                    val json = JSONObject(body)
                    val cidade = json.optString("city").ifBlank { null }
                    val codigoPais = json.optString("country").ifBlank { null }
                    val local = cidade ?: codigoPais?.let { nomePaisPtBr(it) }
                    // Se local e nulo (JSON sem city/country), exibe "Cloudflare" sem cidade
                    localizacaoServidor.value = UiState.Success(if (local != null) "Cloudflare · $local" else "Cloudflare")
                } catch (_: Exception) {
                    // Falha de rede ou parse — expoe o estado de erro para a UI
                    localizacaoServidor.value = UiState.Error("Servidor indisponivel")
                }
            }

        private fun nomePaisPtBr(codigo: String): String =
            when (codigo.uppercase()) {
                "BR" -> "Brasil"
                "US" -> "Estados Unidos"
                "AR" -> "Argentina"
                "CL" -> "Chile"
                "CO" -> "Colombia"
                "PE" -> "Peru"
                "UY" -> "Uruguai"
                "PT" -> "Portugal"
                "ES" -> "Espanha"
                "GB" -> "Reino Unido"
                "DE" -> "Alemanha"
                "FR" -> "Franca"
                "JP" -> "Japao"
                "CN" -> "China"
                else -> codigo
            }

        override fun onCleared() {
            super.onCleared()
            observadorHistorico.cancel()
            monitorRede.encerrar()
            bancoDados.close()
        }

        // -------------------------------------------------------------------------
        // Helper: InternetDiagnosticInput a partir da fonte mais fresca disponivel.
        //
        // Ordem de preferencia:
        //  1. executorSpeedtest.snapshotFlow.value.resultado — atualizado imediatamente
        //     quando o speedtest termina, sem depender do commit no banco de dados.
        //  2. bancoDados.medicaoDao().observarUltimas(1) — fallback para sessoes onde
        //     nenhum speedtest foi rodado (ex.: app reaberto com historico gravado).
        //
        // Ler apenas do BD causava race condition: o save ao BD acontece numa coroutine
        // separada (observer do snapshotFlow) e pode nao ter terminado antes de
        // iniciarRotinasNaoSpeedtest() / iniciarDiagnostico() rodarem.
        // -------------------------------------------------------------------------
        private suspend fun speedtestResultToInternetInput(): InternetDiagnosticInput? {
            val resultado = executorSpeedtest.snapshotFlow.value.resultado
            if (resultado != null) {
                return InternetDiagnosticInput(
                    downloadMbps = resultado.downloadMbps,
                    uploadMbps = resultado.uploadMbps,
                    latencyMs = resultado.latenciaMs,
                    jitterMs = resultado.jitterMs,
                    perdaPercentual = resultado.perdaPercentual,
                    bufferbloatMs = resultado.bufferbloatMs,
                )
            }
            return bancoDados.medicaoDao().observarUltimas(1).first().firstOrNull()?.let {
                InternetDiagnosticInput(
                    downloadMbps = it.downloadMbps,
                    uploadMbps = it.uploadMbps,
                    latencyMs = it.latencyMs,
                    jitterMs = it.jitterMs,
                    perdaPercentual = it.perdaPercentual,
                    bufferbloatMs = it.bufferbloatMs,
                )
            }
        }

        // -------------------------------------------------------------------------
        // Data classes de UiState agrupado — usadas pelos flows combinados acima.
        // Imutaveis e comparaveis por valor (data class), permitindo que
        // distinctUntilChanged() filtre emissoes redundantes corretamente.
        // -------------------------------------------------------------------------

        data class PreferenciasModemUiState(
            val host: String? = null,
            val username: String = "userAdmin",
            val password: String = "",
            val permanecerConectado: Boolean = false,
        )

        data class PreferenciasNotificacaoUiState(
            val latenciaAtiva: Boolean = true,
            val dnsAtiva: Boolean = true,
            val rssiAtiva: Boolean = true,
            val semInternetAtiva: Boolean = true,
        )

        data class PreferenciasUiUiState(
            val temaSelecionado: String = "sistema",
            val analiseAvancada: Boolean = false,
        )

        data class PreferenciasPerfilProvedorUiState(
            val nomeUsuario: String = "",
            val fotoUriUsuario: String? = null,
            val operadora: String = "",
            val planoInternet: String = "",
            val regiao: String = "",
            val estadoUf: String = "",
            val cidadeNome: String = "",
            val ispConfirmado: Boolean = false,
            val limiteAlertaMbps: Int = 0,
            val velocidadeContratadaDownMbps: Int = 0,
            val velocidadeContratadaUpMbps: Int = 0,
        )

        data class PreferenciasSpeedtestMovelUiState(
            val permiteHeavy: Boolean = false,
            val mbConsumidosMes: Long = 0L,
        )
    }
