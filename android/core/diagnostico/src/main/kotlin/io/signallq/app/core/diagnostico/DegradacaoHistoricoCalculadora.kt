package io.signallq.app.core.diagnostico

import kotlin.math.round

/**
 * Calcula o flag resumido de degradação (download 7d vs. 30d) consumido tanto
 * por `RecomendacaoPraticaEngine.recomendarUpgradeRoteadorRecorrente` (REC-14,
 * via `HistoricalDiagnosticInput.degradationDetected/degradationPercent`)
 * quanto pelo payload NDS (`historical.degradation_detected/_percent`,
 * ADR-018 seção 13, NDS-Snapshot-06 — issue #1838).
 *
 * Deliberadamente separado dos vários `DiagnosticResult` multi-métrica de
 * [HistoricalDegradationEngine] (que cobre download/upload/ping/dns com
 * severidade textual para exibição na tela de diagnóstico local) — este
 * cálculo alimenta um único flag booleano consumido programaticamente por
 * outro motor e por um contrato externo, não uma lista de achados para UI.
 *
 * Os limiares abaixo espelham deliberadamente o nível "attention" de
 * [HistoricalDegradationEngine] — mesma definição de "degradação com confiança
 * estatística mínima" em todo o app, para não ter dois critérios divergentes
 * do mesmo conceito. Duplicados aqui (não importados) porque são
 * `private const val` no arquivo de origem; se aqueles limiares mudarem,
 * revisar este arquivo também.
 */
object DegradacaoHistoricoCalculadora {
    private const val MIN_TESTS_7D = 5
    private const val MIN_TESTS_30D = 10
    private const val LIMIAR_PERCENTUAL = 20.0

    /**
     * Retorna `null` (degradação omitida, nunca um `false` inventado) quando
     * faltar a média de alguma janela, quando a média de 30d for `<= 0`
     * (divisão sem sentido) ou quando qualquer janela tiver menos testes que o
     * mínimo de confiança. Quando não-nulo, o primeiro valor do par é
     * `degradationDetected` e o segundo é `degradationPercent` (positivo = 7d
     * pior que 30d, isto é, queda; negativo = melhora).
     */
    fun calcular(
        avgDownload7d: Double?,
        avgDownload30d: Double?,
        testsCount7d: Int,
        testsCount30d: Int,
    ): Pair<Boolean, Double>? {
        if (avgDownload7d == null || avgDownload30d == null || avgDownload30d <= 0.0) return null
        if (testsCount7d < MIN_TESTS_7D || testsCount30d < MIN_TESTS_30D) return null

        val percentual = arredondar1CasaDecimal(((avgDownload30d - avgDownload7d) / avgDownload30d) * 100.0)
        val detectada = percentual >= LIMIAR_PERCENTUAL
        return detectada to percentual
    }

    private fun arredondar1CasaDecimal(valor: Double): Double = round(valor * 10.0) / 10.0
}
