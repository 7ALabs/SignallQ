package io.signallq.app.core.featureflags

/**
 * Valor efetivo de uma flag em runtime -- o que [FeatureFlagProvider.observe] emite
 * e o que alimenta [FeatureFlagProvider.isEnabled].
 */
data class FeatureFlagValue(
    val key: FeatureFlagKey,
    val raw: FeatureFlagRawValue,
    val source: FeatureFlagSource,
)
