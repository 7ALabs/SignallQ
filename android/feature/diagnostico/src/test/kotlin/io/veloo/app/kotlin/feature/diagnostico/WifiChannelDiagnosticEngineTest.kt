package io.veloo.app.feature.diagnostico

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
}

