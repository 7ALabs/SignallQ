package io.veloo.app.feature.diagnostico

enum class DiagnosticArea {
    VELOCIDADE,
    WIFI_SINAL,
    LATENCIA,
    FIBRA,
    DNS,
}

/**
 * Executor puro do diagnostico (sem Android/Flow/Log) para facilitar testes unitarios.
 * O DiagnosticOrchestrator so faz o "plumbing" e publica snapshots.
 */
object DiagnosticRunner {

    fun run(
        input: DiagnosticInput,
        enabledAreas: Set<DiagnosticArea> = DiagnosticArea.entries.toSet(),
    ): DiagnosticReport {
        val rodarWifi = DiagnosticArea.WIFI_SINAL in enabledAreas && input.connectionType == ConnectionType.wifi
        val rodarInternet = DiagnosticArea.VELOCIDADE in enabledAreas || DiagnosticArea.LATENCIA in enabledAreas
        val rodarFibra = DiagnosticArea.FIBRA in enabledAreas
        val rodarDns = DiagnosticArea.DNS in enabledAreas

        val wifiQuality =
            if (rodarWifi) {
                WifiSignalQualityEngine.avaliar(input.wifi)
            } else {
                WifiQualityResult(emptyList(), confiavelParaTeste = true)
            }

        val internetResultados =
            if (rodarInternet) {
                InternetDiagnosticEngine.avaliar(
                    input = input.internet,
                    wifiConfiavelParaTeste = wifiQuality.confiavelParaTeste,
                )
            } else {
                emptyList()
            }

        val mobileResultados =
            MobileSignalDiagnosticEngine.avaliar(
                connectionType = input.connectionType,
                input = input.mobile,
            )

        val fibraResultados = if (rodarFibra) FibraSignalQualityEngine.avaliar(input.fibra) else emptyList()
        val dnsResultados = if (rodarDns) DnsDiagnosticEngine.avaliar(input.dns) else emptyList()
        val historicoResultados = HistoricalDegradationEngine.avaliar(input.historico)
        val wifiCanalResultados =
            if (rodarWifi) {
                WifiChannelDiagnosticEngine.avaliar(
                    wifi = input.wifi,
                    scan = input.wifiScan,
                )
            } else {
                emptyList()
            }

        val decisao =
            DiagnosticDecisionEngine.decidir(
                internetResultados = internetResultados + mobileResultados + dnsResultados + historicoResultados + wifiCanalResultados,
                wifiQuality = wifiQuality,
                fibraResultados = fibraResultados,
                rttGatewayMs = input.internet?.rttGatewayMs,
                latenciaInternetMs = input.internet?.latencyMs,
            )

        return DiagnosticReport(
            wifiResultados = wifiQuality.resultados,
            internetResultados = internetResultados,
            mobileResultados = mobileResultados,
            fibraResultados = fibraResultados,
            dnsResultados = dnsResultados,
            historicoResultados = historicoResultados,
            wifiCanalResultados = wifiCanalResultados,
            decisao = decisao,
            perfisUsoSpeedtest = input.internet?.qualidadeUso,
            geradoEmMs = System.currentTimeMillis(),
        )
    }
}
