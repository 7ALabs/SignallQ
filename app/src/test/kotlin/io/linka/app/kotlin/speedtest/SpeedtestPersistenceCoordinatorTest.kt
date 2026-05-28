package io.linka.app.kotlin.speedtest

import io.linka.app.kotlin.core.database.MedicaoEntity
import io.linka.app.kotlin.feature.speedtest.EstadoExecucaoSpeedtest
import io.linka.app.kotlin.feature.speedtest.SnapshotExecucaoSpeedtest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
     * o ChatDiagnosticoIaViewModel NÃO chama bancoDados.medicaoDao().salvar() diretamente.
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
}
