package io.signallq.app

import io.signallq.app.core.diagnostico.MetricClassifier
import io.signallq.app.core.diagnostico.MetricStatus
import io.signallq.app.feature.speedtest.SeveridadeBufferbloat
import io.signallq.app.feature.speedtest.SpeedtestQualityClassifier
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Fase 0 da auditoria #1228 — congela que as DUAS implementacoes independentes de
 * threshold de bufferbloat concordam hoje, valor a valor, em toda a fronteira testada.
 *
 * [MetricClassifier.classificarBufferbloat] (core/diagnostico) e
 * [SpeedtestQualityClassifier.classificarBufferbloat] (feature/speedtest) usam os MESMOS
 * 3 cortes (5/30/100ms), mas sao DUAS implementacoes fisicamente separadas -- o proprio
 * kdoc de [MetricClassifier] explica que a duplicacao existe porque `:core:diagnostico`
 * nao pode depender de `:featureSpeedtest` (lei de dependencia `:feature* -> :feature*`
 * proibida). So `:app` depende dos dois modulos ao mesmo tempo, entao so aqui e possivel
 * comparar as duas implementacoes lado a lado.
 *
 * O valor persistido em `MedicaoEntity.gargaloPrimario` vem da implementacao de
 * [SpeedtestQualityClassifier]; o badge de bufferbloat mostrado ao usuario (tela de
 * Resultado, Historico) vem da implementacao de [MetricClassifier]. Hoje elas concordam
 * -- mas nao ha nenhuma constante compartilhada nem versao registrada amarrando as duas.
 * Se qualquer uma mudar no futuro sem a outra, ESTE TESTE E QUEM VAI QUEBRAR PRIMEIRO --
 * ele existe para ser o alarme dessa dessincronizacao (ver
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
