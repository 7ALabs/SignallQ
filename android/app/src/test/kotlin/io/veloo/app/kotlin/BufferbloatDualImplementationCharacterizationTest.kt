package io.signallq.app

import io.signallq.app.core.diagnostico.MetricClassifier
import io.signallq.app.core.diagnostico.MetricStatus
import io.signallq.app.feature.speedtest.SeveridadeBufferbloat
import io.signallq.app.feature.speedtest.SpeedtestQualityClassifier
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Fase 0 da auditoria #1228 — nasceu para congelar que as DUAS implementacoes
 * independentes de threshold de bufferbloat concordavam, valor a valor, em toda a
 * fronteira testada. **RESOLVIDO na GH#1228 Fatia 6 (P1-4, 2026-07-31):**
 * [SpeedtestQualityClassifier.classificarBufferbloat] (feature/speedtest) passou a
 * delegar para [MetricClassifier.classificarBufferbloat] (core/diagnostico) --
 * `feature/speedtest` ganhou dependencia em `core:diagnostico` (`:feature* -> :core*`,
 * direcao permitida), entao a concordancia deixou de ser coincidencia de valores e virou
 * garantia estrutural (mesma fonte).
 *
 * Este teste continua valendo como guard-rail: se a delegacao for revertida por engano
 * (ex.: alguem reintroduzir um `when` literal em `SpeedtestQualityClassifier` em vez de
 * chamar `MetricClassifier`), ele volta a ser o alarme de dessincronizacao. O valor
 * persistido em `MedicaoEntity.gargaloPrimario` vem de [SpeedtestQualityClassifier]; o
 * badge de bufferbloat mostrado ao usuario (tela de Resultado, Historico) vem de
 * [MetricClassifier] -- ambos agora leem o mesmo corte numerico (ver
 * `docs_ai/ARQUITETURA/AUDITORIA_1228_FASE0_INVENTARIO_COMPLETO.md`, Parte 8, P1-4).
 */
class BufferbloatDualImplementationCharacterizationTest {
    private fun statusEquivalente(deltaMs: Double): Boolean {
        val doMetricClassifier = MetricClassifier.classificarBufferbloat(deltaMs)
        val doSpeedtestQualityClassifier = SpeedtestQualityClassifier.classificarBufferbloat(deltaMs)
        return when (doSpeedtestQualityClassifier) {
            SeveridadeBufferbloat.none -> doMetricClassifier == MetricStatus.excelente
            SeveridadeBufferbloat.mild -> doMetricClassifier == MetricStatus.bom
            SeveridadeBufferbloat.moderate -> doMetricClassifier == MetricStatus.regular
            SeveridadeBufferbloat.severe -> doMetricClassifier == MetricStatus.ruim
        }
    }

    @Test
    fun `bufferbloat concorda entre as duas implementacoes em todas as fronteiras conhecidas`() {
        val fronteiras = listOf(0.0, 4.9, 5.0, 5.1, 29.9, 30.0, 30.1, 99.9, 100.0, 100.1, 500.0)
        fronteiras.forEach { deltaMs ->
            assertEquals(
                "Divergencia em deltaMs=$deltaMs -- MetricClassifier e SpeedtestQualityClassifier " +
                    "pararam de concordar; ver P1-4 da auditoria #1228",
                true,
                statusEquivalente(deltaMs),
            )
        }
    }

    @Test
    fun `severo do SpeedtestQualityClassifier equivale a ruim do MetricClassifier em 150ms`() {
        assertEquals(SeveridadeBufferbloat.severe, SpeedtestQualityClassifier.classificarBufferbloat(150.0))
        assertEquals(MetricStatus.ruim, MetricClassifier.classificarBufferbloat(150.0))
    }
}
