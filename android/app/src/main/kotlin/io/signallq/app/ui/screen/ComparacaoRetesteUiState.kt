package io.signallq.app.ui.screen

/**
 * GH#1707 (Task 2.0.09e, parte 2/2, épico #1647) — estado do reteste vinculado à análise original
 * (spec §8.8/§14.6). [Ausente] antes de qualquer reteste, ou depois que o resultado atual já foi
 * consumido (nova análise iniciada do zero, tela fechada); [EmAndamento] enquanto a medição nova
 * roda; [Concluido] com o veredito já em texto pronto pra exibição — nunca número, nunca gráfico
 * (decisão de Luiz, 2026-08-20).
 */
sealed class ComparacaoRetesteUiState {
    data object Ausente : ComparacaoRetesteUiState()

    data object EmAndamento : ComparacaoRetesteUiState()

    data class Concluido(
        /** Rótulo pronto pra exibição: "Melhorou"/"Não mudou"/"Piorou"/"Comparação inconclusiva"
         *  (ver `TendenciaEstado?.rotuloComparacaoReteste()` em `:feature:history`). */
        val veredito: String,
        /** Só para telemetria/decisões futuras — a UI não bifurca texto por isto (o próprio
         *  [veredito] já vem como "Comparação inconclusiva" quando `comparavel` é `false`). */
        val comparavel: Boolean,
    ) : ComparacaoRetesteUiState()
}
