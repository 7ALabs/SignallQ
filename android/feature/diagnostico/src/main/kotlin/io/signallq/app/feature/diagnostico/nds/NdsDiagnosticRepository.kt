package io.signallq.app.feature.diagnostico.nds

import io.signallq.app.core.diagnostico.DiagnosticArea
import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticReport
import io.signallq.app.core.diagnostico.DiagnosticRunner
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.GameReadinessClassifier
import io.signallq.app.core.diagnostico.UsageProfileClassifier
import io.signallq.app.core.network.AnalyticsHelper
import io.signallq.app.core.network.NoOpAnalyticsHelper
import io.signallq.app.core.nds.NdsClient
import io.signallq.app.core.nds.NdsDiagnosticsOutcome
import io.signallq.app.core.nds.NdsDiagnosticsResponse
import io.signallq.app.core.nds.toDiagnosticReport
import io.signallq.app.core.nds.toNdsDiagnosticsRequest
import io.signallq.app.feature.diagnostico.BuildConfig
import io.signallq.app.feature.diagnostico.RecomendacaoPraticaEngine
import timber.log.Timber

/**
 * Ponte de producao entre [DiagnosticOrchestrator][io.signallq.app.feature.diagnostico.DiagnosticOrchestrator]
 * e o Network Diagnostics Service — NDS-02k (issue #1759, item 6 do escopo).
 *
 * ## Estratégias separadas
 * `evaluate()` é o caminho legado remoto-primeiro com fallback total para o `DiagnosticRunner`
 * local, sem diferença visível de UI. `evaluateForAssist()` é o caminho remoto dedicado do
 * Assist: o NDS é obrigatório e qualquer erro é propagado como [NdsAssistEvaluationException],
 * para a UI exibir erro recuperável em vez de inventar um resultado local.
 *
 * ## Quando este repository é chamado
 * `evaluate()` só é usado pelo diagnóstico legado quando
 * `consumer_diagnostico_nds_live_enabled` está ligada. `evaluateForAssist()` ignora essa flag
 * global de propósito e é usado sempre que o usuário entra no Assist.
 *
 * ## Tratamento de falhas
 * [NdsClient.evaluate] não lança exceção de rede (todo erro vira [NdsDiagnosticsOutcome]). O
 * caminho legado converte esses estados para fallback local; o caminho Assist os converte para
 * [NdsAssistEvaluationException]. O mapeamento da resposta remota também segue essa distinção.
 *
 * ## `profile="gamer"` (issue #1762)
 * O campo `profile` do payload NDS existe desde NDS-02a/#1747 e a regra `profile`/
 * `capabilities` foi decidida em #1746 secao 3b — NAO e um gap de contrato (ver
 * [io.signallq.app.core.nds.NdsDiagnosticsRequestMapper.toNdsDiagnosticsRequest], param
 * `perfilGamer`, ja mapeado para `profile="gamer"` por
 * [io.signallq.app.core.nds.ndsProfile]). O gap era so aqui: [evaluate] ate a correcao da
 * issue #1762 nunca repassava esse sinal para o mapper (sempre `false`). Agora [evaluate]
 * aceita `perfilGamer` e repassa — mas nenhum chamador de producao ainda sabe detectar "esta
 * dentro do Modo Gamer" (o Modo Gamer roda por [io.signallq.app.core.diagnostico.ModoGamerEngine],
 * fluxo hoje desacoplado de [DiagnosticOrchestrator][io.signallq.app.feature.diagnostico.DiagnosticOrchestrator]).
 * Fio de wiring ponta-a-ponta (Modo Gamer -> orquestrador -> aqui) fica para uma tarefa
 * separada quando o rollout real da flag `nds_live_enabled` estiver planejado.
 */
class NdsDiagnosticRepository(
    private val ndsClient: NdsClient,
    private val analyticsHelper: AnalyticsHelper = NoOpAnalyticsHelper,
) {
    /**
     * Caminho exclusivo do Assist: NDS remoto é obrigatório e falhas não podem virar um
     * diagnóstico local silencioso. A UI traduz [NdsAssistEvaluationException] para o estado
     * de erro recuperável do Assist.
     */
    suspend fun evaluateForAssist(input: DiagnosticInput): DiagnosticReport =
        evaluate(
            input = input,
            enabledAreas = DiagnosticArea.entries.toSet(),
            fallbackLocalOnError = false,
        )

    suspend fun evaluate(
        input: DiagnosticInput,
        enabledAreas: Set<DiagnosticArea> = DiagnosticArea.entries.toSet(),
        // issue #1762 (achado do Caio na PR #1760) — o campo `profile="gamer"` existe no
        // contrato do NDS desde NDS-02a/#1747 (regra decidida em #1746 secao 3b), mas nao
        // havia wiring nenhum ate aqui: nenhum chamador desta funcao ainda sabe dizer se o
        // diagnostico atual roda dentro do Modo Gamer. Default `false` preserva o
        // comportamento atual; quem chamar de dentro do Modo Gamer deve passar `true`.
        perfilGamer: Boolean = false,
    ): DiagnosticReport = evaluate(input, enabledAreas, perfilGamer, fallbackLocalOnError = true)

    private suspend fun evaluate(
        input: DiagnosticInput,
        enabledAreas: Set<DiagnosticArea>,
        perfilGamer: Boolean = false,
        fallbackLocalOnError: Boolean,
    ): DiagnosticReport {
        val startedAtMs = System.currentTimeMillis()
        val request = input.toNdsDiagnosticsRequest(appVersion = BuildConfig.APP_VERSION, perfilGamer = perfilGamer)
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
                        outcome = if (relatorio.decisao.status == DiagnosticStatus.inconclusive) {
                            "remote_inconclusive"
                        } else {
                            "success"
                        },
                        fallbackLocalUsado = false,
                        latenciaMs = latenciaMs,
                    )
                    relatorio
                } catch (t: Throwable) {
                    if (!fallbackLocalOnError) {
                        analyticsHelper.registrarDiagNdsOutcome(
                            outcome = "unknown_error",
                            fallbackLocalUsado = false,
                            latenciaMs = latenciaMs,
                        )
                        throw NdsAssistEvaluationException("falha ao interpretar resposta remota do NDS", t)
                    }
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
                if (!fallbackLocalOnError) {
                    analyticsHelper.registrarDiagNdsOutcome(
                        outcome = "known_error",
                        fallbackLocalUsado = false,
                        latenciaMs = latenciaMs,
                        errorCode = outcome.code ?: outcome.error,
                    )
                    throw NdsAssistEvaluationException(
                        "NDS recusou a avaliação (${outcome.statusCode})",
                    )
                }
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
                if (!fallbackLocalOnError) {
                    analyticsHelper.registrarDiagNdsOutcome(
                        outcome = "unknown_error",
                        fallbackLocalUsado = false,
                        latenciaMs = latenciaMs,
                    )
                    throw NdsAssistEvaluationException("NDS indisponível", outcome.cause)
                }
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

class NdsAssistEvaluationException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)
