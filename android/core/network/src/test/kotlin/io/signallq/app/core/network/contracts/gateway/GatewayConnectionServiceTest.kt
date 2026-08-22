package io.signallq.app.core.network.contracts.gateway

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * BUG#1511 (P0) — guarda de regressao: nenhum caminho de producao pode voltar a injetar um
 * [GatewayConnectionService] que retorne [GatewayConnectionResultado.Sucesso] por padrao, sem
 * autenticação real comprovada. Ver `AppShell.kt`/`Inicio2Screen.kt` (parâmetro `conectarGateway`)
 * e `AjustesUiState.kt` (`AjustesModemState.conectarGateway`) — todos os defaults de producao
 * devem apontar para [GatewayConnectionServiceIndisponivelPadrao], nunca duplicar um literal
 * `GatewayConnectionResultado.Sucesso`.
 */
class GatewayConnectionServiceTest {
    @Test
    fun `default seguro nunca retorna Sucesso, para qualquer host, usuario e senha`() =
        runTest {
            val entradas =
                listOf(
                    Triple("192.168.1.1", "admin", "admin"),
                    Triple("", "", ""),
                    Triple("10.0.0.1", "root", "senha-correta-de-verdade"),
                    Triple("host-invalido", "usuario_qualquer", "qualquer-coisa"),
                )

            entradas.forEach { (host, usuario, senha) ->
                val resultado = GatewayConnectionServiceIndisponivelPadrao.conectar(host, usuario, senha)
                assertTrue(
                    "esperava Indisponivel para host=$host usuario=$usuario, obteve $resultado",
                    resultado is GatewayConnectionResultado.Indisponivel,
                )
            }
        }

    @Test
    fun `default seguro nunca marca sessao valida (Sucesso) em tentativas repetidas`() =
        runTest {
            // BUG#1511 — simula o padrao de autoconexao (varias tentativas para o mesmo
            // host/BSSID): nenhuma delas pode, em algum momento, virar Sucesso.
            repeat(10) {
                val resultado = GatewayConnectionServiceIndisponivelPadrao.conectar("192.168.1.1", "admin", "admin")
                assertEquals(GatewayConnectionResultado.Indisponivel, resultado)
            }
        }
}
