package io.signallq.app.core.diagnostico

import io.signallq.app.core.network.contracts.topologia.NivelConfianca
import io.signallq.app.core.network.contracts.topologia.PapelTopologia
import io.signallq.app.core.network.contracts.wifi.channel.Band
import io.signallq.app.core.network.contracts.wifi.channel.ChannelScore
import io.signallq.app.core.network.contracts.wifi.channel.ChannelWidth
import io.signallq.app.core.network.contracts.wifi.channel.EvalConfig
import io.signallq.app.core.network.contracts.wifi.channel.Neighbor
import io.signallq.app.core.network.contracts.wifi.channel.candidateSpan
import io.signallq.app.core.network.contracts.wifi.channel.evaluateChannels
import io.signallq.app.core.network.contracts.wifi.channel.freqToChannel
import io.signallq.app.core.network.contracts.wifi.channel.neighborSpans
import io.signallq.app.core.network.contracts.wifi.channel.overlapMhz
import kotlin.math.log10

private const val CAT_WIFI_CANAL = "wifi-canal"
private const val MIN_REDES_PARA_ANALISE = 6

/**
 * GH#1207 — motor de canal Wi-Fi. Ponto único de verdade para congestionamento,
 * recomendação e espectro visual da tela Sinal > Canal.
 *
 * Correções desta issue (numeração conforme o corpo de #1207):
 * 1. Rede própria não é mais sempre contada como terceiro ([contarProprioETerceiro]).
 * 2. [DadoCanal.banda] dá identidade composta banda+canal — "Todos" não colide mais.
 * 3. Largura real do canal é propagada quando o scan reportou ([RedeWifiVizinha.larguraCanalMhz]);
 *    sem isso, assume 20 MHz e marca [DadoCanal.larguraEstimada].
 * 4. Status/barra/recomendação usam a mesma fonte: [classificarPorScore] deriva de
 *    [DadoCanal.fracaoInterferencia], o mesmo score espectral (mW) que decide a recomendação —
 *    não mais contagem crua de APs.
 */
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
        val (neighbors, _) = redesValidas.toNeighbors()

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

    /**
     * @param redes vizinhos do scan atual (uma banda quando [banda] != null, todas quando é o
     * modo "Todos" representado por `banda` textual vazio/"Todos" no chamador).
     * @param seuSSID GH#1207 item 1 — SSID da rede conectada, usado como sinal de "rede própria"
     * junto com [RedeWifiVizinha.papelTopologia] (quando o motor de topologia unificado já
     * classificou o BSSID) — a combinação dos dois é mais confiável que SSID sozinho (mesmo SSID
     * não comprova mesma infraestrutura, GH#1209 item 1), mas aqui isso já reduz o erro mais
     * grosseiro (countProprios sempre 0).
     */
    fun computarEspectro(
        redes: List<RedeWifiVizinha>,
        canalAtual: Int?,
        banda: String,
        seuSSID: String? = null,
        // GH#1207 item 2 — banda da rede CONECTADA, distinta de [banda] (que no modo "Todos" vale
        // literalmente "Todos", sem indicar banda nenhuma). Sem isso, `ehCanalAtual` no modo
        // "Todos" não tinha como saber em qual banda o canal atual realmente está, podendo marcar
        // o canal errado (mesmo número, banda errada) como "seu canal".
        bandaConectada: String? = null,
    ): SnapshotEspectroCanal {
        val (neighbors, larguraEstimadaGlobal) = redes.toNeighbors()
        val targetBand = bandaStringToBand(banda)
        val bandaAtualEfetiva = bandaConectada ?: banda
        val proprioPorBssid = redes.associate { r -> (r.bssid ?: "") to ehRedePropria(r, seuSSID) }

        // Config de visualização: W real por canal quando disponível, todos os canais incluindo DFS
        val vizConfig = EvalConfig(
            targetWidth24 = ChannelWidth.W20,
            targetWidth5 = ChannelWidth.W20,
            targetWidth6 = ChannelWidth.W20,
            allow24Overlapping = true,
            avoidDfs = false,
            preferPsc = true,
        )
        val vizScores = evaluateChannels(neighbors, vizConfig)

        // Config de recomendação: melhores práticas (sem DFS, sem canais sobrepostos, 1/6/11, PSC)
        val recConfig = EvalConfig(
            targetWidth24 = ChannelWidth.W20,
            targetWidth5 = ChannelWidth.W20,
            allow24Overlapping = false,
            avoidDfs = true,
            preferPsc = true,
        )

        val (recChannel, recBanda, motivo) = if (targetBand != null) {
            val rec = evaluateChannels(neighbors, recConfig)[targetBand]?.firstOrNull { it.recommended }
            if (rec != null) {
                val m = if (rec.score == 0.0) {
                    "Canal livre — sem interferência espectral"
                } else when (targetBand) {
                    Band.GHZ_24 -> "Menor interferência espectral entre 1, 6 e 11"
                    else -> "Menor interferência espectral na faixa $banda"
                }
                Triple(rec.channel, bandLabel(targetBand), m)
            } else {
                Triple(null, null, null)
            }
        } else {
            Triple(null, null, null)
        }

        fun construirDado(bandaCanal: Band, cs: ChannelScore): DadoCanal {
            // Visualização sempre em granularidade de 20 MHz (vizConfig) — a largura real de
            // cada vizinho já foi usada em toNeighbors() pro cálculo de sobreposição espectral.
            val (proprios, terceiros) = contarProprioETerceiro(
                candidatoCanal = cs.channel,
                bandaCanal = bandaCanal,
                width = ChannelWidth.W20,
                neighbors = neighbors,
                proprioPorBssid = proprioPorBssid,
            )
            return DadoCanal(
                canal = cs.channel,
                banda = bandLabel(bandaCanal),
                count = cs.overlappingAps,
                countProprios = proprios,
                countTerceiros = terceiros,
                maxRssiDbm = cs.strongestNeighborDbm,
                nivel = classificarPorScore(cs.score),
                fracaoInterferencia = fracaoDeScore(cs.score),
                larguraEstimada = larguraEstimadaGlobal,
                ehCanalAtual = cs.channel == canalAtual && (targetBand != null || bandLabel(bandaCanal) == bandaAtualEfetiva),
                ehCanalRecomendado = cs.channel == recChannel && bandLabel(bandaCanal) == recBanda,
            )
        }

        val dadosPorCanal = when {
            targetBand != null -> {
                (vizScores[targetBand] ?: emptyList())
                    .map { cs -> construirDado(targetBand, cs) }
                    .sortedBy { it.canal }
            }
            else -> {
                // "Todos": combina todas as bandas, exibe apenas canais com APs presentes.
                // GH#1207 item 2 — cada DadoCanal carrega `banda`, então canais com o mesmo
                // número em bandas diferentes (ex.: canal 1 em 2,4GHz e canal 1 em 6GHz) não
                // colidem mais como se fossem o mesmo canal.
                Band.entries.flatMap { b ->
                    (vizScores[b] ?: emptyList())
                        .filter { it.overlappingAps > 0 }
                        .map { cs -> construirDado(b, cs) }
                }.sortedWith(compareBy({ it.banda }, { it.canal }))
            }
        }

        val confianca = calcularConfianca(
            totalRedes = redes.size,
            larguraEstimada = larguraEstimadaGlobal,
        )

        return SnapshotEspectroCanal(
            dadosPorCanal = dadosPorCanal,
            canalAtual = canalAtual,
            canalRecomendado = recChannel,
            motivoRecomendacao = motivo,
            banda = banda,
            confianca = confianca,
        )
    }

    /**
     * GH#1207 (critérios de robustez) — estabilidade da recomendação entre scans recentes.
     * Recebe o canal recomendado em cada uma das últimas 3-5 leituras e diz se o resultado é
     * estável (mesmo canal na maioria das leituras) ou está oscilando — nesse caso a UI deve
     * reduzir a confiança exibida em vez de trocar de recomendação a cada refresh.
     */
    fun avaliarEstabilidadeRecomendacao(historicoRecomendados: List<Int?>): Boolean {
        if (historicoRecomendados.size < 2) return true
        val naoNulos = historicoRecomendados.filterNotNull()
        if (naoNulos.isEmpty()) return true
        val maisFrequente = naoNulos.groupingBy { it }.eachCount().maxByOrNull { it.value }?.value ?: 0
        // Estável quando o canal mais recomendado aparece em pelo menos 2/3 das leituras.
        return maisFrequente.toDouble() / naoNulos.size >= 2.0 / 3.0
    }

    // ── Helpers internos ───────────────────────────────────────────────────────

    /**
     * GH#1207 item 4 — único ponto que decide [NivelCongestionamento] a partir do score
     * espectral (mW) que também decide qual canal é recomendado. Converte o score acumulado
     * pra um "dBm equivalente" (mesma unidade que os limiares de RSSI já usados no app) — acima
     * de -40 dBm equivalente é congestionamento pesado, entre -40 e -60 é moderado, abaixo disso
     * a interferência é fraca o bastante pra ser tratada como livre.
     */
    internal fun classificarPorScore(scoreMw: Double): NivelCongestionamento {
        if (scoreMw <= 0.0) return NivelCongestionamento.livre
        val equivalenteDbm = 10.0 * log10(scoreMw)
        return when {
            equivalenteDbm > -40.0 -> NivelCongestionamento.congestionado
            equivalenteDbm > -60.0 -> NivelCongestionamento.moderado
            else -> NivelCongestionamento.livre
        }
    }

    /** Mesma fonte (score em mW) normalizada pra 0..1 — alimenta a barra de ocupação da UI,
     *  que antes usava `count / 8` independente do nível classificado. */
    internal fun fracaoDeScore(scoreMw: Double): Double {
        if (scoreMw <= 0.0) return 0.0
        val equivalenteDbm = 10.0 * log10(scoreMw)
        return ((equivalenteDbm + 90.0) / 60.0).coerceIn(0.0, 1.0)
    }

    private fun calcularConfianca(
        totalRedes: Int,
        larguraEstimada: Boolean,
    ): NivelConfianca = when {
        totalRedes < MIN_REDES_PARA_ANALISE -> NivelConfianca.BAIXA
        larguraEstimada -> NivelConfianca.MEDIA
        else -> NivelConfianca.ALTA
    }

    /** GH#1207 item 1 — sinal combinado de "rede própria": SSID igual ao conectado OU já
     *  classificada pelo motor de topologia unificado (papelTopologia != null/DESCONHECIDO).
     *  SSID sozinho não é prova definitiva (GH#1209 item 1), mas aqui só decide contagem de
     *  display (proprios/terceiros), não a classificação de topologia em si — o pior caso é
     *  contar uma rede vizinha homônima como "própria" na exibição de canal, não afirmar que ela
     *  É a sua estrutura em nenhum outro lugar da UI. */
    private fun ehRedePropria(
        rede: RedeWifiVizinha,
        seuSSID: String?,
    ): Boolean {
        if (rede.papelTopologia != null && rede.papelTopologia != PapelTopologia.DESCONHECIDO) return true
        return seuSSID != null && rede.ssid != null && rede.ssid == seuSSID
    }

    private fun contarProprioETerceiro(
        candidatoCanal: Int,
        bandaCanal: Band,
        width: ChannelWidth,
        neighbors: List<Neighbor>,
        proprioPorBssid: Map<String, Boolean>,
    ): Pair<Int, Int> {
        val (cLo, cHi) = candidateSpan(bandaCanal, candidatoCanal, width)
        var proprios = 0
        var terceiros = 0
        for (n in neighbors) {
            if (n.band != bandaCanal) continue
            val overlaps = neighborSpans(n).any { (lo, hi) -> overlapMhz(lo, hi, cLo, cHi) > 0 }
            if (!overlaps) continue
            if (proprioPorBssid[n.bssid] == true) proprios++ else terceiros++
        }
        return proprios to terceiros
    }

    fun classificarCongestionamento(count: Int): NivelCongestionamento = when {
        count <= 2 -> NivelCongestionamento.livre
        count <= 5 -> NivelCongestionamento.moderado
        else -> NivelCongestionamento.congestionado
    }

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

    private fun bandLabel(band: Band): String = when (band) {
        Band.GHZ_24 -> "2.4GHz"
        Band.GHZ_5 -> "5GHz"
        Band.GHZ_6 -> "6GHz"
    }
}

