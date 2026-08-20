package io.signallq.app.ui.screen

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Issue #1670 — caracterização/regressão de MainViewModel.executarAcaoDadosLocais.
 *
 * MainViewModel é um AndroidViewModel com dependências Hilt e não pode ser instanciado num
 * teste unitário puro (mesma limitação documentada em MainViewModelHistoricoTest). Este teste
 * reproduz a lógica exata da função (mesma ordem de estados, mesmo tratamento de
 * CancellationException) de forma isolada, com um bloco fake no lugar do DAO/repositório real.
 *
 * Cobre o critério de aceite de #1670 "confirmação destrutiva/progresso/falha parcial": antes
 * desta issue as 3 ações (limpar histórico/apagar dados/resetar app) eram fire-and-forget — uma
 * falha no meio da operação era indistinguível de sucesso pra quem usa o app.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AcaoDadosLocaisEstadoTest {
    /** Reproduz MainViewModel.executarAcaoDadosLocais sem viewModelScope/dispatchers reais. */
    private class ExecutorFake {
        private val _estado = MutableStateFlow<AcaoDadosLocaisEstado>(AcaoDadosLocaisEstado.Ocioso)
        val estado: StateFlow<AcaoDadosLocaisEstado> = _estado.asStateFlow()

        suspend fun executar(
            acao: TipoAcaoDadosLocais,
            bloco: suspend () -> Unit,
        ) {
            _estado.value = AcaoDadosLocaisEstado.EmAndamento(acao)
            try {
                bloco()
                _estado.value = AcaoDadosLocaisEstado.Sucesso(acao)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _estado.value =
                    AcaoDadosLocaisEstado.Falha(acao, "Não foi possível concluir. Tente novamente.")
            }
        }

        fun consumir() {
            _estado.value = AcaoDadosLocaisEstado.Ocioso
        }
    }

    @Test
    fun `estado inicial e Ocioso`() {
        val executor = ExecutorFake()
        assertEquals(AcaoDadosLocaisEstado.Ocioso, executor.estado.value)
    }

    @Test
    fun `acao bem sucedida termina em Sucesso com a mesma acao`() =
        runTest {
            val executor = ExecutorFake()

            executor.executar(TipoAcaoDadosLocais.LIMPAR_HISTORICO) { /* sucesso */ }

            assertEquals(
                AcaoDadosLocaisEstado.Sucesso(TipoAcaoDadosLocais.LIMPAR_HISTORICO),
                executor.estado.value,
            )
        }

    @Test
    fun `acao que lanca excecao termina em Falha, nao em Sucesso`() =
        runTest {
            val executor = ExecutorFake()

            executor.executar(TipoAcaoDadosLocais.APAGAR_DADOS_LOCAIS) {
                error("escrita no DataStore falhou")
            }

            val estado = executor.estado.value
            assertTrue(estado is AcaoDadosLocaisEstado.Falha)
            assertEquals(TipoAcaoDadosLocais.APAGAR_DADOS_LOCAIS, (estado as AcaoDadosLocaisEstado.Falha).acao)
        }

    @Test
    fun `CancellationException nao vira Falha - e relancada`() =
        runTest {
            val executor = ExecutorFake()

            try {
                executor.executar(TipoAcaoDadosLocais.RESETAR_APP) {
                    throw CancellationException("corrotina cancelada")
                }
                error("deveria ter relançado CancellationException")
            } catch (e: CancellationException) {
                // esperado — cancelamento nunca deve virar Falha visível pro usuário
            }

            // Estado fica em EmAndamento (nunca chega a Sucesso nem Falha) porque a
            // corrotina foi cancelada antes de atualizar o estado final.
            assertEquals(
                AcaoDadosLocaisEstado.EmAndamento(TipoAcaoDadosLocais.RESETAR_APP),
                executor.estado.value,
            )
        }

    @Test
    fun `consumir volta o estado para Ocioso depois de Sucesso`() =
        runTest {
            val executor = ExecutorFake()
            executor.executar(TipoAcaoDadosLocais.LIMPAR_HISTORICO) { }

            executor.consumir()

            assertEquals(AcaoDadosLocaisEstado.Ocioso, executor.estado.value)
        }

    @Test
    fun `consumir volta o estado para Ocioso depois de Falha`() =
        runTest {
            val executor = ExecutorFake()
            executor.executar(TipoAcaoDadosLocais.RESETAR_APP) { error("falhou") }

            executor.consumir()

            assertEquals(AcaoDadosLocaisEstado.Ocioso, executor.estado.value)
        }
}
