package io.veloo.app.feature.dns

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

private const val TAG = "SignallQDnsBenchmark"

class BenchmarkDnsDoh : BenchmarkDns {
    private val executando = AtomicBoolean(false)
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
                Log.i(TAG, "iniciando benchmark DNS host=$hostConsulta resolvedores=$resolvedoresAtivos privateDns=$privateDnsHostname")
                publicar(EstadoBenchmarkDns.executando, 5, emptyList(), null)
                val host = URLEncoder.encode(hostConsulta, Charsets.UTF_8.name())

                val resultadoSistema = medirSistemaDns(hostConsulta, resolvedoresAtivos, privateDnsHostname)
                Log.i(TAG, "sistema dns: nome=${resultadoSistema.nomeProvedor} tempo=${resultadoSistema.tempoMs} grade=${resultadoSistema.gradeRapidez} amostras=${resultadoSistema.amostrasMs}")
                val acumulados = mutableListOf<ResultadoBenchmarkDns>()
                if (resultadoSistema.tempoMs != null) acumulados.add(resultadoSistema)
                publicar(EstadoBenchmarkDns.executando, 15, acumulados.sortedBy { it.tempoMs }, null)

                val provedoresPublicos = listOf(
                    "Cloudflare" to "https://cloudflare-dns.com/dns-query?name=$host&type=A",
                    "Google DNS" to "https://dns.google/resolve?name=$host&type=A",
                    "Quad9" to "https://dns.quad9.net:5053/dns-query?name=$host&type=A",
                    "OpenDNS" to "https://doh.opendns.com/resolve?name=$host&type=A",
                    "AdGuard" to "https://dns.adguard-dns.com/resolve?name=$host&type=A",
                    "Registro.br" to "https://dns.registro.br/query?name=$host&type=A",
                    "CETIC.br" to "https://resolver.cetic.br/dns-query?name=$host&type=A",
                )

                provedoresPublicos.forEachIndexed { idx, (nome, url) ->
                    val resultado = medirProvedor(nome, hostConsulta, url)
                    Log.i(TAG, "provedor $nome: tempo=${resultado.tempoMs} grade=${resultado.gradeRapidez} amostras=${resultado.amostrasMs} erro=${resultado.erroMensagem}")
                    if (resultado.tempoMs != null) acumulados.add(resultado)
                    val progresso = 20 + (((idx + 1).toDouble() / provedoresPublicos.size.toDouble()) * 75.0).toInt()
                    publicar(EstadoBenchmarkDns.executando, progresso, acumulados.sortedBy { it.tempoMs }, null)
                }

                val final = acumulados.sortedBy { it.tempoMs }
                Log.i(TAG, "benchmark concluido: ${final.size} provedores validos: ${final.map { "${it.nomeProvedor}=${it.tempoMs}ms(${it.gradeRapidez})" }}")
                publicar(EstadoBenchmarkDns.concluido, 100, final, null)
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
        return ResultadoBenchmarkDns(
            nomeProvedor = nome,
            hostConsulta = hostConsulta,
            tempoMs = tempo,
            amostrasMs = amostras,
            tentativas = 3,
            sucessos = amostras.size,
            taxaSucessoPercentual = if (amostras.isEmpty()) 0.0 else (amostras.size.toDouble() / 2.0) * 100.0,
            erroMensagem = if (tempo == null) "semResposta" else null,
            gradeRapidez = tempo?.let { calcularGrade(it) },
        )
    }

    // 3 rounds, descarta round 0 (warmup), P50 dos rounds 1-2.
    private fun medirProvedor(
        nomeProvedor: String,
        hostConsulta: String,
        endpoint: String,
    ): ResultadoBenchmarkDns {
        val amostras = mutableListOf<Double>()
        var ultimoErro: String? = null

        repeat(3) { round ->
            val tempo = medirTentativa(endpoint)
            if (round > 0) {
                if (tempo != null) amostras.add(tempo)
                else if (ultimoErro == null) ultimoErro = "semResposta"
            }
        }

        val tempo = calcularP50(amostras)
        return ResultadoBenchmarkDns(
            nomeProvedor = nomeProvedor,
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

    private fun medirTentativa(endpoint: String): Double? {
        val inicio = System.nanoTime()
        return try {
            val conn = (URL(endpoint).openConnection() as HttpURLConnection)
            conn.connectTimeout = 4000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            conn.setRequestProperty("accept", "application/dns-json, application/json")
            val code = conn.responseCode
            val elapsed = (System.nanoTime() - inicio) / 1_000_000.0
            if (code in 200..299) {
                conn.inputStream.use { it.readBytes() }
            } else {
                conn.errorStream?.use { it.readBytes() }
            }
            conn.disconnect()
            if (code in 200..299) elapsed else null
        } catch (_: Throwable) {
            null
        }
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
            mapaIpParaProvedor[ip]?.let { return it }
        }
        return "DNS do Provedor"
    }

    private companion object {
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
            "200.160.0.80" to "Registro.br",
            "200.160.2.3" to "Registro.br",
            "191.234.170.40" to "CETIC.br",
        )
        val mapaHostParaProvedor = mapOf(
            "one.one.one.one" to "Cloudflare",
            "dns.google" to "Google DNS",
            "dns.quad9.net" to "Quad9",
            "doh.opendns.com" to "OpenDNS",
            "adguard" to "AdGuard",
            "adguard-dns.com" to "AdGuard",
            "dns.registro.br" to "Registro.br",
            "resolver.cetic.br" to "CETIC.br",
        )
    }
}
