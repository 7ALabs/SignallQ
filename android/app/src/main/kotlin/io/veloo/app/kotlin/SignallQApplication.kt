package io.veloo.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import io.veloo.app.core.network.AnalyticsTracker
import io.veloo.app.featureflags.FeatureFlagManager
import io.veloo.app.logging.ReleaseTree
import io.veloo.app.monitoramento.AdminSyncScheduler
import io.veloo.app.notificacao.SignallQNotificationHelper
import io.veloo.app.speedtest.SpeedtestPersistenceCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class SignallQApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var speedtestPersistenceCoordinator: SpeedtestPersistenceCoordinator

    @Inject
    lateinit var featureFlagManager: FeatureFlagManager

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree(analyticsTracker))
        }

        SignallQNotificationHelper.criarCanais(this)

        // Inicia o coordinator singleton que persiste resultados do speedtest no Room.
        // Centraliza a lógica que estava duplicada em MainViewModel e ChatDiagnosticoIaViewModel.
        speedtestPersistenceCoordinator.iniciar()

        // Agenda sync retroativo de historico para o painel admin.
        // One-shot com KEEP: roda uma vez por instalacao (ou apos crash/wipe), depois so periodico.
        // Nao bloqueia o startup — o WorkManager agenda na proxima oportunidade com rede disponivel.
        AdminSyncScheduler.agendar(this)

        // Sincroniza feature flags do worker em background.
        // Nao bloqueia o startup — UI usa fallback (todos enabled) ate o fetch completar.
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        featureFlagManager.inicializar(applicationScope)
    }
}
