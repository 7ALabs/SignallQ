package io.signallq.app.feature.diagnostico.remote

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Fake em memoria de [ProviderDirectoryCache] — evita depender de Room (`:coreDatabase`,
 *  instrumentado) so pra validar a orquestracao de cache do repository (GH#1462). */
private class InMemoryProviderDirectoryCache : ProviderDirectoryCache {
    private val armazenado = mutableMapOf<String, CachedProviderEntry>()

    override suspend fun get(key: String): CachedProviderEntry? = armazenado[key]

    override suspend fun put(key: String, info: RemoteProviderInfo) {
        armazenado[key] = CachedProviderEntry(info)
    }

    fun tamanho(): Int = armazenado.size
}

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

    private fun providerJson(
        id: String = "regional_teste",
        status: String? = "VERIFIED",
        cacheVersion: Int? = 1,
        cacheExpiresAt: String? = "2026-07-21T00:00:00.000Z",
    ): String = """
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
          }${if (status != null) ""","status": "$status"""" else ""}${
        if (cacheVersion != null) ""","cacheVersion": $cacheVersion""" else ""
    }${if (cacheExpiresAt != null) ""","cacheExpiresAt": "$cacheExpiresAt"""" else ""}
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
        // GH#1462 — cacheVersion/cacheExpiresAt do worker mapeados corretamente.
        assertEquals(1, info?.cacheVersion)
        assertEquals(1784592000000L, info?.cacheExpiresAtMs) // 2026-07-21T00:00:00.000Z
    }

    // ── GH#1464 (parte de #951): campo `status` de curadoria ────────────────────────────────

    @Test
    fun `findById mapeia o status de curadoria quando presente`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(providerJson(status = "PENDING_REVIEW")))
        val repo = ProviderDirectoryRepository(baseUrl = server.url("/").toString())

        val info = repo.findById("regional_teste")

        assertEquals("PENDING_REVIEW", info?.status)
    }

    @Test
    fun `findById com status ausente no JSON mapeia status nulo`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(providerJson(status = null)))
        val repo = ProviderDirectoryRepository(baseUrl = server.url("/").toString())

        val info = repo.findById("regional_teste")

        assertNull(info?.status)
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

    // ── GH#1462 (parte de #951): cache local com versao/expiracao ──────────────────────────

    @Test
    fun `sucesso de rede grava o resultado no cache`() = runTest {
        val cache = InMemoryProviderDirectoryCache()
        server.enqueue(MockResponse().setResponseCode(200).setBody(providerJson()))
        val repo = ProviderDirectoryRepository(baseUrl = server.url("/").toString(), cache = cache)

        repo.findById("regional_teste")

        val cacheado = cache.get("id:regional_teste")
        assertEquals("regional_teste", cacheado?.info?.providerId)
        assertEquals(1, cacheado?.info?.cacheVersion)
    }

    @Test
    fun `falha de rede consulta o cache antes de cair no fallback generico (cache hit)`() = runTest {
        val cache = InMemoryProviderDirectoryCache()
        cache.put("id:regional_teste", parsedInfo(id = "regional_teste"))
        // Porta 1 (privilegiada, sem listener) — conexao recusada imediatamente.
        val repo = ProviderDirectoryRepository(baseUrl = "http://127.0.0.1:1", cache = cache)

        val info = repo.findById("regional_teste")

        assertEquals("regional_teste", info?.providerId)
    }

    @Test
    fun `falha de rede sem entrada de cache (cache miss) devolve null`() = runTest {
        val cache = InMemoryProviderDirectoryCache()
        val repo = ProviderDirectoryRepository(baseUrl = "http://127.0.0.1:1", cache = cache)

        assertNull(repo.findById("nunca_consultado"))
    }

    @Test
    fun `cache expirado ainda e usado como ultimo dado valido quando a rede falha`() = runTest {
        val cache = InMemoryProviderDirectoryCache()
        cache.put(
            "id:regional_expirado",
            parsedInfo(id = "regional_expirado", cacheExpiresAtMs = 1L), // ja expirado (epoch=1ms)
        )
        val repo = ProviderDirectoryRepository(baseUrl = "http://127.0.0.1:1", cache = cache)

        val info = repo.findById("regional_expirado")

        assertEquals("regional_expirado", info?.providerId)
    }

    @Test
    fun `sucesso de rede sobrescreve cache antigo com o dado novo`() = runTest {
        val cache = InMemoryProviderDirectoryCache()
        cache.put("id:regional_teste", parsedInfo(id = "regional_teste", cacheVersion = 1))

        server.enqueue(MockResponse().setResponseCode(200).setBody(providerJson(cacheVersion = 2)))
        val repo = ProviderDirectoryRepository(baseUrl = server.url("/").toString(), cache = cache)
        repo.findById("regional_teste")

        assertEquals(2, cache.get("id:regional_teste")?.info?.cacheVersion)
        assertEquals(1, cache.tamanho())
    }

    @Test
    fun `searchByName com sucesso grava no cache usando o nome buscado como chave`() = runTest {
        val cache = InMemoryProviderDirectoryCache()
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"items":[${providerJson("regional_a")}]}"""))
        val repo = ProviderDirectoryRepository(baseUrl = server.url("/").toString(), cache = cache)

        repo.searchByName("Regional")

        assertEquals("regional_a", cache.get("search:regional")?.info?.providerId)
    }

    @Test
    fun `searchByName com falha de rede consulta o cache pela mesma chave normalizada`() = runTest {
        val cache = InMemoryProviderDirectoryCache()
        cache.put("search:regional teste isp", parsedInfo(id = "regional_teste"))
        val repo = ProviderDirectoryRepository(baseUrl = "http://127.0.0.1:1", cache = cache)

        val info = repo.searchByName("  Regional Teste ISP  ")

        assertEquals("regional_teste", info?.providerId)
    }

    @Test
    fun `searchByName com resultado vazio nao consulta o cache (resposta autoritativa do worker)`() = runTest {
        val cache = InMemoryProviderDirectoryCache()
        cache.put("search:nao existe nenhuma", parsedInfo(id = "regional_antigo"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"items":[]}"""))
        val repo = ProviderDirectoryRepository(baseUrl = server.url("/").toString(), cache = cache)

        assertNull(repo.searchByName("Nao Existe Nenhuma"))
    }

    private fun parsedInfo(
        id: String,
        cacheVersion: Int? = 1,
        cacheExpiresAtMs: Long? = null,
    ) = RemoteProviderInfo(
        providerId = id,
        displayName = "Regional Teste",
        logoUrl = "https://assets.signallq.com/providers/$id/logo-square-v1.webp",
        sacPhone = "10000",
        technicalSupportPhone = null,
        whatsappUrl = "https://wa.me/5511999999999",
        websiteUrl = "https://regional.example.com",
        customerAreaUrl = null,
        ombudsmanPhone = null,
        status = "VERIFIED",
        cacheVersion = cacheVersion,
        cacheExpiresAtMs = cacheExpiresAtMs,
    )
}
