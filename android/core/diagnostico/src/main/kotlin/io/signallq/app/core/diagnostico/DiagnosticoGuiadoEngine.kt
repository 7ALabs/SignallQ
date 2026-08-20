package io.signallq.app.core.diagnostico

/**
 * Motor local determinístico do diagnóstico guiado por objetivo (Feature #550,
 * issue #1475). Recebe o [ObjetivoDiagnostico] escolhido, as respostas das
 * [PerguntaFechada] (por índice de opção, não texto) e o [DiagnosticInput] real do
 * teste — devolve um [ResultadoDiagnosticoGuiado] com status/evidências/ações.
 *
 * ## Regra de produto (não-negociável, issue #1475/#550)
 * Este objeto é a ÚNICA fonte do `status`/evidências. A camada de IA (ver
 * `AnalisadorState`/`onAnalisarProblema` em `:app`) só pode **explicar** o
 * resultado já decidido aqui — nunca alterar [ResultadoDiagnosticoGuiado.status],
 * nunca inventar evidência sem métrica real por trás, nunca sugerir compra sem
 * recorrência comprovada (fora do escopo deste motor, que não lida com catálogo
 * de produtos). Ver `AiVsMotorExplainer` no protótipo #1474.
 *
 * ## Como cada objetivo prioriza métricas (issue #1475, critério "cada um
 * priorizando as métricas relevantes daquele objetivo")
 * - [ObjetivoDiagnostico.INTERNET_CAI_OSCILA]: perda de pacotes + jitter (pior faixa vence).
 * - [ObjetivoDiagnostico.VIDEOS_TRAVAM]: atraso sob carga (bufferbloat) + download.
 * - [ObjetivoDiagnostico.JOGOS_COM_LAG]: latência + jitter + perda (mesmas 3 dimensões
 *   de [GameReadinessClassifier] FPS competitivo) — resposta "Cabo de rede" na 1ª
 *   pergunta desativa a penalidade de Wi-Fi fraco (não faz sentido puxar RSSI se o
 *   usuário já disse que joga por cabo).
 * - [ObjetivoDiagnostico.CHAMADAS_CONGELAM]: jitter + perda + upload.
 * - [ObjetivoDiagnostico.SITES_DEMORAM]: latência de DNS quando disponível (gargalo
 *   mais específico de "sites demoram"); cai para latência geral quando o teste não
 *   mediu DNS.
 * - [ObjetivoDiagnostico.VELOCIDADE_NAO_CHEGA]: download medido vs. plano contratado
 *   ([DiagnosticInput.velocidadeContratadaMbps]) quando o usuário informou o plano;
 *   cai para a régua genérica de download quando não informou.
 * - [ObjetivoDiagnostico.WIFI_VS_OPERADORA]: sinal da conexão atual (RSSI Wi-Fi ou
 *   sinal móvel, conforme [DiagnosticInput.connectionType]) combinado com a resposta
 *   auto-relatada da 1ª pergunta (nunca é a única evidência — sempre soma ao dado
 *   medido, não substitui).
 */
object DiagnosticoGuiadoEngine {

    fun avaliar(
        objetivo: ObjetivoDiagnostico,
        respostas: List<Int>,
        input: DiagnosticInput?,
    ): ResultadoDiagnosticoGuiado {
        val avaliador =
            when (objetivo) {
                ObjetivoDiagnostico.INTERNET_CAI_OSCILA -> ::avaliarInternetCaiOscila
                ObjetivoDiagnostico.VIDEOS_TRAVAM -> ::avaliarVideosTravam
                ObjetivoDiagnostico.JOGOS_COM_LAG -> ::avaliarJogosComLag
                ObjetivoDiagnostico.CHAMADAS_CONGELAM -> ::avaliarChamadasCongelam
                ObjetivoDiagnostico.SITES_DEMORAM -> ::avaliarSitesDemoram
                ObjetivoDiagnostico.VELOCIDADE_NAO_CHEGA -> ::avaliarVelocidadeNaoChega
                ObjetivoDiagnostico.WIFI_VS_OPERADORA -> ::avaliarWifiVsOperadora
            }
        return avaliador(respostas, input)
    }

