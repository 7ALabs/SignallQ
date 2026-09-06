package io.signallq.app.core.network.connectivity

import io.signallq.app.core.network.contracts.connectivity.ProbeFailureReason
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class DohFallbackProbeTest {
    private lateinit var server: MockWebServer
    private lateinit var httpClient: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        httpClient =
            OkHttpClient
                .Builder()
                .connectTimeout(500, TimeUnit.MILLISECONDS)
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .callTimeout(500, TimeUnit.MILLISECONDS)
                .build()
    }

    private fun probeComTimeoutMs(
        timeoutMs: Long,
        httpClientBase: OkHttpClient = httpClient,
    ) = DohFallbackProbe(
        httpClientBase = httpClientBase,
        endpoint = server.url("/dns-query").toString(),
        timeoutMs = timeoutMs,
    )

    @After
    fun tearDown() {
        server.shutdown()
    }

    /** Resposta DNS binária mínima válida (RCODE=0, ANCOUNT=1) -- corpo além do header é irrelevante ao probe. */
    private fun respostaDnsBinaria(
        rcode: Int,
        ancount: Int,
    ): ByteArray {
        val header = ByteArray(12)
        header[3] = (rcode and 0x0F).toByte()
        header[6] = ((ancount shr 8) and 0xFF).toByte()
        header[7] = (ancount and 0xFF).toByte()
        return header
    }

    @Test
    fun `sucesso quando o provedor DoH responde NOERROR com pelo menos um registro`() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(okio.Buffer().write(respostaDnsBinaria(rcode = 0, ancount = 1))),
            )
            val probe = probeComTimeoutMs(timeoutMs = 500)

            val resultado = probe.probe(listOf("exemplo.com"))

            assertTrue("esperava Success, obteve $resultado", resultado is ProbeResult.Success)
        }

    @Test
    fun `timeout quando o provedor DoH nao responde dentro do prazo`() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setSocketPolicy(SocketPolicy.NO_RESPONSE),
            )
            val probe = probeComTimeoutMs(timeoutMs = 500)

            val resultado = probe.probe(listOf("exemplo.com"))

            assertTrue("esperava Timeout, obteve $resultado", resultado is ProbeResult.Timeout)
        }

    @Test
    fun `falha de resolucao quando o provedor DoH responde NXDOMAIN`() =
        runBlocking {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setBody(okio.Buffer().write(respostaDnsBinaria(rcode = 3, ancount = 0))),
            )
            val probe = probeComTimeoutMs(timeoutMs = 500)

            val resultado = probe.probe(listOf("host-inexistente.invalido"))

            assertTrue("esperava Failure, obteve $resultado", resultado is ProbeResult.Failure)
            assertEquals(ProbeFailureReason.DNS_RESOLUTION_FAILED, (resultado as ProbeResult.Failure).reason)
        }

    @Test
    fun `sem hostname informado retorna Unavailable sem chamar a rede`() =
        runBlocking {
            val probe = probeComTimeoutMs(timeoutMs = 500)

            val resultado = probe.probe(emptyList())

            assertTrue("esperava Unavailable, obteve $resultado", resultado is ProbeResult.Unavailable)
            assertEquals(0, server.requestCount)
        }

    /**
     * Regressão da ressalva de revisão do Caio na PR #1812: `timeoutMs` precisa ser o
     * timeout de fato aplicado ao cliente HTTP, não só o número reportado em
     * [ProbeResult.Timeout]. Usa deliberadamente um `httpClientBase` SEM timeout curto
     * configurado (padrão OkHttp, dezenas de segundos) para provar que é `timeoutMs` --
     * não o cliente base -- quem estoura a chamada.
     */
    @Test
    fun `timeoutMs configura o cliente de fato, mesmo com cliente base sem timeout curto`() =
        runBlocking {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val clienteBaseSemTimeoutCurto = OkHttpClient.Builder().build()
            val probe = probeComTimeoutMs(timeoutMs = 300, httpClientBase = clienteBaseSemTimeoutCurto)

            val decorridoMs = System.currentTimeMillis()
            val resultado = probe.probe(listOf("exemplo.com"))
            val duracaoMs = System.currentTimeMillis() - decorridoMs

            assertTrue("esperava Timeout, obteve $resultado", resultado is ProbeResult.Timeout)
            assertEquals(300L, (resultado as ProbeResult.Timeout).afterMs)
            assertTrue(
                "esperava estourar perto de 300ms (timeoutMs), levou ${duracaoMs}ms -- indica que o cliente base, nao timeoutMs, esta valendo",
                duracaoMs < 2000,
            )
        }
}
