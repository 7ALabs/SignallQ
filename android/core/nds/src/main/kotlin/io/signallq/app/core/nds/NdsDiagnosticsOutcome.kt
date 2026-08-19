package io.signallq.app.core.nds

/**
 * Resultado de uma chamada a [NdsClient.evaluate]. O cliente nunca lanca
 * excecao ao chamador — toda falha (auth, rate limit, timeout, corpo
 * inesperado) vira um destes estados.
 */
sealed class NdsDiagnosticsOutcome {
    data class Success(val response: NdsDiagnosticsResponse) : NdsDiagnosticsOutcome()

    /**
     * Corpo de erro com shape confirmado no ADR-017: `{"error", "message"}`.
     * Confirmado para HTTP 401 (`Unauthorized`) e 429 (`Too Many Requests`) —
     * qualquer outro status cujo corpo tenha esse mesmo shape tambem cai aqui,
     * em vez de restringir por codigo de status.
     */
    data class KnownError(
        val statusCode: Int,
        val error: String,
        val message: String,
    ) : NdsDiagnosticsOutcome()

    /**
     * Erro sem shape confirmado: 5xx, corpo vazio/nao-JSON, resposta 2xx que
     * nao parseia como [NdsDiagnosticsResponse], ou excecao de rede antes de
     * qualquer resposta chegar (timeout, sem conectividade). [statusCode] e
     * [rawBody] ficam `null` quando a falha ocorreu antes da resposta HTTP.
     * Lacuna documentada no ADR-017 — formato de erro generico do NDS ainda
     * nao foi confirmado por teste real.
     */
    data class UnknownError(
        val statusCode: Int?,
        val rawBody: String?,
        val cause: Throwable? = null,
    ) : NdsDiagnosticsOutcome()
}
