package io.veloo.app.feature.speedtest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import kotlin.math.abs
import java.util.concurrent.TimeUnit

data class PingResultado(
    val latenciaMs: Double,
    val jitterMs: Double,
    val perdaPercentual: Double,
    val amostras: Int,
)

class PingExecutor {
    private companion object {
        const val UA = "Mozilla/5.0 (Linux; Android 14; SM-A256E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

        val pingClient: OkHttpClient = OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .callTimeout(4, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", UA)
                        .header("Cache-Control", "no-store")
                        .build(),
                )
            }
            .build()
    }

    suspend fun executar(
        count: Int = 20,
        onProgresso: (Int) -> Unit = {},
    ): PingResultado = withContext(Dispatchers.IO) {
        val amostras = mutableListOf<Double>()
        val bruto = mutableListOf<Double?>()

        repeat(count) { i ->
            val rtt = medirPing()
            bruto.add(rtt)
            if (rtt != null) {
                amostras.add(rtt)
            }
            onProgresso(i + 1)
        }

        val semPrimeiro = bruto.drop(1)
        val timeouts = semPrimeiro.count { it == null }
        val validos = semPrimeiro.filterNotNull()
        val mediana = calcularMediana(validos)
        val filtrados = if (mediana > 0.0) validos.filter { it <= mediana * 3.0 } else validos
        val usados = if (filtrados.isNotEmpty()) filtrados else validos

        val latenciaMs = calcularMediana(usados)
        val jitterMs = calcularJitter(usados)
        val perdaPercentual = if (semPrimeiro.isNotEmpty()) {
            ((timeouts.toDouble() / semPrimeiro.size.toDouble()) * 100.0).toInt()
        } else {
            0
        }

        PingResultado(
            latenciaMs = latenciaMs,
            jitterMs = jitterMs,
            perdaPercentual = perdaPercentual.toDouble(),
            amostras = count,
        )
    }

    private fun medirPing(): Double? {
        val cb = "${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(10_000, 99_999)}"
        val url = "https://speed.cloudflare.com/__down?bytes=0&_cb=$cb"
        val request = Request.Builder().url(url).get().build()
        val inicio = System.nanoTime()
        return try {
            val response = pingClient.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.bytes()
                (System.nanoTime() - inicio) / 1_000_000.0
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun calcularMediana(valores: List<Double>): Double {
        if (valores.isEmpty()) return 0.0
        val ordenadas = valores.sorted()
        val m = ordenadas.size / 2
        return if (ordenadas.size % 2 == 0) {
            (ordenadas[m - 1] + ordenadas[m]) / 2.0
        } else {
            ordenadas[m]
        }
    }

    private fun calcularJitter(valores: List<Double>): Double {
        if (valores.size < 2) return 0.0
        val deltas = valores.zipWithNext { a, b -> abs(b - a) }
        return if (deltas.isEmpty()) 0.0 else deltas.average()
    }
}
