package io.veloo.app.feature.diagnostico

data class DiagnosticResult(
    val id: String,
    val titulo: String,
    val status: DiagnosticStatus,
    val evidencia: String?,
    val mensagemUsuario: String,
    val recomendacao: String?,
    val categoria: String,
    val podeConcluir: Boolean = false,
)
