package io.veloo.app.feature.diagnostico.chat

import io.veloo.app.core.database.chat.ChatMessageEntity
import io.veloo.app.core.database.chat.ChatSessionEntity
import org.json.JSONObject

// =============================================================================
// Enums de domínio
// =============================================================================

enum class PapelChatMensagem { usuario, assistente, sistema }

enum class StatusChatMensagem { enviando, streaming, concluido, falhou }

enum class StatusSessao { ativa, concluida, falhou, interrompida }

enum class TipoDiagnostico { ultimoTeste, novoTeste, historico }

// =============================================================================
// Data classes de domínio
// =============================================================================

data class ChatMensagem(
    val id: String,
    val sessionId: String,
    val papel: PapelChatMensagem,
    val conteudo: String,
    val criadoEmEpochMs: Long,
    val status: StatusChatMensagem,
    val nomeModelo: String? = null,
    val tipoDiagnostico: TipoDiagnostico? = null,
    val isLocal: Boolean = false,
    val codigoErro: String? = null,
)

data class SessaoChatDiagnostico(
    val id: String,
    val titulo: String,
    val criadoEmEpochMs: Long,
    val atualizadoEmEpochMs: Long,
    val status: StatusSessao,
    val tipoDiagnostico: TipoDiagnostico? = null,
    val nomeModelo: String? = null,
    val diagnosticoPayloadJson: String? = null,
)

// =============================================================================
// Mapeadores Entity ↔ Domain
// =============================================================================

// --- String helpers ---

private fun PapelChatMensagem.paraString(): String = when (this) {
    PapelChatMensagem.usuario -> "user"
    PapelChatMensagem.assistente -> "assistant"
    PapelChatMensagem.sistema -> "system"
}

private fun String.paraPapel(): PapelChatMensagem = when (this) {
    "user" -> PapelChatMensagem.usuario
    "assistant" -> PapelChatMensagem.assistente
    else -> PapelChatMensagem.sistema
}

private fun StatusChatMensagem.paraString(): String = when (this) {
    StatusChatMensagem.enviando -> "sending"
    StatusChatMensagem.streaming -> "streaming"
    StatusChatMensagem.concluido -> "completed"
    StatusChatMensagem.falhou -> "failed"
}

private fun String.paraStatusMensagem(): StatusChatMensagem = when (this) {
    "sending" -> StatusChatMensagem.enviando
    "streaming" -> StatusChatMensagem.streaming
    "completed" -> StatusChatMensagem.concluido
    else -> StatusChatMensagem.falhou
}

private fun StatusSessao.paraString(): String = when (this) {
    StatusSessao.ativa -> "active"
    StatusSessao.concluida -> "completed"
    StatusSessao.falhou -> "failed"
    StatusSessao.interrompida -> "interrupted"
}

private fun String.paraStatusSessao(): StatusSessao = when (this) {
    "active" -> StatusSessao.ativa
    "completed" -> StatusSessao.concluida
    "failed" -> StatusSessao.falhou
    else -> StatusSessao.interrompida
}

private fun TipoDiagnostico.paraString(): String = when (this) {
    TipoDiagnostico.ultimoTeste -> "last_test"
    TipoDiagnostico.novoTeste -> "new_test"
    TipoDiagnostico.historico -> "history"
}

private fun String?.paraTipoDiagnostico(): TipoDiagnostico? = when (this) {
    "last_test" -> TipoDiagnostico.ultimoTeste
    "new_test" -> TipoDiagnostico.novoTeste
    "history" -> TipoDiagnostico.historico
    else -> null
}

// --- metadataJson helpers ---

private fun buildMetadataJson(
    nomeModelo: String?,
    tipoDiagnostico: TipoDiagnostico?,
    isLocal: Boolean,
    codigoErro: String?,
): String? {
    val temAlgo = nomeModelo != null || tipoDiagnostico != null || isLocal || codigoErro != null
    if (!temAlgo) return null
    val o = JSONObject()
    nomeModelo?.let { o.put("modelName", it) }
    tipoDiagnostico?.let { o.put("diagnosticType", it.paraString()) }
    if (isLocal) o.put("isLocalMessage", true)
    codigoErro?.let { o.put("errorCode", it) }
    return o.toString()
}

private fun parseMetadataJson(json: String?): Triple<String?, TipoDiagnostico?, Pair<Boolean, String?>> {
    if (json.isNullOrBlank()) return Triple(null, null, Pair(false, null))
    return try {
        val o = JSONObject(json)
        val nomeModelo = if (o.has("modelName") && !o.isNull("modelName")) o.optString("modelName") else null
        val tipoDiag = (if (o.has("diagnosticType") && !o.isNull("diagnosticType")) o.optString("diagnosticType") else null).paraTipoDiagnostico()
        val isLocal = o.optBoolean("isLocalMessage", false)
        val errorCode = if (o.has("errorCode") && !o.isNull("errorCode")) o.optString("errorCode") else null
        Triple(nomeModelo, tipoDiag, Pair(isLocal, errorCode))
    } catch (_: Exception) {
        Triple(null, null, Pair(false, null))
    }
}

// --- Extension functions de conversão ---

fun ChatMensagem.paraEntity(): ChatMessageEntity {
    return ChatMessageEntity(
        id = id,
        sessionId = sessionId,
        role = papel.paraString(),
        content = conteudo,
        createdAtEpochMs = criadoEmEpochMs,
        status = status.paraString(),
        metadataJson = buildMetadataJson(nomeModelo, tipoDiagnostico, isLocal, codigoErro),
    )
}

fun ChatMessageEntity.paraDominio(): ChatMensagem {
    val (nomeModelo, tipoDiag, localEErro) = parseMetadataJson(metadataJson)
    val (isLocal, codigoErro) = localEErro
    return ChatMensagem(
        id = id,
        sessionId = sessionId,
        papel = role.paraPapel(),
        conteudo = content,
        criadoEmEpochMs = createdAtEpochMs,
        status = status.paraStatusMensagem(),
        nomeModelo = nomeModelo,
        tipoDiagnostico = tipoDiag,
        isLocal = isLocal,
        codigoErro = codigoErro,
    )
}

fun SessaoChatDiagnostico.paraEntity(): ChatSessionEntity {
    return ChatSessionEntity(
        id = id,
        titulo = titulo,
        criadoEmEpochMs = criadoEmEpochMs,
        atualizadoEmEpochMs = atualizadoEmEpochMs,
        status = status.paraString(),
        tipoDiagnostico = tipoDiagnostico?.paraString(),
        nomeModelo = nomeModelo,
        diagnosticoPayloadJson = diagnosticoPayloadJson,
    )
}

fun ChatSessionEntity.paraDominio(): SessaoChatDiagnostico {
    return SessaoChatDiagnostico(
        id = id,
        titulo = titulo,
        criadoEmEpochMs = criadoEmEpochMs,
        atualizadoEmEpochMs = atualizadoEmEpochMs,
        status = status.paraStatusSessao(),
        tipoDiagnostico = tipoDiagnostico.paraTipoDiagnostico(),
        nomeModelo = nomeModelo,
        diagnosticoPayloadJson = diagnosticoPayloadJson,
    )
}
