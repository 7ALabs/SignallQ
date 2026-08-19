package io.signallq.app.feature.speedtest

import io.signallq.app.core.diagnostico.MetricStatus

/**
 * Classificador extraido do ExecutorSpeedtestCloudflare para permitir reuso no diagnostico
 * sem duplicar thresholds. Nao alterar a logica aqui sem testes de regressao.
 */
object SpeedtestQualityClassifier {

    /**
     * GH#1228 Fatia 6 (P1-4): fonte unica dos 3 cortes de bufferbloat (5/30/100ms) e
     * [MetricClassifier.classificarBufferbloat][io.signallq.app.core.diagnostico.MetricClassifier.classificarBufferbloat]
     * (`core/diagnostico`) — aqui so traduz o vocabulario canonico [MetricStatus] para
     * [SeveridadeBufferbloat], vocabulario proprio deste modulo. Ate a fatia GH#1228, os
     * thresholds eram reimplementados aqui porque `core/diagnostico` nao podia depender de
     * `feature/speedtest`; a duplicacao foi resolvida na direcao permitida pela regra
     * `:feature* -> :core*` (`feature/speedtest` passou a depender de `core/diagnostico`, nunca
     * o contrario).
     *
     * NDS-02k (issue #1746/#1759): a chamada ao motor deixou de ser direta a `MetricClassifier`
     * e passou a ser via [classificarBufferbloatLocal] (`ClassificacaoMetricaLocal.kt`, mesmo
     * modulo) — seam que documenta por que este ponto continua 100% local (classificacao de
     * bufferbloat medido on-device, sem veredicto de avaliacao do NDS equivalente). Ver KDoc de
     * `ClassificacaoMetricaLocal.kt` para o racional completo.
     */
    fun classificarBufferbloat(deltaMs: Double): SeveridadeBufferbloat =
        when (classificarBufferbloatLocal(deltaMs)) {
            MetricStatus.excelente -> SeveridadeBufferbloat.none
            MetricStatus.bom -> SeveridadeBufferbloat.mild
            MetricStatus.regular -> SeveridadeBufferbloat.moderate
            MetricStatus.ruim, MetricStatus.critico, MetricStatus.inconclusivo -> SeveridadeBufferbloat.severe
        }

    fun classificarQualidade(
        dl: Double,
        ul: Double,
        latency: Double,
        jitter: Double,
        packetLoss: Double,
        bufferbloatDeltaMs: Double,
        bufferbloat: SeveridadeBufferbloat,
    ): DiagnosticoQualidadeSpeedtest {
        val streaming =
            when {
                dl >= 25.0 && latency <= 200.0 && jitter <= 50.0 && packetLoss <= 2.0 -> VereditoUso.good
                dl >= 15.0 && latency <= 500.0 && jitter <= 100.0 && packetLoss <= 5.0 -> VereditoUso.acceptable
                else -> VereditoUso.poor
            }
        val gamer =
            when {
                dl >= 10.0 && ul >= 3.0 && latency <= 50.0 && jitter <= 15.0 && packetLoss <= 0.5 -> VereditoUso.good
                dl >= 5.0 && ul >= 1.0 && latency <= 100.0 && jitter <= 30.0 && packetLoss <= 1.0 -> VereditoUso.acceptable
                else -> VereditoUso.poor
            }
        val videoChamada =
            when {
                dl >= 10.0 && ul >= 3.0 && latency <= 80.0 && jitter <= 30.0 && packetLoss <= 1.0 -> VereditoUso.good
                dl >= 5.0 && ul >= 1.0 && latency <= 150.0 && jitter <= 50.0 && packetLoss <= 3.0 -> VereditoUso.acceptable
                else -> VereditoUso.poor
            }
        val gargalo =
            when {
                packetLoss > 2.0 -> GargaloPrimario.packetLoss
                bufferbloatDeltaMs >= 100.0 || bufferbloat == SeveridadeBufferbloat.severe -> GargaloPrimario.bufferbloat
                latency > 100.0 -> GargaloPrimario.latency
                ul < 5.0 -> GargaloPrimario.upload
                else -> GargaloPrimario.none
            }
        return DiagnosticoQualidadeSpeedtest(
            vereditoStreaming = streaming,
            vereditoGamer = gamer,
            vereditoVideoChamada = videoChamada,
            gargaloPrimario = gargalo,
        )
    }
}

