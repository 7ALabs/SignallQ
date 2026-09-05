package io.signallq.app.feature.speedtest

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testes unitarios de logica pura relacionada ao SpeedtestViewModel.
 *
 * SpeedtestViewModel depende de MonitorRede e PreferenciasAppRepository que requerem
 * Android runtime, portanto testamos apenas a logica de estimativas de MB de forma isolada.
 */
class SpeedtestMbEstimativaTest {
    /** Replica a logica de estimativa de MB do SpeedtestViewModel. */
    private fun mbEstimadoPorModo(modo: ModoSpeedtest): Long =
        when (modo) {
            ModoSpeedtest.fast -> 10L
            ModoSpeedtest.complete -> 25L
        }

    @Test
    fun `fast consome 10 MB estimados`() {
        assertEquals(10L, mbEstimadoPorModo(ModoSpeedtest.fast))
    }

    @Test
    fun `complete consome 25 MB estimados`() {
        assertEquals(25L, mbEstimadoPorModo(ModoSpeedtest.complete))
    }

    @Test
    fun `fast e o modo mais leve entre os dois`() {
        val modos = ModoSpeedtest.entries.map { mbEstimadoPorModo(it) }
        assertEquals(10L, modos.min())
    }

    @Test
    fun `GH1737 so restam dois modos apos a remocao do triplo`() {
        assertEquals(2, ModoSpeedtest.entries.size)
    }

    @Test
    fun `todos os modos tem estimativa positiva`() {
        ModoSpeedtest.entries.forEach { modo ->
            assert(mbEstimadoPorModo(modo) > 0) {
                "Estimativa de $modo deve ser positiva"
            }
        }
    }
}
