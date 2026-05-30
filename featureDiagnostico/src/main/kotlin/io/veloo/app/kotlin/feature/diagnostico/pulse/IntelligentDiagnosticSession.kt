package io.veloo.app.feature.diagnostico.pulse

import io.veloo.app.feature.diagnostico.DiagnosticReport

data class IntelligentDiagnosticSession(
    val sessionId: String,
    val createdAt: Long,
    val speedtestDownloadMbps: Double?,
    val speedtestUploadMbps: Double?,
    val speedtestLatencyMs: Double?,
    val speedtestJitterMs: Double?,
    val speedtestLossPercent: Double?,
    val speedtestStabilityScore: Double?,
    val wifiSsid: String?,
    val wifiRssiDbm: Int?,
    val wifiFrequencyMhz: Int?,
    val diagnosticReport: DiagnosticReport?,
    val questionHistory: List<QuestionAnswer>,
    val pendingQuestion: QuestionNode?,
    val activeChips: List<OpcaoResposta>,
    val analyses: List<AiAnalysisEntry>,
    val contextAccumulated: String,
    val chipHistory: List<String> = emptyList(),
    /** Foco/intenção escolhida pelo usuário no picker inicial. Enviado ao Worker
     *  via feedbackUsuario. null = diagnóstico geral sem foco específico. */
    val focoDiagnostico: String? = null,
    /** Quantidade de mensagens enviadas pelo usuário nesta sessão.
     *  Não conta insights nem respostas Gemma — apenas turnos iniciados pelo usuário.
     *  O input é bloqueado quando >= 5. */
    val userTurnCount: Int = 0,
)
