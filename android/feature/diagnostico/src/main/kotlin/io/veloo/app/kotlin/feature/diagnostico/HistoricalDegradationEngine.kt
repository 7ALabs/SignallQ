package io.veloo.app.feature.diagnostico

private const val CAT_HIST = "historico"

object HistoricalDegradationEngine {

    // Regras simples e deterministicas. Se precisarmos de algo mais sofisticado, deve vir de dados brutos.
    private const val MIN_TESTS_7D = 5
    private const val MIN_TESTS_30D = 10

    fun avaliar(input: HistoricalDiagnosticInput?): List<DiagnosticResult> {
        if (input == null) return emptyList()

        val temDadosSuficientes = input.testsCount7d >= MIN_TESTS_7D && input.testsCount30d >= MIN_TESTS_30D
        if (!temDadosSuficientes) {
            // Nao declara tendencia com poucos testes.
            return listOf(
                DiagnosticResult(
                    id = "HIST-INC-01",
                    titulo = "Histórico Insuficiente",
                    status = DiagnosticStatus.inconclusive,
                    evidencia = "tests7d=${input.testsCount7d} tests30d=${input.testsCount30d}",
                    mensagemUsuario = "Ainda não há testes suficientes para detectar tendência de degradação (mínimo ${MIN_TESTS_7D} em 7 dias e ${MIN_TESTS_30D} em 30 dias).",
                    recomendacao = "Execute mais testes ao longo dos próximos dias para termos uma base confiável.",
                    categoria = CAT_HIST,
                ),
            )
        }

        val resultados = mutableListOf<DiagnosticResult>()

        // Comparacao 7d vs 30d: 7d pior que 30d sugere degradacao recente.
        val degradacao =
            calcularDegradacaoPercentual(
                avg30 = input.avgDownload30d,
                avg7 = input.avgDownload7d,
                sentido = SentidoMelhor.maior,
            )
        val degradacaoUp =
            calcularDegradacaoPercentual(
                avg30 = input.avgUpload30d,
                avg7 = input.avgUpload7d,
                sentido = SentidoMelhor.maior,
            )
        val degradacaoPing =
            calcularDegradacaoPercentual(
                avg30 = input.avgPing30d,
                avg7 = input.avgPing7d,
                sentido = SentidoMelhor.menor,
            )
        val degradacaoDns =
            calcularDegradacaoPercentual(
                avg30 = input.avgDns30d,
                avg7 = input.avgDns7d,
                sentido = SentidoMelhor.menor,
            )

        val piores = listOfNotNull(
            degradacao?.let { "download=${fmt(it)}%" },
            degradacaoUp?.let { "upload=${fmt(it)}%" },
            degradacaoPing?.let { "ping=${fmt(it)}%" },
            degradacaoDns?.let { "dns=${fmt(it)}%" },
        )

        val severidade = severidade(piores.mapNotNull { extrairPercentual(it) }.maxOrNull())
        if (severidade != null) {
            resultados.add(
                DiagnosticResult(
                    id = if (severidade == DiagnosticStatus.critical) "HIST-01" else "HIST-01b",
                    titulo = if (severidade == DiagnosticStatus.critical) "Degradacao Detectada" else "Possivel Degradacao",
                    status = severidade,
                    evidencia = "7dvs30d ${piores.joinToString(" ")} worst=${input.worstTimeWindow ?: "—"} best=${input.bestTimeWindow ?: "—"}",
                    mensagemUsuario = "O historico sugere degradacao recente (ultimos 7 dias piores que a media de 30 dias).",
                    recomendacao = "Se a degradacao persistir, tente testar em horarios diferentes e, se possivel, via cabo. Caso confirme, contate o provedor com evidencias.",
                    categoria = CAT_HIST,
                    podeConcluir = severidade == DiagnosticStatus.critical,
                ),
            )
        }

        return resultados
    }

    private enum class SentidoMelhor { maior, menor }

    private fun calcularDegradacaoPercentual(
        avg30: Double?,
        avg7: Double?,
        sentido: SentidoMelhor,
    ): Double? {
        if (avg30 == null || avg7 == null) return null
        if (avg30 <= 0.0) return null

        return when (sentido) {
            SentidoMelhor.maior -> ((avg30 - avg7) / avg30) * 100.0
            SentidoMelhor.menor -> ((avg7 - avg30) / avg30) * 100.0
        }
    }

    private fun severidade(maxPercent: Double?): DiagnosticStatus? {
        if (maxPercent == null) return null
        // Thresholds intencionais e conservadores para evitar falso-positivo.
        return when {
            maxPercent >= 40.0 -> DiagnosticStatus.critical
            maxPercent >= 20.0 -> DiagnosticStatus.attention
            else -> null
        }
    }

    private fun fmt(v: Double): String = String.format(java.util.Locale.US, "%.0f", v)

    private fun extrairPercentual(token: String): Double? {
        val idx = token.indexOf("=")
        val pctIdx = token.indexOf("%")
        if (idx < 0 || pctIdx < 0 || pctIdx <= idx) return null
        return token.substring(idx + 1, pctIdx).toDoubleOrNull()
    }
}

