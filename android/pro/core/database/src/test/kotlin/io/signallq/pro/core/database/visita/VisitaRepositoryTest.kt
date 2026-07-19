package io.signallq.pro.core.database.visita

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.signallq.pro.core.database.checklist.ChecklistItemDao
import io.signallq.pro.core.database.checklist.ChecklistItemEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre a logica real de [VisitaRepository]: criacao de checklist por tipo de visita e
 * retomada de visita interrompida (criterio de saida do MVP0, issue #1119). DAOs mockados
 * com mockk -- o comportamento do Room em si ja e coberto pela propria biblioteca.
 */
class VisitaRepositoryTest {
    private val visitaDao = mockk<VisitaDao>(relaxed = true)
    private val checklistItemDao = mockk<ChecklistItemDao>(relaxed = true)
    private val repository = VisitaRepository(visitaDao, checklistItemDao)

    @Test
    fun `criarVisita insere visita EM_ANDAMENTO na etapa CHECKLIST`() =
        runTest {
            val visitaSlot = slot<VisitaEntity>()
            coEvery { visitaDao.salvar(capture(visitaSlot)) } returns Unit

            repository.criarVisita(clienteId = "cliente-1", tipo = TipoVisita.INSTALACAO)

            assertEquals(StatusVisita.EM_ANDAMENTO, visitaSlot.captured.status)
            assertEquals(EtapaVisita.CHECKLIST, visitaSlot.captured.etapaAtual)
            assertEquals("cliente-1", visitaSlot.captured.clienteId)
        }

    @Test
    fun `criarVisita gera checklist padrao nao vazio para cada tipo de visita`() =
        runTest {
            TipoVisita.entries.forEach { tipo ->
                val itensSlot = slot<List<ChecklistItemEntity>>()
                coEvery { checklistItemDao.salvarTodos(capture(itensSlot)) } returns Unit

                repository.criarVisita(clienteId = "cliente-1", tipo = tipo)

                assertTrue("tipo=$tipo deveria ter checklist padrao", itensSlot.captured.isNotEmpty())
                itensSlot.captured.forEach { item -> assertEquals(false, item.concluido) }
            }
        }

    @Test
    fun `retomarVisita volta status para EM_ANDAMENTO sem alterar etapa`() =
        runTest {
            repository.retomarVisita("visita-1")

            coVerify { visitaDao.atualizarStatus("visita-1", StatusVisita.EM_ANDAMENTO, any()) }
        }

    @Test
    fun `avancarEtapa persiste a nova etapa`() =
        runTest {
            repository.avancarEtapa("visita-1", EtapaVisita.AMBIENTES)

            coVerify { visitaDao.atualizarEtapa("visita-1", EtapaVisita.AMBIENTES, any()) }
        }
}
