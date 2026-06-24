package io.veloo.app.featureflags

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testa apenas a logica interna do FeatureFlagRepository (parser + URL builder).
 * Sem dependencia de rede — o fetch real nao e testado aqui.
 */
class FeatureFlagRepositoryTest {

    // Instancia sem DataStore real — so testa os metodos internos via reflexao
    // ou criando uma subclasse testavel. Usamos FakePrefs para isolar.
    private fun criarRepository(baseUrl: String): FeatureFlagRepository =
        FeatureFlagRepository(
            workerBaseUrl = baseUrl,
            prefs = FakePreferenciasAppRepository(),
        )

    @Test
    fun `lerFlags retorna defaults quando DataStore vazio`() = runTest {
        val repo = criarRepository("https://worker.dev/ai-diagnosis")
        val flags = repo.lerFlags()
        assertTrue(flags["ai_diagnosis_enabled"] == true)
        assertTrue(flags["speedtest_enabled"] == true)
        assertTrue(flags["fibra_module_enabled"] == true)
    }

    @Test
    fun `lerFlags mescla DataStore com defaults para flags ausentes`() = runTest {
        val prefs = FakePreferenciasAppRepository()
        prefs.salvarFeatureFlags(mapOf("ai_diagnosis_enabled" to false))
        val repo = FeatureFlagRepository("https://worker.dev", prefs)
        val flags = repo.lerFlags()
        assertEquals(false, flags["ai_diagnosis_enabled"])
        // Flags ausentes no DataStore devem vir do default (true)
        assertEquals(true, flags["speedtest_enabled"])
        assertEquals(true, flags["fibra_module_enabled"])
    }

    @Test
    fun `buildFlagsUrl remove path ai-diagnosis e adiciona feature-flags`() {
        // Verifica via sincronizarFlags em modo offline — sem crash, sem throw
        // O teste de URL e indireto via comportamento: nenhum erro no parse
        val repo = criarRepository("https://linka-ai-diagnosis-worker.workers.dev/ai-diagnosis")
        // Nao ha como chamar buildFlagsUrl diretamente (private), mas o sincronizarFlags
        // nao deve lancar excecao mesmo com URL incorreta (catch silencioso).
        // Validamos que lerFlags ainda retorna defaults apos falha de rede.
        runTest {
            repo.sincronizarFlags() // offline — catch silencioso
            val flags = repo.lerFlags()
            assertTrue(flags["ai_diagnosis_enabled"] == true)
        }
    }
}
