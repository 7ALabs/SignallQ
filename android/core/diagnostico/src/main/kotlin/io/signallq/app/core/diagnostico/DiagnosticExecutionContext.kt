package io.signallq.app.core.diagnostico

/**
 * Identidade mínima de uma execução de diagnóstico (GH#1228, Fase 3 — `executionId`/
 * `rulesVersion`): quem gerou este resultado ([executionId]), com qual conjunto de regras
 * ([rulesVersion]) e quando começou ([startedAtEpochMs]).
 *
 * Este contrato NÃO gera um novo identificador — [executionId] deve vir sempre do UUID já
 * gerado uma única vez no início da execução real (`ResultadoSpeedtest.executionId`,
 * GH#1221/#1225, gerado em `ExecutorSpeedtestCloudflare.executar`). Este tipo só existe para
 * agrupar identidade + versão de regra + início em um único valor imutável, propagado —
 * nunca reconstruído — por quem o recebe (diagnóstico, persistência, histórico, Laudo/PDF,
 * compartilhamento).
 *
 * `startedAtEpochMs` (não `java.time.Instant`) porque `core/diagnostico` não tem
 * core library desugaring habilitado e o app declara `minSdk 24` (API < 26 não tem
 * `java.time` nativo) — mesmo padrão já usado por `timestampEpochMs`/`geradoEmMs`/
 * `startedAtEpochMs` (`ConnectivityDiagnosisHistoryEntity`) no restante do projeto.
 */
data class DiagnosticExecutionContext(
    val executionId: String,
    val rulesVersion: String,
    val startedAtEpochMs: Long,
) {
    companion object {
        /**
         * Constrói o contexto a partir de um [executionId] já existente (nunca gera um novo
         * aqui) e da versão canônica atual ([DiagnosticRulesVersion.CURRENT]).
         *
         * @throws IllegalArgumentException se [executionId] for vazio — este contrato nunca
         *   representa uma execução sem identidade (ver requisito não-negociável da issue #1228).
         */
        fun iniciar(
            executionId: String,
            startedAtEpochMs: Long = System.currentTimeMillis(),
        ): DiagnosticExecutionContext {
            require(executionId.isNotBlank()) {
                "DiagnosticExecutionContext.iniciar: executionId nao pode ser vazio (GH#1228 Fase 3)"
            }
            return DiagnosticExecutionContext(
                executionId = executionId,
                rulesVersion = DiagnosticRulesVersion.CURRENT,
                startedAtEpochMs = startedAtEpochMs,
            )
        }
    }
}
