package io.veloo.app.core.database.chat

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Representa uma sessão de chat de diagnóstico IA persistida no Room.
 *
 * Tabela: chat_sessions
 * Index em atualizadoEmEpochMs DESC para listagem no drawer (mais recentes primeiro).
 */
@Entity(
    tableName = "chat_sessions",
    indices = [
        Index(value = ["atualizadoEmEpochMs"]),
    ],
)
data class ChatSessionEntity(
    @PrimaryKey
    val id: String,
    val titulo: String,
    val criadoEmEpochMs: Long,
    val atualizadoEmEpochMs: Long,
    /** Valores: active | completed | failed | interrupted */
    val status: String,
    /** Valores: last_test | new_test | history — null se ainda não escolhido */
    val tipoDiagnostico: String?,
    val nomeModelo: String?,
    /** Snapshot JSON dos dados de diagnóstico usados, para reabrir sessão */
    val diagnosticoPayloadJson: String?,
    /** UUID da MedicaoEntity correspondente, para correlação no ingest. Null quando sessão não está vinculada a um diagnóstico específico. */
    val diagnosisId: String? = null,
    /** Tokens consumidos na chamada ao AI Worker. Zero enquanto não disponível. */
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
)
