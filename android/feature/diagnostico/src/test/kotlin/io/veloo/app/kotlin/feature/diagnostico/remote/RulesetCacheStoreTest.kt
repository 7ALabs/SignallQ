package io.signallq.app.feature.diagnostico.remote

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * Cobre a persistencia atomica do ultimo ruleset remoto valido (GH#1450, secao
 * "Fallback local" de #952): escrita atomica, corrupcao nunca derruba o app, e
 * conteudo invalido nunca substitui um cache valido ja existente.
 */
class RulesetCacheStoreTest {

    private lateinit var cacheDir: File
    private lateinit var store: FileRulesetCacheStore

    @Before
    fun setUp() {
        cacheDir = Files.createTempDirectory("ruleset-cache-test").toFile()
        store = FileRulesetCacheStore(cacheDir)
    }

    @After
    fun tearDown() {
        cacheDir.deleteRecursively()
    }

    @Test
    fun `sem cache persistido - load retorna null`() {
        assertNull(store.load())
    }

    @Test
    fun `save com conteudo valido - load devolve o mesmo conteudo e metadados`() {
        val ok = store.save(content = payload("v1"), rulesetVersion = 3, syncedAtMs = 1_000L)

        assertTrue(ok)
        val cached = store.load()
        assertNotNull(cached)
        assertEquals(payload("v1"), cached!!.content)
        assertEquals(3, cached.rulesetVersion)
        assertEquals(1_000L, cached.syncedAtMs)
    }

    @Test
    fun `save sem rulesetVersion - persiste null, nunca inventa valor`() {
        store.save(content = payload("v1"), rulesetVersion = null, syncedAtMs = 1_000L)

        val cached = store.load()
        assertNull(cached!!.rulesetVersion)
    }

    @Test
    fun `arquivo atual corrompido - load ignora e retorna null quando nao ha previous`() {
        cacheDir.mkdirs()
        File(cacheDir, "diagnostic_ruleset_cache.json").writeText("{ isso nao eh json valido", StandardCharsets.UTF_8)

        assertNull(store.load())
    }

    @Test
    fun `hash divergente - trata como corrompido mesmo com JSON bem formado`() {
        cacheDir.mkdirs()
        val forjado = JSONObject()
            .put("content", payload("v1"))
            .put("rulesetVersion", 1)
            .put("hash", "hash-forjado-nao-bate")
            .put("syncedAtMs", 1_000L)
            .toString()
        File(cacheDir, "diagnostic_ruleset_cache.json").writeText(forjado, StandardCharsets.UTF_8)

        assertNull(store.load())
    }

    @Test
    fun `segundo save valido - promove o anterior para previous e load usa o mais novo`() {
        store.save(content = payload("v1"), rulesetVersion = 1, syncedAtMs = 1_000L)
        store.save(content = payload("v2"), rulesetVersion = 2, syncedAtMs = 2_000L)

        val cached = store.load()
        assertEquals(payload("v2"), cached!!.content)
        assertTrue(File(cacheDir, "diagnostic_ruleset_cache.previous.json").exists())
    }

    @Test
    fun `atual corrompido apos um save valido anterior - rollback automatico usa o previous`() {
        store.save(content = payload("v1"), rulesetVersion = 1, syncedAtMs = 1_000L)
        store.save(content = payload("v2"), rulesetVersion = 2, syncedAtMs = 2_000L)
        // Corrompe manualmente o arquivo atual (simula corrupcao em disco).
        File(cacheDir, "diagnostic_ruleset_cache.json").writeText("{ corrompido", StandardCharsets.UTF_8)

        val cached = store.load()
        assertNotNull(cached)
        assertEquals(payload("v1"), cached!!.content)
    }

    @Test
    fun `save com conteudo vazio ainda produz wrapper valido e recuperavel`() {
        // Nao ha validacao de shape do 'content' no store (isso e responsabilidade do
        // RemoteDiagnosticReportMapper, chamado depois) -- o store so garante que o
        // que foi escrito e o que e lido de volta batem via hash.
        val ok = store.save(content = "", rulesetVersion = null, syncedAtMs = 1_000L)
        assertTrue(ok)
        assertEquals("", store.load()!!.content)
    }

    @Test
    fun `nao ha cache existente - novo save nunca deixa lixo em tmp`() {
        store.save(content = payload("v1"), rulesetVersion = 1, syncedAtMs = 1_000L)
        assertFalse(File(cacheDir, "diagnostic_ruleset_cache.tmp.json").exists())
    }

    private fun payload(tag: String): String =
        JSONObject().put("decisao", JSONObject().put("id", "DECISAO-$tag")).toString()
}
