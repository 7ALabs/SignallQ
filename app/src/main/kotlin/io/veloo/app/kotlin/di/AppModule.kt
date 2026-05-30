package io.veloo.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.veloo.app.core.database.CoreDatabaseModulo
import io.veloo.app.core.database.VelooDatabase
import io.veloo.app.core.database.MedicaoDao
import io.veloo.app.core.datastore.PreferenciasAppRepository
import io.veloo.app.core.network.CoreNetworkModulo
import io.veloo.app.core.network.DefaultDispatcherProvider
import io.veloo.app.core.network.DispatcherProvider
import io.veloo.app.core.network.MonitorRede
import io.veloo.app.core.network.NetworkCapabilitiesProvider
import io.veloo.app.core.permissions.CorePermissionsModulo
import io.veloo.app.core.permissions.GerenciadorPermissoesRede
import io.veloo.app.core.telephony.CoreTelephonyModulo
import io.veloo.app.core.telephony.MonitorTelephony
import io.veloo.app.feature.devices.FeatureDevicesModulo
import io.veloo.app.feature.devices.ScannerDispositivos
import io.veloo.app.feature.dns.BenchmarkDns
import io.veloo.app.feature.dns.FeatureDnsModulo
import io.veloo.app.feature.fibra.ExecutorFibra
import io.veloo.app.feature.fibra.FeatureFibraModulo
import io.veloo.app.feature.speedtest.ExecutorSpeedtest
import io.veloo.app.feature.speedtest.FeatureSpeedtestModulo
import io.veloo.app.feature.wifi.FeatureWifiModulo
import io.veloo.app.feature.wifi.ScannerRedesWifi
import io.veloo.app.speedtest.SpeedtestPersistenceCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    @Singleton
    fun provideBancoDados(
        @ApplicationContext ctx: Context,
    ): VelooDatabase = CoreDatabaseModulo.criarBanco(ctx)

    @Provides
    @Singleton
    fun providePreferenciasAppRepository(
        @ApplicationContext ctx: Context,
        dispatchers: DispatcherProvider,
    ): PreferenciasAppRepository = PreferenciasAppRepository(ctx, dispatchers.io)

    @Provides
    @Singleton
    fun provideMonitorRede(
        @ApplicationContext ctx: Context,
    ): MonitorRede = CoreNetworkModulo.criarMonitorRede(ctx)

    @Provides
    @Singleton
    fun provideNetworkCapabilitiesProvider(
        @ApplicationContext ctx: Context,
    ): NetworkCapabilitiesProvider = CoreNetworkModulo.criarNetworkCapabilitiesProvider(ctx)

    @Provides
    @Singleton
    fun provideGerenciadorPermissoes(
        @ApplicationContext ctx: Context,
    ): GerenciadorPermissoesRede = CorePermissionsModulo.criarGerenciadorPermissoesRede(ctx)

    @Provides
    @Singleton
    fun provideScannerDispositivos(
        @ApplicationContext ctx: Context,
    ): ScannerDispositivos = FeatureDevicesModulo.criarScannerDispositivos(ctx)

    @Provides
    @Singleton
    fun provideBenchmarkDns(): BenchmarkDns = FeatureDnsModulo.criarBenchmarkDns()

    @Provides
    @Singleton
    fun provideExecutorSpeedtest(networkCapabilitiesProvider: NetworkCapabilitiesProvider): ExecutorSpeedtest =
        FeatureSpeedtestModulo.criarExecutorSpeedtest(
            isMobile = networkCapabilitiesProvider.isMeteredNetwork(),
        )

    @Provides
    @Singleton
    fun provideScannerRedesWifi(
        @ApplicationContext ctx: Context,
    ): ScannerRedesWifi = FeatureWifiModulo.criarScannerRedesWifi(ctx)

    @Provides
    @Singleton
    fun provideExecutorFibra(): ExecutorFibra = FeatureFibraModulo.criarExecutor()

    @Provides
    @Singleton
    fun provideMonitorTelephony(
        @ApplicationContext ctx: Context,
    ): MonitorTelephony = CoreTelephonyModulo.criarMonitorTelephony(ctx)

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideMedicaoDao(bancoDados: VelooDatabase): MedicaoDao = bancoDados.medicaoDao()

    @Provides
    @Singleton
    fun provideSpeedtestPersistenceCoordinator(
        executorSpeedtest: ExecutorSpeedtest,
        medicaoDao: MedicaoDao,
        monitorTelephony: MonitorTelephony,
        monitorRede: MonitorRede,
        @ApplicationScope applicationScope: CoroutineScope,
    ): SpeedtestPersistenceCoordinator =
        SpeedtestPersistenceCoordinator(
            executorSpeedtest = executorSpeedtest,
            medicaoDao = medicaoDao,
            monitorTelephony = monitorTelephony,
            monitorRede = monitorRede,
            applicationScope = applicationScope,
        )
}
