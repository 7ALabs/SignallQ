package io.signallq.app.feature.history

import io.signallq.app.core.database.MedicaoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class VereditoResteteTest {
    private fun medicao(
        downloadMbps: Double?,
        networkId: String? = "wifi-bssid:aa:bb:cc:dd:ee:ff",
    ) = MedicaoEntity(
        id = UUID.randomUUID().toString(),
        timestampEpochMs = 0L,
        connectionType = "wifi",
        connectionTypeStart = null,
        connectionTypeEnd = null,
        contaminado = false,
        speedtestMode = "complete",
        specVersion = null,
        downloadMbps = downloadMbps,
        uploadMbps = null,
        latencyMs = null,
        jitterMs = null,
        perdaPercentual = null,
        bufferbloatMs = null,
        packetLossSource = null,
        vereditoStreaming = null,
        vereditoGamer = null,
        vereditoVideoChamada = null,
        gargaloPrimario = null,
        networkId = networkId,
    )

    @Test
    fun `download 20 por cento maior melhorou`() {
        val original = medicao(downloadMbps = 50.0)
        val novo = medicao(downloadMbps = 60.0)
        assertEquals(TendenciaEstado.MELHOROU, calcularVereditoReteste(original, novo))
    }

    @Test
    fun `download 20 por cento menor piorou`() {
        val original = medicao(downloadMbps = 50.0)
        val novo = medicao(downloadMbps = 40.0)
        assertEquals(TendenciaEstado.PIOROU, calcularVereditoReteste(original, novo))
    }

    @Test
    fun `download dentro de 10 por cento nao mudou`() {
        val original = medicao(downloadMbps = 50.0)
        val novo = medicao(downloadMbps = 52.0)
        assertEquals(TendenciaEstado.ESTAVEL, calcularVereditoReteste(original, novo))
    }

    @Test
    fun `sem original e inconclusiva`() {
        assertNull(calcularVereditoReteste(null, medicao(downloadMbps = 50.0)))
    }

    @Test
    fun `sem novo e inconclusiva`() {
        assertNull(calcularVereditoReteste(medicao(downloadMbps = 50.0), null))
    }

    @Test
    fun `download original nulo e inconclusiva`() {
        assertNull(calcularVereditoReteste(medicao(downloadMbps = null), medicao(downloadMbps = 50.0)))
    }

    @Test
    fun `download original zero e inconclusiva (evita divisao por zero)`() {
        assertNull(calcularVereditoReteste(medicao(downloadMbps = 0.0), medicao(downloadMbps = 50.0)))
    }

    @Test
    fun `rotulo de exibicao cobre os 4 casos`() {
        assertEquals("Melhorou", TendenciaEstado.MELHOROU.rotuloComparacaoReteste())
        assertEquals("Piorou", TendenciaEstado.PIOROU.rotuloComparacaoReteste())
        assertEquals("Não mudou", TendenciaEstado.ESTAVEL.rotuloComparacaoReteste())
        assertEquals("Comparação inconclusiva", null.rotuloComparacaoReteste())
    }

    @Test
    fun `vocabulario de telemetria cobre os 4 casos`() {
        assertEquals("melhorou", TendenciaEstado.MELHOROU.paraTelemetriaReteste())
        assertEquals("piorou", TendenciaEstado.PIOROU.paraTelemetriaReteste())
        assertEquals("nao_mudou", TendenciaEstado.ESTAVEL.paraTelemetriaReteste())
        assertEquals("inconclusiva", null.paraTelemetriaReteste())
    }
}
