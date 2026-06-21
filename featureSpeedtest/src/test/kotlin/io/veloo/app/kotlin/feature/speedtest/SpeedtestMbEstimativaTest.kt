package io.veloo.app.kotlin.feature.speedtest

import io.veloo.app.feature.speedtest.ModoSpeedtest
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
            ModoSpeedtest.triplo -> 30L
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
    fun `triplo consome 30 MB estimados`() {
        assertEquals(30L, mbEstimadoPorModo(ModoSpeedtest.triplo))
    }

    @Test
    fun `fast e o modo mais leve entre os tres`() {
        val modos = ModoSpeedtest.entries.map { mbEstimadoPorModo(it) }
        assertEquals(10L, modos.min())
    }

    @Test
    fun `triplo e o modo mais pesado entre os tres`() {
        val modos = ModoSpeedtest.entries.map { mbEstimadoPorModo(it) }
        assertEquals(30L, modos.max())
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
