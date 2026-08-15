package io.signallq.app.core.database.chat

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatSessionDao {

    @Query("SELECT * FROM chat_sessions ORDER BY atualizadoEmEpochMs DESC")
    fun observarSessoes(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    fun observarSessao(id: String): Flow<ChatSessionEntity?>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAtEpochMs ASC")
    fun observarMensagens(sessionId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarSessao(sessao: ChatSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvarMensagem(mensagem: ChatMessageEntity)

    @Update
    suspend fun atualizarMensagem(mensagem: ChatMessageEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun apagarSessao(id: String)

    @Query("UPDATE chat_sessions SET titulo = :titulo, atualizadoEmEpochMs = :atualizadoEmEpochMs WHERE id = :id")
    suspend fun renomearSessao(
        id: String,
        titulo: String,
        atualizadoEmEpochMs: Long,
    )

    @Query("SELECT COUNT(*) FROM chat_sessions")
    suspend fun contarSessoes(): Int

    /** Atualiza apenas o campo atualizadoEmEpochMs, sem alterar outros campos. */
    @Query("UPDATE chat_sessions SET atualizadoEmEpochMs = :atualizadoEmEpochMs WHERE id = :id")
    suspend fun atualizarAtualizadoEm(id: String, atualizadoEmEpochMs: Long)

    /**
     * Retorna sessoes concluidas apos [desde] (epoch ms) para sync retroativo.
     *
     * Filtros: status = completed AND nomeModelo != null (sessoes com IA ativa).
     * Ordenadas por criadoEmEpochMs ASC para processar do mais antigo para o mais recente
     * e atualizar o checkpoint corretamente por batch.
     */
    @Query(
        "SELECT * FROM chat_sessions " +
            "WHERE criadoEmEpochMs > :desde " +
            "AND status = 'completed' " +
            "AND nomeModelo IS NOT NULL " +
            "ORDER BY criadoEmEpochMs ASC",
    )
    suspend fun buscarCompletasDesde(desde: Long): List<ChatSessionEntity>

    @Query(
        "UPDATE chat_sessions SET diagnosisId = :diagnosisId WHERE id = :id",
    )
    suspend fun atualizarDiagnosisId(id: String, diagnosisId: String)

    @Query(
        "UPDATE chat_sessions SET promptTokens = :promptTokens, " +
            "completionTokens = :completionTokens, totalTokens = :totalTokens WHERE id = :id",
    )
    suspend fun atualizarTokens(
        id: String,
        promptTokens: Int,
        completionTokens: Int,
        totalTokens: Int,
    )
}
