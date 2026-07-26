package io.signallq.app.core.diagnostico

private const val CAT = "internet"

object InternetDiagnosticEngine {

    fun avaliar(
        input: InternetDiagnosticInput?,
        wifiConfiavelParaTeste: Boolean,
        /** Tipo de conexao ativa. Usado para nao falar de "roteador/modem" quando a
         *  conexao e 100% rede movel (GH#521, mesmo padrao de FindingEngine SIG-514). */
        connectionType: ConnectionType = ConnectionType.wifi,
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
        // NAO migrado para MetricClassifier.classificarPerdaPacotes() (issue #1228 Fase 1,
        // ADR-011): a tabela do classifier usa fronteiras 0.5%/2.0% (excelente/bom/regular/ruim),
        // valores substancialmente diferentes dos limiares de negocio 1.0%/3.0% deste achado —
        // nao e um caso de fronteira coincidente por acidente de estrita-maior-que como jitter,
        // e sim duas reguas de produto distintas. Migrar mudaria o comportamento observavel (ex.:
        // perda=1.5% hoje gera "moderada", classifier ja classificaria como "regular" != "ruim"
        // aos 2.0%, sem gerar achado "critico" nem no ponto certo). Ver achado de arquitetura
        // registrado na issue #1466.
        val emRedeMovel = connectionType == ConnectionType.mobile
        if (perda != null) {
            when {
                perda >= 3.0 -> resultados.add(
                    DiagnosticResult(
                        id = "IN-NORMAL-07",
                        titulo = "Perda de Pacotes Alta",
                        status = DiagnosticStatus.critical,
                        evidencia = "perda=${"%.1f".format(perda)}%",
                        mensagemUsuario = "Há perda de pacotes alta (${"%.1f".format(perda)}%). Calls de vídeo e jogos serão gravemente afetados.",
                        recomendacao = if (emRedeMovel) {
                            "Teste em outro local ou horário. Se persistir, contate a operadora."
                        } else {
                            "Reinicie o roteador e o modem. Se persistir, contate o provedor."
                        },
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

        // IN-NORMAL-06: jitter — migrado para MetricClassifier (issue #1228 Fase 1, ADR-011).
        // classificarJitter() so tem fronteira "ruim" exatamente em >20.0 (mesmo valor e
        // mesma estrita-maior-que do limiar historico deste achado) — unico ponto de verdade
        // sem alterar o resultado observavel (golden test: InternetDiagnosticEngineTest).
        if (jit != null && MetricClassifier.classificarJitter(jit) == MetricStatus.ruim) {
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
        // NAO migrado para MetricClassifier.classificarLatencia() (issue #1228 Fase 1, ADR-011):
        // a tabela do classifier (excelente <100 | bom <=150 | regular <=200 | ruim >200) usa uma
        // fronteira "nao-excelente" em >=100.0 (inclusiva), enquanto este achado historicamente
        // dispara em >100.0 (exclusiva) — divergem no exato ponto lat=100.0, e nenhuma categoria
        // do classifier reproduz o limiar de negocio deste achado sem mudar comportamento
        // observavel (golden test trava lat=100.0 sem achado, lat=100.01 com achado). Tabelas sao
        // de fontes de produto diferentes (skill /regras-diagnostico-rede vs Anatel RQUAL) — ver
        // achado de arquitetura registrado na issue #1466.
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

        // IN-NORMAL-09: bufferbloat — migrado para MetricClassifier (issue #1228 Fase 1, ADR-011).
        // classificarBufferbloat() usa as mesmas fronteiras ja documentadas aqui historicamente
        // (nenhum <5ms | leve <=30ms=bom | moderado <=100ms=regular | severo >100ms=ruim) —
        // regular vira "elevado" (09b), ruim vira "critico" (09), bom/excelente nao geram achado.
        // Unico ponto de verdade sem alterar o resultado observavel (golden test).
        val bb = input.bufferbloatMs
        val bbStatus = bb?.let { MetricClassifier.classificarBufferbloat(it) }
        if (bb != null && (bbStatus == MetricStatus.regular || bbStatus == MetricStatus.ruim)) {
            val isCritico = bbStatus == MetricStatus.ruim
            resultados.add(
                DiagnosticResult(
                    id = if (isCritico) "IN-NORMAL-09" else "IN-NORMAL-09b",
                    titulo = if (isCritico) "Bufferbloat Crítico" else "Bufferbloat Elevado",
                    status = if (isCritico) DiagnosticStatus.critical else DiagnosticStatus.attention,
                    evidencia = "bufferbloat=${"%.0f".format(bb)} ms",
                    mensagemUsuario = if (isCritico)
                        "O bufferbloat está muito alto (${"%.0f".format(bb)} ms). Streaming, jogos e chamadas serão gravemente prejudicados mesmo com velocidade adequada."
                    else
                        "O bufferbloat está elevado (${"%.0f".format(bb)} ms). Jogos e chamadas podem ter instabilidade sob carga.",
                    recomendacao = "Verifique se o roteador suporta QoS ou SQM. Reduza o número de dispositivos usando a rede simultaneamente.",
                    categoria = CAT,
                    podeConcluir = isCritico,
                ),
            )
        }

        // IN-NORMAL-04: upload
        // IN-NORMAL-04Z: upload zerado (prioridade maxima sobre upload baixo generico)
        // NAO migrado para MetricClassifier.classificarUpload() (issue #1228 Fase 1, ADR-011):
        // a tabela do classifier usa fronteiras 1/3/10/20 Mbps (throughput bruto de speedtest),
        // sem nenhuma categoria na fronteira de 5.0 Mbps usada por este achado (limiar de
        // videoconferencia/upload de arquivo) — 4.999 e 5.0 caem na mesma categoria "regular" do
        // classifier, entao nenhuma combinacao de status reproduz o limiar sem mudar
        // comportamento observavel. Ver achado de arquitetura registrado na issue #1466.
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

        // IN-NORMAL-03: download — migrado para MetricClassifier (issue #1228 Fase 1, ADR-011).
        // classificarDownload() tem fronteira regular/ruim exatamente em 25.0 Mbps (mesmo valor
        // e mesma estrita-menor-que do limiar historico deste achado) — trigger "ruim ou critico"
        // reproduz exatamente dl<25.0 sem alterar o resultado observavel (golden test).
        val dlStatus = MetricClassifier.classificarDownload(dl)
        if (dlStatus == MetricStatus.ruim || dlStatus == MetricStatus.critico) {
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
