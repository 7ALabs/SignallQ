package io.signallq.app.feature.diagnostico.remote

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** GH#1445 (parte de #952) — busca (com cache curto) do status de rollout publico do worker. */
class DiagnosticRolloutStatusRepositoryTest {
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

    private fun repository(cacheTtlMs: Long = 15 * 60 * 1000L): DiagnosticRolloutStatusRepository =
        DiagnosticRolloutStatusRepository(
            baseUrl = server.url("/").toString(),
            client = OkHttpClient(),
            cacheTtlMs = cacheTtlMs,
        )

    @Test
    fun `current mapeia o payload completo do worker`() =
        runTest {
            server.enqueue(
                MockResponse().setResponseCode(200).setBody(
                    """{"rulesetVersion":7,"rolloutPercent":25,"rolloutMinVersionCode":320,"rolloutChannels":["play_store","sideload"]}""",
                ),
            )

            val status = repository().current()

            assertEquals(7, status?.rulesetVersion)
            assertEquals(25, status?.rolloutPercent)
            assertEquals(320, status?.rolloutMinVersionCode)
            assertEquals(listOf("play_store", "sideload"), status?.rolloutChannels)
        }

    @Test
    fun `current mapeia rulesetVersion null quando nao ha ruleset publicado`() =
        runTest {
            server.enqueue(
                MockResponse().setResponseCode(200).setBody(
                    """{"rulesetVersion":null,"rolloutPercent":0,"rolloutMinVersionCode":null,"rolloutChannels":null}""",
                ),
            )

            val status = repository().current()

            assertNull(status?.rulesetVersion)
            assertEquals(0, status?.rolloutPercent)
        }

    @Test
    fun `current devolve null quando o worker responde erro e nao ha cache anterior`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(500))

            val status = repository().current()

            assertNull(status)
        }

    @Test
    fun `current usa cache dentro do TTL, sem nova chamada de rede`() =
        runTest {
            server.enqueue(
                MockResponse().setResponseCode(200).setBody(
                    """{"rulesetVersion":1,"rolloutPercent":50,"rolloutMinVersionCode":null,"rolloutChannels":null}""",
                ),
            )
            val repo = repository(cacheTtlMs = 60_000L)

            val primeira = repo.current()
            val segunda = repo.current()

            assertEquals(50, primeira?.rolloutPercent)
            assertEquals(50, segunda?.rolloutPercent)
            assertEquals(1, server.requestCount)
        }

    @Test
    fun `current busca de novo apos o TTL expirar`() =
        runTest {
            server.enqueue(
                MockResponse().setResponseCode(200).setBody(
                    """{"rulesetVersion":1,"rolloutPercent":50,"rolloutMinVersionCode":null,"rolloutChannels":null}""",
                ),
            )
            server.enqueue(
                MockResponse().setResponseCode(200).setBody(
                    """{"rulesetVersion":2,"rolloutPercent":75,"rolloutMinVersionCode":null,"rolloutChannels":null}""",
                ),
            )
            val repo = repository(cacheTtlMs = 0L)

            val primeira = repo.current()
            val segunda = repo.current()

            assertEquals(50, primeira?.rolloutPercent)
            assertEquals(75, segunda?.rolloutPercent)
            assertEquals(2, server.requestCount)
        }

    @Test
    fun `current mantem o ultimo valor cacheado quando uma busca posterior falha`() =
        runTest {
            server.enqueue(
                MockResponse().setResponseCode(200).setBody(
                    """{"rulesetVersion":1,"rolloutPercent":40,"rolloutMinVersionCode":null,"rolloutChannels":null}""",
                ),
            )
            server.enqueue(MockResponse().setResponseCode(500))
            val repo = repository(cacheTtlMs = 0L)

            val primeira = repo.current()
            val segunda = repo.current()

            assertEquals(40, primeira?.rolloutPercent)
            assertEquals(40, segunda?.rolloutPercent)
        }
}
