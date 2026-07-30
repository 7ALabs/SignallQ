package io.signallq.app.core.network.connectivity

import io.signallq.app.core.network.contracts.connectivity.ProbeFailureReason
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

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

    /** `runInterruptible`: ver KDoc equivalente em [GatewayReachabilityProbe.probe]. */
    override suspend fun probe(): HostnameProbeOutcome = runInterruptible(Dispatchers.IO) {
        var houveTimeout = false
        var houveRespostaInesperada = false
        for (urlStr in urls) {
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
                // inesperado é resposta inesperada, nao login obrigatorio. Um endpoint com
                // resposta inesperada nao encerra a sondagem -- tenta o proximo antes de
                // desistir (nunca decide por uma unica URL).
                val portalSuspeito = codigo in 300..399
                if (codigo == HttpURLConnection.HTTP_NO_CONTENT || portalSuspeito) {
                    return@runInterruptible HostnameProbeOutcome(ProbeResult.Success(), portalSuspeito)
                }
                houveRespostaInesperada = true
            } catch (_: SocketTimeoutException) {
                houveTimeout = true
            } catch (_: IOException) {
                // host/endpoint indisponivel -- tenta o proximo, a menos que a interrupcao
                // (cancelamento/timeout do motor) tenha disparado esta excecao
                if (Thread.currentThread().isInterrupted) {
                    return@runInterruptible HostnameProbeOutcome(ProbeResult.Timeout(timeoutMs.toLong()), captivePortalSuspeito = false)
                }
            } catch (_: SecurityException) {
                return@runInterruptible HostnameProbeOutcome(
                    ProbeResult.Unavailable("sem permissao para HTTP na rede sob analise"),
                    captivePortalSuspeito = false,
                )
            } finally {
                conexao?.disconnect()
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
