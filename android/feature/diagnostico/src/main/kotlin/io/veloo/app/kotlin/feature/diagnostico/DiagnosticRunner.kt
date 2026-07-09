package io.signallq.app.feature.diagnostico

import io.signallq.app.feature.diagnostico.topology.model.NatStatus

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
                    connectionType = input.connectionType,
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
        val redeResultados = avaliarNat(input.natStatus)

        val achados =
            FindingEngine.analisar(
                internetResultados = internetResultados + mobileResultados + dnsResultados + historicoResultados + wifiCanalResultados + redeResultados,
                wifiQuality = wifiQuality,
                fibraResultados = fibraResultados,
                rttGatewayMs = input.internet?.rttGatewayMs,
                latenciaInternetMs = input.internet?.latencyMs,
                connectionType = input.connectionType,
                localDevice = input.localDevice,
            )

        val recomendacoes = RecommendationEngine.recomendar(input = input, achados = achados)

        val reportParcial = DiagnosticReport(
            wifiResultados = wifiQuality.resultados,
            internetResultados = internetResultados,
            mobileResultados = mobileResultados,
            fibraResultados = fibraResultados,
            dnsResultados = dnsResultados,
            historicoResultados = historicoResultados,
            wifiCanalResultados = wifiCanalResultados,
            redeResultados = redeResultados,
            decisao = achados.principal,
            achadosSecundarios = achados.secundarios,
            hipotesesDescartadas = achados.hipotesesDescartadas,
            dadosAusentes = achados.dadosAusentes,
            limitacoesEquipamentoLocal = achados.limitacoesEquipamentoLocal,
            recomendacoes = recomendacoes,
            perfisUsoSpeedtest = input.internet?.qualidadeUso,
            perfisUso = UsageProfileClassifier.classificarTodos(input),
            gameReadiness = GameReadinessClassifier.classificarTodos(input),
            geradoEmMs = System.currentTimeMillis(),
        )

        val scoreResultado = ScoreEngine.calcular(
            tipo = ScoreEvidenceBuilder.tipoConexao(input),
            evidencias = ScoreEvidenceBuilder.construir(input, reportParcial),
        )

        return reportParcial.copy(scoreEngineResultado = scoreResultado)
    }

    private const val CAT_REDE = "rede"

    // NAT/CGNAT nao e por si so um problema, mas explica sintomas (ex.: jogos P2P,
    // conexoes de entrada, alguns fallbacks de VPN). Reportado como "info" — nao eleva
    // o veredito geral da conexao.
    private fun avaliarNat(nat: NatStatus?): List<DiagnosticResult> {
        if (nat == null || nat == NatStatus.UNKNOWN || nat == NatStatus.DIRECT_PUBLIC) return emptyList()

        val (titulo, mensagem) = when (nat) {
            NatStatus.CGNAT -> "CGNAT Detectado" to
                "Sua conexao esta atras de CGNAT (Carrier-Grade NAT) — o IP publico e compartilhado entre varios clientes da operadora."
            NatStatus.DOUBLE_NAT_OR_CGNAT -> "NAT Duplo Detectado" to
                "Sua conexao parece estar atras de NAT duplo (roteador + outro NAT antes da internet)."
            else -> return emptyList()
        }

        return listOf(
            DiagnosticResult(
                id = "REDE-NAT-01",
                titulo = titulo,
                status = DiagnosticStatus.info,
                evidencia = "natStatus=${nat.name}",
                mensagemUsuario = mensagem,
                recomendacao = "Isso pode afetar port forwarding, jogos com conexao direta (P2P) e alguns servidores hospedados em casa.",
                categoria = CAT_REDE,
            ),
        )
    }
}
