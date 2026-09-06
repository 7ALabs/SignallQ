package io.signallq.app.core.network.connectivity

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Executor compartilhado para sondagens de conectividade que precisam impor um teto real
 * de tempo de parede sobre chamadas nativas bloqueantes que não respondem a
 * `Thread.interrupt()` -- resolução DNS via netd ([DnsReachabilityProbe]) e a resolução
 * embutida em `HttpURLConnection.connect()` para URLs por hostname
 * ([HostnameReachabilityProbe]), que ocorre ANTES da fase coberta por
 * `connectTimeout`/`readTimeout` (GH#1512, achado de revisão).
 *
 * Cache pool: threads ociosas são liberadas após 60s; uma resolução que trava fica órfã
 * (não há como cancelar com segurança uma chamada nativa de DNS) até o próprio sistema
 * desistir. Compartilhado entre sondagens para não criar um pool novo a cada instância.
 */
internal object ProbeBlockingExecutor {
    val instance: ExecutorService =
        Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "connectivity-probe-blocking-io").apply { isDaemon = true }
        }
}
