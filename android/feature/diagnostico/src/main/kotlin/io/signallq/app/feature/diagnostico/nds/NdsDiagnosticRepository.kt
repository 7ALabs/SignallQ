package io.signallq.app.feature.diagnostico.nds

import io.signallq.app.core.diagnostico.DiagnosticArea
import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticReport
import io.signallq.app.core.diagnostico.DiagnosticRunner
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.GameReadinessClassifier
import io.signallq.app.core.diagnostico.UsageProfileClassifier
import io.signallq.app.core.nds.NDS_SNAPSHOT_SCHEMA_VERSION
import io.signallq.app.core.nds.NdsClient
import io.signallq.app.core.nds.NdsDiagnosticsOutcome
import io.signallq.app.core.nds.NdsSnapshotCoverage
import io.signallq.app.core.nds.analyzeNdsSnapshotCoverage
import io.signallq.app.core.nds.asAi
import io.signallq.app.core.nds.toDiagnosticReport
import io.signallq.app.core.nds.toNdsDiagnosticsRequest
import io.signallq.app.core.network.AnalyticsHelper
import io.signallq.app.core.network.NoOpAnalyticsHelper
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
 * ## v1/v2 (feat/nds-v2-fluxo-principal)
 * Ambos os caminhos aceitam `usarNdsV2`/`useV2` e reaproveitam o mesmo parsing de resposta
 * ([io.signallq.app.core.nds.toDiagnosticReport], via `NdsDiagnosticsResponse.explanationV2`) —
 * o v2 só troca o envelope de transporte (`{raw, explanation}` em vez do formato direto do v1),
 * nunca a lógica de mapeamento. `evaluateForAssist()` já lê `USAR_NDS_V2_NO_ASSIST`; `evaluate()`
 * agora também aceita a decisão equivalente do fluxo principal, `USAR_NDS_V2_NO_FLUXO_PRINCIPAL`,
 * lida por [io.signallq.app.feature.diagnostico.DiagnosticOrchestrator.executarProtegido].
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
     *
     * [usarNdsV2] (feat/nds-client-v2) — decisão de `FeatureFlagKeys.USAR_NDS_V2_NO_ASSIST`,
     * lida pelo chamador ([io.signallq.app.feature.diagnostico.DiagnosticOrchestrator.avaliarAssist],
     * mesmo padrão já usado para `CONSUMER_DIAGNOSTICO_NDS_LIVE_ENABLED` em
     * [DiagnosticOrchestrator.executarProtegido][io.signallq.app.feature.diagnostico.DiagnosticOrchestrator]).
     * Este repository não lê flags diretamente. Default `false` preserva o contrato v1
     * inalterado; quando `true`, [NdsClient.evaluate] usa v2 mesmo com contexto parcial
     * (ver [io.signallq.app.core.nds.NdsClient.evaluate]).
     */
    suspend fun evaluateForAssist(
        input: DiagnosticInput,
        usarNdsV2: Boolean = false,
    ): DiagnosticReport =
        evaluate(
            input = input,
            enabledAreas = DiagnosticArea.entries.toSet(),
            fallbackLocalOnError = false,
            useV2 = usarNdsV2,
        )

    /**
     * [usarNdsV2] (feat/nds-v2-fluxo-principal) — decisão de
     * `FeatureFlagKeys.USAR_NDS_V2_NO_FLUXO_PRINCIPAL`, lida pelo chamador
     * ([io.signallq.app.feature.diagnostico.DiagnosticOrchestrator.executarProtegido]), mesmo
     * padrão já usado por [evaluateForAssist] para `USAR_NDS_V2_NO_ASSIST`. Este repository não
     * lê flags diretamente. Default `false` preserva o contrato v1 inalterado.
     */
    suspend fun evaluate(
        input: DiagnosticInput,
        enabledAreas: Set<DiagnosticArea> = DiagnosticArea.entries.toSet(),
        // issue #1762 (achado do Caio na PR #1760) — o campo `profile="gamer"` existe no
        // contrato do NDS desde NDS-02a/#1747 (regra decidida em #1746 secao 3b), mas nao
        // havia wiring nenhum ate aqui: nenhum chamador desta funcao ainda sabe dizer se o
        // diagnostico atual roda dentro do Modo Gamer. Default `false` preserva o
        // comportamento atual; quem chamar de dentro do Modo Gamer deve passar `true`.
        perfilGamer: Boolean = false,
        usarNdsV2: Boolean = false,
    ): DiagnosticReport = evaluate(input, enabledAreas, perfilGamer, fallbackLocalOnError = true, useV2 = usarNdsV2)

    private suspend fun evaluate(
        input: DiagnosticInput,
        enabledAreas: Set<DiagnosticArea>,
        perfilGamer: Boolean = false,
        fallbackLocalOnError: Boolean,
        useV2: Boolean = false,
    ): DiagnosticReport {
        val startedAtMs = System.currentTimeMillis()
        val request = input.toNdsDiagnosticsRequest(appVersion = BuildConfig.APP_VERSION, perfilGamer = perfilGamer)
        val coverage =
            analyzeNdsSnapshotCoverage(
                request = request,
                connectionType = input.connectionType,
                mobileCapturaReduzida = input.mobile?.capturaReduzida == true,
            )
        logCoverageEmDebug(coverage)
        val outcome = ndsClient.evaluate(request, useV2 = useV2)
        val latenciaMs = System.currentTimeMillis() - startedAtMs

        return when (outcome) {
            is NdsDiagnosticsOutcome.Success -> {
                val iaInvocada = outcome.response.resultFor("ai") != null || outcome.response.explanationV2 != null
                val iaProvider =
                    outcome.response
                        .resultFor("ai")
                        ?.asAi()
                        ?.aiModelUsed
                        ?.takeIf(String::isNotBlank)
                try {
                    val relatorio =
                        outcome.response
                            .toDiagnosticReport(input = input, geradoEmMs = System.currentTimeMillis())
                            .copy(
                                evaluationSource = DiagnosticEvaluationSource.REMOTE,
                                perfisUso = UsageProfileClassifier.classificarTodos(input),
                                gameReadiness = GameReadinessClassifier.classificarTodos(input),
                            )
                    val outcomeLabel =
                        if (relatorio.decisao.status == DiagnosticStatus.inconclusive) {
                            "remote_inconclusive"
                        } else {
                            "success"
                        }
                    analyticsHelper.registrarDiagNdsOutcome(
                        outcome = outcomeLabel,
                        fallbackLocalUsado = false,
                        latenciaMs = latenciaMs,
                    )
                    registrarCoberturaSnapshot(
                        coverage = coverage,
                        latenciaMs = latenciaMs,
                        outcomeLabel = outcomeLabel,
                        iaInvocada = iaInvocada,
                        iaProvider = iaProvider,
                        resultConfidence = relatorio.confianca,
                    )
                    relatorio
                } catch (t: Throwable) {
                    if (!fallbackLocalOnError) {
                        analyticsHelper.registrarDiagNdsOutcome(
                            outcome = "unknown_error",
                            fallbackLocalUsado = false,
                            latenciaMs = latenciaMs,
                        )
                        registrarCoberturaSnapshot(
                            coverage = coverage,
                            latenciaMs = latenciaMs,
                            outcomeLabel = "unknown_error",
                            iaInvocada = iaInvocada,
                            iaProvider = iaProvider,
                            resultConfidence = null,
                        )
                        throw NdsAssistEvaluationException("falha ao interpretar resposta remota do NDS", t)
                    }
                    Timber.w(t, "NdsDiagnosticRepository: falha ao mapear resposta do NDS, caindo para motor local")
                    analyticsHelper.registrarDiagNdsOutcome(
                        outcome = "unknown_error",
                        fallbackLocalUsado = true,
                        latenciaMs = latenciaMs,
                    )
                    registrarCoberturaSnapshot(
                        coverage = coverage,
                        latenciaMs = latenciaMs,
                        outcomeLabel = "unknown_error",
                        iaInvocada = iaInvocada,
                        iaProvider = iaProvider,
                        resultConfidence = null,
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
                    registrarCoberturaSnapshot(coverage = coverage, latenciaMs = latenciaMs, outcomeLabel = "known_error")
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
                registrarCoberturaSnapshot(coverage = coverage, latenciaMs = latenciaMs, outcomeLabel = "known_error")
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
                    registrarCoberturaSnapshot(coverage = coverage, latenciaMs = latenciaMs, outcomeLabel = "unknown_error")
                    throw NdsAssistEvaluationException("NDS indisponível", outcome.cause)
                }
                analyticsHelper.registrarDiagNdsOutcome(
                    outcome = "unknown_error",
                    fallbackLocalUsado = true,
                    latenciaMs = latenciaMs,
                )
                registrarCoberturaSnapshot(coverage = coverage, latenciaMs = latenciaMs, outcomeLabel = "unknown_error")
                fallbackLocal(input, enabledAreas)
            }
        }
    }

    /**
     * NDS-Snapshot-12 (issue #1844) — log de debug (nunca analytics) listando blocos
     * montados/omitidos e a razão da omissão. Formato exato pedido pela issue:
     * ```
     * NDS snapshot:
     * speed=present
     * wifi=present
     * wifiScan=missing:no_permission
     * mobile=missing:not_mobile
     * ```
     * Só emite em build de debug — nunca em release, mesmo critério já usado por
     * `SignallQApplication`/`AppModule` para `BuildConfig.DEBUG`.
     */
    private fun logCoverageEmDebug(coverage: NdsSnapshotCoverage) {
        if (!BuildConfig.DEBUG) return
        Timber.d("NDS snapshot:\n" + coverage.toDebugLogLines().joinToString("\n"))
    }

    /** Dispara [AnalyticsHelper.registrarNdsSnapshotEnviado] uma vez por chamada ao NDS — ver
     *  KDoc do método na interface para o que cada propriedade mede e por quê. */
    private fun registrarCoberturaSnapshot(
        coverage: NdsSnapshotCoverage,
        latenciaMs: Long,
        outcomeLabel: String,
        iaInvocada: Boolean = false,
        iaProvider: String? = null,
        resultConfidence: Double? = null,
    ) {
        analyticsHelper.registrarNdsSnapshotEnviado(
            schemaVersion = NDS_SNAPSHOT_SCHEMA_VERSION,
            blocosPresentes = coverage.blocksPresent.joinToString(","),
            qtdBlocosPresentes = coverage.blocksPresent.size.toLong(),
            camposPresentesCount = coverage.fieldsPresentCount.toLong(),
            blocosCriticosAusentes = coverage.missingCriticalBlocks.joinToString(","),
            iaInvocada = iaInvocada,
            iaProvider = iaProvider,
            duracaoMs = latenciaMs,
            resultConfidence = resultConfidence,
            outcome = outcomeLabel,
        )
    }

    /** Rede de seguranca — mesmo motor 100% offline que [DiagnosticOrchestrator][io.signallq.app.feature.diagnostico.DiagnosticOrchestrator]
     *  ja usa hoje via `RemoteDiagnosticRepository.evaluateShadow`. */
    private fun fallbackLocal(
        input: DiagnosticInput,
        enabledAreas: Set<DiagnosticArea>,
    ): DiagnosticReport =
        DiagnosticRunner
            .run(input, enabledAreas, gerarRecomendacoes = RecomendacaoPraticaEngine::recomendar)
            .copy(evaluationSource = DiagnosticEvaluationSource.BUNDLED_LOCAL)
}

class NdsAssistEvaluationException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
