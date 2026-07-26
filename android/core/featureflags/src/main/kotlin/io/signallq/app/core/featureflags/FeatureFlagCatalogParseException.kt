package io.signallq.app.core.featureflags

/**
 * Catalogo JSON invalido ou divergente do schema canonico (Epico #1347). Lancada
 * cedo e alto -- o catalogo e a fonte da verdade, entao um erro de schema deve
 * quebrar em vez de degradar silenciosamente para um provider sem flags.
 */
class FeatureFlagCatalogParseException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
