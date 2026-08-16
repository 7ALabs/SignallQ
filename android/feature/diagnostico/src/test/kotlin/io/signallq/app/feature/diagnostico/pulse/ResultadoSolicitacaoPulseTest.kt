package io.signallq.app.feature.diagnostico.pulse

import io.signallq.app.feature.diagnostico.DiagnosticOrchestrator
import org.junit.Assert.assertNull
import org.junit.Test

class ResultadoSolicitacaoPulseTest {
    @Test
    fun `Pulse rejeitado nao consome terminal de outra geracao`() {
        assertNull(DiagnosticOrchestrator.ResultadoSolicitacao.Rejeitada.relatorioCorrelacionado())
    }
}
