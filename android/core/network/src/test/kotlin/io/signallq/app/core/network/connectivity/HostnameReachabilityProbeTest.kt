package io.signallq.app.core.network.connectivity

import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import java.net.URLConnection

class HostnameReachabilityProbeTest {

    /**
     * `connect()` que nunca retorna sozinho -- simula o cenário real da 3ª revisão
     * adversarial (GH#1512): a resolução do hostname embutida em
     * `HttpURLConnection.connect()` não tem teto nativo (a mesma chamada via netd que o
     * `DnsReachabilityProbe` precisou corrigir), e `connectTimeout`/`readTimeout` só
     * cobrem a fase que vem DEPOIS da resolução -- então um `connect()` que trava não era
     * limitado por eles.
     */
    private class BindingComConnectTravado : ConnectivityProbeBinding {
        override fun bindSocket(socket: Socket) = Unit

        override fun resolveHost(hostname: String): Array<java.net.InetAddress> = error("nao usado neste teste")

        override fun openConnection(url: URL): URLConnection = object : HttpURLConnection(url) {
            override fun connect() {
                Thread.sleep(60_000)
            }

            override fun disconnect() = Unit

            override fun usingProxy(): Boolean = false

            override fun getResponseCode(): Int = error("nao alcancado -- connect() nunca retorna neste teste")
        }
    }

    // GH#1512 (3a revisao) -- timeout de JUnit: se uma regressao futura reintroduzir o
    // bloqueio sem teto, o teste falha em segundos em vez de pendurar a suite por 60s.
    @Test(timeout = 6_000L)
    fun `sondagem de hostname cujo connect trava retorna dentro de um prazo real`() = runBlocking {
        val probe = HostnameReachabilityProbe(
            binding = BindingComConnectTravado(),
            timeoutMs = 200,
            urls = listOf("https://exemplo.invalido/generate_204"),
        )

        val inicio = System.currentTimeMillis()
        val resultado = probe.probe()
        val duracaoMs = System.currentTimeMillis() - inicio

        assertTrue("esperava Timeout, obteve ${resultado.result}", resultado.result is ProbeResult.Timeout)
        assertTrue(
            "sondagem deveria retornar perto do prazo por tentativa (3x timeoutMs), levou ${duracaoMs}ms",
            duracaoMs < 3_000,
        )
    }

    // GH#1512 (4a revisao, achado de revisao) -- o retorno imediato no catch
    // (InterruptedException) nao tinha nenhum teste dedicado provando o cancelamento.
    @Test(timeout = 6_000L)
    fun `cancelamento durante o connect nao acumula custo da proxima url`() = runBlocking {
        val probe = HostnameReachabilityProbe(
            binding = BindingComConnectTravado(),
            timeoutMs = 2_000,
            urls = listOf("https://exemplo1.invalido/generate_204", "https://exemplo2.invalido/generate_204"),
        )

        val inicio = System.currentTimeMillis()
        val job = launch { probe.probe() }
        delay(200)
        job.cancelAndJoin()
        val duracaoMs = System.currentTimeMillis() - inicio

        assertTrue(
            "cancelamento nao deveria custar o prazo da proxima URL, levou ${duracaoMs}ms",
            duracaoMs < 3_000,
        )
    }
}
