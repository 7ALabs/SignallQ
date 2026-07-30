package io.signallq.app.core.network.connectivity

import io.signallq.app.core.network.contracts.connectivity.ProbeFailureReason
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * Testa alcance externo por IP puro — literal numérico, `InetAddress.getByName` não
 * dispara resolução DNS para entrada já numérica, então esta sondagem é independente da
 * sondagem de DNS ([DnsReachabilityProbe]) por construção.
 *
 * Mais de um IP tentado (Cloudflare/Google/Quad9) para não decidir "sem rota externa"
 * por um único provedor fora do ar.
 */
class ExternalIpReachabilityProbe(
    private val binding: ConnectivityProbeBinding,
    private val timeoutMs: Int = TIMEOUT_MS_DEFAULT,
    private val enderecos: List<String> = ENDERECOS_PADRAO,
    private val porta: Int = 443,
) : ExternalIpProbe {

    companion object {
        private const val TIMEOUT_MS_DEFAULT = 1500
        private val ENDERECOS_PADRAO = listOf("1.1.1.1", "8.8.8.8", "9.9.9.9")
    }

    /** `runInterruptible`: ver KDoc equivalente em [GatewayReachabilityProbe.probe]. */
    override suspend fun probe(): ProbeResult = runInterruptible(Dispatchers.IO) {
        var houveTimeout = false
        for (endereco in enderecos) {
            try {
                Socket().use { socket ->
                    binding.bindSocket(socket)
                    val alvo = InetSocketAddress(InetAddress.getByName(endereco), porta)
                    val inicio = System.currentTimeMillis()
                    socket.connect(alvo, timeoutMs)
                    return@runInterruptible ProbeResult.Success(System.currentTimeMillis() - inicio)
                }
            } catch (_: SocketTimeoutException) {
                houveTimeout = true
            } catch (_: IOException) {
                // recusado/inalcancavel nesse IP -- tenta o proximo, a menos que a
                // interrupcao (cancelamento/timeout do motor) tenha disparado esta excecao
                if (Thread.currentThread().isInterrupted) return@runInterruptible ProbeResult.Timeout(timeoutMs.toLong())
            } catch (_: SecurityException) {
                return@runInterruptible ProbeResult.Unavailable("sem permissao para socket na rede sob analise")
            }
        }
        if (houveTimeout) ProbeResult.Timeout(timeoutMs.toLong()) else ProbeResult.Failure(ProbeFailureReason.HOST_UNREACHABLE)
    }
}
