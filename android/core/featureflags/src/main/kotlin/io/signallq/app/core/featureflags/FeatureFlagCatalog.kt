package io.signallq.app.core.featureflags

/**
 * Catalogo de flags carregado em memoria -- fonte dos defaults locais aplicados
 * antes de qualquer consulta remota (criterio de aceite da issue #1477).
 *
 * Construtor recebe a lista ja parseada (nao le arquivo/classpath diretamente)
 * para ficar trivialmente testavel com fixtures -- ver [FeatureFlagCatalogLoader]
 * para a instancia real carregada do catalogo canonico do repositorio.
 */
class FeatureFlagCatalog(
    val definitions: List<FeatureFlagDefinition>,
) {
    private val byKey: Map<FeatureFlagKey, FeatureFlagDefinition> = definitions.associateBy { it.key }

    fun definitionFor(key: FeatureFlagKey): FeatureFlagDefinition? = byKey[key]

    /** Valor default de [key] como [FeatureFlagValue] com [FeatureFlagSource.DEFAULT] --
     *  `null` se a chave nao existir no catalogo (chamador deveria estar usando uma
     *  constante de [FeatureFlagKeys], entao isso so acontece por bug de sincronizacao). */
    fun defaultValueFor(key: FeatureFlagKey): FeatureFlagValue? =
        byKey[key]?.let { definicao ->
            FeatureFlagValue(key = key, raw = definicao.defaultValue, source = FeatureFlagSource.DEFAULT)
        }

    /** Todos os defaults do catalogo, com [FeatureFlagSource.DEFAULT] -- usado para
     *  semear o estado inicial do provider antes de qualquer fetch. */
    fun defaultsAsValues(): Map<FeatureFlagKey, FeatureFlagValue> =
        definitions.associate { definicao ->
            definicao.key to FeatureFlagValue(key = definicao.key, raw = definicao.defaultValue, source = FeatureFlagSource.DEFAULT)
        }

    /** Mapa `chave -> valor nativo` para alimentar `FirebaseRemoteConfig.setDefaultsAsync`. */
    fun toRemoteConfigDefaultsMap(): Map<String, Any> =
        definitions.associate { it.key.id to it.defaultValue.toNativeValue() }
}
