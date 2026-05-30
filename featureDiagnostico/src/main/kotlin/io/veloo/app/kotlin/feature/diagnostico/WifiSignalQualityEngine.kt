package io.veloo.app.feature.diagnostico

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

        // Confiabilidade: sinal >= WIFI-03 (>= -75 dBm) é confiável para teste
        val confiavelParaTeste = rssi == null || rssi >= -75

        return WifiQualityResult(resultados = resultados, confiavelParaTeste = confiavelParaTeste)
    }
}
