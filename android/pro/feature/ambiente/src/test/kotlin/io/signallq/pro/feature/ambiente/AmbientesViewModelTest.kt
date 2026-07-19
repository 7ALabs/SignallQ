package io.signallq.pro.feature.ambiente

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.signallq.pro.core.database.ambiente.AmbienteEntity
import io.signallq.pro.core.database.ambiente.AmbienteRepository
import io.signallq.pro.core.database.medicao.MedicaoProRepository
import io.signallq.pro.core.database.visita.VisitaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Cobre o criterio de aceite "exclusao bloqueada por medicao associada" (handoff Fase 2,
 * #1161) -- o achado real que motivou este teste: sem ele, seria facil regredir a checagem
 * silenciosamente.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AmbientesViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    private val ambienteRepository = mockk<AmbienteRepository>(relaxed = true)
    private val medicaoProRepository = mockk<MedicaoProRepository>()
    private val visitaRepository = mockk<VisitaRepository>(relaxed = true)

    private val ambiente = AmbienteEntity(id = "ambiente-1", visitaId = "visita-1", nome = "Sala", criadoEmEpochMs = 1L)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { ambienteRepository.observarPorVisita(any()) } returns MutableStateFlow(emptyList())
        coEvery { ambienteRepository.buscarPorId("ambiente-1") } returns ambiente
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun criarViewModel() =
        AmbientesViewModel(
            savedStateHandle = SavedStateHandle(mapOf("visitaId" to "visita-1")),
            ambienteRepository = ambienteRepository,
            medicaoProRepository = medicaoProRepository,
            visitaRepository = visitaRepository,
        )

    @Test
    fun `exclusao e bloqueada quando ambiente tem medicao valida`() =
        runTest(dispatcher) {
            coEvery { medicaoProRepository.temMedicaoValida("ambiente-1") } returns true
            val viewModel = criarViewModel()
            val job = launch { viewModel.uiState.collect {} }

            viewModel.excluirAmbiente("ambiente-1")

            assertEquals(
                "Este ambiente tem medicoes registradas e nao pode ser excluido.",
                viewModel.uiState.value.erroExclusaoBloqueada,
            )
            coVerify(exactly = 0) { ambienteRepository.excluir(any()) }
            job.cancel()
        }

    @Test
    fun `exclusao acontece quando ambiente nao tem medicao valida`() =
        runTest(dispatcher) {
            coEvery { medicaoProRepository.temMedicaoValida("ambiente-1") } returns false
            val viewModel = criarViewModel()
            val job = launch { viewModel.uiState.collect {} }

            viewModel.excluirAmbiente("ambiente-1")

            assertNull(viewModel.uiState.value.erroExclusaoBloqueada)
            coVerify { ambienteRepository.excluir(ambiente) }
            job.cancel()
        }
}
