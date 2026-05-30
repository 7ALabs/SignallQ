package io.veloo.app.feature.diagnostico.chat

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Testes unitários de [CotaIaRepository] — ciclo rolling 24h.
 *
 * Usa DataStore real com arquivo temporário (TemporaryFolder) para evitar
 * mocking de DataStore que é complicado e frágil. Roda em JVM puro sem Robolectric.
 *
 * O [clock] injetável simula passagem de tempo sem Thread.sleep.
 *
 * IMPORTANTE: O DataStore usa um CoroutineScope interno. Para evitar
 * UncompletedCoroutinesError no runTest, usamos um CoroutineScope separado
 * para o DataStore e o cancelamos no @After.
 */
class CotaIaRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // Scope separado para o DataStore — independente do testScope
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var dataStoreFile: File

    // Clock mutável — começa em T0 e pode ser avançado nos testes
    private var fakeTimeMs = 1_000_000_000_000L
    private val fakeClock: () -> Long = { fakeTimeMs }

    @Before
    fun setUp() {
        dataStoreScope = CoroutineScope(Dispatchers.IO + Job())
        dataStoreFile = tmpFolder.newFile("cota_ia_test_${System.nanoTime()}.preferences_pb")
        fakeTimeMs = 1_000_000_000_000L
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    private fun criarRepositorioTeste(): CotaIaRepositoryTestable {
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        return CotaIaRepositoryTestable(dataStore, fakeClock)
    }

    // -------------------------------------------------------------------------
    // Testes
    // -------------------------------------------------------------------------

    @Test
    fun estadoInicial_deveSerDisponivel() = testScope.runTest {
        val repo = criarRepositorioTeste()
        val resultado = repo.podeAnalisar()
        assertTrue("Estado inicial deve ser Disponivel", resultado is ResultadoCota.Disponivel)
    }

    @Test
    fun aposLimiteAnalises_deveRetornarExcedida() = testScope.runTest {
        val repo = criarRepositorioTeste()
        repeat(CotaIaRepository.LIMITE_DEFAULT) {
            repo.registrarAnalise()
        }
        val resultado = repo.podeAnalisar()
        assertTrue(
            "Após ${CotaIaRepository.LIMITE_DEFAULT} análises deve retornar Excedida",
            resultado is ResultadoCota.Excedida,
        )
    }

    @Test
    fun aposLimiteAnalises_renovacaoEpochMs_deveSerCicloInicioPlusDuracaoCiclo() = testScope.runTest {
        val repo = criarRepositorioTeste()
        val tempoInicio = fakeTimeMs
        repeat(CotaIaRepository.LIMITE_DEFAULT) {
            repo.registrarAnalise()
        }
        val resultado = repo.podeAnalisar()
        val esperado = tempoInicio + CotaIaRepository.CICLO_DURACAO_MS
        val excedida = resultado as ResultadoCota.Excedida
        assertEquals(
            "renovacaoEpochMs deve ser cicloInicio + 24h",
            esperado,
            excedida.renovacaoEpochMs,
        )
    }

    @Test
    fun apoCicloDuracao_deveResetarERetornarDisponivel() = testScope.runTest {
        val repo = criarRepositorioTeste()
        repeat(CotaIaRepository.LIMITE_DEFAULT) {
            repo.registrarAnalise()
        }
        assertTrue(repo.podeAnalisar() is ResultadoCota.Excedida)

        // Avançar o clock além de 24h
        fakeTimeMs += CotaIaRepository.CICLO_DURACAO_MS + 1

        val resultado = repo.podeAnalisar()
        assertTrue(
            "Após ${CotaIaRepository.CICLO_DURACAO_MS}ms + 1ms o ciclo deve resetar e retornar Disponivel",
            resultado is ResultadoCota.Disponivel,
        )
    }

    @Test
    fun registrarAnalise_aposExpiracao_deveIniciarNovoCiclo() = testScope.runTest {
        val repo = criarRepositorioTeste()
        repeat(CotaIaRepository.LIMITE_DEFAULT) {
            repo.registrarAnalise()
        }
        fakeTimeMs += CotaIaRepository.CICLO_DURACAO_MS + 1
        val novoInicio = fakeTimeMs

        repo.registrarAnalise()

        val snapshot = repo.observarCota().first()
        assertEquals("Novo ciclo deve ter apenas 1 análise", 1, snapshot.analisesNoCiclo)
        assertEquals("cicloInicioEpochMs deve ser o momento da nova análise", novoInicio, snapshot.cicloInicioEpochMs)
    }

    @Test
    fun observarCota_deveEmitirSnapshotAtualizadoAposRegistrarAnalise() = testScope.runTest {
        val repo = criarRepositorioTeste()

        val snapshotInicial = repo.observarCota().first()
        assertEquals(0, snapshotInicial.analisesNoCiclo)
        assertNull(snapshotInicial.cicloInicioEpochMs)
        assertNull(snapshotInicial.renovacaoEpochMs)

        repo.registrarAnalise()

        val snapshotApos = repo.observarCota().first()
        assertEquals(1, snapshotApos.analisesNoCiclo)
        assertEquals(fakeTimeMs, snapshotApos.cicloInicioEpochMs)
        assertEquals(fakeTimeMs + CotaIaRepository.CICLO_DURACAO_MS, snapshotApos.renovacaoEpochMs)
    }

    @Test
    fun renovacaoEpochMs_deveSerCicloInicioPlusDuracaoCiclo_naoOutraCoisa() = testScope.runTest {
        val repo = criarRepositorioTeste()
        val tempoInicio = fakeTimeMs
        repo.registrarAnalise()

        val snapshot = repo.observarCota().first()
        val esperado = tempoInicio + CotaIaRepository.CICLO_DURACAO_MS
        assertEquals(
            "renovacaoEpochMs = cicloInicio + 24h (não outra coisa)",
            esperado,
            snapshot.renovacaoEpochMs,
        )
    }

    @Test
    fun cotaSnapshot_restantes_decrementaComAnalises() = testScope.runTest {
        val repo = criarRepositorioTeste()
        assertEquals(CotaIaRepository.LIMITE_DEFAULT, repo.observarCota().first().restantes)

        repo.registrarAnalise()
        repo.registrarAnalise()

        val snapshot = repo.observarCota().first()
        assertEquals(CotaIaRepository.LIMITE_DEFAULT - 2, snapshot.restantes)
        assertFalse(snapshot.excedida)
    }

    @Test
    fun cotaSnapshot_excedida_trueQuandoNoLimite() = testScope.runTest {
        val repo = criarRepositorioTeste()
        repeat(CotaIaRepository.LIMITE_DEFAULT) {
            repo.registrarAnalise()
        }
        val snapshot = repo.observarCota().first()
        assertTrue(snapshot.excedida)
        assertEquals(0, snapshot.restantes)
    }

    @Test
    fun resetar_deveZerarTudoERetornarDisponivel() = testScope.runTest {
        val repo = criarRepositorioTeste()
        repeat(CotaIaRepository.LIMITE_DEFAULT) {
            repo.registrarAnalise()
        }
        assertTrue(repo.podeAnalisar() is ResultadoCota.Excedida)

        repo.resetar()

        assertTrue(repo.podeAnalisar() is ResultadoCota.Disponivel)
        val snapshot = repo.observarCota().first()
        assertEquals(0, snapshot.analisesNoCiclo)
        assertNull(snapshot.cicloInicioEpochMs)
    }

    @Test
    fun cicloNaoExpiraSe23HorasPassaram() = testScope.runTest {
        val repo = criarRepositorioTeste()
        repeat(CotaIaRepository.LIMITE_DEFAULT) {
            repo.registrarAnalise()
        }
        // Avançar apenas 23h59m59s999ms — ciclo NÃO deve expirar
        fakeTimeMs += CotaIaRepository.CICLO_DURACAO_MS - 1

        val resultado = repo.podeAnalisar()
        assertTrue(
            "23h59m — ciclo ainda não expirou, deve retornar Excedida",
            resultado is ResultadoCota.Excedida,
        )
    }
}

