package io.linka.app.kotlin

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.linka.app.kotlin.logging.ReleaseTree
import io.linka.app.kotlin.notificacao.LinkaNotificationHelper
import io.linka.app.kotlin.speedtest.SpeedtestPersistenceCoordinator
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class LinkaApplication : Application() {
    @Inject
    lateinit var speedtestPersistenceCoordinator: SpeedtestPersistenceCoordinator

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        LinkaNotificationHelper.criarCanais(this)

        // Inicia o coordinator singleton que persiste resultados do speedtest no Room.
        // Centraliza a lógica que estava duplicada em MainViewModel e ChatDiagnosticoIaViewModel.
        speedtestPersistenceCoordinator.iniciar()
    }
}
