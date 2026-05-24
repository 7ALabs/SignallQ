package io.linka.app.kotlin.pulse

import io.linka.app.kotlin.core.database.MedicaoDao
import io.linka.app.kotlin.core.network.GatewayLatencyMeasurer
import io.linka.app.kotlin.core.network.MonitorRede
import io.linka.app.kotlin.core.network.NetworkCapabilitiesProvider
import io.linka.app.kotlin.feature.diagnostico.ConnectionType
import io.linka.app.kotlin.feature.diagnostico.DiagnosticInput
import io.linka.app.kotlin.feature.diagnostico.DiagnosticOrchestrator
import io.linka.app.kotlin.feature.diagnostico.EstadoDiagnostico
import io.linka.app.kotlin.feature.diagnostico.InternetDiagnosticInput
import io.linka.app.kotlin.feature.diagnostico.WifiDiagnosticInput
import io.linka.app.kotlin.feature.diagnostico.ai.AdditionalAiContext
import io.linka.app.kotlin.feature.diagnostico.ai.AiDiagnosisRepository
import io.linka.app.kotlin.feature.diagnostico.ai.AiDiagnosisState
import io.linka.app.kotlin.feature.diagnostico.ai.AiFallbackFactory
import io.linka.app.kotlin.feature.diagnostico.ai.DiagnosisAiContextFactory
import io.linka.app.kotlin.feature.diagnostico.pulse.AiAnalysisEntry
import io.linka.app.kotlin.feature.diagnostico.pulse.ContextAccumulator
import io.linka.app.kotlin.feature.diagnostico.pulse.DynamicQuestionEngine
import io.linka.app.kotlin.feature.diagnostico.pulse.IntelligentDiagnosticSession
import io.linka.app.kotlin.feature.diagnostico.pulse.OpcaoResposta
import io.linka.app.kotlin.feature.diagnostico.pulse.OrbitInsightGenerator
import io.linka.app.kotlin.feature.diagnostico.pulse.OrbitState
import io.linka.app.kotlin.feature.diagnostico.pulse.QuestionAnswer
import io.linka.app.kotlin.feature.diagnostico.pulse.ResponseSource
import io.linka.app.kotlin.feature.diagnostico.pulse.RotatingMessageProvider
import io.linka.app.kotlin.feature.diagnostico.pulse.SnapshotOrbit
import io.linka.app.kotlin.feature.speedtest.DiagnosticoFasesSpeedtest
import io.linka.app.kotlin.feature.speedtest.EstadoExecucaoSpeedtest
import io.linka.app.kotlin.feature.speedtest.ExecutorSpeedtest
import io.linka.app.kotlin.feature.speedtest.ModoSpeedtest
import io.linka.app.kotlin.feature.speedtest.ResultadoSpeedtest
import io.linka.app.kotlin.feature.speedtest.SpeedtestQualityClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.UUID

private const val TAG = "OrbitOrchestrator"
private const val AI_BASE_URL = "https://linka-ai-diagnosis-worker.giammattey-luiz.workers.dev"
private const val MESSAGE_ROTATION_INTERVAL_MS = 2500L
private const val SPEEDTEST_REUSE_WINDOW_MS = 10 * 60 * 1000L

/** Limite de turnos do usuário por sessão. Ao atingir este valor, o input é bloqueado. */
private const val USER_TURN_LIMIT = 5

/** Palavras-chave que indicam mensagem fora do escopo de rede/internet (PT-BR). */
private val OFF_TOPIC_KEYWORDS =
    listOf(
        "receita",
        "culinária",
        "cozinhar",
        "comida",
        "bolo",
        "pão",
        "macarrão",
        "futebol",
        "esporte",
        "jogo",
        "copa",
        "política",
        "religião",
        "deus",
        "medicina",
        "saúde",
        "dor",
        "doença",
        "remédio",
        "sintoma",
        "hospital",
        "amor",
        "relacionamento",
        "namoro",
        "psicologia",
        "terapia",
        "investimento",
        "bolsa",
        "ação",
        "cripto",
        "bitcoin",
        "notícia",
        "governo",
        "eleição",
        "filme",
        "série",
        "música",
        "entretenimento",
    )

private fun isOffTopic(text: String): Boolean {
    val lower = text.lowercase()
    return OFF_TOPIC_KEYWORDS.any { lower.contains(it) }
}

