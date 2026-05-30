package io.veloo.app.feature.diagnostico

private const val CAT_WIFI_CANAL = "wifi-canal"

object WifiChannelDiagnosticEngine {

    private const val MIN_REDENAS_PARA_ANALISE = 6

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

        val redes = scan.redes.filter { it.canal != null && it.rssiDbm != null }
        if (redes.size < MIN_REDENAS_PARA_ANALISE || scan.conectadoCanal == null) {
            return listOf(
                DiagnosticResult(
                    id = "WIFI-CANAL-INC-01",
                    titulo = "Scan Insuficiente",
                    status = DiagnosticStatus.inconclusive,
                    evidencia = "redesValidas=${redes.size} canalAtual=${scan.conectadoCanal ?: "—"}",
                    mensagemUsuario = "O scan de redes não tem dados suficientes para avaliar o congestionamento do canal.",
                    recomendacao = "Refaça o scan perto do roteador e aguarde alguns segundos para coletar mais redes.",
                    categoria = CAT_WIFI_CANAL,
                ),
            )
        }

        val canalAtual = scan.conectadoCanal
        val seuSsid = wifi.ssid ?: ""
        val banda = wifi.banda()

        // Fix 3: usa score ponderado (própria rede = 0.5, vizinho = 1.0) igual ao computarEspectro
        val scorePorCanal = redes.groupBy { it.canal!! }.mapValues { (_, rs) -> calcularScoreCanal(rs, seuSsid) }
        val scoreAtual = scorePorCanal[canalAtual] ?: 0.0

        // Fix 1 + Fix 3: em 2.4GHz, só canais padrão {1, 6, 11} são candidatos à recomendação
        val candidatos = if (banda == BandaWifi.ghz24) {
            scorePorCanal.filter { it.key in listOf(1, 6, 11) }
        } else {
            scorePorCanal
        }
        val melhor = candidatos.entries.minByOrNull { it.value }
        val canalMelhor = melhor?.key
        val scoreMelhor = melhor?.value ?: 0.0

        val diferenca = scoreAtual - scoreMelhor
        val congestionado = diferenca >= 3.0 && scoreAtual >= 4.0

