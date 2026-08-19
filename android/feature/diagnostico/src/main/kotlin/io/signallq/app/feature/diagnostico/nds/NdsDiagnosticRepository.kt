package io.signallq.app.feature.diagnostico.nds

import io.signallq.app.core.diagnostico.DiagnosticArea
import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticReport
import io.signallq.app.core.diagnostico.DiagnosticRunner
import io.signallq.app.core.diagnostico.GameReadinessClassifier
import io.signallq.app.core.diagnostico.UsageProfileClassifier
import io.signallq.app.core.network.AnalyticsHelper
import io.signallq.app.core.network.NoOpAnalyticsHelper
import io.signallq.app.core.nds.NdsClient
import io.signallq.app.core.nds.NdsDiagnosticsOutcome
import io.signallq.app.core.nds.toDiagnosticReport
import io.signallq.app.core.nds.toNdsDiagnosticsRequest
import io.signallq.app.feature.diagnostico.BuildConfig
import io.signallq.app.feature.diagnostico.RecomendacaoPraticaEngine
import timber.log.Timber

/**
 * Ponte de producao entre [DiagnosticOrchestrator][io.signallq.app.feature.diagnostico.DiagnosticOrchestrator]
 * e o Network Diagnostics Service — NDS-02k (issue #1759, item 6 do escopo).
 *
 * ## Estrategia: remoto-primeiro, fallback total (estilo `evaluate()`, NAO `evaluateShadow()`)
 * Mesmo espirito de
 * [io.signallq.app.feature.diagnostico.remote.RemoteDiagnosticRepository.evaluate] — decisao
 * explicita registrada no inventario da issue #1759, secao 3c: o `DiagnosticRunner` local e rede
 * de seguranca (qualquer falha cai para ele, sem diferenca visivel de UI), NAO um segundo motor
 * autoritativo rodando em paralelo so para comparacao (isso duplicaria o problema que o shadow
 * mode antigo ja tem hoje — 2 chamadas remotas por diagnostico).
 *
 * ## Quando este repository e chamado
 * Só quando `consumer.diagnostico.nds_live_enabled` está ligada — ver
 * `DiagnosticOrchestrator.executarProtegido`. Com a flag desligada (default, todo ambiente hoje),
 * este repository nunca é instanciado com tráfego real: a instância default do Hilt existe, mas
 * `evaluate()` nunca é chamado.
 *
 * ## Fallback nunca lança exceção
 * [NdsClient.evaluate] já nunca lança exceção (todo erro vira [NdsDiagnosticsOutcome]); o único
 * ponto de risco adicional aqui é o mapeamento da resposta de sucesso
 * ([io.signallq.app.core.nds.toDiagnosticReport]) — também protegido com `try/catch`, caindo para
 * o motor local em qualquer falha de mapeamento (corpo válido mas inesperado).
 */
class NdsDiagnosticRepository(
    private val ndsClient: NdsClient,
    private val analyticsHelper: AnalyticsHelper = NoOpAnalyticsHelper,
) {
    suspend fun evaluate(
        input: DiagnosticInput,
        enabledAreas: Set<DiagnosticArea> = DiagnosticArea.entries.toSet(),
    ): DiagnosticReport {
        val startedAtMs = System.currentTimeMillis()
        val request = input.toNdsDiagnosticsRequest(appVersion = BuildConfig.APP_VERSION)
        val outcome = ndsClient.evaluate(request)
        val latenciaMs = System.currentTimeMillis() - startedAtMs

        return when (outcome) {
            is NdsDiagnosticsOutcome.Success -> {
                try {
                    val relatorio = outcome.response
                        .toDiagnosticReport(input = input, geradoEmMs = System.currentTimeMillis())
                        .copy(
                            evaluationSource = DiagnosticEvaluationSource.REMOTE,
                            perfisUso = UsageProfileClassifier.classificarTodos(input),
                            gameReadiness = GameReadinessClassifier.classificarTodos(input),
                        )
                    analyticsHelper.registrarDiagNdsOutcome(
                        outcome = "success",
                        fallbackLocalUsado = false,
                        latenciaMs = latenciaMs,
                    )
                    relatorio
                } catch (t: Throwable) {
                    Timber.w(t, "NdsDiagnosticRepository: falha ao mapear resposta do NDS, caindo para motor local")
                    analyticsHelper.registrarDiagNdsOutcome(
                        outcome = "unknown_error",
                        fallbackLocalUsado = true,
                        latenciaMs = latenciaMs,
                    )
                    fallbackLocal(input, enabledAreas)
                }
            }

            is NdsDiagnosticsOutcome.KnownError -> {
                Timber.w(
                    "NdsDiagnosticRepository: KnownError statusCode=${outcome.statusCode} " +
                        "error=${outcome.error} message=${outcome.message}",
                )
                analyticsHelper.registrarDiagNdsOutcome(
                    outcome = "known_error",
                    fallbackLocalUsado = true,
                    latenciaMs = latenciaMs,
                    errorCode = outcome.code ?: outcome.error,
                )
                fallbackLocal(input, enabledAreas)
            }

            is NdsDiagnosticsOutcome.UnknownError -> {
                Timber.w(
                    outcome.cause,
                    "NdsDiagnosticRepository: UnknownError statusCode=${outcome.statusCode}",
                )
                analyticsHelper.registrarDiagNdsOutcome(
                    outcome = "unknown_error",
                    fallbackLocalUsado = true,
                    latenciaMs = latenciaMs,
                )
                fallbackLocal(input, enabledAreas)
            }
        }
    }

    /** Rede de seguranca — mesmo motor 100% offline que [DiagnosticOrchestrator][io.signallq.app.feature.diagnostico.DiagnosticOrchestrator]
     *  ja usa hoje via `RemoteDiagnosticRepository.evaluateShadow`. */
    private fun fallbackLocal(input: DiagnosticInput, enabledAreas: Set<DiagnosticArea>): DiagnosticReport =
        DiagnosticRunner.run(input, enabledAreas, gerarRecomendacoes = RecomendacaoPraticaEngine::recomendar)
            .copy(evaluationSource = DiagnosticEvaluationSource.BUNDLED_LOCAL)
}
