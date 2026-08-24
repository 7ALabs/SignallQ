package io.signallq.app.ui.screen

import io.signallq.app.core.database.MedicaoEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoricoComparacaoTest {
    @Test
    fun `comparacao sempre apresenta a medicao antiga antes da recente`() {
        val antiga = medicao("antiga", 1_000L)
        val recente = medicao("recente", 2_000L)

        assertEquals(antiga to recente, ordenarMedicoesParaComparacao(recente, antiga))
    }

    private fun medicao(
        id: String,
        timestamp: Long,
    ) =
        MedicaoEntity(
            id = id,
            timestampEpochMs = timestamp,
            connectionType = "wifi",
            connectionTypeStart = null,
            connectionTypeEnd = null,
            contaminado = false,
            speedtestMode = null,
            specVersion = null,
            downloadMbps = 50.0,
            uploadMbps = 20.0,
            latencyMs = 20.0,
            jitterMs = null,
            perdaPercentual = null,
            bufferbloatMs = null,
            packetLossSource = null,
            vereditoStreaming = null,
            vereditoGamer = null,
            vereditoVideoChamada = null,
            gargaloPrimario = null,
            networkId = "wifi-casa",
        )
}
