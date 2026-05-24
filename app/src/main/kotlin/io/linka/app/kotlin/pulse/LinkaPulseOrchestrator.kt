package io.linka.app.kotlin.pulse

import io.linka.app.kotlin.core.database.MedicaoDao
import io.linka.app.kotlin.core.network.MonitorRede
import io.linka.app.kotlin.feature.diagnostico.ConnectionType
import io.linka.app.kotlin.feature.diagnostico.DiagnosticOrchestrator
import io.linka.app.kotlin.feature.diagnostico.EstadoDiagnostico
import io.linka.app.kotlin.feature.diagnostico.InternetDiagnosticInput
import io.linka.app.kotlin.feature.diagnostico.WifiDiagnosticInput
import io.linka.app.kotlin.feature.diagnostico.ai.AiDiagnosisRepository
import io.linka.app.kotlin.feature.diagnostico.ai.AiDiagnosisState
import io.linka.app.kotlin.feature.diagnostico.ai.AiFallbackFactory
import io.linka.app.kotlin.feature.diagnostico.ai.DiagnosisAiContextFactory
import io.linka.app.kotlin.feature.diagnostico.pulse.AiAnalysisEntry
import io.linka.app.kotlin.feature.diagnostico.pulse.ContextAccumulator
import io.linka.app.kotlin.feature.diagnostico.pulse.DynamicQuestionEngine
import io.linka.app.kotlin.feature.diagnostico.pulse.IntelligentDiagnosticSession
import io.linka.app.kotlin.feature.diagnostico.pulse.OpcaoResposta
import io.linka.app.kotlin.feature.diagnostico.pulse.PulseState
import io.linka.app.kotlin.feature.diagnostico.pulse.QuestionAnswer
import io.linka.app.kotlin.feature.diagnostico.pulse.RotatingMessageProvider
import io.linka.app.kotlin.feature.diagnostico.pulse.SnapshotLinkaPulse
import io.linka.app.kotlin.feature.speedtest.EstadoExecucaoSpeedtest
import io.linka.app.kotlin.feature.speedtest.ExecutorSpeedtest
import io.linka.app.kotlin.feature.speedtest.ModoSpeedtest
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

private const val TAG = "LinkaPulse"
private const val AI_BASE_URL = "https://linka-ai-diagnosis-worker.giammattey-luiz.workers.dev"
private const val MESSAGE_ROTATION_INTERVAL_MS = 2500L

