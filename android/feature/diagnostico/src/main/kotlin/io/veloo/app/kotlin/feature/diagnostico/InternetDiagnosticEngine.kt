package io.signallq.app.feature.diagnostico

private const val CAT = "internet"

object InternetDiagnosticEngine {

    fun avaliar(
        input: InternetDiagnosticInput?,
        wifiConfiavelParaTeste: Boolean,
    ): List<DiagnosticResult> {
        if (input == null) {
            return listOf(
                DiagnosticResult(
                    id = "IN-NORMAL-00",
                    titulo = "Sem Dados de Velocidade",
                    status = DiagnosticStatus.inconclusive,
                    evidencia = null,
                    mensagemUsuario = "Nenhum teste de velocidade disponível para análise.",
                    recomendacao = "Execute um teste de velocidade para obter o diagnóstico completo.",
                    categoria = CAT,
                ),
            )
        }

        // IN-NORMAL-01: internet indisponível
        if (input.downloadMbps == null) {
            return listOf(
                DiagnosticResult(
                    id = "IN-NORMAL-01",
                    titulo = "Internet Indisponível",
                    status = DiagnosticStatus.critical,
                    evidencia = "download=null",
                    mensagemUsuario = "O teste de velocidade não conseguiu medir o download. A internet pode estar sem acesso.",
                    recomendacao = "Verifique se outros sites ou apps funcionam. Se não funcionarem, o problema pode ser no roteador ou no provedor.",
                    categoria = CAT,
                    podeConcluir = true,
                ),
            )
        }

        val resultados = mutableListOf<DiagnosticResult>()
        val dl = input.downloadMbps
        val ul = input.uploadMbps
        val lat = input.latencyMs
        val jit = input.jitterMs
        val perda = input.perdaPercentual

        // IN-NORMAL-07 / 07b: perda de pacotes
        if (perda != null) {
            when {
                perda >= 3.0 -> resultados.add(
                    DiagnosticResult(
                        id = "IN-NORMAL-07",
                        titulo = "Perda de Pacotes Alta",
                        status = DiagnosticStatus.critical,
                        evidencia = "perda=${"%.1f".format(perda)}%",
                        mensagemUsuario = "Há perda de pacotes alta (${"%.1f".format(perda)}%). Calls de vídeo e jogos serão gravemente afetados.",
                        recomendacao = "Reinicie o roteador e o modem. Se persistir, contate o provedor.",
                        categoria = CAT,
                        podeConcluir = true,
                    ),
                )
                perda >= 1.0 -> resultados.add(
                    DiagnosticResult(
                        id = "IN-NORMAL-07b",
                        titulo = "Perda de Pacotes Moderada",
                        status = DiagnosticStatus.attention,
                        evidencia = "perda=${"%.1f".format(perda)}%",
                        mensagemUsuario = "Há alguma perda de pacotes (${"%.1f".format(perda)}%). Jogos e chamadas podem ser afetados.",
                        recomendacao = "Verifique interferências no Wi-Fi ou instabilidade no link.",
                        categoria = CAT,
                    ),
                )
            }
        }

        // IN-NORMAL-06: jitter
        if (jit != null && jit > 20.0) {
            resultados.add(
                DiagnosticResult(
                    id = "IN-NORMAL-06",
                    titulo = "Jitter Elevado",
                    status = DiagnosticStatus.attention,
                    evidencia = "jitter=${"%.0f".format(jit)} ms",
                    mensagemUsuario = "O jitter está alto (${"%.0f".format(jit)} ms). Chamadas de voz e jogos podem ter instabilidade.",
                    recomendacao = "Verifique se há outros dispositivos consumindo a rede. Um jitter alto pode indicar congestionamento.",
                    categoria = CAT,
                ),
            )
        }

        // IN-NORMAL-05: latência (Anatel RQUAL: > 100ms = problema)
        if (lat != null && lat > 100.0) {
            resultados.add(
                DiagnosticResult(
                    id = "IN-NORMAL-05",
                    titulo = "Latência Alta",
                    status = DiagnosticStatus.attention,
                    evidencia = "latencia=${"%.0f".format(lat)} ms",
                    mensagemUsuario = "A latência está acima de 100 ms (${"%.0f".format(lat)} ms), acima da referência Anatel RQUAL.",
                    recomendacao = "Latência alta pode ser causada por congestionamento no provedor ou Wi-Fi com sinal fraco.",
                    categoria = CAT,
                ),
            )
        }

        // IN-NORMAL-09: bufferbloat — thresholds DSLReports/waveform (alinhados ao SpeedtestQualityClassifier):
        //   nenhum < 5ms | leve 5-30ms | moderado 30-100ms | severo > 100ms
        //   Reportar apenas moderado (>30ms) e severo (>100ms) — leve é ruído normal.
        val bb = input.bufferbloatMs
        if (bb != null && bb > 30.0) {
            resultados.add(
                DiagnosticResult(
                    id = if (bb > 100.0) "IN-NORMAL-09" else "IN-NORMAL-09b",
                    titulo = if (bb > 100.0) "Bufferbloat Crítico" else "Bufferbloat Elevado",
                    status = if (bb > 100.0) DiagnosticStatus.critical else DiagnosticStatus.attention,
                    evidencia = "bufferbloat=${"%.0f".format(bb)} ms",
                    mensagemUsuario = if (bb > 100.0)
                        "O bufferbloat está muito alto (${"%.0f".format(bb)} ms). Streaming, jogos e chamadas serão gravemente prejudicados mesmo com velocidade adequada."
                    else
                        "O bufferbloat está elevado (${"%.0f".format(bb)} ms). Jogos e chamadas podem ter instabilidade sob carga.",
                    recomendacao = "Verifique se o roteador suporta QoS ou SQM. Reduza o número de dispositivos usando a rede simultaneamente.",
                    categoria = CAT,
                    podeConcluir = bb > 100.0,
                ),
            )
        }

        // IN-NORMAL-04: upload
        // IN-NORMAL-04Z: upload zerado (prioridade maxima sobre upload baixo generico)
        if (ul != null && ul == 0.0) {
            resultados.add(
                DiagnosticResult(
                    id = "IN-NORMAL-04Z",
                    titulo = "Upload Zerado",
                    status = DiagnosticStatus.critical,
                    evidencia = "upload=0.0 Mbps",
                    mensagemUsuario = "O upload medido foi 0 Mbps. Isso costuma quebrar videoconferencias, jogos online, trabalho remoto e envio de arquivos.",
                    recomendacao = "Verifique se ha algum bloqueio no roteador, QoS mal configurado, ou instabilidade no link. Reinicie o roteador. Se persistir, contate o provedor.",
                    categoria = CAT,
                    podeConcluir = true,
                ),
            )
        }
        if (ul != null && ul > 0.0 && ul < 5.0) {
            resultados.add(
                DiagnosticResult(
                    id = "IN-NORMAL-04",
                    titulo = "Upload Baixo",
                    status = DiagnosticStatus.attention,
                    evidencia = "upload=${"%.1f".format(ul)} Mbps",
                    mensagemUsuario = "O upload está baixo (${"%.1f".format(ul)} Mbps). Videoconferências e envio de arquivos podem ser afetados.",
                    recomendacao = "Verifique se há uploads em andamento em outros dispositivos.",
                    categoria = CAT,
                ),
            )
        }

        // IN-NORMAL-03: download
        if (dl < 25.0) {
            resultados.add(
                DiagnosticResult(
                    id = "IN-NORMAL-03",
                    titulo = "Download Baixo",
                    status = DiagnosticStatus.attention,
                    evidencia = "download=${"%.1f".format(dl)} Mbps",
                    mensagemUsuario = "O download está abaixo de 25 Mbps (${"%.1f".format(dl)} Mbps), mínimo recomendado para uso confortável.",
                    recomendacao = "Verifique se o plano contratado entrega essa velocidade e se outros dispositivos estão consumindo a rede.",
                    categoria = CAT,
                ),
            )
        }

        // IN-NORMAL-08: se WiFi não é confiável e há qualquer problema, marca tudo como inconclusivo
        if (!wifiConfiavelParaTeste && resultados.isNotEmpty()) {
            return resultados.map {
                it.copy(
                    id = "${it.id}-inc",
                    status = DiagnosticStatus.inconclusive,
                    mensagemUsuario = "${it.mensagemUsuario} Porém, o sinal Wi-Fi fraco pode ser a causa real — o teste pode não refletir o link de internet.",
                    recomendacao = "Aproxime-se do roteador e refaça o teste para um diagnóstico confiável.",
                    podeConcluir = false,
                )
            }
        }

        // IN-NORMAL-02: tudo ok
        if (resultados.isEmpty()) {
            resultados.add(
                DiagnosticResult(
                    id = "IN-NORMAL-02",
                    titulo = "Conexão Saudável",
                    status = DiagnosticStatus.ok,
                    evidencia = "dl=${"%.1f".format(dl)} Mbps ul=${ul?.let { "%.1f".format(it) } ?: "—"} Mbps lat=${lat?.let { "%.0f".format(it) } ?: "—"} ms",
                    mensagemUsuario = "Todos os indicadores de internet estão dentro dos parâmetros normais.",
                    recomendacao = null,
                    categoria = CAT,
                    podeConcluir = true,
                ),
            )
        }

        return resultados
    }
}
