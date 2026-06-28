package io.signallq.app.core.network.contracts.wifi.channel

// Ponto de entrada publico — avalia todos os canais candidatos e retorna ranking por banda
fun evaluateChannels(
    neighbors: List<Neighbor>,
    config: EvalConfig = EvalConfig(),
): Map<Band, List<ChannelScore>> {
    // Regra 8 — remove o proprio roteador do calculo
    val filtered = neighbors.filter { it.bssid !in config.excludeBssids }

    return Band.entries.associateWith { band ->
        val width = when (band) {
            Band.GHZ_24 -> config.targetWidth24
            Band.GHZ_5 -> config.targetWidth5
            Band.GHZ_6 -> config.targetWidth6
        }
        val candidates = candidateChannels(
            band = band,
            width = width,
            avoidDfs = config.avoidDfs,
            allow24Overlapping = config.allow24Overlapping,
            preferPsc = config.preferPsc,
        )
        val bandNeighbors = filtered.filter { it.band == band }

        val scores = candidates.map { channel ->
            scoreChannel(channel, band, width, bandNeighbors, config)
        }

        // Regra 7 — ordenacao: score -> overlappingAps -> strongestNeighborDbm -> canal
        val sorted = scores.sortedWith(
            compareBy(
                { it.score },
                { it.overlappingAps },
                { it.strongestNeighborDbm ?: Int.MIN_VALUE }, // null (sem vizinho) -> melhor
                { it.channel },
            ),
        )

        // Regra 7 — recommended=true apenas para o vencedor de cada banda
        sorted.mapIndexed { idx, s -> if (idx == 0) s.copy(recommended = true) else s }
    }
}

private fun scoreChannel(
    channel: Int,
    band: Band,
    width: ChannelWidth,
    neighbors: List<Neighbor>,
    config: EvalConfig,
): ChannelScore {
    // Regra 2 — span espectral do canal candidato
    val (cLo, cHi) = candidateSpan(band, channel, width)

    var totalScore = 0.0
    var overlappingCount = 0
    var strongestDbm: Int? = null

    for (ap in neighbors) {
        // Regra 2 — spans do AP (pode ser 2 spans para 80+80)
        val spans = neighborSpans(ap)
        var apContributed = false

        for ((apLo, apHi) in spans) {
            // Regra 4 — sobreposicao espectral em MHz
            val overlap = overlapMhz(apLo, apHi, cLo, cHi)
            if (overlap <= 0) continue

            val fraction = overlap.toDouble() / width.mhz
            // Regra 3 — acumula no dominio linear (mW), nunca soma dBm diretamente
            totalScore += dbmToMw(ap.rssiDbm) * fraction

            if (!apContributed) {
                apContributed = true
                overlappingCount++
                if (strongestDbm == null || ap.rssiDbm > strongestDbm) {
                    strongestDbm = ap.rssiDbm
                }
            }
        }
    }

    val isDfs = band == Band.GHZ_5 && isDfs5Ghz(channel)
    val isPsc = band == Band.GHZ_6 && isPsc6Ghz(channel)

    // Regra 6 — penalidade DFS (apenas quando avoidDfs=false; com true os DFS ja sao excluidos dos candidatos)
    val finalScore = if (isDfs && !config.avoidDfs) totalScore * config.dfsPenalty else totalScore

    return ChannelScore(
        band = band,
        channel = channel,
        width = width,
        score = finalScore,
        overlappingAps = overlappingCount,
        strongestNeighborDbm = strongestDbm,
        isDfs = isDfs,
        isPsc = isPsc,
        recommended = false,
    )
}
