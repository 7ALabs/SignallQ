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
     * O teto real de tempo desta sondagem vem do [timeoutMs] passado explicitamente a
     * `Socket.connect(SocketAddress, timeout)` -- é o próprio JDK que impõe esse limite via
     * `SO_TIMEOUT` nativo, lançando [SocketTimeoutException] (já tratado abaixo). Correção
     * (3ª revisão, GH#1512): `runInterruptible` NÃO garante cancelamento de um
     * `Socket.connect()` em andamento -- `java.net.Socket` bloqueante não responde a
     * `Thread.interrupt()` (o `IoBridge` do Android faz retry em `EINTR`), então os ramos
     * `if (Thread.currentThread().isInterrupted)` abaixo são defesa best-effort, não
     * garantia. Mantido mesmo assim por consistência com as demais sondagens e porque é
     * inofensivo; o teto de tempo de verdade é sempre o [timeoutMs] explícito. Isso é
     * diferente da sondagem de DNS/hostname ([DnsReachabilityProbe],
     * [HostnameReachabilityProbe]), que não tem nenhum teto nativo próprio na fase de
     * resolução e por isso precisa de um mecanismo adicional de prazo real
     * ([ProbeBlockingExecutor]).
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
