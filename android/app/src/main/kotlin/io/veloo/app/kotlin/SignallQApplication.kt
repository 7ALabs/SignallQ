package io.signallq.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import io.signallq.app.core.datastore.PreferenciasAppRepository
import io.signallq.app.core.network.AnalyticsTracker
import io.signallq.app.featureflags.FeatureFlagManager
import io.signallq.app.logging.ReleaseTree
import io.signallq.app.monitoramento.AdminSyncScheduler
import io.signallq.app.notificacao.SignallQNotificationHelper
import io.signallq.app.speedtest.SpeedtestPersistenceCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    @Inject
    lateinit var preferenciasAppRepository: PreferenciasAppRepository

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
            // Desabilita coleta de crashes em debug para não poluir dados de produção no Firebase.
            // runCatching: Firebase não é inicializado em testes com Robolectric — evita crash nos 37 unit tests.
            runCatching {
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
            }
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

        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        // Migra credenciais do modem de plaintext para EncryptedSharedPreferences.
        // Roda uma vez — nas execucoes seguintes o flag "migrado" curto-circuita.
        applicationScope.launch(Dispatchers.IO) {
            preferenciasAppRepository.migrarCredenciaisSeNecessario()
        }

        // Sincroniza feature flags do worker em background.
        // Nao bloqueia o startup — UI usa fallback (todos enabled) ate o fetch completar.
        featureFlagManager.inicializar(applicationScope)

        // LGPD: desativa coleta Firebase por padrao. Reativa apenas apos consentimento explícito.
        // setAnalyticsCollectionEnabled sobrevive a reinicializacoes do Firebase SDK.
        firebaseAnalytics.setAnalyticsCollectionEnabled(false)
        applicationScope.launch {
            preferenciasAppRepository.consentimentoLgpdFlow.collect { consentimento ->
                firebaseAnalytics.setAnalyticsCollectionEnabled(consentimento == true)
            }
        }
    }
}
