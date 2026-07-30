package io.signallq.pro.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.signallq.app.core.datastore.PreferenciasAppRepository
import io.signallq.app.core.network.AnalyticsHelper
import io.signallq.app.core.network.AnalyticsTracker
import io.signallq.app.core.network.CoreNetworkModulo
import io.signallq.app.core.network.DefaultDispatcherProvider
import io.signallq.app.core.network.DispatcherProvider
import io.signallq.app.core.network.MonitorRede
import io.signallq.app.core.network.NetworkCapabilitiesProvider
import io.signallq.app.core.network.NoOpAnalyticsHelper
import io.signallq.app.core.network.contracts.connectivity.ConnectivityDiagnosis
import io.signallq.app.core.telephony.CoreTelephonyModulo
import io.signallq.app.core.telephony.MonitorTelephony
import io.signallq.app.feature.speedtest.ExecutorSpeedtest
import io.signallq.app.feature.speedtest.FeatureSpeedtestModulo
import io.signallq.app.feature.speedtest.connectivity.ConnectivityDiagnosisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Singleton

/**
 * Compatibilidade de DI para reaproveitar `:featureSpeedtest` no Pro (issue #1161, Fase 2).
 *
 * `:featureSpeedtest` expõe `SpeedtestViewModel` (`@HiltViewModel`) — o processador de
 * agregação do Hilt inclui esse ViewModel no grafo de QUALQUER app que tenha o módulo no
 * classpath, mesmo que o Pro nunca chame `hiltViewModel<SpeedtestViewModel>()` (o Pro usa o
 * motor via `FeatureSpeedtestModulo.criarExecutorSpeedtest()` direto, sem Hilt). Sem estes
 * bindings o `:pro:app` não compila (`Dagger/MissingBinding`).
 *
 * `MonitorRede`/`NetworkCapabilitiesProvider`/`MonitorTelephony`/`DispatcherProvider` são
 * infra genuinamente reutilizável (`:coreNetwork`/`:coreTelephony`, zero acoplamento ao
 * consumidor) — instanciados de verdade, escopados ao processo do Pro.
 * `PreferenciasAppRepository` também é reutilizável (`:coreDatastore`), mas grava num
 * DataStore próprio do processo do Pro -- não compartilha arquivo com o consumidor (apps
 * diferentes, processos diferentes).
 * `AnalyticsHelper`/`AnalyticsTracker` usam no-op: o Pro ainda não tem projeto Firebase
 * próprio configurado (pendente, issue #1158) -- `NoOpAnalyticsHelper` já é o padrão oficial
 * do próprio `:coreNetwork` para uso fora do grafo Hilt do consumidor.
 */
@Module
@InstallIn(SingletonComponent::class)
object ProSpeedtestCompatModule {
    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    @Singleton
    fun providePreferenciasAppRepository(
        @ApplicationContext context: Context,
        dispatchers: DispatcherProvider,
    ): PreferenciasAppRepository = PreferenciasAppRepository(context, dispatchers.io)

    @Provides
    @Singleton
    fun provideMonitorRede(
        @ApplicationContext context: Context,
    ): MonitorRede = CoreNetworkModulo.criarMonitorRede(context)

    @Provides
    @Singleton
    fun provideNetworkCapabilitiesProvider(
        @ApplicationContext context: Context,
    ): NetworkCapabilitiesProvider = CoreNetworkModulo.criarNetworkCapabilitiesProvider(context)

    @Provides
    @Singleton
    fun provideMonitorTelephony(
        @ApplicationContext context: Context,
    ): MonitorTelephony = CoreTelephonyModulo.criarMonitorTelephony(context)

    @Provides
    @Singleton
    fun provideAnalyticsHelper(): AnalyticsHelper = NoOpAnalyticsHelper

    @Provides
    @Singleton
    fun provideAnalyticsTracker(): AnalyticsTracker = NoOpAnalyticsTrackerPro

    /** [SpeedtestViewModel] em si nunca é chamado pelo Pro -- só satisfaz o grafo agregado
     *  do Hilt. `:pro:feature:medicao-diagnostico` cria seu próprio executor via
     *  [FeatureSpeedtestModulo] diretamente, sem depender deste binding. */
    @Provides
    fun provideExecutorSpeedtest(): ExecutorSpeedtest = FeatureSpeedtestModulo.criarExecutorSpeedtest()

    /** GH#1512 -- [SpeedtestViewModel] passou a exigir `ConnectivityDiagnosisRepository`
     *  no construtor, então o Pro precisa satisfazer esse binding pelo mesmo motivo de
     *  [provideExecutorSpeedtest] acima. Não usa a implementação real
     *  (`ConnectivityDiagnosisRepositoryImpl`, que depende de `ConnectivityDiagnosisHistoryDao`
     *  -- Room do Consumer, `:coreDatabase`, fora do classpath do Pro por design): o Pro
     *  nunca chama este repositório de verdade. */
    @Provides
    fun provideConnectivityDiagnosisRepository(): ConnectivityDiagnosisRepository = NoOpConnectivityDiagnosisRepositoryPro
}

/** Sem equivalente oficial em `:coreNetwork` (só [NoOpAnalyticsHelper] existe lá) --
 *  no-op mínimo próprio do Pro, mesmo espírito. */
private object NoOpAnalyticsTrackerPro : AnalyticsTracker {
    override fun registrarFeatureUsada(
        featureId: String,
        sessionIdOverride: String?,
    ) = Unit

    override fun registrarScreenView(screenName: String) = Unit

    override fun registrarSessionStart() = Unit

    override fun registrarFeatureCrash(
        featureId: String,
        errorType: String,
    ) = Unit

    override fun registrarBatterySnapshot(
        level: Int,
        charging: Boolean,
    ) = Unit

    // GH#1480 -- feature flags do Consumer (:core:featureflags) nao se aplicam ao Pro
    // (fora de escopo do Epico #1347), mas o contrato precisa do metodo mesmo assim.
    override fun registrarFeatureBloqueadaRemota(featureId: String) = Unit
}

/** GH#1512 -- ver kdoc de `provideConnectivityDiagnosisRepository` acima: só satisfaz o
 *  grafo agregado do Hilt, nunca é chamado de verdade pelo Pro. */
private object NoOpConnectivityDiagnosisRepositoryPro : ConnectivityDiagnosisRepository {
    override val ultimoDiagnostico: StateFlow<ConnectivityDiagnosis?> = MutableStateFlow(null)

    override suspend fun diagnosticar(): ConnectivityDiagnosis {
        error("ConnectivityDiagnosisRepository nao e usado pelo Pro -- so satisfaz o grafo Hilt agregado (GH#1512)")
    }
}
