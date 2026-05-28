package io.linka.app.kotlin.core.database.chat

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.linka.app.kotlin.core.database.ApelidoDispositivoEntity
import io.linka.app.kotlin.core.database.LinkaDatabase
import io.linka.app.kotlin.core.database.MedicaoEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatSessionDaoTest {

    private lateinit var db: LinkaDatabase
    private lateinit var dao: ChatSessionDao

    @Before
    fun criarBanco() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LinkaDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.chatSessionDao()
    }

    @After
    fun fecharBanco() {
        db.close()
    }

    private fun sessao(
        id: String = "sessao-1",
        titulo: String = "Sessão de teste",
        criadoEm: Long = 1_000L,
        atualizadoEm: Long = 1_000L,
        status: String = "active",
    ) = ChatSessionEntity(
        id = id,
        titulo = titulo,
        criadoEmEpochMs = criadoEm,
        atualizadoEmEpochMs = atualizadoEm,
        status = status,
        tipoDiagnostico = null,
        nomeModelo = null,
        diagnosticoPayloadJson = null,
    )

    private fun mensagem(
        id: String = "msg-1",
        sessionId: String = "sessao-1",
        role: String = "user",
        content: String = "Olá",
        criadoEm: Long = 1_000L,
        status: String = "completed",
    ) = ChatMessageEntity(
        id = id,
        sessionId = sessionId,
        role = role,
        content = content,
        createdAtEpochMs = criadoEm,
        status = status,
        metadataJson = null,
    )

    @Test
    fun inserirSessao_leDevolvo() = runTest {
        dao.salvarSessao(sessao())

        val resultado = dao.observarSessao("sessao-1").first()
        assertNotNull(resultado)
        assertEquals("sessao-1", resultado!!.id)
        assertEquals("Sessão de teste", resultado.titulo)
        assertEquals("active", resultado.status)
    }

    @Test
    fun inserirMensagem_leDevolvo() = runTest {
        dao.salvarSessao(sessao())
        dao.salvarMensagem(mensagem())

        val mensagens = dao.observarMensagens("sessao-1").first()
        assertEquals(1, mensagens.size)
        assertEquals("msg-1", mensagens[0].id)
        assertEquals("user", mensagens[0].role)
        assertEquals("Olá", mensagens[0].content)
    }

    @Test
    fun apagarSessao_mensagensCascateiam() = runTest {
        dao.salvarSessao(sessao())
        dao.salvarMensagem(mensagem(id = "msg-1"))
        dao.salvarMensagem(mensagem(id = "msg-2"))

        // Confirmar que mensagens existem
        val antes = dao.observarMensagens("sessao-1").first()
        assertEquals(2, antes.size)

        // Apagar sessão — FK CASCADE deve apagar mensagens
        dao.apagarSessao("sessao-1")

        val depois = dao.observarMensagens("sessao-1").first()
        assertTrue(depois.isEmpty())

        val sessaoDepois = dao.observarSessao("sessao-1").first()
        assertNull(sessaoDepois)
    }

    @Test
    fun renomearSessao_atualizaTituloETimestamp() = runTest {
        dao.salvarSessao(sessao(atualizadoEm = 1_000L))

        val novoTimestamp = 9_999L
        dao.renomearSessao("sessao-1", "Novo título", novoTimestamp)

        val resultado = dao.observarSessao("sessao-1").first()
        assertNotNull(resultado)
        assertEquals("Novo título", resultado!!.titulo)
        assertEquals(novoTimestamp, resultado.atualizadoEmEpochMs)
    }

    @Test
    fun observarSessoes_ordenadoPorAtualizadoEmDesc() = runTest {
        dao.salvarSessao(sessao(id = "s1", titulo = "Antiga", criadoEm = 100L, atualizadoEm = 100L))
        dao.salvarSessao(sessao(id = "s2", titulo = "Recente", criadoEm = 200L, atualizadoEm = 900L))
        dao.salvarSessao(sessao(id = "s3", titulo = "Meio", criadoEm = 150L, atualizadoEm = 500L))

        val sessoes = dao.observarSessoes().first()
        assertEquals(3, sessoes.size)
        // Deve vir DESC por atualizadoEmEpochMs: s2 (900), s3 (500), s1 (100)
        assertEquals("s2", sessoes[0].id)
        assertEquals("s3", sessoes[1].id)
        assertEquals("s1", sessoes[2].id)
    }

    @Test
    fun observarMensagens_ordenadoPorCreatedAtAsc() = runTest {
        dao.salvarSessao(sessao())
        dao.salvarMensagem(mensagem(id = "msg-3", criadoEm = 300L))
        dao.salvarMensagem(mensagem(id = "msg-1", criadoEm = 100L))
        dao.salvarMensagem(mensagem(id = "msg-2", criadoEm = 200L))

        val mensagens = dao.observarMensagens("sessao-1").first()
        assertEquals(3, mensagens.size)
        // Deve vir ASC por createdAtEpochMs: msg-1, msg-2, msg-3
        assertEquals("msg-1", mensagens[0].id)
        assertEquals("msg-2", mensagens[1].id)
        assertEquals("msg-3", mensagens[2].id)
    }

    @Test
    fun contarSessoes_retornaNumeroCorreto() = runTest {
        assertEquals(0, dao.contarSessoes())

        dao.salvarSessao(sessao(id = "s1"))
        assertEquals(1, dao.contarSessoes())

        dao.salvarSessao(sessao(id = "s2"))
        assertEquals(2, dao.contarSessoes())

        dao.apagarSessao("s1")
        assertEquals(1, dao.contarSessoes())
    }

    @Test
    fun atualizarMensagem_persisteAlteracoes() = runTest {
        dao.salvarSessao(sessao())
        val original = mensagem(status = "streaming")
        dao.salvarMensagem(original)

        val atualizada = original.copy(status = "completed", content = "Resposta completa")
        dao.atualizarMensagem(atualizada)

        val mensagens = dao.observarMensagens("sessao-1").first()
        assertEquals(1, mensagens.size)
        assertEquals("completed", mensagens[0].status)
        assertEquals("Resposta completa", mensagens[0].content)
    }
}
