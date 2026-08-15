package io.signallq.app.ui.screen

import io.signallq.app.core.database.MedicaoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

/**
 * GH#1265 — caracterização do bug: a Home mostrava "Resultado anterior · há 25 min" sem
 * download/upload porque o ping sintético do MonitoramentoWorker (fonte="monitor", roda a
 * cada 30 min) era mais recente que o último teste real de speedtest e vencia por timestamp
 * em `historico.maxByOrNull { it.timestampEpochMs }` — sem excluir `fonte == "monitor"`.
 */
class HomeMedicaoAdapterTest {
    private fun medicaoReal(
        timestampEpochMs: Long,
        downloadMbps: Double = 711.9,
    ) = MedicaoEntity(
        id = UUID.randomUUID().toString(),
        timestampEpochMs = timestampEpochMs,
        connectionType = "movel",
        connectionTypeStart = null,
        connectionTypeEnd = null,
        contaminado = false,
        speedtestMode = null,
        specVersion = null,
        downloadMbps = downloadMbps,
        uploadMbps = 45.0,
        latencyMs = 20.0,
        jitterMs = 5.0,
        perdaPercentual = 0.0,
        bufferbloatMs = null,
        packetLossSource = null,
        vereditoStreaming = null,
        vereditoGamer = null,
        vereditoVideoChamada = null,
        gargaloPrimario = null,
        fonte = null,
        status = "completed",
    )

    private fun pingMonitor(timestampEpochMs: Long) =
        MedicaoEntity(
            id = UUID.randomUUID().toString(),
            timestampEpochMs = timestampEpochMs,
            connectionType = "monitor",
            connectionTypeStart = null,
            connectionTypeEnd = null,
            contaminado = false,
            speedtestMode = null,
            specVersion = null,
            downloadMbps = null,
            uploadMbps = null,
            latencyMs = 150.0,
            jitterMs = null,
            perdaPercentual = null,
            bufferbloatMs = null,
            packetLossSource = null,
            vereditoStreaming = null,
            vereditoGamer = null,
            vereditoVideoChamada = null,
            gargaloPrimario = null,
            fonte = "monitor",
            status = "completed",
        )

    @Test
    fun `ping do monitor mais recente que teste real nao vence a escolha do resultado anterior`() {
        val testeReal = medicaoReal(timestampEpochMs = 1_000_000L)
        // Ping do monitor 25 min depois do teste real -- cenario exato do print do Luiz.
        val pingMaisRecente = pingMonitor(timestampEpochMs = 1_000_000L + 25 * 60_000L)

        val resolvido = listOf(testeReal, pingMaisRecente).resolverPrimeiraHistoria()

        assertNotNull("deveria escolher o teste real, nunca retornar nulo com os dois presentes", resolvido)
        assertEquals(testeReal.id, resolvido?.id)
        assertNotNull("resultado escolhido precisa ter download utilizavel", resolvido?.downloadMbps)
    }

    @Test
    fun `so ping de monitor no historico retorna nulo em vez de resultado vazio`() {
        val resolvido = listOf(pingMonitor(timestampEpochMs = 1_000L)).resolverPrimeiraHistoria()
        assertNull(resolvido)
    }

    @Test
    fun `sem ping de monitor comportamento permanece o mesmo -- mais recente por timestamp`() {
        val antigo = medicaoReal(timestampEpochMs = 1_000L, downloadMbps = 181.3)
        val recente = medicaoReal(timestampEpochMs = 2_000L, downloadMbps = 711.9)

        val resolvido = listOf(antigo, recente).resolverPrimeiraHistoria()

        assertEquals(recente.id, resolvido?.id)
        assertEquals(711.9, resolvido?.downloadMbps!!, 0.001)
    }

    @Test
    fun `paraMetricasMedicaoHome do teste real fica utilizavel`() {
        val metricas = medicaoReal(timestampEpochMs = 1_000L).paraMetricasMedicaoHome()
        assertEquals(true, metricas.utilizavel)
        assertNotNull(metricas.downloadMbps)
    }
}
