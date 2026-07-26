package io.signallq.app.core.featureflags

/**
 * Constantes tipadas das chaves do catalogo -- unico jeito permitido de referenciar
 * uma flag em codigo de feature (criterio de aceite da issue #1477: "catalogo tipado
 * sem strings de chave soltas nas features").
 *
 * Toda chave aqui **precisa** existir em `consumer-catalog.json`, e vice-versa --
 * ver `FeatureFlagKeysParityTest` (garante que as duas fontes nunca divergem
 * conforme flags novas forem adicionadas).
 *
 * [CONSUMER_SPEEDTEST_CLOUDFLARE_ENGINE_ENABLED] continua smoke-test da fundacao
 * (issue #1477, `androidImplemented=false`) -- nao instrumentada por F4/#1480, que
 * cobriu so a chave principal `enabled` de cada um dos 9 modulos feature. As
 * demais 9 chaves abaixo (uma por modulo `:feature:*` do Consumer) sao reais desde
 * F4/#1480: cada uma gateia entrada de navegacao/overlay do respectivo modulo em
 * `AppShell.kt` (ver `AppShellFeatureGating.kt`).
 */
object FeatureFlagKeys {
    val CONSUMER_SPEEDTEST_ENABLED = FeatureFlagKey("consumer.speedtest.enabled")
    val CONSUMER_SPEEDTEST_CLOUDFLARE_ENGINE_ENABLED = FeatureFlagKey("consumer.speedtest.cloudflare_engine_enabled")
    val CONSUMER_HOME_ENABLED = FeatureFlagKey("consumer.home.enabled")
    val CONSUMER_WIFI_ENABLED = FeatureFlagKey("consumer.wifi.enabled")
    val CONSUMER_DEVICES_ENABLED = FeatureFlagKey("consumer.devices.enabled")
    val CONSUMER_DNS_ENABLED = FeatureFlagKey("consumer.dns.enabled")
    val CONSUMER_FIBRA_ENABLED = FeatureFlagKey("consumer.fibra.enabled")
    val CONSUMER_DIAGNOSTICO_ENABLED = FeatureFlagKey("consumer.diagnostico.enabled")
    val CONSUMER_HISTORY_ENABLED = FeatureFlagKey("consumer.history.enabled")
    val CONSUMER_SETTINGS_ENABLED = FeatureFlagKey("consumer.settings.enabled")

    /** Todas as constantes declaradas aqui -- usado pelo teste de paridade catalogo/codigo. */
    val ALL: List<FeatureFlagKey> =
        listOf(
            CONSUMER_SPEEDTEST_ENABLED,
            CONSUMER_SPEEDTEST_CLOUDFLARE_ENGINE_ENABLED,
            CONSUMER_HOME_ENABLED,
            CONSUMER_WIFI_ENABLED,
            CONSUMER_DEVICES_ENABLED,
            CONSUMER_DNS_ENABLED,
            CONSUMER_FIBRA_ENABLED,
            CONSUMER_DIAGNOSTICO_ENABLED,
            CONSUMER_HISTORY_ENABLED,
            CONSUMER_SETTINGS_ENABLED,
        )

    /** Os 9 modulos feature do Consumer instrumentados por F4/#1480, na ordem do
     *  catalogo/Epico #1347 -- usado por [ALL] e por testes que precisam iterar
     *  so as flags principais de modulo (exclui o smoke-test de engine). */
    val CONSUMER_MODULE_KEYS: List<FeatureFlagKey> =
        listOf(
            CONSUMER_HOME_ENABLED,
            CONSUMER_SPEEDTEST_ENABLED,
            CONSUMER_WIFI_ENABLED,
            CONSUMER_DEVICES_ENABLED,
            CONSUMER_DNS_ENABLED,
            CONSUMER_FIBRA_ENABLED,
            CONSUMER_DIAGNOSTICO_ENABLED,
            CONSUMER_HISTORY_ENABLED,
            CONSUMER_SETTINGS_ENABLED,
        )
}
