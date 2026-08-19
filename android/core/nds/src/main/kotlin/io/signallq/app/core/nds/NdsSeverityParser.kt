package io.signallq.app.core.nds

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