    // ── Internet cai ou oscila ───────────────────────────────────────────────
    private fun avaliarInternetCaiOscila(
        @Suppress("UNUSED_PARAMETER") respostas: List<Int>,
        input: DiagnosticInput?,
    ): ResultadoDiagnosticoGuiado {
        val internet = input?.internet
        val dims = mutableListOf<Dimensao>()
        internet?.perdaPercentual?.let {
            dims += Dimensao("Falhas estimadas na conexão", "%.1f%%".format(it), MetricClassifier.classificarPerdaPacotes(it))
        }
        internet?.jitterMs?.let {
            dims += Dimensao("Variação da conexão", "%.0f ms".format(it), MetricClassifier.classificarJitter(it))
        }
        return montarResultado(
            objetivo = ObjetivoDiagnostico.INTERNET_CAI_OSCILA,
            dims = dims,
            mensagens = MensagensStatus(
                ok = "Não encontramos falhas na conexão nem oscilação fora do esperado nos últimos testes.",
                atencao = "Sinais de instabilidade na sua rede: falhas na conexão ou oscilação um pouco acima do ideal.",
                critica = "Sinal de instabilidade real na sua rede: falhas na conexão e oscilação acima do esperado para uma conexão estável.",
            ),
            acoes = { status ->
                if (status == DiagnosticStatus.ok) {
                    emptyList()
                } else {
                    listOf(
                        "Reposicione o roteador longe de paredes e eletrônicos",
                        "Teste por cabo para isolar se é Wi-Fi ou operadora",
                    )
                }
            },
        )
    }

    // ── Vídeos travam ou dão buffer ──────────────────────────────────────────
    private fun avaliarVideosTravam(
        @Suppress("UNUSED_PARAMETER") respostas: List<Int>,
        input: DiagnosticInput?,
    ): ResultadoDiagnosticoGuiado {
        val internet = input?.internet
        val dims = mutableListOf<Dimensao>()
        internet?.bufferbloatMs?.let {
            dims += Dimensao("Lentidão com a rede ocupada", "%.0f ms".format(it), MetricClassifier.classificarBufferbloat(it))
        }
        internet?.downloadMbps?.let {
            dims += Dimensao("Download", "%.1f Mbps".format(it), MetricClassifier.classificarDownload(it))
        }
        return montarResultado(
            objetivo = ObjetivoDiagnostico.VIDEOS_TRAVAM,
            dims = dims,
            mensagens = MensagensStatus(
                ok = "Download e atraso sob carga dentro do esperado. Sua rede aguenta streaming em boa qualidade.",
                atencao = "O atraso sob carga ou o download ficaram no limite. O streaming pode perder qualidade em horário de pico.",
                critica = "O download cai bastante sob carga, com atraso alto. Isso costuma travar o vídeo ou derrubar a qualidade sozinho.",
            ),
            acoes = { status ->
                if (status == DiagnosticStatus.ok) {
                    emptyList()
                } else {
                    listOf(
                        "Pause downloads em segundo plano durante o streaming",
                        "Teste novamente fora do horário de pico",
                    )
                }
            },
        )
    }

    // ── Jogos com lag ou ping alto ───────────────────────────────────────────
    private fun avaliarJogosComLag(
        respostas: List<Int>,
        input: DiagnosticInput?,
    ): ResultadoDiagnosticoGuiado {
        val internet = input?.internet
        val jogaPorCabo = respostas.getOrNull(0) == 1
        // Issue #1667 (convergência entrada guiada + Modo gamer) — mesma construção de
        // latência+jitter+perda de [ModoGamerEngine.avaliarFpsCompetitivo]/`avaliarOutro`
        // (mesmo perfil de jogo competitivo/genérico). Não recalcula, não duplica: as duas
        // entradas do único fluxo "problema com jogos" priorizam as mesmas 3 métricas a
        // partir daqui.
        val dims = dimsLatenciaJitterPerda(internet)
        // Wi-Fi fraco só entra como evidência quando o usuário não disse que joga por cabo —
        // mesmo princípio de GameReadinessClassifier.aplicarPenalidadeWifi.
        if (!jogaPorCabo) {
            input?.wifi?.rssiDbm?.let { rssi ->
                val banda = input.wifi.banda()
                val wifiBand = if (banda == BandaWifi.ghz24) MetricClassifier.WifiBand.GHZ_2_4 else MetricClassifier.WifiBand.GHZ_5
                dims += Dimensao("Força do sinal Wi-Fi", "$rssi dBm", MetricClassifier.classificarRssiWifi(rssi, wifiBand))
            }
        }
        return montarResultado(
            objetivo = ObjetivoDiagnostico.JOGOS_COM_LAG,
            dims = dims,
            mensagens = MensagensStatus(
                ok = "Tempo de resposta, variação do tempo de resposta e falhas na conexão dentro da faixa recomendada para jogos online.",
                atencao = "O tempo de resposta sob carga ou a variação do tempo de resposta estão no limite. Pode haver atraso perceptível em momentos de disputa.",
                critica = "O tempo de resposta sob carga está prejudicando suas partidas. Faixa crítica para jogos competitivos.",
            ),
            acoes = { status ->
                if (status == DiagnosticStatus.ok) {
                    emptyList()
                } else if (jogaPorCabo) {
                    listOf("Ative priorização de jogos (prioridade de tráfego) no roteador, se disponível")
                } else {
                    listOf(
                        "Jogue com cabo de rede em vez de Wi-Fi",
                        "Ative priorização de jogos (prioridade de tráfego) no roteador, se disponível",
                    )
                }
            },
        )
    }