class LinkaPulseOrchestrator(
    private val executorSpeedtest: ExecutorSpeedtest,
    private val diagnosticOrchestrator: DiagnosticOrchestrator,
    private val monitorRede: MonitorRede,
    private val medicaoDao: MedicaoDao,
    private val questionEngine: DynamicQuestionEngine = DynamicQuestionEngine(),
    private val scope: CoroutineScope,
) {
    private val aiRepository =
        AiDiagnosisRepository(
            baseUrl = AI_BASE_URL,
            isAuthorized = { true },
        )

    private val mutableSnapshotFlow = MutableStateFlow(SnapshotLinkaPulse())
    val snapshotFlow: StateFlow<SnapshotLinkaPulse> = mutableSnapshotFlow.asStateFlow()

    private var activeSession: IntelligentDiagnosticSession? = null
    private var messageRotationJob: Job? = null

    // ---- API pública ----

    suspend fun iniciarDiagnostico() {
        Timber.i("iniciarDiagnostico")
        activeSession = null
        cancelarRotacaoMensagens()

        // --- Fase 1: Collecting (speedtest silencioso) ---
        emit(PulseState.Collecting)
        iniciarRotacaoMensagens(PulseState.Collecting)

        val speedtestResult = runSilentSpeedtest()
        val wifiSnapshot = monitorRede.snapshotFlow.value.wifiLinkSnapshot
        val connectionType =
            when (monitorRede.snapshotFlow.value.estadoConexao.name) {
                "wifi" -> ConnectionType.wifi
                "movel" -> ConnectionType.mobile
                else -> ConnectionType.desconhecido
            }

        // --- Fase 2: Thinking (engines locais) ---
        emit(PulseState.Thinking)
        iniciarRotacaoMensagens(PulseState.Thinking)

        val internetInput =
            speedtestResult?.let {
                InternetDiagnosticInput(
                    downloadMbps = it.downloadMbps,
                    uploadMbps = it.uploadMbps,
                    latencyMs = it.latenciaMs,
                    jitterMs = it.jitterMs,
                    perdaPercentual = it.perdaPercentual,
                    bufferbloatMs = it.bufferbloatMs,
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

        // Esperar diagnóstico concluir (já é síncrono mas garante o state)
        val relatorio =
            withTimeoutOrNull(5_000L) {
                diagnosticOrchestrator.snapshotFlow
                    .first {
                        it.estado == EstadoDiagnostico.concluido || it.estado == EstadoDiagnostico.erro
                    }.relatorio
            }

        // --- Fase 3: Analyzing (IA) ---
        emit(PulseState.Analyzing)
        cancelarRotacaoMensagens()
        iniciarRotacaoMensagens(PulseState.Analyzing)

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

        val aiEntry =
            callAi(
                trigger = "initial",
                report = relatorio,
                connectionType = connectionType,
                additionalContext = null,
            )

        // --- Monta sessão com chips iniciais ---
        val chips = questionEngine.getInitialChips(relatorio)
        val pulseState = mapPulseState(relatorio)

        activeSession =
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
                activeChips = chips,
                analyses = listOf(aiEntry),
                contextAccumulated = contextInicial,
            )

        cancelarRotacaoMensagens()
        emitSession(pulseState)
        Timber.i("iniciarDiagnostico concluído estado=$pulseState")
    }

    suspend fun selecionarChip(chip: OpcaoResposta) {
        val session = activeSession ?: return
        Timber.i("selecionarChip id=${chip.id}")

        val novoContexto = ContextAccumulator.appendChip(session.contextAccumulated, chip)
        val proximaPergunta = questionEngine.getNextQuestion(chip.id, emptyList())
        val isLeaf = questionEngine.isLeafAnswer(chip.id, "", emptyList()) || proximaPergunta == null

        if (isLeaf) {
            // Chip leva direto a análise complementar
            gerarAnaliseComplementar(session, chip.id, novoContexto)
        } else {
            activeSession =
                session.copy(
                    activeChips = emptyList(),
                    pendingQuestion = proximaPergunta,
                    contextAccumulated = novoContexto,
                )
            emitSession(mapPulseState(session.diagnosticReport))
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

        // Identificar chipId do fluxo atual (primeiro item do histórico ou chip ativo anterior)
        val chipId =
            session.questionHistory.firstOrNull()?.let {
                // busca chipId original via contexto
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
                session.copy(questionHistory = novoHistorico, contextAccumulated = novoContexto),
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
            emitSession(mapPulseState(session.diagnosticReport))
        }
    }

    fun reset() {
        cancelarRotacaoMensagens()
        activeSession = null
        mutableSnapshotFlow.value = SnapshotLinkaPulse()
    }

    // ---- Internos ----

    private suspend fun runSilentSpeedtest() =
        try {
            val connType = monitorRede.snapshotFlow.value.estadoConexao.name
            executorSpeedtest.executar(
                modo = ModoSpeedtest.complete,
                connectionType = connType,
                connectionTypeProvider = { monitorRede.snapshotFlow.value.estadoConexao.name },
            )
            val snap = executorSpeedtest.snapshotFlow.value
            if (snap.estado == EstadoExecucaoSpeedtest.concluido) snap.resultado else null
        } catch (t: Throwable) {
            Timber.w("speedtest silencioso falhou: ${t.message}")
            null
        }

    private suspend fun callAi(
        trigger: String,
        report: io.linka.app.kotlin.feature.diagnostico.DiagnosticReport?,
        connectionType: ConnectionType,
        additionalContext: String?,
    ): AiAnalysisEntry {
        val fallbackEntry = { isFallback: Boolean, text: String ->
            AiAnalysisEntry(trigger = trigger, content = text, isFallback = isFallback, timestamp = System.currentTimeMillis())
        }

        if (report == null) {
            return fallbackEntry(true, "Não foi possível coletar dados de rede suficientes para análise.")
        }

        val baseCtx = DiagnosisAiContextFactory.from(report, connectionType)
        // Schema v3 raw: contexto adicional do usuário vai em `feedbackUsuario`
        // (não mais em `limitesDaAnalise`/evidência rotulada). Evidências v3
        // são raw, sem campo `interpretacao`.
        val ctx =
            if (additionalContext != null) {
                baseCtx.copy(feedbackUsuario = additionalContext.take(500))
            } else {
                baseCtx
            }

        val genericFallbackText = report.decisao.mensagemUsuario.ifBlank { "Diagnóstico em andamento..." }

        // Timeout de segurança: evita travar a UI se o servidor demorar além do esperado.
        // OkHttp readTimeout = 90s; damos margem extra de 5s via coroutine.
        val state =
            withTimeoutOrNull(95_000L) {
                aiRepository.explainDiagnosis(ctx) { AiFallbackFactory.fromLocal(report) }
            } ?: run {
                Timber.w("callAi[$trigger] timeout após 95s — usando fallback local")
                return fallbackEntry(true, genericFallbackText)
            }

        return when (state) {
            is AiDiagnosisState.success -> {
                val content =
                    state.result.resumo
                        .ifBlank { state.result.textoLaudo }
                        .ifBlank { genericFallbackText }
                Timber.d("callAi[$trigger] success — content length=${content.length}, isFallback=false")
                fallbackEntry(false, content)
            }
            is AiDiagnosisState.fallback -> {
                val content =
                    state.result.resumo
                        .ifBlank { state.result.textoLaudo }
                        .ifBlank { genericFallbackText }
                Timber.d("callAi[$trigger] fallback — content length=${content.length}, isFallback=true")
                fallbackEntry(true, content)
            }
            else -> {
                Timber.w("callAi[$trigger] estado inesperado — usando mensagemUsuario do report: '$genericFallbackText'")
                fallbackEntry(true, genericFallbackText)
            }
        }
    }

    private suspend fun gerarAnaliseComplementar(
        session: IntelligentDiagnosticSession,
        chipId: String,
        novoContexto: String,
    ) {
        emit(PulseState.Analyzing)
        cancelarRotacaoMensagens()
        iniciarRotacaoMensagens(PulseState.Analyzing)

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

        val chips = questionEngine.getInitialChips(session.diagnosticReport)
        val novasAnalises = session.analyses + aiEntry

        activeSession =
            session.copy(
                pendingQuestion = null,
                activeChips = chips,
                analyses = novasAnalises,
                contextAccumulated = novoContexto,
            )

        cancelarRotacaoMensagens()
        emitSession(mapPulseState(session.diagnosticReport))
    }

    private fun emit(
        state: PulseState,
        erro: String? = null,
    ) {
        mutableSnapshotFlow.value =
            SnapshotLinkaPulse(
                estado = state,
                session = activeSession,
                mensagemAtual = RotatingMessageProvider.first(state),
                erro = erro,
            )
    }

    private fun emitSession(pulseState: PulseState) {
        mutableSnapshotFlow.value =
            SnapshotLinkaPulse(
                estado = pulseState,
                session = activeSession,
                mensagemAtual = RotatingMessageProvider.first(pulseState),
                erro = null,
            )
    }

    private fun iniciarRotacaoMensagens(state: PulseState) {
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

    private fun mapPulseState(report: io.linka.app.kotlin.feature.diagnostico.DiagnosticReport?): PulseState {
        if (report == null) return PulseState.AwaitingInput
        return when {
            report.temCritico -> PulseState.Critical
            report.temAtencao -> PulseState.Warning
            else -> PulseState.Success
        }
    }
}
