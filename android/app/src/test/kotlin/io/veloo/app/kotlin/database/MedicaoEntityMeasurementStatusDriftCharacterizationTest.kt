package io.signallq.app.database

import io.signallq.app.feature.speedtest.MeasurementStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fase 0 da auditoria #1228 — congela que `MedicaoEntity.status` (String persistida no
 * Room, `core/database`) NAO tem correspondencia 1:1 com [MeasurementStatus] (enum de
 * `feature/speedtest`, GH#1221/#1225), apesar de os dois nominalmente cobrirem o mesmo
 * conceito ("integridade de uma medicao"). Ver
 * `docs_ai/ARQUITETURA/AUDITORIA_1228_FASE0_INVENTARIO_COMPLETO.md`, Parte 3.4 e Parte 8
 * (P2-3).
 *
 * Os valores possiveis de `MedicaoEntity.status` estao documentados apenas no kdoc do
 * campo (nao ha enum nem conversor no codigo de producao): "completed", "failed",
 * "partial", "timeout", "contaminated", "inconclusive". Este teste os reproduz aqui,
 * porque nao existe nenhum tipo compartilhado para importar -- e exatamente essa
 * ausencia de contrato compartilhado o achado desta caracterizacao.
 *
 * `core/database` nao pode depender de `feature/speedtest` (nem deveria, por ser um
 * modulo core) -- entao este teste vive em `:app`, o unico modulo que depende dos dois.
 */
class MedicaoEntityMeasurementStatusDriftCharacterizationTest {
    /** Valores documentados no kdoc de `MedicaoEntity.status` (core/database),
     *  reproduzidos aqui porque nao ha enum de producao para importar. */
    private val valoresDocumentadosDeMedicaoEntityStatus =
        setOf(
            "completed",
            "failed",
            "partial",
            "timeout",
            "contaminated",
            "inconclusive",
        )

    @Test
    fun `nem todo valor documentado de MedicaoEntity-status tem MeasurementStatus correspondente`() {
        val nomesDoEnum = MeasurementStatus.values().map { it.name.lowercase() }.toSet()

        // "failed" e "timeout" sao valores reais e documentados de MedicaoEntity.status
        // (core/database) sem NENHUM caso equivalente em MeasurementStatus (feature/speedtest).
        assertFalse("failed" in nomesDoEnum)
        assertFalse("timeout" in nomesDoEnum)
    }

    @Test
    fun `MeasurementStatus-CANCELLED nao tem nenhum valor string correspondente documentado`() {
        // MeasurementStatus.CANCELLED existe no enum (mantido para consumidores que
        // modelam o ciclo de vida inteiro, ver kdoc do proprio enum), mas nao consta
        // entre os valores documentados de MedicaoEntity.status -- nunca chega a ser
        // persistido com esse nome.
        assertFalse("cancelled" in valoresDocumentadosDeMedicaoEntityStatus)
        assertTrue(MeasurementStatus.values().any { it == MeasurementStatus.CANCELLED })
    }

    @Test
    fun `apenas 3 dos 6 valores documentados de MedicaoEntity-status batem com o enum`() {
        val nomesDoEnum = MeasurementStatus.values().map { it.name.lowercase() }.toSet()
        val interseccao = valoresDocumentadosDeMedicaoEntityStatus.intersect(nomesDoEnum)

        // "completed"->COMPLETE nao bate nem por nome (singular vs particípio) -- so
        // "partial", "inconclusive" e "contaminated" tem correspondencia direta de
        // nome com o enum. "completed" precisaria de um mapeamento manual explicito
        // para virar MeasurementStatus.COMPLETE, que nao existe hoje no codigo de
        // producao (confirmado por busca no repositorio).
        assertTrue(interseccao == setOf("partial", "inconclusive", "contaminated"))
    }
}
