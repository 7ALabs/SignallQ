package io.signallq.app.feature.dns

import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// #378: suite inteira (sistema + 7 provedores x 3 rounds) tinha timeout apenas por
// tentativa individual — sem rede, a soma das tentativas travava o sheet no skeleton
// por muito além do razoável. Timeout global garante feedback rápido ao usuário.
private const val TIMEOUT_SUITE_DNS_MS = 15_000L

@OptIn(ExperimentalEncodingApi::class)
class BenchmarkDnsDoh : BenchmarkDns {
    private val executando = AtomicBoolean(false)
    private val httpClient =
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(9, TimeUnit.SECONDS)
            .build()

    private val mutableSnapshotFlow =
        MutableStateFlow(
            SnapshotBenchmarkDns(
                estado = EstadoBenchmarkDns.idle,
                progressoPercentual = 0,
                resultados = emptyList(),
                erroMensagem = null,
            ),
        )

    override val snapshotFlow: StateFlow<SnapshotBenchmarkDns> = mutableSnapshotFlow.asStateFlow()

    override suspend fun executar(
        hostConsulta: String,
        resolvedoresAtivos: List<String>,
        privateDnsHostname: String?,
    ) {
        if (!executando.compareAndSet(false, true)) return
        withContext(Dispatchers.IO) {
            try {
                val concluiu =
                    withTimeoutOrNull(TIMEOUT_SUITE_DNS_MS) {
                        Timber.i("iniciando benchmark DNS host=$hostConsulta resolvedores=$resolvedoresAtivos privateDns=$privateDnsHostname")
                        publicar(EstadoBenchmarkDns.executando, 5, emptyList(), null)

                        val resultadoSistema = medirSistemaDns(hostConsulta, resolvedoresAtivos, privateDnsHostname)
                        Timber.i("sistema dns: nome=${resultadoSistema.nomeProvedor} tempo=${resultadoSistema.tempoMs} grade=${resultadoSistema.gradeRapidez} amostras=${resultadoSistema.amostrasMs}")
                        val acumulados = mutableListOf<ResultadoBenchmarkDns>()
                        publicar(
                            EstadoBenchmarkDns.executando,
                            15,
                            combinarResultados(resultadoSistema, acumulados),
                            null,
                        )

                        provedoresPublicos.forEachIndexed { idx, provedor ->
                            val resultado = medirProvedor(provedor, hostConsulta)
                            Timber.i("provedor ${provedor.nome}: tempo=${resultado.tempoMs} grade=${resultado.gradeRapidez} amostras=${resultado.amostrasMs} erro=${resultado.erroMensagem}")
                            acumulados.add(resultado)
                            val progresso = 20 + (((idx + 1).toDouble() / provedoresPublicos.size.toDouble()) * 75.0).toInt()
                            publicar(
                                EstadoBenchmarkDns.executando,
                                progresso,
                                combinarResultados(resultadoSistema, acumulados),
                                null,
                            )
                        }

                        val final = combinarResultados(resultadoSistema, acumulados)
                        Timber.i("benchmark concluido: ${final.size} provedores exibidos: ${final.map { "${it.nomeProvedor}=${it.tempoMs}ms(${it.gradeRapidez}) erro=${it.erroMensagem}" }}")
                        publicar(EstadoBenchmarkDns.concluido, 100, final, null)
                        Unit
                    }
                if (concluiu == null) {
                    Timber.w("benchmark DNS excedeu timeout de ${TIMEOUT_SUITE_DNS_MS}ms — provável offline")
                    publicar(EstadoBenchmarkDns.erro, 100, emptyList(), "semRede")
                }
            } catch (t: Throwable) {
                publicar(EstadoBenchmarkDns.erro, 100, emptyList(), t.message ?: "erroBenchmarkDns")
            } finally {
                executando.set(false)
            }
        }
    }

    private fun publicar(
        estado: EstadoBenchmarkDns,
        progressoPercentual: Int,
        resultados: List<ResultadoBenchmarkDns>,
        erroMensagem: String?,
    ) {
        mutableSnapshotFlow.value =
            SnapshotBenchmarkDns(
                estado = estado,
                progressoPercentual = min(100, max(0, progressoPercentual)),
                resultados = resultados,
                erroMensagem = erroMensagem,
            )
    }

    internal fun combinarResultados(
        resultadoSistema: ResultadoBenchmarkDns,
        resultadosPublicos: List<ResultadoBenchmarkDns>,
    ): List<ResultadoBenchmarkDns> {
        val resultados = mutableListOf<ResultadoBenchmarkDns>()
        if (resultadoSistema.tempoMs != null && !resultadoSistema.isGatewayLocal) {
            resultados.add(resultadoSistema)
        }
        resultados += resultadosPublicos
        return resultados.sortedWith(
            compareBy<ResultadoBenchmarkDns> { it.tempoMs == null }
                .thenBy { it.tempoMs ?: Double.MAX_VALUE },
        )
    }

