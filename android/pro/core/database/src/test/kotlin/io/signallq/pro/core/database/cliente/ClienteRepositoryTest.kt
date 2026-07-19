package io.signallq.pro.core.database.cliente

import androidx.room.Room
import io.mockk.coEvery
import io.mockk.mockk
import io.signallq.pro.core.database.SignallQProDatabase
import io.signallq.pro.core.database.local.LocalDao
import io.signallq.pro.core.database.local.LocalRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.IOException

/**
 * Cobre a atomicidade de [ClienteRepository.criarCliente] -- achado do Rhodolfo na PR #1167
 * (issue #1166): cliente e local eram duas escritas Room nao atomicas; se o processo morresse
 * entre as duas, o cliente ficava persistido sem local, cenario alcancavel em producao (nao
 * teorico) que quebra o invariante que `VisitaRepository.criarVisita` assume.
 *
 * Usa um banco Room real em memoria (Robolectric, nao mock) para o `ClienteDao` -- so assim e
 * possivel provar que a falha no segundo insert (local, mockado para lancar excecao) desfaz o
 * primeiro (cliente) via rollback real de transacao, nao apenas "o mock nao reclamou".
 */
@RunWith(RobolectricTestRunner::class)
class ClienteRepositoryTest {
    private val db =
        Room
            .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), SignallQProDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `criarCliente persiste cliente e local na mesma transacao`() =
        runTest {
            val localDao = mockk<LocalDao>()
            coEvery { localDao.salvar(any()) } returns Unit
            val repository = ClienteRepository(db, db.clienteDao(), LocalRepository(localDao))

            val id = repository.criarCliente(nome = "Maria Cliente", telefone = null, endereco = "Rua A, 1")

            val cliente = db.clienteDao().buscarPorId(id)
            assertEquals("Maria Cliente", cliente?.nome)
        }

    @Test
    fun `criarCliente e atomico -- falha ao criar local desfaz insercao do cliente`() =
        runTest {
            val localDaoQuebrado = mockk<LocalDao>()
            coEvery { localDaoQuebrado.salvar(any()) } throws IOException("disco cheio")
            val repository = ClienteRepository(db, db.clienteDao(), LocalRepository(localDaoQuebrado))

            var propagouFalha = false
            try {
                repository.criarCliente(nome = "Cliente Orfao", telefone = null, endereco = "")
            } catch (esperada: IOException) {
                propagouFalha = esperada.message == "disco cheio"
            }

            assertTrue("criarCliente deveria propagar a falha do segundo insert", propagouFalha)
            val clientesPersistidos = db.clienteDao().observarTodos().first()
            assertTrue(
                "cliente nao pode ficar persistido orfao sem local quando o insert do local falha",
                clientesPersistidos.isEmpty(),
            )
        }
}
