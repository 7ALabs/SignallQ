package io.signallq.app.feature.diagnostico.remote

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
 * Cobre a persistencia atomica do cache local do diretorio remoto de provedores por
 * chave de consulta (GH#1462): escrita atomica, corrupcao nunca derruba o app, e
 * multiplas chaves nao se atropelam no mesmo arquivo.
 */
class ProviderDirectoryCacheStoreTest {

    private lateinit var cacheDir: File
    private lateinit var store: FileProviderDirectoryCacheStore

    @Before
    fun setUp() {
        cacheDir = Files.createTempDirectory("provider-directory-cache-test").toFile()
        store = FileProviderDirectoryCacheStore(cacheDir)
    }

    @After
    fun tearDown() {
        cacheDir.deleteRecursively()
    }

    private fun info(id: String = "regional_teste") = RemoteProviderInfo(
        providerId = id,
        displayName = "Regional Teste",
        logoUrl = "https://assets.signallq.com/providers/$id/logo-square-v1.webp",
        sacPhone = "10000",
        technicalSupportPhone = null,
        whatsappUrl = "https://wa.me/5511999999999",
        websiteUrl = "https://regional.example.com",
        customerAreaUrl = null,
        ombudsmanPhone = null,
        cacheVersion = 3,
        cacheExpiresAt = "2026-08-01T00:00:00.000Z",
    )

    @Test
    fun `sem cache persistido - load retorna null`() {
        assertNull(store.load("id:regional_teste"))
    }

    @Test
    fun `save com entrada valida - load devolve o mesmo conteudo e metadados`() {
        val entry = CachedProviderEntry(info = info(), syncedAtMs = 1_000L, expiresAtMs = 2_000L)
        val ok = store.save("id:regional_teste", entry)

        assertTrue(ok)
        val cached = store.load("id:regional_teste")
        assertNotNull(cached)
        assertEquals(info(), cached!!.info)
        assertEquals(1_000L, cached.syncedAtMs)
        assertEquals(2_000L, cached.expiresAtMs)
    }

    @Test
    fun `save sem cacheVersion do worker - persiste null, nunca inventa valor`() {
        val semVersao = info().copy(cacheVersion = null, cacheExpiresAt = null)
        store.save("id:regional_teste", CachedProviderEntry(semVersao, 1_000L, 2_000L))

        val cached = store.load("id:regional_teste")
        assertNull(cached!!.info.cacheVersion)
        assertNull(cached.info.cacheExpiresAt)
    }

    @Test
    fun `multiplas chaves coexistem no mesmo arquivo`() {
        store.save("id:regional_a", CachedProviderEntry(info("regional_a"), 1_000L, 2_000L))
        store.save("search:regional teste", CachedProviderEntry(info("regional_a"), 1_500L, 2_500L))
        store.save("id:regional_b", CachedProviderEntry(info("regional_b"), 3_000L, 4_000L))

        assertEquals("regional_a", store.load("id:regional_a")!!.info.providerId)
        assertEquals("regional_a", store.load("search:regional teste")!!.info.providerId)
        assertEquals("regional_b", store.load("id:regional_b")!!.info.providerId)
    }

    @Test
    fun `segunda save na mesma chave sobrescreve so aquela entrada, preserva as outras`() {
        store.save("id:regional_a", CachedProviderEntry(info("regional_a"), 1_000L, 2_000L))
        store.save("id:regional_b", CachedProviderEntry(info("regional_b"), 1_000L, 2_000L))

        val atualizado = info("regional_a").copy(displayName = "Regional A Atualizado")
        store.save("id:regional_a", CachedProviderEntry(atualizado, 5_000L, 6_000L))

        assertEquals("Regional A Atualizado", store.load("id:regional_a")!!.info.displayName)
        assertEquals(5_000L, store.load("id:regional_a")!!.syncedAtMs)
        assertEquals("regional_b", store.load("id:regional_b")!!.info.providerId)
    }

    @Test
    fun `arquivo atual corrompido - load retorna null, nunca lanca excecao`() {
        cacheDir.mkdirs()
        File(cacheDir, "provider_directory_cache.json").writeText("{ isso nao eh json valido", StandardCharsets.UTF_8)

        assertNull(store.load("id:regional_teste"))
    }

    @Test
    fun `chave inexistente em arquivo valido retorna null`() {
        store.save("id:regional_a", CachedProviderEntry(info("regional_a"), 1_000L, 2_000L))
        assertNull(store.load("id:nao_existe"))
    }

    @Test
    fun `save nao deixa lixo em tmp`() {
        store.save("id:regional_a", CachedProviderEntry(info("regional_a"), 1_000L, 2_000L))
        assertFalse(File(cacheDir, "provider_directory_cache.tmp.json").exists())
    }
}
