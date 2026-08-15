package io.signallq.app.feature.diagnostico.pulse

import io.signallq.app.core.diagnostico.DiagnosticReport
import io.signallq.app.core.diagnostico.DiagnosticStatus

object ContextAccumulator {

    fun buildInitial(
        downloadMbps: Double?,
        uploadMbps: Double?,
        latencyMs: Double?,
        jitterMs: Double?,
        lossPercent: Double?,
        stabilityScore: Double?,
        wifiSsid: String?,
        wifiRssiDbm: Int?,
        wifiFrequencyMhz: Int?,
        report: DiagnosticReport?,
    ): String = buildString {
        appendLine("=== CONTEXTO INICIAL DO DIAGNÓSTICO ===")
        appendLine()
        appendLine("--- Speedtest ---")
        downloadMbps?.let { appendLine("Download: %.1f Mbps".format(it)) }
        uploadMbps?.let { appendLine("Upload: %.1f Mbps".format(it)) }
        latencyMs?.let { appendLine("Latência: %.0f ms".format(it)) }
        jitterMs?.let { appendLine("Jitter: %.0f ms".format(it)) }
        lossPercent?.let { appendLine("Perda de pacotes: %.1f%%".format(it)) }
        stabilityScore?.let { appendLine("Score de estabilidade: %.0f/100".format(it)) }
        appendLine()
        appendLine("--- Wi-Fi ---")
        wifiSsid?.let { appendLine("SSID: $it") }
        wifiRssiDbm?.let { appendLine("Sinal: $it dBm") }
        wifiFrequencyMhz?.let {
            val banda = if (it >= 5000) "5 GHz" else if (it >= 2400) "2.4 GHz" else "${it} MHz"
            appendLine("Banda: $banda")
        }
        appendLine()
        report?.let {
            appendLine("--- Diagnóstico Local ---")
            appendLine("Decisão: ${it.decisao.id} (${it.decisao.status.name})")
            appendLine("Mensagem: ${it.decisao.mensagemUsuario}")
            it.decisao.recomendacao?.let { rec -> appendLine("Recomendação principal: $rec") }
            val criticos = (it.wifiResultados + it.internetResultados + it.dnsResultados)
                .filter { r -> r.status == DiagnosticStatus.critical || r.status == DiagnosticStatus.attention }
            if (criticos.isNotEmpty()) {
                appendLine("Alertas: " + criticos.joinToString("; ") { r -> "${r.titulo} (${r.status.name})" })
            }
        }
    }

    fun appendChip(accumulated: String, chip: OpcaoResposta): String = buildString {
        append(accumulated)
        appendLine()
        appendLine("=== CONTEXTO ADICIONAL: SELEÇÃO DO USUÁRIO ===")
        appendLine("Categoria escolhida: ${chip.label}")
        appendLine("Contexto: ${chip.contextoParaIA}")
    }

    fun appendAnswer(accumulated: String, question: QuestionNode, answer: OpcaoResposta): String = buildString {
        append(accumulated)
        appendLine()
        appendLine("Pergunta: ${question.texto}")
        appendLine("Resposta: ${answer.label}")
        appendLine("Contexto: ${answer.contextoParaIA}")
    }
}
