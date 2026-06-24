package io.veloo.app.feature.diagnostico.chat

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.veloo.app.core.database.SignallQDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testes de instrumentação de [ChatDiagnosticoIaRepository].
 *
 * Usa banco Room em memória ([allowMainThreadQueries] + [inMemoryDatabaseBuilder]) para
 * isolamento total entre testes — sem estado residual em disco.
 *
 * Cobre: CRUD, mapping Entity↔Domain, cascade delete, salvarMensagem atualiza
 * atualizadoEmEpochMs da sessão, e derivarTituloDe.
 */
@RunWith(AndroidJUnit4::class)
class ChatDiagnosticoIaRepositoryTest {

    private lateinit var db: SignallQDatabase
    private lateinit var repo: ChatDiagnosticoIaRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SignallQDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
        repo = ChatDiagnosticoIaRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // -------------------------------------------------------------------------
    // criarSessaoVazia
    // -------------------------------------------------------------------------

    @Test
    fun criarSessaoVazia_geraUuidValidoETimestampsCorretos() = runTest {
        val antes = System.currentTimeMillis()
        val sessao = repo.criarSessaoVazia()
        val depois = System.currentTimeMillis()

        // UUID: 36 chars, formato 8-4-4-4-12
        assertTrue("ID deve ter 36 chars", sessao.id.length == 36)
        assertTrue("ID deve conter hífens em posições corretas", sessao.id[8] == '-')

        assertTrue("criadoEmEpochMs deve ser >= antes", sessao.criadoEmEpochMs >= antes)
        assertTrue("criadoEmEpochMs deve ser <= depois", sessao.criadoEmEpochMs <= depois)
        assertEquals(
            "criadoEmEpochMs deve ser igual a atualizadoEmEpochMs na criação",
            sessao.criadoEmEpochMs,
            sessao.atualizadoEmEpochMs,
        )
        assertEquals(StatusSessao.ativa, sessao.status)
        assertEquals("Nova conversa", sessao.titulo)
    }

    @Test
    fun criarSessaoVazia_persisteNoBanco() = runTest {
        val sessao = repo.criarSessaoVazia("Minha sessão")
        val recuperada = repo.observarSessao(sessao.id).first()

        assertNotNull(recuperada)
        assertEquals(sessao.id, recuperada!!.id)
        assertEquals("Minha sessão", recuperada.titulo)
        assertEquals(StatusSessao.ativa, recuperada.status)
    }

    // -------------------------------------------------------------------------
    // salvarMensagem — atualiza atualizadoEmEpochMs da sessão
    // -------------------------------------------------------------------------

    @Test
    fun salvarMensagem_atualizaAtualizadoEmEpochMsDaSessao() = runTest {
        val sessao = repo.criarSessaoVazia()
        val timestampOriginal = sessao.atualizadoEmEpochMs

        // Pequena espera para garantir timestamp diferente
        Thread.sleep(5)

        val mensagem = ChatMensagem(
            id = "msg-1",
            sessionId = sessao.id,
            papel = PapelChatMensagem.usuario,
            conteudo = "Teste de conexão",
            criadoEmEpochMs = System.currentTimeMillis(),
            status = StatusChatMensagem.concluido,
        )
        repo.salvarMensagem(mensagem)

        val sessaoAtualizada = repo.observarSessao(sessao.id).first()
        assertNotNull(sessaoAtualizada)
        assertTrue(
            "atualizadoEmEpochMs deve ser >= ao timestamp da mensagem",
            sessaoAtualizada!!.atualizadoEmEpochMs >= mensagem.criadoEmEpochMs,
        )
        assertTrue(
            "atualizadoEmEpochMs deve ter sido atualizado",
            sessaoAtualizada.atualizadoEmEpochMs > timestampOriginal,
        )
    }

    // -------------------------------------------------------------------------
    // Mapping Entity ↔ Domain — todos os campos preservados
    // -------------------------------------------------------------------------

    @Test
    fun mappingEntityDomain_preservaTodosOsCampos() = runTest {
        val sessao = repo.criarSessaoVazia()
        val mensagem = ChatMensagem(
            id = "msg-completa",
            sessionId = sessao.id,
            papel = PapelChatMensagem.assistente,
            conteudo = "Análise concluída.",
            criadoEmEpochMs = 1_700_000_000_000L,
            status = StatusChatMensagem.concluido,
            nomeModelo = "Gemma 4 26B",
            tipoDiagnostico = TipoDiagnostico.ultimoTeste,
            isLocal = false,
            codigoErro = null,
        )
        repo.salvarMensagem(mensagem)

        val mensagens = repo.observarMensagens(sessao.id).first()
        assertEquals(1, mensagens.size)
        val recuperada = mensagens[0]

        assertEquals(mensagem.id, recuperada.id)
        assertEquals(mensagem.sessionId, recuperada.sessionId)
        assertEquals(PapelChatMensagem.assistente, recuperada.papel)
        assertEquals("Análise concluída.", recuperada.conteudo)
        assertEquals(1_700_000_000_000L, recuperada.criadoEmEpochMs)
        assertEquals(StatusChatMensagem.concluido, recuperada.status)
        assertEquals("Gemma 4 26B", recuperada.nomeModelo)
        assertEquals(TipoDiagnostico.ultimoTeste, recuperada.tipoDiagnostico)
        assertFalse(recuperada.isLocal)
        assertNull(recuperada.codigoErro)
    }

