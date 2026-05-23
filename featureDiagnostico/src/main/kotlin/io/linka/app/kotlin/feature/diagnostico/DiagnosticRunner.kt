package io.linka.app.kotlin.feature.diagnostico

/**
 * Executor puro do diagnostico (sem Android/Flow/Log) para facilitar testes unitarios.
 * O DiagnosticOrchestrator so faz o "plumbing" e publica snapshots.
 */
object DiagnosticRunner {

    fun run(input: DiagnosticInput): DiagnosticReport {
        val wifiQuality =
            if (input.connectionType == ConnectionType.wifi) {
                WifiSignalQualityEngine.avaliar(input.wifi)
            } else {
                WifiQualityResult(emptyList(), confiavelParaTeste = true)
            }

        val internetResultados =
            InternetDiagnosticEngine.avaliar(
                input = input.internet,
                wifiConfiavelParaTeste = wifiQuality.confiavelParaTeste,
            )

        val mobileResultados =
            MobileSignalDiagnosticEngine.avaliar(
                connectionType = input.connectionType,
                input = input.mobile,
            )

        val fibraResultados = FibraSignalQualityEngine.avaliar(input.fibra)
        val dnsResultados = DnsDiagnosticEngine.avaliar(input.dns)
        val historicoResultados = HistoricalDegradationEngine.avaliar(input.historico)
        val wifiCanalResultados =
            if (input.connectionType == ConnectionType.wifi) {
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
