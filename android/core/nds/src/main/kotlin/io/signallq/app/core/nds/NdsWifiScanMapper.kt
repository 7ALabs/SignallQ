package io.signallq.app.core.nds

import io.signallq.app.core.diagnostico.BandaWifi
import io.signallq.app.core.diagnostico.RedeWifiVizinha
import io.signallq.app.core.diagnostico.WifiScanDiagnosticInput
import io.signallq.app.core.network.contracts.wifi.channel.Band
import io.signallq.app.core.network.contracts.wifi.channel.ChannelScore
import io.signallq.app.core.network.contracts.wifi.channel.ChannelWidth
import io.signallq.app.core.network.contracts.wifi.channel.Neighbor
import io.signallq.app.core.network.contracts.wifi.channel.evaluateChannels
import io.signallq.app.core.network.contracts.wifi.channel.freqToChannel
import kotlin.math.log10
import kotlin.math.roundToInt

/** Identifica o metodo/versao do algoritmo de avaliacao de canal usado para
 *  calcular [ChannelScore] (ADR-018, campo `wifiScan.algorithmVersion`). Trocar
 *  quando a formula de score do `ChannelEvaluator` mudar de forma que quebre
 *  comparabilidade historica de payloads. */
private const val CHANNEL_EVALUATOR_VERSION = "channel-evaluator@1"

/**
 * Mapeia a saida do motor de avaliacao de canal Wi-Fi (`:coreNetwork`,
 * `ChannelEvaluator.evaluateChannels()`) e a evidencia bruta do scan para o bloco
 * `wifiScan` do payload do NDS (ADR-017/ADR-018, NDS-Snapshot-02 — issue #1834).
 * Funcao pura, sem I/O e sem decisao de severidade — so traducao de shape do que
 * o app ja coleta.
 *
 * @param bandScores lista de [ChannelScore] avaliados para a banda da rede
 * conectada (retorno de `evaluateChannels()[bandaAtual]`).
 * @param canalConectado canal atualmente conectado, para localizar o
 * [ChannelScore] correspondente dentro de [bandScores]. `null` (sem conexao
 * Wi-Fi ativa) produz `channelCongestion = null`.
 * @param redesVizinhas evidencia bruta das redes vizinhas do scan — incluida
 * mesmo quando algum campo individual estiver ausente, para o NDS poder
 * explicar por que um canal foi considerado congestionado, nao so confiar no
 * resultado ja calculado (#1832 secao 3). BSSID nunca entra na evidencia
 * enviada — ver [RedeWifiVizinha.toNdsNeighborInfo].
 *
 * Alem do percentual ja normalizado ([NdsWifiScanInfo.channelCongestion]), tambem
 * propaga os scores brutos em mW do canal conectado e do melhor candidato
 * ([NdsWifiScanInfo.currentScoreMw]/[NdsWifiScanInfo.bestScoreMw]) e a contagem de
 * vizinhas efetivamente utilizaveis pelo `ChannelEvaluator`
 * ([NdsWifiScanInfo.validNetworkCount]) — exigidos pela regra `WIFI-CANAL-*` do
 * NDS para nao ficar inconclusiva.
 */
fun mapWifiScanToNds(
    bandScores: List<ChannelScore>,
    canalConectado: Int?,
    redesVizinhas: List<RedeWifiVizinha> = emptyList(),
): NdsWifiScanInfo {
    val best = bandScores.firstOrNull { it.recommended }
    val current = canalConectado?.let { canal -> bandScores.firstOrNull { it.channel == canal } }
    val channelCongestion = current?.let { congestionPercent(it.score) }
    return NdsWifiScanInfo(
        connectedChannel = canalConectado,
        channelCongestion = channelCongestion,
        bestChannel = best?.channel,
        neighborCount = redesVizinhas.size,
        neighbors = redesVizinhas.map { it.toNdsNeighborInfo() },
        algorithmVersion = CHANNEL_EVALUATOR_VERSION.takeIf { bandScores.isNotEmpty() },
        currentScoreMw = current?.score,
        bestScoreMw = best?.score,
        validNetworkCount = redesVizinhas.toEvaluatorNeighbors().size,
    )
}

