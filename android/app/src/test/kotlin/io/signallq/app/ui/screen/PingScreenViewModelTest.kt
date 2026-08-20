package io.signallq.app.ui.screen

import io.signallq.app.feature.speedtest.PingExecutor
import io.signallq.app.ui.component.SignallQScreenState
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Caracterização de [PingScreenViewModel] antes da migração 2.0 (issue #1665) — cobre os
 * estados exigidos pelos critérios de aceite: executando/concluído, cancelamento e destino
 * customizado vs. padrão. O round-trip HTTP bem-sucedido (progresso, mediana, jitter, perda)
 * já é coberto por [io.signallq.app.feature.speedtest.PingExecutorTest], que roda dentro do
 * módulo `feature/speedtest` — não duplicado aqui. Todos os cenários abaixo usam um host
 * reservado por RFC 2606 (nunca resolve, sem depender de rede/DNS reais disponíveis no
 * ambiente de CI), o mesmo padrão determinístico já usado por `PingExecutorTest`.
 */
class PingScreenViewModelTest {
    private val destinoQueNuncaResolve = "https://nao-existe.invalid/__down"

    @Test
    fun `estado inicial e Loading antes de qualquer execucao`() {
        val viewModel = PingScreenViewModel()
        assertEquals(SignallQScreenState.Loading, viewModel.stateFlow.value)
    }

    @Test
    fun `executarPing transiciona para Concluido com abortadoPorRede quando destino nao resolve`() =
        runTest {
            val viewModel =
                PingScreenViewModel(criarExecutor = { PingExecutor(targetUrl = destinoQueNuncaResolve) })

            viewModel.executarPing(destino = null)

            // Rede indisponível vira Content<Concluido> com abortadoPorRede=true (tratamento
            // dedicado — não é o mesmo estado de erro genérico; ver PingExecutorTest).
            val estado = viewModel.stateFlow.value
            assertTrue(estado is SignallQScreenState.Content<*>)
            val dado = (estado as SignallQScreenState.Content<*>).value as PingUiData.Concluido
            assertTrue(dado.resultado.abortadoPorRede)
        }

    @Test
    fun `executarPing repassa o destino informado para a fabrica do executor`() =
        runTest {
            var hostRecebido: String? = "nao chamado"
            val viewModel =
                PingScreenViewModel(
                    criarExecutor = { destino ->
                        hostRecebido = destino
                        PingExecutor(targetUrl = destinoQueNuncaResolve)
                    },
                )

            viewModel.executarPing(destino = "host-customizado.invalid")

            assertEquals("host-customizado.invalid", hostRecebido)
        }

    @Test
    fun `executarPing sem destino repassa null para a fabrica, preservando o padrao`() =
        runTest {
            var hostRecebido: String? = "nao chamado"
            val viewModel =
                PingScreenViewModel(
                    criarExecutor = { destino ->
                        hostRecebido = destino
                        PingExecutor(targetUrl = destinoQueNuncaResolve)
                    },
                )

            viewModel.executarPing(destino = null)

            assertEquals(null, hostRecebido)
        }

    @Test
    fun `job cancelado antes de iniciar nunca produz RecoverableError`() =
        runTest {
            // GH#1211 item 5 / PingExecutorTest: cancelamento cooperativo real em pleno voo
            // (com I/O de rede real em andamento) é inerentemente não-determinístico num teste
            // JVM -- a mesma ressalva já registrada em PingExecutorTest se aplica aqui. Este
            // teste cobre o contrato que É determinístico: se a coroutine que chama
            // [PingScreenViewModel.executarPing] for cancelada antes de rodar (voltar da tela
            // antes da primeira composição concluir, por ex.), nunca produz RecoverableError --
            // o `catch (e: CancellationException) { throw e }` nunca é convertido em erro.
            val viewModel =
                PingScreenViewModel(criarExecutor = { PingExecutor(targetUrl = destinoQueNuncaResolve) })
            val job = launch(start = CoroutineStart.LAZY) { viewModel.executarPing(destino = null) }

            job.cancel()
            job.join()

            assertTrue(job.isCancelled)
            assertFalse(viewModel.stateFlow.value is SignallQScreenState.RecoverableError)
        }

    @Test
    fun `resetar volta o estado para Loading`() =
        runTest {
            val viewModel =
                PingScreenViewModel(criarExecutor = { PingExecutor(targetUrl = destinoQueNuncaResolve) })

            viewModel.executarPing(destino = null)
            assertTrue(viewModel.stateFlow.value is SignallQScreenState.Content<*>)

            viewModel.resetar()

            assertEquals(SignallQScreenState.Loading, viewModel.stateFlow.value)
        }
}