    // ── Chamadas de vídeo congelam ───────────────────────────────────────────
    private fun avaliarChamadasCongelam(
        @Suppress("UNUSED_PARAMETER") respostas: List<Int>,
        input: DiagnosticInput?,
    ): ResultadoDiagnosticoGuiado {
        val internet = input?.internet
        val dims = mutableListOf<Dimensao>()
        internet?.jitterMs?.let {
            dims += Dimensao("Variação do tempo de resposta", "%.0f ms".format(it), MetricClassifier.classificarJitter(it))
        }
        internet?.perdaPercentual?.let {
            dims += Dimensao("Falhas estimadas na conexão", "%.1f%%".format(it), MetricClassifier.classificarPerdaPacotes(it))
        }
        internet?.uploadMbps?.let {
            dims += Dimensao("Upload", "%.1f Mbps".format(it), MetricClassifier.classificarUpload(it))
        }
        return montarResultado(
            objetivo = ObjetivoDiagnostico.CHAMADAS_CONGELAM,
            dims = dims,
            mensagens = MensagensStatus(
                ok = "A variação do tempo de resposta, a perda e o upload estão dentro do esperado. A rede sustenta uma chamada de vídeo estável.",
                atencao = "A variação do tempo de resposta ou o upload estão no limite. As chamadas podem sofrer quando a rede está mais ocupada.",
                critica = "A variação do tempo de resposta está alta e/ou o upload está comprometido. Isso costuma congelar a imagem ou cortar o áudio na chamada.",
            ),
            acoes = { status ->
                if (status == DiagnosticStatus.ok) {
                    emptyList()
                } else {
                    listOf(
                        "Peça para outras pessoas em casa pausarem downloads durante a chamada",
                        "Prefira cabo de rede para chamadas importantes",
                    )
                }
            },
        )
    }

    // ── Sites demoram para carregar ──────────────────────────────────────────
    private fun avaliarSitesDemoram(
        @Suppress("UNUSED_PARAMETER") respostas: List<Int>,
        input: DiagnosticInput?,
    ): ResultadoDiagnosticoGuiado {
        val internet = input?.internet
        val dns = input?.dns
        val dims = mutableListOf<Dimensao>()
        val dnsLatencia = dns?.currentDnsLatencyMs
        if (dnsLatencia != null) {
            dims += Dimensao("Tempo para localizar sites", "$dnsLatencia ms", MetricClassifier.classificarLatenciaDns(dnsLatencia))
        } else {
            internet?.latencyMs?.let {
                dims += Dimensao("Tempo de resposta", "%.0f ms".format(it), MetricClassifier.classificarLatencia(it))
            }
        }
        return montarResultado(
            objetivo = ObjetivoDiagnostico.SITES_DEMORAM,
            dims = dims,
            mensagens = MensagensStatus(
                ok = "O tempo de resposta está dentro do esperado. Os sites devem abrir sem demora perceptível.",
                atencao = "A resolução de nomes (serviço que localiza sites) ou a tempo de resposta geral estão um pouco lentas.",
                critica = "O tempo de resposta do DNS ou o tempo de resposta geral estão altos. Isso costuma atrasar o carregamento de sites.",
            ),
            acoes = { status ->
                if (status == DiagnosticStatus.ok) {
                    emptyList()
                } else if (dnsLatencia != null) {
                    listOf(
                        "Troque o DNS da rede nas configurações. Um DNS mais rápido pode acelerar a abertura de sites",
                        "Teste novamente em outro navegador para descartar cache",
                    )
                } else {
                    listOf("Repita o teste com a medição de DNS ativa para isolar melhor a causa")
                }
            },
        )
    }

