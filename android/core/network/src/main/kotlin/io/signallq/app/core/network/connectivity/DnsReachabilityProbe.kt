package io.signallq.app.core.network.connectivity

import io.signallq.app.core.network.contracts.connectivity.ProbeFailureReason
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.UnknownHostException

/**
 * Testa resolução DNS pela rede sob análise (via [binding], nunca pelo resolver padrão
 * do sistema). Mais de um hostname tentado — não decide "sem internet" por uma única
 * falha de resolução.
 */
class DnsReachabilityProbe(
    private val binding: ConnectivityProbeBinding,
) : DnsProbe {

    override suspend fun probe(hostnames: List<String>): ProbeResult = withContext(Dispatchers.IO) {
        for (hostname in hostnames) {
            try {
                val inicio = System.currentTimeMillis()
                val enderecos = binding.resolveHost(hostname)
                if (enderecos.isNotEmpty()) {
                    return@withContext ProbeResult.Success(System.currentTimeMillis() - inicio)
                }
            } catch (_: UnknownHostException) {
                // tenta o proximo hostname -- inclui timeout de resolucao (UnknownHostException
                // tambem e lancada quando o resolver estoura o prazo interno)
            } catch (_: SecurityException) {
                return@withContext ProbeResult.Unavailable("sem permissao para resolver DNS na rede sob analise")
            }
        }
        ProbeResult.Failure(ProbeFailureReason.DNS_RESOLUTION_FAILED)
    }
}