    @Test
    fun mappingEntityDomain_preservaCamposOpcionais_isLocal_codigoErro() = runTest {
        val sessao = repo.criarSessaoVazia()
        val mensagem = ChatMensagem(
            id = "msg-erro",
            sessionId = sessao.id,
            papel = PapelChatMensagem.sistema,
            conteudo = "Erro ao processar.",
            criadoEmEpochMs = System.currentTimeMillis(),
            status = StatusChatMensagem.falhou,
            isLocal = true,
            codigoErro = "NETWORK_ERROR",
        )
        repo.salvarMensagem(mensagem)

        val recuperada = repo.observarMensagens(sessao.id).first()[0]
        assertTrue(recuperada.isLocal)
        assertEquals("NETWORK_ERROR", recuperada.codigoErro)
        assertEquals(PapelChatMensagem.sistema, recuperada.papel)
        assertEquals(StatusChatMensagem.falhou, recuperada.status)
    }

    @Test
    fun mappingEntityDomain_todosEnumsPapel_roundTrip() = runTest {
        val sessao = repo.criarSessaoVazia()
        val papeis = listOf(
            PapelChatMensagem.usuario,
            PapelChatMensagem.assistente,
            PapelChatMensagem.sistema,
        )
        papeis.forEachIndexed { i, papel ->
            val msg = ChatMensagem(
                id = "msg-papel-$i",
                sessionId = sessao.id,
                papel = papel,
                conteudo = "conteudo $i",
                criadoEmEpochMs = System.currentTimeMillis() + i,
                status = StatusChatMensagem.concluido,
            )
            repo.salvarMensagem(msg)
        }

        val recuperadas = repo.observarMensagens(sessao.id).first()
        assertEquals(papeis.size, recuperadas.size)
        papeis.forEachIndexed { i, papel ->
            assertEquals("Papel $papel não fez round-trip", papel, recuperadas[i].papel)
        }
    }

    @Test
    fun mappingEntityDomain_todosEnumsStatus_roundTrip() = runTest {
        val sessao = repo.criarSessaoVazia()
        val statuses = listOf(
            StatusChatMensagem.enviando,
            StatusChatMensagem.streaming,
            StatusChatMensagem.concluido,
            StatusChatMensagem.falhou,
        )
        statuses.forEachIndexed { i, status ->
            val msg = ChatMensagem(
                id = "msg-status-$i",
                sessionId = sessao.id,
                papel = PapelChatMensagem.usuario,
                conteudo = "conteudo $i",
                criadoEmEpochMs = System.currentTimeMillis() + i,
                status = status,
            )
            repo.salvarMensagem(msg)
        }

        val recuperadas = repo.observarMensagens(sessao.id).first()
        assertEquals(statuses.size, recuperadas.size)
        statuses.forEachIndexed { i, status ->
            assertEquals("Status $status não fez round-trip", status, recuperadas[i].status)
        }
    }

    @Test
    fun mappingEntityDomain_todosEnumsTipoDiagnostico_roundTrip() = runTest {
        val sessao = repo.criarSessaoVazia()
        val tipos = listOf(
            TipoDiagnostico.ultimoTeste,
            TipoDiagnostico.novoTeste,
            TipoDiagnostico.historico,
        )
        tipos.forEachIndexed { i, tipo ->
            val msg = ChatMensagem(
                id = "msg-tipo-$i",
                sessionId = sessao.id,
                papel = PapelChatMensagem.usuario,
                conteudo = "conteudo $i",
                criadoEmEpochMs = System.currentTimeMillis() + i,
                status = StatusChatMensagem.concluido,
                tipoDiagnostico = tipo,
            )
            repo.salvarMensagem(msg)
        }

        val recuperadas = repo.observarMensagens(sessao.id).first()
        assertEquals(tipos.size, recuperadas.size)
        tipos.forEachIndexed { i, tipo ->
            assertEquals("TipoDiagnostico $tipo não fez round-trip", tipo, recuperadas[i].tipoDiagnostico)
        }
    }

    // -------------------------------------------------------------------------
    // apagarSessao — FK CASCADE remove mensagens
    // -------------------------------------------------------------------------

