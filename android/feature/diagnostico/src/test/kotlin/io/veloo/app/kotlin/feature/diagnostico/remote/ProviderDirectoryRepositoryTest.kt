package io.signallq.app.feature.diagnostico.remote

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
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
}
