package io.signallq.app.feature.diagnostico.pulse

import android.os.Build
import io.signallq.app.feature.diagnostico.BuildConfig
import io.signallq.app.core.database.MedicaoDao
import io.signallq.app.core.network.AnalyticsHelper
import io.signallq.app.core.network.AnalyticsTracker
import io.signallq.app.core.network.GatewayLatencyMeasurer
import io.signallq.app.core.network.MonitorRede
import io.signallq.app.core.network.NetworkCapabilitiesProvider
import io.signallq.app.core.network.NoOpAnalyticsHelper
import io.signallq.app.feature.diagnostico.ConnectionType
import io.signallq.app.feature.diagnostico.DiagnosticInput
import io.signallq.app.feature.diagnostico.DiagnosticOrchestrator
import io.signallq.app.feature.diagnostico.EstadoDiagnostico
import io.signallq.app.feature.diagnostico.InternetDiagnosticInput
import io.signallq.app.feature.diagnostico.WifiDiagnosticInput
import io.signallq.app.feature.diagnostico.ai.AI_PROMPT_VERSION
import io.signallq.app.feature.diagnostico.ai.AdditionalAiContext
import io.signallq.app.feature.diagnostico.ai.AiDiagnosisRepository
import io.signallq.app.feature.diagnostico.ai.AiDiagnosisState
import io.signallq.app.feature.diagnostico.ai.AiFallbackFactory
import io.signallq.app.feature.diagnostico.ai.DiagnosisAiContextFactory
import io.signallq.app.feature.diagnostico.ingest.AdminIngestRepository
import io.signallq.app.feature.diagnostico.ingest.AiUsageIngestPayload
import io.signallq.app.feature.diagnostico.ingest.DiagnosticIngestPayload
import io.signallq.app.feature.diagnostico.ingest.frequenciaMhzParaBanda
import io.signallq.app.feature.diagnostico.ingest.idParaIssueLabel
import io.signallq.app.feature.speedtest.DiagnosticoFasesSpeedtest
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.ExecutorSpeedtest
import io.signallq.app.feature.speedtest.ModoSpeedtest
import io.signallq.app.feature.speedtest.ResultadoSpeedtest
import io.signallq.app.feature.speedtest.SpeedtestQualityClassifier
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

private const val TAG = "SignallQOrchestrator"
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

