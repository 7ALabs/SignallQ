package io.signallq.app.featureflags

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.signallq.app.core.featureflags.FeatureFlagKey
import io.signallq.app.core.featureflags.FeatureFlagKeys
import io.signallq.app.core.featureflags.FeatureFlagProvider
import io.signallq.app.core.featureflags.FeatureFlagRawValue
import io.signallq.app.core.featureflags.FeatureFlagSource
import io.signallq.app.core.featureflags.FeatureFlagValue
import io.signallq.app.core.network.AnalyticsTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre o coordinator que deriva [io.signallq.app.ui.screen.AppShellFeatureFlagsState]
 * do [FeatureFlagProvider] real (Epico #1347, F4/#1480) -- garante que uma flag desligada
 * no provider chega desligada no estado consumido por AppShell, e que o bloqueio dispara
 * o evento de analytics tipado sem propagar dado pessoal (so o id curto do modulo).
 */
class ConsumerFeatureGateCoordinatorTest {
    private fun flagFlow(
        key: FeatureFlagKey,
        habilitada: Boolean,
    ) = MutableStateFlow(FeatureFlagValue(key = key, raw = FeatureFlagRawValue.BooleanValue(habilitada), source = FeatureFlagSource.DEFAULT))

    private fun mockProvider(wifiHabilitada: Boolean): FeatureFlagProvider {
        val provider = mockk<FeatureFlagProvider>()
        every { provider.observe(FeatureFlagKeys.CONSUMER_HOME_ENABLED) } returns flagFlow(FeatureFlagKeys.CONSUMER_HOME_ENABLED, true)
        every { provider.observe(FeatureFlagKeys.CONSUMER_SPEEDTEST_ENABLED) } returns
            flagFlow(FeatureFlagKeys.CONSUMER_SPEEDTEST_ENABLED, true)
        every { provider.observe(FeatureFlagKeys.CONSUMER_WIFI_ENABLED) } returns
            flagFlow(FeatureFlagKeys.CONSUMER_WIFI_ENABLED, wifiHabilitada)
        every { provider.observe(FeatureFlagKeys.CONSUMER_DEVICES_ENABLED) } returns
            flagFlow(FeatureFlagKeys.CONSUMER_DEVICES_ENABLED, true)
        every { provider.observe(FeatureFlagKeys.CONSUMER_DNS_ENABLED) } returns flagFlow(FeatureFlagKeys.CONSUMER_DNS_ENABLED, true)
        every { provider.observe(FeatureFlagKeys.CONSUMER_FIBRA_ENABLED) } returns flagFlow(FeatureFlagKeys.CONSUMER_FIBRA_ENABLED, true)
        every { provider.observe(FeatureFlagKeys.CONSUMER_DIAGNOSTICO_ENABLED) } returns
            flagFlow(FeatureFlagKeys.CONSUMER_DIAGNOSTICO_ENABLED, true)
        every { provider.observe(FeatureFlagKeys.CONSUMER_HISTORY_ENABLED) } returns
            flagFlow(FeatureFlagKeys.CONSUMER_HISTORY_ENABLED, true)
        every { provider.observe(FeatureFlagKeys.CONSUMER_SETTINGS_ENABLED) } returns
            flagFlow(FeatureFlagKeys.CONSUMER_SETTINGS_ENABLED, true)
        every { provider.observe(FeatureFlagKeys.CONSUMER_APP_SHELL_GUIDED_2_ENABLED) } returns
            flagFlow(FeatureFlagKeys.CONSUMER_APP_SHELL_GUIDED_2_ENABLED, false)
        return provider
    }

    @Test
    fun `uiState reflete flag desligada do provider (wifi) sem afetar as demais`() =
        runTest {
            val provider = mockProvider(wifiHabilitada = false)
            val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
            val scope = TestScope(StandardTestDispatcher(testScheduler))

            val coordinator = ConsumerFeatureGateCoordinator(provider, analyticsTracker, scope)
            scope.testScheduler.runCurrent()

            val estado = coordinator.uiState.value
            assertFalse(estado.wifiEnabled)
            assertTrue(estado.homeEnabled)
            assertTrue(estado.speedtestEnabled)
            assertTrue(estado.settingsEnabled)
        }

    @Test
    fun `registrarBloqueio dispara AnalyticsTracker com o id curto do modulo`() =
        runTest {
            val provider = mockProvider(wifiHabilitada = true)
            val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
            val scope = TestScope(StandardTestDispatcher(testScheduler))

            val coordinator = ConsumerFeatureGateCoordinator(provider, analyticsTracker, scope)
            coordinator.registrarBloqueio("fibra")

            verify(exactly = 1) { analyticsTracker.registrarFeatureBloqueadaRemota("fibra") }
        }
}
