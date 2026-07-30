package io.signallq.app.core.network.connectivity

import io.signallq.app.core.network.contracts.connectivity.ProbeFailureReason
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

/**
 * Testa alcance externo por hostname via HTTP GET curto, amarrado à rede sob análise.
 * Usa endpoints "generate_204" (mesmo padrão de detecção de captive portal usado pelo
 * próprio Android/Chrome) — uma resposta HTTP que não seja 204 nesses endpoints, ou um
 * redirect, é evidência (não prova isolada) de captive portal.
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

    override suspend fun probe(): HostnameProbeOutcome = withContext(Dispatchers.IO) {
        var houveTimeout = false
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
                val portalSuspeito = codigo in 300..399 || codigo != HttpURLConnection.HTTP_NO_CONTENT
                return@withContext HostnameProbeOutcome(ProbeResult.Success(), portalSuspeito)
            } catch (_: SocketTimeoutException) {
                houveTimeout = true
            } catch (_: IOException) {
                // host/endpoint indisponivel -- tenta o proximo
            } catch (_: SecurityException) {
                return@withContext HostnameProbeOutcome(
                    ProbeResult.Unavailable("sem permissao para HTTP na rede sob analise"),
                    captivePortalSuspeito = false,
                )
            } finally {
                conexao?.disconnect()
            }
        }
        val resultado = if (houveTimeout) ProbeResult.Timeout(timeoutMs.toLong()) else ProbeResult.Failure(ProbeFailureReason.HOST_UNREACHABLE)
        HostnameProbeOutcome(resultado, captivePortalSuspeito = false)
    }
}