        val resultados = mutableListOf<DiagnosticResult>()
        if (congestionado) {
            resultados.add(
                DiagnosticResult(
                    id = "WIFI-CANAL-01",
                    titulo = "Canal Wi-Fi Congestionado",
                    status = DiagnosticStatus.attention,
                    evidencia = "canalAtual=$canalAtual scoreCanal=${"%.1f".format(scoreAtual)} melhorCanal=$canalMelhor scoreMelhor=${"%.1f".format(scoreMelhor)}",
                    mensagemUsuario = "O canal Wi-Fi atual parece congestionado (muitas redes fortes no mesmo canal). Isso pode causar lentidao e instabilidade.",
                    recomendacao = if (canalMelhor != null && canalMelhor != canalAtual) "Considere trocar o canal Wi-Fi para $canalMelhor (menos ocupado no scan)." else "Considere trocar o canal Wi-Fi para um canal menos ocupado.",
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
        val redesValidas = redes.filter { it.canal != null }
        val porCanal: Map<Int?, List<RedeWifiVizinha>> = redesValidas.groupBy { it.canal }

        val canaisBase: List<Int> = when (banda) {
            "2.4GHz" -> (1..13).toList()
            "5GHz" -> listOf(36, 40, 44, 48, 52, 56, 60, 64, 100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 149, 153, 157, 161, 165)
            "6GHz" -> listOf(1, 5, 9, 13, 17, 21, 25, 29, 33, 37, 41, 45, 49, 53, 57, 61, 65, 69, 73, 77, 81, 85, 89, 93)
            else -> redesValidas.mapNotNull { it.canal }.distinct().sorted()
        }

        val (canalRec, motivo) = recomendarCanal(banda, porCanal, seuSSID) ?: (null to null)

        val dadosPorCanal = canaisBase.map { ch ->
            val redesNoCanal = porCanal[ch] ?: emptyList()
            val countProprios = redesNoCanal.count { r ->
                r.ssid != null && seuSSID != null && r.ssid.trim().equals(seuSSID.trim(), ignoreCase = true)
            }
            val countTerceiros = redesNoCanal.size - countProprios
            val count = redesNoCanal.size
            val maxRssi = redesNoCanal.mapNotNull { it.rssiDbm }.maxOrNull()
            DadoCanal(
                canal = ch,
                count = count,
                countProprios = countProprios,
                countTerceiros = countTerceiros,
                maxRssiDbm = maxRssi,
                nivel = classificarCongestionamentoPonderado(countProprios, countTerceiros),
                ehCanalAtual = ch == canalAtual,
                ehCanalRecomendado = ch == canalRec,
            )
        }

        return SnapshotEspectroCanal(
            dadosPorCanal = dadosPorCanal,
            canalAtual = canalAtual,
            canalRecomendado = canalRec,
            motivoRecomendacao = motivo,
            banda = banda,
        )
    }

    fun classificarCongestionamento(count: Int): NivelCongestionamento = when {
        count <= 2 -> NivelCongestionamento.livre
        count <= 5 -> NivelCongestionamento.moderado
        else -> NivelCongestionamento.congestionado
    }

    // Fix 3: score ponderado compartilhado entre avaliar() e computarEspectro().
    // Rede própria vale 0.5 (já sabemos que ela está lá); vizinho vale 1.0 (interferência real).
    private fun calcularScoreCanal(redes: List<RedeWifiVizinha>, seuSsid: String): Double {
        return redes.sumOf { rede ->
            val ePropria = seuSsid.isNotBlank() && rede.ssid?.trim().equals(seuSsid.trim(), ignoreCase = true) == true
            if (ePropria) 0.5 else 1.0
        }
    }

    private fun classificarCongestionamentoPonderado(countProprios: Int, countTerceiros: Int): NivelCongestionamento {
        val scoreTotal = countProprios * 0.5 + countTerceiros * 1.0
        return when {
            scoreTotal <= 2.0 -> NivelCongestionamento.livre
            scoreTotal <= 5.0 -> NivelCongestionamento.moderado
            else -> NivelCongestionamento.congestionado
        }
    }

    private fun recomendarCanal(banda: String, porCanal: Map<Int?, List<RedeWifiVizinha>>, seuSSID: String? = null): Pair<Int, String>? {
        // Usa calcularScoreCanal (sem threshold de RSSI) — mesma lógica de avaliar() e computarEspectro(),
        // eliminando a divergência anterior que ignorava redes com RSSI entre -80 e -100 dBm.
        val counts = porCanal
            .filterKeys { it != null }
            .mapKeys { it.key!! }
            .mapValues { (_, redes) -> calcularScoreCanal(redes, seuSSID ?: "") }

        return when (banda) {
            "2.4GHz" -> {
                val best = listOf(1, 6, 11).minByOrNull { counts[it] ?: 0.0 } ?: 1
                val scoreRec = counts[best] ?: 0.0
                val motivo = if (scoreRec == 0.0) "Canal livre — sem congestionamento"
                else "Menor congestionamento entre 1, 6 e 11"
                Pair(best, motivo)
            }
            "5GHz" -> {
                // Fix 2: priorizar canais não-DFS (36,40,44,48,149,153,157,161,165) — DFS pode perder
                // conectividade ao detectar radar em roteadores sem suporte ou certificação adequada.
                val naoDfs = listOf(36, 40, 44, 48, 149, 153, 157, 161, 165)
                val candidatosNaoDfs = counts.filter { it.key in naoDfs }
                val candidatosDfs = counts.filter { it.key !in naoDfs }
                val best = if (candidatosNaoDfs.isNotEmpty()) {
                    candidatosNaoDfs.minByOrNull { it.value }?.key
                } else {
                    candidatosDfs.minByOrNull { it.value }?.key
                } ?: return null
                val scoreRec = counts[best] ?: 0.0
                val ehDfs = best !in naoDfs
                val sufixoDfs = if (ehDfs) " (DFS)" else ""
                val motivoScore = if (scoreRec == 0.0) "Canal livre" else "Menor congestionamento"
                Pair(best, "$motivoScore na faixa 5 GHz — canal $best$sufixoDfs")
            }
            else -> null
        }
    }
}

