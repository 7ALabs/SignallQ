package io.signallq.app.feature.history

import io.signallq.app.core.database.MedicaoDao
import io.signallq.app.core.database.MedicaoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

// ---------------------------------------------------------------------------
// Fake DAO
// ---------------------------------------------------------------------------

private class FakeMedicaoDao(
    private val medicoes: List<MedicaoEntity> = emptyList(),
) : MedicaoDao {

    override suspend fun salvar(medicao: MedicaoEntity) = Unit

    override fun observarTodas(): Flow<List<MedicaoEntity>> = flowOf(medicoes)

    override fun observarUltimas(limite: Int): Flow<List<MedicaoEntity>> =
        flowOf(medicoes.take(limite))

    override fun observarPorModo(modo: String, limite: Int): Flow<List<MedicaoEntity>> =
        flowOf(medicoes.filter { it.speedtestMode == modo }.take(limite))

    override fun observarDesde(timestampMin: Long, limite: Int): Flow<List<MedicaoEntity>> =
        flowOf(medicoes.filter { it.timestampEpochMs >= timestampMin }.take(limite))

    override fun observarContaminadasDesde(timestampMin: Long, limite: Int): Flow<List<MedicaoEntity>> =
        flowOf(medicoes.filter { it.contaminado && it.timestampEpochMs >= timestampMin }.take(limite))

    override fun observarPorModoDesde(modo: String, timestampMin: Long, limite: Int): Flow<List<MedicaoEntity>> =
        flowOf(medicoes.filter { it.speedtestMode == modo && it.timestampEpochMs >= timestampMin }.take(limite))

    override fun observarFiltrado(timestampMin: Long, modo: String?, apenasContaminado: Int, limite: Int): Flow<List<MedicaoEntity>> =
        flowOf(medicoes.filter { it.timestampEpochMs >= timestampMin }.take(limite))

    override suspend fun buscarDesde(timestampMin: Long): List<MedicaoEntity> =
        medicoes.filter { it.timestampEpochMs >= timestampMin }

    override suspend fun buscarTodas(): List<MedicaoEntity> = medicoes

    override suspend fun deletarTodos() = Unit

    override suspend fun atualizarDiagnostico(id: String, texto: String?, origem: String?, problemas: String?) = Unit

    override suspend fun atualizarScore(id: String, score: Double) = Unit
}

// ---------------------------------------------------------------------------
// Helper
// ---------------------------------------------------------------------------

private fun medicaoMonitor(
    timestampEpochMs: Long,
    latenciaMs: Double?,
): MedicaoEntity = MedicaoEntity(
    id = UUID.randomUUID().toString(),
    timestampEpochMs = timestampEpochMs,
    connectionType = "monitor",
    connectionTypeStart = null,
    connectionTypeEnd = null,
    contaminado = false,
    speedtestMode = null,
    specVersion = null,
    downloadMbps = null,
    uploadMbps = null,
    latencyMs = latenciaMs,
    jitterMs = null,
    perdaPercentual = null,
    bufferbloatMs = null,
    packetLossSource = null,
    vereditoStreaming = null,
    vereditoGamer = null,
    vereditoVideoChamada = null,
    gargaloPrimario = null,
    fonte = "monitor",
)

// ---------------------------------------------------------------------------
// Testes
// ---------------------------------------------------------------------------

class UptimeChartUseCaseTest {

    @Test
    fun `retorna exatamente 336 blocos para 7 dias`() = runBlocking {
        val useCase = UptimeChartUseCase(FakeMedicaoDao(emptyList()))
        val blocos = useCase.gerar7dias()
        assertEquals("Deve retornar 336 blocos (7 dias x 48 blocos/dia)", 336, blocos.size)
    }

    @Test
    fun `blocos sem medicoes sao classificados como SEM_DADO`() = runBlocking {
        val useCase = UptimeChartUseCase(FakeMedicaoDao(emptyList()))
        val blocos = useCase.gerar7dias()
        assertTrue(
            "Todos os blocos sem medicao devem ser SEM_DADO",
            blocos.all { it.status == StatusUptime.SEM_DADO },
        )
    }

    @Test
    fun `medicoes com latencia baixa classificam bloco como OK`() = runBlocking {
        // Coloca 3 medicoes no slot mais recente (dentro dos ultimos 30 minutos)
        val agora = System.currentTimeMillis()
        val medicoes = listOf(
            medicaoMonitor(agora - 5 * 60 * 1000L, 100.0),
            medicaoMonitor(agora - 10 * 60 * 1000L, 150.0),
            medicaoMonitor(agora - 15 * 60 * 1000L, 200.0),
        )
        val useCase = UptimeChartUseCase(FakeMedicaoDao(medicoes))
        val blocos = useCase.gerar7dias()

        val ultimoBloco = blocos.last()
        assertEquals("Latencia media de 150ms deve classificar como OK", StatusUptime.OK, ultimoBloco.status)
        assertNotNull("latencyMediaMs deve estar presente", ultimoBloco.latencyMediaMs)
    }

    @Test
    fun `medicoes com latencia alta classificam bloco como LENTO`() = runBlocking {
        val agora = System.currentTimeMillis()
        val medicoes = listOf(
            medicaoMonitor(agora - 5 * 60 * 1000L, 500.0),
            medicaoMonitor(agora - 10 * 60 * 1000L, 600.0),
        )
        val useCase = UptimeChartUseCase(FakeMedicaoDao(medicoes))
        val blocos = useCase.gerar7dias()

        val ultimoBloco = blocos.last()
        assertEquals("Latencia media de 550ms deve classificar como LENTO", StatusUptime.LENTO, ultimoBloco.status)
    }

    @Test
    fun `medicoes com latencia muito alta classificam bloco como OFFLINE`() = runBlocking {
        val agora = System.currentTimeMillis()
        val medicoes = listOf(
            medicaoMonitor(agora - 5 * 60 * 1000L, 1200.0),
            medicaoMonitor(agora - 10 * 60 * 1000L, 900.0),
        )
        val useCase = UptimeChartUseCase(FakeMedicaoDao(medicoes))
        val blocos = useCase.gerar7dias()

        val ultimoBloco = blocos.last()
        assertEquals("Latencia media > 800ms deve classificar como OFFLINE", StatusUptime.OFFLINE, ultimoBloco.status)
    }

    @Test
    fun `medicoes sem latencia mensuravel classificam bloco como OFFLINE`() = runBlocking {
        val agora = System.currentTimeMillis()
        val medicoes = listOf(
            medicaoMonitor(agora - 5 * 60 * 1000L, null),
        )
        val useCase = UptimeChartUseCase(FakeMedicaoDao(medicoes))
        val blocos = useCase.gerar7dias()

        val ultimoBloco = blocos.last()
        assertEquals("Medicao sem latencia mensuravel indica OFFLINE", StatusUptime.OFFLINE, ultimoBloco.status)
    }

    @Test
    fun `blocos sao ordenados do mais antigo para o mais recente`() = runBlocking {
        val useCase = UptimeChartUseCase(FakeMedicaoDao(emptyList()))
        val blocos = useCase.gerar7dias()
        val timestamps = blocos.map { it.dataHora }
        val ordenados = timestamps.sorted()
        assertEquals("Blocos devem estar em ordem cronologica crescente", ordenados, timestamps)
    }
}
