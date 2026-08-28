package io.signallq.app.diagnosticooffline

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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

    @Test
    fun `iniciar duas vezes seguidas nao lanca duas rodadas concorrentes`() =
        runTest(dispatcher) {
            // Bloqueio único da revisão do Caio na PR #1816: a guarda de Job (`jobEmAndamento`)
            // existe pra ignorar o segundo tap sem cancelar nem reiniciar o fluxo. Prova aqui que
            // cada etapa é executada exatamente 1x mesmo com duas chamadas consecutivas a
            // iniciar() -- o segundo Job nunca chega a ser lançado, então não há segunda rodada
            // pra corromper histórico ou emitir estado duplicado.
            val execucoes = mutableListOf<EtapaDiagnosticoOffline>()
            val vm =
                viewModel(
                    executor = { etapa ->
                        execucoes += etapa
                        delay(10)
                        ResultadoEtapaDiagnosticoOffline.Sucesso(etapa)
                    },
                )
            val estados = coletarEstados(vm)

            vm.iniciar()
            // Tap duplo: chamada imediata, antes de qualquer avanço do dispatcher de teste --
            // jobEmAndamento já está ativo (Job fica ativo assim que launch() retorna, mesmo sem
            // ter começado a rodar o corpo), então esta segunda chamada precisa ser ignorada.
            vm.iniciar()
            dispatcher.scheduler.advanceUntilIdle()

            assertEquals(
                "cada etapa deve ser sondada exatamente uma vez -- tap duplo nao pode lancar rodada concorrente",
                EtapaDiagnosticoOffline.ORDEM,
                execucoes,
            )

            assertTrue(vm.estado.value is DiagnosticoOfflineEstado.DiagnosticoConcluido)
            val estadoFinal = vm.estado.value as DiagnosticoOfflineEstado.DiagnosticoConcluido
            assertNull(estadoFinal.etapaComFalha)
            assertEquals(4, estadoFinal.historico.size)

            // Nenhum TestandoEtapa duplicado na sequência observada -- confirma que não houve
            // uma segunda corrotina emitindo por cima da primeira.
            val etapasTestadasObservadas =
                estados.filterIsInstance<DiagnosticoOfflineEstado.TestandoEtapa>().map { it.etapa }
            assertEquals(EtapaDiagnosticoOffline.ORDEM, etapasTestadasObservadas)
        }

    @Test
    fun `retry durante rodada em andamento cancela a anterior e nao mistura historico`() =
        runTest(dispatcher) {
            // Bloqueio único da revisão do Caio na PR #1816: retry() faz jobEmAndamento?.cancel()
            // e escreve RetryEmAndamento SEM join() antes. Este teste fecha exatamente a janela
            // que o Caio apontou -- força uma rodada antiga a ficar suspensa em pleno GATEWAY
            // (via delay de tempo virtual controlado pelo próprio TestDispatcher), dispara
            // retry() nesse meio-tempo, e prova que a rodada antiga NUNCA consegue escrever por
            // cima do RetryEmAndamento nem duplicar GATEWAY no histórico final.
            var chamadasGateway = 0
            val vm =
                viewModel(
                    executor = { etapa ->
                        if (etapa == EtapaDiagnosticoOffline.GATEWAY) {
                            chamadasGateway++
                            if (chamadasGateway == 1) {
                                // Rodada antiga: suspende "em andamento" e, se não for
                                // cancelada, terminaria em falha -- prova (se aparecer no
                                // histórico ou nos estados observados) que o cancel() falhou.
                                delay(10_000)
                                ResultadoEtapaDiagnosticoOffline.Falha(etapa, motivo = "rodada antiga nao deveria completar")
                            } else {
                                ResultadoEtapaDiagnosticoOffline.Sucesso(etapa)
                            }
                        } else {
                            ResultadoEtapaDiagnosticoOffline.Sucesso(etapa)
                        }
                    },
                )
            val estados = coletarEstados(vm)

            vm.iniciar()
            // Avança só até o ponto de suspensão dentro do delay(10_000) -- a rodada antiga fica
            // com Job ativo, presa em TestandoEtapa(GATEWAY), exatamente o cenário "Job ativo"
            // que o Caio pediu para reproduzir deterministicamente.
            dispatcher.scheduler.runCurrent()
            assertEquals(
                DiagnosticoOfflineEstado.TestandoEtapa(EtapaDiagnosticoOffline.GATEWAY, emptyList()),
                vm.estado.value,
            )

            vm.retry(etapa = EtapaDiagnosticoOffline.GATEWAY)

            // Escrita de RetryEmAndamento é síncrona dentro de retry() -- precisa já valer aqui,
            // antes de qualquer avanço adicional do dispatcher, provando que não há corrida entre
            // o cancel() e esta escrita.
            assertTrue(vm.estado.value is DiagnosticoOfflineEstado.RetryEmAndamento)

            dispatcher.scheduler.advanceUntilIdle()

            assertTrue(vm.estado.value is DiagnosticoOfflineEstado.DiagnosticoConcluido)
            val estadoFinal = vm.estado.value as DiagnosticoOfflineEstado.DiagnosticoConcluido
            assertNull(estadoFinal.etapaComFalha)
            // 4, não 5: se a rodada antiga tivesse sobrevivido ao cancel(), GATEWAY apareceria
            // duas vezes (uma da rodada cancelada, outra da nova).
            assertEquals(4, estadoFinal.historico.size)
            assertEquals(EtapaDiagnosticoOffline.ORDEM, estadoFinal.historico.map { it.etapa })
            assertEquals(2, chamadasGateway)

            // Nenhum estado observado carrega o motivo de falha da rodada cancelada -- prova que
            // ela nunca escreveu em _estado depois do cancel(), nem por corrida.
            val falhasObservadas = estados.filterIsInstance<DiagnosticoOfflineEstado.EtapaFalhou>()
            assertTrue(falhasObservadas.none { it.motivo == "rodada antiga nao deveria completar" })
            assertTrue(
                estados
                    .filterIsInstance<DiagnosticoOfflineEstado.DiagnosticoConcluido>()
                    .none { concluido -> concluido.historico.any { it is ResultadoEtapaDiagnosticoOffline.Falha } },
            )
        }
}
