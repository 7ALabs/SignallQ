package io.linka.app.kotlin.core.database.chat

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Representa uma mensagem de chat de diagnóstico IA persistida no Room.
 *
 * Tabela: chat_messages
 * FK para chat_sessions com ON DELETE CASCADE (apagar sessão apaga mensagens).
 * Index simples em sessionId e composto (sessionId, createdAtEpochMs) para listar
 * mensagens em ordem cronológica por sessão.
 */
@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sessionId", "createdAtEpochMs"]),
    ],
)
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    /** Valores: user | assistant | system */
    val role: String,
    val content: String,
    val createdAtEpochMs: Long,
    /** Valores: sending | streaming | completed | failed */
    val status: String,
    /** JSON com modelName, diagnosticType, isLocalMessage, errorCode */
    val metadataJson: String?,
)
