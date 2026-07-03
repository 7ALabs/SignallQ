package io.signallq.app.feature.diagnostico

private const val CAT = "decisao"

/**
 * Um achado candidato avaliado pelo [FindingEngine] antes do desempate.
 *
 * Encapsula o [DiagnosticResult] da regra junto da confiança que a própria regra
 * declara ter no seu diagnóstico (0.0–1.0) — propriedade da REGRA (o quão direta é
 * a evidência que ela usa), não do dado bruto. Duas regras podem observar o mesmo
 * `DiagnosticStatus.critical` e ainda assim ter confiança diferente: GW-01
 * correlaciona duas fontes independentes (RTT gateway + latência externa),
 * enquanto DECISAO-DNS-01 usa uma fonte só.
 *
 * [ativo] indica se a regra está livre para competir pelo posto de achado
 * principal/secundário ([ativo] = true) ou se sua condição de gatilho bateu mas
 * ela foi suprimida por uma regra de evidência mais forte para a MESMA causa raiz
 * ([ativo] = false → vira hipótese descartada, ver [FindingEngine.analisar]).
 */
private data class Achado(
    val resultado: DiagnosticResult,
    val confianca: Double,
    val ativo: Boolean,
    /** Motivo da supressão quando [ativo] = false. Nulo quando ativo. */
    val motivoDescarte: String? = null,
)

/**
 * Motor de achados (findings) do diagnóstico local — sucessor do
 * [DiagnosticDecisionEngine].
 *
 * ## Por que existe
 * O engine antigo era uma cascata de `if/else`: a primeira regra que casasse
 * vencia e todo o resto era descartado silenciosamente, sem critério de desempate
 * e sem reportar mais de um problema simultâneo (ex.: fibra crítica + DNS ruim ao
 * mesmo tempo — o usuário só ficava sabendo da fibra).
 *
 * ## O que muda aqui
 * Todas as regras são avaliadas (nenhuma para na primeira que casa). Cada regra
 * cujo gatilho bate vira um [Achado] com severidade (derivada do
 * [DiagnosticStatus]), confiança (declarada pela regra) e um flag [Achado.ativo]:
 *  - Regras que representam causas INDEPENDENTES (podem ser verdadeiras ao mesmo
 *    tempo, ex.: fibra crítica + DNS lento) ficam sempre ativas e competem pelo
 *    posto de achado principal via score — a vencedora é o principal, as demais
 *    viram achados secundários.
 *  - Regras que representam a MESMA causa raiz de uma regra com evidência mais
 *    forte (os guards `&& !fibraCritica`, `&& !internetRuim` etc. que existiam no
 *    engine antigo) ficam com `ativo = false` quando a regra mais forte também
 *    bateu — viram hipóteses descartadas, com o motivo do descarte, em vez de
 *    desaparecer silenciosamente.
 *
 * ## Fórmula de desempate (severidade × confiança)
 * ```
 * score = severidade(status) * confianca(regra)
 *
 * severidade(critical)      = 4
 * severidade(attention)     = 2
 * severidade(info)          = 1
 * severidade(inconclusive)  = 1
 * severidade(ok)            = 0
 *
 * confianca(regra) ∈ [0.0, 1.0], declarada por regra (ver comentário de cada bloco)
 * ```
 * O achado ATIVO com maior `score` vence e vira o principal. Em empate exato de
 * score, mantém-se a ordem de avaliação das regras abaixo (regras de correlação
 * direta — GW-01/02, fibra crítica — são avaliadas antes das regras de sintoma
 * isolado, e são preferidas em empate porque explicam a causa, não só o sintoma).
 * `DiagnosticStatus.ok` nunca vence em desempate contra qualquer achado real
 * (score sempre 0) e só é o principal quando NENHUMA regra ativa bateu.
 *
 * ## Regras preservadas do DiagnosticDecisionEngine
 * Nenhuma regra de correlação foi removida — DECISAO-GW-01/02 (gateway vs.
 * operadora), DECISAO-00/00b (fibra crítica/atenção), DECISAO-01 (interferência
 * Wi-Fi), DECISAO-DNS-01/01b, DECISAO-HIST-01, DECISAO-WIFI-CANAL, DECISAO-02/02b,
 * DECISAO-04/04b-WIFI e DECISAO-INC continuam todas presentes com a mesma
 * evidência/condição de gatilho do engine antigo. A diferença é que a condição de
 * gatilho agora só decide SE a regra vira achado — quem vira principal é sempre o
 * score, e quem antes era "descartado por ordem" agora é reportado como hipótese
 * descartada (quando é a mesma causa raiz de uma regra mais forte) ou como achado
 * secundário (quando é uma causa independente).
 */
