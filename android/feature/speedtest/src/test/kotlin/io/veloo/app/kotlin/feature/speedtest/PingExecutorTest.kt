package io.signallq.app.feature.speedtest

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * GH#1211 — cobre o comportamento novo do [PingExecutor]: destino/método legível, aborto
 * cedo por falhas consecutivas de rede/DNS (distinto de falha do destino) e execução
 * parcial. Não cobre timeout de amostra individual (SocketTimeoutException) nem
 * cancelamento cooperativo real via coroutine — ambos exigiriam esperar o timeout fixo de
 * 4s do client em cada tentativa, custo alto pra suíte de unit test; a correção de código
 * (rethrow de CancellationException antes de qualquer catch genérico) foi feita por
 * inspeção, não por teste de concorrência real.
 */
class PingExecutorTest {

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

    @Test
    fun `medicao bem sucedida devolve destino legivel e amostras validas`() = runTest {
        repeat(5) { server.enqueue(MockResponse().setResponseCode(200).setBody("ok")) }
        val url = server.url("/__down").toString()
        val executor = PingExecutor(targetUrl = url)

        val resultado = executor.executar(count = 5)

        // Host real do MockWebServer varia por ambiente (localhost, 127.0.0.1,
        // kubernetes.docker.internal em Docker Desktop) -- compara contra o que o próprio
        // server reporta, não hardcoda um valor de host.
        assertEquals(server.hostName, resultado.destino)
        assertFalse(resultado.abortadoPorRede)
        assertFalse(resultado.execucaoParcial)
        assertTrue(resultado.amostrasValidas > 0)
    }

    @Test
    fun `host DNS invalido aborta cedo por falhas consecutivas de rede`() = runTest {
        // .invalid e reservado por RFC 2606 -- nunca resolve, independente de conectividade.
        val executor = PingExecutor(targetUrl = "https://nao-existe.invalid/__down")

        val resultado = executor.executar(count = 20)

        assertTrue(resultado.abortadoPorRede)
        assertTrue(resultado.execucaoParcial)
        // Aborta em ate 3 tentativas (FALHAS_REDE_CONSECUTIVAS_PARA_ABORTAR), nunca chega
        // a esgotar as 20 solicitadas.
        assertTrue(resultado.amostras <= 3)
    }

    @Test
    fun `respostas HTTP com erro nao disparam abortadoPorRede`() = runTest {
        repeat(5) { server.enqueue(MockResponse().setResponseCode(500)) }
        val url = server.url("/__down").toString()
        val executor = PingExecutor(targetUrl = url)

        val resultado = executor.executar(count = 5)

        // Falha do destino (HTTP 500) e diferente de rede caida -- nao aborta cedo,
        // executa as 5 tentativas e conta como perda, nao como problema de conectividade.
        assertFalse(resultado.abortadoPorRede)
        assertFalse(resultado.execucaoParcial)
        assertEquals(5, resultado.amostras)
        assertEquals(100.0, resultado.perdaPercentual, 0.0001)
    }
}
