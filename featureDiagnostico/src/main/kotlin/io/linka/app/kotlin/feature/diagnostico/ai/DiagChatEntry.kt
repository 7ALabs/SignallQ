package io.linka.app.kotlin.feature.diagnostico.ai

enum class DiagChatAutor { Usuario, Ia }

data class DiagChatEntry(
    val autor: DiagChatAutor,
    val texto: String,
    val timestamp: Long = System.currentTimeMillis(),
    val nomeModelo: String? = null,
    val isErro: Boolean = false,
    val isParcial: Boolean = false,
)
