package io.signallq.app

import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.ui.FiltroConexaoHistorico
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Testa a logica de filtro do historicoFiltrado e operadorasDisponiveisHistorico
 * sem instanciar o MainViewModel completo (dependencias Hilt/AndroidViewModel
 * sao incompativeis com unit tests puros).
 *
 * Reproduz o combine exato do MainViewModel para garantir que a logica esta correta.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelHistoricoTest {
    // ── helpers ────────────────────────────────────────────────────────────────

    private fun medicao(
        connectionType: String,
        operadoraMovel: String? = null,
        fonte: String? = null,
    ) = MedicaoEntity(
        id = UUID.randomUUID().toString(),
        timestampEpochMs = System.currentTimeMillis(),
        connectionType = connectionType,
        connectionTypeStart = null,
        connectionTypeEnd = null,
        contaminado = false,
        speedtestMode = null,
        specVersion = null,
        downloadMbps = null,
        uploadMbps = null,
        latencyMs = null,
        jitterMs = null,
        perdaPercentual = null,
        bufferbloatMs = null,
        packetLossSource = null,
        vereditoStreaming = null,
        vereditoGamer = null,
        vereditoVideoChamada = null,
        gargaloPrimario = null,
        fonte = fonte,
        operadoraMovel = operadoraMovel,
    )

    /**
     * Recria o combine do MainViewModel — mesma logica, testavel de forma isolada.
     */
    private suspend fun filtrar(
        lista: List<MedicaoEntity>,
        filtroConexao: FiltroConexaoHistorico,
        filtroOp: String?,
    ): List<MedicaoEntity> {
        val historicoFlow = MutableStateFlow(lista)
        val filtroConexaoFlow = MutableStateFlow(filtroConexao)
        val filtroOpFlow = MutableStateFlow(filtroOp)

        return combine(
            historicoFlow,
            filtroConexaoFlow,
            filtroOpFlow,
        ) { medicoes, fc, op ->
            medicoes
                // #1096 -- medicoes sinteticas do MonitoramentoWorker (fonte="monitor")
                // nao devem aparecer no Historico visivel.
                .filter { m -> m.fonte != "monitor" }
                .filter { m ->
                    when (fc) {
                        FiltroConexaoHistorico.TODOS -> true
                        FiltroConexaoHistorico.WIFI -> m.connectionType == "wifi"
                        FiltroConexaoHistorico.MOVEL -> m.connectionType == EstadoConexao.movel.name
                    }
                }.filter { m -> op == null || m.operadoraMovel == op }
        }.distinctUntilChanged().first()
    }

    private fun operadorasDisponiveis(lista: List<MedicaoEntity>): List<String> =
        lista
            .filter { it.connectionType == EstadoConexao.movel.name }
            .mapNotNull { it.operadoraMovel?.trim()?.ifBlank { null } }
            .distinct()
            .sorted()

    // ── dados de apoio ─────────────────────────────────────────────────────────

    private val listaCompleta =
        listOf(
            medicao("wifi"),
            medicao("wifi"),
            medicao(EstadoConexao.movel.name, "Claro"),
            medicao(EstadoConexao.movel.name, "Vivo"),
            medicao(EstadoConexao.movel.name, "Claro"),
        )

    // ── testes de historicoFiltrado ────────────────────────────────────────────

    @Test
    fun `historicoFiltrado com filtro WIFI retorna apenas items com connectionType wifi`() =
        runTest {
            val resultado = filtrar(listaCompleta, FiltroConexaoHistorico.WIFI, null)
            assertTrue(resultado.isNotEmpty())
            assertTrue(resultado.all { it.connectionType == "wifi" })
            assertEquals(2, resultado.size)
        }

    @Test
    fun `historicoFiltrado com filtro MOVEL retorna apenas items com connectionType cellular`() =
        runTest {
            val resultado = filtrar(listaCompleta, FiltroConexaoHistorico.MOVEL, null)
            assertTrue(resultado.isNotEmpty())
            assertTrue(resultado.all { it.connectionType == EstadoConexao.movel.name })
            assertEquals(3, resultado.size)
        }

    @Test
    fun `historicoFiltrado com filtro TODOS retorna todos os items`() =
        runTest {
            val resultado = filtrar(listaCompleta, FiltroConexaoHistorico.TODOS, null)
            assertEquals(listaCompleta.size, resultado.size)
        }

    @Test
    fun `historicoFiltrado com filtro TODOS exclui medicoes sinteticas do monitoramento passivo`() =
        runTest {
            val lista =
                listaCompleta +
                    listOf(
                        medicao("wifi", fonte = "monitor"),
                        medicao(EstadoConexao.movel.name, fonte = "monitor"),
                    )
            val resultado = filtrar(lista, FiltroConexaoHistorico.TODOS, null)
            assertEquals(listaCompleta.size, resultado.size)
            assertTrue(resultado.none { it.fonte == "monitor" })
        }

    @Test
    fun `historicoFiltrado com operadora Claro filtra apenas items operadoraMovel Claro`() =
        runTest {
            val resultado = filtrar(listaCompleta, FiltroConexaoHistorico.MOVEL, "Claro")
            assertTrue(resultado.isNotEmpty())
            assertTrue(resultado.all { it.operadoraMovel == "Claro" })
            assertEquals(2, resultado.size)
        }

    // ── testes de operadorasDisponiveisHistorico ───────────────────────────────

    @Test
    fun `operadorasDisponiveisHistorico retorna lista unica de operadoras de items cellular`() {
        val operadoras = operadorasDisponiveis(listaCompleta)
        assertEquals(listOf("Claro", "Vivo"), operadoras)
    }

    @Test
    fun `operadorasDisponiveisHistorico com lista vazia retorna lista vazia`() {
        val operadoras = operadorasDisponiveis(emptyList())
        assertTrue(operadoras.isEmpty())
    }

    @Test
    fun `operadorasDisponiveisHistorico ignora items wifi`() {
        val lista =
            listOf(
                medicao("wifi"),
                medicao(EstadoConexao.movel.name, "Tim"),
            )
        val operadoras = operadorasDisponiveis(lista)
        assertEquals(listOf("Tim"), operadoras)
    }

    @Test
    fun `operadorasDisponiveisHistorico ignora operadora nula ou em branco`() {
        val lista =
            listOf(
                medicao(EstadoConexao.movel.name, null),
                medicao(EstadoConexao.movel.name, "  "),
                medicao(EstadoConexao.movel.name, "Nextel"),
            )
        val operadoras = operadorasDisponiveis(lista)
        assertEquals(listOf("Nextel"), operadoras)
    }
}
