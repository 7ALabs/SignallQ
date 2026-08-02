package io.signallq.app.core.featureflags

/**
 * Resultado de [FeatureFlagProvider.refresh].
 *
 * Distincao deliberada (issue #1477, criterio de aceite): `fetchAndActivate()`
 * retornar `false` (nenhuma config nova precisou ser ativada -- valores atuais
 * ja ativos/cacheados) NAO e uma falha, e um [Success] com [Success.activated]
 * = false. So excecao/timeout real do SDK vira [Failure]. Em qualquer um dos
 * dois casos, a ultima configuracao valida em memoria e preservada -- ver
 * kdoc de [FeatureFlagProvider.refresh].
 */
sealed interface FeatureFlagRefreshResult {
    data class Success(
        val activated: Boolean,
        val fetchTimeMillis: Long?,
    ) : FeatureFlagRefreshResult

    data class Failure(
        val reason: FeatureFlagRefreshFailureReason,
        val cause: Throwable?,
    ) : FeatureFlagRefreshResult
}

enum class FeatureFlagRefreshFailureReason {
    TIMEOUT,
    NETWORK_ERROR,
    THROTTLED,
    UNKNOWN,
}
