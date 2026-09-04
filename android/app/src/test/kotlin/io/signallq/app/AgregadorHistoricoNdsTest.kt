package io.signallq.app

import io.signallq.app.core.database.MedicaoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Cobertura de [agregarHistoricoNds] — agregação pura das janelas 7d/30d do
 * payload NDS `historical` (ADR-018 seção 13, NDS-Snapshot-06 — issue #1838),
 * sem depender de Room.
 */
class AgregadorHistoricoNdsTest {
    private val agoraEpochMs = 1_700_000_000_000L
    private val umDiaMs = 24L * 60 * 60 * 1000

    private fun medicao(
        diasAtras: Int,
        downloadMbps: Double? = 100.0,
        uploadMbps: Double? = 50.0,
        latencyMs: Double? = 20.0,
    ) = MedicaoEntity(
        id = UUID.randomUUID().toString(),
        timestampEpochMs = agoraEpochMs - diasAtras * umDiaMs,
        connectionType = "wifi",
        connectionTypeStart = null,
        connectionTypeEnd = null,
        contaminado = false,
        speedtestMode = null,
        specVersion = null,
        downloadMbps = downloadMbps,
        uploadMbps = uploadMbps,
        latencyMs = latencyMs,
        jitterMs = null,
        perdaPercentual = null,
        bufferbloatMs = null,
        packetLossSource = null,
        vereditoStreaming = null,
        vereditoGamer = null,
        vereditoVideoChamada = null,
        gargaloPrimario = null,
    )

    @Test
    fun `lista vazia produz historico ausente -- usuario novo`() {
        val resultado = agregarHistoricoNds(medicoesUltimos30Dias = emptyList(), agoraEpochMs = agoraEpochMs)

        assertNull(resultado)
    }

    @Test
    fun `medicoes espalhadas entre 7d e 30d calculam medias e contagens de cada janela`() {
        // Janela de 7d usa ">=" no corte -- diasAtras 1..7 fica dentro, 8 em diante fica fora.
        val medicoes =
            (1..7).map { medicao(diasAtras = it, downloadMbps = 200.0, uploadMbps = 100.0, latencyMs = 15.0) } +
                (8..30).map { medicao(diasAtras = it, downloadMbps = 300.0, uploadMbps = 150.0, latencyMs = 10.0) }

        val resultado = agregarHistoricoNds(medicoesUltimos30Dias = medicoes, agoraEpochMs = agoraEpochMs)

        assertTrue(resultado != null)
        assertEquals(7, resultado?.testsCount7d)
        assertEquals(200.0, resultado?.avgDownload7d)
        assertEquals(100.0, resultado?.avgUpload7d)
        assertEquals(15.0, resultado?.avgPing7d)
        assertEquals(medicoes.size, resultado?.testsCount30d)
        // Media 30d combina as duas faixas -- nao e so a faixa "de fora".
        assertTrue("media 30d deve ficar entre 200 e 300", (resultado?.avgDownload30d ?: 0.0) in 200.0..300.0)
    }

    @Test
    fun `degradacao chega calculada quando ha testes suficientes nas duas janelas`() {
        val medicoes =
            (1..7).map { medicao(diasAtras = it, downloadMbps = 100.0) } +
                (8..30).map { medicao(diasAtras = it, downloadMbps = 300.0) }

        val resultado = agregarHistoricoNds(medicoesUltimos30Dias = medicoes, agoraEpochMs = agoraEpochMs)

        assertTrue(resultado != null)
        assertEquals(true, resultado?.degradationDetected)
        assertTrue((resultado?.degradationPercent ?: 0.0) > 0.0)
    }

    @Test
    fun `poucos testes em 7d calcula medias mas nao declara degradacao`() {
        val medicoes =
            (1..2).map { medicao(diasAtras = it, downloadMbps = 50.0) } +
                (8..30).map { medicao(diasAtras = it, downloadMbps = 300.0) }

        val resultado = agregarHistoricoNds(medicoesUltimos30Dias = medicoes, agoraEpochMs = agoraEpochMs)

        assertTrue(resultado != null)
        assertEquals(2, resultado?.testsCount7d)
        assertEquals(50.0, resultado?.avgDownload7d)
        assertNull("degradacao nao pode ser declarada com so 2 testes em 7d", resultado?.degradationDetected)
    }

    @Test
    fun `janela com testes suficientes mas poucos downloads validos nao declara degradacao`() {
        // 5 medições em 7d (>= MIN_TESTS_7D) mas só 1 com downloadMbps não-nulo --
        // testsCount7d "bruto" passaria no gate de confiança com média de amostra
        // única. O gate real deve olhar a contagem de downloads válidos, não o
        // total de medições da janela.
        val medicoes =
            listOf(medicao(diasAtras = 1, downloadMbps = 50.0)) +
                (2..5).map { medicao(diasAtras = it, downloadMbps = null) } +
                (8..30).map { medicao(diasAtras = it, downloadMbps = 300.0) }

        val resultado = agregarHistoricoNds(medicoesUltimos30Dias = medicoes, agoraEpochMs = agoraEpochMs)

        assertTrue(resultado != null)
        assertEquals(5, resultado?.testsCount7d)
        assertEquals(50.0, resultado?.avgDownload7d)
        assertNull(
            "so 1 download valido em 7d nao pode dar confianca estatistica pra degradacao",
            resultado?.degradationDetected,
        )
        assertNull(resultado?.degradationPercent)
    }

    @Test
    fun `so ha teste fora da janela de 7d -- 7d fica com contagem zero e media nula, 30d preenchido`() {
        val medicoes = (10..25).map { medicao(diasAtras = it, downloadMbps = 250.0) }

        val resultado = agregarHistoricoNds(medicoesUltimos30Dias = medicoes, agoraEpochMs = agoraEpochMs)

        assertTrue(resultado != null)
        assertEquals(0, resultado?.testsCount7d)
        assertNull("sem teste em 7d, media nao pode ser inventada", resultado?.avgDownload7d)
        assertEquals(medicoes.size, resultado?.testsCount30d)
        assertEquals(250.0, resultado?.avgDownload30d)
    }

    @Test
    fun `medicoes com metrica nula nao entram na media daquela metrica`() {
        val medicoes =
            (1..6).map { medicao(diasAtras = it, downloadMbps = 200.0, uploadMbps = null) }

        val resultado = agregarHistoricoNds(medicoesUltimos30Dias = medicoes, agoraEpochMs = agoraEpochMs)

        assertTrue(resultado != null)
        assertEquals(200.0, resultado?.avgDownload7d)
        assertNull("upload sempre nulo nao pode virar media 0", resultado?.avgUpload7d)
    }
}
