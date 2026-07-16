package io.signallq.app.core.network.contracts.dispositivo

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Fase 0 (issue #977) — confirma que `TipoDispositivoRede` é superset do `TipoDispositivo`
 * atual de `feature/devices` mais `console`, reservado sem lógica de classificação nesta fase.
 */
class TipoDispositivoRedeTest {

    @Test
    fun `superset inclui todos os valores atuais de TipoDispositivo mais console`() {
        val valoresAtuais = setOf(
            "roteador",
            "pontoAcesso",
            "computador",
            "smartphone",
            "smarthome",
            "impressora",
            "desconhecido",
        )
        val nomes = TipoDispositivoRede.entries.map { it.name }.toSet()

        assertEquals(valoresAtuais + "console", nomes)
    }
}
