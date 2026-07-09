package io.signallq.app.analytics

import androidx.test.core.app.ApplicationProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.signallq.app.core.datastore.PreferenciasAppRepository
import io.signallq.app.feature.diagnostico.ingest.AdminIngestRepository
import io.signallq.app.feature.diagnostico.ingest.AnalyticsEventIngestPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
    private lateinit var tracker: CompositeAnalyticsTracker

    @Before
    fun setUp() {
        firebaseTracker = mockk(relaxed = true)
        adminIngestRepository = mockk(relaxed = true)
        preferenciasAppRepository = mockk(relaxed = true)
        coEvery { preferenciasAppRepository.buscarOuGerarAnonDeviceId() } returns "device-anon-123"

        val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        tracker =
            CompositeAnalyticsTracker(
                firebaseTracker = firebaseTracker,
                adminIngestRepository = adminIngestRepository,
                preferenciasAppRepository = preferenciasAppRepository,
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

    @Test
    fun `sessionId permanece o mesmo entre eventos consecutivos`() {
        tracker.registrarFeatureUsada("wifi_scan")
        tracker.registrarScreenView("wifi")

        val slots = mutableListOf<AnalyticsEventIngestPayload>()
        coVerify(exactly = 2) { adminIngestRepository.sendAnalyticsEvent(capture(slots)) }
        assertEquals(slots[0].sessionId, slots[1].sessionId)
        assertTrue(slots[0].sessionId!!.isNotBlank())
    }
}
