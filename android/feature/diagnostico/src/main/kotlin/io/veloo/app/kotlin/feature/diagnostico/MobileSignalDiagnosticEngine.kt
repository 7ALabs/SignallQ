package io.signallq.app.feature.diagnostico

private const val CAT_MOBILE = "mobile"

/** Nivel de qualidade de uma metrica de sinal movel (RSRP/RSRQ/SINR). */
private enum class NivelSinalMovel { excelente, bom, aceitavel, ruim }

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
            q <= 25 -> resultados.add(
                DiagnosticResult(
                    id = "MOB-01",
                    titulo = "Sinal Movel Muito Ruim",
                    status = DiagnosticStatus.critical,
                    evidencia = "qualidade=${q}% tecnologia=${input.mobileTechnology ?: "—"} operadora=${input.carrierName ?: "—"}",
                    mensagemUsuario = "O sinal da rede movel esta muito ruim. Isso pode indicar cobertura fraca ou congestionamento.",
                    recomendacao = "Tente mudar de local (proximo a janelas) ou alternar modo aviao. Se persistir, pode ser cobertura da operadora.",
                    categoria = CAT_MOBILE,
                    podeConcluir = false,
                ),
            )
            q <= 40 -> resultados.add(
                DiagnosticResult(
                    id = "MOB-01b",
                    titulo = "Sinal Móvel Ruim",
                    status = DiagnosticStatus.attention,
                    evidencia = "qualidade=${q}% tecnologia=${input.mobileTechnology ?: "—"} operadora=${input.carrierName ?: "—"}",
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
    // Tabela unica de referencia (fonte: skill /regras-diagnostico-rede). NAO usar os
    // valores divergentes de MovelSnapshot.kt (kdoc desatualizado) nem os da regra 15b
    // do prompt da IA — esta e a fonte da verdade para o motor local.
    //
    // 4G LTE:
    //   RSRP: excelente >-80 | bom -80..-90 | aceitavel -90..-100 | ruim <-100 (dBm)
    //   RSRQ: excelente >-10 | bom -10..-15 | aceitavel -15..-20  | ruim <-20  (dB)
    //   SINR: excelente >20  | bom 13..20   | aceitavel 0..13     | ruim <0   (dB)
    // 5G NR:
    //   RSRP: excelente >-80 | bom -80..-95 | aceitavel -95..-110 | ruim <-110 (dBm)
    //   RSRQ: mesma faixa do 4G (sem tabela propria documentada para 5G)
    //   SINR: excelente >20  | bom 10..20   | aceitavel 0..10     | ruim <0    (dB)
    private fun avaliarThresholdsLocais(input: MobileDiagnosticInput): DiagnosticResult? {
        val rsrp = input.rsrpDbm
        val rsrq = input.rsrqDb
        val sinr = input.sinrDb
        if (rsrp == null && rsrq == null && sinr == null) return null

        val is5g = input.mobileTechnology?.startsWith("5G", ignoreCase = true) == true

        val niveis = mutableListOf<NivelSinalMovel>()
        rsrp?.let { niveis.add(classificarRsrp(it, is5g)) }
        rsrq?.let { niveis.add(classificarRsrq(it)) }
        sinr?.let { niveis.add(classificarSinr(it, is5g)) }
        if (niveis.isEmpty()) return null

        val piorNivel = niveis.maxBy { it.ordinal }
        val evidencia =
            "rsrp=${rsrp?.let { "${it}dBm" } ?: "—"} rsrq=${rsrq?.let { "${it}dB" } ?: "—"} " +
                "sinr=${sinr?.let { "${it}dB" } ?: "—"} tecnologia=${input.mobileTechnology ?: "—"}"

        return when (piorNivel) {
            NivelSinalMovel.ruim -> DiagnosticResult(
                id = "MOB-02",
                titulo = "Metricas de Sinal Movel Ruins",
                status = DiagnosticStatus.critical,
                evidencia = evidencia,
                mensagemUsuario = "As métricas técnicas do sinal móvel (RSRP/RSRQ/SINR) indicam cobertura ruim ou muita interferência.",
                recomendacao = "Tente mudar de local ou verificar cobertura da operadora na região.",
                categoria = CAT_MOBILE,
                podeConcluir = false,
            )
            NivelSinalMovel.aceitavel -> DiagnosticResult(
                id = "MOB-02b",
                titulo = "Metricas de Sinal Movel Aceitaveis",
                status = DiagnosticStatus.attention,
                evidencia = evidencia,
                mensagemUsuario = "As métricas técnicas do sinal móvel estão na faixa aceitável, mas abaixo do ideal.",
                recomendacao = "Se a conexão oscilar, tente um local com melhor linha de visada para a antena.",
                categoria = CAT_MOBILE,
            )
            NivelSinalMovel.bom, NivelSinalMovel.excelente -> null
        }
    }

    private fun classificarRsrp(rsrpDbm: Int, is5g: Boolean): NivelSinalMovel = when {
        rsrpDbm > -80 -> NivelSinalMovel.excelente
        is5g && rsrpDbm > -95 -> NivelSinalMovel.bom
        !is5g && rsrpDbm > -90 -> NivelSinalMovel.bom
        is5g && rsrpDbm > -110 -> NivelSinalMovel.aceitavel
        !is5g && rsrpDbm > -100 -> NivelSinalMovel.aceitavel
        else -> NivelSinalMovel.ruim
    }

    // RSRQ 5G usa a mesma faixa do 4G — sem tabela propria documentada.
    private fun classificarRsrq(rsrqDb: Int): NivelSinalMovel = when {
        rsrqDb > -10 -> NivelSinalMovel.excelente
        rsrqDb > -15 -> NivelSinalMovel.bom
        rsrqDb > -20 -> NivelSinalMovel.aceitavel
        else -> NivelSinalMovel.ruim
    }

    private fun classificarSinr(sinrDb: Int, is5g: Boolean): NivelSinalMovel = when {
        sinrDb > 20 -> NivelSinalMovel.excelente
        is5g && sinrDb > 10 -> NivelSinalMovel.bom
        !is5g && sinrDb > 13 -> NivelSinalMovel.bom
        sinrDb > 0 -> NivelSinalMovel.aceitavel
        else -> NivelSinalMovel.ruim
    }
}