    // Mede o DNS do sistema via InetAddress com mesma metodologia dos publicos:
    // 3 rounds, descarta round 0 (warmup) + filtra <3ms (cache SO), P50 dos validos.
    private fun medirSistemaDns(
        hostConsulta: String,
        resolvedoresAtivos: List<String>,
        privateDnsHostname: String?,
    ): ResultadoBenchmarkDns {
        val amostras = mutableListOf<Double>()
        repeat(3) { round ->
            try {
                val inicio = System.nanoTime()
                InetAddress.getByName(hostConsulta)
                val ms = (System.nanoTime() - inicio) / 1_000_000.0
                // Round 0 descartado como warmup. Rounds 1-2 sempre incluídos —
                // o filtro anterior (>= 3ms) excluía DNS rápidos de operadora falsamente
                // considerando-os "cache do SO". Latência real < 3ms é válida.
                if (round > 0) amostras.add(ms)
            } catch (_: Throwable) { }
        }
        val tempo = calcularP50(amostras)
        val nome = inferirNomeSistemaDns(resolvedoresAtivos, privateDnsHostname)
        val gatewayLocal = nome == "Roteador da rede"
        return ResultadoBenchmarkDns(
            nomeProvedor = nome,
            hostConsulta = hostConsulta,
            tempoMs = tempo,
            amostrasMs = amostras,
            tentativas = 3,
            sucessos = amostras.size,
            taxaSucessoPercentual = if (amostras.isEmpty()) 0.0 else (amostras.size.toDouble() / 2.0) * 100.0,
            erroMensagem = if (tempo == null) "semResposta" else null,
            gradeRapidez = if (gatewayLocal) null else tempo?.let { calcularGrade(it) },
            isGatewayLocal = gatewayLocal,
        )
    }

    // 3 rounds, descarta round 0 (warmup), P50 dos rounds 1-2.
    private fun medirProvedor(
        provedor: DnsPublico,
        hostConsulta: String,
    ): ResultadoBenchmarkDns {
        val amostras = mutableListOf<Double>()
        var ultimoErro: String? = null

        repeat(3) { round ->
            val tempo = medirTentativa(provedor, hostConsulta)
            if (round > 0) {
                if (tempo != null) amostras.add(tempo)
                else if (ultimoErro == null) ultimoErro = "semResposta"
            }
        }

        val tempo = calcularP50(amostras)
        return ResultadoBenchmarkDns(
            nomeProvedor = provedor.nome,
            hostConsulta = hostConsulta,
            tempoMs = tempo,
            amostrasMs = amostras,
            tentativas = 3,
            sucessos = amostras.size,
            taxaSucessoPercentual = if (amostras.isEmpty()) 0.0 else (amostras.size.toDouble() / 2.0) * 100.0,
            erroMensagem = if (tempo == null) (ultimoErro ?: "erroConsulta") else null,
            gradeRapidez = tempo?.let { calcularGrade(it) },
        )
    }

