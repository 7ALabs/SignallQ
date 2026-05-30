package io.veloo.app.feature.diagnostico

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

        val q = input.signalQualityPercent
        if (q == null) return emptyList()

        return when {
            q <= 25 -> listOf(
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
            q <= 40 -> listOf(
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
            else -> emptyList()
        }
    }
}

