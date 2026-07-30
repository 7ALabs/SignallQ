package io.signallq.app.core.network.connectivity

import io.signallq.app.core.network.contracts.connectivity.ProbeFailureReason
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.net.UnknownHostException

/**
 * Testa resolução DNS pela rede sob análise (via [binding], nunca pelo resolver padrão
 * do sistema). Mais de um hostname tentado — não decide "sem internet" por uma única
 * falha de resolução.
 *
 * `Network.getAllByName` (por trás de [binding]) não tem parâmetro de timeout — é a
 * única sondagem sem nenhum limite nativo próprio. `runInterruptible` é o que garante um
 * teto real de tempo aqui: sem ele, o timeout do motor (`withTimeoutOrNull`) não teria
 * efeito nenhum sobre uma chamada bloqueante sem pontos de suspensão (GH#1512, achado de
 * revisão) -- a resolução seguiria rodando (numa thread do dispatcher) até o resolver do
 * Android desistir por conta própria, prazo que não é controlado por este código.
 */
class DnsReachabilityProbe(
    private val binding: ConnectivityProbeBinding,
) : DnsProbe {

    override suspend fun probe(hostnames: List<String>): ProbeResult = runInterruptible(Dispatchers.IO) {
        for (hostname in hostnames) {
            try {
                val inicio = System.currentTimeMillis()
                val enderecos = binding.resolveHost(hostname)
                if (enderecos.isNotEmpty()) {
                    return@runInterruptible ProbeResult.Success(System.currentTimeMillis() - inicio)
                }
            } catch (_: UnknownHostException) {
                // tenta o proximo hostname, a menos que a interrupcao (cancelamento/timeout
                // do motor) tenha disparado esta excecao
                if (Thread.currentThread().isInterrupted) return@runInterruptible ProbeResult.Timeout(0)
            } catch (_: SecurityException) {
                return@runInterruptible ProbeResult.Unavailable("sem permissao para resolver DNS na rede sob analise")
            }
        }
        ProbeResult.Failure(ProbeFailureReason.DNS_RESOLUTION_FAILED)
    }
}
