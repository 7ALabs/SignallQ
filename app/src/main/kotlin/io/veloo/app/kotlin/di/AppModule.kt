package io.veloo.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.veloo.app.BuildConfig
import io.veloo.app.core.datastore.FeatureFlagStore
import io.veloo.app.core.network.FeatureFlagProvider
import io.veloo.app.featureflags.FeatureFlagManager
import io.veloo.app.featureflags.FeatureFlagRepository
import io.veloo.app.core.database.CoreDatabaseModulo
import io.veloo.app.core.database.MedicaoDao
import io.veloo.app.core.database.SignallQDatabase
import io.veloo.app.core.database.chat.ChatSessionDao
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
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named
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
    ): SignallQDatabase = CoreDatabaseModulo.criarBanco(ctx)

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

    /**
     * Cliente HTTP para ScannerDispositivosAndroid (SSDP local).
     *
     * Timeout de 2s adequado para descoberta de dispositivos na LAN — redes locais
     * respondem em <1s. Timeout maior aumentaria o tempo total do scan sem benefício.
     */
    @Provides
    @Singleton
    @Named("upnpClient")
    fun provideUpnpOkHttpClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .build()

    /**
     * Cliente HTTP para UpnpIgdDiscovery (IGD/gateway discovery).
     *
     * Timeout de 5s necessário para redes ADSL/4G instáveis onde o roteador pode
     * demorar mais para responder ao fetch do XML de descrição UPnP. Reduzir este
     * valor causa regressão em discovery em redes lentas (banda < 5 Mbps ou alta latência).
     */
    @Provides
    @Singleton
    @Named("upnpIgdClient")
    fun provideUpnpIgdOkHttpClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideScannerDispositivos(
        @ApplicationContext ctx: Context,
        @Named("upnpClient") okHttpClient: OkHttpClient,
    ): ScannerDispositivos = FeatureDevicesModulo.criarScannerDispositivos(ctx, okHttpClient)

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

    /**
     * URL base do signallq-admin-worker para ingest de telemetria.
     * Vem do BuildConfig — nunca hardcoded aqui.
     */
    @Provides
    @Singleton
    @Named("adminIngestUrl")
    fun provideAdminIngestUrl(): String = BuildConfig.ADMIN_INGEST_URL

    /**
     * Chave de autenticacao para /ingest/ do signallq-admin-worker.
     * Scope limitado: so pode escrever em /ingest/. Nao e o ADMIN_SECRET do painel.
     * Vem do BuildConfig — lido de local.properties em dev, CI inject em release.
     */
    @Provides
    @Singleton
    @Named("adminIngestKey")
    fun provideAdminIngestKey(): String = BuildConfig.ADMIN_INGEST_KEY

    /**
     * Repository para busca e persistencia de feature flags remotas.
     * URL derivada do AI_WORKER_URL — remove path /ai-diagnosis se presente.
     */
    @Provides
    @Singleton
    fun provideFeatureFlagRepository(
        store: FeatureFlagStore,
    ): FeatureFlagRepository =
        FeatureFlagRepository(
            workerBaseUrl = io.veloo.app.feature.diagnostico.BuildConfig.AI_WORKER_URL,
            prefs = store,
        )

    /**
     * Expoe PreferenciasAppRepository como FeatureFlagStore.
     * Evita criar uma segunda instancia de PreferenciasAppRepository so para flags.
     */
    @Provides
    @Singleton
    fun provideFeatureFlagStore(prefs: PreferenciasAppRepository): FeatureFlagStore = prefs

    /**
     * Expoe FeatureFlagManager como FeatureFlagProvider para os modulos feature.
     * O FeatureFlagManager e @Singleton via @Inject constructor — aqui apenas bind a interface.
     */
    @Provides
    @Singleton
    fun provideFeatureFlagProvider(manager: FeatureFlagManager): FeatureFlagProvider = manager

    @Provides
    @Singleton
    fun provideMedicaoDao(bancoDados: SignallQDatabase): MedicaoDao = bancoDados.medicaoDao()

    @Provides
    @Singleton
    fun provideChatSessionDao(bancoDados: SignallQDatabase): ChatSessionDao = bancoDados.chatSessionDao()

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
