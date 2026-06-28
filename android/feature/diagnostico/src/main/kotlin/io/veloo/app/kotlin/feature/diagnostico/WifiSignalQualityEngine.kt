package io.signallq.app.feature.diagnostico

private const val CAT = "wifi"

data class WifiQualityResult(
    val resultados: List<DiagnosticResult>,
    val confiavelParaTeste: Boolean,
)

object WifiSignalQualityEngine {

    fun avaliar(input: WifiDiagnosticInput?): WifiQualityResult {
        if (input == null) {
            return WifiQualityResult(emptyList(), confiavelParaTeste = true)
        }

        val resultados = mutableListOf<DiagnosticResult>()
        val rssi = input.rssiDbm
        val banda = input.banda()

        // RSSI
        val rssiResultado = when {
            rssi == null -> null
            rssi > -60 -> DiagnosticResult(
                id = "WIFI-01",
                titulo = "Sinal Excelente",
                status = DiagnosticStatus.ok,
                evidencia = "${rssi} dBm",
                mensagemUsuario = "O sinal Wi-Fi está excelente, próximo ao roteador.",
                recomendacao = null,
                categoria = CAT,
                podeConcluir = true,
            )
            rssi >= -67 -> DiagnosticResult(
                id = "WIFI-02",
                titulo = "Sinal Bom",
                status = DiagnosticStatus.ok,
                evidencia = "${rssi} dBm",
                mensagemUsuario = "O sinal Wi-Fi está bom para uso normal.",
                recomendacao = null,
                categoria = CAT,
                podeConcluir = true,
            )
            rssi >= -75 -> DiagnosticResult(
                id = "WIFI-03",
                titulo = "Sinal Fraco",
                status = DiagnosticStatus.attention,
                evidencia = "${rssi} dBm",
                mensagemUsuario = "O sinal Wi-Fi está fraco. Isso pode afetar a qualidade da conexão.",
                recomendacao = "Aproxime-se do roteador ou remova obstáculos entre o dispositivo e o roteador.",
                categoria = CAT,
                podeConcluir = false,
            )
            else -> DiagnosticResult(
                id = "WIFI-04",
                titulo = "Sinal Muito Fraco",
                status = DiagnosticStatus.critical,
                evidencia = "${rssi} dBm",
                mensagemUsuario = "O sinal Wi-Fi está muito fraco. A conexão pode ser instável ou indisponível.",
                recomendacao = "Aproxime-se do roteador. Se o sinal persistir fraco, considere usar um repetidor Wi-Fi.",
                categoria = CAT,
                podeConcluir = false,
            )
        }
        if (rssiResultado != null) resultados.add(rssiResultado)

        // Banda
        val bandaResultado = when (banda) {
            BandaWifi.ghz24 -> DiagnosticResult(
                id = "WIFI-05",
                titulo = "Rede 2.4 GHz",
                status = DiagnosticStatus.info,
                evidencia = "${input.frequenciaMhz} MHz",
                mensagemUsuario = "Você está conectado na faixa 2.4 GHz, que tem maior alcance mas menor velocidade.",
                recomendacao = "Se o roteador suportar, conecte-se à rede 5 GHz para melhor desempenho.",
                categoria = CAT,
            )
            BandaWifi.ghz5 -> DiagnosticResult(
                id = "WIFI-06",
                titulo = "Rede 5 GHz",
                status = DiagnosticStatus.ok,
                evidencia = "${input.frequenciaMhz} MHz",
                mensagemUsuario = "Você está conectado na faixa 5 GHz, ideal para velocidade e baixa interferência.",
                recomendacao = null,
                categoria = CAT,
            )
            BandaWifi.desconhecida -> null
        }
        if (bandaResultado != null) resultados.add(bandaResultado)

        // WiFi Standard — informa o padrão de conexão e alerta quando limita velocidade
        val wifiStandardStr = input.wifiStandard
        if (wifiStandardStr != null) {
            val stdResultado = when {
                wifiStandardStr.contains("Wi-Fi 4") -> DiagnosticResult(
                    id = "WIFI-STD-04",
                    titulo = "Padrão Wi-Fi 4 (802.11n)",
                    status = DiagnosticStatus.info,
                    evidencia = wifiStandardStr,
                    mensagemUsuario = "Dispositivo conectado em Wi-Fi 4 (802.11n). Velocidade máxima de enlace: ~150 Mbps em 2.4 GHz ou ~300 Mbps em 5 GHz.",
                    recomendacao = if (banda == BandaWifi.ghz24) "Troque para a rede 5 GHz se disponível, ou considere atualizar o roteador para Wi-Fi 5 ou superior." else null,
                    categoria = CAT,
                )
                wifiStandardStr.contains("Wi-Fi 5") -> DiagnosticResult(
                    id = "WIFI-STD-05",
                    titulo = "Padrão Wi-Fi 5 (802.11ac)",
                    status = DiagnosticStatus.ok,
                    evidencia = wifiStandardStr,
                    mensagemUsuario = "Dispositivo conectado em Wi-Fi 5 (802.11ac) — bom suporte a velocidades acima de 300 Mbps.",
                    recomendacao = null,
                    categoria = CAT,
                )
                wifiStandardStr.contains("Wi-Fi 6") || wifiStandardStr.contains("Wi-Fi 7") -> DiagnosticResult(
                    id = "WIFI-STD-6X",
                    titulo = wifiStandardStr.substringBefore(" ("),
                    status = DiagnosticStatus.ok,
                    evidencia = wifiStandardStr,
                    mensagemUsuario = "Dispositivo conectado em $wifiStandardStr — excelente suporte a velocidades altas e múltiplos dispositivos simultâneos (MU-MIMO).",
                    recomendacao = null,
                    categoria = CAT,
                )
                else -> null
            }
            if (stdResultado != null) resultados.add(stdResultado)
        }

        // Link speed — alerta quando o enlace entre device e roteador é o gargalo
        val linkSpeed = input.linkSpeedMbps
        if (linkSpeed != null) {
            when {
                linkSpeed < 54 -> resultados.add(
                    DiagnosticResult(
                        id = "WIFI-LINK-01",
                        titulo = "Enlace Wi-Fi Muito Lento",
                        status = DiagnosticStatus.attention,
                        evidencia = "$linkSpeed Mbps",
                        mensagemUsuario = "A velocidade de enlace entre o dispositivo e o roteador está muito baixa ($linkSpeed Mbps). Isso pode limitar a velocidade independentemente do plano contratado.",
                        recomendacao = "Aproxime-se do roteador ou verifique interferências no ambiente.",
                        categoria = CAT,
                    )
                )
                linkSpeed < 144 -> resultados.add(
                    DiagnosticResult(
                        id = "WIFI-LINK-02",
                        titulo = "Enlace Wi-Fi Limitado",
                        status = DiagnosticStatus.info,
                        evidencia = "$linkSpeed Mbps",
                        mensagemUsuario = "Enlace Wi-Fi de $linkSpeed Mbps. Se o plano for superior a 100 Mbps, essa velocidade de enlace pode ser um gargalo.",
                        recomendacao = "Para planos acima de 100 Mbps, conecte-se mais próximo ao roteador ou use a faixa 5 GHz.",
                        categoria = CAT,
                    )
                )
            }
        }

        // Dispositivos na rede — quantidade pode causar congestionamento no roteador
        val dispositivos = input.dispositivosNaRede
        if (dispositivos != null && dispositivos > 0) {
            when {
                dispositivos > 20 -> resultados.add(
                    DiagnosticResult(
                        id = "WIFI-DEV-02",
                        titulo = "Muitos Dispositivos na Rede",
                        status = DiagnosticStatus.attention,
                        evidencia = "$dispositivos dispositivos",
                        mensagemUsuario = "$dispositivos dispositivos detectados na rede local. Alta quantidade pode causar congestionamento no roteador e reduzir a velocidade para todos.",
                        recomendacao = "Verifique se há dispositivos desconhecidos e considere um roteador com suporte a MU-MIMO.",
                        categoria = CAT,
                    )
                )
                dispositivos > 10 -> resultados.add(
                    DiagnosticResult(
                        id = "WIFI-DEV-01",
                        titulo = "Vários Dispositivos na Rede",
                        status = DiagnosticStatus.info,
                        evidencia = "$dispositivos dispositivos",
                        mensagemUsuario = "$dispositivos dispositivos detectados na rede. Se a velocidade estiver abaixo do esperado, algum pode estar consumindo banda.",
                        recomendacao = null,
                        categoria = CAT,
                    )
                )
            }
        }

        // Confiabilidade: sinal >= WIFI-03 (>= -75 dBm) é confiável para teste
        val confiavelParaTeste = rssi == null || rssi >= -75

        return WifiQualityResult(resultados = resultados, confiavelParaTeste = confiavelParaTeste)
    }
}
