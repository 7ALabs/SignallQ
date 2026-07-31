package io.signallq.app.core.diagnostico

import io.signallq.app.core.network.contracts.fibra.ClassificadorSaudeGpon
import io.signallq.app.core.network.contracts.fibra.GponSaudeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fase 0 da auditoria #1228 — achado novo desta auditoria (nao documentado antes em
 * nenhum ADR/issue): o sentinela `0.0` usado por `NokiaModemParser` (feature/fibra) para
 * "leitura ausente/nao reportada" tem DOIS comportamentos incompativeis dependendo de
 * qual codigo o le.
 *
 * [ClassificadorSaudeGpon.classificarRx] trata `0.0` explicitamente como o PIOR CASO
 * (`GponSaudeStatus.ruim`) -- ausencia de leitura e tratada como "assuma o pior".
 *
 * [FibraSignalQualityEngine.avaliar] faz `if (rx != null && rx != 0.0)` antes de chamar
 * o classificador -- ou seja, quando `rx == 0.0`, o engine PULA o achado inteiramente,
 * como se o dado nunca tivesse sido coletado.
 *
 * Resultado: o mesmo sentinela de "sem dado" vira "ruim" (crítico, com recomendacao) num
 * caminho e "nenhum achado" (silencio) no outro -- cenario 16 da Parte 5 do documento
 * `docs_ai/ARQUITETURA/AUDITORIA_1228_FASE0_INVENTARIO_COMPLETO.md`, achado P0-7.
 *
 * Este teste NAO decide qual comportamento e o correto -- so congela o que existe hoje,
 * para que uma consolidacao futura (fora do escopo desta fatia) precise decidir
 * deliberadamente, e nao por acidente.
 */
class FibraSignalQualityEngineZeroSentinelCharacterizationTest {

    @Test
    fun `ClassificadorSaudeGpon trata RX igual a 0,0 como pior caso -- ruim`() {
        assertEquals(GponSaudeStatus.ruim, ClassificadorSaudeGpon.classificarRx(0.0))
    }

    @Test
    fun `FibraSignalQualityEngine nao gera nenhum achado FIB-02 quando RX e 0,0`() {
        val input = FibraDiagnosticInput(
            rxPowerDbm = 0.0,
            txPowerDbm = 2.0,
            temperatureCelsius = 40.0,
            isUp = true,
        )
        val achados = FibraSignalQualityEngine.avaliar(input)

        // Caracteriza a divergencia: o classificador diria "ruim" para RX=0.0, mas o
        // engine simplesmente nao produz nenhum achado FIB-02/FIB-02b/FIB-02-OK para
        // essa metrica -- o mesmo "sem dado" e "silencioso" aqui e "critico" no
        // classificador usado diretamente.
        assertTrue(achados.none { it.id.startsWith("FIB-02") })
    }

    @Test
    fun `FibraSignalQualityEngine gera achado normalmente para RX diferente de zero`() {
        // Contraste: com um valor real (nao-sentinela) abaixo do limiar, o engine gera
        // o achado FIB-02 normalmente -- confirma que o "pulo" so acontece no sentinela
        // 0.0, nao em qualquer valor ruim.
        val input = FibraDiagnosticInput(
            rxPowerDbm = -30.0,
            txPowerDbm = 2.0,
            temperatureCelsius = 40.0,
            isUp = true,
        )
        val achados = FibraSignalQualityEngine.avaliar(input)
        assertTrue(achados.any { it.id == "FIB-02" })
    }
}
