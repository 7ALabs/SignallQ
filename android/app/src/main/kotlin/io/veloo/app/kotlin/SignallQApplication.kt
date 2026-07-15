package io.signallq.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import io.signallq.app.ads.AdsFlagsManager
import io.signallq.app.core.datastore.PreferenciasAppRepository
import io.signallq.app.core.network.AnalyticsTracker
import io.signallq.app.di.ApplicationScope
import io.signallq.app.featureflags.FeatureFlagManager
import io.signallq.app.logging.ReleaseTree
import io.signallq.app.monitoramento.AdminSyncScheduler
import io.signallq.app.notificacao.SignallQNotificationHelper
import io.signallq.app.speedtest.SpeedtestPersistenceCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    lateinit var adsFlagsManager: AdsFlagsManager

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    @Inject
    lateinit var preferenciasAppRepository: PreferenciasAppRepository

    // Reusar o scope singleton provido pelo Hilt — elimina scope duplicado.
    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

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

        // Canais de notificacao: criados em IO para nao bloquear o main thread no startup.
        // O MonitoramentoWorker so dispara apos rede disponivel (constraint CONNECTED) e
        // intervalo minimo de 30 min — canais estarao criados muito antes da primeira notificacao.
        applicationScope.launch(Dispatchers.IO) {
            SignallQNotificationHelper.criarCanais(this@SignallQApplication)
        }

        // Inicia o coordinator singleton que persiste resultados do speedtest no Room.
        // Centraliza a lógica que antes estava duplicada entre ViewModels de diagnóstico.
        speedtestPersistenceCoordinator.iniciar()

        // Agenda sync retroativo de historico para o painel admin.
        // Movido para Default: WorkManager.getInstance() pode inicializar banco interno
        // (Room) na primeira execucao, bloqueando o main thread desnecessariamente.
        applicationScope.launch(Dispatchers.Default) {
            AdminSyncScheduler.agendar(this@SignallQApplication)
        }

        // Migra credenciais do modem de plaintext para EncryptedSharedPreferences.
        // Roda uma vez — nas execucoes seguintes o flag "migrado" curto-circuita.
        applicationScope.launch(Dispatchers.IO) {
            preferenciasAppRepository.migrarCredenciaisSeNecessario()
        }

        // Sincroniza feature flags do worker em background.
        // Nao bloqueia o startup — UI usa fallback (todos enabled) ate o fetch completar.
        featureFlagManager.inicializar(applicationScope)

        // Toggle remoto de anuncios nativos (issue #555) via Firebase Remote Config.
        // Nao bloqueia o startup — fallback local e "desligado" ate o fetch completar.
        adsFlagsManager.inicializar(applicationScope)

        // LGPD: desativa coleta Firebase por padrao. Reativa apenas apos consentimento explícito.
        // setAnalyticsCollectionEnabled deve ser chamado antes do collect para garantir ordem:
        // desabilita primeiro, depois reativa condicionalmente via consentimento.
        firebaseAnalytics.setAnalyticsCollectionEnabled(false)
        applicationScope.launch {
            preferenciasAppRepository.consentimentoLgpdFlow.collect { consentimento ->
                firebaseAnalytics.setAnalyticsCollectionEnabled(consentimento == true)
            }
        }
    }
}
