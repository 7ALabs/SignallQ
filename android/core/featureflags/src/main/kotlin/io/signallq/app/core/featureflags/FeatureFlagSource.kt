package io.signallq.app.core.featureflags

/**
 * Origem do valor efetivo de uma flag, para observabilidade (Epico #1347 exige
 * "observabilidade de fetch, activate, default, erro e versao do template").
 *
 * Mapeia 1:1 para `FirebaseRemoteConfigValue.getSource()` -- nao inventa uma
 * quarta origem tipo "cache": o proprio SDK do Remote Config ja trata REMOTE
 * como "ultimo valor ativado com sucesso" (persistido em disco pelo SDK,
 * sobrevive a reinicio do processo), entao nao ha um estado "vindo do cache"
 * distinto de REMOTE do ponto de vista deste contrato.
 */
enum class FeatureFlagSource {
    /** Valor local embarcado no APK (nenhum fetch ainda ativado para esta chave). */
    DEFAULT,

    /** Valor do ultimo template ativado com sucesso (rede ou cache em disco do SDK). */
    REMOTE,

    /**
     * Nenhum default e nenhum valor remoto conhecidos para esta chave -- o SDK
     * caiu no valor estatico dele (`false`/`0`/`""`). So deve acontecer se o
     * catalogo e o `setDefaultsAsync` divergirem (bug de sincronizacao, nao
     * comportamento esperado).
     */
    STATIC,
}
