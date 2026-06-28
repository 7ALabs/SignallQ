package io.signallq.app.feature.diagnostico.pulse

data class QuestionNode(
    val id: String,
    val texto: String,
    val opcoes: List<OpcaoResposta>,
)
