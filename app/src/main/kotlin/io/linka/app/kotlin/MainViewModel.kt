package io.linka.app.kotlin

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.linka.app.kotlin.core.database.ApelidoDispositivoEntity
import io.linka.app.kotlin.core.database.LinkaDatabase
import io.linka.app.kotlin.core.database.MedicaoEntity
import io.linka.app.kotlin.core.datastore.PreferenciasAppRepository
import io.linka.app.kotlin.core.network.EstadoConexao
import io.linka.app.kotlin.core.network.MonitorRede
import io.linka.app.kotlin.core.network.NetworkCapabilitiesProvider
import io.linka.app.kotlin.core.permissions.GerenciadorPermissoesRede
import io.linka.app.kotlin.core.telephony.MonitorTelephony
import io.linka.app.kotlin.core.telephony.MovelSnapshot
import io.linka.app.kotlin.feature.devices.ScannerDispositivos
import io.linka.app.kotlin.feature.diagnostico.DiagnosticOrchestrator
import io.linka.app.kotlin.feature.diagnostico.FibraDiagnosticInput
import io.linka.app.kotlin.feature.diagnostico.InternetDiagnosticInput
import io.linka.app.kotlin.feature.diagnostico.WifiDiagnosticInput
import io.linka.app.kotlin.feature.diagnostico.ai.AdditionalAiContext
import io.linka.app.kotlin.feature.diagnostico.ai.AiDispositivosInfo
import io.linka.app.kotlin.feature.diagnostico.ai.AiMovelInfo
import io.linka.app.kotlin.feature.diagnostico.ai.AiRedeVizinha
import io.linka.app.kotlin.feature.diagnostico.ai.AiTesteHistorico
import io.linka.app.kotlin.feature.diagnostico.pulse.OpcaoResposta
import io.linka.app.kotlin.monitoramento.MonitoramentoScheduler
import io.linka.app.kotlin.notificacao.LinkaNotificationHelper
import io.linka.app.kotlin.feature.dns.AvaliadorCoerenciaDns
import io.linka.app.kotlin.feature.dns.BenchmarkDns
import io.linka.app.kotlin.feature.dns.EstadoBenchmarkDns
import io.linka.app.kotlin.feature.dns.OrientadorConfiguracaoDns
import io.linka.app.kotlin.feature.fibra.EstadoFibra
import io.linka.app.kotlin.feature.fibra.ExecutorFibra
import io.linka.app.kotlin.feature.history.BlocoUptime
import io.linka.app.kotlin.feature.history.ObservadorHistoricoRoom
import io.linka.app.kotlin.feature.history.ResumoHistorico
import io.linka.app.kotlin.feature.history.UptimeChartUseCase
import io.linka.app.kotlin.feature.history.UptimeNarrativaEngine
import io.linka.app.kotlin.feature.speedtest.EstadoExecucaoSpeedtest
import io.linka.app.kotlin.feature.speedtest.ExecutorSpeedtest
import io.linka.app.kotlin.feature.speedtest.ModoSpeedtest
import io.linka.app.kotlin.feature.wifi.ScannerRedesWifi
import io.linka.app.kotlin.pulse.OrbitOrchestrator
import io.linka.app.kotlin.pulse.OrbitUiStateMapper
import io.linka.app.kotlin.ui.ConnectionNodeType
import io.linka.app.kotlin.ui.GatewayInfo
import io.linka.app.kotlin.ui.HistoryPoint
import io.linka.app.kotlin.ui.IspInfo
import io.linka.app.kotlin.ui.screen.OrbitUiState
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

