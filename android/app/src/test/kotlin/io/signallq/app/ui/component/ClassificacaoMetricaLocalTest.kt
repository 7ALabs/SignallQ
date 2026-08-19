package io.signallq.app.ui.component

import io.signallq.app.core.diagnostico.BandaWifi
import io.signallq.app.core.diagnostico.MetricClassifier
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Teste de caracterização — issue #1749 (NDS-02b) e #1752 (NDS-02d), escrito ANTES de qualquer
 * call site trocar `MetricClassifier.classificarX()` direto pelas funções deste arquivo. Prova que
 * `classificarRssiWifiLocal`/`classificarRsrpLocal`/`classificarRsrqLocal`/`classificarSinrLocal`/
 * `classificarBufferbloatLocal`/`classificarDownloadLocal`/`classificarUploadLocal`/
 * `classificarPerdaPacotesLocal`/`classificarLatenciaLocal`/`classificarJitterLocal` produzem
 * exatamente o mesmo resultado que `MetricClassifier` produzia antes da migração — comportamento
 * visual idêntico, só a fonte da chamada muda de lugar (ver KDoc de `ClassificacaoMetricaLocal.kt`
 * para o racional completo).
 */
class ClassificacaoMetricaLocalTest {
    @Test
    fun `classificarRssiWifiLocal converge com MetricClassifier em todas as bandas e fronteiras`() {
        val fronteiras = listOf(-49, -50, -51, -59, -60, -61, -69, -70, -71, -79, -80, -81, -90)
        val bandas = listOf(BandaWifi.ghz24, BandaWifi.ghz5, BandaWifi.desconhecida)
        bandas.forEach { banda ->
            fronteiras.forEach { rssi ->
                assertEquals(
                    "divergencia em rssi=$rssi banda=$banda",
                    MetricClassifier.classificarRssiWifi(rssi, banda),
                    classificarRssiWifiLocal(rssi, banda),
                )
            }
        }
    }

    @Test
    fun `classificarRsrpLocal converge com MetricClassifier em 4G e 5G`() {
        val fronteiras = listOf(-79, -80, -81, -89, -90, -91, -94, -95, -96, -99, -100, -101, -109, -110, -111)
        MetricClassifier.RadioTech.entries.forEach { tech ->
            fronteiras.forEach { rsrp ->
                assertEquals(
                    "divergencia em rsrp=$rsrp tech=$tech",
                    MetricClassifier.classificarRsrp(rsrp, tech),
                    classificarRsrpLocal(rsrp, tech),
                )
            }
        }
    }

    @Test
    fun `classificarRsrqLocal converge com MetricClassifier`() {
        val fronteiras = listOf(-9, -10, -11, -14, -15, -16, -19, -20, -21)
        MetricClassifier.RadioTech.entries.forEach { tech ->
            fronteiras.forEach { rsrq ->
                assertEquals(
                    "divergencia em rsrq=$rsrq tech=$tech",
                    MetricClassifier.classificarRsrq(rsrq, tech),
                    classificarRsrqLocal(rsrq, tech),
                )
            }
        }
    }

    @Test
    fun `classificarSinrLocal converge com MetricClassifier em 4G e 5G`() {
        val fronteiras = listOf(21, 20, 19, 14, 13, 12, 11, 10, 9, 1, 0, -1)
        MetricClassifier.RadioTech.entries.forEach { tech ->
            fronteiras.forEach { sinr ->
                assertEquals(
                    "divergencia em sinr=$sinr tech=$tech",
                    MetricClassifier.classificarSinr(sinr, tech),
                    classificarSinrLocal(sinr, tech),
                )
            }
        }
    }

    @Test
    fun `classificarBufferbloatLocal converge com MetricClassifier em todas as fronteiras conhecidas`() {
        val fronteiras = listOf(0.0, 4.9, 5.0, 5.1, 29.9, 30.0, 30.1, 99.9, 100.0, 100.1, 500.0)
        fronteiras.forEach { deltaMs ->
            assertEquals(
                "divergencia em deltaMs=$deltaMs",
                MetricClassifier.classificarBufferbloat(deltaMs),
                classificarBufferbloatLocal(deltaMs),
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NDS-02d (#1752) — os 5 pontos de ResultadoVelocidadeScreen.kt (download/upload/perda/
    // latência/jitter). Fronteiras espelham exatamente os cortes de MetricClassifier.kt.
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun `classificarDownloadLocal converge com MetricClassifier em todas as fronteiras conhecidas`() {
        val fronteiras = listOf(9.9, 10.0, 10.1, 24.9, 25.0, 25.1, 49.9, 50.0, 50.1, 99.9, 100.0, 100.1)
        fronteiras.forEach { mbps ->
            assertEquals(
                "divergencia em mbps=$mbps",
                MetricClassifier.classificarDownload(mbps),
                classificarDownloadLocal(mbps),
            )
        }
    }

    @Test
    fun `classificarUploadLocal converge com MetricClassifier em todas as fronteiras conhecidas`() {
        val fronteiras = listOf(0.9, 1.0, 1.1, 2.9, 3.0, 3.1, 9.9, 10.0, 10.1, 19.9, 20.0, 20.1)
        fronteiras.forEach { mbps ->
            assertEquals(
                "divergencia em mbps=$mbps",
                MetricClassifier.classificarUpload(mbps),
                classificarUploadLocal(mbps),
            )
        }
    }

    @Test
    fun `classificarPerdaPacotesLocal converge com MetricClassifier em todas as fronteiras conhecidas`() {
        val fronteiras = listOf(0.0, 0.1, 0.4, 0.5, 0.6, 1.9, 2.0, 2.1, 5.0)
        fronteiras.forEach { perdaPercentual ->
            assertEquals(
                "divergencia em perdaPercentual=$perdaPercentual",
                MetricClassifier.classificarPerdaPacotes(perdaPercentual),
                classificarPerdaPacotesLocal(perdaPercentual),
            )
        }
    }

    @Test
    fun `classificarLatenciaLocal converge com MetricClassifier em todas as fronteiras conhecidas`() {
        val fronteiras = listOf(99.9, 100.0, 100.1, 149.9, 150.0, 150.1, 199.9, 200.0, 200.1, 500.0)
        fronteiras.forEach { latenciaMs ->
            assertEquals(
                "divergencia em latenciaMs=$latenciaMs",
                MetricClassifier.classificarLatencia(latenciaMs),
                classificarLatenciaLocal(latenciaMs),
            )
        }
    }

    @Test
    fun `classificarJitterLocal converge com MetricClassifier em todas as fronteiras conhecidas`() {
        val fronteiras = listOf(4.9, 5.0, 5.1, 9.9, 10.0, 10.1, 19.9, 20.0, 20.1, 50.0)
        fronteiras.forEach { jitterMs ->
            assertEquals(
                "divergencia em jitterMs=$jitterMs",
                MetricClassifier.classificarJitter(jitterMs),
                classificarJitterLocal(jitterMs),
            )
        }
    }
}
