package io.signallq.app.core.nds

/**
 * Resultado de uma chamada a [NdsClient.evaluate]. O cliente nunca lanca
 * excecao ao chamador — toda falha (auth, rate limit, timeout, corpo
 * inesperado) vira um destes estados.
 */
sealed class NdsDiagnosticsOutcome {
    data class Success(val response: NdsDiagnosticsResponse) : NdsDiagnosticsOutcome()

    /**
     * Corpo de erro reconhecido — dois shapes possiveis (NDS-02k, issue #1759,
     * item 9), distinguidos por [code] ser nulo ou nao:
     *
     * 1. Shape antigo flat, confirmado no ADR-017: `{"error", "message"}` —
     *    observado em HTTP 401 (`Unauthorized`) e 429 (`Too Many Requests`).
     *    [code]/[retryable]/[requestId] ficam `null` neste caso; [error] carrega
     *    o texto curto original (`"Unauthorized"`, `"Rate limit exceeded"`).
     * 2. Envelope canonico do PR #12 do NDS (ADR-017, ainda em draft, NAO
     *    confirmado implantado em producao):
     *    `{"error":{"code","message","retryable"},"request_id"}`. [error] recebe
     *    o mesmo valor de [code] (mantem os dois campos sempre preenchidos, sem
     *    o chamador precisar ramificar por shape so para ler uma descricao
     *    curta); [retryable]/[requestId] vem do envelope quando presentes.
     *
     * [NdsClient] tenta o envelope canonico primeiro, cai para o shape antigo se
     * nao bater — ver [NdsClient.parseErrorOutcome].
     */
    data class KnownError(
        val statusCode: Int,
        val error: String,
        val message: String,
        /** Codigo estavel do envelope canonico (ex.: `"NDS_TIMEOUT"`,
         *  `"RATE_LIMITED"`) — `null` quando o corpo bateu com o shape antigo. */
        val code: String? = null,
        /** `true`/`false` do envelope canonico — `null` quando o corpo bateu com
         *  o shape antigo (que nunca informa isso). */
        val retryable: Boolean? = null,
        /** `request_id` de topo do envelope canonico — `null` no shape antigo. */
        val requestId: String? = null,
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
