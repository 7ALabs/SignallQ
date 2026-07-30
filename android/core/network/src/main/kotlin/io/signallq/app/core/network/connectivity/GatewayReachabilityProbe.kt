package io.signallq.app.core.network.connectivity

import io.signallq.app.core.network.contracts.connectivity.ProbeFailureReason
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * Testa o alcance do gateway local via TCP connect nas portas 53/80/443 (mesma
 * estratégia sem-ICMP de [io.signallq.app.core.network.GatewayLatencyMeasurer], adaptada
 * para devolver [ProbeResult] tipado e amarrar à rede sob análise via [binding]).
 *
 * Uma única porta respondendo já confirma alcance — não precisa das três.
 */
class GatewayReachabilityProbe(
    private val binding: ConnectivityProbeBinding,
    private val timeoutMs: Int = TIMEOUT_MS_DEFAULT,
    private val portas: List<Int> = PORTAS_PADRAO,
) : GatewayProbe {

    companion object {
        private const val TIMEOUT_MS_DEFAULT = 1200
        private val PORTAS_PADRAO = listOf(53, 80, 443)
    }

    /**
     * `runInterruptible` (não `withContext` puro) é obrigatório aqui: uma coroutine
     * cancelada (por [kotlinx.coroutines.withTimeoutOrNull] ou cancelamento externo) só
     * interrompe uma chamada bloqueante de verdade se a thread for interrompida
     * (`Thread.interrupt()`) -- `Socket.connect` honra isso lançando
     * `InterruptedIOException` (subtipo de [IOException], já tratado abaixo). Sem isso, o
     * timeout do motor não teria efeito nenhum sobre esta sondagem (GH#1512, achado de
     * revisão).
     */
    override suspend fun probe(gatewayIp: String): ProbeResult = runInterruptible(Dispatchers.IO) {
        var houveTimeout = false
        for (porta in portas) {
            try {
                Socket().use { socket ->
                    binding.bindSocket(socket)
                    val inicio = System.currentTimeMillis()
                    socket.connect(InetSocketAddress(gatewayIp, porta), timeoutMs)
                    return@runInterruptible ProbeResult.Success(System.currentTimeMillis() - inicio)
                }
            } catch (_: SocketTimeoutException) {
                houveTimeout = true
            } catch (_: IOException) {
                // porta fechada/recusada -- tenta a proxima, a menos que a interrupcao
                // (cancelamento/timeout do motor) tenha disparado esta excecao
                if (Thread.currentThread().isInterrupted) return@runInterruptible ProbeResult.Timeout(timeoutMs.toLong())
            } catch (_: SecurityException) {
                return@runInterruptible ProbeResult.Unavailable("sem permissao para socket na rede sob analise")
            }
        }
        if (houveTimeout) ProbeResult.Timeout(timeoutMs.toLong()) else ProbeResult.Failure(ProbeFailureReason.HOST_UNREACHABLE)
    }
}
