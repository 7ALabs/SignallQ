package io.signallq.app.featureflags

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testa logica interna do FeatureFlagRepository (parser + defaults + mescla).
 * Sem dependencia de rede — o fetch real nao e testado aqui.
 */
class FeatureFlagRepositoryTest {
    private fun criarRepository(
        baseUrl: String = "https://signallq-admin.workers.dev",
        prefs: FakePreferenciasAppRepository = FakePreferenciasAppRepository(),
    ): FeatureFlagRepository =
        FeatureFlagRepository(
            adminWorkerBaseUrl = baseUrl,
            prefs = prefs,
        )

    @Test
    fun `lerFlags retorna defaults SIG-13 quando DataStore vazio`() =
        runTest {
            val flags = criarRepository().lerFlags()
            assertTrue(flags["feature_speedtest"] == true)
            assertTrue(flags["feature_wifi"] == true)
            assertTrue(flags["feature_diagnostico_ia"] == true)
            assertTrue(flags["feature_dns"] == true)
            assertTrue(flags["feature_fibra"] == true)
            assertTrue(flags["feature_devices"] == true)
        }

    @Test
    fun `lerFlags retorna defaults legados quando DataStore vazio`() =
        runTest {
            val flags = criarRepository().lerFlags()
            assertTrue(flags["ai_diagnosis_enabled"] == true)
            assertTrue(flags["speedtest_enabled"] == true)
            assertTrue(flags["fibra_module_enabled"] == true)
        }

    @Test
    fun `lerFlags mescla DataStore com defaults para flags ausentes`() =
        runTest {
            val prefs = FakePreferenciasAppRepository()
            prefs.salvarFeatureFlags(mapOf("feature_speedtest" to false, "ai_diagnosis_enabled" to false))
            val flags = criarRepository(prefs = prefs).lerFlags()
            assertEquals(false, flags["feature_speedtest"])
            assertEquals(false, flags["ai_diagnosis_enabled"])
            // Flags ausentes no DataStore devem vir do default (true)
            assertEquals(true, flags["feature_wifi"])
            assertEquals(true, flags["speedtest_enabled"])
        }

    @Test
    fun `sincronizarFlags e silencioso em modo offline`() =
        runTest {
            val repo = criarRepository("https://url-invalida-que-nao-existe.dev")
            // Nao deve lancar excecao — catch silencioso interno
            repo.sincronizarFlags()
            val flags = repo.lerFlags()
            // Sem dados salvos, retorna defaults
            assertTrue(flags["feature_speedtest"] == true)
        }
}
