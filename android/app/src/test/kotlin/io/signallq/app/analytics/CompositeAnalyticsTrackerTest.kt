@file:Suppress("ForbiddenImport") // Mesma justificativa de ReleaseTree.kt: so as constantes de
// prioridade (Log.WARN/Log.ERROR) para replicar o contrato dela em teste, nao logging direto.

package io.signallq.app.analytics

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.signallq.app.core.database.analytics.AnalyticsOutboxDao
import io.signallq.app.core.database.analytics.AnalyticsOutboxEntity
import io.signallq.app.core.datastore.PreferenciasAppRepository
import io.signallq.app.feature.diagnostico.ingest.AdminIngestRepository
import io.signallq.app.feature.diagnostico.ingest.AnalyticsEventIngestPayload
import io.signallq.app.feature.diagnostico.ingest.analyticsPayloadFromOutboxJson
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import timber.log.Timber

/**
 * Testes unitarios de [CompositeAnalyticsTracker] (GH#759) — cobertura que ficou
 * pendente no PR #762 (que introduziu a classe). Garante que cada evento de
 * produto dispara tanto para o Firebase (GA4, ja existia) quanto para o
 * signallq-admin-worker via [AdminIngestRepository.sendAnalyticsEvent]
 * (POST /ingest/analytics), com o payload correto por tipo de evento.
 *
 * O gate de consentimento LGPD/baseUrl vazio e responsabilidade de
 * [AdminIngestRepository] (ja coberto pelo padrao fire-and-forget dela) — nao
 * duplicado aqui.
 *
 * Dispatchers.Unconfined faz o `applicationScope.launch { ... }` do tracker
 * executar de forma sincrona (sem suspensao real, ja que os mocks respondem
 * imediatamente), entao os verify() logo apos a chamada ja veem o evento.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CompositeAnalyticsTrackerTest {
    private lateinit var firebaseTracker: FirebaseAnalyticsTracker
    private lateinit var adminIngestRepository: AdminIngestRepository
    private lateinit var preferenciasAppRepository: PreferenciasAppRepository
    private lateinit var analyticsOutboxDao: AnalyticsOutboxDao
    private lateinit var outboxFunnelTracker: AnalyticsOutboxFunnelTracker
    private lateinit var tracker: CompositeAnalyticsTracker

    @Before
    fun setUp() {
        firebaseTracker = mockk(relaxed = true)
        adminIngestRepository = mockk(relaxed = true)
        preferenciasAppRepository = mockk(relaxed = true)
        analyticsOutboxDao = mockk(relaxed = true)
        outboxFunnelTracker = mockk(relaxed = true)
        coEvery { preferenciasAppRepository.buscarOuGerarAnonDeviceId() } returns "device-anon-123"
        coEvery { adminIngestRepository.canSendTelemetry() } returns true
        coEvery { analyticsOutboxDao.enqueue(any()) } coAnswers {
            val entry = firstArg<AnalyticsOutboxEntity>()
            adminIngestRepository.sendAnalyticsEvent(analyticsPayloadFromOutboxJson(entry.payloadJson)!!)
            1L
        }

        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        tracker =
            CompositeAnalyticsTracker(
                firebaseTracker = firebaseTracker,
                adminIngestRepository = adminIngestRepository,
                preferenciasAppRepository = preferenciasAppRepository,
                analyticsOutboxDao = analyticsOutboxDao,
                outboxFunnelTracker = outboxFunnelTracker,
                context = ApplicationProvider.getApplicationContext(),
                applicationScope = scope,
            )
    }

    @Test
    fun `registrarFeatureUsada dispara para Firebase e admin-worker`() {
        tracker.registrarFeatureUsada("speedtest")

        verify { firebaseTracker.registrarFeatureUsada("speedtest") }
        val slot = slot<AnalyticsEventIngestPayload>()
        coVerify { adminIngestRepository.sendAnalyticsEvent(capture(slot)) }
        assertEquals("feature_used", slot.captured.name)
        assertEquals("speedtest", slot.captured.featureId)
        assertEquals("device-anon-123", slot.captured.deviceId)
    }

    @Test
    fun `registrarScreenView envia screen_name para o admin-worker`() {
        tracker.registrarScreenView("home")

        verify { firebaseTracker.registrarScreenView("home") }
        val slot = slot<AnalyticsEventIngestPayload>()
        coVerify { adminIngestRepository.sendAnalyticsEvent(capture(slot)) }
        assertEquals("screen_view", slot.captured.name)
        assertEquals("home", slot.captured.screenName)
    }

    @Test
    fun `registrarSessionStart nao preenche campos opcionais`() {
        tracker.registrarSessionStart()

        val slot = slot<AnalyticsEventIngestPayload>()
        coVerify { adminIngestRepository.sendAnalyticsEvent(capture(slot)) }
        assertEquals("session_start", slot.captured.name)
        assertNull(slot.captured.featureId)
        assertNull(slot.captured.screenName)
        assertNull(slot.captured.errorType)
    }

    @Test
    fun `registrarSessionEnd fecha a mesma sessao no Firebase e no admin-worker`() {
        tracker.registrarSessionStart()
        Thread.sleep(2)
        tracker.registrarSessionEnd()

        verify { firebaseTracker.registrarSessionEnd() }
        val slots = mutableListOf<AnalyticsEventIngestPayload>()
        coVerify(exactly = 2) { adminIngestRepository.sendAnalyticsEvent(capture(slots)) }
        assertEquals("session_start", slots[0].name)
        assertEquals("session_end", slots[1].name)
        assertEquals(slots[0].sessionId, slots[1].sessionId)
        assertTrue(slots[0].id != slots[1].id)
        assertTrue(requireNotNull(slots[1].durationMs) > 0)
    }

    @Test
    fun `ciclo foreground background foreground renova a sessao`() {
        tracker.registrarSessionStart()
        tracker.registrarSessionEnd()
        tracker.registrarSessionStart()

        val slots = mutableListOf<AnalyticsEventIngestPayload>()
        coVerify(exactly = 3) { adminIngestRepository.sendAnalyticsEvent(capture(slots)) }
        assertEquals("session_start", slots[0].name)
        assertEquals("session_end", slots[1].name)
        assertEquals("session_start", slots[2].name)
        assertEquals(slots[0].sessionId, slots[1].sessionId)
        assertNotEquals(slots[0].sessionId, slots[2].sessionId)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `start e stop rapidos preservam o par de sessao quando coroutine executa depois`() =
        runTest {
            val delayedTracker =
                CompositeAnalyticsTracker(
                    firebaseTracker = firebaseTracker,
                    adminIngestRepository = adminIngestRepository,
                    preferenciasAppRepository = preferenciasAppRepository,
                    analyticsOutboxDao = analyticsOutboxDao,
                    outboxFunnelTracker = outboxFunnelTracker,
                    context = ApplicationProvider.getApplicationContext(),
                    applicationScope = CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob()),
                )

            delayedTracker.registrarSessionStart()
            delayedTracker.registrarSessionEnd()
            advanceUntilIdle()

            val slots = mutableListOf<AnalyticsEventIngestPayload>()
            coVerify(exactly = 2) { adminIngestRepository.sendAnalyticsEvent(capture(slots)) }
            assertEquals("session_start", slots[0].name)
            assertEquals("session_end", slots[1].name)
            assertEquals(slots[0].sessionId, slots[1].sessionId)
        }

    @Test
    fun `registrarFeatureCrash envia featureId e errorType para o admin-worker`() {
        tracker.registrarFeatureCrash("dns_diagnostico", "NullPointerException")

        verify { firebaseTracker.registrarFeatureCrash("dns_diagnostico", "NullPointerException") }
        val slot = slot<AnalyticsEventIngestPayload>()
        coVerify { adminIngestRepository.sendAnalyticsEvent(capture(slot)) }
        assertEquals("feature_crash", slot.captured.name)
        assertEquals("dns_diagnostico", slot.captured.featureId)
        assertEquals("NullPointerException", slot.captured.errorType)
    }

    @Test
    fun `registrarBatterySnapshot envia level e charging para o admin-worker`() {
        tracker.registrarBatterySnapshot(level = 42, charging = true)

        verify { firebaseTracker.registrarBatterySnapshot(42, true) }
        val slot = slot<AnalyticsEventIngestPayload>()
        coVerify { adminIngestRepository.sendAnalyticsEvent(capture(slot)) }
        assertEquals("battery_snapshot", slot.captured.name)
        assertEquals(42, slot.captured.batteryLevel)
        assertEquals(true, slot.captured.batteryCharging)
    }

    // GH#1480 (Epico #1347, F4) — decisao registrada no KDoc do metodo: encaminha so ao
    // Firebase, sem replicar pro ingest do admin-worker (schema separado, fora de escopo).
    @Test
    fun `registrarFeatureBloqueadaRemota encaminha so ao Firebase, sem chamar o admin-worker`() {
        tracker.registrarFeatureBloqueadaRemota("fibra")

        verify { firebaseTracker.registrarFeatureBloqueadaRemota("fibra") }
        coVerify(exactly = 0) { adminIngestRepository.sendAnalyticsEvent(any()) }
    }

    @Test
    fun `sessionId permanece o mesmo entre eventos consecutivos`() {
        tracker.registrarFeatureUsada("wifi_scan")
        tracker.registrarScreenView("wifi")

        val slots = mutableListOf<AnalyticsEventIngestPayload>()
        coVerify(exactly = 2) { adminIngestRepository.sendAnalyticsEvent(capture(slots)) }
        assertEquals(slots[0].sessionId, slots[1].sessionId)
        assertTrue(slots[0].sessionId!!.isNotBlank())
    }

    // GH#1684 (bloqueio 1 da revisao do Caio na PR #1688) -- prova que uma falha real dentro do
    // corpo do applicationScope.launch de enviarEvento (ex.: analyticsOutboxDao.enqueue lancando,
    // como um WorkManager.getInstance(context) sem guarda faria em AdminSyncScheduler) fica
    // contida por runCatching e NAO escapa para o CoroutineExceptionHandler do escopo. Sem essa
    // contencao, a excecao chegaria ao applicationScopeExceptionHandler (AppModule.kt) que a
    // reporta via Timber -> ReleaseTree -> registrarFeatureCrash -> enviarEvento -> este mesmo
    // launch -- um ciclo sem limite, backoff ou dedup, invisivel em debug e nos 580 testes porque
    // so existe com ReleaseTree plantada.
    @Test
    fun `falha real dentro de enviarEvento fica contida e nao realimenta o applicationScope`() {
        coEvery { analyticsOutboxDao.enqueue(any()) } throws RuntimeException("falha simulada de outbox")
        var excecaoVazadaParaOEscopo: Throwable? = null
        val handlerDoEscopo = CoroutineExceptionHandler { _, throwable -> excecaoVazadaParaOEscopo = throwable }
        val trackerComHandlerNoEscopo =
            CompositeAnalyticsTracker(
                firebaseTracker = firebaseTracker,
                adminIngestRepository = adminIngestRepository,
                preferenciasAppRepository = preferenciasAppRepository,
                analyticsOutboxDao = analyticsOutboxDao,
                outboxFunnelTracker = outboxFunnelTracker,
                context = ApplicationProvider.getApplicationContext(),
                applicationScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob() + handlerDoEscopo),
            )

        trackerComHandlerNoEscopo.registrarFeatureUsada("wifi_scan")

        assertNull(excecaoVazadaParaOEscopo)
    }

    // GH#1684 (bloqueio 1, RODADA 2 da revisao do Caio na PR #1688) -- o teste acima prova que a
    // excecao fica contida (nao escapa do applicationScope), mas isso sozinho NAO prova que o
    // ciclo esta quebrado: com o runCatching intacto e Timber.e (em vez de Timber.w) no
    // onFailure, o ciclo original reabre por INTEIRO -- so que roteado pelo ReleaseTree em vez do
    // CoroutineExceptionHandler -- porque Timber.e tem prioridade >= Log.ERROR, e e exatamente
    // essa prioridade que faz ReleaseTree.log() chamar registrarFeatureCrash -> enviarEvento ->
    // o MESMO applicationScope.launch de novo. Uma excecao nunca "escapa" nesse cenario -- ela so
    // reentra indefinidamente dentro do proprio escopo, entao o teste acima nao pega essa
    // regressao. Este teste trava a escolha especifica (Timber.w, nao so "algum runCatching"):
    // planta uma replica do contrato de ReleaseTree.log() (prioridade >= Log.ERROR chama
    // registrarFeatureCrash, senao nao) e afirma que, com enqueue sempre lancando, ele roda
    // EXATAMENTE uma vez -- se `Timber.w` de enviarEvento virar `Timber.e`, a replica chama
    // registrarFeatureCrash de novo, o ciclo reabre, e o enqueue roda mais de uma vez (na
    // pratica, recursao sem limite -- sob Dispatchers.Unconfined isso se manifesta como
    // OutOfMemoryError antes de uma contagem de chamada estavel ser observavel -- confirmado
    // mutando manualmente Timber.w -> Timber.e nesta suite: falha, restaurado depois. Qualquer um
    // dos dois desfechos [OutOfMemoryError ou coVerify != 1] basta pra travar a regressao.
    //
    // Nao usamos o ReleaseTree real (io.signallq.app.logging.ReleaseTree) aqui: ele chama
    // FirebaseCrashlytics.getInstance() sem guarda, que lanca sob Robolectric -- e por isso que
    // SignallQApplication.onCreate() so o planta quando `!BuildConfig.DEBUG`, e builds de teste
    // sao sempre debug (`Timber.plant(Timber.DebugTree())` no outro branch). Os 580+ testes
    // normais nunca passam pelo ReleaseTree de verdade; a replica reproduz so o que importa pro
    // invariante sob teste, sem tocar Firebase.
    @Test
    fun `Timber_w no onFailure de enviarEvento nao reabre o ciclo via um ReleaseTree equivalente`() {
        coEvery { analyticsOutboxDao.enqueue(any()) } throws RuntimeException("falha simulada de outbox")
        val arvoreReplicaReleaseTree =
            object : Timber.Tree() {
                override fun log(
                    priority: Int,
                    tag: String?,
                    message: String,
                    t: Throwable?,
                ) {
                    if (priority < Log.WARN) return
                    if (priority >= Log.ERROR) {
                        tracker.registrarFeatureCrash(tag ?: "desconhecido", t?.javaClass?.simpleName ?: "Erro")
                    }
                }
            }
        Timber.plant(arvoreReplicaReleaseTree)

        try {
            tracker.registrarFeatureUsada("wifi_scan")

            coVerify(exactly = 1) { analyticsOutboxDao.enqueue(any()) }
        } finally {
            // Timber.Forest e estado global do processo -- exatamente a classe de problema desta
            // issue. Nunca deixar uma arvore plantada por um teste vazar para os outros.
            Timber.uproot(arvoreReplicaReleaseTree)
        }
    }
}
