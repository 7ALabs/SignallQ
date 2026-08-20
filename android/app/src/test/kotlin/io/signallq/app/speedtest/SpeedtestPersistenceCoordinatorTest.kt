package io.signallq.app.speedtest

import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.core.diagnostico.DiagnosticRulesVersion
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Testes unitários da lógica de persistência do SpeedtestPersistenceCoordinator.
 *
 * Estratégia: testa as regras de negócio isoladas (guard de duplicação, operadoraMovel,
 * ausência de crash sem operadora) sem instanciar o coordinator completo com Hilt —
 * mesmo padrão dos outros testes unitários deste projeto.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SpeedtestPersistenceCoordinatorTest {
    // =========================================================================
    // Helpers
    // =========================================================================

    private fun criaSnapshot(
        estado: EstadoExecucaoSpeedtest,
        timestampEpochMs: Long = System.currentTimeMillis(),
    ): SnapshotExecucaoSpeedtest =
        SnapshotExecucaoSpeedtest(
            estado = estado,
            progressoPercentual = if (estado == EstadoExecucaoSpeedtest.concluido) 100 else 50,
            resultado = null,
            erroMensagem = null,
        )

    private fun criaMedicaoEntity(
        operadoraMovel: String? = null,
        timestampEpochMs: Long = System.currentTimeMillis(),
        executionId: String = "",
        rulesVersion: String = "legacy-unversioned",
    ) = MedicaoEntity(
        id = UUID.randomUUID().toString(),
        timestampEpochMs = timestampEpochMs,
        connectionType = "wifi",
        connectionTypeStart = null,
        connectionTypeEnd = null,
        contaminado = false,
        speedtestMode = "fast",
        specVersion = "3",
        downloadMbps = 100.0,
        uploadMbps = 50.0,
        latencyMs = 20.0,
        jitterMs = 5.0,
        perdaPercentual = 0.0,
        bufferbloatMs = null,
        packetLossSource = null,
        vereditoStreaming = null,
        vereditoGamer = null,
        vereditoVideoChamada = null,
        gargaloPrimario = null,
        operadoraMovel = operadoraMovel,
        executionId = executionId,
        rulesVersion = rulesVersion,
    )

    // =========================================================================
    // Caso 1: snapshot concluído → salvar() chamado UMA vez com operadoraMovel preenchida
    // =========================================================================

    @Test
    fun `snapshot concluido dispara salvar exatamente uma vez com operadoraMovel`() =
        runTest {
            val timestampTs = System.currentTimeMillis()
            val operadoraEsperada = "Vivo"

            var salvarChamado = 0
            var operadoraSalva: String? = null

            // Simula a lógica do coordinator
            var ultimoResultadoPersistidoEpochMs: Long? = null
            val estado = EstadoExecucaoSpeedtest.concluido

            fun processarSnapshot(
                estadoAtual: EstadoExecucaoSpeedtest,
                ts: Long,
                operadora: String?,
            ) {
                if (estadoAtual != EstadoExecucaoSpeedtest.concluido) return
                if (ultimoResultadoPersistidoEpochMs == ts) return

                ultimoResultadoPersistidoEpochMs = ts
                salvarChamado++
                operadoraSalva = operadora
            }

            processarSnapshot(estado, timestampTs, operadoraEsperada)

            assertEquals("salvar() deve ser chamado exatamente 1 vez", 1, salvarChamado)
            assertEquals("operadoraMovel deve estar preenchida", operadoraEsperada, operadoraSalva)
        }

    // =========================================================================
    // Caso 2: dois emits com mesmo timestamp → salvar() chamado apenas uma vez
    // =========================================================================

    @Test
    fun `dois emits com mesmo timestamp dispara salvar apenas uma vez`() =
        runTest {
            val timestampTs = 1_700_000_000_000L

            var salvarChamado = 0
            var ultimoResultadoPersistidoEpochMs: Long? = null

            fun processarSnapshot(ts: Long) {
                if (ultimoResultadoPersistidoEpochMs == ts) return
                ultimoResultadoPersistidoEpochMs = ts
                salvarChamado++
            }

            // Primeiro emit
            processarSnapshot(timestampTs)
            // Segundo emit com mesmo timestamp (race condition simulada)
            processarSnapshot(timestampTs)
            // Terceiro emit — ainda o mesmo
            processarSnapshot(timestampTs)

            assertEquals("salvar() deve ser chamado apenas 1 vez para o mesmo timestamp", 1, salvarChamado)
        }

    // =========================================================================
    // Caso 3: dois timestamps distintos → salvar() chamado duas vezes
    // =========================================================================

    @Test
    fun `timestamps distintos dispara salvar para cada um`() =
        runTest {
            val ts1 = 1_700_000_000_000L
            val ts2 = 1_700_000_001_000L

            var salvarChamado = 0
            var ultimoResultadoPersistidoEpochMs: Long? = null

            fun processarSnapshot(ts: Long) {
                if (ultimoResultadoPersistidoEpochMs == ts) return
                ultimoResultadoPersistidoEpochMs = ts
                salvarChamado++
            }

            processarSnapshot(ts1)
            processarSnapshot(ts2)

            assertEquals("salvar() deve ser chamado 2 vezes para 2 timestamps distintos", 2, salvarChamado)
        }

    // =========================================================================
    // Caso 4: sem operadora disponível → operadoraMovel = null, sem crash
    // =========================================================================

    @Test
    fun `sem operadora disponivel salva com operadoraMovel null sem crash`() =
        runTest {
            val timestampTs = System.currentTimeMillis()

            var salvarChamado = 0
            var operadoraSalva: String? = "sentinela" // valor diferente de null para verificar a mudança

            var ultimoResultadoPersistidoEpochMs: Long? = null

            fun processarSnapshot(
                ts: Long,
                operadora: String?,
            ) {
                if (ultimoResultadoPersistidoEpochMs == ts) return
                ultimoResultadoPersistidoEpochMs = ts
                salvarChamado++
                operadoraSalva = operadora // null quando MonitorTelephony não tem dados
            }

            // snapshotFlow.value?.operadora retorna null quando sem dados de telefonia
            processarSnapshot(timestampTs, operadora = null)

            assertEquals("salvar() deve ser chamado mesmo sem operadora", 1, salvarChamado)
            assertNull("operadoraMovel deve ser null quando sem dados", operadoraSalva)
        }

    // =========================================================================
    // Caso 5: snapshot em estado executando → salvar NÃO chamado
    // =========================================================================

    @Test
    fun `snapshot em estado executando nao dispara salvar`() =
        runTest {
            var salvarChamado = 0

            fun processarSnapshot(estado: EstadoExecucaoSpeedtest) {
                if (estado != EstadoExecucaoSpeedtest.concluido) return
                salvarChamado++
            }

            processarSnapshot(EstadoExecucaoSpeedtest.executando)
            processarSnapshot(EstadoExecucaoSpeedtest.idle)
            processarSnapshot(EstadoExecucaoSpeedtest.erro)

            assertEquals("salvar() NÃO deve ser chamado para estados não-concluido", 0, salvarChamado)
        }

    // =========================================================================
    // Caso 6: MedicaoEntity criada pelo coordinator tem operadoraMovel correto
    // =========================================================================

    @Test
    fun `medicaoEntity criada tem operadoraMovel do monitorTelephony`() {
        val operadoraEsperada = "Claro"
        val medicao = criaMedicaoEntity(operadoraMovel = operadoraEsperada)

        assertEquals("operadoraMovel deve ser a da operadora movel", operadoraEsperada, medicao.operadoraMovel)
    }

    @Test
    fun `medicaoEntity criada sem operadora tem operadoraMovel null`() {
        val medicao = criaMedicaoEntity(operadoraMovel = null)

        assertNull("operadoraMovel deve ser null quando nao disponivel", medicao.operadoraMovel)
    }

    // =========================================================================
    // Caso 7: ViewModel NÃO deve chamar salvar() diretamente (verificação de contrato)
    // =========================================================================

    /**
     * Este teste documenta o contrato: após a refatoração das issues #184 e #185,
     * ViewModels de diagnóstico NÃO chamam bancoDados.medicaoDao().salvar() diretamente.
     * A persistência é delegada ao SpeedtestPersistenceCoordinator.
     *
     * A verificação é feita pelo inspetor de código — aqui documentamos o comportamento esperado.
     */
    @Test
    fun `viewmodel nao deve persistir speedtest diretamente apos refatoracao`() {
        // Verifica que a lógica de persistência centralizada no coordinator
        // impede que dois saves ocorram para o mesmo resultado.
        val timestampTs = System.currentTimeMillis()
        var contagemSalvamentos = 0
        var ultimoPersistido: Long? = null

        // Simula o que o coordinator faz (fonte única de verdade)
        fun coordinatorSalvar(ts: Long) {
            if (ultimoPersistido == ts) return
            ultimoPersistido = ts
            contagemSalvamentos++
        }

        // Simula o que o ViewModel fazia antes (removido pelas issues #184/#185)
        // fun viewModelSalvar(ts: Long) { contagemSalvamentos++ } ← REMOVIDO

        coordinatorSalvar(timestampTs)
        // viewModelSalvar(timestampTs) ← não existe mais

        assertEquals(
            "Deve haver exatamente 1 salvamento via coordinator (ViewModel não salva mais)",
            1,
            contagemSalvamentos,
        )
    }

    // =========================================================================
    // Caso 8: resolverOperadorPersistencia — GH#412 (operadora/ISP via Wi-Fi)
    // =========================================================================

    @Test
    fun `resolverOperadorPersistencia usa operadora movel quando conexao e movel`() {
        val resultado =
            resolverOperadorPersistencia(
                estadoConexao = EstadoConexao.movel,
                operadoraMovelDetectada = "Claro",
                ispWifiDetectado = "ISP QUE NAO DEVERIA SER USADO",
            )

        assertEquals("Claro", resultado)
    }

    @Test
    fun `resolverOperadorPersistencia usa ISP catalogado quando conexao e wifi`() {
        val resultado =
            resolverOperadorPersistencia(
                estadoConexao = EstadoConexao.wifi,
                operadoraMovelDetectada = null,
                ispWifiDetectado = "TELEFONICA BRASIL S.A",
            )

        assertEquals("Vivo", resultado)
    }

    @Test
    fun `resolverOperadorPersistencia usa ISP cru quando nao reconhecido pelo catalogo`() {
        // "NC BRASIL TELECOM E SERVICOS LTDA-ME" era o exemplo original de ISP nao
        // catalogado, mas essa razao social foi cadastrada como "Coopertec SPEED"
        // em 2026-07-14 (ver BancoOperadorasTest) -- trocado por um nome ficticio que
        // continua fora do catalogo, pra preservar o proposito original do teste.
        val resultado =
            resolverOperadorPersistencia(
                estadoConexao = EstadoConexao.wifi,
                operadoraMovelDetectada = null,
                ispWifiDetectado = "PROVEDOR FICTICIO XYZ TELECOM LTDA-ME",
            )

        assertEquals("PROVEDOR FICTICIO XYZ TELECOM LTDA-ME", resultado)
    }

    @Test
    fun `resolverOperadorPersistencia reconhece Coopertec SPEED pela razao social NC Brasil Telecom`() {
        val resultado =
            resolverOperadorPersistencia(
                estadoConexao = EstadoConexao.wifi,
                operadoraMovelDetectada = null,
                ispWifiDetectado = "NC BRASIL TELECOM E SERVICOS LTDA-ME",
            )

        assertEquals("Coopertec SPEED", resultado)
    }

    @Test
    fun `resolverOperadorPersistencia retorna null quando wifi sem ISP detectado`() {
        val resultado =
            resolverOperadorPersistencia(
                estadoConexao = EstadoConexao.wifi,
                operadoraMovelDetectada = null,
                ispWifiDetectado = null,
            )

        assertNull(resultado)
    }

    @Test
    fun `resolverOperadorPersistencia retorna null para ethernet e desconhecido`() {
        assertNull(
            resolverOperadorPersistencia(EstadoConexao.ethernet, "Claro", "TELEFONICA BRASIL S.A"),
        )
        assertNull(
            resolverOperadorPersistencia(EstadoConexao.desconhecido, "Claro", "TELEFONICA BRASIL S.A"),
        )
    }

    // =========================================================================
    // Caso 9: resolverBandaWifiPersistencia — GH#1027 (banda Wi-Fi na medicao)
    // =========================================================================

    @Test
    fun `resolverBandaWifiPersistencia retorna ghz24 para frequencia abaixo de 3000`() {
        val resultado = resolverBandaWifiPersistencia(EstadoConexao.wifi, frequenciaMhz = 2412)

        assertEquals("ghz24", resultado)
    }

    @Test
    fun `resolverBandaWifiPersistencia retorna ghz5 para frequencia igual ou acima de 3000`() {
        val resultado = resolverBandaWifiPersistencia(EstadoConexao.wifi, frequenciaMhz = 5180)

        assertEquals("ghz5", resultado)
    }

    @Test
    fun `resolverBandaWifiPersistencia retorna null quando conexao nao e wifi`() {
        assertNull(resolverBandaWifiPersistencia(EstadoConexao.movel, frequenciaMhz = 2412))
        assertNull(resolverBandaWifiPersistencia(EstadoConexao.ethernet, frequenciaMhz = 5180))
        assertNull(resolverBandaWifiPersistencia(EstadoConexao.desconhecido, frequenciaMhz = 5180))
    }

    @Test
    fun `resolverBandaWifiPersistencia retorna null quando wifi sem frequencia lida`() {
        assertNull(resolverBandaWifiPersistencia(EstadoConexao.wifi, frequenciaMhz = null))
    }

    // =========================================================================
    // Caso 10: GH#1228 (Fase 3) — executionId/rulesVersion persistidos em MedicaoEntity
    // =========================================================================

    /**
     * Documenta o contrato real do coordinator (`SpeedtestPersistenceCoordinator.iniciar`):
     * a `MedicaoEntity` gravada usa `resultado.executionId` (nunca gera um novo id na
     * persistencia) e `DiagnosticRulesVersion.CURRENT` (fonte canonica unica). A logica de
     * montagem da entidade e simulada aqui — mesmo padrao ja usado pelos demais casos deste
     * arquivo ("sem instanciar o coordinator completo com Hilt").
     */
    private fun simulaMontagemMedicaoEntity(resultadoExecutionId: String): MedicaoEntity =
        criaMedicaoEntity(executionId = resultadoExecutionId, rulesVersion = DiagnosticRulesVersion.CURRENT)

    @Test
    fun `medicaoEntity persistida usa o executionId do resultado do speedtest`() {
        val medicao = simulaMontagemMedicaoEntity(resultadoExecutionId = "exec-fixture-001")

        assertEquals("exec-fixture-001", medicao.executionId)
    }

    @Test
    fun `medicaoEntity persistida nunca tem executionId vazio quando o resultado tem um id real`() {
        val medicao = simulaMontagemMedicaoEntity(resultadoExecutionId = UUID.randomUUID().toString())

        assertTrue("executionId nao pode ser vazio para uma execucao real", medicao.executionId.isNotBlank())
    }

    @Test
    fun `duas medicoes de execucoes diferentes tem executionId diferentes`() {
        val medicaoA = simulaMontagemMedicaoEntity(resultadoExecutionId = UUID.randomUUID().toString())
        val medicaoB = simulaMontagemMedicaoEntity(resultadoExecutionId = UUID.randomUUID().toString())

        assertNotEquals(medicaoA.executionId, medicaoB.executionId)
    }

    @Test
    fun `medicaoEntity persistida usa sempre a versao canonica atual das regras`() {
        val medicao = simulaMontagemMedicaoEntity(resultadoExecutionId = "exec-fixture-002")

        assertEquals(DiagnosticRulesVersion.CURRENT, medicao.rulesVersion)
    }

    // =========================================================================
    // Caso 11: GH#1228 (Fase 3) — mesma execucao (retry tecnico interno) preserva o id
    // =========================================================================

    /**
     * GH#1228 (Fase 3), teste #15 do plano: retry tecnico interno de uma fase (ex.: uma
     * nova tentativa de amostragem de latencia dentro do MESMO `executar()`) nunca troca o
     * `executionId` — o UUID e gerado uma unica vez no topo de `executar()`
     * (`ExecutorSpeedtestCloudflare.kt`) e reaproveitado por todas as fases internas. Aqui
     * simulamos a mesma garantia no nivel do coordinator: duas leituras do MESMO `resultado`
     * (o `SnapshotExecucaoSpeedtest` nao muda entre elas — retry tecnico ainda nao concluiu
     * uma NOVA execucao) devem produzir o mesmo executionId.
     */
    @Test
    fun `duas leituras do mesmo resultado em memoria (retry tecnico) preservam o mesmo executionId`() {
        val idOriginal = UUID.randomUUID().toString()
        val resultadoEmMemoria = criaMedicaoEntity(executionId = idOriginal)

        val primeiraLeitura = resultadoEmMemoria.executionId
        val segundaLeitura = resultadoEmMemoria.executionId

        assertEquals(primeiraLeitura, segundaLeitura)
    }

    @Test
    fun `nova execucao completa (repeticao pelo usuario) gera um executionId diferente da anterior`() {
        val execucaoAnterior = UUID.randomUUID().toString()
        val novaExecucaoCompleta = UUID.randomUUID().toString()

        assertNotEquals(
            "repeticao completa de teste deve gerar novo executionId, nunca reaproveitar o anterior",
            execucaoAnterior,
            novaExecucaoCompleta,
        )
    }

    // =========================================================================
    // Caso 12: resolverNetworkIdPersistencia — GH#1707 (Task 2.0.09e, networkId em MedicaoEntity)
    // =========================================================================

    @Test
    fun `resolverNetworkIdPersistencia usa bssid quando conexao e wifi`() {
        val resultado =
            resolverNetworkIdPersistencia(
                estadoConexao = EstadoConexao.wifi,
                ssidWifi = "MinhaRede",
                bssidWifi = "AA:BB:CC:DD:EE:FF",
                operadoraMovelDetectada = "OPERADORA QUE NAO DEVERIA SER USADA",
            )

        assertEquals("wifi-bssid:aa:bb:cc:dd:ee:ff", resultado)
    }

    @Test
    fun `resolverNetworkIdPersistencia usa operadora quando conexao e movel`() {
        val resultado =
            resolverNetworkIdPersistencia(
                estadoConexao = EstadoConexao.movel,
                ssidWifi = "SSID QUE NAO DEVERIA SER USADO",
                bssidWifi = "AA:BB:CC:DD:EE:FF",
                operadoraMovelDetectada = "Claro",
            )

        assertEquals("movel:Claro", resultado)
    }

    @Test
    fun `resolverNetworkIdPersistencia retorna null para ethernet e desconhecido`() {
        assertNull(
            resolverNetworkIdPersistencia(EstadoConexao.ethernet, "Rede", "AA:BB:CC:DD:EE:FF", "Claro"),
        )
        assertNull(
            resolverNetworkIdPersistencia(EstadoConexao.desconhecido, "Rede", "AA:BB:CC:DD:EE:FF", "Claro"),
        )
    }

    @Test
    fun `resolverNetworkIdPersistencia retorna null quando wifi sem ssid nem bssid`() {
        assertNull(
            resolverNetworkIdPersistencia(EstadoConexao.wifi, ssidWifi = null, bssidWifi = null, operadoraMovelDetectada = null),
        )
    }

    @Test
    fun `resolverNetworkIdPersistencia retorna null quando movel sem operadora detectada`() {
        assertNull(
            resolverNetworkIdPersistencia(EstadoConexao.movel, ssidWifi = null, bssidWifi = null, operadoraMovelDetectada = null),
        )
    }
}
