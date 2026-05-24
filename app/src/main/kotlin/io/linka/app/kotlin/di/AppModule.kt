package io.linka.app.kotlin.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.linka.app.kotlin.core.database.CoreDatabaseModulo
import io.linka.app.kotlin.core.database.LinkaDatabase
import io.linka.app.kotlin.core.datastore.CoreDatastoreModulo
import io.linka.app.kotlin.core.datastore.PreferenciasAppRepository
import io.linka.app.kotlin.core.network.CoreNetworkModulo
import io.linka.app.kotlin.core.network.MonitorRede
import io.linka.app.kotlin.core.network.NetworkCapabilitiesProvider
import io.linka.app.kotlin.core.permissions.CorePermissionsModulo
import io.linka.app.kotlin.core.permissions.GerenciadorPermissoesRede
import io.linka.app.kotlin.core.telephony.CoreTelephonyModulo
import io.linka.app.kotlin.core.telephony.MonitorTelephony
import io.linka.app.kotlin.feature.devices.FeatureDevicesModulo
import io.linka.app.kotlin.feature.devices.ScannerDispositivos
import io.linka.app.kotlin.feature.dns.BenchmarkDns
import io.linka.app.kotlin.feature.dns.FeatureDnsModulo
import io.linka.app.kotlin.feature.fibra.ExecutorFibra
import io.linka.app.kotlin.feature.fibra.FeatureFibraModulo
import io.linka.app.kotlin.feature.speedtest.ExecutorSpeedtest
import io.linka.app.kotlin.feature.speedtest.FeatureSpeedtestModulo
import io.linka.app.kotlin.feature.wifi.FeatureWifiModulo
import io.linka.app.kotlin.feature.wifi.ScannerRedesWifi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBancoDados(@ApplicationContext ctx: Context): LinkaDatabase =
        CoreDatabaseModulo.criarBanco(ctx)

    @Provides
    @Singleton
    fun providePreferenciasAppRepository(@ApplicationContext ctx: Context): PreferenciasAppRepository =
        CoreDatastoreModulo.criarPreferenciasAppRepository(ctx)

    @Provides
    @Singleton
    fun provideMonitorRede(@ApplicationContext ctx: Context): MonitorRede =
        CoreNetworkModulo.criarMonitorRede(ctx)

    @Provides
    @Singleton
    fun provideNetworkCapabilitiesProvider(@ApplicationContext ctx: Context): NetworkCapabilitiesProvider =
        CoreNetworkModulo.criarNetworkCapabilitiesProvider(ctx)

    @Provides
    @Singleton
    fun provideGerenciadorPermissoes(@ApplicationContext ctx: Context): GerenciadorPermissoesRede =
        CorePermissionsModulo.criarGerenciadorPermissoesRede(ctx)

    @Provides
    @Singleton
    fun provideScannerDispositivos(@ApplicationContext ctx: Context): ScannerDispositivos =
        FeatureDevicesModulo.criarScannerDispositivos(ctx)

    @Provides
    @Singleton
    fun provideBenchmarkDns(): BenchmarkDns =
        FeatureDnsModulo.criarBenchmarkDns()

    @Provides
    @Singleton
    fun provideExecutorSpeedtest(): ExecutorSpeedtest =
        FeatureSpeedtestModulo.criarExecutorSpeedtest()

    @Provides
    @Singleton
    fun provideScannerRedesWifi(@ApplicationContext ctx: Context): ScannerRedesWifi =
        FeatureWifiModulo.criarScannerRedesWifi(ctx)

    @Provides
    @Singleton
    fun provideExecutorFibra(): ExecutorFibra =
        FeatureFibraModulo.criarExecutor()

    @Provides
    @Singleton
    fun provideMonitorTelephony(@ApplicationContext ctx: Context): MonitorTelephony =
        CoreTelephonyModulo.criarMonitorTelephony(ctx)
}