class SignallQOrchestrator(
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
     *  no speedtest silencioso (sem dialog, sem interrompimento do fluxo do SignallQOrchestrator). */
    private val networkCapabilitiesProvider: NetworkCapabilitiesProvider? = null,
    /** Instancia unica de AiDiagnosisRepository injetada pelo Hilt via DiagnosticoModule.
     *  Antes desta mudanca era instanciada manualmente aqui (dois caches independentes). */
    val aiRepository: AiDiagnosisRepository,
    /** Repositorio de telemetria para o painel admin. Null = ingest desabilitado (testes). */
    private val adminIngestRepository: AdminIngestRepository? = null,
    /** Lambda que retorna o device_id anonimo persistente. Deve vir do DataStore via PreferenciasAppRepository. */
    private val deviceIdProvider: suspend () -> String = { "unknown" },
    /** Lambda que retorna o canal de distribuicao ("play_store", "sideload", "unknown"). */
    private val distChannelProvider: () -> String = { "unknown" },
    /** Lambda que le o toggle "Analise avancada" (SIG-282) no momento da chamada.
     *  Default true preserva o comportamento anterior em quem nao passa o provider
     *  (ex.: testes existentes que nao testam o toggle). Quando false, [callAi]
     *  pula a chamada de rede e devolve o resultado do motor local direto. */
    private val analiseAvancadaProvider: suspend () -> Boolean = { true },
    /** Funil principal de engajamento (SIG-155) — ia_laudo_solicitado/ia_laudo_recebido.
     *  Disparado apenas nos triggers "initial"/"initial_from_result" (o laudo automatico
     *  do funil) — perguntas de acompanhamento (chips/texto livre) NAO contam como um
     *  novo passo do funil, sao conversa complementar sobre o mesmo laudo. */
    private val analyticsHelper: AnalyticsHelper = NoOpAnalyticsHelper,
    /** GH#919 — schema SIG-134 (feature_used/session_start, distinto de [analyticsHelper]
     *  acima). Null = tracker desabilitado (testes) — o evento correlato de "diagnostico"
     *  so e emitido quando presente. */
    private val analyticsTracker: AnalyticsTracker? = null,
) {

    private val mutableSnapshotFlow = MutableStateFlow(SignallQSnapshot())
    val snapshotFlow: StateFlow<SignallQSnapshot> = mutableSnapshotFlow.asStateFlow()

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
        emit(SignallQState.Thinking, focoDiagnostico = focoDiagnostico)
        iniciarRotacaoMensagens(SignallQState.Thinking)

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

        val internetInput =
            InternetDiagnosticInput(
                downloadMbps = resultado.downloadMbps,
                uploadMbps = resultado.uploadMbps,
                latencyMs = resultado.latenciaMs,
                jitterMs = resultado.jitterMs,
                perdaPercentual = resultado.perdaPercentual,
                bufferbloatMs = resultado.bufferbloatMs,
                rttGatewayMs = rttGatewayMs,
                packetLossSource = resultado.packetLossSource,
            )

        val wifiInput =
            wifiSnapshot?.let { ws ->
                WifiDiagnosticInput(
                    rssiDbm = ws.rssiDbm,
                    linkSpeedMbps = ws.linkSpeedMbps,
                    frequenciaMhz = ws.frequenciaMhz,
                    ssid = ws.ssid,
                    is5GhzCapable = ws.is5GhzCapable,
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
        // GH#919 — evento correlato ao diagnostico real, com diagnostic_sessions.id
        // (mesmo id de ai_usage.session_id via dispararIngestDiagnostico/dispararIngestAiUsage
        // abaixo). Substitui o feature_used("diagnostico") solto do clique inicial (MainActivity)
        // que so tinha o UUID de instancia generico, sem correlacao possivel.
        analyticsTracker?.registrarFeatureUsada("diagnostico", sessionIdOverride = partialSession.sessionId)
        emit(SignallQState.Analyzing, focoDiagnostico = focoDiagnostico)
        cancelarRotacaoMensagens()
        iniciarRotacaoMensagens(SignallQState.Analyzing)

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
                ingestSessionId = partialSession.sessionId,
            )

        val chips = questionEngine.getInitialChips(relatorio)
        val feedbackChips = questionEngine.getFeedbackChips()
        val chipsComFeedback = chips + feedbackChips
        val pulseState = mapSignallQState(relatorio)

        // Insights ficam antes da resposta Gemma na lista
        activeSession =
            partialSession.copy(
                activeChips = chipsComFeedback,
                analyses = insightsGerados + aiEntry,
            )

        cancelarRotacaoMensagens()
        emitSession(pulseState)

        // Ingest de diagnostico concluido — fire-and-forget.
        dispararIngestDiagnostico(
            sessionId = partialSession.sessionId,
            connectionType = connectionType,
            relatorio = relatorio,
            speedtestResult = resultado,
            wifiFrequenciaMhz = wifiSnapshot?.frequenciaMhz,
            movelTecnologia = extraContext.movel?.tecnologia,
            // GH#412: movel.operadora e null em Wi-Fi — cai para o ISP ja
            // identificado (normalizado pelo catalogo de operadoras).
            operadoraMovel = extraContext.movel?.operadora ?: extraContext.ispOperadoraDetectada,
            aiSummaryReport = if (!aiEntry.isFallback) aiEntry.content else "",
        )

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
        emit(SignallQState.Collecting, focoDiagnostico = focoDiagnostico)
        iniciarRotacaoMensagens(SignallQState.Collecting)

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
        // duplicada quando o teste vinha do SignallQ (Bug #1). O observer global
        // é a fonte única de verdade.

        // --- Fase 2: Thinking (engines locais) ---
        emit(SignallQState.Thinking, focoDiagnostico = focoDiagnostico)
        iniciarRotacaoMensagens(SignallQState.Thinking)

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
                    packetLossSource = it.packetLossSource,
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
                        packetLossSource = it.packetLossSource,
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
                    is5GhzCapable = ws.is5GhzCapable,
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
        // GH#919 — evento correlato ao diagnostico real, com diagnostic_sessions.id
        // (mesmo id de ai_usage.session_id via dispararIngestDiagnostico/dispararIngestAiUsage
        // abaixo). Substitui o feature_used("diagnostico") solto do clique inicial (MainActivity)
        // que so tinha o UUID de instancia generico, sem correlacao possivel.
        analyticsTracker?.registrarFeatureUsada("diagnostico", sessionIdOverride = partialSession.sessionId)
        emit(SignallQState.Analyzing, focoDiagnostico = focoDiagnostico)
        cancelarRotacaoMensagens()
        iniciarRotacaoMensagens(SignallQState.Analyzing)

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
                ingestSessionId = partialSession.sessionId,
            )

        val chips = questionEngine.getInitialChips(relatorio)
        val feedbackChips = questionEngine.getFeedbackChips()
        val chipsComFeedback = chips + feedbackChips
        val pulseState = mapSignallQState(relatorio)

        // Insights ficam antes da resposta Gemma na lista
        activeSession =
            partialSession.copy(
                activeChips = chipsComFeedback,
                analyses = insightsGerados + aiEntry,
            )

        cancelarRotacaoMensagens()
        emitSession(pulseState)

        // Ingest de diagnostico concluido — fire-and-forget.
        dispararIngestDiagnostico(
            sessionId = partialSession.sessionId,
            connectionType = connectionType,
            relatorio = relatorio,
            speedtestResult = speedtestResult,
            wifiFrequenciaMhz = wifiSnapshot?.frequenciaMhz,
            movelTecnologia = extraContext.movel?.tecnologia,
            // GH#412: movel.operadora e null em Wi-Fi — cai para o ISP ja
            // identificado (normalizado pelo catalogo de operadoras).
            operadoraMovel = extraContext.movel?.operadora ?: extraContext.ispOperadoraDetectada,
            aiSummaryReport = if (!aiEntry.isFallback) aiEntry.content else "",
        )

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
            emitSession(mapSignallQState(session.diagnosticReport))
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
            emitSession(mapSignallQState(session.diagnosticReport))
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
                    content =
                        "Posso ajudar apenas com dúvidas sobre sua conexão de internet, Wi-Fi, DNS e rede doméstica. " +
                            "Tem alguma pergunta sobre sua conexão?",
                    isFallback = false,
                    timestamp = System.currentTimeMillis(),
                    fullResult = null,
                    source = ResponseSource.LOCAL,
                )
            activeSession =
                session.copy(
                    analyses = session.analyses + offTopicEntry,
                )
            emitSession(mapSignallQState(session.diagnosticReport))
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
        mutableSnapshotFlow.value = SignallQSnapshot()
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
                // Sem dialog — decisão silenciosa e automática dentro do SignallQOrchestrator.
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

    private fun io.signallq.app.core.database.MedicaoEntity.toReusableSpeedtestResult(): ResultadoSpeedtest? {
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
        val insights = SignallQInsightGenerator.generate(session)
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
            emitSession(SignallQState.Analyzing)
        }

        // Pequena pausa para o usuário ler o último insight antes do Gemma responder
        delay(1000L)
    }

    private suspend fun callAi(
        trigger: String,
        report: io.signallq.app.feature.diagnostico.DiagnosticReport?,
        connectionType: ConnectionType,
        additionalContext: String?,
        input: DiagnosticInput? = null,
        /** Contexto adicional pré-coletado. Se null, coleta agora (compatibilidade). */
        preCollectedExtra: AdditionalAiContext? = null,
        /** Session ID para correlacao no ingest de AI usage. Null = nao faz ingest. */
        ingestSessionId: String? = null,
    ): AiAnalysisEntry {
        val fallbackEntry = { isFallback: Boolean, text: String, full: io.signallq.app.feature.diagnostico.ai.AiDiagnosisResult? ->
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

        // SIG-282: "Analise avancada" (IA) e opcional — desligada, o motor local
        // (Fases 3a/3b) ja resolveu tudo em [report]. Nao e erro, entao nao usa
        // ResponseSource.LOCAL (reservado a fallback de falha) — usa INSIGHT, o
        // mesmo canal do card tecnico local que ja aparece antes da IA responder.
        if (!analiseAvancadaProvider()) {
            val local = AiFallbackFactory.fromLocal(report)
            return AiAnalysisEntry(
                trigger = trigger,
                content = local.resumo.ifBlank { local.textoLaudo },
                // isFallback=true so pra sinalizacao de origem ("local" no card) e para
                // o ingest de telemetria nao contar isto como resumo gerado por IA —
                // NAO e erro, e escolha do usuario (ver checagem acima).
                isFallback = true,
                timestamp = System.currentTimeMillis(),
                fullResult = local,
                source = ResponseSource.INSIGHT,
            )
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

        // Funil principal (SIG-155): so o laudo inicial conta como passo do funil —
        // followups/chips sao conversa complementar sobre o mesmo laudo.
        val ehPassoDoFunil = trigger == "initial" || trigger == "initial_from_result"
        if (ehPassoDoFunil) {
            analyticsHelper.registrarIaLaudoSolicitado(
                schemaVersion = ctx.schemaVersion,
                promptVersion = AI_PROMPT_VERSION,
                statusDiagLocal = report.decisao.status.name,
                temFeedbackUsuario = !additionalContext.isNullOrBlank(),
            )
        }

        val tsAntesChamada = System.currentTimeMillis()
        val state =
            aiRepository.explainDiagnosis(
                context = ctx,
                // decisaoLocalStatus fica FORA do payload — usado apenas como
                // fallback de normalização se a IA devolver status inválido.
                decisaoLocalStatus = report.decisao.status.name,
                localFallback = { AiFallbackFactory.fromLocal(report) },
            )
        val latenciaMs = System.currentTimeMillis() - tsAntesChamada

        if (ehPassoDoFunil) {
            when (state) {
                is AiDiagnosisState.success ->
                    analyticsHelper.registrarIaLaudoRecebido(
                        schemaVersion = state.result.schemaVersion,
                        promptVersion = AI_PROMPT_VERSION,
                        statusIa = state.result.status,
                        source = "cloud",
                        modeloIa = state.result.modeloIa.familia.ifBlank { null },
                        promptTokens = state.result.promptTokens.toLong(),
                        completionTokens = state.result.completionTokens.toLong(),
                        totalTokens = state.result.totalTokens.toLong(),
                        latenciaMs = latenciaMs,
                    )
                is AiDiagnosisState.fallback ->
                    analyticsHelper.registrarIaLaudoRecebido(
                        schemaVersion = state.result.schemaVersion,
                        promptVersion = AI_PROMPT_VERSION,
                        statusIa = state.result.status,
                        source = "local",
                        latenciaMs = latenciaMs,
                    )
                else ->
                    analyticsHelper.registrarIaLaudoRecebido(
                        schemaVersion = ctx.schemaVersion,
                        promptVersion = AI_PROMPT_VERSION,
                        statusIa = "inconclusivo",
                        source = "local",
                        latenciaMs = latenciaMs,
                    )
            }
        }

        // Ingest de AI usage — fire-and-forget, correlaciona com a sessao de diagnostico.
        if (ingestSessionId != null) {
            dispararIngestAiUsage(sessionId = ingestSessionId, aiState = state)
        }

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
        emit(SignallQState.Analyzing)
        cancelarRotacaoMensagens()
        iniciarRotacaoMensagens(SignallQState.Analyzing)

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
        emitSession(mapSignallQState(session.diagnosticReport))
    }

    private fun emit(
        state: SignallQState,
        erro: String? = null,
        focoDiagnostico: String? = null,
    ) {
        val foco = focoDiagnostico ?: mutableSnapshotFlow.value.focoDiagnostico
        mutableSnapshotFlow.value =
            SignallQSnapshot(
                estado = state,
                session = activeSession,
                mensagemAtual = RotatingMessageProvider.first(state),
                erro = erro,
                focoDiagnostico = foco,
            )
    }

    private fun emitSession(pulseState: SignallQState) {
        val foco = activeSession?.focoDiagnostico ?: mutableSnapshotFlow.value.focoDiagnostico
        mutableSnapshotFlow.value =
            SignallQSnapshot(
                estado = pulseState,
                session = activeSession,
                mensagemAtual = RotatingMessageProvider.first(pulseState),
                erro = null,
                focoDiagnostico = foco,
            )
    }

    private fun iniciarRotacaoMensagens(state: SignallQState) {
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

    // ---- Ingest de telemetria (fire-and-forget) ----

    /**
     * Dispara ingest de diagnostico concluido em background.
     * Falhas sao silenciosas — nao afetam o usuario.
     */
    private fun dispararIngestDiagnostico(
        sessionId: String,
        connectionType: ConnectionType,
        relatorio: io.signallq.app.feature.diagnostico.DiagnosticReport?,
        speedtestResult: io.signallq.app.feature.speedtest.ResultadoSpeedtest?,
        /** Frequencia Wi-Fi em MHz — null se movel ou indisponivel (Samsung One UI apos reconexao). */
        wifiFrequenciaMhz: Int? = null,
        /** Tecnologia movel ja como string — ex: "5G", "5G NSA", "4G". Null se Wi-Fi ou Xiaomi sem permissao. */
        movelTecnologia: String? = null,
        /** Operadora movel OU provedor Wi-Fi identificado — ex: "Claro", "Vivo".
         *  Null apenas se realmente indisponivel (GH#412). */
        operadoraMovel: String? = null,
        /** Resumo gerado pela IA. Vazio se IA nao foi chamada ou falhou. */
        aiSummaryReport: String = "",
    ) {
        val repo = adminIngestRepository ?: return
        scope.launch {
            // GH#764 — antes mandava "completed"/"failed" (ciclo de vida da sessao,
            // nao o veredito de qualidade), o que zerava a Taxa de Sucesso no admin
            // (a query la espera o vocabulario canonico excelente/bom/regular/
            // ruim/critico/inconclusivo). Mapeia a decisao real do motor local.
            val status = when (relatorio?.decisao?.status) {
                null -> "failed"
                io.signallq.app.feature.diagnostico.DiagnosticStatus.ok -> "excelente"
                io.signallq.app.feature.diagnostico.DiagnosticStatus.info -> "bom"
                io.signallq.app.feature.diagnostico.DiagnosticStatus.attention -> "regular"
                io.signallq.app.feature.diagnostico.DiagnosticStatus.critical -> "critico"
                io.signallq.app.feature.diagnostico.DiagnosticStatus.inconclusive -> "inconclusivo"
            }
            val deviceId = runCatching { deviceIdProvider() }.getOrDefault("unknown")
            val distChannel = runCatching { distChannelProvider() }.getOrDefault("unknown")
            // network_type refinado:
            //  - movel: usa tecnologia direta (ex: "5G", "4G") ou null se indisponivel (Xiaomi quirk)
            //  - wifi: converte frequencia para banda (ex: "wifi_5GHz") ou "wifi" se frequencia invalida
            //  - outros: null
            val networkTypeName: String? = when (connectionType) {
                ConnectionType.mobile -> movelTecnologia // null e valido — Xiaomi pode nao ter
                ConnectionType.wifi -> frequenciaMhzParaBanda(wifiFrequenciaMhz) ?: "wifi"
                else -> null
            }
            val issues = relatorio?.let { rep ->
                (rep.wifiResultados + rep.internetResultados + rep.mobileResultados +
                    rep.fibraResultados + rep.dnsResultados + rep.historicoResultados +
                    rep.wifiCanalResultados)
                    .filter {
                        it.status == io.signallq.app.feature.diagnostico.DiagnosticStatus.critical ||
                            it.status == io.signallq.app.feature.diagnostico.DiagnosticStatus.attention
                    }
                    .map { idParaIssueLabel(it.id) }
            } ?: emptyList()

            repo.sendDiagnostic(
                DiagnosticIngestPayload(
                    id = sessionId,
                    networkType = networkTypeName,
                    status = status,
                    score = relatorio?.scoreConexao,
                    downloadMbps = speedtestResult?.downloadMbps?.toFloat(),
                    uploadMbps = speedtestResult?.uploadMbps?.toFloat(),
                    latencyMs = speedtestResult?.latenciaMs?.toInt(),
                    jitterMs = speedtestResult?.jitterMs?.toInt(),
                    packetLoss = speedtestResult?.perdaPercentual?.toFloat(),
                    issues = issues,
                    operator = operadoraMovel?.takeIf { it.isNotBlank() },
                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                    osVersion = "Android ${Build.VERSION.RELEASE}",
                    appVersion = BuildConfig.APP_VERSION,
                    aiSummaryReport = aiSummaryReport,
                    environment = if (distChannel == "play_store") "production" else "staging",
                    distChannel = distChannel,
                    buildType = BuildConfig.BUILD_TYPE,
                    versionCode = BuildConfig.VERSION_CODE,
                    deviceId = deviceId,
                ),
            )
        }
    }

    /**
     * Dispara ingest de uso de IA em background.
     * Correlaciona com o diagnostico via [sessionId].
     * Falhas sao silenciosas.
     */
    private fun dispararIngestAiUsage(
        sessionId: String,
        aiState: AiDiagnosisState,
    ) {
        val repo = adminIngestRepository ?: return
        if (aiState !is AiDiagnosisState.success) return
        val result = aiState.result
        val modelId = result.modeloIa.idInterno.ifBlank {
            result.modeloIa.nomeExibicao.ifBlank { "unknown" }
        }
        scope.launch {
            val deviceId = runCatching { deviceIdProvider() }.getOrDefault("unknown")
            val distChannel = runCatching { distChannelProvider() }.getOrDefault("unknown")
            repo.sendAiUsage(
                AiUsageIngestPayload(
                    id = java.util.UUID.randomUUID().toString(),
                    model = modelId,
                    sessionId = sessionId,
                    promptTokens = result.promptTokens,
                    completionTokens = result.completionTokens,
                    totalTokens = result.totalTokens,
                    environment = if (distChannel == "play_store") "production" else "staging",
                    distChannel = distChannel,
                    buildType = BuildConfig.BUILD_TYPE,
                    versionCode = BuildConfig.VERSION_CODE,
                    deviceId = deviceId,
                ),
            )
        }
    }

    private fun mapSignallQState(report: io.signallq.app.feature.diagnostico.DiagnosticReport?): SignallQState {
        if (report == null) return SignallQState.AwaitingInput
        return when {
            report.temCritico -> SignallQState.Critical
            report.temAtencao -> SignallQState.Warning
            else -> SignallQState.Success
        }
    }
}
