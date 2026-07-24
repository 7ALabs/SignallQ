package io.signallq.app.analytics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.mockk
import io.mockk.verify
import io.signallq.app.BuildConfig
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes unitarios de [FirebaseAnalyticsTracker], com foco na cobertura nova de
 * GH#1360 (user properties `environment`/`dist_channel`/`build_type`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirebaseAnalyticsTrackerTest {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var tracker: FirebaseAnalyticsTracker

    @Before
    fun setUp() {
        firebaseAnalytics = mockk(relaxed = true)
        val context = ApplicationProvider.getApplicationContext<Context>()
        tracker = FirebaseAnalyticsTracker(firebaseAnalytics, context)
    }

    @Test
    fun `registrarSessionStart define as tres user properties de ambiente`() {
        tracker.registrarSessionStart()

        verify { firebaseAnalytics.setUserProperty("environment", any()) }
        verify { firebaseAnalytics.setUserProperty("dist_channel", any()) }
        verify { firebaseAnalytics.setUserProperty("build_type", BuildConfig.BUILD_TYPE) }
    }

    @Test
    fun `registrarSessionStart usa staging como environment sem instalador Play Store`() {
        // Robolectric nao simula instalacao via Play Store — getInstallSourceInfo
        // nao retorna "com.android.vending", entao distributionChannel cai em
        // "sideload"/"unknown" e environmentFor mapeia para "staging" (nunca "production").
        tracker.registrarSessionStart()

        verify { firebaseAnalytics.setUserProperty("environment", "staging") }
    }

    @Test
    fun `registrarSessionStart continua enviando o evento app_session_start`() {
        tracker.registrarSessionStart()

        verify { firebaseAnalytics.logEvent("app_session_start", any()) }
    }
}
