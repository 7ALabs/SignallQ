package io.veloo.app.featureflags

import io.veloo.app.core.network.FeatureFlagProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureFlagManagerTest {
    private fun criarManager(flagsSalvas: Map<String, Boolean> = emptyMap()): FeatureFlagManager {
        val prefs = FakePreferenciasAppRepository()
        if (flagsSalvas.isNotEmpty()) {
            kotlinx.coroutines.runBlocking { prefs.salvarFeatureFlags(flagsSalvas) }
        }
        val repo = FeatureFlagRepository(adminWorkerBaseUrl = "https://worker.dev", prefs = prefs)
        return FeatureFlagManager(repo)
    }

    @Test
    fun `isEnabled retorna true por padrao antes de inicializar`() {
        val manager = criarManager()
        assertTrue(manager.isEnabled("ai_diagnosis_enabled"))
        assertTrue(manager.isEnabled("flag_desconhecida"))
    }

    @Test
    fun `isAiDiagnosisEnabled retorna false quando flag desabilitada`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val testScope = TestScope(dispatcher)
            val manager = criarManager(mapOf("ai_diagnosis_enabled" to false))
            manager.inicializar(testScope)
            advanceUntilIdle()
            assertFalse(manager.isAiDiagnosisEnabled())
        }

    @Test
    fun `isSpeedtestEnabled retorna true por padrao`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val testScope = TestScope(dispatcher)
            val manager = criarManager()
            manager.inicializar(testScope)
            advanceUntilIdle()
            assertTrue(manager.isSpeedtestEnabled())
        }

    @Test
    fun `implementa FeatureFlagProvider`() {
        val manager = criarManager()
        // A linha abaixo compila apenas se FeatureFlagManager implementa FeatureFlagProvider.
        val provider: FeatureFlagProvider = manager
        assertTrue(provider.isEnabled("qualquer_flag"))
    }
}
