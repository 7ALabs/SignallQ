package io.signallq.app.feature.diagnostico.recommendation

import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticReport
import io.signallq.app.core.recommendation.RecommendationDecision
import io.signallq.app.core.recommendation.RecommendationEngine
import io.signallq.app.core.recommendation.RecommendationFeedbackType
import io.signallq.app.core.recommendation.RecommendationFlags
import io.signallq.app.feature.diagnostico.RecommendationRequestMapper
import javax.inject.Inject

/**
 * Unico ponto de integracao entre o resultado do diagnostico, o historico persistido
 * (Room, issue #812) e o `RecommendationEngine` (issue #790). Consumido pela camada de
 * UI da issue #813 -- ela nao precisa saber que existe Room nem montar o
 * `RecommendationRequest` na mao.
 *
 * Uma unica leitura de historico e uma unica escrita de exibicao por chamada de
 * [escolherRecomendacao]: sem leitura duplicada, sem corrida entre exibir e persistir.
 */
class RecommendationDecisionCoordinator
    @Inject
    constructor(
        private val engine: RecommendationEngine,
        private val historyRepository: RecommendationHistoryRepository,
    ) {
        suspend fun escolherRecomendacao(
            report: DiagnosticReport,
            input: DiagnosticInput,
            isp: String? = null,
            flags: RecommendationFlags = RecommendationFlags(),
            diagnosticId: String? = null,
            nowMs: Long = System.currentTimeMillis(),
        ): RecommendationDecision? {
            val historico = historyRepository.historicoRecente(nowMs)
            val request =
                RecommendationRequestMapper.map(
                    report = report,
                    input = input,
                    isp = isp,
                    history = historico,
                    flags = flags,
                    diagnosticId = diagnosticId,
                )

            val decisao = engine.choose(request) ?: return null
            historyRepository.registrarExibicao(decisao, nowMs)
            return decisao
        }

        suspend fun registrarFeedback(
            trackingId: String,
            feedback: RecommendationFeedbackType,
            nowMs: Long = System.currentTimeMillis(),
        ) = historyRepository.registrarFeedback(trackingId, feedback, nowMs)
    }