    // ── Velocidade não chega no contratado ───────────────────────────────────
    private fun avaliarVelocidadeNaoChega(
        @Suppress("UNUSED_PARAMETER") respostas: List<Int>,
        input: DiagnosticInput?,
    ): ResultadoDiagnosticoGuiado {
        val internet = input?.internet
        val download = internet?.downloadMbps
        val contratado = input?.velocidadeContratadaMbps
        val dims = mutableListOf<Dimensao>()
        if (download != null && contratado != null && contratado > 0) {
            val percentual = (download / contratado) * 100.0
            val status =
                when {
                    percentual >= 90.0 -> MetricStatus.excelente
                    percentual >= 70.0 -> MetricStatus.bom
                    percentual >= 50.0 -> MetricStatus.regular
                    percentual >= 30.0 -> MetricStatus.ruim
                    else -> MetricStatus.critico
                }
            dims += Dimensao("Velocidade recebida do plano", "%.0f%%".format(percentual), status)
        } else if (download != null) {
            dims += Dimensao("Download", "%.1f Mbps".format(download), MetricClassifier.classificarDownload(download))
        }
        return montarResultado(
            objetivo = ObjetivoDiagnostico.VELOCIDADE_NAO_CHEGA,
            dims = dims,
            mensagens = MensagensStatus(
                ok = "O download medido está próximo ou acima do que você contratou.",
                atencao = "O download ficou um pouco abaixo do esperado para o plano contratado.",
                critica = "Resultado bem abaixo do plano contratado.",
            ),
            acoes = { status ->
                if (status == DiagnosticStatus.ok) {
                    emptyList()
                } else {
                    listOf(
                        "Repita o teste conectado por cabo de rede",
                        "Se o resultado persistir por cabo, contate a operadora com este resultado",
                    )
                }
            },
        )
    }

    // ── Não sei se é o Wi-Fi ou a operadora ──────────────────────────────────
    private fun avaliarWifiVsOperadora(
        respostas: List<Int>,
        input: DiagnosticInput?,
    ): ResultadoDiagnosticoGuiado {
        val melhoraDesligandoWifi = respostas.getOrNull(0)
        val dims = mutableListOf<Dimensao>()

        when (input?.connectionType) {
            ConnectionType.wifi ->
                input.wifi?.rssiDbm?.let { rssi ->
                    val banda = input.wifi.banda()
                    val wifiBand = if (banda == BandaWifi.ghz24) MetricClassifier.WifiBand.GHZ_2_4 else MetricClassifier.WifiBand.GHZ_5
                    dims += Dimensao("Força do sinal Wi-Fi", "$rssi dBm", MetricClassifier.classificarRssiWifi(rssi, wifiBand))
                }
            ConnectionType.mobile ->
                input.mobile?.signalStrengthDbm?.let { dbm ->
                    // Sem tabela dedicada de sinal generico (RSSI cru) para movel — usa a mesma
                    // regua de Wi-Fi 5GHz como proxy conservador (nao ha tabela RSSI-generico
                    // documentada na skill /regras-diagnostico-rede para movel fora de RSRP/RSRQ/SINR).
                    dims += Dimensao("Força do sinal da operadora", "$dbm dBm", MetricClassifier.classificarRssiWifi(dbm, MetricClassifier.WifiBand.GHZ_5))
                }
            else -> Unit
        }

        // Auto-relato do usuário entra como evidência adicional (nunca substitui o dado medido
        // acima) — regra do produto: motor pode combinar métricas + contexto declarado, só a
        // IA é que não pode inventar causa sem evidência.
        val statusAutoRelato =
            when (melhoraDesligandoWifi) {
                0 -> MetricStatus.ruim // "Sim, melhora muito" -> problema tende a ser Wi-Fi
                1 -> MetricStatus.regular // "Sim, um pouco"
                2 -> MetricStatus.bom // "Não muda nada" -> problema tende a ser fora do Wi-Fi
                else -> null // "Ainda não testei" -> sem evidência adicional
            }
        if (statusAutoRelato != null) {
            dims += Dimensao(
                melhoraDesligandoWifi.rotuloRelatoMelhoraSemWifi(),
                melhoraDesligandoWifi.valorRelatoMelhoraSemWifi(),
                statusAutoRelato,
            )
        }

        return montarResultado(
            objetivo = ObjetivoDiagnostico.WIFI_VS_OPERADORA,
            dims = dims,
            mensagens = MensagensStatus(
                ok = "O sinal da conexão atual está dentro do esperado. O problema não parece estar entre o roteador e os dispositivos.",
                atencao = "O sinal está um pouco abaixo do ideal. Vale testar por cabo ou reposicionar o roteador antes de acionar a operadora.",
                critica = "Problema parece estar entre roteador e dispositivos, não na internet contratada.",
            ),
            acoes = { status ->
                if (status == DiagnosticStatus.ok) {
                    emptyList()
                } else {
                    listOf(
                        "Reposicione o roteador ou troque o canal Wi-Fi",
                        "Se o problema for só de um aparelho, teste esquecer e reconectar a rede nele",
                    )
                }
            },
        )
    }

