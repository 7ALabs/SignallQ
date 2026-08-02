package io.signallq.app.core.network.connectivity

import io.signallq.app.core.network.contracts.connectivity.ProbeFailureReason
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.net.InetAddress
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Testa resolução DNS pela rede sob análise (via [binding], nunca pelo resolver padrão
 * do sistema). Mais de um hostname tentado — não decide "sem internet" por uma única
 * falha de resolução.
 *
 * `Network.getAllByName` (por trás de [binding]) não tem parâmetro de timeout e não
 * responde a `Thread.interrupt()` -- é resolução nativa bloqueante via netd, então
 * `runInterruptible` sozinho NÃO impõe teto nenhum aqui (GH#1512, achado de revisão: o
 * timeout do motor não tinha efeito real sobre esta sondagem -- exatamente o caminho sem
 * limite algum no cenário central da issue, WAN caída com DNS travando). A resolução roda
 * em [ProbeBlockingExecutor] e o resultado é aguardado com um prazo real via
 * [java.util.concurrent.Future.get] -- se estourar, devolve Timeout sem esperar a thread
 * terminar; a resolução órfã pode seguir rodando em segundo plano e seu resultado é
 * descartado quando/se chegar (não há como cancelar uma chamada nativa de DNS com
 * segurança).
 */
class DnsReachabilityProbe(
    private val binding: ConnectivityProbeBinding,
    private val timeoutMs: Long = TIMEOUT_MS_DEFAULT,
) : DnsProbe {

    companion object {
        private const val TIMEOUT_MS_DEFAULT = 1500L
    }

    override suspend fun probe(hostnames: List<String>): ProbeResult = runInterruptible(Dispatchers.IO) {
        var houveTimeout = false
        for (hostname in hostnames) {
            val inicio = System.currentTimeMillis()
            val future = ProbeBlockingExecutor.instance.submit<Array<InetAddress>> { binding.resolveHost(hostname) }
            try {
                val enderecos = future.get(timeoutMs, TimeUnit.MILLISECONDS)
                if (enderecos.isNotEmpty()) {
                    return@runInterruptible ProbeResult.Success(System.currentTimeMillis() - inicio)
                }
            } catch (_: TimeoutException) {
                houveTimeout = true
            } catch (e: ExecutionException) {
                if (e.cause is SecurityException) {
                    return@runInterruptible ProbeResult.Unavailable("sem permissao para resolver DNS na rede sob analise")
                }
                // UnknownHostException (ou outra falha de resolucao) -- tenta o proximo hostname.
            } catch (_: InterruptedException) {
                // Cancelamento/timeout do motor -- correcao (3a revisao, GH#1512): retorna
                // IMEDIATAMENTE em vez de continuar o loop. `Future.get` limpa a flag de
                // interrupcao da thread ao lancar; continuar para o proximo hostname
                // custaria ate mais `timeoutMs` inteiros sem nenhum novo sinal de
                // cancelamento (runInterruptible so interrompe a thread uma vez).
                Thread.currentThread().interrupt()
                return@runInterruptible ProbeResult.Timeout(timeoutMs)
            }
        }
        if (houveTimeout) ProbeResult.Timeout(timeoutMs) else ProbeResult.Failure(ProbeFailureReason.DNS_RESOLUTION_FAILED)
    }
}
