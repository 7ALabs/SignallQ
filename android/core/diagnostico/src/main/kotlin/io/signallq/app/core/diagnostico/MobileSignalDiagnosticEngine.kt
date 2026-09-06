package io.signallq.app.core.diagnostico

private const val CAT_MOBILE = "mobile"

object MobileSignalDiagnosticEngine {
    fun avaliar(
        connectionType: ConnectionType,
        input: MobileDiagnosticInput?,
    ): List<DiagnosticResult> {
        if (connectionType != ConnectionType.mobile) return emptyList()
        if (input == null) {
            return listOf(
                DiagnosticResult(
                    id = "MOB-INC-00",
                    titulo = "Sem Dados de Sinal Movel",
                    status = DiagnosticStatus.inconclusive,
                    evidencia = null,
                    mensagemUsuario = "Nao foi possivel obter dados de sinal da rede movel para o diagnostico.",
                    recomendacao = "Verifique permissoes e tente novamente em um local com melhor cobertura.",
                    categoria = CAT_MOBILE,
                ),
            )
        }

        val resultados = mutableListOf<DiagnosticResult>()

        val q = input.signalQualityPercent
        when {
            q == null -> Unit
            q <= 25 ->
                resultados.add(
                    DiagnosticResult(
                        id = "MOB-01",
                        titulo = "Sinal Movel Muito Ruim",
                        status = DiagnosticStatus.critical,
                        evidencia = "qualidade=$q% tecnologia=${input.mobileTechnology ?: "—"} operadora=${input.carrierName ?: "—"}",
                        mensagemUsuario = "O sinal da rede movel esta muito ruim. Isso pode indicar cobertura fraca ou congestionamento.",
                        recomendacao = "Tente mudar de local (proximo a janelas) ou alternar modo aviao. Se persistir, pode ser cobertura da operadora.",
                        categoria = CAT_MOBILE,
                        podeConcluir = false,
                    ),
                )
            q <= 40 ->
                resultados.add(
                    DiagnosticResult(
                        id = "MOB-01b",
                        titulo = "Sinal Móvel Ruim",
                        status = DiagnosticStatus.attention,
                        evidencia = "qualidade=$q% tecnologia=${input.mobileTechnology ?: "—"} operadora=${input.carrierName ?: "—"}",
                        mensagemUsuario = "O sinal da rede móvel está ruim. A conexão pode oscilar ou ficar lenta.",
                        recomendacao = "Tente mudar de local ou testar em outro horário para verificar congestionamento.",
                        categoria = CAT_MOBILE,
                    ),
                )
        }

        avaliarThresholdsLocais(input)?.let { resultados.add(it) }

        return resultados
    }

    // ── Thresholds locais RSRP/RSRQ/SINR ─────────────────────────────────────
    // Classificacao delegada ao MetricClassifier (issue #998), unica fonte de
    // thresholds para RSRP/RSRQ/SINR (4G LTE e 5G NR).
    private fun avaliarThresholdsLocais(input: MobileDiagnosticInput): DiagnosticResult? {
        val rsrp = input.rsrpDbm
        val rsrq = input.rsrqDb
        val sinr = input.sinrDb
        if (rsrp == null && rsrq == null && sinr == null) return null

        val tech =
            if (input.mobileTechnology?.startsWith("5G", ignoreCase = true) == true) {
                MetricClassifier.RadioTech.NR_5G
            } else {
                MetricClassifier.RadioTech.LTE_4G
            }

        val statuses = mutableListOf<MetricStatus>()
        rsrp?.let { statuses.add(MetricClassifier.classificarRsrp(it, tech)) }
        rsrq?.let { statuses.add(MetricClassifier.classificarRsrq(it, tech)) }
        sinr?.let { statuses.add(MetricClassifier.classificarSinr(it, tech)) }
        if (statuses.isEmpty()) return null

        val piorStatus = statuses.maxBy { it.ordinal }
        val evidencia =
            "rsrp=${rsrp?.let { "${it}dBm" } ?: "—"} rsrq=${rsrq?.let { "${it}dB" } ?: "—"} " +
                "sinr=${sinr?.let { "${it}dB" } ?: "—"} tecnologia=${input.mobileTechnology ?: "—"}"

        return when (piorStatus) {
            MetricStatus.ruim, MetricStatus.critico ->
                DiagnosticResult(
                    id = "MOB-02",
                    titulo = "Metricas de Sinal Movel Ruins",
                    status = DiagnosticStatus.critical,
                    evidencia = evidencia,
                    mensagemUsuario = "As métricas técnicas do sinal móvel (RSRP/RSRQ/SINR) indicam cobertura ruim ou muita interferência.",
                    recomendacao = "Tente mudar de local ou verificar cobertura da operadora na região.",
                    categoria = CAT_MOBILE,
                    podeConcluir = false,
                )
            MetricStatus.regular ->
                DiagnosticResult(
                    id = "MOB-02b",
                    titulo = "Metricas de Sinal Movel Aceitaveis",
                    status = DiagnosticStatus.attention,
                    evidencia = evidencia,
                    mensagemUsuario = "As métricas técnicas do sinal móvel estão na faixa aceitável, mas abaixo do ideal.",
                    recomendacao = "Se a conexão oscilar, tente um local com melhor linha de visada para a antena.",
                    categoria = CAT_MOBILE,
                )
            MetricStatus.bom, MetricStatus.excelente, MetricStatus.inconclusivo -> null
        }
    }
}