    private fun Int?.rotuloRelatoMelhoraSemWifi(): String =
        if (this == null) "Comparação com a rede móvel" else "Ao usar a rede móvel"

    private fun Int?.valorRelatoMelhoraSemWifi(): String =
        when (this) {
            0 -> "melhora muito"
            1 -> "melhora um pouco"
            2 -> "não muda"
            else -> "não feita"
        }

    // ── Compartilhado ────────────────────────────────────────────────────────

    /**
     * Monta o [ResultadoDiagnosticoGuiado] a partir das [dims] já coletadas pelo
     * `avaliarXxx` do objetivo — pior faixa vence (mesmo princípio de
     * [GameReadinessClassifier]/[UsageProfileClassifier]). Sem nenhuma dimensão
     * disponível (teste não mediu nada relevante para este objetivo), devolve
     * status [DiagnosticStatus.inconclusive] e `dadosInsuficientes = true` — nunca
     * inventa evidência.
     */
    private fun montarResultado(
        objetivo: ObjetivoDiagnostico,
        dims: List<Dimensao>,
        mensagens: MensagensStatus,
        acoes: (DiagnosticStatus) -> List<String>,
    ): ResultadoDiagnosticoGuiado {
        val base = construirResultadoBase(dims, mensagens)
        return ResultadoDiagnosticoGuiado(
            objetivo = objetivo,
            status = base.status,
            mensagemMotor = base.mensagem,
            evidencias = base.evidencias,
            // Sem dados suficientes nunca sugere ação (nada pra agir em cima) — mesmo
            // comportamento de antes da extração de construirResultadoBase.
            acoes = if (base.dadosInsuficientes) emptyList() else acoes(base.status),
            dadosInsuficientes = base.dadosInsuficientes,
        )
    }
}

/**
 * Uma dimensão medida (ex.: "Latência", "42 ms", [MetricStatus.bom]) que entra na
 * disputa de "pior faixa vence" de [construirResultadoBase]. `internal` — usada por
 * [DiagnosticoGuiadoEngine] e por [ModoGamerEngine] (issue #1476), que reaproveita a
 * mesma infraestrutura de montagem de resultado em vez de duplicá-la.
 */
internal data class Dimensao(
    val label: String,
    val valorExibido: String,
    val status: MetricStatus,
)

/** As 3 mensagens de motor (`ok`/`atencao`/`critica`) que [construirResultadoBase]
 *  escolhe conforme o status agregado das [Dimensao]. `internal` pelo mesmo motivo
 *  de [Dimensao] — ver [ModoGamerEngine]. */
internal data class MensagensStatus(
    val ok: String,
    val atencao: String,
    val critica: String,
)

/** Resultado agregado (sem o campo específico de contexto — [ObjetivoDiagnostico] em
 *  [DiagnosticoGuiadoEngine], [CategoriaJogoModoGamer] em [ModoGamerEngine]) que
 *  [construirResultadoBase] devolve. Cada motor "embrulha" isto no seu próprio tipo de
 *  resultado público, acrescentando o campo de contexto e as ações. */
internal data class ResultadoBase(
    val status: DiagnosticStatus,
    val mensagem: String,
    val evidencias: List<EvidenciaDiagnostico>,
    val dadosInsuficientes: Boolean,
)

/**
 * As 3 dimensões priorizadas por "jogos com lag" — mesmas dos perfis competitivo/genérico do
 * Modo gamer ([ModoGamerEngine.avaliarFpsCompetitivo]/`avaliarOutro`). Extraída pela issue
 * #1667 (Task 2.0.19, épico #1647, critério "entrada guiada e ferramenta convergem no mesmo
 * engine") — antes desta extração, [DiagnosticoGuiadoEngine.avaliarJogosComLag] e as duas
 * funções do Modo gamer citadas reimplementavam a mesma leitura de
 * latência/jitter/perda com os mesmos rótulos e o mesmo [MetricClassifier]. `internal` pelo
 * mesmo motivo de [Dimensao]/[MensagensStatus] — consumida pelos dois motores deste pacote.
 */
