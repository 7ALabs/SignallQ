package io.signallq.app.diagnosticooffline

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticoOfflineViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(executor: ExecutorEtapaDiagnosticoOffline) = DiagnosticoOfflineViewModel(executor)

    @Test
    fun `estado inicial e Idle`() {
        val vm = viewModel(executor = { etapa -> ResultadoEtapaDiagnosticoOffline.Sucesso(etapa) })
        assertEquals(DiagnosticoOfflineEstado.Idle, vm.estado.value)
    }

    @Test
    fun `iniciar com todas as etapas ok testa na ordem gateway-dns-rota-hostname e conclui sem falha`() =
        runTest(dispatcher) {
            val ordemTestada = mutableListOf<EtapaDiagnosticoOffline>()
            val vm =
                viewModel(
                    executor = { etapa ->
                        ordemTestada += etapa
                        ResultadoEtapaDiagnosticoOffline.Sucesso(etapa)
                    },
                )

            vm.iniciar()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                listOf(
                    EtapaDiagnosticoOffline.GATEWAY,
                    EtapaDiagnosticoOffline.DNS,
                    EtapaDiagnosticoOffline.ROTA_EXTERNA,
                    EtapaDiagnosticoOffline.HOSTNAME_CAPTIVE_PORTAL,
                ),
                ordemTestada,
            )

            assertTrue(vm.estado.value is DiagnosticoOfflineEstado.DiagnosticoConcluido)
            val estadoFinal = vm.estado.value as DiagnosticoOfflineEstado.DiagnosticoConcluido
            assertNull(estadoFinal.etapaComFalha)
            assertEquals(4, estadoFinal.historico.size)
            assertEquals(true, estadoFinal.historico.all { it is ResultadoEtapaDiagnosticoOffline.Sucesso })
        }

    @Test
    fun `falha na etapa DNS interrompe o fluxo antes de rota externa e hostname`() =
        runTest(dispatcher) {
            val ordemTestada = mutableListOf<EtapaDiagnosticoOffline>()
            val vm =
                viewModel(
                    executor = { etapa ->
                        ordemTestada += etapa
                        if (etapa == EtapaDiagnosticoOffline.DNS) {
                            ResultadoEtapaDiagnosticoOffline.Falha(etapa, motivo = "timeout DNS")
                        } else {
                            ResultadoEtapaDiagnosticoOffline.Sucesso(etapa)
                        }
                    },
                )

            vm.iniciar()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                listOf(EtapaDiagnosticoOffline.GATEWAY, EtapaDiagnosticoOffline.DNS),
                ordemTestada,
            )

            assertTrue(vm.estado.value is DiagnosticoOfflineEstado.DiagnosticoConcluido)
            val estadoFinal = vm.estado.value as DiagnosticoOfflineEstado.DiagnosticoConcluido
            assertEquals(EtapaDiagnosticoOffline.DNS, estadoFinal.etapaComFalha)
            assertEquals(2, estadoFinal.historico.size)
            assertTrue(estadoFinal.historico.last() is ResultadoEtapaDiagnosticoOffline.Falha)
        }

    @Test
    fun `retry apos falha reexecuta a partir da etapa que falhou preservando historico anterior`() =
        runTest(dispatcher) {
            var falharGateway = false
            val ordemTestada = mutableListOf<EtapaDiagnosticoOffline>()
            val vm =
                viewModel(
                    executor = { etapa ->
                        ordemTestada += etapa
                        when {
                            etapa == EtapaDiagnosticoOffline.GATEWAY && falharGateway -> {
                                ResultadoEtapaDiagnosticoOffline.Falha(etapa, motivo = "gateway inalcancavel")
                            }
                            else -> ResultadoEtapaDiagnosticoOffline.Sucesso(etapa)
                        }
                    },
                )

            falharGateway = true
            vm.iniciar()
            dispatcher.scheduler.advanceUntilIdle()

            assertTrue(vm.estado.value is DiagnosticoOfflineEstado.DiagnosticoConcluido)
            val falhouPrimeiraVez = vm.estado.value as DiagnosticoOfflineEstado.DiagnosticoConcluido
            assertEquals(EtapaDiagnosticoOffline.GATEWAY, falhouPrimeiraVez.etapaComFalha)

            falharGateway = false
            ordemTestada.clear()
            vm.retry()

            // Estado transitório de retry deve aparecer antes da nova rodada completar.
            assertTrue(vm.estado.value is DiagnosticoOfflineEstado.RetryEmAndamento)

            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                listOf(
                    EtapaDiagnosticoOffline.GATEWAY,
                    EtapaDiagnosticoOffline.DNS,
                    EtapaDiagnosticoOffline.ROTA_EXTERNA,
                    EtapaDiagnosticoOffline.HOSTNAME_CAPTIVE_PORTAL,
                ),
                ordemTestada,
            )

            assertTrue(vm.estado.value is DiagnosticoOfflineEstado.DiagnosticoConcluido)
            val estadoFinal = vm.estado.value as DiagnosticoOfflineEstado.DiagnosticoConcluido
            assertNull(estadoFinal.etapaComFalha)
            assertEquals(4, estadoFinal.historico.size)
        }

    @Test
    fun `retry explicito de uma etapa especifica ignora a ultima falha registrada`() =
        runTest(dispatcher) {
            val vm =
                viewModel(
                    executor = { etapa -> ResultadoEtapaDiagnosticoOffline.Sucesso(etapa) },
                )

            vm.retry(etapa = EtapaDiagnosticoOffline.ROTA_EXTERNA)
            dispatcher.scheduler.advanceUntilIdle()

            assertTrue(vm.estado.value is DiagnosticoOfflineEstado.DiagnosticoConcluido)
            val estadoFinal = vm.estado.value as DiagnosticoOfflineEstado.DiagnosticoConcluido
            assertNull(estadoFinal.etapaComFalha)
            // Retry explícito parte só de rota externa + hostname, não repete gateway/DNS.
            assertEquals(2, estadoFinal.historico.size)
            assertEquals(
                listOf(EtapaDiagnosticoOffline.ROTA_EXTERNA, EtapaDiagnosticoOffline.HOSTNAME_CAPTIVE_PORTAL),
                estadoFinal.historico.map { it.etapa },
            )
        }

    @Test
    fun `retry sem falha anterior e sem etapa explicita e no-op`() =
        runTest(dispatcher) {
            val vm =
                viewModel(
                    executor = { etapa -> ResultadoEtapaDiagnosticoOffline.Sucesso(etapa) },
                )

            vm.retry()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(DiagnosticoOfflineEstado.Idle, vm.estado.value)
        }
}
