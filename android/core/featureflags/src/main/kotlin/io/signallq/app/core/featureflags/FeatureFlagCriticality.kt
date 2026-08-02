package io.signallq.app.core.featureflags

/**
 * Criticidade declarada no catalogo (campo `criticality` do schema, Epico #1347).
 * Usada pelo Admin (F3) para exigir confirmacao reforcada em kill switches
 * [CRITICAL]/[HIGH] antes de publicar -- nao tem efeito no comportamento do
 * provider Android nesta fase, so metadado do catalogo.
 */
enum class FeatureFlagCriticality {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}
