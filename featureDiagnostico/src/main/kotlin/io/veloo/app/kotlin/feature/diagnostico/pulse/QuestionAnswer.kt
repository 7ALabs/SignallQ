package io.veloo.app.feature.diagnostico.pulse

data class QuestionAnswer(
    val questionId: String,
    val questionText: String,
    val answerId: String,
    val answerText: String,
    val contextContribution: String,
)
