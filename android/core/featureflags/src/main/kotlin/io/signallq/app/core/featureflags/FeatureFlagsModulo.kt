package io.signallq.app.core.featureflags

import com.google.firebase.remoteconfig.FirebaseRemoteConfig

/**
 * Fabrica dos componentes de `:core:featureflags` -- segue o mesmo padrao de
 * `CoreNetworkModulo`/`FeatureDevicesModulo`: o modulo expoe funcoes de fabrica
 * simples, o wiring Hilt (`@Provides`/`@Singleton`) fica em `AppModule` (`:app`).
 */
object FeatureFlagsModulo {
    /** Catalogo canonico do Consumer, carregado do classpath deste modulo. */
    fun criarCatalogo(): FeatureFlagCatalog = FeatureFlagCatalogLoader.loadConsumerCatalog()

    /** [remoteConfigProvider] normalmente encapsula um `dagger.Lazy<FirebaseRemoteConfig>.get()`
     *  -- ver kdoc de [RemoteConfigFeatureFlagProvider] sobre o motivo de nao usar
     *  `dagger.Lazy` diretamente neste modulo. */
    fun criarProvider(
        remoteConfigProvider: () -> FirebaseRemoteConfig,
        catalog: FeatureFlagCatalog,
    ): FeatureFlagProvider = RemoteConfigFeatureFlagProvider(remoteConfigProvider = remoteConfigProvider, catalog = catalog)
}
