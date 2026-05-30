package io.veloo.app.feature.diagnostico.pulse

import io.veloo.app.feature.diagnostico.ai.AiDiagnosisResult
import java.util.concurrent.atomic.AtomicLong

enum class ResponseSource { INSIGHT, GEMMA, LOCAL }

data class AiAnalysisEntry(
    val trigger: String,
    val content: String,
    val isFallback: Boolean,
    val timestamp: Long,
    val fullResult: AiDiagnosisResult? = null,
    val source: ResponseSource = ResponseSource.GEMMA,
    val id: Long = nextId(),
) {
    companion object {
        private val counter = AtomicLong(0)
        private fun nextId(): Long = System.currentTimeMillis() * 1000 + counter.incrementAndGet() % 1000
    }
}
