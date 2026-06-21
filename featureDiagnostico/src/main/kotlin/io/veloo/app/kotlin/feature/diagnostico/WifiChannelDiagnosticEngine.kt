package io.veloo.app.feature.diagnostico

import io.veloo.app.core.network.contracts.wifi.channel.Band
import io.veloo.app.core.network.contracts.wifi.channel.ChannelWidth
import io.veloo.app.core.network.contracts.wifi.channel.EvalConfig
import io.veloo.app.core.network.contracts.wifi.channel.Neighbor
import io.veloo.app.core.network.contracts.wifi.channel.evaluateChannels
import io.veloo.app.core.network.contracts.wifi.channel.freqToChannel

private const val CAT_WIFI_CANAL = "wifi-canal"
private const val MIN_REDES_PARA_ANALISE = 6

object WifiChannelDiagnosticEngine {

    // ── Diagnóstico de congestionamento ────────────────────────────────────────

    fun avaliar(
        wifi: WifiDiagnosticInput?,
        scan: WifiScanDiagnosticInput?,
    ): List<DiagnosticResult> {
        if (wifi == null) return emptyList()
        if (scan == null) {
            return listOf(
                DiagnosticResult(
                    id = "WIFI-CANAL-INC-00",
                    titulo = "Sem Scan de Canais",
                    status = DiagnosticStatus.inconclusive,
                    evidencia = null,
                    mensagemUsuario = "Nao ha dados suficientes de scan para avaliar congestionamento de canal Wi-Fi.",
                    recomendacao = "Execute o scan de redes Wi-Fi e tente novamente.",
                    categoria = CAT_WIFI_CANAL,
                ),
            )
        }

        val redesValidas = scan.redes.filter { it.frequenciaMhz != null && it.rssiDbm != null }
        if (redesValidas.size < MIN_REDES_PARA_ANALISE || scan.conectadoCanal == null) {
            return listOf(
                DiagnosticResult(
                    id = "WIFI-CANAL-INC-01",
                    titulo = "Scan Insuficiente",
                    status = DiagnosticStatus.inconclusive,
                    evidencia = "redesValidas=${redesValidas.size} canalAtual=${scan.conectadoCanal ?: "—"}",
                    mensagemUsuario = "O scan de redes não tem dados suficientes para avaliar o congestionamento do canal.",
                    recomendacao = "Refaça o scan perto do roteador e aguarde alguns segundos para coletar mais redes.",
                    categoria = CAT_WIFI_CANAL,
                ),
            )
        }

        val canalAtual = scan.conectadoCanal
        val targetBand = wifi.banda().toCoreBand() ?: return emptyList()
        val neighbors = redesValidas.toNeighbors()

        // Busca o score do canal atual (incluindo canais não-padrão e DFS)
        val wideConfig = EvalConfig(
            targetWidth24 = ChannelWidth.W20,
            targetWidth5 = ChannelWidth.W20,
            allow24Overlapping = true,
            avoidDfs = false,
        )
        val scoreAtual = evaluateChannels(neighbors, wideConfig)[targetBand]
            ?.firstOrNull { it.channel == canalAtual }
            ?.score
            ?: return emptyList()

        // Busca o melhor canal entre os padrões recomendados
        val recConfig = EvalConfig(
            targetWidth24 = ChannelWidth.W20,
            targetWidth5 = ChannelWidth.W20,
            allow24Overlapping = false,
            avoidDfs = true,
        )
        val rec = evaluateChannels(neighbors, recConfig)[targetBand]
            ?.firstOrNull { it.recommended }
            ?: return emptyList()

        // Congestionado se o melhor canal reduz ao menos 50% da interferência atual
        val congestionado = rec.channel != canalAtual && scoreAtual > 0.0 && rec.score < scoreAtual * 0.5

        val resultados = mutableListOf<DiagnosticResult>()
        if (congestionado) {
            resultados.add(
                DiagnosticResult(
                    id = "WIFI-CANAL-01",
                    titulo = "Canal Wi-Fi Congestionado",
                    status = DiagnosticStatus.attention,
                    evidencia = "canalAtual=$canalAtual scoreAtual=${"%.2e".format(scoreAtual)}mW melhorCanal=${rec.channel} scoreMelhor=${"%.2e".format(rec.score)}mW",
                    mensagemUsuario = "O canal Wi-Fi atual parece congestionado (muitas redes fortes no mesmo canal). Isso pode causar lentidao e instabilidade.",
                    recomendacao = "Considere trocar o canal Wi-Fi para ${rec.channel} (menos interferência espectral).",
                    categoria = CAT_WIFI_CANAL,
                    podeConcluir = false,
                ),
            )
        }

        return resultados
    }

    // ── Espectro visual ────────────────────────────────────────────────────────

