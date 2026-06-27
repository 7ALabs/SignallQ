package io.veloo.app.feature.diagnostico.chat

import io.veloo.app.core.database.SignallQDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Repository de domínio para sessões e mensagens de chat de diagnóstico IA.
 *
 * Wraps [ChatSessionDao] expondo objetos de domínio ([SessaoChatDiagnostico] / [ChatMensagem]).
 * Toda escrita de mensagem também atualiza [SessaoChatDiagnostico.atualizadoEmEpochMs] da sessão
 * para que o drawer exiba as sessões mais ativas primeiro.
 *
 * Padrão de construção: recebe [SignallQDatabase] diretamente, igual a outros repositories
 * no featureDiagnostico (não usa @Provides separado em módulo Hilt).
 */
class ChatDiagnosticoIaRepository(
    private val bancoDados: SignallQDatabase,
) {
    private val dao get() = bancoDados.chatSessionDao()

    // -------------------------------------------------------------------------
    // Observe
    // -------------------------------------------------------------------------

    fun observarSessoes(): Flow<List<SessaoChatDiagnostico>> =
        dao.observarSessoes().map { lista -> lista.map { it.paraDominio() } }

    fun observarSessao(id: String): Flow<SessaoChatDiagnostico?> =
        dao.observarSessao(id).map { it?.paraDominio() }

    fun observarMensagens(sessionId: String): Flow<List<ChatMensagem>> =
        dao.observarMensagens(sessionId).map { lista -> lista.map { it.paraDominio() } }

    // -------------------------------------------------------------------------
    // Writes — sessão
    // -------------------------------------------------------------------------

    /**
     * Cria e persiste uma sessão vazia com UUID novo e timestamps atuais.
     * Status inicial: [StatusSessao.ativa].
     */
    suspend fun criarSessaoVazia(titulo: String = "Nova conversa"): SessaoChatDiagnostico {
        val agora = System.currentTimeMillis()
        val sessao = SessaoChatDiagnostico(
            id = UUID.randomUUID().toString(),
            titulo = titulo,
            criadoEmEpochMs = agora,
            atualizadoEmEpochMs = agora,
            status = StatusSessao.ativa,
        )
        dao.salvarSessao(sessao.paraEntity())
        return sessao
    }

    /** Persiste o estado completo de uma sessão (upsert via REPLACE). */
    suspend fun atualizarSessao(sessao: SessaoChatDiagnostico) {
        dao.salvarSessao(sessao.paraEntity())
    }

    /** Remove a sessão e todas as suas mensagens (FK CASCADE no Room). */
    suspend fun apagarSessao(id: String) {
        dao.apagarSessao(id)
    }

    /** Renomeia a sessão e atualiza [atualizadoEmEpochMs] para o momento atual. */
    suspend fun renomearSessao(id: String, novoTitulo: String) {
        dao.renomearSessao(id, novoTitulo, System.currentTimeMillis())
    }

    /** Vincula a sessão a um diagnóstico específico para correlação no ingest retroativo. */
    suspend fun atualizarDiagnosisId(id: String, diagnosisId: String) {
        dao.atualizarDiagnosisId(id, diagnosisId)
    }

    /** Persiste os tokens consumidos na chamada ao AI Worker. */
    suspend fun atualizarTokens(
        id: String,
        promptTokens: Int,
        completionTokens: Int,
        totalTokens: Int,
    ) {
        dao.atualizarTokens(id, promptTokens, completionTokens, totalTokens)
    }

    // -------------------------------------------------------------------------
    // Writes — mensagem
    // -------------------------------------------------------------------------

    /**
     * Salva uma mensagem e atualiza [atualizadoEmEpochMs] da sessão correspondente
     * para manter a ordenação correta no drawer (mais recentes primeiro).
     */
    suspend fun salvarMensagem(mensagem: ChatMensagem) {
        dao.salvarMensagem(mensagem.paraEntity())
        dao.atualizarAtualizadoEm(mensagem.sessionId, mensagem.criadoEmEpochMs)
    }

    /** Atualiza o conteúdo/status de uma mensagem existente (ex: ao concluir streaming). */
    suspend fun atualizarMensagem(mensagem: ChatMensagem) {
        dao.atualizarMensagem(mensagem.paraEntity())
        dao.atualizarAtualizadoEm(mensagem.sessionId, System.currentTimeMillis())
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Deriva um título de sessão a partir da primeira mensagem do usuário.
     * - Mensagem curta (≤40 chars após trimEnd): retorna inteira.
     * - Mensagem longa: trunca em 40 chars (com trimEnd no resultado) e adiciona "…".
     */
    fun derivarTituloDe(primeiraMensagem: String): String {
        val trimada = primeiraMensagem.trimEnd()
        return if (trimada.length > 40) {
            trimada.take(40).trimEnd() + "…"
        } else {
            trimada
        }
    }
}
