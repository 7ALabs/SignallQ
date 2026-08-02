package io.signallq.app.core.network.contracts.connectivity

/**
 * Resultado tipado de uma sondagem de conectividade (GH#1512). Ausência de dado nunca
 * vira `false`/`null` silencioso — cada sondagem devolve exatamente um destes estados,
 * preservando o motivo quando ela não confirma sucesso.
 */
sealed interface ProbeResult {
    /** Sondagem executada e confirmou alcance. [elapsedMs] é informativo, não usado para decisão. */
    data class Success(val elapsedMs: Long? = null) : ProbeResult

    /** Sondagem executada e falhou de forma conclusiva antes do timeout. */
    data class Failure(val reason: ProbeFailureReason) : ProbeResult

    /** Sondagem executada mas excedeu o tempo limite sem resposta conclusiva. */
    data class Timeout(val afterMs: Long) : ProbeResult

    /** Sondagem não executada nesta rodada (ex.: etapa anterior já falhou, sem sentido tentar). */
    data class NotExecuted(val reason: String) : ProbeResult

    /** Pré-requisito da sondagem está ausente (ex.: sem gateway configurado, sem DNS configurado). */
    data class Unavailable(val reason: String) : ProbeResult
}

enum class ProbeFailureReason {
    /** Resolução de nome falhou (host inexistente, DNS não respondeu com registro válido). */
    DNS_RESOLUTION_FAILED,

    /** Alvo (IP/host) recusou ou não aceitou a conexão (ex.: connection refused). */
    HOST_UNREACHABLE,

    /** Resposta chegou mas não é a esperada (ex.: HTTP inesperado fora de timeout/portal). */
    UNEXPECTED_RESPONSE,

    /** Falha não classificável nas categorias acima. */
    UNKNOWN,
}
