package io.signallq.app.core.network.connectivity

import io.signallq.app.core.network.contracts.connectivity.ProbeFailureReason
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Sondagem DoH (DNS-over-HTTPS, RFC 8484) contra um provedor público fixo, independente
 * do resolvedor DNS da rede sob análise (GH#1811, Task 1/4). Serve de *fallback* para
 * diferenciar "DNS configurado na rede quebrado" de "sem internet real": se este probe
 * resolve mas [DnsReachabilityProbe] (amarrado à rede via [ConnectivityProbeBinding])
 * falha, a causa provável é o resolvedor da própria rede, não a rota externa.
 *
 * Implementa [DnsProbe] — a MESMA interface de [DnsReachabilityProbe] — para ficar
 * compatível com o agregado consumido por [ConnectivityDiagnosisEngine] sem exigir
 * mudança de contrato; o wiring de fato ao motor é a Task 4 da #1811, fora deste escopo.
 *
 * Diferente de [DnsReachabilityProbe]: não usa [ConnectivityProbeBinding] — a requisição
 * HTTPS roda pelo caminho padrão do sistema via OkHttp, porque o propósito aqui é
 * justamente contornar o DNS/rota configurados na rede sob análise, não amarrar-se a ela.
 *
 * Uma única tentativa, contra um único provedor, sem retry entre hostnames — é um
 * fallback pontual do diagnóstico local, não um benchmark comparativo (isso é
 * `BenchmarkDnsDoh`, em `feature/dns`, fora do alcance de `core/network` pela regra de
 * fronteira core→feature — este arquivo não importa nada de lá).
 *
 * O parsing da resposta DoH é mínimo e local a este arquivo: só o suficiente para
 * distinguir NOERROR-com-resposta de NXDOMAIN/SERVFAIL/vazio/malformado (RFC 1035 §4.1.1),
 * sem decodificar records.
 */
class DohFallbackProbe(
    httpClientBase: OkHttpClient = HTTP_CLIENT_PADRAO,
    private val endpoint: String = ENDPOINT_PADRAO,
    private val timeoutMs: Long = TIMEOUT_MS_DEFAULT,
) : DnsProbe {

    // Construído uma única vez por instância (não a cada chamada de `probe`) -- OkHttpClient
    // é caro de criar (pool de conexões, dispatcher próprio). `httpClientBase` só empresta
    // configuração compartilhada (ex.: interceptors); os timeouts de fato aplicados são
    // sempre derivados de `timeoutMs`, nunca do valor fixo do cliente base -- é a causa raiz
    // da ressalva de revisão do Caio na PR #1812: antes, `timeoutMs` não configurava nada.
    private val httpClient: OkHttpClient = httpClientBase.newBuilder()
        .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .build()

    companion object {
        private const val TIMEOUT_MS_DEFAULT = 2000L

        // Cloudflare -- mesmo provedor usado como primeira opção em BenchmarkDnsDoh
        // (feature/dns), mas sem nenhuma dependência de código daquele módulo.
        private const val ENDPOINT_PADRAO = "https://cloudflare-dns.com/dns-query"

        // Base sem timeout customizado -- os timeouts reais são sempre reaplicados acima
        // a partir de `timeoutMs`, então o valor aqui é irrelevante na prática.
        private val HTTP_CLIENT_PADRAO = OkHttpClient.Builder().build()
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun probe(hostnames: List<String>): ProbeResult = withContext(Dispatchers.IO) {
        val hostname = hostnames.firstOrNull()
            ?: return@withContext ProbeResult.Unavailable("nenhum hostname informado para sondagem DoH")

        val inicio = System.currentTimeMillis()
        try {
            val request = construirRequest(hostname)
            httpClient.newCall(request).execute().use { response ->
                val decorridoMs = System.currentTimeMillis() - inicio
                if (!response.isSuccessful) {
                    return@withContext ProbeResult.Failure(ProbeFailureReason.UNEXPECTED_RESPONSE)
                }
                val corpo = response.body.bytes()

                when (validarRespostaDoh(corpo)) {
                    DohValidationResult.VALIDA -> ProbeResult.Success(decorridoMs)
                    DohValidationResult.NXDOMAIN, DohValidationResult.SEM_RESPOSTA ->
                        ProbeResult.Failure(ProbeFailureReason.DNS_RESOLUTION_FAILED)
                    DohValidationResult.MALFORMADA, DohValidationResult.OUTRO_ERRO ->
                        ProbeResult.Failure(ProbeFailureReason.UNEXPECTED_RESPONSE)
                }
            }
        } catch (_: InterruptedIOException) {
            // SocketTimeoutException (subclasse) cobre connect/read timeout; OkHttp usa
            // InterruptedIOException puro para estourar o callTimeout global.
            ProbeResult.Timeout(timeoutMs)
        } catch (_: IOException) {
            // Falha de conexão/TLS/host inalcançável -- sem alcance ao provedor DoH.
            ProbeResult.Failure(ProbeFailureReason.HOST_UNREACHABLE)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun construirRequest(hostname: String): Request {
        val url = endpoint.toHttpUrl()
            .newBuilder()
            .addQueryParameter("dns", construirDnsQueryBase64Url(hostname))
            .build()

        return Request.Builder()
            .url(url)
            .get()
            .header("accept", "application/dns-message")
            .build()
    }

    /** Monta uma consulta DNS binária mínima (RFC 1035), tipo A, codificada em base64url sem padding (RFC 8484). */
    @OptIn(ExperimentalEncodingApi::class)
    private fun construirDnsQueryBase64Url(hostname: String): String {
        val saida = ByteArrayOutputStream()
        saida.write(byteArrayOf(0, 0)) // ID = 0 -- irrelevante em DoH via GET
        saida.write(byteArrayOf(1, 0)) // flags: RD=1
        saida.write(byteArrayOf(0, 1, 0, 0, 0, 0, 0, 0)) // QDCOUNT=1, demais=0
        hostname
            .trim('.')
            .split('.')
            .filter { it.isNotBlank() }
            .forEach { label ->
                val bytes = label.toByteArray(Charsets.UTF_8)
                saida.write(bytes.size)
                saida.write(bytes)
            }
        saida.write(0) // terminador do QNAME
        saida.write(byteArrayOf(0, 1, 0, 1)) // QTYPE=A, QCLASS=IN
        return Base64.UrlSafe.encode(saida.toByteArray()).trimEnd('=')
    }

    /**
     * Decodifica só o cabeçalho RFC 1035 da resposta (RCODE nos 4 bits baixos do byte 3,
     * ANCOUNT nos bytes 6-7) -- suficiente para distinguir sucesso real de
     * NXDOMAIN/SERVFAIL/vazio, sem parsing de records.
     */
    private fun validarRespostaDoh(corpo: ByteArray): DohValidationResult {
        if (corpo.size < 12) return DohValidationResult.MALFORMADA
        val rcode = corpo[3].toInt() and 0x0F
        val ancount = ((corpo[6].toInt() and 0xFF) shl 8) or (corpo[7].toInt() and 0xFF)
        return when {
            rcode == 3 -> DohValidationResult.NXDOMAIN
            rcode != 0 -> DohValidationResult.OUTRO_ERRO
            ancount == 0 -> DohValidationResult.SEM_RESPOSTA
            else -> DohValidationResult.VALIDA
        }
    }

    private enum class DohValidationResult {
        VALIDA,
        NXDOMAIN,
        SEM_RESPOSTA,
        MALFORMADA,
        OUTRO_ERRO,
    }
}
