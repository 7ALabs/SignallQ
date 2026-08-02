package io.signallq.app.ui.screen

import io.signallq.app.core.network.contracts.gateway.GatewayConnectionResultado
import io.signallq.app.core.network.contracts.gateway.GatewayConnectionServiceIndisponivelPadrao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * BUG#1511 (P0) — guarda de regressao: o default de [AjustesModemState.conectarGateway] nunca
 * pode voltar a ser um literal `GatewayConnectionService { _, _, _ -> GatewayConnectionResultado.Sucesso }`
 * (era exatamente esse o mock que a #1511 removeu). O default de producao deve ser sempre
 * [GatewayConnectionServiceIndisponivelPadrao] (core:network), a mesma fonte unica usada por
 * `HomeScreen.conectarGateway` e por `AppShell.kt`.
 */
class AjustesUiStateTest {
    @Test
    fun `AjustesModemState conectarGateway default nunca retorna Sucesso`() =
        runTest {
            val estado =
                AjustesModemState(
                    modemHost = null,
                    modemUsername = "",
                    modemPassword = "",
                    modemPermanecerConectado = false,
                    gatewayIpDetectado = null,
                    onSalvarConfiguracaoModem = { _, _, _, _ -> },
                    onConectarFibra = { _, _, _ -> },
                )

            val resultado = estado.conectarGateway.conectar("192.168.1.1", "admin", "admin")

            assertEquals(GatewayConnectionResultado.Indisponivel, resultado)
        }

    @Test
    fun `AjustesModemState conectarGateway default e o mesmo default compartilhado de HomeScreen e AppShell`() {
        val estado =
            AjustesModemState(
                modemHost = null,
                modemUsername = "",
                modemPassword = "",
                modemPermanecerConectado = false,
                gatewayIpDetectado = null,
                onSalvarConfiguracaoModem = { _, _, _, _ -> },
                onConectarFibra = { _, _, _ -> },
            )

        assertEquals(GatewayConnectionServiceIndisponivelPadrao, estado.conectarGateway)
    }
}