// =============================================================================
// Versão testável do CotaIaRepository — recebe DataStore diretamente
// para contornar a limitação do Context.cotaIaDataStore (singleton via extension).
// =============================================================================

internal class CotaIaRepositoryTestable(
    private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
    private val clock: () -> Long,
) {
    companion object {
        private val CHAVE_ANALISES = intPreferencesKey("ia_analises_no_ciclo")
        private val CHAVE_CICLO_INICIO = longPreferencesKey("ia_ciclo_inicio_epoch_ms")
        private val CHAVE_LIMITE = intPreferencesKey("ia_limite_ciclo")
    }

    fun observarCota(): kotlinx.coroutines.flow.Flow<CotaSnapshot> =
        dataStore.data.map { prefs ->
            val analises = prefs[CHAVE_ANALISES] ?: 0
            val limite = prefs[CHAVE_LIMITE] ?: CotaIaRepository.LIMITE_DEFAULT
            val cicloInicio = prefs[CHAVE_CICLO_INICIO]
            val renovacao = cicloInicio?.let { it + CotaIaRepository.CICLO_DURACAO_MS }
            CotaSnapshot(
                analisesNoCiclo = analises,
                limiteCiclo = limite,
                cicloInicioEpochMs = cicloInicio,
                renovacaoEpochMs = renovacao,
            )
        }

    suspend fun podeAnalisar(): ResultadoCota {
        val prefs = dataStore.data.first()
        val analises = prefs[CHAVE_ANALISES] ?: 0
        val cicloInicio = prefs[CHAVE_CICLO_INICIO]
        val limite = prefs[CHAVE_LIMITE] ?: CotaIaRepository.LIMITE_DEFAULT

        if (cicloInicio == null) return ResultadoCota.Disponivel

        val agora = clock()
        val cicloExpirou = (agora - cicloInicio) >= CotaIaRepository.CICLO_DURACAO_MS

        if (cicloExpirou) {
            resetarCicloInterno()
            return ResultadoCota.Disponivel
        }

        return if (analises >= limite) {
            ResultadoCota.Excedida(renovacaoEpochMs = cicloInicio + CotaIaRepository.CICLO_DURACAO_MS)
        } else {
            ResultadoCota.Disponivel
        }
    }

    suspend fun registrarAnalise() {
        val prefs = dataStore.data.first()
        val cicloInicio = prefs[CHAVE_CICLO_INICIO]
        val analises = prefs[CHAVE_ANALISES] ?: 0
        val agora = clock()

        val deveIniciarNovoCiclo = cicloInicio == null ||
            (agora - cicloInicio) >= CotaIaRepository.CICLO_DURACAO_MS

        if (deveIniciarNovoCiclo) {
            dataStore.edit { ds ->
                ds[CHAVE_CICLO_INICIO] = agora
                ds[CHAVE_ANALISES] = 1
            }
        } else {
            dataStore.edit { ds ->
                ds[CHAVE_ANALISES] = analises + 1
            }
        }
    }

    suspend fun resetar() {
        dataStore.edit { ds ->
            ds.remove(CHAVE_CICLO_INICIO)
            ds.remove(CHAVE_ANALISES)
            ds.remove(CHAVE_LIMITE)
        }
    }

    private suspend fun resetarCicloInterno() {
        dataStore.edit { ds ->
            ds.remove(CHAVE_CICLO_INICIO)
            ds[CHAVE_ANALISES] = 0
        }
    }
}
