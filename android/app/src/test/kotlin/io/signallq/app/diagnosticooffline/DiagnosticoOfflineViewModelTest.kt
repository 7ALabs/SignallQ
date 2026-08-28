package io.signallq.app.diagnosticooffline

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
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

    /**
     * Coleta a sequência REAL de estados emitidos por [vm], não só o `.value` final.
     * Roda num coletor separado em `backgroundScope` (encerra sozinho quando o teste termina) --
     * é o que expõe estados transitórios (`EtapaOk`/`EtapaFalhou`) que `StateFlow` conflado
     * descarta se ninguém observar entre uma emissão e a próxima. Achado de revisão do Caio na
     * PR #1814: os 6 testes anteriores só checavam `vm.estado.value` no final e não pegaram o
     * bloqueio 1 (EtapaOk/EtapaFalhou inobserváveis) por causa disso.
     */
    private fun kotlinx.coroutines.test.TestScope.coletarEstados(
        vm: DiagnosticoOfflineViewModel,
    ): List<DiagnosticoOfflineEstado> {
        val coletados = mutableListOf<DiagnosticoOfflineEstado>()
        backgroundScope.launch { vm.estado.toList(coletados) }
        return coletados
    }

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
            val estados = coletarEstados(vm)

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

            // Regressão do bloqueio 1 (revisão Caio, PR #1814): EtapaOk precisa aparecer na
            // SEQUÊNCIA de estados emitidos para cada uma das 4 etapas -- não só existir no
            // histórico do estado final. Antes da correção, EtapaOk(GATEWAY) e EtapaOk(DNS) e
            // EtapaOk(ROTA_EXTERNA) eram sobrescritos por TestandoEtapa da próxima iteração sem
            // nenhum coletor conseguir observá-los.
            val etapasOkObservadas =
                estados.filterIsInstance<DiagnosticoOfflineEstado.EtapaOk>().map { it.etapa }
            assertEquals(EtapaDiagnosticoOffline.ORDEM, etapasOkObservadas)
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
            val estados = coletarEstados(vm)

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

            // Regressão do bloqueio 1: EtapaFalhou precisa ser observável antes de ser
            // sobrescrito por DiagnosticoConcluido na linha seguinte.
            val falhasObservadas = estados.filterIsInstance<DiagnosticoOfflineEstado.EtapaFalhou>()
            assertEquals(1, falhasObservadas.size)
            assertEquals(EtapaDiagnosticoOffline.DNS, falhasObservadas.single().etapa)
            assertEquals("timeout DNS", falhasObservadas.single().motivo)
        }

    @Test
    fun `excecao do executor vira EtapaFalhou em vez de travar em TestandoEtapa`() =
        runTest(dispatcher) {
            val vm =
                viewModel(
                    executor = { etapa ->
                        if (etapa == EtapaDiagnosticoOffline.ROTA_EXTERNA) {
                            throw java.io.IOException("host inalcancavel")
                        }
                        ResultadoEtapaDiagnosticoOffline.Sucesso(etapa)
                    },
                )
            val estados = coletarEstados(vm)

            vm.iniciar()
            dispatcher.scheduler.advanceUntilIdle()

            // Bloqueio 2 (revisão Caio, PR #1814): sem o try/catch, o estado ficaria congelado em
            // TestandoEtapa(ROTA_EXTERNA, ...) para sempre -- o cenário esperado quando o usuário
            // está de fato offline (I/O real falha com exceção, não com um Falha "limpo").
            assertTrue(
                "estado nao pode travar em TestandoEtapa apos excecao do executor, era ${vm.estado.value}",
                vm.estado.value is DiagnosticoOfflineEstado.DiagnosticoConcluido,
            )
            val estadoFinal = vm.estado.value as DiagnosticoOfflineEstado.DiagnosticoConcluido
            assertEquals(EtapaDiagnosticoOffline.ROTA_EXTERNA, estadoFinal.etapaComFalha)
            assertTrue(estadoFinal.historico.last() is ResultadoEtapaDiagnosticoOffline.Falha)

            // Depois de EtapaFalhou/DiagnosticoConcluido, retry() não pode ser no-op -- prova que
            // o estado realmente destravou e o fluxo é recuperável pelo usuário.
            val falhasObservadas = estados.filterIsInstance<DiagnosticoOfflineEstado.EtapaFalhou>()
            assertEquals(1, falhasObservadas.size)
            assertEquals(EtapaDiagnosticoOffline.ROTA_EXTERNA, falhasObservadas.single().etapa)
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
    fun `retry explicito apos fluxo completo com sucesso nao duplica etapas anteriores no historico`() =
        runTest(dispatcher) {
            // Regressão do bloqueio 3 (revisão Caio, PR #1814): as 4 etapas concluem com
            // sucesso, depois o usuário pede retry(GATEWAY) -- a implementação antiga filtrava
            // só a etapa-alvo por igualdade (`it.etapa != etapaParaRetry`), deixando DNS/ROTA/
            // HOSTNAME duplicados porque a reexecução também os gera de novo. O corte correto é
            // posicional: descarta GATEWAY e tudo que vem depois dele na ORDEM.
            val vm =
                viewModel(
                    executor = { etapa -> ResultadoEtapaDiagnosticoOffline.Sucesso(etapa) },
                )

            vm.iniciar()
            dispatcher.scheduler.advanceUntilIdle()
            assertTrue(vm.estado.value is DiagnosticoOfflineEstado.DiagnosticoConcluido)

            vm.retry(etapa = EtapaDiagnosticoOffline.GATEWAY)
            dispatcher.scheduler.advanceUntilIdle()

            assertTrue(vm.estado.value is DiagnosticoOfflineEstado.DiagnosticoConcluido)
            val estadoFinal = vm.estado.value as DiagnosticoOfflineEstado.DiagnosticoConcluido
            assertNull(estadoFinal.etapaComFalha)
            assertEquals(4, estadoFinal.historico.size)
            assertEquals(
                EtapaDiagnosticoOffline.ORDEM,
                estadoFinal.historico.map { it.etapa },
            )
        }

    @Test
    fun `retry sem etapa explicita apos falha nao duplica o registro de falha do historico`() =
        runTest(dispatcher) {
            // Cenário citado no review: retry() sem argumento parte de EtapaFalhou, cujo
            // historico JA inclui o resultado da própria falha (setado antes de EtapaFalhou ser
            // emitido). O corte precisa ser consistente com o branch de DiagnosticoConcluido --
            // os dois usam o mesmo corte posicional agora.
            var falharDns = true
            val vm =
                viewModel(
                    executor = { etapa ->
                        if (etapa == EtapaDiagnosticoOffline.DNS && falharDns) {
                            ResultadoEtapaDiagnosticoOffline.Falha(etapa, motivo = "timeout DNS")
                        } else {
                            ResultadoEtapaDiagnosticoOffline.Sucesso(etapa)
                        }
                    },
                )

            vm.iniciar()
            dispatcher.scheduler.advanceUntilIdle()
            assertTrue(vm.estado.value is DiagnosticoOfflineEstado.DiagnosticoConcluido)

            falharDns = false
            vm.retry()
            dispatcher.scheduler.advanceUntilIdle()

            assertTrue(vm.estado.value is DiagnosticoOfflineEstado.DiagnosticoConcluido)
            val estadoFinal = vm.estado.value as DiagnosticoOfflineEstado.DiagnosticoConcluido
            assertNull(estadoFinal.etapaComFalha)
            assertEquals(4, estadoFinal.historico.size)
            assertEquals(
                EtapaDiagnosticoOffline.ORDEM,
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