    private fun medirTentativa(
        provedor: DnsPublico,
        hostConsulta: String,
    ): Double? {
        val inicio = System.nanoTime()
        return try {
            val request = construirRequest(provedor, hostConsulta)
            httpClient.newCall(request).execute().use { response ->
                val elapsed = (System.nanoTime() - inicio) / 1_000_000.0
                response.body?.bytes()
                if (response.isSuccessful) elapsed else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun construirRequest(
        provedor: DnsPublico,
        hostConsulta: String,
    ): Request {
        val url =
            when (provedor.modoConsulta) {
                DnsQueryMode.Json ->
                    provedor.endpoint
                        .toHttpUrl()
                        .newBuilder()
                        .addQueryParameter("name", hostConsulta)
                        .addQueryParameter("type", "A")
                        .build()
                DnsQueryMode.Rfc8484 ->
                    provedor.endpoint
                        .toHttpUrl()
                        .newBuilder()
                        .addQueryParameter("dns", construirDnsQueryBase64Url(hostConsulta))
                        .build()
            }

        val acceptHeader =
            when (provedor.modoConsulta) {
                DnsQueryMode.Json -> "application/dns-json, application/json"
                DnsQueryMode.Rfc8484 -> "application/dns-message"
            }

        return Request.Builder()
            .url(url)
            .get()
            .header("accept", acceptHeader)
            .build()
    }

    internal fun construirDnsQueryBase64Url(hostConsulta: String): String {
        val saida = ByteArrayOutputStream()
        val id = 0
        saida.write(byteArrayOf(0, id.toByte()))
        saida.write(byteArrayOf(1, 0)) // RD
        saida.write(byteArrayOf(0, 1, 0, 0, 0, 0, 0, 0))
        hostConsulta
            .trim('.')
            .split('.')
            .filter { it.isNotBlank() }
            .forEach { label ->
                val bytes = label.toByteArray(Charsets.UTF_8)
                saida.write(bytes.size)
                saida.write(bytes)
            }
        saida.write(0)
        saida.write(byteArrayOf(0, 1, 0, 1))
        return Base64.UrlSafe.encode(saida.toByteArray()).trimEnd('=')
    }

    // P50 identico ao Flutter: sorted[size / 2] (divisao inteira).
    private fun calcularP50(amostras: List<Double>): Double? {
        if (amostras.isEmpty()) return null
        val ordenadas = amostras.sorted()
        return ordenadas[ordenadas.size / 2]
    }

    private fun calcularGrade(ms: Double): String = when {
        ms <= 15.0 -> "A"
        ms <= 30.0 -> "B"
        ms <= 50.0 -> "C"
        else -> "D"
    }

    private fun inferirNomeSistemaDns(
        resolvedoresAtivos: List<String>,
        privateDnsHostname: String?,
    ): String {
        if (!privateDnsHostname.isNullOrBlank()) {
            val h = privateDnsHostname.lowercase()
            mapaHostParaProvedor.entries.firstOrNull { h.contains(it.key) }?.value?.let { return it }
        }
        for (ip in resolvedoresAtivos.map { it.trim() }.filter { it.isNotBlank() }) {
            if (isIpPrivado(ip)) return "Roteador da rede"
            mapaIpParaProvedor[ip]?.let { return it }
        }
        return "DNS do Provedor"
    }

    // Retorna true para IPs RFC-1918, link-local e loopback — não são DNS públicos reais.
    private fun isIpPrivado(ip: String): Boolean {
        val partes = ip.split(".").mapNotNull { it.toIntOrNull() }
        if (partes.size != 4) return false
        val (a, b) = partes
        return when {
            a == 10 -> true
            a == 172 && b in 16..31 -> true
            a == 192 && b == 168 -> true
            a == 169 && b == 254 -> true  // link-local
            a == 127 -> true               // loopback
            else -> false
        }
    }

    private companion object {
        val provedoresPublicos =
            listOf(
                DnsPublico("Cloudflare", "https://cloudflare-dns.com/dns-query", DnsQueryMode.Json),
                DnsPublico("Google DNS", "https://dns.google/resolve", DnsQueryMode.Json),
                DnsPublico("Quad9", "https://dns.quad9.net/dns-query", DnsQueryMode.Rfc8484),
                DnsPublico("OpenDNS", "https://doh.opendns.com/dns-query", DnsQueryMode.Rfc8484),
                DnsPublico("AdGuard", "https://dns.adguard-dns.com/dns-query", DnsQueryMode.Rfc8484),
                DnsPublico("Control D", "https://freedns.controld.com/p0", DnsQueryMode.Rfc8484),
                DnsPublico("CleanBrowsing", "https://doh.cleanbrowsing.org/doh/security-filter/", DnsQueryMode.Rfc8484),
            )

        val mapaIpParaProvedor = mapOf(
            "1.1.1.1" to "Cloudflare",
            "1.0.0.1" to "Cloudflare",
            "8.8.8.8" to "Google DNS",
            "8.8.4.4" to "Google DNS",
            "9.9.9.9" to "Quad9",
            "149.112.112.112" to "Quad9",
            "208.67.222.222" to "OpenDNS",
            "208.67.220.220" to "OpenDNS",
            "94.140.14.14" to "AdGuard",
            "94.140.15.15" to "AdGuard",
            "76.76.2.0" to "Control D",
            "76.76.10.0" to "Control D",
            "185.228.168.9" to "CleanBrowsing",
            "185.228.169.9" to "CleanBrowsing",
        )
        val mapaHostParaProvedor = mapOf(
            "one.one.one.one" to "Cloudflare",
            "dns.google" to "Google DNS",
            "dns.quad9.net" to "Quad9",
            "doh.opendns.com" to "OpenDNS",
            "adguard" to "AdGuard",
            "adguard-dns.com" to "AdGuard",
            "freedns.controld.com" to "Control D",
            "controld.com" to "Control D",
            "cleanbrowsing.org" to "CleanBrowsing",
            "security-filter-dns.cleanbrowsing.org" to "CleanBrowsing",
        )
    }
}

private enum class DnsQueryMode {
    Json,
    Rfc8484,
}

private data class DnsPublico(
    val nome: String,
    val endpoint: String,
    val modoConsulta: DnsQueryMode,
)
