package io.signallq.app.feature.diagnostico

import io.signallq.app.core.network.FeatureFlagProvider
import io.signallq.app.feature.diagnostico.ai.AiDiagnosisRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Testes unitarios do DiagnosticoViewModel.
 *
 * Foca no estado inicial e na logica de limpeza de DiagChat — sem depender
 * de Android runtime, HTTP ou DiagnosticRunner.
 *
 * Testes de integracao (enviarPerguntaDiagnostico com relatorio real) ficam
 * no androidTest via Robolectric.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticoViewModelTest {

    private lateinit var viewModel: DiagnosticoViewModel

    @Before
    fun setUp() {
        viewModel = DiagnosticoViewModel(
            diagnosticOrchestrator = DiagnosticOrchestrator(),
            aiRepository = AiDiagnosisRepository(
                baseUrl = "https://fake.example.com",
                isAuthorized = { false },
            ),
            featureFlags = object : FeatureFlagProvider {
                override fun isEnabled(key: String): Boolean = true
            },
        )
    }

    @Test
    fun `estado inicial do diagChatHistorico e vazio`() {
        assertTrue(viewModel.diagChatHistorico.value.isEmpty())
    }

    @Test
    fun `estado inicial do diagChatCarregando e false`() {
        assertFalse(viewModel.diagChatCarregando.value)
    }

    @Test
    fun `estado inicial do snapshotDiagnostico e idle`() {
        assertEquals(EstadoDiagnostico.idle, viewModel.snapshotDiagnostico.value.estado)
    }

    @Test
    fun `estado inicial do snapshotDiagnostico nao tem relatorio`() {
        assertNull(viewModel.snapshotDiagnostico.value.relatorio)
    }

    @Test
    fun `limparDiagChat com historico vazio nao lanca excecao`() {
        viewModel.limparDiagChat()
        assertTrue(viewModel.diagChatHistorico.value.isEmpty())
        assertFalse(viewModel.diagChatCarregando.value)
    }

    @Test
    fun `limparDiagChat pode ser chamado multiplas vezes sem erro`() {
        repeat(5) { viewModel.limparDiagChat() }
        assertTrue(viewModel.diagChatHistorico.value.isEmpty())
        assertFalse(viewModel.diagChatCarregando.value)
    }
}