object FindingEngine {

    fun analisar(
        internetResultados: List<DiagnosticResult>,
        wifiQuality: WifiQualityResult,
        fibraResultados: List<DiagnosticResult> = emptyList(),
        /** RTT TCP para o gateway local. Usado pelas regras GW-01 e GW-02.
         *  Null = não disponível (emulador, Doze, gateway sem TCP). */
        rttGatewayMs: Int? = null,
        /** Latência de internet (ping externo) para correlação com RTT gateway. */
        latenciaInternetMs: Double? = null,
    ): FindingResult {
        val dnsCritico = internetResultados.any { it.categoria == "dns" && it.status == DiagnosticStatus.critical }
        val dnsAtencao = internetResultados.any { it.categoria == "dns" && it.status == DiagnosticStatus.attention }
        val histCritico = internetResultados.any { it.categoria == "historico" && it.status == DiagnosticStatus.critical }
        val histAtencao = internetResultados.any { it.categoria == "historico" && it.status == DiagnosticStatus.attention }
        val wifiCanalAtencao = internetResultados.any { it.categoria == "wifi-canal" && it.status == DiagnosticStatus.attention }
        val historicoIds = internetResultados.filter { it.categoria == "historico" }.map { it.id }
        val wifiCanalIds = internetResultados.filter { it.categoria == "wifi-canal" }.map { it.id }

        val internetCritico = internetResultados.any { it.status == DiagnosticStatus.critical }
        val internetAtencao = internetResultados.any { it.status == DiagnosticStatus.attention }
        val internetInconclusivo = internetResultados.any { it.status == DiagnosticStatus.inconclusive }
        val internetRuim = internetCritico || internetAtencao
        val wifiRuim = !wifiQuality.confiavelParaTeste
        val wifiCritico = wifiQuality.resultados.any { it.status == DiagnosticStatus.critical }
        val fibraDisponivel = fibraResultados.isNotEmpty() &&
            fibraResultados.any { it.status != DiagnosticStatus.inconclusive }
        val fibraCritica = fibraDisponivel && fibraResultados.any { it.status == DiagnosticStatus.critical }
        val fibraAtencao = fibraDisponivel && fibraResultados.any { it.status == DiagnosticStatus.attention }

        val criticoNaoDns =
            internetResultados.any {
                it.status == DiagnosticStatus.critical &&
                    it.categoria != "dns" &&
                    it.categoria != "historico" &&
                    it.categoria != "wifi-canal"
            }

        val candidatos = mutableListOf<Achado>()

        // DECISAO-GW-01: gateway rápido + internet lenta → problema na operadora.
        // Confiança alta (0.9): correlaciona duas fontes independentes (RTT local
        // TCP vs. latência de ping externo), não é sintoma isolado. Sempre ativa
        // (causa independente das demais).
        if (rttGatewayMs != null && rttGatewayMs < 10 &&
            latenciaInternetMs != null && latenciaInternetMs > 200.0 &&
            !wifiRuim
        ) {
            candidatos += Achado(
                DiagnosticResult(
                    id = "DECISAO-GW-01",
                    titulo = "Problema na Operadora",
                    status = DiagnosticStatus.critical,
                    evidencia = "rttGatewayMs=$rttGatewayMs latenciaInternetMs=$latenciaInternetMs",
                    mensagemUsuario = "Seu roteador está funcionando bem — a lentidão vem da sua operadora.",
                    recomendacao = "Entre em contato com sua operadora e informe que a conexão está lenta fora de casa. Se possível, peça abertura de chamado com o resultado deste diagnóstico.",
                    categoria = CAT,
                    podeConcluir = true,
                ),
                confianca = 0.9,
                ativo = true,
            )
        }

        // DECISAO-GW-02: gateway lento → roteador ou Wi-Fi é o problema.
        // Confiança média-alta (0.75): fonte única (RTT gateway), mas é medição
        // direta do equipamento suspeito. Sempre ativa.
        if (rttGatewayMs != null && rttGatewayMs > 50) {
            candidatos += Achado(
                DiagnosticResult(
                    id = "DECISAO-GW-02",
                    titulo = "Roteador Respondendo Lentamente",
                    status = DiagnosticStatus.attention,
                    evidencia = "rttGatewayMs=$rttGatewayMs",
                    mensagemUsuario = "Seu roteador está respondendo lentamente — isso pode estar causando a lentidão.",
                    recomendacao = "Reinicie o roteador: desligue da tomada, aguarde 30 segundos e ligue novamente. Se a lentidão persistir, tente se aproximar do roteador.",
                    categoria = CAT,
                    podeConcluir = false,
                ),
                confianca = 0.75,
                ativo = true,
            )
        }

        // DECISAO-00 / DECISAO-00b: fibra crítica/atenção — mesma causa raiz
        // (estado físico da fibra), só severidade diferente. Ambas nunca ativas
        // ao mesmo tempo (fibraCritica e fibraAtencao são mutuamente exclusivos
        // pela definição acima), então não há disputa entre elas.
        if (fibraCritica && internetRuim) {
            candidatos += Achado(
                DiagnosticResult(
                    id = "DECISAO-00",
                    titulo = "Problema na Fibra",
                    status = DiagnosticStatus.critical,
                    evidencia = "fibraProblemas=${fibraResultados.filter { it.status == DiagnosticStatus.critical }.map { it.id }}",
                    mensagemUsuario = "Foram detectados problemas na fibra óptica que podem estar causando instabilidade na internet.",
                    recomendacao = "Verifique o estado da ONT. Se o cabo de fibra ou o laser estiver com problema, contate o provedor.",
                    categoria = CAT,
                    podeConcluir = true,
                ),
                confianca = 0.9,
                ativo = true,
            )
        }
        if (fibraAtencao && !internetCritico) {
            candidatos += Achado(
                DiagnosticResult(
                    id = "DECISAO-00b",
                    titulo = "Atenção na Qualidade da Fibra",
                    status = DiagnosticStatus.attention,
                    evidencia = "fibraAlertas=${fibraResultados.filter { it.status == DiagnosticStatus.attention }.map { it.id }}",
                    mensagemUsuario = "Alguns indicadores da fibra merecem atenção. Monitore a estabilidade da conexão.",
                    recomendacao = "Verifique a ventilação da ONT e o estado do cabo de fibra. Informe o provedor se houver quedas recorrentes.",
                    categoria = CAT,
                ),
                confianca = 0.6,
                ativo = true,
            )
        }

        // DECISAO-01: internet com problema OU inconclusiva + WiFi ruim.
        // Cobre dois casos:
        //   a) internetRuim && wifiRuim → problemas reais + WiFi fraco
        //   b) internetInconclusivo && wifiRuim → InternetDiagnosticEngine marcou tudo
        //      como inconclusive por causa do WiFi fraco (IN-NORMAL-08).
        // Quando o Wi-Fi é ruim, DECISAO-02/02b (que dependem de `!wifiRuim`) tornam-se
        // hipóteses suprimidas em favor desta regra — ver os blocos DECISAO-02/02b
        // abaixo, que verificam `ativo = !wifiRuim`.
        if ((internetRuim || internetInconclusivo) && wifiRuim) {
            candidatos += Achado(
                DiagnosticResult(
                    id = "DECISAO-01",
                    titulo = "Possível Interferência de Wi-Fi",
                    status = DiagnosticStatus.attention,
                    evidencia = "wifiConfiavel=false internetProblemas=${internetResultados.filter { it.status != DiagnosticStatus.ok }.map { it.id }}",
                    mensagemUsuario = "Foram detectados problemas na conexão, mas o sinal Wi-Fi fraco pode ser a causa. O diagnóstico de internet pode não ser preciso.",
                    recomendacao = "Aproxime-se do roteador, reconecte ao Wi-Fi e refaça o teste.",
                    categoria = CAT,
                    podeConcluir = false,
                ),
                confianca = 0.65,
                ativo = true,
            )
        }

        // DECISAO-DNS-01: DNS crítico. Mesma causa raiz de DECISAO-00 (fibra crítica)
        // quando ambas batem — fibra crítica é evidência mais forte e mais próxima
        // do hardware, então suprime DNS quando concorrem (criticoNaoDns preservado
        // do engine antigo: se há outro crítico não-DNS relevante, DNS também cede).
        val dnsSuprimidaPorFibraOuOutroCritico = dnsCritico && (fibraCritica || criticoNaoDns)
        if (dnsCritico) {
            candidatos += Achado(
                DiagnosticResult(
                    id = "DECISAO-DNS-01",
                    titulo = "Problema no DNS",
                    status = DiagnosticStatus.critical,
                    evidencia = "dnsCritico=true",
                    mensagemUsuario = "Foi detectado um problema critico no DNS em uso. Isso pode causar lentidao para abrir sites e falhas em apps.",
                    recomendacao = "Troque o DNS para uma opcao mais rapida (ex.: Cloudflare ou Google DNS) e refaca o teste.",
                    categoria = CAT,
                    podeConcluir = true,
                ),
                confianca = 0.85,
                ativo = !dnsSuprimidaPorFibraOuOutroCritico,
                motivoDescarte = if (dnsSuprimidaPorFibraOuOutroCritico) {
                    "suprimida por evidência mais forte: fibra crítica ou outro problema crítico não-DNS presente"
                } else {
                    null
                },
            )
        }

        // DECISAO-DNS-01b: DNS com atenção. Causa independente de fibra/internet —
        // sempre ativa, compete por score. Diferente do engine antigo (que escondia
        // esse achado quando havia internet/fibra crítica), agora aparece como
        // achado secundário nesses casos em vez de desaparecer.
        if (dnsAtencao) {
            candidatos += Achado(
                DiagnosticResult(
                    id = "DECISAO-DNS-01b",
                    titulo = "Atenção ao DNS",
                    status = DiagnosticStatus.attention,
                    evidencia = "dnsAtencao=true",
                    mensagemUsuario = "O DNS atual esta lento e pode impactar a experiencia, mesmo com boa velocidade.",
                    recomendacao = "Compare com outros DNS e considere trocar para o melhor no comparativo.",
                    categoria = CAT,
                    podeConcluir = false,
                ),
                confianca = 0.6,
                ativo = true,
            )
        }

        // DECISAO-HIST-01: degradação histórica relevante (não conclui causa raiz).
        // Causa independente (tendência ao longo do tempo, não sintoma do momento) —
        // sempre ativa, compete por score. Antes era escondida quando havia
        // internet/fibra crítica; agora aparece como achado secundário.
        if (histCritico || histAtencao) {
            candidatos += Achado(
                DiagnosticResult(
                    id = "DECISAO-HIST-01",
                    titulo = "Degradação Recente Detectada",
                    status = if (histCritico) DiagnosticStatus.critical else DiagnosticStatus.attention,
                    evidencia = "historico=$historicoIds",
                    mensagemUsuario = "O histórico sugere degradação recente da conexão. Vale investigar horários e condições em que piora.",
                    recomendacao = "Refaça testes em horarios diferentes (inclusive via cabo). Se confirmar, leve o historico ao provedor.",
                    categoria = CAT,
                    podeConcluir = false,
                ),
                confianca = 0.5,
                ativo = true,
            )
        }

        // DECISAO-WIFI-CANAL: canal congestionado (não conclui internet/ISP). Causa
        // independente (observação de scan de vizinhança) — sempre ativa, compete
        // por score. Antes era escondida quando havia internet/fibra crítica; agora
        // aparece como achado secundário.
        if (wifiCanalAtencao) {
            candidatos += Achado(
                DiagnosticResult(
                    id = "DECISAO-WIFI-CANAL",
                    titulo = "Possível Congestionamento de Wi-Fi",
                    status = DiagnosticStatus.attention,
                    evidencia = "wifiCanal=$wifiCanalIds",
                    mensagemUsuario = "O canal Wi-Fi parece congestionado e pode explicar instabilidade ou queda de desempenho no Wi-Fi.",
                    recomendacao = "Troque o canal Wi-Fi para um canal menos ocupado e refaça o teste.",
                    categoria = CAT,
                    podeConcluir = false,
                ),
                confianca = 0.55,
                ativo = true,
            )
        }

        // DECISAO-02 / DECISAO-02b: internet com problema + WiFi ok → culpa é
        // internet/ISP. Guard do engine antigo (`!wifiRuim`) preservado: quando o
        // Wi-Fi é ruim, a causa vira hipótese suprimida em favor de DECISAO-01
        // (Wi-Fi pode estar mascarando/causando o sintoma de internet).
        if (internetCritico) {
            candidatos += Achado(
                DiagnosticResult(
                    id = "DECISAO-02",
                    titulo = "Problema na Internet",
                    status = DiagnosticStatus.critical,
                    evidencia = "problemas=${internetResultados.filter { it.status == DiagnosticStatus.critical }.map { it.id }}",
                    mensagemUsuario = "O Wi-Fi está bom, mas há problemas na conexão com a internet. O problema pode estar no roteador ou no provedor.",
                    recomendacao = "Reinicie o roteador. Se o problema persistir, contate o suporte do seu provedor de internet.",
                    categoria = CAT,
                    podeConcluir = true,
                ),
                confianca = 0.8,
                ativo = !wifiRuim,
                motivoDescarte = if (wifiRuim) "suprimida por Wi-Fi não confiável durante o teste (ver DECISAO-01)" else null,
            )
        }
        if (internetAtencao) {
            candidatos += Achado(
                DiagnosticResult(
                    id = "DECISAO-02b",
                    titulo = "Atenção na Qualidade da Internet",
                    status = DiagnosticStatus.attention,
                    evidencia = "alertas=${internetResultados.filter { it.status == DiagnosticStatus.attention }.map { it.id }}",
                    mensagemUsuario = "O Wi-Fi está bom, mas alguns indicadores de internet merecem atenção.",
                    recomendacao = "Monitore a conexão. Se o problema persistir, reinicie o roteador ou entre em contato com o provedor.",
                    categoria = CAT,
                    podeConcluir = false,
                ),
                confianca = 0.6,
                ativo = !wifiRuim,
                motivoDescarte = if (wifiRuim) "suprimida por Wi-Fi não confiável durante o teste (ver DECISAO-01)" else null,
            )
        }

        // DECISAO-04-WIFI: WiFi crítico mas internet ok. Guard do engine antigo
        // (`!internetRuim`) preservado: internet com problema é evidência mais forte.
        if (wifiCritico) {
            candidatos += Achado(
                DiagnosticResult(
                    id = "DECISAO-04-WIFI",
                    titulo = "Atenção ao Sinal Wi-Fi",
                    status = DiagnosticStatus.attention,
                    evidencia = "wifiStatus=" + wifiQuality.resultados.map { it.id + ":" + it.status },
                    mensagemUsuario = "A internet está funcionando bem, mas o sinal Wi-Fi merece atenção para evitar problemas futuros.",
                    recomendacao = "Aproxime-se do roteador ou reduza obstáculos entre o dispositivo e o roteador.",
                    categoria = CAT,
                ),
                confianca = 0.7,
                ativo = !internetRuim,
                motivoDescarte = if (internetRuim) "suprimida por problema de internet mais relevante já identificado" else null,
            )
        }

        // DECISAO-04b-WIFI: Wi-Fi com atenção não-crítica (link speed baixo, muitos
        // dispositivos). Não cobre sinal fraco — esse caso vai para DECISAO-01 via
        // wifiRuim. Guards do engine antigo (`!wifiCritico && !internetRuim && !wifiRuim`)
        // preservados.
        val wifiTemAtencao = wifiQuality.resultados.any { it.status == DiagnosticStatus.attention }
        if (wifiTemAtencao) {
            val alertas = wifiQuality.resultados.filter { it.status == DiagnosticStatus.attention }.map { it.id }
            val suprimida = wifiCritico || internetRuim || wifiRuim
            candidatos += Achado(
                DiagnosticResult(
                    id = "DECISAO-04b-WIFI",
                    titulo = "Atenção ao Wi-Fi",
                    status = DiagnosticStatus.attention,
                    evidencia = "wifiAlertas=$alertas",
                    mensagemUsuario = "A internet está funcionando, mas há indicadores do Wi-Fi que merecem atenção e podem afetar a experiência.",
                    recomendacao = "Verifique os itens sinalizados para melhorar a qualidade da conexão Wi-Fi.",
                    categoria = CAT,
                ),
                confianca = 0.5,
                ativo = !suprimida,
                motivoDescarte = if (suprimida) "suprimida por Wi-Fi crítico, internet ruim ou Wi-Fi não confiável" else null,
            )
        }

        // DECISAO-INC: inconclusivo (sem dados suficientes). Confiança propositalmente
        // baixa (0.3) — é o "não sei", nunca deve vencer um achado real em desempate.
        // Guard do engine antigo (`!internetRuim`) preservado: se já há problema real
        // detectado, não é "inconclusivo".
        if (internetInconclusivo) {
            candidatos += Achado(
                DiagnosticResult(
                    id = "DECISAO-INC",
                    titulo = "Diagnóstico Inconclusivo",
                    status = DiagnosticStatus.inconclusive,
                    evidencia = null,
                    mensagemUsuario = "Não há dados suficientes para um diagnóstico preciso.",
                    recomendacao = "Execute um teste de velocidade completo e refaça o diagnóstico.",
                    categoria = CAT,
                ),
                confianca = 0.3,
                ativo = !internetRuim,
                motivoDescarte = if (internetRuim) "suprimida: há problema real detectado, diagnóstico não é inconclusivo" else null,
            )
        }

        val achadoTudoOk = DiagnosticResult(
            id = "DECISAO-04",
            titulo = "Conexão Sem Problemas",
            status = DiagnosticStatus.ok,
            evidencia = null,
            mensagemUsuario = "Todos os indicadores analisados estão dentro do esperado. Sua internet está funcionando bem.",
            recomendacao = null,
            categoria = CAT,
            podeConcluir = true,
        )

        val ativos = candidatos.filter { it.ativo }
        val descartados = candidatos.filter { !it.ativo }

        if (ativos.isEmpty()) {
            return FindingResult(
                principal = achadoTudoOk,
                secundarios = emptyList(),
                hipotesesDescartadas = descartados.map { it.comMotivoNaEvidencia() },
                dadosAusentes = dadosAusentes(rttGatewayMs, fibraDisponivel),
            )
        }

        // Desempate: maior score (severidade × confiança) vence entre os achados
        // ATIVOS. Em empate exato, mantém a ordem de avaliação acima (regras de
        // correlação direta primeiro).
        val ordenados = ativos.sortedByDescending { scoreDesempate(it) }
        val principal = ordenados.first()
        val secundarios = ordenados.drop(1).filter { it.resultado.status != DiagnosticStatus.ok }

        return FindingResult(
            principal = principal.resultado,
            secundarios = secundarios.map { it.resultado },
            hipotesesDescartadas = descartados.map { it.comMotivoNaEvidencia() },
            dadosAusentes = dadosAusentes(rttGatewayMs, fibraDisponivel),
        )
    }