    fun computarEspectro(
        redes: List<RedeWifiVizinha>,
        canalAtual: Int?,
        banda: String,
        seuSSID: String? = null,
    ): SnapshotEspectroCanal {
        val neighbors = redes.toNeighbors()
        val targetBand = bandaStringToBand(banda)

        // Config de visualização: W20 por canal, todos os canais incluindo DFS
        val vizConfig = EvalConfig(
            targetWidth24 = ChannelWidth.W20,
            targetWidth5 = ChannelWidth.W20,
            targetWidth6 = ChannelWidth.W20,
            allow24Overlapping = true,
            avoidDfs = false,
            preferPsc = true,
        )
        val vizScores = evaluateChannels(neighbors, vizConfig)

        // Config de recomendação: melhores práticas (sem DFS, sem canais sobrepostos)
        val recConfig = EvalConfig(
            targetWidth24 = ChannelWidth.W20,
            targetWidth5 = ChannelWidth.W20,
            allow24Overlapping = false,
            avoidDfs = true,
        )

        val (recChannel, motivo) = if (targetBand != null) {
            val rec = evaluateChannels(neighbors, recConfig)[targetBand]?.firstOrNull { it.recommended }
            if (rec != null) {
                val m = if (rec.score == 0.0) {
                    "Canal livre — sem interferência espectral"
                } else when (targetBand) {
                    Band.GHZ_24 -> "Menor interferência espectral entre 1, 6 e 11"
                    else -> "Menor interferência espectral na faixa $banda"
                }
                rec.channel to m
            } else {
                null to null
            }
        } else {
            null to null
        }

        val dadosPorCanal = when {
            targetBand != null -> {
                (vizScores[targetBand] ?: emptyList()).map { cs ->
                    DadoCanal(
                        canal = cs.channel,
                        count = cs.overlappingAps,
                        countProprios = 0,
                        countTerceiros = cs.overlappingAps,
                        maxRssiDbm = cs.strongestNeighborDbm,
                        nivel = classificarCongestionamento(cs.overlappingAps),
                        ehCanalAtual = cs.channel == canalAtual,
                        ehCanalRecomendado = cs.channel == recChannel,
                    )
                }.sortedBy { it.canal }
            }
            else -> {
                // "Todos": combina todas as bandas, exibe apenas canais com APs presentes
                Band.entries.flatMap { b ->
                    (vizScores[b] ?: emptyList())
                        .filter { it.overlappingAps > 0 }
                        .map { cs ->
                            DadoCanal(
                                canal = cs.channel,
                                count = cs.overlappingAps,
                                countProprios = 0,
                                countTerceiros = cs.overlappingAps,
                                maxRssiDbm = cs.strongestNeighborDbm,
                                nivel = classificarCongestionamento(cs.overlappingAps),
                                ehCanalAtual = cs.channel == canalAtual,
                                ehCanalRecomendado = false,
                            )
                        }
                }.sortedBy { it.canal }
            }
        }

        return SnapshotEspectroCanal(
            dadosPorCanal = dadosPorCanal,
            canalAtual = canalAtual,
            canalRecomendado = recChannel,
            motivoRecomendacao = motivo,
            banda = banda,
        )
    }

    fun classificarCongestionamento(count: Int): NivelCongestionamento = when {
        count <= 2 -> NivelCongestionamento.livre
        count <= 5 -> NivelCongestionamento.moderado
        else -> NivelCongestionamento.congestionado
    }

    // ── Helpers internos ───────────────────────────────────────────────────────

    private fun BandaWifi.toCoreBand(): Band? = when (this) {
        BandaWifi.ghz24 -> Band.GHZ_24
        BandaWifi.ghz5 -> Band.GHZ_5
        BandaWifi.desconhecida -> null
    }

    private fun bandaStringToBand(banda: String): Band? = when (banda) {
        "2.4GHz" -> Band.GHZ_24
        "5GHz" -> Band.GHZ_5
        "6GHz" -> Band.GHZ_6
        else -> null
    }
}

// Converte RedeWifiVizinha → Neighbor para o motor de avaliação espectral.
// Assume W20 porque RedeWifiVizinha não carrega informação de largura de canal.
private fun List<RedeWifiVizinha>.toNeighbors(): List<Neighbor> = mapNotNull { r ->
    val freq = r.frequenciaMhz ?: return@mapNotNull null
    val rssi = r.rssiDbm ?: return@mapNotNull null
    val (band, _) = freqToChannel(freq) ?: return@mapNotNull null
    val bssid = r.bssid ?: "synth_${freq}_${rssi}_${r.ssid?.hashCode() ?: 0}"
    Neighbor(
        bssid = bssid,
        band = band,
        centerFreqMhz = freq,
        centerFreq1Mhz = null,
        width = ChannelWidth.W20,
        rssiDbm = rssi,
    )
}