/**
 * Ponte `WifiScanDiagnosticInput -> NdsWifiScanInfo` — roda o `ChannelEvaluator`
 * sobre as redes vizinhas do scan (quando ha dado suficiente para identificar
 * banda/frequencia/RSSI de cada uma) e delega o calculo de congestionamento para
 * [mapWifiScanToNds]. Chamada por `NdsDiagnosticsRequestMapper.toNdsDiagnosticsRequest`
 * no lugar do antigo `wifiScan = null` hardcoded.
 *
 * `null` apenas quando o receptor e null OU quando o scan nao tem nenhuma evidencia
 * util (nem redes vizinhas, nem canal conectado) — nesses casos nao ha nada para
 * reportar como bloco `wifiScan` e o bloco fica omitido do payload, nao um objeto
 * vazio. Zero redes vizinhas com canal conectado presente (scan rodou, achou
 * nada) ainda produz um `NdsWifiScanInfo` valido com `neighborCount = 0`.
 *
 * @param bandaConectada banda da rede Wi-Fi conectada (`WifiDiagnosticInput.banda()`),
 * usada para localizar a lista de [ChannelScore] correta dentro do mapa retornado
 * por `evaluateChannels()`. `null`/desconhecida cai no fallback de localizar a
 * banda pelo canal conectado dentro dos scores calculados.
 */
fun WifiScanDiagnosticInput?.toNdsWifiScanInfo(bandaConectada: BandaWifi?): NdsWifiScanInfo? {
    if (this == null) return null
    if (redes.isEmpty() && conectadoCanal == null) return null

    val neighbors = redes.toEvaluatorNeighbors()
    // evaluateChannels() gera os candidatos por banda independente de haver
    // vizinhas (Band.entries e sempre percorrido) — zero vizinhas e uma medicao
    // real de "scan rodou, canal livre", nao ausencia de avaliacao. Rodar sempre
    // preserva essa distincao em vez de forcar channelCongestion=null nesse caso.
    val scoresPorBanda = evaluateChannels(neighbors)
    val targetBand = bandaConectada?.toCoreBand()
    val bandScores =
        targetBand?.let { scoresPorBanda[it] }
            ?: conectadoCanal?.let { canal ->
                scoresPorBanda.values.firstOrNull { scores -> scores.any { it.channel == canal } }
            }
            ?: emptyList()

    return mapWifiScanToNds(
        bandScores = bandScores,
        canalConectado = conectadoCanal,
        redesVizinhas = redes,
    )
}

private fun RedeWifiVizinha.toNdsNeighborInfo() =
    NdsWifiNeighborInfo(
        channel = canal,
        frequencyMhz = frequenciaMhz,
        rssiDbm = rssiDbm,
        widthMhz = larguraCanalMhz,
    )

/**
 * Converte as redes vizinhas do scan em [Neighbor] para o `ChannelEvaluator`.
 * Vizinhas sem frequencia, RSSI ou banda reconhecida sao ignoradas pelo
 * evaluator (nao contribuem para o calculo de congestionamento) mas continuam
 * presentes como evidencia bruta em [RedeWifiVizinha.toNdsNeighborInfo] — a
 * evidencia enviada ao NDS nao depende de a vizinha ser utilizavel pelo
 * evaluator.
 *
 * BSSID ausente ganha um identificador sintetico local (mesmo criterio usado em
 * `core/diagnostico` `WifiChannelDiagnosticEngine.toNeighbors()`, duplicado aqui
 * e nao importado porque `core/diagnostico` esta marcado para remocao pelo
 * ADR-017 — nao vale abrir uma dependencia nova para uma funcao interna de um
 * modulo em saida).
 */
private fun List<RedeWifiVizinha>.toEvaluatorNeighbors(): List<Neighbor> =
    mapNotNull { rede ->
        val freq = rede.frequenciaMhz ?: return@mapNotNull null
        val rssi = rede.rssiDbm ?: return@mapNotNull null
        val (band, _) = freqToChannel(freq) ?: return@mapNotNull null
        val bssid = rede.bssid ?: "synth_${freq}_${rssi}_${rede.ssid?.hashCode() ?: 0}"
        Neighbor(
            bssid = bssid,
            band = band,
            centerFreqMhz = freq,
            centerFreq1Mhz = null,
            width = larguraParaChannelWidth(rede.larguraCanalMhz) ?: ChannelWidth.W20,
            rssiDbm = rssi,
        )
    }

private fun larguraParaChannelWidth(larguraMhz: Int?): ChannelWidth? =
    when (larguraMhz) {
        20 -> ChannelWidth.W20
        40 -> ChannelWidth.W40
        80 -> ChannelWidth.W80
        160 -> ChannelWidth.W160
        320 -> ChannelWidth.W320
        else -> null
    }

private fun BandaWifi.toCoreBand(): Band? =
    when (this) {
        BandaWifi.ghz24 -> Band.GHZ_24
        BandaWifi.ghz5 -> Band.GHZ_5
        BandaWifi.desconhecida -> null
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