    /** Anexa [Achado.motivoDescarte] à evidência do resultado, para que o motivo da
     *  supressão não se perca ao sair do escopo interno do [FindingEngine]. */
    private fun Achado.comMotivoNaEvidencia(): DiagnosticResult {
        val motivo = motivoDescarte ?: return resultado
        val evidenciaComMotivo = listOfNotNull(resultado.evidencia, "descarte: $motivo").joinToString(" | ")
        return resultado.copy(evidencia = evidenciaComMotivo)
    }

    /**
     * Score de desempate entre achados concorrentes: severidade × confiança.
     * Ver fórmula completa e justificativa no kdoc de [FindingEngine].
     */
    private fun scoreDesempate(achado: Achado): Double =
        severidade(achado.resultado.status) * achado.confianca

    private fun severidade(status: DiagnosticStatus): Int = when (status) {
        DiagnosticStatus.critical -> 4
        DiagnosticStatus.attention -> 2
        DiagnosticStatus.info -> 1
        DiagnosticStatus.inconclusive -> 1
        DiagnosticStatus.ok -> 0
    }

    private fun dadosAusentes(rttGatewayMs: Int?, fibraDisponivel: Boolean): List<String> {
        val ausentes = mutableListOf<String>()
        if (rttGatewayMs == null) ausentes += "rttGateway"
        if (!fibraDisponivel) ausentes += "fibra"
        return ausentes
    }
}

/**
 * Resultado do [FindingEngine]: um achado principal (o que o desempate elegeu
 * entre os achados ativos), achados secundários (demais causas independentes
 * detectadas simultaneamente — antes silenciosamente descartadas pela cascata
 * if/else), hipóteses descartadas (regras cujo gatilho bateu mas foram suprimidas
 * por evidência mais forte da MESMA causa raiz — reportadas com motivo, não
 * escondidas) e dados que faltaram para uma conclusão mais precisa.
 */
data class FindingResult(
    val principal: DiagnosticResult,
    val secundarios: List<DiagnosticResult> = emptyList(),
    val hipotesesDescartadas: List<DiagnosticResult> = emptyList(),
    val dadosAusentes: List<String> = emptyList(),
)
