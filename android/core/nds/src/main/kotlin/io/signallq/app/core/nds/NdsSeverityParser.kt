package io.signallq.app.core.nds

import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.MetricStatus

/**
 * Vocabulario canonico de severidade do NDS (ADR-017, decisao registrada em
 * #1746 secao 5 — NDS-02a, issue #1747).
 *
 * O campo `veredicto` do modulo `scoring` da resposta do NDS fala o MESMO
 * vocabulario de 6 valores que [MetricStatus] ja usa (`excelente/bom/regular/
 * ruim/critico/inconclusivo`) — nao e coincidencia, o proprio KDoc de
 * [MetricStatus] documenta essa origem. `DiagnosticStatus` (motor local,
 * `core/diagnostico`) e aposentado como fonte de dado; [MetricStatus] vira o
 * unico vocabulario de UI para severidade.
 *
 * [parseNdsVeredicto] usa `MetricStatus.valueOf(veredicto)` com fallback
 * tolerante: o NDS pode devolver um valor fora dos 6 conhecidos (ex.
 * `"fraco"`, visto no contrato do `signallq-diagnostic-worker` — familia
 * parecida, nao identica). Nunca lanca excecao — um `veredicto` desconhecido
 * ou ausente vira [MetricStatus.inconclusivo], nunca crasha o chamador.
 */
fun parseNdsVeredicto(veredicto: String?): MetricStatus {
    if (veredicto == null) return MetricStatus.inconclusivo
    return runCatching { MetricStatus.valueOf(veredicto) }.getOrDefault(MetricStatus.inconclusivo)
}

/**
 * Segundo salto de vocabulario (NDS-02k, issue #1759, item 5 — secao 2b do
 * inventario da issue): [MetricStatus] (6 valores) -> `DiagnosticStatus` (5
 * valores, `core/diagnostico`, o que `DiagnosticReport.decisao.status` exige e
 * o que alimenta `registrarDiagConcluido`/funil SIG-155). Nao existia nenhuma
 * ponte pronta para esta direcao — `comSeveridadeConciliada()` (removida na
 * NDS-02d/#1752) reconciliava outra coisa (dois motores LOCAIS concorrentes).
 *
 * Colapso deliberado dos 6 graus em 5, sem regra de produto por tras (decisao
 * tecnica, revisavel):
 * - `excelente`/`bom` -> `ok` (mesmo tratamento que
 *   [io.signallq.app.core.diagnostico.DiagnosticoGuiadoEngine] ja da a
 *   `ok`+`info` juntos — "nenhuma acao necessaria");
 * - `regular` -> `info` (algo a notar, sem gravidade);
 * - `ruim` -> `attention` (vale investigar);
 * - `critico` -> `critical`;
 * - `inconclusivo` -> `inconclusive`.
 */
fun MetricStatus.toDiagnosticStatus(): DiagnosticStatus =
    when (this) {
        MetricStatus.excelente, MetricStatus.bom -> DiagnosticStatus.ok
        MetricStatus.regular -> DiagnosticStatus.info
        MetricStatus.ruim -> DiagnosticStatus.attention
        MetricStatus.critico -> DiagnosticStatus.critical
        MetricStatus.inconclusivo -> DiagnosticStatus.inconclusive
    }
