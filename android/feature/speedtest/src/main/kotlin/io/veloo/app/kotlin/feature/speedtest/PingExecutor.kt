package io.signallq.app.feature.speedtest

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * GH#1211 — motivo de uma amostra individual não ter retornado latência válida. Usado só
 * para diagnóstico agregado ([PingResultado.abortadoPorRede]) — o cálculo de perda em si
 * continua tratando qualquer amostra nula como uma coisa só (ver [AnalisadorAmostragemPing]).
 */
private enum class MotivoFalhaPing {
    SEM_REDE_OU_DNS,
    TIMEOUT,
    DESTINO_INDISPONIVEL,
}

/**
 * GH#1211 — esta ferramenta NUNCA mede ICMP real. Android proíbe socket ICMP bruto sem
 * privilégio elevado (CAP_NET_RAW não é concedido a apps); a alternativa de shell-exec do
 * `/system/bin/ping` do sistema é frágil entre OEMs/versões e não seria "a própria app"
 * medindo — decisão registrada: manter Estratégia A (latência HTTPS), nunca chamar isso de
 * ping ICMP na UI, e corrigir a semântica ao redor (perda, cancelamento, timeout, picos).
 *
 * [latenciaMs]/[jitterMs] vêm da mediana pós-filtro de outlier (ver [AnalisadorAmostragemPing]).
 * [maxMs]/[p95Ms]/[picos] preservam os picos que esse filtro descarta, pra análise de
 * estabilidade não escondê-los. [perdaPercentual] mede tentativas HTTPS sem resposta válida,
 * não perda de pacote ICMP comprovada — a UI é responsável por rotular isso corretamente.
 */
data class PingResultado(
    val latenciaMs: Double,
    val jitterMs: Double,
    val perdaPercentual: Double,
    val amostras: Int,
    val amostrasValidas: Int = 0,
    val timeouts: Int = 0,
    val maxMs: Double = 0.0,
    val p95Ms: Double = 0.0,
    val picos: Int = 0,
    val destino: String = "",
    val abortadoPorRede: Boolean = false,
    val execucaoParcial: Boolean = false,
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

        // GH#1211 item 4 — timeout global explícito pra execução inteira (antes só existia
        // timeout por amostra; pior caso de 20 tentativas x 4s podia passar de 1 minuto).
        const val TIMEOUT_GLOBAL_MS = 30_000L

        // GH#1211 item 6/7 — 3 falhas consecutivas de rede/DNS é sinal forte de rede caída,
        // não de instabilidade do destino; aborta cedo em vez de esgotar as 20 tentativas.
        const val FALHAS_REDE_CONSECUTIVAS_PARA_ABORTAR = 3

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
        var falhasRedeConsecutivas = 0
        var abortadoPorRede = false

        val concluiuDentroDoPrazo =
            withTimeoutOrNull(TIMEOUT_GLOBAL_MS) {
                var i = 0
                while (i < count) {
                    val (ms, motivo) = medirPingComMotivo()
                    bruto.add(ms)
                    onProgresso(i + 1)
                    i++

                    if (ms == null && motivo == MotivoFalhaPing.SEM_REDE_OU_DNS) {
                        falhasRedeConsecutivas++
                        if (falhasRedeConsecutivas >= FALHAS_REDE_CONSECUTIVAS_PARA_ABORTAR) {
                            abortadoPorRede = true
                            break
                        }
                    } else {
                        falhasRedeConsecutivas = 0
                    }
                }
            } != null

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
            amostras = bruto.size,
            amostrasValidas = resultado.amostrasValidas,
            timeouts = resultado.timeouts,
            maxMs = resultado.maxMs,
            p95Ms = resultado.p95Ms,
            picos = resultado.picos,
            destino = destinoLegivel(),
            abortadoPorRede = abortadoPorRede,
            execucaoParcial = abortadoPorRede || !concluiuDentroDoPrazo,
        )
    }

    private fun destinoLegivel(): String =
        runCatching { java.net.URI(targetUrl).host }.getOrNull() ?: targetUrl

    /**
     * GH#1211 item 5 — `CancellationException` nunca pode ser absorvida por este método:
     * fechar a tela ou cancelar o teste precisa propagar de verdade, não virar "mais uma
     * amostra falhou". Todo `catch` abaixo é de tipo concreto, nunca `Throwable`/`Exception`
     * genérico que capturaria cancelamento por engano.
     */
    private fun medirPingComMotivo(): Pair<Double?, MotivoFalhaPing?> {
        val cb = "${System.currentTimeMillis()}_${kotlin.random.Random.nextInt(10_000, 99_999)}"
        val separador = if (targetUrl.contains("?")) "&" else "?"
        val url = "$targetUrl${separador}_cb=$cb"
        val request = Request.Builder().url(url).get().build()
        val inicio = System.nanoTime()
        return try {
            val response = pingClient.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) return null to MotivoFalhaPing.DESTINO_INDISPONIVEL
                resp.body?.bytes()
                ((System.nanoTime() - inicio) / 1_000_000.0) to null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: UnknownHostException) {
            null to MotivoFalhaPing.SEM_REDE_OU_DNS
        } catch (_: SocketTimeoutException) {
            null to MotivoFalhaPing.TIMEOUT
        } catch (_: IOException) {
            null to MotivoFalhaPing.DESTINO_INDISPONIVEL
        }
    }
}
