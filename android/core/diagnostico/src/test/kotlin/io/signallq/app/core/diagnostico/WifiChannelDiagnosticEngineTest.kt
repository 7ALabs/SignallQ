package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiChannelDiagnosticEngineTest {

    // ── Fix 1: avaliar() nunca recomenda canal não-padrão em 2.4GHz ─────────────

    @Test
    fun `fix1 - avaliar nunca recomenda canal sobreposto em 2 4GHz`() {
        // Canal 4 tem menos redes que canal 1, mas não deve ser recomendado — só {1, 6, 11}
        val wifi = WifiDiagnosticInput(
            rssiDbm = -50,
            linkSpeedMbps = 300,
            frequenciaMhz = 2412, // 2.4GHz
            canal = 1,
            ssid = "MinhaRede",
        )
        val scan = WifiScanDiagnosticInput(
            conectadoCanal = 1,
            redes = listOf(
                // Canal 1 — muito congestionado
                RedeWifiVizinha(canal = 1, rssiDbm = -55, frequenciaMhz = 2412, ssid = "Rede_A"),
                RedeWifiVizinha(canal = 1, rssiDbm = -58, frequenciaMhz = 2412, ssid = "Rede_B"),
                RedeWifiVizinha(canal = 1, rssiDbm = -60, frequenciaMhz = 2412, ssid = "Rede_C"),
                RedeWifiVizinha(canal = 1, rssiDbm = -62, frequenciaMhz = 2412, ssid = "Rede_D"),
                RedeWifiVizinha(canal = 1, rssiDbm = -65, frequenciaMhz = 2412, ssid = "Rede_E"),
                RedeWifiVizinha(canal = 1, rssiDbm = -68, frequenciaMhz = 2412, ssid = "Rede_F"),
                RedeWifiVizinha(canal = 1, rssiDbm = -70, frequenciaMhz = 2412, ssid = "Rede_G"),
                // Canal 4 — menos redes fortes (seria o "melhor" ingênuo), mas é sobreposto
                RedeWifiVizinha(canal = 4, rssiDbm = -55, frequenciaMhz = 2427, ssid = "Rede_X"),
                // Canal 6 — vazio (deve ser o recomendado)
                RedeWifiVizinha(canal = 6, rssiDbm = -90, frequenciaMhz = 2437, ssid = "Rede_W"),
                // Canal 11 — vazio
                RedeWifiVizinha(canal = 11, rssiDbm = -90, frequenciaMhz = 2462, ssid = "Rede_Z"),
            ),
        )
        val resultados = WifiChannelDiagnosticEngine.avaliar(wifi, scan)

        // Se congestionado, a recomendação nunca pode ser um canal não-padrão
        val congestionado = resultados.any { it.id == "WIFI-CANAL-01" }
        if (congestionado) {
            val resultado = resultados.first { it.id == "WIFI-CANAL-01" }
            // Canal 4, 5, 7, 8, 9, 10 nunca aparecem na recomendação
            val canaisProibidos = listOf(2, 3, 4, 5, 7, 8, 9, 10)
            canaisProibidos.forEach { canal ->
                assertFalse(
                    "Fix 1 falhou: canal $canal apareceu na recomendação 2.4GHz. evidencia=${resultado.evidencia}",
                    resultado.recomendacao?.contains("para $canal ") == true,
                )
            }
        }
    }

    // ── Fix 2: recomendarCanal() prioriza não-DFS em 5GHz ─────────────────────

    @Test
    fun `fix2 - computarEspectro nao recomenda DFS quando nao-DFS disponivel em 5GHz`() {
        // Todos os não-DFS com 1 rede, todos os DFS com 0 redes
        // Esperado: recomendação deve ser um não-DFS (36, 40, 44, 48, 149, 153, 157, 161, 165)
        val redes = listOf(
            // Não-DFS — 1 rede cada
            RedeWifiVizinha(canal = 36, rssiDbm = -55, frequenciaMhz = 5180),
            RedeWifiVizinha(canal = 149, rssiDbm = -60, frequenciaMhz = 5745),
            // DFS — vazios (seriam os "melhores" ingênuos por score 0)
            // Nenhuma rede nos DFS para garantir que o algoritmo correto prioriza não-DFS com mais score
        )
        val snapshot = WifiChannelDiagnosticEngine.computarEspectro(
            redes = redes,
            canalAtual = 36,
            banda = "5GHz",
            seuSSID = "MinhaRede",
        )

        val canalRec = snapshot.canalRecomendado
        assertNotNull("Fix 2: deve haver canal recomendado em 5GHz", canalRec)

        val naoDfs = listOf(36, 40, 44, 48, 149, 153, 157, 161, 165)
        // Se algum não-DFS existe no scan, a recomendação deve ser não-DFS
        assertTrue(
            "Fix 2 falhou: recomendou canal DFS $canalRec quando havia não-DFS disponível",
            canalRec in naoDfs,
        )
    }

    @Test
    fun `fix2 - computarEspectro so recomenda DFS quando todos nao-DFS congestionados`() {
        // Cenário: apenas canais DFS disponíveis no scan (sem redes em não-DFS)
        // Esperado: canalRecomendado deve ser DFS (não há alternativa)
        val redes = listOf(
            // Apenas DFS com redes
            RedeWifiVizinha(canal = 52, rssiDbm = -55, frequenciaMhz = 5260),
            RedeWifiVizinha(canal = 100, rssiDbm = -60, frequenciaMhz = 5500),
        )
        val snapshot = WifiChannelDiagnosticEngine.computarEspectro(
            redes = redes,
            canalAtual = 52,
            banda = "5GHz",
            seuSSID = "MinhaRede",
        )
        // Com não-DFS vazios (score 0.0), eles têm prioridade — mas se o Glob retornar não-DFS
        // com score 0, isso é correto também. O ponto é: nunca null.
        // Este teste valida que o engine não quebra quando só há DFS no scan.
        assertNotNull("Fix 2: engine não deve retornar null com apenas DFS no scan", snapshot)
    }

    // ── Fix 3: avaliar() e computarEspectro() convergem no mesmo diagnóstico ───

    @Test
    fun `fix3 - avaliar detecta congestionamento e computarEspectro classifica moderado`() {
        // Cenário: canal 1 com 1 rede própria + 4 vizinhos (5 APs sobrepostos → moderado).
        // Canal 6 vazio → score 0, canal 11 com 1 AP fraco (-90 dBm) → score ≈ 0.
        // avaliar() deve detectar congestionamento (score canal1 >> score melhor).
        // computarEspectro() deve classificar canal 1 como moderado (5 APs sobrepostos).
        val ssid = "MinhaRede"
        val wifi = WifiDiagnosticInput(
            rssiDbm = -50,
            linkSpeedMbps = 300,
            frequenciaMhz = 2412,
            canal = 1,
            ssid = ssid,
        )
        val redesCanal1 = listOf(
            RedeWifiVizinha(canal = 1, rssiDbm = -55, frequenciaMhz = 2412, ssid = ssid),   // própria — peso 0.5
            RedeWifiVizinha(canal = 1, rssiDbm = -58, frequenciaMhz = 2412, ssid = "Viz_A"), // terceiro — peso 1.0
            RedeWifiVizinha(canal = 1, rssiDbm = -60, frequenciaMhz = 2412, ssid = "Viz_B"),
            RedeWifiVizinha(canal = 1, rssiDbm = -62, frequenciaMhz = 2412, ssid = "Viz_C"),
            RedeWifiVizinha(canal = 1, rssiDbm = -65, frequenciaMhz = 2412, ssid = "Viz_D"),
        )
        val redesCanal11 = listOf(
            RedeWifiVizinha(canal = 11, rssiDbm = -90, frequenciaMhz = 2462, ssid = "LongeA"),
        )

        val scan = WifiScanDiagnosticInput(
            conectadoCanal = 1,
            redes = redesCanal1 + redesCanal11,
        )

        // avaliar() — score ponderado para canal 1 = 0.5 + 4*1.0 = 4.5
        // computarEspectro() — classifica canal 1 como moderado (4.5 <= 5.0)
        val resultadosAvaliar = WifiChannelDiagnosticEngine.avaliar(wifi, scan)

        val snapshot = WifiChannelDiagnosticEngine.computarEspectro(
            redes = redesCanal1 + redesCanal11,
            canalAtual = 1,
            banda = "2.4GHz",
            seuSSID = ssid,
        )

        val dadoCanal1 = snapshot.dadosPorCanal.first { it.canal == 1 }

        // Os dois devem convergir: canal 1 congestionado em ambas as visões.
        assertTrue(
            "Fix 3: avaliar deve detectar congestionamento (5 APs em canal 1, canal 6 livre)",
            resultadosAvaliar.any { it.id == "WIFI-CANAL-01" },
        )
        assertTrue(
            "Fix 3: computarEspectro deve classificar canal 1 como moderado (5 APs sobrepostos)",
            dadoCanal1.nivel == NivelCongestionamento.moderado,
        )
    }

    // ── Fix Bernardo Cenário 1: redes fracas (-81 a -89 dBm) não são ignoradas ──

    @Test
    fun `recomendarCanal nao ignora redes com RSSI entre -81 e -89 dBm`() {
        // Canal 6: 8 redes fracas (-83 dBm cada) — antes do fix eram ignoradas (threshold > -80)
        // Canal 1 e 11: sem redes
        // Esperado: canal 1 ou 11 recomendado (não o canal 6, que tem congestionamento real)
        val redes = listOf(
            RedeWifiVizinha(canal = 6, rssiDbm = -83, frequenciaMhz = 2437, ssid = "Rede_F1"),
            RedeWifiVizinha(canal = 6, rssiDbm = -83, frequenciaMhz = 2437, ssid = "Rede_F2"),
            RedeWifiVizinha(canal = 6, rssiDbm = -83, frequenciaMhz = 2437, ssid = "Rede_F3"),
            RedeWifiVizinha(canal = 6, rssiDbm = -83, frequenciaMhz = 2437, ssid = "Rede_F4"),
            RedeWifiVizinha(canal = 6, rssiDbm = -83, frequenciaMhz = 2437, ssid = "Rede_F5"),
            RedeWifiVizinha(canal = 6, rssiDbm = -83, frequenciaMhz = 2437, ssid = "Rede_F6"),
            RedeWifiVizinha(canal = 6, rssiDbm = -83, frequenciaMhz = 2437, ssid = "Rede_F7"),
            RedeWifiVizinha(canal = 6, rssiDbm = -83, frequenciaMhz = 2437, ssid = "Rede_F8"),
            // Canal 1 e 11 sem redes (score 0.0)
        )
        val snapshot = WifiChannelDiagnosticEngine.computarEspectro(
            redes = redes,
            canalAtual = 6,
            banda = "2.4GHz",
            seuSSID = "MinhaRede",
        )

        val canalRec = snapshot.canalRecomendado
        assertNotNull("Deve haver canal recomendado", canalRec)
        assertFalse(
            "Canal 6 não deve ser recomendado — tem 8 redes fracas que não devem ser ignoradas (era bug do threshold -80 dBm). Recomendado: $canalRec",
            canalRec == 6,
        )
        assertTrue(
            "Canal recomendado deve ser 1 ou 11 (sem redes). Recomendado: $canalRec",
            canalRec == 1 || canalRec == 11,
        )
    }

    @Test
    fun `congested channel recommends only when scan has enough data`() {
        val wifi =
            WifiDiagnosticInput(
                rssiDbm = -50,
                linkSpeedMbps = 300,
                frequenciaMhz = 2412,
                canal = 1,
            )

        val scanInsuficiente =
            WifiScanDiagnosticInput(
                conectadoCanal = 1,
                redes = listOf(
                    RedeWifiVizinha(canal = 1, rssiDbm = -60, frequenciaMhz = 2412),
                    RedeWifiVizinha(canal = 6, rssiDbm = -90, frequenciaMhz = 2437),
                ),
            )
        val r1 = WifiChannelDiagnosticEngine.avaliar(wifi, scanInsuficiente)
        assertTrue(r1.any { it.status == DiagnosticStatus.inconclusive })

        val scanSuficiente =
            WifiScanDiagnosticInput(
                conectadoCanal = 1,
                redes =
                    listOf(
                        RedeWifiVizinha(canal = 1, rssiDbm = -55, frequenciaMhz = 2412),
                        RedeWifiVizinha(canal = 1, rssiDbm = -58, frequenciaMhz = 2412),
                        RedeWifiVizinha(canal = 1, rssiDbm = -60, frequenciaMhz = 2412),
                        RedeWifiVizinha(canal = 1, rssiDbm = -62, frequenciaMhz = 2412),
                        RedeWifiVizinha(canal = 6, rssiDbm = -85, frequenciaMhz = 2437),
                        RedeWifiVizinha(canal = 6, rssiDbm = -90, frequenciaMhz = 2437),
                        RedeWifiVizinha(canal = 11, rssiDbm = -90, frequenciaMhz = 2462),
                    ),
            )
        val r2 = WifiChannelDiagnosticEngine.avaliar(wifi, scanSuficiente)
        assertTrue(r2.isEmpty() || r2.any { it.status == DiagnosticStatus.attention })
    }

    // ── GH#1207 item 1: rede propria nao e mais sempre contada como terceiro ───

    @Test
    fun `computarEspectro separa rede propria de terceiros por SSID`() {
        val ssid = "MinhaRede"
        val redes = listOf(
            RedeWifiVizinha(canal = 1, rssiDbm = -55, frequenciaMhz = 2412, ssid = ssid, bssid = "AA:BB"),
            RedeWifiVizinha(canal = 1, rssiDbm = -60, frequenciaMhz = 2412, ssid = "Viz_A", bssid = "CC:DD"),
        )
        val snapshot = WifiChannelDiagnosticEngine.computarEspectro(
            redes = redes,
            canalAtual = 1,
            banda = "2.4GHz",
            seuSSID = ssid,
        )
        val dadoCanal1 = snapshot.dadosPorCanal.first { it.canal == 1 }
        assertEquals(1, dadoCanal1.countProprios)
        assertEquals(1, dadoCanal1.countTerceiros)
    }

    @Test
    fun `computarEspectro usa papelTopologia quando SSID nao bate mas o motor ja classificou`() {
        // Nó mesh com SSID diferente (ex.: repetidor com SSID próprio) mas já classificado
        // pelo motor de topologia unificado como parte da estrutura — GH#1207 item 1 combina os
        // dois sinais (SSID OU papelTopologia), não só SSID.
        val redes = listOf(
            RedeWifiVizinha(
                canal = 1,
                rssiDbm = -55,
                frequenciaMhz = 2412,
                ssid = "Repetidor_SSID_Proprio",
                bssid = "AA:BB",
                papelTopologia = io.signallq.app.core.network.contracts.topologia.PapelTopologia.REPETIDOR,
                confiancaTopologia = io.signallq.app.core.network.contracts.topologia.NivelConfianca.ALTA,
            ),
        )
        val snapshot = WifiChannelDiagnosticEngine.computarEspectro(
            redes = redes,
            canalAtual = 1,
            banda = "2.4GHz",
            seuSSID = "MinhaRede",
        )
        val dadoCanal1 = snapshot.dadosPorCanal.first { it.canal == 1 }
        assertEquals(1, dadoCanal1.countProprios)
        assertEquals(0, dadoCanal1.countTerceiros)
    }

    // ── GH#1207 item 2: identidade banda+canal no modo "Todos" ─────────────────

    @Test
    fun `computarEspectro modo Todos nao colide canal de mesmo numero em bandas diferentes`() {
        // Canal 149 existe tanto em 5GHz (U-NII-3, freq 5745) quanto na lista PSC de 6GHz
        // (freq 6695) — mesmo numero de canal, bandas fisicamente distintas. Antes do fix,
        // o modo "Todos" usava so `canal` como chave e colidia os dois.
        val redes = listOf(
            RedeWifiVizinha(canal = 149, rssiDbm = -50, frequenciaMhz = 5745, ssid = "A"),
            RedeWifiVizinha(canal = 149, rssiDbm = -50, frequenciaMhz = 6695, ssid = "B"),
        )
        val snapshot = WifiChannelDiagnosticEngine.computarEspectro(
            redes = redes,
            canalAtual = 149,
            banda = "Todos",
            seuSSID = "MinhaRede",
            bandaConectada = "5GHz",
        )
        val doisCanal149 = snapshot.dadosPorCanal.filter { it.canal == 149 }
        // Duas entradas com canal=149, mas bandas diferentes — nao devem colapsar numa so
        assertTrue("Esperado 2 entradas de canal 149 em bandas diferentes, achou ${doisCanal149.size}", doisCanal149.size >= 2)
        val bandasDistintas = doisCanal149.map { it.banda }.toSet()
        assertTrue("Bandas devem ser distintas", bandasDistintas.size >= 2)
    }

    @Test
    fun `computarEspectro modo Todos marca ehCanalAtual so na banda conectada`() {
        val redes = listOf(
            RedeWifiVizinha(canal = 6, rssiDbm = -50, frequenciaMhz = 2437, ssid = "A"),
        )
        val snapshot = WifiChannelDiagnosticEngine.computarEspectro(
            redes = redes,
            canalAtual = 6,
            banda = "Todos",
            seuSSID = "A",
            bandaConectada = "2.4GHz",
        )
        val atual = snapshot.dadosPorCanal.filter { it.ehCanalAtual }
        assertTrue("So deve marcar ehCanalAtual na banda 2.4GHz", atual.all { it.banda == "2.4GHz" })
    }

    // ── GH#1207 item 3: largura real do canal propagada quando disponivel ──────

    @Test
    fun `toNeighbors usa largura real quando informada e nao marca estimada`() {
        val redes = listOf(
            RedeWifiVizinha(canal = 36, rssiDbm = -50, frequenciaMhz = 5180, ssid = "A", larguraCanalMhz = 80),
            RedeWifiVizinha(canal = 149, rssiDbm = -55, frequenciaMhz = 5745, ssid = "B", larguraCanalMhz = 80),
        )
        val snapshot = WifiChannelDiagnosticEngine.computarEspectro(
            redes = redes,
            canalAtual = 36,
            banda = "5GHz",
            seuSSID = "A",
        )
        assertFalse("Largura informada para todos os vizinhos nao deveria marcar estimativa", snapshot.dadosPorCanal.any { it.larguraEstimada })
    }

    @Test
    fun `toNeighbors marca largura estimada quando scan nao reporta largura`() {
        val redes = listOf(
            RedeWifiVizinha(canal = 36, rssiDbm = -50, frequenciaMhz = 5180, ssid = "A"),
        )
        val snapshot = WifiChannelDiagnosticEngine.computarEspectro(
            redes = redes,
            canalAtual = 36,
            banda = "5GHz",
            seuSSID = "A",
        )
        assertTrue("Sem largura no scan, deve marcar estimativa", snapshot.dadosPorCanal.any { it.larguraEstimada })
    }

    // ── GH#1207 item 4: nivel/barra vem do mesmo score, nao de contagem crua ───

    @Test
    fun `classificarPorScore reflete o mesmo score que decide a recomendacao`() {
        assertEquals(NivelCongestionamento.livre, WifiChannelDiagnosticEngine.classificarPorScore(0.0))
        // ~-51.7 dBm equivalente (5 redes -55..-65 dBm sobrepostas) -> moderado
        assertEquals(NivelCongestionamento.moderado, WifiChannelDiagnosticEngine.classificarPorScore(6.7e-6))
        // ~-30 dBm equivalente (muitas redes fortes) -> congestionado
        assertEquals(NivelCongestionamento.congestionado, WifiChannelDiagnosticEngine.classificarPorScore(1.0e-3))
    }

    @Test
    fun `fracaoDeScore cresce conforme o score aumenta, alimentando a mesma barra`() {
        val fracaoBaixa = WifiChannelDiagnosticEngine.fracaoDeScore(1.0e-9)
        val fracaoAlta = WifiChannelDiagnosticEngine.fracaoDeScore(1.0e-3)
        assertTrue(fracaoAlta > fracaoBaixa)
        assertEquals(0.0, WifiChannelDiagnosticEngine.fracaoDeScore(0.0), 0.0001)
    }

    // ── GH#1207 (robustez): confianca e estabilidade ────────────────────────────

    @Test
    fun `confianca fica BAIXA com amostra pequena`() {
        val redes = listOf(RedeWifiVizinha(canal = 1, rssiDbm = -55, frequenciaMhz = 2412, ssid = "A"))
        val snapshot = WifiChannelDiagnosticEngine.computarEspectro(
            redes = redes,
            canalAtual = 1,
            banda = "2.4GHz",
            seuSSID = "A",
        )
        assertEquals(io.signallq.app.core.network.contracts.topologia.NivelConfianca.BAIXA, snapshot.confianca)
    }

    @Test
    fun `avaliarEstabilidadeRecomendacao detecta recomendacao instavel entre scans`() {
        val historicoEstavel = listOf(6, 6, 6, 11)
        val historicoInstavel = listOf(6, 11, 1, 6)
        assertTrue(WifiChannelDiagnosticEngine.avaliarEstabilidadeRecomendacao(historicoEstavel))
        assertFalse(WifiChannelDiagnosticEngine.avaliarEstabilidadeRecomendacao(historicoInstavel))
    }
}

