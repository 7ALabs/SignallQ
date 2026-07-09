package io.signallq.app.ui.screen

import io.signallq.app.ui.HistoryPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regressão GH#515 — "5G NSA"/"5G SA" (jargão de operadora) não deve aparecer em texto
 * voltado ao usuário leigo fora de telas de detalhe técnico.
 */
class HomeScreenHelpersTest {
    @Test
    fun `5G NSA simplifica para 5G`() {
        assertEquals("5G", tecnologiaSimplificada("5G NSA"))
    }

    @Test
    fun `5G SA simplifica para 5G`() {
        assertEquals("5G", tecnologiaSimplificada("5G SA"))
    }

    @Test
    fun `4G sem sufixo permanece 4G`() {
        assertEquals("4G", tecnologiaSimplificada("4G"))
    }

    @Test
    fun `nulo ou vazio retorna null`() {
        assertNull(tecnologiaSimplificada(null))
        assertNull(tecnologiaSimplificada(""))
    }
}

/**
 * Regressão GH#827 — card "Medições" não deve reservar altura de gráfico quando não há
 * pelo menos 2 pontos com dado válido pra traçar uma linha real.
 */
class HasRenderableChartDataTest {
    @Test
    fun `historico vazio nao tem grafico renderizavel`() {
        assertFalse(hasRenderableChartData(emptyList()))
    }

    @Test
    fun `um unico ponto valido nao tem grafico renderizavel`() {
        val history = listOf(HistoryPoint(timestampEpochMs = 1L, downloadMbps = 50.0, uploadMbps = 20.0))
        assertFalse(hasRenderableChartData(history))
    }

    @Test
    fun `dois pontos validos tem grafico renderizavel`() {
        val history =
            listOf(
                HistoryPoint(timestampEpochMs = 1L, downloadMbps = 50.0, uploadMbps = 20.0),
                HistoryPoint(timestampEpochMs = 2L, downloadMbps = 60.0, uploadMbps = 25.0),
            )
        assertTrue(hasRenderableChartData(history))
    }

    @Test
    fun `pontos sem download nem upload nao contam como validos`() {
        val history =
            listOf(
                HistoryPoint(timestampEpochMs = 1L, downloadMbps = 50.0, uploadMbps = 20.0),
                HistoryPoint(timestampEpochMs = 2L, downloadMbps = null, uploadMbps = null),
            )
        assertFalse(hasRenderableChartData(history))
    }

    @Test
    fun `ponto valido com apenas upload conta`() {
        val history =
            listOf(
                HistoryPoint(timestampEpochMs = 1L, downloadMbps = null, uploadMbps = 20.0),
                HistoryPoint(timestampEpochMs = 2L, downloadMbps = 30.0, uploadMbps = null),
            )
        assertTrue(hasRenderableChartData(history))
    }
}