// Converte RedeWifiVizinha → Neighbor para o motor de avaliação espectral.
// GH#1207 item 3 — usa a largura real do canal quando o scan reportou; caso contrário
// assume 20 MHz e devolve `true` no segundo componente do par (largura estimada).
private fun List<RedeWifiVizinha>.toNeighbors(): Pair<List<Neighbor>, Boolean> {
    var larguraEstimada = false
    val neighbors = mapNotNull { r ->
        val freq = r.frequenciaMhz ?: return@mapNotNull null
        val rssi = r.rssiDbm ?: return@mapNotNull null
        val (band, _) = freqToChannel(freq) ?: return@mapNotNull null
        val bssid = r.bssid ?: "synth_${freq}_${rssi}_${r.ssid?.hashCode() ?: 0}"
        val width = larguraParaChannelWidth(r.larguraCanalMhz)
        if (width == null) larguraEstimada = true
        Neighbor(
            bssid = bssid,
            band = band,
            centerFreqMhz = freq,
            centerFreq1Mhz = null,
            width = width ?: ChannelWidth.W20,
            rssiDbm = rssi,
        )
    }
    return neighbors to larguraEstimada
}

private fun larguraParaChannelWidth(larguraMhz: Int?): ChannelWidth? = when (larguraMhz) {
    20 -> ChannelWidth.W20
    40 -> ChannelWidth.W40
    80 -> ChannelWidth.W80
    160 -> ChannelWidth.W160
    320 -> ChannelWidth.W320
    else -> null
}
