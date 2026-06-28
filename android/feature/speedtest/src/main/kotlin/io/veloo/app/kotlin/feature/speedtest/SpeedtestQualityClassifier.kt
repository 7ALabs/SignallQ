package io.signallq.app.feature.speedtest

/**
 * Classificador extraido do ExecutorSpeedtestCloudflare para permitir reuso no diagnostico
 * sem duplicar thresholds. Nao alterar a logica aqui sem testes de regressao.
 */
object SpeedtestQualityClassifier {

    fun classificarBufferbloat(deltaMs: Double): SeveridadeBufferbloat {
        if (deltaMs < 5.0) return SeveridadeBufferbloat.none
        if (deltaMs <= 30.0) return SeveridadeBufferbloat.mild
        if (deltaMs <= 100.0) return SeveridadeBufferbloat.moderate
        return SeveridadeBufferbloat.severe
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