    @Test
    fun apagarSessao_removeSessaoEMensagens_cascade() = runTest {
        val sessao = repo.criarSessaoVazia()
        val sessaoId = sessao.id

        repeat(3) { i ->
            repo.salvarMensagem(
                ChatMensagem(
                    id = "msg-del-$i",
                    sessionId = sessaoId,
                    papel = PapelChatMensagem.usuario,
                    conteudo = "mensagem $i",
                    criadoEmEpochMs = System.currentTimeMillis() + i,
                    status = StatusChatMensagem.concluido,
                ),
            )
        }
        assertEquals(3, repo.observarMensagens(sessaoId).first().size)

        repo.apagarSessao(sessaoId)

        assertNull(repo.observarSessao(sessaoId).first())
        assertEquals(0, repo.observarMensagens(sessaoId).first().size)
    }

    @Test
    fun apagarSessao_naoAfetaOutrasSessoes() = runTest {
        val sessao1 = repo.criarSessaoVazia("Sessão 1")
        val sessao2 = repo.criarSessaoVazia("Sessão 2")

        repo.salvarMensagem(
            ChatMensagem(
                id = "msg-s2",
                sessionId = sessao2.id,
                papel = PapelChatMensagem.usuario,
                conteudo = "permanece",
                criadoEmEpochMs = System.currentTimeMillis(),
                status = StatusChatMensagem.concluido,
            ),
        )

        repo.apagarSessao(sessao1.id)

        assertNull(repo.observarSessao(sessao1.id).first())
        assertNotNull(repo.observarSessao(sessao2.id).first())
        assertEquals(1, repo.observarMensagens(sessao2.id).first().size)
    }

    // -------------------------------------------------------------------------
    // derivarTituloDe
    // -------------------------------------------------------------------------

    @Test
    fun derivarTituloDe_mensagemCurta_retornaInteira() {
        val titulo = repo.derivarTituloDe("Minha conexão está lenta")
        assertEquals("Minha conexão está lenta", titulo)
    }

    @Test
    fun derivarTituloDe_mensagemExatamente40Chars_retornaInteira() {
        val msg = "a".repeat(40)
        val titulo = repo.derivarTituloDe(msg)
        assertEquals(msg, titulo)
    }

    @Test
    fun derivarTituloDe_mensagemMaisDe40Chars_truncaEAdicionaEticpse() {
        val msg = "Minha internet está muito lenta hoje e eu quero entender o motivo"
        val titulo = repo.derivarTituloDe(msg)
        assertTrue("Título deve terminar com …", titulo.endsWith("…"))
        assertTrue("Título sem … deve ter <= 40 chars", titulo.dropLast(1).length <= 40)
    }

    @Test
    fun derivarTituloDe_mensagemComWhitespaceNoFinal_aplicaTrimEnd() {
        val msg = "Verificar velocidade   "
        val titulo = repo.derivarTituloDe(msg)
        assertFalse("Título não deve ter espaços no final", titulo.endsWith(" "))
        assertEquals("Verificar velocidade", titulo)
    }

    @Test
    fun derivarTituloDe_mensagemLongaComEspacosNoFinal_truncaDepoisDeTrimEnd() {
        // 42 chars reais + 3 espaços = 45 chars total, mas trim remove espaços
        // Resultado: se os espaços estão depois do char 40, trim pode encurtar
        val msg = "Este diagnóstico é sobre minha rede lenta   "
        val titulo = repo.derivarTituloDe(msg)
        assertFalse("Título não deve terminar com espaço", titulo.endsWith(" "))
    }

    // -------------------------------------------------------------------------
    // observarSessoes — ordenação e atualização reativa
    // -------------------------------------------------------------------------

    @Test
    fun observarSessoes_retornaTodasAsSessoes() = runTest {
        val s1 = repo.criarSessaoVazia("Sessão A")
        val s2 = repo.criarSessaoVazia("Sessão B")

        val sessoes = repo.observarSessoes().first()
        val ids = sessoes.map { it.id }

        assertTrue(ids.contains(s1.id))
        assertTrue(ids.contains(s2.id))
    }

    // -------------------------------------------------------------------------
    // renomearSessao
    // -------------------------------------------------------------------------

    @Test
    fun renomearSessao_atualizaTituloETimestamp() = runTest {
        val sessao = repo.criarSessaoVazia("Título original")
        Thread.sleep(5)

        repo.renomearSessao(sessao.id, "Título novo")

        val atualizada = repo.observarSessao(sessao.id).first()
        assertNotNull(atualizada)
        assertEquals("Título novo", atualizada!!.titulo)
        assertTrue(atualizada.atualizadoEmEpochMs > sessao.atualizadoEmEpochMs)
    }
}
