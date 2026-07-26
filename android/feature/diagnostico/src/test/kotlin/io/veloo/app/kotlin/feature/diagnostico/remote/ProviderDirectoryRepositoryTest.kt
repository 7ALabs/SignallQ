package io.signallq.app.feature.diagnostico.remote

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ProviderDirectoryRepositoryTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun providerJson(id: String = "regional_teste"): String = """
        {
          "id": "$id",
          "displayName": "Regional Teste",
          "logo": { "url": "https://assets.signallq.com/providers/$id/logo-square-v1.webp", "version": 1, "updatedAt": "2026-07-14T00:00:00.000Z" },
          "support": {
            "sacPhone": "10000",
            "technicalSupportPhone": null,
            "whatsappUrl": "https://wa.me/5511999999999",
            "websiteUrl": "https://regional.example.com",
            "customerAreaUrl": null,
            "ombudsmanPhone": null
          }
        }
    """.trimIndent()

    @Test
    fun `findById mapeia logo e contato corretamente`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(providerJson()))
        val repo = ProviderDirectoryRepository(baseUrl = server.url("/").toString())

        val info = repo.findById("regional_teste")

        assertEquals("regional_teste", info?.providerId)
        assertEquals("https://assets.signallq.com/providers/regional_teste/logo-square-v1.webp", info?.logoUrl)
        assertEquals("10000", info?.sacPhone)
        assertEquals("https://wa.me/5511999999999", info?.whatsappUrl)
        assertNull(info?.technicalSupportPhone)
    }

    @Test
    fun `findById com 404 retorna null (nao quebra)`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"Provider not found."}"""))
        val repo = ProviderDirectoryRepository(baseUrl = server.url("/").toString())
        assertNull(repo.findById("nao_existe"))
    }

    @Test
    fun `searchByName pega o primeiro item da lista`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"items":[${providerJson("regional_a")}, ${providerJson("regional_b")}]}"""),
        )
        val repo = ProviderDirectoryRepository(baseUrl = server.url("/").toString())
        val info = repo.searchByName("Regional")
        assertEquals("regional_a", info?.providerId)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/providers/search?q=Regional", recorded.path)
    }

    @Test
    fun `searchByName sem resultados retorna null`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"items":[]}"""))
        val repo = ProviderDirectoryRepository(baseUrl = server.url("/").toString())
        assertNull(repo.searchByName("Nao Existe Nenhuma"))
    }

    @Test
    fun `searchByName com nome em branco nunca faz chamada de rede`() = runTest {
        val repo = ProviderDirectoryRepository(baseUrl = server.url("/").toString())
        assertNull(repo.searchByName("   "))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `worker fora do ar retorna null, nunca lanca excecao`() = runTest {
        // Porta 1 (privilegiada, sem listener) — conexao recusada imediatamente.
        val repo = ProviderDirectoryRepository(baseUrl = "http://127.0.0.1:1")
        assertNull(repo.findById("qualquer"))
    }

    // ── Cache local por consulta (GH#1462) ──────────────────────────────────

    private class FakeProviderDirectoryCacheStore : ProviderDirectoryCacheStore {
        val entries = mutableMapOf<String, CachedProviderEntry>()
        override fun load(key: String): CachedProviderEntry? = entries[key]
        override fun save(key: String, entry: CachedProviderEntry): Boolean {
            entries[key] = entry
            return true
        }
    }

    @Test
    fun `sucesso de rede grava no cache local com versao e expiracao vindas do worker`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "id": "regional_teste",
                  "displayName": "Regional Teste",
                  "logo": { "url": "https://assets.signallq.com/providers/regional_teste/logo-square-v1.webp" },
                  "support": { "sacPhone": "10000", "technicalSupportPhone": null, "whatsappUrl": null, "websiteUrl": null, "customerAreaUrl": null, "ombudsmanPhone": null },
                  "cacheVersion": 7,
                  "cacheExpiresAt": "2026-08-01T00:00:00.000Z"
                }
                """.trimIndent(),
            ),
        )
        val cacheStore = FakeProviderDirectoryCacheStore()
        var now = 10_000L
        val repo = ProviderDirectoryRepository(
            baseUrl = server.url("/").toString(),
            cacheStore = cacheStore,
            cacheTtlMs = 48 * 60 * 60 * 1000L,
            nowProvider = { now },
        )

        val info = repo.findById("regional_teste")

        assertEquals("regional_teste", info?.providerId)
        assertEquals(7, info?.cacheVersion)
        val cached = cacheStore.entries["id:regional_teste"]
        assertNotNull(cached)
        assertEquals(now, cached!!.syncedAtMs)
        assertEquals(now + 48 * 60 * 60 * 1000L, cached.expiresAtMs)
        assertEquals("regional_teste", cached.info.providerId)
    }

    @Test
    fun `falha remota com cache local valido dentro do TTL usa o ultimo dado valido conhecido`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val cacheStore = FakeProviderDirectoryCacheStore()
        val syncedAtMs = 10_000L
        val ttlMs = 48 * 60 * 60 * 1000L
        cacheStore.entries["id:regional_teste"] = CachedProviderEntry(
            info = RemoteProviderInfo(
                providerId = "regional_teste",
                displayName = "Regional Teste (cache)",
                logoUrl = "https://assets.signallq.com/providers/regional_teste/logo-square-v1.webp",
                sacPhone = "10000",
                technicalSupportPhone = null,
                whatsappUrl = null,
                websiteUrl = null,
                customerAreaUrl = null,
                ombudsmanPhone = null,
                cacheVersion = 5,
                cacheExpiresAt = null,
            ),
            syncedAtMs = syncedAtMs,
            expiresAtMs = syncedAtMs + ttlMs,
        )
        // "Agora" um pouco depois da sincronizacao, mas ainda dentro do TTL de 48h.
        val repo = ProviderDirectoryRepository(
            baseUrl = server.url("/").toString(),
            cacheStore = cacheStore,
            cacheTtlMs = ttlMs,
            nowProvider = { syncedAtMs + 60_000L },
        )

        val info = repo.findById("regional_teste")

        assertEquals("Regional Teste (cache)", info?.displayName)
    }

    @Test
    fun `falha remota sem cache local retorna null (cai pro fallback do caller)`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val cacheStore = FakeProviderDirectoryCacheStore()
        val repo = ProviderDirectoryRepository(
            baseUrl = server.url("/").toString(),
            cacheStore = cacheStore,
        )

        assertNull(repo.findById("regional_teste"))
    }

    @Test
    fun `falha remota com cache local alem do TTL e tratada como cache inexistente`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        val cacheStore = FakeProviderDirectoryCacheStore()
        val syncedAtMs = 10_000L
        val ttlMs = 48 * 60 * 60 * 1000L
        cacheStore.entries["id:regional_teste"] = CachedProviderEntry(
            info = RemoteProviderInfo(
                providerId = "regional_teste",
                displayName = "Regional Teste (cache expirado)",
                logoUrl = null,
                sacPhone = null,
                technicalSupportPhone = null,
                whatsappUrl = null,
                websiteUrl = null,
                customerAreaUrl = null,
                ombudsmanPhone = null,
            ),
            syncedAtMs = syncedAtMs,
            expiresAtMs = syncedAtMs + ttlMs,
        )
        // "Agora" bem depois do TTL de 48h.
        val repo = ProviderDirectoryRepository(
            baseUrl = server.url("/").toString(),
            cacheStore = cacheStore,
            cacheTtlMs = ttlMs,
            nowProvider = { syncedAtMs + ttlMs + 60_000L },
        )

        assertNull(repo.findById("regional_teste"))
    }

    @Test
    fun `sucesso de rede sobrescreve cache antigo da mesma chave`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(providerJson("regional_novo")))
        val cacheStore = FakeProviderDirectoryCacheStore()
        cacheStore.entries["id:regional_teste"] = CachedProviderEntry(
            info = RemoteProviderInfo(
                providerId = "regional_antigo",
                displayName = "Regional Antigo",
                logoUrl = null,
                sacPhone = null,
                technicalSupportPhone = null,
                whatsappUrl = null,
                websiteUrl = null,
                customerAreaUrl = null,
                ombudsmanPhone = null,
            ),
            syncedAtMs = 1_000L,
            expiresAtMs = 999_999_999L,
        )
        val repo = ProviderDirectoryRepository(baseUrl = server.url("/").toString(), cacheStore = cacheStore)

        val info = repo.findById("regional_teste")

        assertEquals("regional_novo", info?.providerId)
        assertEquals("regional_novo", cacheStore.entries["id:regional_teste"]?.info?.providerId)
    }

    @Test
    fun `searchByName tambem usa cache local em falha remota`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"items":[${providerJson("regional_a")}]}"""))
        val cacheStore = FakeProviderDirectoryCacheStore()
        val repo = ProviderDirectoryRepository(baseUrl = server.url("/").toString(), cacheStore = cacheStore)
        repo.searchByName("Regional")
        assertNotNull(cacheStore.entries["search:regional"])

        server.enqueue(MockResponse().setResponseCode(500))
        val info = repo.searchByName("Regional")
        assertEquals("regional_a", info?.providerId)
    }
}
