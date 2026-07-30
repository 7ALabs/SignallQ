package io.signallq.app.core.network.connectivity

import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.Socket
import java.net.URL
import java.net.URLConnection

class DnsReachabilityProbeTest {

    /** Resolvedor que nunca retorna sozinho -- simula `Network.getAllByName` travado (o
     *  cenário real da #1512: WAN caída com DNS pendurado). `Thread.sleep` em si responde
     *  a `Thread.interrupt()` (lança `InterruptedException`), mas nada nesta sondagem
     *  cancela a `Future` que executa esta chamada -- o teste força o caminho de timeout
     *  (não o de cancelamento), que é o que reproduz `Network.getAllByName` de verdade
     *  (chamada nativa via netd que essa classe representa, e que não responde a
     *  interrupção nenhuma). */
    private object BindingQueNuncaResolve : ConnectivityProbeBinding {
        override fun bindSocket(socket: Socket) = Unit

        override fun resolveHost(hostname: String): Array<InetAddress> {
            Thread.sleep(60_000)
            return emptyArray()
        }

        override fun openConnection(url: URL): URLConnection = error("nao usado neste teste")
    }

    // GH#1512 (3a revisao, achado de revisao) -- timeout de JUnit: se uma regressao futura
    // reintroduzir o bloqueio sem teto, o teste falha em poucos segundos em vez de
    // pendurar a suite inteira pelos 60s do Thread.sleep acima.
    @Test(timeout = 4_000L)
    fun `sondagem de dns que trava alem do timeout retorna dentro de um prazo real`() = runBlocking {
        // GH#1512 (achado de revisao) -- antes desta correcao, `Network.getAllByName`
        // travado nao respeitava NENHUM teto de tempo (runInterruptible sozinho nao basta
        // para uma chamada nativa que ignora Thread.interrupt()). Este teste usa tempo de
        // parede REAL (nao tempo virtual de runTest) porque a correcao depende de uma
        // thread dedicada de verdade -- e o proprio ponto do teste e provar que o
        // chamador nao fica bloqueado pelo tempo todo do Thread.sleep(60_000) acima.
        val probe = DnsReachabilityProbe(binding = BindingQueNuncaResolve, timeoutMs = 200L)

        val inicio = System.currentTimeMillis()
        val resultado = probe.probe(listOf("exemplo.invalido"))
        val duracaoMs = System.currentTimeMillis() - inicio

        assertTrue("esperava Timeout, obteve $resultado", resultado is ProbeResult.Timeout)
        assertTrue(
            "sondagem deveria retornar perto do timeoutMs configurado, levou ${duracaoMs}ms",
            duracaoMs < 2_000,
        )
    }
}
