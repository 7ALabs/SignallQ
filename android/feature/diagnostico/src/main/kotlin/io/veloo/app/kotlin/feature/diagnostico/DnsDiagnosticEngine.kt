package io.signallq.app.feature.diagnostico

private const val CAT_DNS = "dns"

object DnsDiagnosticEngine {

    fun avaliar(input: DnsDiagnosticInput?): List<DiagnosticResult> {
        if (input == null) return emptyList()

        val resultados = mutableListOf<DiagnosticResult>()

        val atual = input.currentDnsLatencyMs
        if (atual != null) {
            when {
                atual > 300 -> resultados.add(
                    DiagnosticResult(
                        id = "DNS-01",
                        titulo = "DNS Muito Lento",
                        status = DiagnosticStatus.critical,
                        evidencia = "dnsAtual=${input.currentDnsName ?: "desconhecido"} ${atual}ms ip=${input.currentDnsIp ?: "—"}",
                        mensagemUsuario = "O DNS atual esta muito lento (${atual} ms). Isso pode causar demora para abrir sites e instabilidade em apps.",
                        recomendacao = "Considere trocar o DNS. Se estiver usando DNS do provedor, testar Cloudflare ou Google DNS pode ajudar.",
                        categoria = CAT_DNS,
                        podeConcluir = true,
                    ),
                )
                atual > 150 -> resultados.add(
                    DiagnosticResult(
                        id = "DNS-02",
                        titulo = "DNS Lento",
                        status = DiagnosticStatus.attention,
                        evidencia = "dnsAtual=${input.currentDnsName ?: "desconhecido"} ${atual}ms ip=${input.currentDnsIp ?: "—"}",
                        mensagemUsuario = "O DNS atual esta lento (${atual} ms). Sites podem demorar mais para carregar.",
                        recomendacao = "Compare com outros DNS e considere trocar se houver opcao melhor.",
                        categoria = CAT_DNS,
                    ),
                )
                atual > 50 -> resultados.add(
                    DiagnosticResult(
                        id = "DNS-03",
                        titulo = "DNS Acima do Ideal",
                        status = DiagnosticStatus.info,
                        evidencia = "dnsAtual=${input.currentDnsName ?: "desconhecido"} ${atual}ms ip=${input.currentDnsIp ?: "—"}",
                        mensagemUsuario = "O DNS atual (${atual} ms) está um pouco acima do ideal. Navegação normal não é afetada, mas apps que abrem muitas conexões podem perceber demora.",
                        recomendacao = null,
                        categoria = CAT_DNS,
                    ),
                )
            }
        } else if (input.dnsComparisonAvailable) {
            resultados.add(
                DiagnosticResult(
                    id = "DNS-00",
                    titulo = "DNS Sem Medicao do Atual",
                    status = DiagnosticStatus.inconclusive,
                    evidencia = null,
                    mensagemUsuario = "Ha comparativo de DNS, mas nao foi possivel medir a latencia do DNS atual em uso.",
                    recomendacao = "Refaca o benchmark de DNS e tente novamente.",
                    categoria = CAT_DNS,
                ),
            )
        }

        if (input.dnsComparisonAvailable) {
            val bestMs = input.bestDnsLatencyMsFromComparison
            val bestName = input.bestDnsNameFromComparison
            if (atual != null && bestMs != null && bestName != null) {
                val melhorQueAtual = bestMs + 5 < atual // evita "recomendacao" por ruido
                val nomeAtual = (input.currentDnsName ?: "").trim().lowercase()
                val nomeBest = bestName.trim().lowercase()
                if (melhorQueAtual && (nomeAtual.isBlank() || nomeAtual != nomeBest)) {
                    resultados.add(
                        DiagnosticResult(
                            id = "DNS-REC-01",
                            titulo = "Melhor DNS Encontrado",
                            status = DiagnosticStatus.info,
                            evidencia = "dnsAtual=${atual}ms melhor=${bestName}=${bestMs}ms grade=${input.dnsGrade ?: "—"}",
                            mensagemUsuario = "Encontramos um DNS melhor no comparativo: $bestName (${bestMs} ms).",
                            recomendacao = "Considere configurar $bestName como DNS no roteador ou no Android (DNS privado), e refaça o teste.",
                            categoria = CAT_DNS,
                        ),
                    )
                }
            }
        }

        avaliarCoerencia(input)?.let { resultados.add(it) }

        return resultados
    }

    // Resultado de AvaliadorCoerenciaDns.registrarCoerencia() (feature/dns), repassado
    // como primitivos em DnsDiagnosticInput — feature/diagnostico nao pode depender de
    // feature/dns (lei de dependencias :feature* -> :feature* proibido).
    private fun avaliarCoerencia(input: DnsDiagnosticInput): DiagnosticResult? {
        val nivel = input.coerenciaNivelAlerta ?: return null
        val consecutivas = input.coerenciaDivergenciasConsecutivas ?: 0
        val taxa = input.coerenciaTaxaDivergenciaPercentual ?: 0.0
        val evidencia = "nivel=$nivel consecutivas=$consecutivas taxaDivergencia=${"%.1f".format(taxa)}%"

        return when (nivel) {
            "critical" -> DiagnosticResult(
                id = "DNS-COERENCIA-01",
                titulo = "DNS Divergente da Configuracao",
                status = DiagnosticStatus.critical,
                evidencia = evidencia,
                mensagemUsuario = "O DNS efetivamente em uso diverge repetidamente do DNS configurado. Isso pode indicar que a configuracao nao esta sendo aplicada pela operadora ou pelo roteador.",
                recomendacao = "Verifique se o DNS privado/configurado esta realmente ativo. Em redes moveis, algumas operadoras ignoram configuracoes de DNS.",
                categoria = CAT_DNS,
                podeConcluir = false,
            )
            "attention" -> DiagnosticResult(
                id = "DNS-COERENCIA-02",
                titulo = "DNS Instavel",
                status = DiagnosticStatus.attention,
                evidencia = evidencia,
                mensagemUsuario = "O DNS em uso divergiu do configurado em parte das medicoes recentes.",
                recomendacao = "Acompanhe se a divergencia persiste. Pode indicar troca automatica de DNS pela rede.",
                categoria = CAT_DNS,
            )
            else -> null
        }
    }
}