@HiltViewModel
class MainViewModel @Inject constructor(
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
    private val bancoDados: LinkaDatabase,
) : AndroidViewModel(application) {

    private companion object {
        const val logTag = "LinkaSpeedtestSuite"
        const val DNS_CACHE_TTL_MS = 15 * 60 * 1_000L
    }

    private val avaliadorCoerenciaDns by lazy { AvaliadorCoerenciaDns() }
    @Suppress("unused")
    private val orientadorConfiguracaoDns by lazy { OrientadorConfiguracaoDns() }
    val diagnosticOrchestrator by lazy { DiagnosticOrchestrator() }
    val movelSnapshot: StateFlow<MovelSnapshot?> get() = monitorTelephony.snapshotFlow
    val orbitOrchestrator by lazy {
        OrbitOrchestrator(
            executorSpeedtest = executorSpeedtest,
            diagnosticOrchestrator = diagnosticOrchestrator,
            monitorRede = monitorRede,
            medicaoDao = bancoDados.medicaoDao(),
            scope = viewModelScope,
            additionalContextProvider = { coletarContextoAdicionalIa() },
            networkCapabilitiesProvider = networkCapabilitiesProvider,
        )
    }
    val orbitUiStateFlow by lazy {
        orbitOrchestrator.snapshotFlow
            .map { OrbitUiStateMapper.from(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrbitUiState.Idle)
    }

    val onboardingConcluido: StateFlow<Boolean> by lazy {
        preferenciasAppRepository.onboardingConcluidoFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    }

    fun marcarOnboardingConcluido() {
        viewModelScope.launch { preferenciasAppRepository.definirOnboardingConcluido(true) }
    }

    fun concluirOnboarding() {
        viewModelScope.launch { preferenciasAppRepository.definirOnboardingConcluido(true) }
    }

    val gemmaAvailable = MutableStateFlow(false)

    // Speedtest em rede medida — emite o modo aguardando confirmação do usuário, null caso contrário
    private val _speedtestPendenteModoMovel = MutableStateFlow<ModoSpeedtest?>(null)
    val speedtestPendenteModoMovel: StateFlow<ModoSpeedtest?> = _speedtestPendenteModoMovel

    fun verificarDisponibilidadeGemma() {
        viewModelScope.launch {
            gemmaAvailable.value = orbitOrchestrator.checkAiAvailability()
        }
    }
    val apelidos by lazy {
        bancoDados.apelidoDispositivoDao()
            .observarTodos()
            // Filtra entidades sem apelido (registradas silenciosamente para supressao de
            // notificacao de dispositivo novo). So inclui no mapa dispositivos com apelido definido.
            .map { list -> list.mapNotNull { e -> e.apelido?.let { ap -> e.mac to ap } }.toMap() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    }

    val localIp = MutableStateFlow<String?>(null)
    val publicIp = MutableStateFlow<String?>(null)
    val ispInfo = MutableStateFlow<IspInfo?>(null)
    val gateways = MutableStateFlow<List<GatewayInfo>>(emptyList())
    val history = MutableStateFlow<List<HistoryPoint>>(emptyList())
    val historico = MutableStateFlow<List<MedicaoEntity>>(emptyList())
    val localizacaoServidor = MutableStateFlow<String?>(null)
    val blocoUptime = MutableStateFlow<List<BlocoUptime>>(emptyList())
    val narrativaUptime = MutableStateFlow<String>("")

    private val observadorHistorico by lazy { ObservadorHistoricoRoom(bancoDados.medicaoDao()) }

    val resumoHistorico: StateFlow<ResumoHistorico?> = observadorHistorico.resumoFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val uptimeChartUseCase by lazy { UptimeChartUseCase(bancoDados.medicaoDao()) }

    private var scannerDispositivosDisparado = false
    private var scanWifiDisparado = false
    private var benchmarkDnsDisparado = false
    private var diagnosticoDisparado = false
    private var fibraDisparada = false
    private var infoLocalRedeColetada = false
    private var ispInfoColetada = false
    private var localizacaoServidorColetada = false
    private var ultimoResultadoPersistidoEpochMs: Long? = null
    private var ultimoBenchmarkDnsEpochMs: Long? = null
    private var ssidAoDispararDns: String? = null
    private var estadoConexaoAoDispararDns: EstadoConexao? = null

    init {
        iniciarObservadores()
    }

    private fun iniciarObservadores() {
        viewModelScope.launch {
            executorSpeedtest.snapshotFlow.collect { snapshot ->
                if (snapshot.estado == EstadoExecucaoSpeedtest.concluido) {
                    val resultado = snapshot.resultado ?: return@collect
                    if (ultimoResultadoPersistidoEpochMs == resultado.timestampEpochMs) return@collect
                    ultimoResultadoPersistidoEpochMs = resultado.timestampEpochMs
                    bancoDados.medicaoDao().salvar(
                        MedicaoEntity(
                            id = UUID.randomUUID().toString(),
                            timestampEpochMs = resultado.timestampEpochMs,
                            connectionType = monitorRede.snapshotFlow.value.estadoConexao.name,
                            connectionTypeStart = resultado.connectionTypeStart,
                            connectionTypeEnd = resultado.connectionTypeEnd,
                            contaminado = resultado.contaminado,
                            speedtestMode = resultado.modo.name,
                            specVersion = resultado.specVersion,
                            downloadMbps = resultado.downloadMbps,
                            uploadMbps = resultado.uploadMbps,
                            latencyMs = resultado.latenciaMs,
                            jitterMs = resultado.jitterMs,
                            perdaPercentual = resultado.perdaPercentual,
                            bufferbloatMs = resultado.bufferbloatMs,
                            packetLossSource = resultado.packetLossSource,
                            vereditoStreaming = resultado.diagnosticoQualidade.vereditoStreaming.name,
                            vereditoGamer = resultado.diagnosticoQualidade.vereditoGamer.name,
                            vereditoVideoChamada = resultado.diagnosticoQualidade.vereditoVideoChamada.name,
                            gargaloPrimario = resultado.diagnosticoQualidade.gargaloPrimario.name,
                            operadoraMovel = monitorTelephony.snapshotFlow.value?.operadora,
                        ),
                    )
                }
            }
        }

        viewModelScope.launch {
            executorFibra.snapshotFlow.collect { snapshot ->
                if (snapshot.estado != EstadoFibra.concluido) return@collect
                val gpon = snapshot.gpon ?: return@collect
                val fibraInput = FibraDiagnosticInput(
                    rxPowerDbm = gpon.rxPowerDbm,
                    txPowerDbm = gpon.txPowerDbm,
                    temperatureCelsius = gpon.temperatureCelsius,
                    isUp = gpon.isUp,
                )
                val ultimaMedicao = bancoDados.medicaoDao().observarUltimas(1).first().firstOrNull()
                val wifiSnapshot = monitorRede.snapshotFlow.value.wifiLinkSnapshot
                val internetInput = ultimaMedicao?.let {
                    InternetDiagnosticInput(
                        downloadMbps = it.downloadMbps,
                        uploadMbps = it.uploadMbps,
                        latencyMs = it.latencyMs,
                        jitterMs = it.jitterMs,
                        perdaPercentual = it.perdaPercentual,
                        bufferbloatMs = it.bufferbloatMs,
                    )
                }
                val wifiInput = wifiSnapshot?.let { ws ->
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
                val melhor = snapshot.resultados
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
                history.value = medicoes
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
            // Recarrega o grid de uptime quando chegam novas medições.
            // observarUltimas(1) serve como trigger — qualquer nova medicao
            // invalida o cache e gera novamente os 336 blocos.
            bancoDados.medicaoDao().observarUltimas(1).collect {
                val blocos = withContext(Dispatchers.IO) { uptimeChartUseCase.gerar7dias() }
                blocoUptime.value = blocos
                narrativaUptime.value = UptimeNarrativaEngine.gerarNarrativa(blocos)
            }
        }

        viewModelScope.launch {
            var estadoAnterior: EstadoConexao? = null
            monitorRede.snapshotFlow.collect { snapshot ->
                val estadoAtual = snapshot.estadoConexao
                val ssidAtual = snapshot.wifiLinkSnapshot?.ssid
                // Mantem monitor de telefonia ativo apenas em rede movel.
                // Evita callbacks de radio desnecessarios em Wi-Fi/Ethernet.
                if (estadoAtual == EstadoConexao.movel) {
                    iniciarMonitorTelefoniaSeMovel()
                } else {
                    monitorTelephony.encerrar()
                }
                if (estadoAnterior != null && estadoAtual != estadoAnterior) {
                    infoLocalRedeColetada = false
                    ispInfoColetada = false
                    gateways.value = emptyList()
                    localIp.value = null
                    publicIp.value = null
                    ispInfo.value = null
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

    fun reiniciarSuite(modo: ModoSpeedtest) {
        scannerDispositivosDisparado = false
        scanWifiDisparado = false
        benchmarkDnsDisparado = false
        diagnosticoDisparado = false
        fibraDisparada = false
        infoLocalRedeColetada = false
        ispInfoColetada = false
        localizacaoServidorColetada = false
        ultimoResultadoPersistidoEpochMs = null
        viewModelScope.launch {
            // Guarda de rede medida: se móvel, modo pesado e usuário não autorizou,
            // suspende e aguarda confirmação via dialog (Task 4). Sem dialog agora.
            if (modo != ModoSpeedtest.fast && networkCapabilitiesProvider.isMeteredNetwork()) {
                val permiteHeavy = preferenciasAppRepository.speedtestPermiteHeavyMovel.first()
                if (!permiteHeavy) {
                    _speedtestPendenteModoMovel.value = modo
                    return@launch
                }
            }
            try {
                executarSpeedtest(modo)
                acumularMbConsumidos(modo)
            } finally {
                iniciarRotinasNaoSpeedtest()
            }
        }
    }

    /** Chamada pelo dialog de confirmação (Task 4 — Lia) quando usuário aceita usar dados móveis. */
    fun confirmarSpeedtestEmMovel() {
        val modo = _speedtestPendenteModoMovel.value ?: return
        _speedtestPendenteModoMovel.value = null
        viewModelScope.launch {
            try {
                executarSpeedtest(modo)
                acumularMbConsumidos(modo)
            } finally {
                iniciarRotinasNaoSpeedtest()
            }
        }
    }

    /** Chamada pelo dialog quando o usuário cancela. */
    fun cancelarSpeedtestMovel() {
        _speedtestPendenteModoMovel.value = null
    }

    /** Persiste a preferência de permitir testes pesados em rede medida (Task 5). */
    fun setSpeedtestPermiteHeavyMovel(valor: Boolean) {
        viewModelScope.launch { preferenciasAppRepository.setSpeedtestPermiteHeavyMovel(valor) }
    }

    /**
     * Acumula MB estimados consumidos no mês corrente.
     * Reset automático quando o mês muda em relação ao valor salvo em [speedtestMesReferencia].
     * Estimativas: fast=10 MB, complete=25 MB, triplo=30 MB.
     */
    private fun acumularMbConsumidos(modo: ModoSpeedtest) {
        val mbEstimado = when (modo) {
            ModoSpeedtest.fast -> 10L
            ModoSpeedtest.complete -> 25L
            ModoSpeedtest.triplo -> 30L
        }
        // Usa Calendar para compatibilidade com minSdk 24 (java.time requer API 26+ ou desugaring)
        val cal = java.util.Calendar.getInstance()
        val mesAtual = "%04d-%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1)
        viewModelScope.launch {
            // NonCancellable garante que a escrita no DataStore completa mesmo se o ViewModel
            // for destruído entre o .first() e o set — evita race condition de contagem perdida.
            withContext(kotlinx.coroutines.NonCancellable) {
                val mesReferencia = preferenciasAppRepository.speedtestMesReferencia.first()
                val mbAcumulados = if (mesReferencia == mesAtual) {
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
                // Usa o resultado em memória do speedtest como fonte primária.
                // O snapshotFlow é atualizado imediatamente quando o speedtest termina.
                // O BD é o fallback para sessões sem speedtest novo (ex.: app reaberto).
                // Ler do BD aqui causava race condition: o save acontece em outra
                // coroutine e pode não ter terminado quando este bloco executa.
                val internetInput = speedtestResultToInternetInput()
                val wifiSnapshot = monitorRede.snapshotFlow.value.wifiLinkSnapshot
                val wifiInput = wifiSnapshot?.let {
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
                val permanecerConectado = preferenciasAppRepository.modemPermanecerConectadoFlow.first()
                if (!permanecerConectado) return@launch
                if (monitorRede.snapshotFlow.value.estadoConexao == EstadoConexao.desconectado) {
                    executorFibra.marcarSemRede()
                } else {
                    val host = preferenciasAppRepository.modemHostFlow.first()
                        ?: gateways.value.firstOrNull()?.ip
                        ?: return@launch
                    val username = preferenciasAppRepository.modemUsernameFlow.first()
                    val password = preferenciasAppRepository.modemPasswordFlow.first()
                    executorFibra.executar(host, username, password)
                }
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
        val expirado = ultimoBenchmarkDnsEpochMs == null ||
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

    fun iniciarDiagnostico() {
        viewModelScope.launch {
            val internetInput = speedtestResultToInternetInput()
            val wifiSnapshot = monitorRede.snapshotFlow.value.wifiLinkSnapshot
            val wifiInput = wifiSnapshot?.let { ws ->
                WifiDiagnosticInput(
                    rssiDbm = ws.rssiDbm,
                    linkSpeedMbps = ws.linkSpeedMbps,
                    frequenciaMhz = ws.frequenciaMhz,
                )
            }
            diagnosticOrchestrator.executar(internetInput, wifiInput)
        }
    }

    fun reconectarFibra(host: String, username: String, password: String) {
        viewModelScope.launch {
            val resolvedHost = host.ifBlank {
                preferenciasAppRepository.modemHostFlow.first()
                    ?: gateways.value.firstOrNull()?.ip
                    ?: return@launch
            }
            executorFibra.executar(resolvedHost, username, password)
        }
    }

    fun salvarConfiguracaoModem(host: String, user: String, pass: String, perm: Boolean) {
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
            if (ativo) MonitoramentoScheduler.agendar(getApplication())
            else MonitoramentoScheduler.cancelar(getApplication())
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
        viewModelScope.launch { bancoDados.medicaoDao().deletarTodos() }
    }

    fun apagarDadosLocais() {
        viewModelScope.launch { preferenciasAppRepository.limparTodasPreferencias() }
    }

    fun resetarApp() {
        viewModelScope.launch {
            bancoDados.medicaoDao().deletarTodos()
            preferenciasAppRepository.limparTodasPreferencias()
        }
    }

    fun salvarPerfil(nome: String, fotoUri: String?) {
        viewModelScope.launch {
            preferenciasAppRepository.definirNomeUsuario(nome)
            preferenciasAppRepository.definirFotoUriUsuario(fotoUri)
        }
    }

    fun salvarDadosProvedor(op: String, plano: String, reg: String) {
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

    fun salvarEstadoCidade(estadoUf: String, cidadeNome: String) {
        viewModelScope.launch {
            preferenciasAppRepository.definirEstadoUf(estadoUf)
            preferenciasAppRepository.definirCidadeNome(cidadeNome)
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

    fun salvarApelido(mac: String, apelido: String) {
        viewModelScope.launch {
            bancoDados.apelidoDispositivoDao().salvar(
                ApelidoDispositivoEntity(mac = mac, apelido = apelido),
            )
        }
    }

    fun refreshDispositivos() {
        viewModelScope.launch { scannerDispositivos.iniciarScan() }
    }

    /**
     * Verifica se ha dispositivos novos na rede e notifica o usuario.
     *
     * Executa um scan leve (profundo=false) e compara os MACs encontrados
     * com os MACs ja conhecidos no banco (com ou sem apelido). Novos MACs
     * sao notificados e registrados silenciosamente para nao repetir a notificacao.
     *
     * Chamado no onResume da MainActivity — scan leve, sem WorkManager.
     */
    fun verificarDispositivosNovos(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Scan leve — nao bloqueia UI, resultado rapido via ARP cache
                scannerDispositivos.iniciarScan(profundo = false)

                val dispositivosAtuais = scannerDispositivos.snapshotFlow.value.dispositivos
                val macsConhecidos = bancoDados.apelidoDispositivoDao().buscarTodos().map { it.mac }.toSet()

                val novosMACs = dispositivosAtuais
                    .mapNotNull { it.mac }
                    .filter { mac -> mac !in macsConhecidos }

                novosMACs.forEach { mac ->
                    LinkaNotificationHelper.notificarDispositivoNovo(context, mac)
                    bancoDados.apelidoDispositivoDao().inserirSilencioso(
                        ApelidoDispositivoEntity(mac = mac, apelido = null),
                    )
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "verificarDispositivosNovos falhou: ${e.message}")
            }
        }
    }

    fun refreshSinal() {
        viewModelScope.launch { scannerRedesWifi.escanear() }
    }

    fun iniciarOrbit(foco: String? = null, forcarNovoSpeedtest: Boolean = false) {
        viewModelScope.launch { orbitOrchestrator.iniciarDiagnostico(foco, forcarNovoSpeedtest) }
    }

    fun iniciarOrbitComResultado(resultado: io.linka.app.kotlin.feature.speedtest.ResultadoSpeedtest, foco: String? = null) {
        viewModelScope.launch { orbitOrchestrator.iniciarDiagnosticoComResultado(resultado, foco) }
    }

    /** Volta ao intent picker (Idle) sem iniciar diagnóstico. */
    fun resetOrbit() {
        orbitOrchestrator.reset()
    }

    fun selecionarChipOrbit(chip: OpcaoResposta) {
        viewModelScope.launch { orbitOrchestrator.selecionarChip(chip) }
    }

    fun responderPerguntaOrbit(opcao: OpcaoResposta) {
        viewModelScope.launch { orbitOrchestrator.responderPergunta(opcao) }
    }

    /** Processa mensagem digitada livremente pelo usuário no chat.
     *  Aplica guard off-topic e incrementa [userTurnCount] apenas se aprovada. */
    fun enviarMensagemTextoOrbit(texto: String) {
        viewModelScope.launch { orbitOrchestrator.enviarMensagemTexto(texto) }
    }

    fun coletarInfoLocalRede() {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return
        val props = cm.getLinkProperties(network) ?: return
        localIp.value = props.linkAddresses
            .mapNotNull { it.address.hostAddress }
            .firstOrNull { it.contains('.') && !it.startsWith("127.") && !it.startsWith("169.254.") }

        val snapshotRede = monitorRede.snapshotFlow.value
        if (snapshotRede.estadoConexao == EstadoConexao.movel) {
            gateways.value = listOf(GatewayInfo(ip = null, name = "Antena móvel", type = ConnectionNodeType.mobile))
            return
        }

        val ssid = snapshotRede.wifiLinkSnapshot?.ssid?.trim('"').orEmpty()
        val redesVizinhas = scannerRedesWifi.snapshotFlow.value.redes
        val gatewayType = inferirTipoGatewayPorScan(ssid, redesVizinhas)
        val gatewayName = ssid.ifBlank {
            when (gatewayType) {
                ConnectionNodeType.wifiMesh -> "Rede Mesh"
                ConnectionNodeType.wifiExtender -> "Repetidor"
                else -> "Roteador"
            }
        }
        val gatewayIps = props.routes
            .mapNotNull { it.gateway?.hostAddress }
            .filter { ip -> !ip.startsWith("0.") && !ip.startsWith("127.") && ip.contains('.') }
            .distinct()
        gateways.value = if (
            gatewayType == ConnectionNodeType.wifiMesh ||
            gatewayType == ConnectionNodeType.wifiExtender
        ) {
            val meshIp = gatewayIps.getOrNull(0)
            val routerIp = gatewayIps.getOrNull(1)
            listOf(
                GatewayInfo(ip = meshIp, name = gatewayName, type = gatewayType),
                GatewayInfo(ip = routerIp, name = "Roteador", type = ConnectionNodeType.wifiRouter),
            )
        } else {
            gatewayIps.map { ip ->
                GatewayInfo(ip = ip, name = gatewayName, type = gatewayType)
            }.ifEmpty {
                listOf(GatewayInfo(ip = null, name = gatewayName, type = gatewayType))
            }
        }
    }

    private suspend fun coletarIspInfo() = withContext(Dispatchers.IO) {
        try {
            val connection = URL("https://ip-api.com/json?fields=query,isp,as,country,regionName")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = 6_000
            connection.readTimeout = 6_000
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val json = JSONObject(body)
            val ip = json.optString("query").ifBlank { null }
            publicIp.value = ip
            val operadora = json.optString("isp").ifBlank { null }
            ispInfo.value = IspInfo(
                ip = ip,
                isp = operadora,
                asn = json.optString("as").ifBlank { null },
                country = json.optString("country").ifBlank { null },
                region = json.optString("regionName").ifBlank { null },
            )
            if (monitorRede.snapshotFlow.value.estadoConexao == EstadoConexao.movel) {
                gateways.value = listOf(
                    GatewayInfo(ip = null, name = operadora ?: "Operadora", type = ConnectionNodeType.mobile),
                )
            }
        } catch (_: Exception) {}
    }

    private suspend fun executarSpeedtest(modo: ModoSpeedtest) {
        val connectionType = monitorRede.snapshotFlow.value.estadoConexao.name
        Log.i(logTag, "iniciando modo=${modo.name} connectionType=$connectionType")
        executorSpeedtest.executar(
            modo = modo,
            connectionType = connectionType,
            connectionTypeProvider = { monitorRede.snapshotFlow.value.estadoConexao.name },
        )
        Log.i(logTag, "finalizado modo=${modo.name}")
    }

    /**
     * Coleta TODOS os dados brutos disponíveis no app que possam ajudar a IA a
     * diagnosticar. Chamada pelo OrbitOrchestrator antes de cada `explainDiagnosis`.
     *
     * Política: dado que não existe → null (omitido do payload). Não inventa.
     * NÃO inclui análise local, classificação ou rótulos. Só dados crus.
     */
    private suspend fun coletarContextoAdicionalIa(): AdditionalAiContext {
        val rede = monitorRede.snapshotFlow.value
        val wifi = rede.wifiLinkSnapshot
        val isp = ispInfo.value

        // Wi-Fi: BSSID, padrão (Wi-Fi 5/6/...) e link speed
        val wifiBssid = wifi?.bssid
        val wifiPadrao = wifi?.padraoWifi
        val wifiLinkSpeedMbps = wifi?.linkSpeedMbps

        // DNS resolver primário (IP + provedor inferido por hostname/IP)
        val dnsResolverIp = rede.dnsServidores.firstOrNull()
        val dnsResolverProvider = inferirProvedorAtivoDns(rede.privateDnsHostname, rede.dnsServidores)

        // Histórico cru — últimas 5 medições (sem rótulo, só números)
        val ultimosTestes = try {
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
        val redesProximas = try {
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

        // Dispositivo do usuário
        val dispositivos = AiDispositivosInfo(
            fabricante = android.os.Build.MANUFACTURER,
            modelo = android.os.Build.MODEL,
            sistema = "Android",
            versaoSO = android.os.Build.VERSION.RELEASE,
            quantidadeNaRede = scannerDispositivos.snapshotFlow.value.dispositivos.size.takeIf { it > 0 },
        )

        // Telefonia movel: SO populado quando connectionType=mobile (economia
        // de bateria — em Wi-Fi/Ethernet o monitor sequer e iniciado). Quando
        // a permissao READ_PHONE_STATE foi negada, snapshot vai null e a IA
        // recebe `movel: null` (gracioso).
        val movel: AiMovelInfo? = if (rede.estadoConexao == EstadoConexao.movel) {
            // Garante que o monitor esta rodando — idempotente.
            monitorTelephony.iniciar()
            monitorTelephony.snapshotFlow.value?.let { snap -> mapMovelSnapshotToAi(snap) }
        } else null

        return AdditionalAiContext(
            ispNome = isp?.isp,
            ispAsn = isp?.asn,
            ipPublico = isp?.ip ?: publicIp.value,
            ipLocal = localIp.value,
            pais = isp?.country,
            regiao = isp?.region,
            gatewayIp = gateways.value.firstOrNull()?.ip,
            dnsResolverIp = dnsResolverIp,
            dnsResolverProvider = dnsResolverProvider,
            dnsLatenciaMs = null,
            servidorTesteCidade = localizacaoServidor.value,
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
    private fun mapMovelSnapshotToAi(snap: MovelSnapshot): AiMovelInfo = AiMovelInfo(
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

    private fun classificarCoerenciaDns(melhorProvedor: String?, provedorAtivo: String?): String {
        if (melhorProvedor.isNullOrBlank()) return "indeterminado"
        if (provedorAtivo.isNullOrBlank()) return "semReferencia"
        return if (melhorProvedor.equals(provedorAtivo, ignoreCase = true)) "coerente" else "divergente"
    }

    private fun inferirProvedorAtivoDns(privateDnsHostname: String?, dnsServidores: List<String>): String? {
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

    private suspend fun buscarLocalizacaoServidor() = withContext(Dispatchers.IO) {
        try {
            val connection = URL("https://speed.cloudflare.com/meta")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = 6_000
            connection.readTimeout = 6_000
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()
            val json = JSONObject(body)
            val cidade = json.optString("city").ifBlank { null }
            val codigoPais = json.optString("country").ifBlank { null }
            val local = cidade ?: codigoPais?.let { nomePaisPtBr(it) }
            // Se local é nulo (JSON sem city/country), exibe "Cloudflare" sem cidade
            // para sair do estado de carregamento indefinido
            localizacaoServidor.value = if (local != null) "Cloudflare · $local" else "Cloudflare"
        } catch (_: Exception) {
            // Falha de rede ou parse — exibe "Cloudflare" para não ficar em "Carregando..."
            localizacaoServidor.value = "Cloudflare"
        }
    }

    private fun nomePaisPtBr(codigo: String): String = when (codigo.uppercase()) {
        "BR" -> "Brasil"
        "US" -> "Estados Unidos"
        "AR" -> "Argentina"
        "CL" -> "Chile"
        "CO" -> "Colômbia"
        "PE" -> "Peru"
        "UY" -> "Uruguai"
        "PT" -> "Portugal"
        "ES" -> "Espanha"
        "GB" -> "Reino Unido"
        "DE" -> "Alemanha"
        "FR" -> "França"
        "JP" -> "Japão"
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
    // Helper: InternetDiagnosticInput a partir da fonte mais fresca disponível.
    //
    // Ordem de preferência:
    //  1. executorSpeedtest.snapshotFlow.value.resultado — atualizado imediatamente
    //     quando o speedtest termina, sem depender do commit no banco de dados.
    //  2. bancoDados.medicaoDao().observarUltimas(1) — fallback para sessões onde
    //     nenhum speedtest foi rodado (ex.: app reaberto com histórico gravado).
    //
    // Ler apenas do BD causava race condition: o save ao BD acontece numa coroutine
    // separada (observer do snapshotFlow) e pode não ter terminado antes de
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
}
