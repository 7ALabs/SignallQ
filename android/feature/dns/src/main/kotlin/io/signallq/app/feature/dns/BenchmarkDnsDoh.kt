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

// #378: suite inteira (sistema + 7 provedores x rounds) tinha timeout apenas por
// tentativa individual — sem rede, a soma das tentativas travava o sheet no skeleton
// por muito além do razoável. Timeout global garante feedback rápido ao usuário.
// GH#1212: aumentado de 15s pra 25s pra acomodar a amostragem maior (6 rounds em vez de 3).
private const val TIMEOUT_SUITE_DNS_MS = 25_000L

// GH#1212 item 5 — antes eram 3 rounds (1 warmup + 2 avaliados), insuficiente pra uma
// recomendação confiável. Agora 6 rounds (1 warmup + 5 avaliados), igual pro sistema e
// pros provedores públicos.
private const val ROUNDS_POR_MEDICAO = 6

// GH#1212 item 8 — diferença abaixo desse valor não justifica declarar um vencedor.
private const val MARGEM_EMPATE_TECNICO_MS = 10.0

/**
 * GH#1212 item 2/4 — hostnames reais e estáveis usados em rotação pra medir o DNS do
 * SISTEMA (via [InetAddress.getByName]). Rotacionar evita que os rounds 2..6 batam sempre
 * na mesma entrada do cache do resolvedor Android/roteador — cada round consulta um nome
 * diferente. Só se aplica à medição do sistema: os provedores DoH públicos já fazem uma
 * consulta de rede de verdade a cada chamada HTTP, então repetir o mesmo hostname neles
 * mede o comportamento real de cache do PRÓPRIO provedor (não é um artefato de teste).
 */
