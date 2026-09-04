package io.signallq.app.core.nds

import io.signallq.app.core.diagnostico.HistoricalDiagnosticInput

/**
 * Ponte `HistoricalDiagnosticInput -> NdsHistoricalInfo` (ADR-018 seção 13,
 * NDS-Snapshot-06 — issue #1838). Função pura, sem I/O e sem recálculo de
 * degradação — `degradationDetected`/`degradationPercent` já vêm calculados
 * na origem (`DegradacaoHistoricoCalculadora`, `core/diagnostico`), a mesma
 * conta que alimenta `RecomendacaoPraticaEngine.recomendarUpgradeRoteadorRecorrente`
 * (REC-14). Este mapper só decide o shape do bloco e quando ele existe.
 *
 * A agregação real (consulta a `MedicaoDao`, janelas de 7/30 dias) acontece em
 * `:app`, que é quem popula `DiagnosticInput.historico` antes de chamar
 * `toNdsDiagnosticsRequest()`.
 *
 * Bloco inteiro fica `null` (omitido do payload) quando não há nenhum teste
 * nas duas janelas — usuário novo, sem histórico algum. Isso é diferente de
 * "poucos testes": um usuário com 2 testes em 7 dias ainda tem histórico real
 * (as médias são calculadas com o que existe), só não confiança suficiente
 * para declarar degradação — critério já resolvido na origem, que deixa
 * `degradationDetected`/`degradationPercent` nulos nesse caso.
 *
 * `testsCount7d`/`testsCount30d` sempre são copiados como estão (mesmo `0` —
 * contagem é fato real, ADR-018). Todos os demais campos são copiados como
 * estão, porque `HistoricalDiagnosticInput` já trata "presente = calculado,
 * null = não calculado" na origem.
 */
fun HistoricalDiagnosticInput?.toNdsHistoricalInfo(): NdsHistoricalInfo? {
    if (this == null) return null
    if (testsCount7d <= 0 && testsCount30d <= 0) return null

    return NdsHistoricalInfo(
        testsCount7d = testsCount7d,
        avgDownload7d = avgDownload7d,
        avgUpload7d = avgUpload7d,
        avgPing7d = avgPing7d,
        avgDns7d = avgDns7d,
        testsCount30d = testsCount30d,
        avgDownload30d = avgDownload30d,
        avgUpload30d = avgUpload30d,
        avgPing30d = avgPing30d,
        avgDns30d = avgDns30d,
        degradationDetected = degradationDetected,
        degradationPercent = degradationPercent,
        worstTimeWindow = worstTimeWindow,
        bestTimeWindow = bestTimeWindow,
    )
}
