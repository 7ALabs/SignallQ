package io.signallq.app.core.featureflags

/**
 * Constantes tipadas das chaves do catalogo -- unico jeito permitido de referenciar
 * uma flag em codigo de feature (criterio de aceite da issue #1477: "catalogo tipado
 * sem strings de chave soltas nas features").
 *
 * Toda chave aqui **precisa** existir em `consumer-catalog.json`, e vice-versa --
 * ver `FeatureFlagKeysParityTest` (garante que as duas fontes nunca divergem
 * conforme flags novas forem adicionadas em F4).
 *
 * As duas chaves abaixo sao smoke-test da fundacao (issue #1477) -- nenhuma feature
 * real ainda le [CONSUMER_SPEEDTEST_ENABLED]/[CONSUMER_SPEEDTEST_CLOUDFLARE_ENGINE_ENABLED]
 * (`androidImplemented=false` no catalogo). Instrumentar os modulos feature de
 * verdade e escopo da F4 (#1480).
 */
object FeatureFlagKeys {
    val CONSUMER_SPEEDTEST_ENABLED = FeatureFlagKey("consumer.speedtest.enabled")
    val CONSUMER_SPEEDTEST_CLOUDFLARE_ENGINE_ENABLED = FeatureFlagKey("consumer.speedtest.cloudflare_engine_enabled")

    /** Todas as constantes declaradas aqui -- usado pelo teste de paridade catalogo/codigo. */
    val ALL: List<FeatureFlagKey> =
        listOf(
            CONSUMER_SPEEDTEST_ENABLED,
            CONSUMER_SPEEDTEST_CLOUDFLARE_ENGINE_ENABLED,
        )
}
