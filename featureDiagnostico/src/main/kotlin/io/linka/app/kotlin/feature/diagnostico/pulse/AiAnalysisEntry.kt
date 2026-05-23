package io.linka.app.kotlin.feature.diagnostico.pulse

import io.linka.app.kotlin.feature.diagnostico.ai.AiDiagnosisResult

enum class ResponseSource { INSIGHT, GEMMA, LOCAL }

data class AiAnalysisEntry(
    val trigger: String,
    val content: String,
    val isFallback: Boolean,
    val timestamp: Long,
    val fullResult: AiDiagnosisResult? = null,
    val source: ResponseSource = ResponseSource.GEMMA,
)
