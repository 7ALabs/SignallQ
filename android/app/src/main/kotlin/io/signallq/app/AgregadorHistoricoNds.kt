package io.signallq.app

import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.core.diagnostico.DegradacaoHistoricoCalculadora
import io.signallq.app.core.diagnostico.HistoricalDiagnosticInput

/** Janela de 7 dias em milissegundos, usada para separar a sub-lista mais
 *  recente dentro de [medicoesUltimos30Dias] (que já chega filtrada a 30 dias
 *  pela query do chamador). */
private const val JANELA_7D_MS = 7L * 24 * 60 * 60 * 1000

/**
 * Agrega [MedicaoEntity] (histórico local, `MedicaoDao`) nas janelas 7d/30d do
 * payload NDS `historical` (ADR-018 seção 13, NDS-Snapshot-06 — issue #1838).
 * Função pura — sem acesso a banco — para ser testável sem Room; quem chama
 * (`MainViewModel.montarHistoricoInput`) já faz a consulta e repassa a lista.
 *
 * Mesmo critério de inclusão já usado por `ObservadorHistoricoRoom` para as
 * médias de 5 medições da tela Histórico: nenhum filtro por `status`/
 * `contaminado` — `mapNotNull` já exclui os valores nulos de cada métrica
 * (medições falhas/incompletas tendem a não ter `downloadMbps`, por exemplo),
 * sem inventar um segundo critério de "medição válida" que a tela de Histórico
 * não usa.
 *
 * `avgDns7d`/`avgDns30d` e `worstTimeWindow`/`bestTimeWindow` ficam sempre
 * `null` — `MedicaoEntity` não tem coluna de latência DNS nem de janela de
 * horário; gap documentado, não coberto por esta fatia (ADR-018 já registra
 * isso como "fora do exemplo do #1832, mas já coletado" só para o DNS, que na
 * verdade não é coletado por medição — é lido em tempo real por
 * `AvaliadorCoerenciaDns`, sem persistência histórica hoje).
 *
 * @param medicoesUltimos30Dias medições com `timestampEpochMs >= agoraEpochMs - 30d`
 * (`MedicaoDao.buscarDesde`). Passar uma lista com medições mais antigas não
 * quebra o cálculo de 30d (contaria mais que deveria) — é responsabilidade do
 * chamador já filtrar a janela de 30 dias na query.
 * @param agoraEpochMs referência de "agora" usada para recortar a sub-janela
 * de 7 dias dentro de [medicoesUltimos30Dias].
 * @return `null` quando não há nenhuma medição no período (usuário novo, ou
 * app sem uso nos últimos 30 dias) — o bloco inteiro deve ser omitido do
 * payload NDS, nunca preenchido com zeros.
 */
fun agregarHistoricoNds(
    medicoesUltimos30Dias: List<MedicaoEntity>,
    agoraEpochMs: Long,
): HistoricalDiagnosticInput? {
    if (medicoesUltimos30Dias.isEmpty()) return null

    val corte7d = agoraEpochMs - JANELA_7D_MS
    val janela30 = medicoesUltimos30Dias
    val janela7 = janela30.filter { it.timestampEpochMs >= corte7d }

    val downloads7d = janela7.mapNotNull { it.downloadMbps }
    val downloads30d = janela30.mapNotNull { it.downloadMbps }
    val avgDownload7d = mediaOuNull(downloads7d)
    val avgDownload30d = mediaOuNull(downloads30d)
    // Gate de confiança da calculadora precisa da contagem de amostras que
    // realmente compõem a média de download, não do total de medições da
    // janela (que pode incluir testes falhos/parciais sem downloadMbps) --
    // caso contrário 5 medições com só 1 download valido passava no limiar
    // MIN_TESTS_7D com uma média de amostra única.
    val degradacao =
        DegradacaoHistoricoCalculadora.calcular(
            avgDownload7d = avgDownload7d,
            avgDownload30d = avgDownload30d,
            testsCount7d = downloads7d.size,
            testsCount30d = downloads30d.size,
        )

    return HistoricalDiagnosticInput(
        avgDownload7d = avgDownload7d,
        avgUpload7d = mediaOuNull(janela7.mapNotNull { it.uploadMbps }),
        avgPing7d = mediaOuNull(janela7.mapNotNull { it.latencyMs }),
        testsCount7d = janela7.size,
        avgDownload30d = avgDownload30d,
        avgUpload30d = mediaOuNull(janela30.mapNotNull { it.uploadMbps }),
        avgPing30d = mediaOuNull(janela30.mapNotNull { it.latencyMs }),
        testsCount30d = janela30.size,
        degradationDetected = degradacao?.first,
        degradationPercent = degradacao?.second,
    )
}

private fun mediaOuNull(valores: List<Double>): Double? {
    if (valores.isEmpty()) return null
    return valores.average()
}
