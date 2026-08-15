package io.signallq.app.core.network

/**
 * Contrato de instrumentacao do funil principal de engajamento do SignallQ (SIG-155).
 *
 * Implementado por FirebaseAnalyticsHelper (:app) — mesma estrategia de
 * desacoplamento do AnalyticsTracker (SIG-134/feature_used), mas dedicado aos
 * 7 eventos do funil descrito em docs_ai/technical/analytics-events.md:
 *
 * app_aberto -> speedtest_iniciado -> speedtest_concluido -> diag_iniciado
 *   -> diag_concluido -> ia_laudo_solicitado -> ia_laudo_recebido
 *
 * Nao substitui o AnalyticsTracker (feature_used/screen_view/etc — schema SIG-134):
 * sao dois contratos distintos que podem compartilhar a mesma instancia de
 * FirebaseAnalytics internamente sem se misturar na API publica.
 *
 * Sem PII nos parametros — ver docs_ai/technical/analytics-events.md.
 * `versao_app` e anexado automaticamente pela implementacao em todos os eventos
 * (nao faz parte da assinatura dos metodos abaixo).
 */
interface AnalyticsHelper {
    /** Disparado no MainActivity.onCreate. */
    fun registrarAppAberto(
        tipoConexao: String,
        primeiraAbertura: Boolean? = null,
    )

    /** Disparado quando o usuario toca "Iniciar teste" ou o teste silencioso comeca. */
    fun registrarSpeedtestIniciado(
        modo: String,
        tipoConexao: String,
    )

    /** Disparado quando o ResultadoSpeedtest da execucao atual fica disponivel. */
    fun registrarSpeedtestConcluido(
        modo: String,
        tipoConexaoInicio: String,
        tipoConexaoFim: String?,
        downloadMbps: Double,
        uploadMbps: Double,
        latenciaMs: Double,
        jitterMs: Double,
        perdaPct: Double,
        bufferbloatMs: Double,
        severidadeBufferbloat: String,
        stabilityScore: Double,
        contaminado: Boolean,
        duracaoMs: Long? = null,
    )

    /** Disparado no inicio de DiagnosticOrchestrator.executar(). */
    fun registrarDiagIniciado(
        tipoConexao: String,
        areasHabilitadas: String?,
        temSpeedtest: Boolean,
    )

    /** Disparado quando o DiagnosticOrchestrator conclui com sucesso. */
    fun registrarDiagConcluido(
        tipoConexao: String,
        statusGeral: String,
        decisaoId: String,
        scoreConexao: Long,
        confianca: Double,
        nResultadosCriticos: Long? = null,
        nResultadosAttention: Long? = null,
    )

    /** Disparado quando o app envia o payload ao Worker (AiDiagnosisRepository). */
    fun registrarIaLaudoSolicitado(
        schemaVersion: String,
        promptVersion: String,
        statusDiagLocal: String,
        temFeedbackUsuario: Boolean,
    )

    /** Disparado quando o AiDiagnosisResult (ou fallback local) fica disponivel. */
    fun registrarIaLaudoRecebido(
        schemaVersion: String,
        promptVersion: String,
        statusIa: String,
        source: String,
        modeloIa: String? = null,
        promptTokens: Long? = null,
        completionTokens: Long? = null,
        totalTokens: Long? = null,
        latenciaMs: Long? = null,
    )
}

/**
 * Implementacao no-op usada como default em pontos de instanciacao manual
 * (fora do grafo Hilt) — evita quebrar testes/previews que nao precisam
 * verificar analytics. O grafo Hilt sempre injeta FirebaseAnalyticsHelper.
 */
object NoOpAnalyticsHelper : AnalyticsHelper {
    override fun registrarAppAberto(
        tipoConexao: String,
        primeiraAbertura: Boolean?,
    ) = Unit

    override fun registrarSpeedtestIniciado(
        modo: String,
        tipoConexao: String,
    ) = Unit

    override fun registrarSpeedtestConcluido(
        modo: String,
        tipoConexaoInicio: String,
        tipoConexaoFim: String?,
        downloadMbps: Double,
        uploadMbps: Double,
        latenciaMs: Double,
        jitterMs: Double,
        perdaPct: Double,
        bufferbloatMs: Double,
        severidadeBufferbloat: String,
        stabilityScore: Double,
        contaminado: Boolean,
        duracaoMs: Long?,
    ) = Unit

    override fun registrarDiagIniciado(
        tipoConexao: String,
        areasHabilitadas: String?,
        temSpeedtest: Boolean,
    ) = Unit

    override fun registrarDiagConcluido(
        tipoConexao: String,
        statusGeral: String,
        decisaoId: String,
        scoreConexao: Long,
        confianca: Double,
        nResultadosCriticos: Long?,
        nResultadosAttention: Long?,
    ) = Unit

    override fun registrarIaLaudoSolicitado(
        schemaVersion: String,
        promptVersion: String,
        statusDiagLocal: String,
        temFeedbackUsuario: Boolean,
    ) = Unit

    override fun registrarIaLaudoRecebido(
        schemaVersion: String,
        promptVersion: String,
        statusIa: String,
        source: String,
        modeloIa: String?,
        promptTokens: Long?,
        completionTokens: Long?,
        totalTokens: Long?,
        latenciaMs: Long?,
    ) = Unit
}