private val HOSTNAMES_ROTACAO_SISTEMA =
    listOf("example.com", "cloudflare.com", "one.one.one.one", "google.com", "iana.org")

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

                        val resultadoSistema = medirSistemaDns(resolvedoresAtivos, privateDnsHostname)
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
                            Timber.i(
                                "provedor ${provedor.nome}: tempo=${resultado.tempoMs} grade=${resultado.gradeRapidez} " +
                                    "amostras=${resultado.amostrasMs} erro=${resultado.erroMensagem} invalida=${resultado.respostaInvalida}",
                            )
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

    /**
     * GH#1212 item 8/13 — mantém provedores que falharam (comportamento preservado dos
     * testes de caracterização), mas o "melhor" pra recomendação (usado pelo [DnsScreen])
     * não é mais simplesmente o menor tempo: ver [melhorComMargem].
     */
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

    // Mede o DNS do sistema via InetAddress com metodologia equivalente aos públicos:
    // ROUNDS_POR_MEDICAO rounds, descarta round 0 (warmup), mediana dos válidos.
    // GH#1212 item 2 — cada round consulta um hostname diferente (rotação), não sempre o
    // mesmo, pra reduzir o efeito do cache do resolvedor Android/roteador.
    private fun medirSistemaDns(
        resolvedoresAtivos: List<String>,
        privateDnsHostname: String?,
    ): ResultadoBenchmarkDns {
        val amostras = mutableListOf<Double>()
        repeat(ROUNDS_POR_MEDICAO) { round ->
            val host = HOSTNAMES_ROTACAO_SISTEMA[round % HOSTNAMES_ROTACAO_SISTEMA.size]
            try {
                val inicio = System.nanoTime()
                InetAddress.getByName(host)
                val ms = (System.nanoTime() - inicio) / 1_000_000.0
                // Round 0 descartado como warmup. Rounds seguintes sempre incluídos —
                // latência real < 3ms é válida (não é necessariamente cache).
                if (round > 0) amostras.add(ms)
            } catch (_: Throwable) { }
        }
        val tentativasAvaliadas = ROUNDS_POR_MEDICAO - 1
        val tempo = calcularMediana(amostras)
        val nome = inferirNomeSistemaDns(resolvedoresAtivos, privateDnsHostname)
        val gatewayLocal = nome == "Roteador da rede"
        return ResultadoBenchmarkDns(
            nomeProvedor = nome,
            hostConsulta = HOSTNAMES_ROTACAO_SISTEMA.first(),
            tempoMs = tempo,
            amostrasMs = amostras,
            tentativas = ROUNDS_POR_MEDICAO,
            sucessos = amostras.size,
            taxaSucessoPercentual = taxaSucesso(amostras.size, tentativasAvaliadas),
            erroMensagem = if (tempo == null) "semResposta" else null,
            gradeRapidez = if (gatewayLocal) null else tempo?.let { calcularGrade(it) },
            isGatewayLocal = gatewayLocal,
            tentativasAvaliadas = tentativasAvaliadas,
        )
    }

    // ROUNDS_POR_MEDICAO rounds, descarta round 0 (warmup), mediana dos rounds avaliados.
    private fun medirProvedor(
        provedor: DnsPublico,
        hostConsulta: String,
    ): ResultadoBenchmarkDns {
        val amostras = mutableListOf<Double>()
        var ultimoErro: String? = null
        var teveRespostaInvalida = false

        repeat(ROUNDS_POR_MEDICAO) { round ->
            val tentativa = medirTentativa(provedor, hostConsulta)
            if (round > 0) {
                when {
                    tentativa is TentativaDns.Sucesso -> amostras.add(tentativa.ms)
                    tentativa is TentativaDns.RespostaInvalida -> {
                        teveRespostaInvalida = true
                        if (ultimoErro == null) ultimoErro = tentativa.motivo
                    }
                    else -> if (ultimoErro == null) ultimoErro = "semResposta"
                }
            }
        }

        val tentativasAvaliadas = ROUNDS_POR_MEDICAO - 1
        val tempo = calcularMediana(amostras)
        return ResultadoBenchmarkDns(
            nomeProvedor = provedor.nome,
            hostConsulta = hostConsulta,
            tempoMs = tempo,
            amostrasMs = amostras,
            tentativas = ROUNDS_POR_MEDICAO,
            sucessos = amostras.size,
            taxaSucessoPercentual = taxaSucesso(amostras.size, tentativasAvaliadas),
            erroMensagem = if (tempo == null) (ultimoErro ?: "erroConsulta") else null,
            gradeRapidez = tempo?.let { calcularGrade(it) },
            tentativasAvaliadas = tentativasAvaliadas,
            respostaInvalida = teveRespostaInvalida,
        )
    }

    internal fun taxaSucesso(
        sucessos: Int,
        tentativasAvaliadas: Int,
    ): Double = if (tentativasAvaliadas <= 0) 0.0 else (sucessos.toDouble() / tentativasAvaliadas.toDouble()) * 100.0

    private fun medirTentativa(
        provedor: DnsPublico,
        hostConsulta: String,
    ): TentativaDns {
        val inicio = System.nanoTime()
        return try {
            val request = construirRequest(provedor, hostConsulta)
            httpClient.newCall(request).execute().use { response ->
                val elapsed = (System.nanoTime() - inicio) / 1_000_000.0
                val corpo = response.body?.bytes()
                if (!response.isSuccessful || corpo == null) return TentativaDns.Falha
                // GH#1212 item 6 — HTTP 200 não é sinônimo de resposta DNS válida: decodifica
                // o cabeçalho RFC 1035 (RCODE nos 4 bits baixos do byte 3, ANCOUNT nos bytes
                // 6-7) e só conta como sucesso quando RCODE=0 (NOERROR) e há ao menos 1 answer.
                val validacao = validarRespostaDnsBinaria(corpo)
                when (validacao) {
                    ValidacaoDns.Valida -> TentativaDns.Sucesso(elapsed)
                    else -> TentativaDns.RespostaInvalida(validacao.motivo)
                }
            }
        } catch (_: Throwable) {
            TentativaDns.Falha
        }
    }

    private fun construirRequest(
        provedor: DnsPublico,
        hostConsulta: String,
    ): Request {
        val url =
            provedor.endpoint
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("dns", construirDnsQueryBase64Url(hostConsulta))
                .build()

        return Request.Builder()
            .url(url)
            .get()
            .header("accept", "application/dns-message")
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

    /**
     * GH#1212 item 6/9 — valida o cabeçalho RFC 1035 de uma resposta DNS binária (RFC 8484).
     * Não faz parsing completo de records (nomes com compressão de ponteiros, TTL, RDATA) —
     * suficiente pra distinguir NOERROR-com-resposta de NXDOMAIN/SERVFAIL/REFUSED/vazio, que
     * é exatamente o que o critério de aceite pede (não tratar essas respostas como sucesso).
     */
    internal fun validarRespostaDnsBinaria(corpo: ByteArray): ValidacaoDns {
        if (corpo.size < 12) return ValidacaoDns.Malformada
        val rcode = corpo[3].toInt() and 0x0F
        val ancount = ((corpo[6].toInt() and 0xFF) shl 8) or (corpo[7].toInt() and 0xFF)
        return when {
            rcode == 3 -> ValidacaoDns.Nxdomain
            rcode == 2 -> ValidacaoDns.Servfail
            rcode == 5 -> ValidacaoDns.Refused
            rcode != 0 -> ValidacaoDns.OutroErro(rcode)
            ancount == 0 -> ValidacaoDns.SemResposta
            else -> ValidacaoDns.Valida
        }
    }

    // Mediana correta pra quantidade par e ímpar de amostras (GH#1212 item 4 — o antigo
    // "P50" fazia sorted[size/2], que pra 2 amostras devolve a MAIOR, não uma mediana real).
    internal fun calcularMediana(amostras: List<Double>): Double? {
        if (amostras.isEmpty()) return null
        val ordenadas = amostras.sorted()
        val m = ordenadas.size / 2
        return if (ordenadas.size % 2 == 0) {
            (ordenadas[m - 1] + ordenadas[m]) / 2.0
        } else {
            ordenadas[m]
        }
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
            if (DetectorEnderecoIpPrivado.ehPrivadoOuLocal(ip)) return "Roteador da rede"
            mapaIpParaProvedor[ip]?.let { return it }
        }
        return "DNS do Provedor"
    }

    private companion object {
        // GH#1212 item 1/7 — Cloudflare e Google migrados de DoH JSON pra RFC 8484 binário,
        // igual aos outros 5 provedores. Endpoint do Google muda de `dns.google/resolve`
        // (só JSON) pra `dns.google/dns-query` (suporta RFC 8484).
        val provedoresPublicos =
            listOf(
                DnsPublico("Cloudflare", "https://cloudflare-dns.com/dns-query"),
                DnsPublico("Google DNS", "https://dns.google/dns-query"),
                DnsPublico("Quad9", "https://dns.quad9.net/dns-query"),
                DnsPublico("OpenDNS", "https://doh.opendns.com/dns-query"),
                DnsPublico("AdGuard", "https://dns.adguard-dns.com/dns-query"),
                DnsPublico("Control D", "https://freedns.controld.com/p0"),
                DnsPublico("CleanBrowsing", "https://doh.cleanbrowsing.org/doh/security-filter/"),
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

/** GH#1212 item 6 — resultado de decodificar o cabeçalho RFC 1035 de uma resposta DoH binária. */
internal sealed class ValidacaoDns(val motivo: String) {
    data object Valida : ValidacaoDns("valida")
    data object Nxdomain : ValidacaoDns("nxdomain")
    data object Servfail : ValidacaoDns("servfail")
    data object Refused : ValidacaoDns("refused")
    data object SemResposta : ValidacaoDns("semRespostaTipo")
    data object Malformada : ValidacaoDns("respostaMalformada")
    data class OutroErro(val rcode: Int) : ValidacaoDns("rcode$rcode")
}

private sealed interface TentativaDns {
    data class Sucesso(val ms: Double) : TentativaDns
    data class RespostaInvalida(val motivo: String) : TentativaDns
    data object Falha : TentativaDns
}

private data class DnsPublico(
    val nome: String,
    val endpoint: String,
)
