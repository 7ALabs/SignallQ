package io.signallq.app.feature.speedtest

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedtestQualityClassifierTest {

    @Test
    fun `bufferbloat thresholds are stable`() {
        assertEquals(SeveridadeBufferbloat.none, SpeedtestQualityClassifier.classificarBufferbloat(0.0))
        assertEquals(SeveridadeBufferbloat.none, SpeedtestQualityClassifier.classificarBufferbloat(4.9))
        assertEquals(SeveridadeBufferbloat.mild, SpeedtestQualityClassifier.classificarBufferbloat(5.0))
        assertEquals(SeveridadeBufferbloat.mild, SpeedtestQualityClassifier.classificarBufferbloat(30.0))
        assertEquals(SeveridadeBufferbloat.moderate, SpeedtestQualityClassifier.classificarBufferbloat(30.01))
        assertEquals(SeveridadeBufferbloat.moderate, SpeedtestQualityClassifier.classificarBufferbloat(100.0))
        assertEquals(SeveridadeBufferbloat.severe, SpeedtestQualityClassifier.classificarBufferbloat(100.01))
    }

    @Test
    fun `quality classifier returns expected veredicts and primary bottleneck`() {
        val d =
            SpeedtestQualityClassifier.classificarQualidade(
                dl = 30.0,
                ul = 10.0,
                latency = 40.0,
                jitter = 10.0,
                packetLoss = 0.1,
                bufferbloatDeltaMs = 0.0,
                bufferbloat = SeveridadeBufferbloat.none,
            )

        assertEquals(VereditoUso.good, d.vereditoStreaming)
        assertEquals(VereditoUso.good, d.vereditoGamer)
        assertEquals(VereditoUso.good, d.vereditoVideoChamada)
        assertEquals(GargaloPrimario.none, d.gargaloPrimario)

        val comPerda =
            SpeedtestQualityClassifier.classificarQualidade(
                dl = 100.0,
                ul = 50.0,
                latency = 20.0,
                jitter = 5.0,
                packetLoss = 3.0,
                bufferbloatDeltaMs = 0.0,
                bufferbloat = SeveridadeBufferbloat.none,
            )
        assertEquals(GargaloPrimario.packetLoss, comPerda.gargaloPrimario)
    }

    @Test
    fun `videoChamada good at 10Mbps not only at 25Mbps`() {
        val good = SpeedtestQualityClassifier.classificarQualidade(
            dl = 12.0,
            ul = 4.0,
            latency = 60.0,
            jitter = 20.0,
            packetLoss = 0.5,
            bufferbloatDeltaMs = 0.0,
            bufferbloat = SeveridadeBufferbloat.none,
        )
        assertEquals(VereditoUso.good, good.vereditoVideoChamada)

        val acceptable = SpeedtestQualityClassifier.classificarQualidade(
            dl = 26.0,
            ul = 2.0,
            latency = 60.0,
            jitter = 20.0,
            packetLoss = 0.5,
            bufferbloatDeltaMs = 0.0,
            bufferbloat = SeveridadeBufferbloat.none,
        )
        assertEquals(VereditoUso.acceptable, acceptable.vereditoVideoChamada)
    }
}

