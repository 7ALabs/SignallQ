package io.signallq.app.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** GH#1223 item 1/RF-02/RF-03 — nunca mistura métricas de execuções diferentes. */
class ResolvedorMedicaoHomeTest {

    private fun metricas(
        download: Double? = 100.0,
        upload: Double? = 50.0,
        latencia: Double? = 20.0,
        jitter: Double? = 5.0,
        perda: Double? = 0.0,
        timestamp: Long? = 1_000L,
        utilizavel: Boolean = true,
    ) = MetricasMedicaoHome(
        downloadMbps = download,
        uploadMbps = upload,
        latenciaMs = latencia,
        jitterMs = jitter,
        perdaPercentual = perda,
        timestampEpochMs = timestamp,
        connectionType = "wifi",
        ssid = "MinhaRede",
        vereditoGamer = "good",
        gargaloPrimario = "none",
        utilizavel = utilizavel,
    )

    @Test
    fun `atual utilizavel e escolhido inteiro, mesmo com anterior disponivel`() {
        val atual = metricas(download = 200.0, latencia = 10.0)
        val anterior = metricas(download = 50.0, latencia = 99.0)

        val resolvido = ResolvedorMedicaoHome.resolver(atual, anterior)

        assertEquals(OrigemMedicaoHome.ATUAL, resolvido?.origem)
        assertEquals(200.0, resolvido?.metricas?.downloadMbps)
        assertEquals(10.0, resolvido?.metricas?.latenciaMs)
    }

    @Test
    fun `atual nao utilizavel (parcial-contaminado) cai integralmente para anterior, nunca mistura`() {
        // Resultado atual parcial: tem numeros mas nao e utilizavel (ex.: contaminado por
        // troca de rede). Nao pode "emprestar" so a latencia do anterior -- usa o anterior
        // inteiro ou nada.
        val atual = metricas(download = 999.0, latencia = 1.0, utilizavel = false)
        val anterior = metricas(download = 80.0, latencia = 22.0, timestamp = 500L)

        val resolvido = ResolvedorMedicaoHome.resolver(atual, anterior)

        assertEquals(OrigemMedicaoHome.ANTERIOR, resolvido?.origem)
        assertEquals(80.0, resolvido?.metricas?.downloadMbps)
        assertEquals(22.0, resolvido?.metricas?.latenciaMs)
        assertEquals(500L, resolvido?.metricas?.timestampEpochMs)
    }

    @Test
    fun `atual nulo usa anterior utilizavel`() {
        val anterior = metricas()
        val resolvido = ResolvedorMedicaoHome.resolver(null, anterior)
        assertEquals(OrigemMedicaoHome.ANTERIOR, resolvido?.origem)
    }

    @Test
    fun `nem atual nem anterior utilizaveis retorna null`() {
        val atual = metricas(utilizavel = false)
        val anterior = metricas(utilizavel = false)
        assertNull(ResolvedorMedicaoHome.resolver(atual, anterior))
    }

    @Test
    fun `ambos nulos retorna null`() {
        assertNull(ResolvedorMedicaoHome.resolver(null, null))
    }
}
