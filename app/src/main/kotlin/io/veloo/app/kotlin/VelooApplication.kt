package io.veloo.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.veloo.app.logging.ReleaseTree
import io.veloo.app.notificacao.VelooNotificationHelper
import io.veloo.app.speedtest.SpeedtestPersistenceCoordinator
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class VelooApplication : Application() {
    @Inject
    lateinit var speedtestPersistenceCoordinator: SpeedtestPersistenceCoordinator

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        VelooNotificationHelper.criarCanais(this)

        // Inicia o coordinator singleton que persiste resultados do speedtest no Room.
        // Centraliza a lógica que estava duplicada em MainViewModel e ChatDiagnosticoIaViewModel.
        speedtestPersistenceCoordinator.iniciar()
    }
}