class OrbitOrchestrator(
    private val executorSpeedtest: ExecutorSpeedtest,
    private val diagnosticOrchestrator: DiagnosticOrchestrator,
    private val monitorRede: MonitorRede,
    private val medicaoDao: MedicaoDao,
    private val questionEngine: DynamicQuestionEngine = DynamicQuestionEngine(),
    private val scope: CoroutineScope,
    /** Lambda que coleta contexto bruto adicional (ISP, IP, DNS, histórico,
     *  dispositivo, etc.) do MainViewModel/host. Default vazio — o payload
     *  ainda funciona, só fica mais enxuto. */
    private val additionalContextProvider: suspend () -> AdditionalAiContext = { AdditionalAiContext() },
    /** Measurer de RTT TCP para o gateway local. Default funciona sem config adicional. */
    private val gatewayLatencyMeasurer: GatewayLatencyMeasurer = GatewayLatencyMeasurer(),
    /** Provedor de capacidades de rede — usado para forcar modo fast em rede medida
     *  no speedtest silencioso (sem dialog, sem interrompimento do fluxo do OrbitOrchestrator). */
    private val networkCapabilitiesProvider: NetworkCapabilitiesProvider? = null,
) {
    private val aiRepository =
        AiDiagnosisRepository(
            baseUrl = AI_BASE_URL,
            isAuthorized = { true },
        )

    private val mutableSnapshotFlow = MutableStateFlow(SnapshotOrbit())
    val snapshotFlow: StateFlow<SnapshotOrbit> = mutableSnapshotFlow.asStateFlow()

    private var activeSession: IntelligentDiagnosticSession? = null
    private var messageRotationJob: Job? = null

    // ---- API pública ----

    /** Delega para [AiDiagnosisRepository.checkAvailability]. */
    suspend fun checkAiAvailability(): Boolean = aiRepository.checkAvailability()

    /**
     * Inicia diagnóstico com resultado de speedtest já disponível — pula a fase Collecting.
     * Usado quando o usuário navega da ResultadoVelocidadeScreen para o chat.
     */
    suspend fun iniciarDiagnosticoComResultado(
        resultado: ResultadoSpeedtest,
        focoDiagnostico: String? = null,
    ) {
        Timber.i("iniciarDiagnosticoComResultado foco=$focoDiagnostico dl=${resultado.downloadMbps}")
        activeSession = null
        cancelarRotacaoMensagens()

        val wifiSnapshot = monitorRede.snapshotFlow.value.wifiLinkSnapshot
        val connectionType =
            when (monitorRede.snapshotFlow.value.estadoConexao.name) {
                "wifi" -> ConnectionType.wifi
                "movel" -> ConnectionType.mobile
                else -> ConnectionType.desconhecido
            }

        // --- Fase 2: Thinking (engines locais — sem speedtest novo) ---
        emit(OrbitState.Thinking, focoDiagnostico = focoDiagnostico)
        iniciarRotacaoMensagens(OrbitState.Thinking)

        // Coleta contexto adicional cedo para usar gatewayIp no RTT gateway.
        val extraContext =
            try {
                additionalContextProvider()
            } catch (t: Throwable) {
                Timber.w("additionalContextProvider falhou na fase Thinking: ${t.message}")
                AdditionalAiContext()
            }

        val rttGatewayMs =
            extraContext.gatewayIp?.let { gw ->
                try {
                    gatewayLatencyMeasurer.measureRttGateway(gw)
                } catch (t: Throwable) {
                    Timber.w("measureRttGateway falhou: ${t.message}")
                    null
                }
            }
        Timber.d("RTT gateway=${rttGatewayMs}ms (gatewayIp=${extraContext.gatewayIp})")

        val internetInput =
            InternetDiagnosticInput(
                downloadMbps = resultado.downloadMbps,
                uploadMbps = resultado.uploadMbps,
                latencyMs = resultado.latenciaMs,
                jitterMs = resultado.jitterMs,
                perdaPercentual = resultado.perdaPercentual,
                bufferbloatMs = resultado.bufferbloatMs,
                rttGatewayMs = rttGatewayMs,
            )

        val wifiInput =
            wifiSnapshot?.let { ws ->
                WifiDiagnosticInput(
                    rssiDbm = ws.rssiDbm,
                    linkSpeedMbps = ws.linkSpeedMbps,
                    frequenciaMhz = ws.frequenciaMhz,
                    ssid = ws.ssid,
                )
            }

        withContext(Dispatchers.Default) {
            diagnosticOrchestrator.executar(internetInput, wifiInput)
        }

        val relatorio =
            withTimeoutOrNull(5_000L) {
                diagnosticOrchestrator.snapshotFlow
                    .first {
                        it.estado == EstadoDiagnostico.concluido || it.estado == EstadoDiagnostico.erro
                    }.relatorio
            }

        val contextInicial =
            ContextAccumulator.buildInitial(
                downloadMbps = resultado.downloadMbps,
                uploadMbps = resultado.uploadMbps,
                latencyMs = resultado.latenciaMs,
                jitterMs = resultado.jitterMs,
                lossPercent = resultado.perdaPercentual,
                stabilityScore = resultado.stabilityScore,
                wifiSsid = wifiSnapshot?.ssid,
                wifiRssiDbm = wifiSnapshot?.rssiDbm,
                wifiFrequencyMhz = wifiSnapshot?.frequenciaMhz,
                report = relatorio,
            )

        // --- Fase 3: Analyzing ---
        val partialSession =
            IntelligentDiagnosticSession(
                sessionId = UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis(),
                speedtestDownloadMbps = resultado.downloadMbps,
                speedtestUploadMbps = resultado.uploadMbps,
                speedtestLatencyMs = resultado.latenciaMs,
                speedtestJitterMs = resultado.jitterMs,
                speedtestLossPercent = resultado.perdaPercentual,
                speedtestStabilityScore = resultado.stabilityScore,
                wifiSsid = wifiSnapshot?.ssid,
                wifiRssiDbm = wifiSnapshot?.rssiDbm,
                wifiFrequencyMhz = wifiSnapshot?.frequenciaMhz,
                diagnosticReport = relatorio,
                questionHistory = emptyList(),
                pendingQuestion = null,
                activeChips = emptyList(),
                analyses = emptyList(),
                contextAccumulated = contextInicial,
                chipHistory = emptyList(),
                focoDiagnostico = focoDiagnostico,
            )
        activeSession = partialSession
        emit(OrbitState.Analyzing, focoDiagnostico = focoDiagnostico)
        cancelarRotacaoMensagens()
        iniciarRotacaoMensagens(OrbitState.Analyzing)

        // Emitir insights locais escalonados antes de chamar a IA
        emitirInsights(partialSession)

        val diagnosticInputSnapshot =
            DiagnosticInput(
                connectionType = connectionType,
                internet = internetInput,
                wifi = wifiInput,
            )

        // Captura os insights emitidos antes de chamar a IA
        val insightsGerados =
            activeSession
                ?.analyses
                .orEmpty()
                .filter { it.source == ResponseSource.INSIGHT }

        val aiEntry =
            callAi(
                trigger = "initial_from_result",
                report = relatorio,
                input = diagnosticInputSnapshot,
                connectionType = connectionType,
                additionalContext = focoDiagnostico,
                preCollectedExtra = extraContext,
            )

        val chips = questionEngine.getInitialChips(relatorio)
        val feedbackChips = questionEngine.getFeedbackChips()
        val chipsComFeedback = chips + feedbackChips
        val pulseState = mapOrbitState(relatorio)

        // Insights ficam antes da resposta Gemma na lista
        activeSession =
            partialSession.copy(
                activeChips = chipsComFeedback,
                analyses = insightsGerados + aiEntry,
            )

        cancelarRotacaoMensagens()
        emitSession(pulseState)
        Timber.i("iniciarDiagnosticoComResultado concluído estado=$pulseState")
    }

    suspend fun iniciarDiagnostico(
        focoDiagnostico: String? = null,
        forcarNovoSpeedtest: Boolean = false,
    ) {
        Timber.i("iniciarDiagnostico foco=$focoDiagnostico forcar=$forcarNovoSpeedtest")
        activeSession = null
        cancelarRotacaoMensagens()

        // --- Fase 1: Collecting (speedtest silencioso) ---
        emit(OrbitState.Collecting, focoDiagnostico = focoDiagnostico)
        iniciarRotacaoMensagens(OrbitState.Collecting)

        val speedtestResult = runSilentSpeedtest(forcarNovoSpeedtest)
        val wifiSnapshot = monitorRede.snapshotFlow.value.wifiLinkSnapshot
        val connectionType =
            when (monitorRede.snapshotFlow.value.estadoConexao.name) {
                "wifi" -> ConnectionType.wifi
                "movel" -> ConnectionType.mobile
                else -> ConnectionType.desconhecido
            }

        // Histórico: NÃO salvamos aqui — `MainViewModel.iniciarObservadores()`
        // já observa `executorSpeedtest.snapshotFlow` globalmente e persiste
        // todo speedtest concluído em `MedicaoDao`. Salvar aqui causava entrada
        // duplicada quando o teste vinha do Orbit (Bug #1). O observer global
        // é a fonte única de verdade.

        // --- Fase 2: Thinking (engines locais) ---
        emit(OrbitState.Thinking, focoDiagnostico = focoDiagnostico)
        iniciarRotacaoMensagens(OrbitState.Thinking)

        // Coleta contexto adicional cedo para usar gatewayIp no RTT gateway.
        // Reutilizado depois no callAi() — evita chamada dupla ao provider.
        val extraContext =
            try {
                additionalContextProvider()
            } catch (t: Throwable) {
                Timber.w("additionalContextProvider falhou na fase Thinking: ${t.message}")
                AdditionalAiContext()
            }

        // Medir RTT TCP para o gateway local (portas 80/443/53).
        // Null se gateway não disponível, Doze Mode ou emulador.
        val rttGatewayMs =
            extraContext.gatewayIp?.let { gw ->
                try {
                    gatewayLatencyMeasurer.measureRttGateway(gw)
                } catch (t: Throwable) {
                    Timber.w("measureRttGateway falhou: ${t.message}")
                    null
                }
            }
        Timber.d("RTT gateway=${rttGatewayMs}ms (gatewayIp=${extraContext.gatewayIp})")

        val internetInput =
            speedtestResult?.let {
                InternetDiagnosticInput(
                    downloadMbps = it.downloadMbps,
                    uploadMbps = it.uploadMbps,
                    latencyMs = it.latenciaMs,
                    jitterMs = it.jitterMs,
                    perdaPercentual = it.perdaPercentual,
                    bufferbloatMs = it.bufferbloatMs,
                    rttGatewayMs = rttGatewayMs,
                )
            } ?: run {
                val ultimaMedicao = medicaoDao.observarUltimas(1).first().firstOrNull()
                ultimaMedicao?.let {
                    InternetDiagnosticInput(
                        downloadMbps = it.downloadMbps,
                        uploadMbps = it.uploadMbps,
                        latencyMs = it.latencyMs,
                        jitterMs = it.jitterMs,
                        perdaPercentual = it.perdaPercentual,
                        bufferbloatMs = it.bufferbloatMs,
                        rttGatewayMs = rttGatewayMs,
                    )
                }
            }

        val wifiInput =
            wifiSnapshot?.let { ws ->
                WifiDiagnosticInput(
                    rssiDbm = ws.rssiDbm,
                    linkSpeedMbps = ws.linkSpeedMbps,
                    frequenciaMhz = ws.frequenciaMhz,
                    ssid = ws.ssid,
                )
            }

        withContext(Dispatchers.Default) {
            diagnosticOrchestrator.executar(internetInput, wifiInput)
        }

        val relatorio =
            withTimeoutOrNull(5_000L) {
                diagnosticOrchestrator.snapshotFlow
                    .first {
                        it.estado == EstadoDiagnostico.concluido || it.estado == EstadoDiagnostico.erro
                    }.relatorio
            }

        val contextInicial =
            ContextAccumulator.buildInitial(
                downloadMbps = speedtestResult?.downloadMbps,
                uploadMbps = speedtestResult?.uploadMbps,
                latencyMs = speedtestResult?.latenciaMs,
                jitterMs = speedtestResult?.jitterMs,
                lossPercent = speedtestResult?.perdaPercentual,
                stabilityScore = speedtestResult?.stabilityScore,
                wifiSsid = wifiSnapshot?.ssid,
                wifiRssiDbm = wifiSnapshot?.rssiDbm,
                wifiFrequencyMhz = wifiSnapshot?.frequenciaMhz,
                report = relatorio,
            )

        // --- Fase 3: Analyzing — criar sessão parcial ANTES de chamar a IA ---
        // A UI já exibe o card técnico enquanto a IA processa
        val partialSession =
            IntelligentDiagnosticSession(
                sessionId = UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis(),
                speedtestDownloadMbps = speedtestResult?.downloadMbps,
                speedtestUploadMbps = speedtestResult?.uploadMbps,
                speedtestLatencyMs = speedtestResult?.latenciaMs,
                speedtestJitterMs = speedtestResult?.jitterMs,
                speedtestLossPercent = speedtestResult?.perdaPercentual,
                speedtestStabilityScore = speedtestResult?.stabilityScore,
                wifiSsid = wifiSnapshot?.ssid,
                wifiRssiDbm = wifiSnapshot?.rssiDbm,
                wifiFrequencyMhz = wifiSnapshot?.frequenciaMhz,
                diagnosticReport = relatorio,
                questionHistory = emptyList(),
                pendingQuestion = null,
                activeChips = emptyList(),
                analyses = emptyList(),
                contextAccumulated = contextInicial,
                chipHistory = emptyList(),
                focoDiagnostico = focoDiagnostico,
            )
        activeSession = partialSession
        emit(OrbitState.Analyzing, focoDiagnostico = focoDiagnostico)
        cancelarRotacaoMensagens()
        iniciarRotacaoMensagens(OrbitState.Analyzing)

        // Emitir insights locais escalonados antes de chamar a IA
        emitirInsights(partialSession)

        val diagnosticInputSnapshot =
            DiagnosticInput(
                connectionType = connectionType,
                internet = internetInput,
                wifi = wifiInput,
            )

        // Captura os insights emitidos antes de chamar a IA
        val insightsGerados =
            activeSession
                ?.analyses
                .orEmpty()
                .filter { it.source == ResponseSource.INSIGHT }

        val aiEntry =
            callAi(
                trigger = "initial",
                report = relatorio,
                input = diagnosticInputSnapshot,
                connectionType = connectionType,
                // O foco escolhido pelo usuário vai como feedbackUsuario no payload.
                additionalContext = focoDiagnostico,
                preCollectedExtra = extraContext,
            )

        val chips = questionEngine.getInitialChips(relatorio)
        val feedbackChips = questionEngine.getFeedbackChips()
        val chipsComFeedback = chips + feedbackChips
        val pulseState = mapOrbitState(relatorio)

        // Insights ficam antes da resposta Gemma na lista
        activeSession =
            partialSession.copy(
                activeChips = chipsComFeedback,
                analyses = insightsGerados + aiEntry,
            )

        cancelarRotacaoMensagens()
        emitSession(pulseState)
        Timber.i("iniciarDiagnostico concluído estado=$pulseState")
    }

    suspend fun selecionarChip(chip: OpcaoResposta) {
        val session = activeSession ?: return
        Timber.i("selecionarChip id=${chip.id}")

        // --- Chips de feedback (após diagnóstico inicial) ---
        // Ir diretamente ao Gemma sem perguntas adicionais
        if (questionEngine.isFeedbackChip(chip.id)) {
            Timber.i("Chip de feedback detectado: ${chip.id}")
            val novoContexto = ContextAccumulator.appendChip(session.contextAccumulated, chip)
            val sessionComChip =
                session.copy(
                    chipHistory = session.chipHistory + chip.label,
                )
            gerarAnaliseComplementar(sessionComChip, chip.id, novoContexto)
            return
        }

        // --- Chips de análise inicial (com árvore de perguntas) ---
        val novoContexto = ContextAccumulator.appendChip(session.contextAccumulated, chip)
        val proximaPergunta = questionEngine.getNextQuestion(chip.id, emptyList())
        val isLeaf = questionEngine.isLeafAnswer(chip.id, "", emptyList()) || proximaPergunta == null

        if (isLeaf) {
            val sessionComChip =
                session.copy(
                    chipHistory = session.chipHistory + chip.label,
                )
            gerarAnaliseComplementar(sessionComChip, chip.id, novoContexto)
        } else {
            activeSession =
                session.copy(
                    activeChips = emptyList(),
                    pendingQuestion = proximaPergunta,
                    contextAccumulated = novoContexto,
                )
            emitSession(mapOrbitState(session.diagnosticReport))
        }
    }

    suspend fun responderPergunta(opcao: OpcaoResposta) {
        val session = activeSession ?: return
        val question = session.pendingQuestion ?: return
        Timber.i("responderPergunta qId=${question.id} aId=${opcao.id}")

        val novoContexto = ContextAccumulator.appendAnswer(session.contextAccumulated, question, opcao)
        val novaResposta =
            QuestionAnswer(
                questionId = question.id,
                questionText = question.texto,
                answerId = opcao.id,
                answerText = opcao.label,
                contextContribution = questionEngine.buildContextContribution(question, opcao),
            )
        val novoHistorico = session.questionHistory + novaResposta

        val chipId =
            session.questionHistory.firstOrNull()?.let {
                session.contextAccumulated
                    .lines()
                    .find { line -> line.startsWith("Categoria escolhida:") }
                    ?.substringAfter(": ")
                    ?.trim()
            } ?: opcao.id

        val isLeaf = questionEngine.isLeafAnswer(chipId, opcao.id, session.questionHistory)
        val proximaPergunta = if (isLeaf) null else questionEngine.getNextQuestion(chipId, novoHistorico)

        if (isLeaf || proximaPergunta == null) {
            gerarAnaliseComplementar(
                session.copy(
                    questionHistory = novoHistorico,
                    chipHistory = session.chipHistory + opcao.label,
                    contextAccumulated = novoContexto,
                ),
                chipId,
                novoContexto,
            )
        } else {
            activeSession =
                session.copy(
                    questionHistory = novoHistorico,
                    pendingQuestion = proximaPergunta,
                    contextAccumulated = novoContexto,
                )
            emitSession(mapOrbitState(session.diagnosticReport))
        }
    }

    /**
     * Processa uma mensagem digitada pelo usuário (campo de texto livre).
     *
     * Diferente de [selecionarChip], este método:
     * - Verifica se o assunto é off-topic antes de enviar ao Gemma.
     * - Incrementa [IntelligentDiagnosticSession.userTurnCount] SOMENTE em
     *   mensagens que passam pela verificação de escopo.
     * - Não incrementa o contador em mensagens bloqueadas por off-topic.
     * - Não faz nada quando [IntelligentDiagnosticSession.userTurnCount] >= 5.
     */
    suspend fun enviarMensagemTexto(texto: String) {
        val session = activeSession ?: return
        val textoTrimado = texto.trim()
        if (textoTrimado.isBlank()) return

        // Limite de turnos: ignora silenciosamente (a UI já bloqueia o input)
        if (session.userTurnCount >= USER_TURN_LIMIT) return

        // Guard de escopo off-topic — retorno local imediato, sem incrementar turno
        if (isOffTopic(textoTrimado)) {
            Timber.i("Mensagem off-topic bloqueada: $textoTrimado")
            val offTopicEntry =
                AiAnalysisEntry(
                    trigger = "off_topic",
                    content = "Posso ajudar apenas com dúvidas sobre sua conexão de internet, Wi-Fi, DNS e rede doméstica. Tem alguma pergunta sobre sua conexão?",
                    isFallback = false,
                    timestamp = System.currentTimeMillis(),
                    fullResult = null,
                    source = ResponseSource.LOCAL,
                )
            activeSession =
                session.copy(
                    analyses = session.analyses + offTopicEntry,
                )
            emitSession(mapOrbitState(session.diagnosticReport))
            return
        }

        // Incrementa turno ANTES de enviar ao Gemma — estado de limite imediato na UI
        val sessionComTurno =
            session.copy(
                userTurnCount = session.userTurnCount + 1,
                chipHistory = session.chipHistory + textoTrimado,
            )
        activeSession = sessionComTurno

        val novoContexto =
            ContextAccumulator.appendChip(
                session.contextAccumulated,
                OpcaoResposta(
                    id = "typed_${System.currentTimeMillis()}",
                    label = textoTrimado,
                    contextoParaIA = textoTrimado,
                ),
            )
        gerarAnaliseComplementar(sessionComTurno, "typed_message", novoContexto)
    }

    fun reset() {
        cancelarRotacaoMensagens()
        activeSession = null
        mutableSnapshotFlow.value = SnapshotOrbit()
    }

    // ---- Internos ----

    private suspend fun runSilentSpeedtest(forcarNovoSpeedtest: Boolean = false): ResultadoSpeedtest? =
        try {
            val medicaoRecente = if (!forcarNovoSpeedtest) medicaoDao.observarUltimas(1).first().firstOrNull() else null
            val resultadoReutilizado = medicaoRecente?.toReusableSpeedtestResult()
            if (resultadoReutilizado != null) {
                Timber.i("Reutilizando speedtest recente (sem execução silenciosa) timestamp=${resultadoReutilizado.timestampEpochMs}")
                resultadoReutilizado
            } else {
                val connType = monitorRede.snapshotFlow.value.estadoConexao.name
                // Em rede medida (móvel), usa modo fast para economizar dados.
                // Sem dialog — decisão silenciosa e automática dentro do OrbitOrchestrator.
                val modoEfetivo =
                    if (networkCapabilitiesProvider?.isMeteredNetwork() == true) {
                        ModoSpeedtest.fast
                    } else {
                        ModoSpeedtest.complete
                    }
                executorSpeedtest.executar(
                    modo = modoEfetivo,
                    connectionType = connType,
                    connectionTypeProvider = { monitorRede.snapshotFlow.value.estadoConexao.name },
                )
                val snap = executorSpeedtest.snapshotFlow.value
                if (snap.estado == EstadoExecucaoSpeedtest.concluido) snap.resultado else null
            }
        } catch (t: Throwable) {
            Timber.w("speedtest silencioso falhou: ${t.message}")
            null
        }

    private fun io.linka.app.kotlin.core.database.MedicaoEntity.toReusableSpeedtestResult(): ResultadoSpeedtest? {
        val now = System.currentTimeMillis()
        val isRecent = timestampEpochMs >= now - SPEEDTEST_REUSE_WINDOW_MS
        if (!isRecent || contaminado) return null
        if (speedtestMode != ModoSpeedtest.complete.name) return null

        val dl = downloadMbps ?: return null
        val ul = uploadMbps ?: return null
        val lat = latencyMs ?: return null
        val jit = jitterMs ?: return null
        val loss = perdaPercentual ?: return null
        val bloat = bufferbloatMs ?: return null

        val bufferbloatSeverity = SpeedtestQualityClassifier.classificarBufferbloat(bloat)
        val diagnosticoQualidade =
            SpeedtestQualityClassifier.classificarQualidade(
                dl = dl,
                ul = ul,
                latency = lat,
                jitter = jit,
                packetLoss = loss,
                bufferbloatDeltaMs = bloat,
                bufferbloat = bufferbloatSeverity,
            )

        return ResultadoSpeedtest(
            timestampEpochMs = timestampEpochMs,
            specVersion = specVersion ?: "legacy_room_reuse",
            modo = ModoSpeedtest.complete,
            connectionTypeStart = connectionTypeStart,
            connectionTypeEnd = connectionTypeEnd,
            contaminado = false,
            latenciaMs = lat,
            jitterMs = jit,
            perdaPercentual = loss,
            bufferbloatMs = bloat,
            severidadeBufferbloat = bufferbloatSeverity,
            downloadMbps = dl,
            uploadMbps = ul,
            latencyDownloadMs = lat,
            latencyUploadMs = lat,
            stabilityScore = 0.0,
            peakDownloadMbps = dl,
            peakUploadMbps = ul,
            packetLossSource = packetLossSource ?: "unknown",
            dnsLatencyMs = null,
            dnsResolverIp = null,
            dnsProvider = null,
            diagnosticoQualidade = diagnosticoQualidade,
            diagnosticoFases =
                DiagnosticoFasesSpeedtest(
                    faseInterrompida = "none",
                    latenciaAmostrasTotais = 0,
                    latenciaAmostrasValidas = 0,
                    latenciaTimeouts = 0,
                    downloadBytesTotal = 0L,
                    downloadAmostrasValidas = 0,
                    downloadRequisicoesSucesso = 0,
                    downloadRequisicoesErro = 0,
                    downloadEncerradaPor = "reused_recent_result",
                    downloadThroughputOrigem = "reused_recent_result",
                    downloadUltimoErro = null,
                    uploadBytesTotal = 0L,
                    uploadAmostrasValidas = 0,
                    uploadRequisicoesSucesso = 0,
                    uploadRequisicoesErro = 0,
                    uploadEncerradaPor = "reused_recent_result",
                    uploadThroughputOrigem = "reused_recent_result",
                    uploadUltimoErro = null,
                    dnsErroMensagem = null,
                ),
            uploadNaoDetectado = false,
        )
    }

    /**
     * Emite insights locais escalonados antes de chamar a IA.
     * Cada insight aparece como uma [AiAnalysisEntry] com [ResponseSource.INSIGHT].
     * A session é atualizada a cada insight para que a UI reflita o progresso.
     */
    private suspend fun emitirInsights(session: IntelligentDiagnosticSession) {
        val insights = OrbitInsightGenerator.generate(session)
        if (insights.isEmpty()) return

        val insightsAcumulados = mutableListOf<AiAnalysisEntry>()

        insights.forEachIndexed { index, texto ->
            delay(if (index == 0) 800L else 1400L)
            val entry =
                AiAnalysisEntry(
                    trigger = "insight_$index",
                    content = texto,
                    isFallback = false,
                    timestamp = System.currentTimeMillis(),
                    fullResult = null,
                    source = ResponseSource.INSIGHT,
                )
            insightsAcumulados += entry
            activeSession = session.copy(analyses = insightsAcumulados.toList())
            emitSession(OrbitState.Analyzing)
        }

        // Pequena pausa para o usuário ler o último insight antes do Gemma responder
        delay(1000L)
    }

    private suspend fun callAi(
        trigger: String,
        report: io.linka.app.kotlin.feature.diagnostico.DiagnosticReport?,
        connectionType: ConnectionType,
        additionalContext: String?,
        input: DiagnosticInput? = null,
        /** Contexto adicional pré-coletado. Se null, coleta agora (compatibilidade). */
        preCollectedExtra: AdditionalAiContext? = null,
    ): AiAnalysisEntry {
        val fallbackEntry = { isFallback: Boolean, text: String, full: io.linka.app.kotlin.feature.diagnostico.ai.AiDiagnosisResult? ->
            AiAnalysisEntry(
                trigger = trigger,
                content = text,
                isFallback = isFallback,
                timestamp = System.currentTimeMillis(),
                fullResult = full,
                source = if (isFallback) ResponseSource.LOCAL else ResponseSource.GEMMA,
            )
        }

        if (report == null) {
            return fallbackEntry(true, "Não foi possível coletar dados de rede suficientes para análise.", null)
        }

        // Coleta contexto bruto adicional do host (ISP, IP, DNS, dispositivo, etc.).
        // Se pré-coletado (para usar gatewayIp no RTT), usa o valor existente.
        val extra =
            preCollectedExtra ?: try {
                additionalContextProvider()
            } catch (t: Throwable) {
                Timber.w("additionalContextProvider falhou: ${t.message}")
                AdditionalAiContext()
            }

        val ctx =
            DiagnosisAiContextFactory.fromRaw(
                report = report,
                input = input,
                connectionType = connectionType,
                wifiLinkBssid = extra.wifiBssid,
                wifiPadrao = extra.wifiPadrao,
                wifiLinkSpeedMbps = extra.wifiLinkSpeedMbps,
                privateDnsAtivo = extra.privateDnsAtivo,
                privateDnsHostname = extra.privateDnsHostname,
                redesProximas = extra.redesProximas,
                ispNome = extra.ispNome,
                ispAsn = extra.ispAsn,
                ipPublico = extra.ipPublico,
                ipLocal = extra.ipLocal,
                pais = extra.pais,
                regiao = extra.regiao,
                gatewayIp = extra.gatewayIp,
                dnsResolverIp = extra.dnsResolverIp,
                dnsResolverProvider = extra.dnsResolverProvider,
                dnsLatenciaMs = extra.dnsLatenciaMs,
                servidorTesteCidade = extra.servidorTesteCidade,
                ultimosTestesHistorico = extra.ultimosTestesHistorico,
                movel = extra.movel,
                dispositivos = extra.dispositivos,
                // Texto livre do usuário fluindo via additionalContext (chip + responses)
                feedbackUsuario = additionalContext?.take(500),
                speedtestExtras = extra.speedtestExtras,
            )

        val state =
            aiRepository.explainDiagnosis(
                context = ctx,
                // decisaoLocalStatus fica FORA do payload — usado apenas como
                // fallback de normalização se a IA devolver status inválido.
                decisaoLocalStatus = report.decisao.status.name,
                localFallback = { AiFallbackFactory.fromLocal(report) },
            )

        return when (state) {
            is AiDiagnosisState.success ->
                fallbackEntry(
                    false,
                    state.result.resumo.ifBlank { state.result.textoLaudo },
                    state.result,
                )
            is AiDiagnosisState.fallback ->
                fallbackEntry(
                    true,
                    state.result.resumo.ifBlank { state.result.textoLaudo },
                    state.result,
                )
            else -> {
                val localResult = AiFallbackFactory.fromLocal(report)
                fallbackEntry(true, report.decisao.mensagemUsuario, localResult)
            }
        }
    }

    private suspend fun gerarAnaliseComplementar(
        session: IntelligentDiagnosticSession,
        chipId: String,
        novoContexto: String,
    ) {
        emit(OrbitState.Analyzing)
        cancelarRotacaoMensagens()
        iniciarRotacaoMensagens(OrbitState.Analyzing)

        val connectionType =
            when (monitorRede.snapshotFlow.value.estadoConexao.name) {
                "wifi" -> ConnectionType.wifi
                "movel" -> ConnectionType.mobile
                else -> ConnectionType.desconhecido
            }

        val aiEntry =
            callAi(
                trigger = "followup_$chipId",
                report = session.diagnosticReport,
                connectionType = connectionType,
                additionalContext = novoContexto,
            )

        // Restaura chips de análise + feedback após qualquer análise complementar,
        // garantindo que feedbackChips permaneçam disponíveis em todas as rodadas.
        val chips =
            questionEngine.getInitialChips(session.diagnosticReport) +
                questionEngine.getFeedbackChips()
        val novasAnalises = session.analyses + aiEntry

        activeSession =
            session.copy(
                pendingQuestion = null,
                activeChips = chips,
                analyses = novasAnalises,
                contextAccumulated = novoContexto,
            )

        cancelarRotacaoMensagens()
        emitSession(mapOrbitState(session.diagnosticReport))
    }

    private fun emit(
        state: OrbitState,
        erro: String? = null,
        focoDiagnostico: String? = null,
    ) {
        val foco = focoDiagnostico ?: mutableSnapshotFlow.value.focoDiagnostico
        mutableSnapshotFlow.value =
            SnapshotOrbit(
                estado = state,
                session = activeSession,
                mensagemAtual = RotatingMessageProvider.first(state),
                erro = erro,
                focoDiagnostico = foco,
            )
    }

    private fun emitSession(pulseState: OrbitState) {
        val foco = activeSession?.focoDiagnostico ?: mutableSnapshotFlow.value.focoDiagnostico
        mutableSnapshotFlow.value =
            SnapshotOrbit(
                estado = pulseState,
                session = activeSession,
                mensagemAtual = RotatingMessageProvider.first(pulseState),
                erro = null,
                focoDiagnostico = foco,
            )
    }

    private fun iniciarRotacaoMensagens(state: OrbitState) {
        messageRotationJob?.cancel()
        messageRotationJob =
            scope.launch {
                var current = RotatingMessageProvider.first(state)
                while (true) {
                    delay(MESSAGE_ROTATION_INTERVAL_MS)
                    current = RotatingMessageProvider.next(state, current)
                    if (mutableSnapshotFlow.value.estado == state) {
                        mutableSnapshotFlow.value = mutableSnapshotFlow.value.copy(mensagemAtual = current)
                    }
                }
            }
    }

    private fun cancelarRotacaoMensagens() {
        messageRotationJob?.cancel()
        messageRotationJob = null
    }

    private fun mapOrbitState(report: io.linka.app.kotlin.feature.diagnostico.DiagnosticReport?): OrbitState {
        if (report == null) return OrbitState.AwaitingInput
        return when {
            report.temCritico -> OrbitState.Critical
            report.temAtencao -> OrbitState.Warning
            else -> OrbitState.Success
        }
    }
}
