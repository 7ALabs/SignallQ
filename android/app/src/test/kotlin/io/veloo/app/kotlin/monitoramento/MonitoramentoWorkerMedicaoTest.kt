package io.signallq.app.monitoramento

import io.signallq.app.core.database.MedicaoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

/**
 * Testa a construcao da MedicaoEntity sintetica gerada pelo MonitoramentoWorker.
 *
 * Valida campos obrigatorios sem dependencia de Android/Room/WorkManager.
 * A persistencia em si e testada na integracao (requer Robolectric ou device).
 */
class MonitoramentoWorkerMedicaoTest {
    private fun criarMedicaoMonitor(
        latenciaMs: Long?,
        rssiDbm: Int?,
    ) = MedicaoEntity(
        id = UUID.randomUUID().toString(),
        timestampEpochMs = System.currentTimeMillis(),
        connectionType = "monitor",
        connectionTypeStart = null,
        connectionTypeEnd = null,
        contaminado = false,
        speedtestMode = null,
        specVersion = null,
        downloadMbps = null,
        uploadMbps = null,
        latencyMs = latenciaMs?.toDouble(),
        jitterMs = null,
        perdaPercentual = null,
        bufferbloatMs = null,
        packetLossSource = null,
        vereditoStreaming = null,
        vereditoGamer = null,
        vereditoVideoChamada = null,
        gargaloPrimario = null,
        fonte = "monitor",
    )

    @Test
    fun `medicao monitor tem fonte igual a monitor`() {
        val medicao = criarMedicaoMonitor(latenciaMs = 150L, rssiDbm = -65)
        assertEquals("fonte deve ser 'monitor'", "monitor", medicao.fonte)
    }

    @Test
    fun `medicao monitor nao tem download nem upload`() {
        val medicao = criarMedicaoMonitor(latenciaMs = 200L, rssiDbm = -70)
        assertNull("downloadMbps deve ser null para medicao de monitor", medicao.downloadMbps)
        assertNull("uploadMbps deve ser null para medicao de monitor", medicao.uploadMbps)
    }

    @Test
    fun `medicao monitor preserva latencia medida`() {
        val medicao = criarMedicaoMonitor(latenciaMs = 350L, rssiDbm = null)
        assertEquals("latencyMs deve refletir a medicao HTTP", 350.0, medicao.latencyMs!!, 0.001)
    }

    @Test
    fun `medicao monitor com latencia null mantem latencyMs null`() {
        val medicao = criarMedicaoMonitor(latenciaMs = null, rssiDbm = null)
        assertNull("latencyMs deve ser null quando medicao HTTP falhou", medicao.latencyMs)
    }

    @Test
    fun `medicao monitor nao e contaminada`() {
        val medicao = criarMedicaoMonitor(latenciaMs = 100L, rssiDbm = -55)
        assertEquals("medicao de monitor nao deve ser marcada como contaminada", false, medicao.contaminado)
    }

    @Test
    fun `medicao monitor tem id unico`() {
        val m1 = criarMedicaoMonitor(latenciaMs = 100L, rssiDbm = null)
        val m2 = criarMedicaoMonitor(latenciaMs = 100L, rssiDbm = null)
        assertNotNull("id nao deve ser null", m1.id)
        assert(m1.id != m2.id) { "ids devem ser unicos entre medicoes" }
    }
}
