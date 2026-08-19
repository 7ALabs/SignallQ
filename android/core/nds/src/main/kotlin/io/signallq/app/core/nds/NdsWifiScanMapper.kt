package io.signallq.app.core.nds

import io.signallq.app.core.network.contracts.wifi.channel.ChannelScore
import kotlin.math.log10
import kotlin.math.roundToInt

/**
 * Mapeia a saida do motor de avaliacao de canal Wi-Fi (`:coreNetwork`,
 * `ChannelEvaluator.evaluateChannels()`) para o bloco `wifiScan` do payload do
 * NDS (ADR-017, NDS-02a — issue #1747). Funcao pura, sem I/O e sem decisao de
 * severidade — so traducao de shape do que o app ja coleta.
 *
 * @param bandScores lista de [ChannelScore] avaliados para a banda da rede
 * conectada (retorno de `evaluateChannels()[bandaAtual]`).
 * @param canalConectado canal atualmente conectado, para localizar o
 * [ChannelScore] correspondente dentro de [bandScores]. `null` (sem conexao
 * Wi-Fi ativa) produz `channelCongestion = null`.
 */
fun mapWifiScanToNds(
    bandScores: List<ChannelScore>,
    canalConectado: Int?,
): NdsWifiScanInfo {
    val bestChannel = bandScores.firstOrNull { it.recommended }?.channel
    val current = canalConectado?.let { canal -> bandScores.firstOrNull { it.channel == canal } }
    val channelCongestion = current?.let { congestionPercent(it.score) }
    return NdsWifiScanInfo(
        channelCongestion = channelCongestion,
        bestChannel = bestChannel,
    )
}

/**
 * Converte `ChannelScore.score` (interferencia acumulada em mW, quanto menor
 * melhor) para um percentual 0..100 de congestionamento do canal.
 *
 * Mesma transformacao (mW -> dBm equivalente -> normalizacao linear) ja usada
 * em producao por `WifiChannelDiagnosticEngine.fracaoDeScore()`
 * (`core/diagnostico`, GH#1207 item 4) para a barra de ocupacao da tela
 * Sinal > Canal — replicada aqui, e nao importada, porque e so matematica de
 * conversao de unidade (nenhum julgamento de severidade), e `core/diagnostico`
 * esta marcado para remocao pelo ADR-017; nao vale abrir uma dependencia nova
 * para uma funcao `internal` de um modulo de saida. Se essa formula ganhar um
 * lar compartilhado antes de `core/diagnostico` sair, revisitar aqui para
 * apontar para ele em vez de duplicar.
 */
private fun congestionPercent(scoreMw: Double): Int {
    if (scoreMw <= 0.0) return 0
    val equivalenteDbm = 10.0 * log10(scoreMw)
    val fracao = ((equivalenteDbm + 90.0) / 60.0).coerceIn(0.0, 1.0)
    return (fracao * 100).roundToInt()
}