internal fun dimsLatenciaJitterPerda(internet: InternetDiagnosticInput?): MutableList<Dimensao> {
    val dims = mutableListOf<Dimensao>()
    internet?.latencyMs?.let {
        dims += Dimensao("Tempo de resposta com a rede ocupada", "%.0f ms".format(it), MetricClassifier.classificarLatencia(it))
    }
    internet?.jitterMs?.let {
        dims += Dimensao("Variação do tempo de resposta", "%.0f ms".format(it), MetricClassifier.classificarJitter(it))
    }
    internet?.perdaPercentual?.let {
        dims += Dimensao("Falhas estimadas na conexão", "%.1f%%".format(it), MetricClassifier.classificarPerdaPacotes(it))
    }
    return dims
}

private fun Dimensao.severidade(): Int =
    when (status) {
        MetricStatus.excelente -> 0
        MetricStatus.bom -> 1
        MetricStatus.regular -> 2
        MetricStatus.ruim -> 3
        MetricStatus.critico -> 4
        MetricStatus.inconclusivo -> -1
    }

private fun MetricStatus.paraDiagnostico(): DiagnosticStatus =
    when (this) {
        MetricStatus.excelente, MetricStatus.bom -> DiagnosticStatus.ok
        MetricStatus.regular -> DiagnosticStatus.attention
        MetricStatus.ruim, MetricStatus.critico -> DiagnosticStatus.critical
        MetricStatus.inconclusivo -> DiagnosticStatus.inconclusive
    }

/**
 * Núcleo de "pior faixa vence" compartilhado por [DiagnosticoGuiadoEngine.montarResultado]
 * e por [ModoGamerEngine] (issue #1476/#550) — único ponto que decide o [DiagnosticStatus]
 * agregado e monta a lista de [EvidenciaDiagnostico] a partir das [dims] medidas. Sem
 * nenhuma dimensão disponível, devolve [DiagnosticStatus.inconclusive] com
 * `dadosInsuficientes = true`, nunca inventa evidência.
 */
internal fun construirResultadoBase(
    dims: List<Dimensao>,
    mensagens: MensagensStatus,
): ResultadoBase {
    if (dims.isEmpty()) {
        return ResultadoBase(
            status = DiagnosticStatus.inconclusive,
            mensagem = "Não há dados suficientes deste teste para avaliar esta situação. Refaça o teste de velocidade e tente novamente.",
            evidencias = emptyList(),
            dadosInsuficientes = true,
        )
    }
    val pior = dims.maxBy { it.severidade() }
    val status = pior.status.paraDiagnostico()
    val mensagem =
        when (status) {
            DiagnosticStatus.ok, DiagnosticStatus.info -> mensagens.ok
            DiagnosticStatus.attention -> mensagens.atencao
            DiagnosticStatus.critical -> mensagens.critica
            DiagnosticStatus.inconclusive -> mensagens.atencao
        }
    return ResultadoBase(
        status = status,
        mensagem = mensagem,
        evidencias = dims.map { EvidenciaDiagnostico(it.label, it.valorExibido, it.status) },
        dadosInsuficientes = false,
    )
}

/** Uma linha de evidência mostrada no container "Medido pelo motor SignallQ" —
 *  sempre rastreável a uma métrica real do [DiagnosticInput], nunca texto solto. */
data class EvidenciaDiagnostico(
    val label: String,
    val valorExibido: String,
    val status: MetricStatus,
)

/**
 * Resultado do diagnóstico guiado para um [objetivo] — [status] e [evidencias] são
 * 100% determinísticos ([DiagnosticoGuiadoEngine]). A explicação em prosa gerada
 * por IA (quando disponível) é responsabilidade da camada de apresentação
 * (`AnalisadorState` em `:app`), nunca deste motor.
 */
data class ResultadoDiagnosticoGuiado(
    val objetivo: ObjetivoDiagnostico,
    val status: DiagnosticStatus,
    val mensagemMotor: String,
    val evidencias: List<EvidenciaDiagnostico>,
    val acoes: List<String>,
    /** `true` quando o [DiagnosticInput] não trazia nenhuma métrica relevante para
     *  este objetivo — [status] fica [DiagnosticStatus.inconclusive] e [evidencias]
     *  fica vazio, nunca preenchido com valor inventado. */
    val dadosInsuficientes: Boolean,
)
