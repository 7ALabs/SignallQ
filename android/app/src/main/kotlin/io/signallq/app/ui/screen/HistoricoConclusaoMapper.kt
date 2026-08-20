package io.signallq.app.ui.screen

import io.signallq.app.core.database.MedicaoEntity

// Task 2.0.21 (épico #1647, issue #1669) — a lista do Histórico passou a priorizar
// conclusão/objetivo/data em vez de só velocidade em Mbps (métricas foram para o detalhe).
//
// Deliberadamente sem migration de schema: MedicaoEntity já persiste tudo que este mapper
// precisa (vereditos, gargalo, texto de diagnóstico, status, fonte) desde antes desta task — é
// puro reaproveitamento/adaptação de apresentação. Registro antigo (campos novos nulos, status
// "completed" padrão, sem veredito) sempre cai num fallback determinístico — nunca lança exceção
// nem produz resultado ilegível --> critério de aceite "registros antigos continuam legíveis sem
// migration destrutiva".

/** Tom visual da conclusão -- o Composable resolve a cor real a partir dos tokens ativos (LkTokens). */
internal enum class TomConclusao {
    POSITIVO,
    ATENCAO,
    NEGATIVO,
    NEUTRO,
}

internal data class ConclusaoMedicao(
    val objetivo: String,
    val conclusao: String,
    val tom: TomConclusao,
)

/**
 * O que motivou a medição, inferido de `diagnosticoOrigem`/`fonte`/`diagnosticoTexto` -- não
 * existe campo explícito. `diagnosticoOrigem == "ia"` é o mesmo sinal que
 * `DiagnosticoHistoricoSection` (sheet de detalhe) já usa para o selo "Gerado por IA"; `fonte ==
 * "orbit"` é um segundo sinal de origem IA que pode aparecer sem texto de diagnóstico associado.
 */
internal fun objetivoDaMedicao(m: MedicaoEntity): String =
    when {
        m.diagnosticoOrigem == "ia" || m.fonte == "orbit" -> "Diagnóstico por IA"
        !m.diagnosticoTexto.isNullOrBlank() -> "Diagnóstico guiado"
        m.fonte == "monitor" -> "Monitoramento contínuo"
        else -> "Teste de velocidade"
    }

/** Primeira frase de um texto de diagnóstico, sem pontuação sobrando. */
private fun primeiraFrase(texto: String): String {
    val fim = texto.indexOf(". ")
    val bruta = if (fim >= 0) texto.substring(0, fim) else texto
    return bruta.trim().trimEnd('.')
}

private fun conclusaoPorStatus(m: MedicaoEntity): ConclusaoMedicao? {
    val objetivo = objetivoDaMedicao(m)
    return when (m.status) {
        "failed" -> ConclusaoMedicao(objetivo, "Teste não concluído", TomConclusao.NEUTRO)
        "timeout" -> ConclusaoMedicao(objetivo, "Tempo esgotado antes de concluir", TomConclusao.NEUTRO)
        "contaminated" -> ConclusaoMedicao(objetivo, "Rede mudou durante o teste", TomConclusao.ATENCAO)
        "inconclusive" -> ConclusaoMedicao(objetivo, "Evidência insuficiente para concluir", TomConclusao.NEUTRO)
        else -> null
    }
}

private fun tomDoVeredito(v: String): TomConclusao =
    when (v) {
        "good" -> TomConclusao.POSITIVO
        "acceptable" -> TomConclusao.ATENCAO
        "poor" -> TomConclusao.NEGATIVO
        else -> TomConclusao.NEUTRO
    }

private fun conclusaoPorVeredito(m: MedicaoEntity): ConclusaoMedicao? {
    val (rotulo, veredito) =
        when {
            m.vereditoStreaming != null -> "streaming" to m.vereditoStreaming
            m.vereditoGamer != null -> "jogos" to m.vereditoGamer
            m.vereditoVideoChamada != null -> "chamadas de vídeo" to m.vereditoVideoChamada
            else -> return null
        }
    val descricao =
        when (veredito) {
            "good" -> "Bom para $rotulo"
            "acceptable" -> "Aceitável para $rotulo"
            "poor" -> "Ruim para $rotulo"
            else -> return null
        }
    return ConclusaoMedicao(objetivoDaMedicao(m), descricao, tomDoVeredito(veredito))
}

private fun conclusaoPorGargalo(m: MedicaoEntity): ConclusaoMedicao? {
    val gargalo = m.gargaloPrimario
    if (gargalo.isNullOrBlank() || gargalo == "none") return null
    val label =
        when (gargalo) {
            "download" -> "Download"
            "upload" -> "Upload"
            "latency" -> "Latência"
            "jitter" -> "Oscilação"
            "packetLoss" -> "Perda de pacotes"
            "bufferbloat" -> "Bufferbloat"
            else -> gargalo
        }
    return ConclusaoMedicao(objetivoDaMedicao(m), "Gargalo identificado: $label", TomConclusao.ATENCAO)
}

/** Fallback final -- registro legado, sem veredito/gargalo/diagnóstico, só velocidade crua. */
private fun conclusaoPorVelocidade(m: MedicaoEntity): ConclusaoMedicao {
    val dl = m.downloadMbps
    val objetivo = objetivoDaMedicao(m)
    val (texto, tom) =
        when {
            dl == null -> "Sem dados suficientes" to TomConclusao.NEUTRO
            dl >= 100 -> "Velocidade excelente" to TomConclusao.POSITIVO
            dl >= 25 -> "Velocidade boa" to TomConclusao.POSITIVO
            dl >= 10 -> "Velocidade regular" to TomConclusao.ATENCAO
            else -> "Velocidade lenta" to TomConclusao.NEGATIVO
        }
    return ConclusaoMedicao(objetivo, texto, tom)
}

/**
 * Conclusão/objetivo para exibição na lista do Histórico (issue #1669). Ordem de prioridade:
 * status anômalo > texto de diagnóstico > veredito de uso > gargalo > velocidade crua (legado).
 */
internal fun conclusaoDaMedicao(m: MedicaoEntity): ConclusaoMedicao {
    conclusaoPorStatus(m)?.let { return it }

    val diagTexto = m.diagnosticoTexto
    if (!diagTexto.isNullOrBlank()) {
        val tom =
            when {
                !m.gargaloPrimario.isNullOrBlank() && m.gargaloPrimario != "none" -> TomConclusao.ATENCAO
                !m.diagnosticoProblemas.isNullOrBlank() -> TomConclusao.ATENCAO
                else -> TomConclusao.POSITIVO
            }
        return ConclusaoMedicao(objetivoDaMedicao(m), primeiraFrase(diagTexto), tom)
    }

    conclusaoPorVeredito(m)?.let { return it }
    conclusaoPorGargalo(m)?.let { return it }
    return conclusaoPorVelocidade(m)
}
