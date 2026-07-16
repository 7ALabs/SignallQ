package io.signallq.app.feature.speedtest

import kotlin.math.abs

/**
 * Resultado do algoritmo puro de amostragem de ping: mediana de latência, jitter,
 * percentual de perda de pacote e contagem de amostras/timeouts.
 */
data class ResultadoAmostragemPing(
    val latenciaMs: Double,
    val jitterMs: Double,
    val perdaPercentual: Double,
    val totalAmostras: Int,
    val amostrasValidas: Int,
    val timeouts: Int,
)

/**
 * Algoritmo puro de amostragem de ping, extraído em GH#1019 por estar duplicado
 * literalmente entre [ExecutorSpeedtestCloudflare] (Tela 1 · Velocidade) e
 * [PingExecutor] (tela Ping + [io.signallq.app.jogos.JogoConexaoEngine]).
 *
 * Regras (preservadas exatamente como estavam nos dois consumidores originais):
 * - a 1ª amostra é sempre descartada (aquecimento de conexão);
 * - a latência final é a mediana das amostras válidas restantes;
 * - amostras acima de 3x a mediana são tratadas como outlier e descartadas — a menos
 *   que isso zere a lista, caso em que o filtro é ignorado;
 * - jitter é a média das deltas absolutas entre amostras consecutivas (após o filtro);
 * - perda é o percentual de timeouts (amostra nula) sobre o total pós-descarte da 1ª,
 *   com precisão total de [Double] (sem arredondamento — ver decisão de consolidação
 *   na issue #1019: o `PingExecutor` antigo arredondava para `Int` antes de devolver,
 *   divergência sem efeito observável dado o número de amostras usado hoje, mas
 *   removida para não haver duas fontes de verdade com precisão diferente).
 */
object AnalisadorAmostragemPing {
    fun analisar(amostrasBrutas: List<Double?>): ResultadoAmostragemPing {
        val semPrimeiro = amostrasBrutas.drop(1)
        val timeouts = semPrimeiro.count { it == null }
        val validos = semPrimeiro.filterNotNull()
        val mediana = mediana(validos)
        val filtrados = if (mediana > 0.0) validos.filter { it <= mediana * 3.0 } else validos
        val usados = if (filtrados.isNotEmpty()) filtrados else validos

        return ResultadoAmostragemPing(
            latenciaMs = mediana(usados),
            jitterMs = jitter(usados),
            perdaPercentual =
                if (semPrimeiro.isNotEmpty()) {
                    (timeouts.toDouble() / semPrimeiro.size.toDouble()) * 100.0
                } else {
                    0.0
                },
            totalAmostras = semPrimeiro.size,
            amostrasValidas = usados.size,
            timeouts = timeouts,
        )
    }

    private fun mediana(valores: List<Double>): Double {
        if (valores.isEmpty()) return 0.0
        val ordenadas = valores.sorted()
        val m = ordenadas.size / 2
        return if (ordenadas.size % 2 == 0) {
            (ordenadas[m - 1] + ordenadas[m]) / 2.0
        } else {
            ordenadas[m]
        }
    }

    private fun jitter(valores: List<Double>): Double {
        if (valores.size < 2) return 0.0
        val deltas = valores.zipWithNext { a, b -> abs(b - a) }
        return if (deltas.isEmpty()) 0.0 else deltas.average()
    }
}
