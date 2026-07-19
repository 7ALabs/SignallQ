package io.signallq.pro.feature.cliente

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.signallq.pro.core.database.cliente.ClienteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Cobre o caso feliz (cliente salvo com nome/endereco) e o negativo (nome vazio bloqueia
 * sem chamar o repositorio) do cadastro rapido -- issue #1166: o cliente precisa nascer com
 * um local associado, e [ClienteRepository.criarCliente] agora exige o parametro `endereco`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NovoClienteViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val clienteRepository = mockk<ClienteRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `salvar cria cliente quando nome preenchido, endereco opcional em branco`() =
        runTest {
            coEvery {
                clienteRepository.criarCliente(nome = "Maria Cliente", telefone = null, endereco = "")
            } returns "cliente-1"

            val viewModel = NovoClienteViewModel(clienteRepository)
            viewModel.atualizarNome("Maria Cliente")
            viewModel.salvar()

            assertEquals("cliente-1", viewModel.uiState.value.clienteIdCriado)
            coVerify { clienteRepository.criarCliente(nome = "Maria Cliente", telefone = null, endereco = "") }
        }

    @Test
    fun `salvar repassa endereco preenchido para criarCliente`() =
        runTest {
            coEvery {
                clienteRepository.criarCliente(nome = "Joao Cliente", telefone = "11988887777", endereco = "Rua A, 123")
            } returns "cliente-2"

            val viewModel = NovoClienteViewModel(clienteRepository)
            viewModel.atualizarNome("Joao Cliente")
            viewModel.atualizarTelefone("11988887777")
            viewModel.atualizarEndereco("Rua A, 123")
            viewModel.salvar()

            assertEquals("cliente-2", viewModel.uiState.value.clienteIdCriado)
        }

    @Test
    fun `salvar bloqueia e nao chama repositorio quando nome vazio`() =
        runTest {
            val viewModel = NovoClienteViewModel(clienteRepository)
            viewModel.atualizarEndereco("Rua A, 123")

            viewModel.salvar()

            assertTrue(viewModel.uiState.value.erroNomeVazio)
            assertNull(viewModel.uiState.value.clienteIdCriado)
            coVerify(exactly = 0) { clienteRepository.criarCliente(any(), any(), any()) }
        }
}
