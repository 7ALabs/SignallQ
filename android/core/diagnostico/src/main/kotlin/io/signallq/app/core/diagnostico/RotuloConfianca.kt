package io.signallq.app.core.diagnostico

/**
 * GH#1657 (spec §14.4) / GH#1707 (Task 2.0.09e, épico #1647) — rótulo textual de confiança do
 * diagnóstico, nunca número. Decisão de Luiz (2026-08-20): só o rótulo em texto ("confiança
 * alta"/"média"/"baixa"), sem porcentagem, sem ícone/pill decorativo.
 *
 * [wireValue] é o vocabulário já aprovado por Luiz em 2026-08-17 para a telemetria do evento
 * `diagnostico_analise_concluida` (`baixa`/`media`/`alta`) — reaproveitado aqui em vez de criar um
 * segundo vocabulário só pra exibição.
 *
 * Limiares calibrados contra os 4 valores reais que [DiagnosticReport.confianca] produz
 * (0.30/0.65/0.88/0.90, ver `DiagnosticReport.kt`): 0.30 (inconclusivo) cai em [BAIXA], 0.65
 * (decisão conclusiva mas fraca) em [MEDIA], 0.88/0.90 (decisão forte) em [ALTA].
 */
enum class RotuloConfianca(val wireValue: String) {
    BAIXA("baixa"),
    MEDIA("media"),
    ALTA("alta"),
    ;

    companion object {
        fun de(confianca: Double): RotuloConfianca =
            when {
                confianca >= 0.8 -> ALTA
                confianca >= 0.5 -> MEDIA
                else -> BAIXA
            }
    }
}

/** Atalho pro rótulo textual (`baixa`/`media`/`alta`) a partir do relatório completo. */
val DiagnosticReport.rotuloConfianca: String
    get() = RotuloConfianca.de(confianca).wireValue
