package io.signallq.app.feature.speedtest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class PingResultado(
    val latenciaMs: Double,
    val jitterMs: Double,
    val perdaPercentual: Double,
    val amostras: Int,
)

/**
 * @param targetUrl Base do endpoint usado como sonda de latencia. Default preserva
 * o comportamento historico (CDN Cloudflare) usado pela tela Ping. GH#935 reaproveita
 * esta classe passando a URL do `game-latency-probe-worker` (sonda regional dedicada,
 * sem logica de jogo) em vez de duplicar a logica de amostragem/jitter/perda.
 */
class PingExecutor(
    private val targetUrl: String = "https://speed.cloudflare.com/__down?bytes=0",
) {
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
        val bruto = mutableListOf<Double?>()

        repeat(count) { i ->
            bruto.add(medirPing())
            onProgresso(i + 1)
        }

        // Algoritmo de mediana/outlier/jitter/perda extraído para AnalisadorAmostragemPing
        // (GH#1019) — reusado também por ExecutorSpeedtestCloudflare. Decisão de
        // consolidação: perdaPercentual passa a manter precisão total de Double (antes
        // era arredondado para Int aqui); sem efeito observável hoje porque os thresholds
        // de classificação (PerfilThresholds) são mais grossos que a granularidade de
        // uma única amostra, e a tela Ping já formata a exibição com "%.0f%%".
        val resultado = AnalisadorAmostragemPing.analisar(bruto)

        PingResultado(
            latenciaMs = resultado.latenciaMs,
            jitterMs = resultado.jitterMs,
            perdaPercentual = resultado.perdaPercentual,
            amostras = count,
        )
    }

    private fun medirPing(): Double? {
        val cb = "${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(10_000, 99_999)}"
        val separador = if (targetUrl.contains("?")) "&" else "?"
        val url = "$targetUrl${separador}_cb=$cb"
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
}
