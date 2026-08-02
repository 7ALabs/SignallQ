package io.signallq.app.core.featureflags

/**
 * Tipo de valor suportado pelo catalogo, espelhando os tipos nativos do Firebase
 * Remote Config (`getBoolean`/`getString`/`getLong`/`getDouble`). Campo `type` do
 * schema JSON canonico (Epico #1347).
 */
enum class FeatureFlagType {
    BOOLEAN,
    STRING,
    LONG,
    DOUBLE,
}
