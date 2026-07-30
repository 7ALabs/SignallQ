package io.signallq.app.core.network.connectivity

import io.signallq.app.core.network.contracts.connectivity.ProbeFailureReason
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Testa alcance externo por hostname via HTTP GET curto, amarrado à rede sob análise.
 * Usa endpoints "generate_204" (mesmo padrão de detecção de captive portal usado pelo
 * próprio Android/Chrome) — um **redirect** (3xx) nesses endpoints é evidência (não prova
 * isolada) de captive portal. Códigos inesperados que não sejam redirect (4xx/5xx/200 com
 * corpo) são tratados como resposta inesperada, não como captive portal — um proxy
 * corporativo ou um erro do endpoint gstatic não deveria virar "esta rede exige login"
 * com confiança alta (GH#1512, achado de revisão).
 *
 * Independente de [ExternalIpReachabilityProbe]: aqui o objetivo é confirmar que HTTP
 * "de verdade" funciona (TLS, HTTP, DNS já usado na camada anterior), não apenas TCP puro.
 *
 * Correção (3ª revisão, GH#1512): `connectTimeout`/`readTimeout` do `HttpURLConnection` só
 * cobrem a fase de `Socket.connect()` -- a resolução do hostname embutida em
 * `binding.openConnection(url)`/`conexao.connect()` acontece ANTES disso e usa a mesma
 * chamada nativa sem teto que [DnsReachabilityProbe] precisou corrigir (a issue original
 * usava `www.gstatic.com`, hostname diferente do sondado por [DnsReachabilityProbe], então
 * um resolvedor parcialmente travado podia responder um e pendurar no outro). Cada
 * tentativa completa (resolução + connect + read) roda em [ProbeBlockingExecutor] e é
 * aguardada com um prazo real via [java.util.concurrent.Future.get].
 */
class HostnameReachabilityProbe(
    private val binding: ConnectivityProbeBinding,
    private val timeoutMs: Int = TIMEOUT_MS_DEFAULT,
    private val urls: List<String> = URLS_PADRAO,
) : HostnameProbe {

    companion object {
        private const val TIMEOUT_MS_DEFAULT = 2500
        private val URLS_PADRAO = listOf(
            "https://www.gstatic.com/generate_204",
            "https://connectivitycheck.gstatic.com/generate_204",
        )
    }

    /** Resultado de uma tentativa completa (resolução + connect + read) numa única URL. */
    private sealed interface ResultadoTentativa {
        data class Concluido(val outcome: HostnameProbeOutcome) : ResultadoTentativa
        object RespostaInesperada : ResultadoTentativa
    }

    override suspend fun probe(): HostnameProbeOutcome = runInterruptible(Dispatchers.IO) {
        var houveTimeout = false
        var houveRespostaInesperada = false
        // Prazo por tentativa: cobre resolucao (sem teto nativo) + connect + read (cada
        // uma ja limitada a timeoutMs por connectTimeout/readTimeout) -- ver KDoc da classe.
        val prazoTentativaMs = timeoutMs.toLong() * 3
        for (urlStr in urls) {
            val future = ProbeBlockingExecutor.instance.submit<ResultadoTentativa> {
                var conexao: HttpURLConnection? = null
                try {
                    val url = URL(urlStr)
                    conexao = (binding.openConnection(url) as HttpURLConnection).apply {
                        connectTimeout = timeoutMs
                        readTimeout = timeoutMs
                        instanceFollowRedirects = false
                        requestMethod = "GET"
                    }
                    conexao.connect()
                    val codigo = conexao.responseCode
                    // Só redirect (3xx) conta como suspeita de captive portal -- 4xx/5xx/200
                    // inesperado e resposta inesperada, nao login obrigatorio.
                    val portalSuspeito = codigo in 300..399
                    if (codigo == HttpURLConnection.HTTP_NO_CONTENT || portalSuspeito) {
                        ResultadoTentativa.Concluido(HostnameProbeOutcome(ProbeResult.Success(), portalSuspeito))
                    } else {
                        ResultadoTentativa.RespostaInesperada
                    }
                } finally {
                    conexao?.disconnect()
                }
            }
            try {
                when (val resultado = future.get(prazoTentativaMs, TimeUnit.MILLISECONDS)) {
                    is ResultadoTentativa.Concluido -> return@runInterruptible resultado.outcome
                    ResultadoTentativa.RespostaInesperada -> houveRespostaInesperada = true
                }
            } catch (_: TimeoutException) {
                houveTimeout = true
            } catch (e: ExecutionException) {
                when (e.cause) {
                    is SecurityException -> return@runInterruptible HostnameProbeOutcome(
                        ProbeResult.Unavailable("sem permissao para HTTP na rede sob analise"),
                        captivePortalSuspeito = false,
                    )
                    is SocketTimeoutException -> houveTimeout = true
                    // outro IOException -- host/endpoint indisponivel, tenta o proximo
                }
            } catch (_: InterruptedException) {
                // Cancelamento/timeout do motor -- retorna imediatamente (ver KDoc
                // equivalente em DnsReachabilityProbe sobre nao continuar o loop aqui).
                Thread.currentThread().interrupt()
                return@runInterruptible HostnameProbeOutcome(ProbeResult.Timeout(timeoutMs.toLong()), captivePortalSuspeito = false)
            }
        }
        val resultado = when {
            houveTimeout -> ProbeResult.Timeout(timeoutMs.toLong())
            houveRespostaInesperada -> ProbeResult.Failure(ProbeFailureReason.UNEXPECTED_RESPONSE)
            else -> ProbeResult.Failure(ProbeFailureReason.HOST_UNREACHABLE)
        }
        HostnameProbeOutcome(resultado, captivePortalSuspeito = false)
    }
}
