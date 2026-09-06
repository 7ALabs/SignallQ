package io.signallq.app.core.diagnostico

/**
 * Motor local determinístico do diagnóstico guiado por objetivo (Feature #550,
 * issue #1475). Recebe o [ObjetivoDiagnostico] escolhido, as respostas das
 * [PerguntaFechada] (por índice de opção, não texto) e o [DiagnosticInput] real do
 * teste — devolve um [ResultadoDiagnosticoGuiado] com status/evidências/ações.
 *
 * ## Roteiro reduzido a 1 pergunta por objetivo (2026-08)
 * [PerguntasDiagnosticoGuiado.perguntas] hoje devolve **1** [PerguntaFechada] por
 * objetivo (antes eram 2) — ver o kdoc daquele objeto para o racional completo.
 * Este motor não precisou mudar: nenhum `avaliarXxx` abaixo já lia
 * `respostas.getOrNull(1)` ou índice maior — [avaliarJogosComLag] e
 * [avaliarWifiVsOperadora] são os únicos que leem `respostas`, e ambos só usam
 * `getOrNull(0)`, que continua sendo a pergunta que sobrou.
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
        input: DiagnosticInput?,
    ): ResultadoDiagnosticoGuiado {
        val avaliador =
            when (objetivo) {
                ObjetivoDiagnostico.INSTABILIDADE_QUEDAS -> ::avaliarInstabilidadeQuedas
                ObjetivoDiagnostico.LENTIDAO_GERAL -> ::avaliarLentidaoGeral
                ObjetivoDiagnostico.PROBLEMAS_VIDEO_JOGOS -> ::avaliarProblemasVideoJogos
                ObjetivoDiagnostico.OUTRO_PROBLEMA -> ::avaliarOutroProblema
            }
        return avaliador(input)
    }

    // ── Instabilidade ou Quedas (antigo: Internet cai ou oscila + Wi-Fi vs Operadora) ──
    private fun avaliarInstabilidadeQuedas(
        input: DiagnosticInput?,
    ): ResultadoDiagnosticoGuiado {
        val internet = input?.internet
        val emRedeMovel = input?.connectionType == ConnectionType.mobile
        val dims = mutableListOf<Dimensao>()

        // 1. Sinal ativo
        when (input?.connectionType) {
            ConnectionType.wifi ->
                input.wifi?.rssiDbm?.let { rssi ->
                    val banda = input.wifi.banda()
                    val wifiBand = if (banda == BandaWifi.ghz24) MetricClassifier.WifiBand.GHZ_2_4 else MetricClassifier.WifiBand.GHZ_5
                    dims += Dimensao("Força do sinal Wi-Fi", "$rssi dBm", MetricClassifier.classificarRssiWifi(rssi, wifiBand))
                }
            ConnectionType.mobile ->
                input.mobile?.signalStrengthDbm?.let { dbm ->
                    dims += Dimensao("Força do sinal da operadora", "$dbm dBm", MetricClassifier.classificarRssiWifi(dbm, MetricClassifier.WifiBand.GHZ_5))
                }
            else -> Unit
        }

        // 2. Falhas
        internet?.perdaPercentual?.let {
            dims += Dimensao("Falhas estimadas na conexão", "%.1f%%".format(it), MetricClassifier.classificarPerdaPacotes(it))
        }
        internet?.jitterMs?.let {
            dims += Dimensao("Variação da conexão", "%.0f ms".format(it), MetricClassifier.classificarJitter(it))
        }

        return montarResultado(
            objetivo = ObjetivoDiagnostico.INSTABILIDADE_QUEDAS,
            dims = dims,
            mensagens = MensagensStatus(
                ok = "Não encontramos falhas na conexão nem oscilação fora do esperado nos últimos testes.",
                atencao = "Sinais de instabilidade na sua rede: falhas na conexão ou oscilação um pouco acima do ideal.",
                critica = "Sinal de instabilidade real na sua rede: falhas na conexão, sinal fraco ou oscilação acima do esperado.",
            ),
            acoes = { status ->
                if (status == DiagnosticStatus.ok) {
                    emptyList()
                } else if (emRedeMovel) {
                    listOf(
                        "Teste em outro local com melhor cobertura da operadora",
                        "Se possível, compare o resultado pelo Wi-Fi",
                    )
                } else {
                    listOf(
                        "Reposicione o roteador longe de paredes e eletrônicos",
                        "Teste por cabo para isolar se o problema está no Wi-Fi ou na operadora",
                    )
                }
            },
        )
    }

    // ── Lentidão Geral (antigo: Velocidade não chega + Sites demoram) ──
    private fun avaliarLentidaoGeral(
        input: DiagnosticInput?,
    ): ResultadoDiagnosticoGuiado {
        val emRedeMovel = input?.connectionType == ConnectionType.mobile
        val internet = input?.internet
        val dns = input?.dns
        val dims = mutableListOf<Dimensao>()

        val download = internet?.downloadMbps
        val contratado = input?.velocidadeContratadaMbps

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

        val dnsLatencia = dns?.currentDnsLatencyMs
        if (dnsLatencia != null) {
            dims += Dimensao("Tempo para localizar sites", "$dnsLatencia ms", MetricClassifier.classificarLatenciaDns(dnsLatencia))
        } else {
            internet?.latencyMs?.let {
                dims += Dimensao("Tempo de resposta", "%.0f ms".format(it), MetricClassifier.classificarLatencia(it))
            }
        }

        return montarResultado(
            objetivo = ObjetivoDiagnostico.LENTIDAO_GERAL,
            dims = dims,
            mensagens = MensagensStatus(
                ok = "Velocidade e tempo de resposta estão dentro do esperado. A navegação não deve apresentar lentidão.",
                atencao = "O download ou o tempo de resolução de sites estão abaixo do ideal. Pode haver lentidão perceptível.",
                critica = "Resultado bem abaixo do plano ou com latência muito alta, o que costuma atrasar o carregamento de sites.",
            ),
            acoes = { status ->
                if (status == DiagnosticStatus.ok) {
                    emptyList()
                } else if (emRedeMovel) {
                    listOf(
                        "Repita o teste em outro local, se possível com mais sinal da operadora",
                        "Se o resultado persistir, contate a operadora com este resultado",
                    )
                } else {
                    listOf(
                        "Troque o DNS da rede nas configurações, caso sites específicos demorem a abrir",
                        "Repita o teste conectado por cabo de rede",
                    )
                }
            },
        )
    }

    // ── Problemas com Vídeo ou Jogos (antigo: Jogos + Vídeos + Chamadas) ──
    private fun avaliarProblemasVideoJogos(
        input: DiagnosticInput?,
    ): ResultadoDiagnosticoGuiado {
        val internet = input?.internet
        // Assume Wi-Fi usage if connected to Wi-Fi. (No more manual "Cabo" question).
        val noWifi = input?.connectionType == ConnectionType.wifi
        val dims = dimsLatenciaJitterPerda(internet)

        internet?.bufferbloatMs?.let {
            dims += Dimensao("Lentidão com a rede ocupada", "%.0f ms".format(it), MetricClassifier.classificarBufferbloat(it))
        }
        internet?.uploadMbps?.let {
            dims += Dimensao("Upload", "%.1f Mbps".format(it), MetricClassifier.classificarUpload(it))
        }

        if (noWifi) {
            input?.wifi?.rssiDbm?.let { rssi ->
                val banda = input.wifi.banda()
                val wifiBand = if (banda == BandaWifi.ghz24) MetricClassifier.WifiBand.GHZ_2_4 else MetricClassifier.WifiBand.GHZ_5
                dims += Dimensao("Força do sinal Wi-Fi", "$rssi dBm", MetricClassifier.classificarRssiWifi(rssi, wifiBand))
            }
        }

        return montarResultado(
            objetivo = ObjetivoDiagnostico.PROBLEMAS_VIDEO_JOGOS,
            dims = dims,
            mensagens = MensagensStatus(
                ok = "Tempo de resposta, falhas e velocidade dentro do esperado. A rede aguenta streaming e jogos.",
                atencao = "O atraso sob carga ou o upload estão no limite. O vídeo pode perder qualidade ou haver lag em disputas.",
                critica = "O tempo de resposta sob carga e a variação prejudicam partidas e chamadas. Faixa crítica para tempo real.",
            ),
            acoes = { status ->
                if (status == DiagnosticStatus.ok) {
                    emptyList()
                } else if (noWifi) {
                    listOf(
                        "Pause downloads em segundo plano durante o uso",
                        "Jogue ou faça chamadas importantes com cabo de rede em vez de Wi-Fi",
                        "Ative priorização de dispositivos no roteador, se disponível",
                    )
                } else {
                    listOf(
                        "Pause downloads em segundo plano",
                        "Teste novamente fora do horário de pico",
                    )
                }
            },
        )
    }

    // ── Outro problema (texto livre) ──
    private fun avaliarOutroProblema(
        input: DiagnosticInput?,
    ): ResultadoDiagnosticoGuiado {
        val internet = input?.internet
        val dims = dimsLatenciaJitterPerda(internet)
        internet?.downloadMbps?.let {
            dims += Dimensao("Download", "%.1f Mbps".format(it), MetricClassifier.classificarDownload(it))
        }
        internet?.uploadMbps?.let {
            dims += Dimensao("Upload", "%.1f Mbps".format(it), MetricClassifier.classificarUpload(it))
        }
        return montarResultado(
            objetivo = ObjetivoDiagnostico.OUTRO_PROBLEMA,
            dims = dims,
            mensagens = MensagensStatus(
                ok = "As métricas gerais da sua conexão estão dentro do esperado.",
                atencao = "Encontramos sinais de que sua conexão pode estar com dificuldades em algum momento.",
                critica = "Encontramos sinais claros de que sua conexão está com problemas.",
            ),
            acoes = { status ->
                if (status == DiagnosticStatus.ok) {
                    emptyList()
                } else {
                    listOf(
                        "Refaça o teste em um horário diferente para comparar",
                        "Teste por cabo para isolar se é Wi-Fi ou operadora",
                    )
                }
            },
        )
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
