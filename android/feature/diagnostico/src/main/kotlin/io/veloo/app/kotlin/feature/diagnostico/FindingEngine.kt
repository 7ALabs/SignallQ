package io.signallq.app.feature.diagnostico

import io.signallq.app.core.network.contracts.localdevice.DeviceType
import io.signallq.app.core.network.contracts.localdevice.LocalDeviceSectionStatus
import io.signallq.app.core.network.contracts.localdevice.SafeLocalDeviceContext
import io.signallq.app.core.network.contracts.localdevice.SupportLevel

private const val CAT = "decisao"

// Mesmo limiar usado por RecommendationEngine.recomendarRoteadorLimitado (">10")
// para "muitos dispositivos" — mantido consistente entre os dois motores.
private const val MUITOS_CLIENTES_THRESHOLD = 10

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
        /** Tipo de conexão ativa no momento do teste. Usado por DECISAO-02/02b para
         *  não falar de "Wi-Fi"/"roteador" quando a conexão é 100% rede móvel. */
        connectionType: ConnectionType = ConnectionType.wifi,
        /** Resumo seguro (allowlisted) do equipamento de rede local (ONT/roteador),
         *  quando disponível — GH#542, epic #547. Null em fluxos sem equipamento
         *  detectado/conectado; o motor continua funcionando normalmente sem ele.
         *  Usado apenas para CORRELACIONAR com os achados acima — nunca decide
         *  sozinho sem evidência de speedtest/Wi-Fi (ver regras LOCAL-EQUIP-*). */
        localDevice: SafeLocalDeviceContext? = null,
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
                    categoriaOrigem = "isp",
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
                    categoriaOrigem = "roteador",
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
                    categoriaOrigem = "fibra",
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
                    categoriaOrigem = "fibra",
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
                    categoriaOrigem = "wifi",
                ),
                confianca = 0.65,
                ativo = true,
            )
        }

        // ---------------------------------------------------------------------
        // Regras LOCAL-EQUIP-*: correlação com o equipamento de rede local
        // (ONT Nokia GPON / roteador TP-Link), quando disponível — GH#542,
        // epic #547. Todas nulas/inativas quando [localDevice] é null: o motor
        // continua funcionando normalmente sem o snapshot.
        //
        // Regras de produto aplicadas aqui:
        //  - Nunca inventa dado ausente: só correlaciona com o que o
        //    [SafeLocalDeviceContext] efetivamente reportou (OK/ATENCAO), nunca
        //    assume valor para INDISPONIVEL/NAO_SUPORTADO.
        //  - Roteador (TP-Link) NUNCA gera recomendação baseada em fibra — as
        //    regras abaixo condicionadas a DeviceType.ROUTER nunca mencionam
        //    fibra na mensagem/recomendação.
        //  - Suporte experimental/inferido (SupportLevel != LAB_VALIDATED) pesa
        //    menos no desempate — ver [confiancaEquipamentoLocal].
        // ---------------------------------------------------------------------

        // LOCAL-EQUIP-WIFI-01: fibra da ONT confirmada OK + Wi-Fi ruim + internet
        // ruim/inconclusiva → o equipamento corrobora que o link óptico está bem,
        // reforçando que a causa é Wi-Fi/local. Confiança maior que DECISAO-01
        // genérico (0.65) porque soma evidência direta do equipamento — quando
        // ambas batem, esta regra vence o desempate e DECISAO-01 aparece como
        // achado secundário (mesmo diagnóstico, evidência mais forte).
        val fibraOntConfirmadaOk = localDevice != null &&
            localDevice.deviceType == DeviceType.ONT_GPON &&
            localDevice.statusFibra == LocalDeviceSectionStatus.OK
        if (fibraOntConfirmadaOk && (internetRuim || internetInconclusivo) && wifiRuim) {
            candidatos += Achado(
                DiagnosticResult(
                    id = "LOCAL-EQUIP-WIFI-01",
                    titulo = "Equipamento Confirma: Problema é Local/Wi-Fi",
                    status = DiagnosticStatus.attention,
                    evidencia = "equipamento=${localDevice.vendor}/${localDevice.modelo} statusFibra=OK wifiConfiavel=false",
                    mensagemUsuario = "A ONT confirma que o sinal da fibra está bom — a lentidão detectada provavelmente vem do Wi-Fi ou de um dispositivo na rede local, não da fibra.",
                    recomendacao = "Aproxime-se do roteador, reconecte ao Wi-Fi e refaça o teste.",
                    categoria = CAT,
                    podeConcluir = false,
                    categoriaOrigem = "wifi",
                ),
                confianca = confiancaEquipamentoLocal(0.85, localDevice.supportLevel),
                ativo = true,
            )
        }

        // LOCAL-EQUIP-FIBRA-01: ONT reporta atenção no link óptico + internet ruim
        // → possível problema óptico/provedor. Suprimida quando DECISAO-00/00b
        // (leitura direta do módulo de fibra, mais granular) já cobriu a mesma
        // causa raiz — vira hipótese descartada em vez de duplicar a mensagem.
        val fibraOntComAtencao = localDevice != null &&
            localDevice.deviceType == DeviceType.ONT_GPON &&
            localDevice.statusFibra == LocalDeviceSectionStatus.ATENCAO
        if (fibraOntComAtencao && internetRuim) {
            val suprimidaPorLeituraDireta = fibraCritica || fibraAtencao
            candidatos += Achado(
                DiagnosticResult(
                    id = "LOCAL-EQUIP-FIBRA-01",
                    titulo = "Equipamento Local Indica Problema na Fibra",
                    status = DiagnosticStatus.attention,
                    evidencia = "equipamento=${localDevice.vendor}/${localDevice.modelo} statusFibra=ATENCAO",
                    mensagemUsuario = "A ONT reportou instabilidade no link óptico. Isso pode indicar um problema físico na fibra ou uma falha do lado do provedor.",
                    recomendacao = "Verifique o cabo de fibra e o estado da ONT. Se a instabilidade persistir, contate o provedor.",
                    categoria = CAT,
                    categoriaOrigem = "fibra",
                ),
                confianca = confiancaEquipamentoLocal(0.7, localDevice.supportLevel),
                ativo = !suprimidaPorLeituraDireta,
                motivoDescarte = if (suprimidaPorLeituraDireta) {
                    "suprimida: já coberta por DECISAO-00/00b (leitura direta da fibra, mais granular)"
                } else {
                    null
                },
            )
        }

        // LOCAL-EQUIP-SATURACAO-01: roteador TP-Link com WAN OK + muitos clientes
        // conectados + internet ruim → possível saturação da rede local, não da
        // operadora. Nunca menciona fibra (TP-Link não tem essa seção).
        val saturacaoLocalProvavel = localDevice != null &&
            localDevice.deviceType == DeviceType.ROUTER &&
            localDevice.statusWan == LocalDeviceSectionStatus.OK &&
            localDevice.quantidadeClientes > MUITOS_CLIENTES_THRESHOLD
        if (saturacaoLocalProvavel && internetRuim) {
            candidatos += Achado(
                DiagnosticResult(
                    id = "LOCAL-EQUIP-SATURACAO-01",
                    titulo = "Possível Saturação da Rede Local",
                    status = DiagnosticStatus.attention,
                    evidencia = "equipamento=${localDevice.vendor}/${localDevice.modelo} statusWan=OK clientes=${localDevice.quantidadeClientes}",
                    mensagemUsuario = "O roteador confirma que a internet (WAN) está funcionando, mas há ${localDevice.quantidadeClientes} dispositivos conectados — a lentidão pode ser saturação da rede local, não da operadora.",
                    recomendacao = "Verifique quais dispositivos estão consumindo mais banda e desconecte os que não estão em uso no momento.",
                    categoria = CAT,
                    categoriaOrigem = "local",
                ),
                confianca = confiancaEquipamentoLocal(0.65, localDevice.supportLevel),
                ativo = true,
            )
        }

        // LOCAL-EQUIP-WAN-01: roteador TP-Link sem WAN funcionando + internet ruim
        // → falha upstream ou local, SEM afirmar que é fibra (o roteador não tem
        // como confirmar isso — regra de produto: TP-Link nunca aponta fibra).
        val wanRoteadorIndisponivel = localDevice != null &&
            localDevice.deviceType == DeviceType.ROUTER &&
            (
                localDevice.statusWan == LocalDeviceSectionStatus.ATENCAO ||
                    localDevice.statusWan == LocalDeviceSectionStatus.INDISPONIVEL
            )
        if (wanRoteadorIndisponivel && internetRuim) {
            candidatos += Achado(
                DiagnosticResult(
                    id = "LOCAL-EQUIP-WAN-01",
                    titulo = "Falha na Conexão Upstream do Roteador",
                    status = DiagnosticStatus.critical,
                    evidencia = "equipamento=${localDevice.vendor}/${localDevice.modelo} statusWan=${localDevice.statusWan}",
                    mensagemUsuario = "O roteador não está recebendo conexão da rede upstream. Pode ser um problema local (cabo/configuração) ou uma falha antes do roteador — este equipamento não fornece dados de fibra para confirmar a causa.",
                    recomendacao = "Reinicie o roteador. Se persistir, verifique o cabo até o equipamento anterior (ONT/modem) e contate o provedor caso o problema continue.",
                    categoria = CAT,
                    podeConcluir = false,
                ),
                confianca = confiancaEquipamentoLocal(0.75, localDevice.supportLevel),
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
                    categoriaOrigem = "dns",
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
                    categoriaOrigem = "dns",
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
                    categoriaOrigem = "wifi",
                ),
                confianca = 0.55,
                ativo = true,
            )
        }

        // DECISAO-02 / DECISAO-02b: internet com problema + WiFi ok → culpa é
        // internet/ISP. Guard do engine antigo (`!wifiRuim`) preservado: quando o
        // Wi-Fi é ruim, a causa vira hipótese suprimida em favor de DECISAO-01
        // (Wi-Fi pode estar mascarando/causando o sintoma de internet).
        // Mensagem/recomendação variam por [connectionType]: em rede móvel não faz
        // sentido falar de Wi-Fi/roteador (SIG-514).
        // Guard adicional (GH#542): quando o equipamento local já aponta uma causa
        // mais específica (fibra da ONT com atenção, saturação local por muitos
        // clientes, ou WAN do roteador fora do ar), a explicação genérica "pode ser
        // roteador ou provedor" cede — vira hipótese descartada em favor da regra
        // LOCAL-EQUIP-* correspondente, que já tem a evidência do equipamento.
        // categoriaOrigem (GH#836): propositalmente NULL nesta regra — a própria
        // mensagem já admite duas causas possíveis não distinguíveis pela evidência
        // disponível ("roteador ou provedor" / "operadora ou servidor de destino").
        // Mesmo padrão de ambiguidade genuína do exemplo LOCAL-EQUIP-WAN-01 citado
        // na issue. Sem RTT de gateway (DECISAO-GW-01/02) ou leitura de equipamento
        // que aponte um lado específico, não há como afirmar "isp" com segurança.
        val emRedeMovel = connectionType == ConnectionType.mobile
        val equipamentoLocalExplicaMelhor = fibraOntComAtencao || saturacaoLocalProvavel || wanRoteadorIndisponivel
        if (internetCritico) {
            candidatos += Achado(
                DiagnosticResult(
                    id = "DECISAO-02",
                    titulo = "Problema na Internet",
                    status = DiagnosticStatus.critical,
                    evidencia = "problemas=${internetResultados.filter { it.status == DiagnosticStatus.critical }.map { it.id }}",
                    mensagemUsuario = if (emRedeMovel) {
                        "A rede móvel está estável, mas há problemas na conexão com a internet. O problema pode estar na operadora ou no servidor de destino."
                    } else {
                        "O Wi-Fi está bom, mas há problemas na conexão com a internet. O problema pode estar no roteador ou no provedor."
                    },
                    recomendacao = if (emRedeMovel) {
                        "Teste em outro local ou horário. Se o problema persistir, verifique com sua operadora."
                    } else {
                        "Reinicie o roteador. Se o problema persistir, contate o suporte do seu provedor de internet."
                    },
                    categoria = CAT,
                    podeConcluir = true,
                ),
                confianca = 0.8,
                ativo = !wifiRuim && !equipamentoLocalExplicaMelhor,
                motivoDescarte = when {
                    wifiRuim -> "suprimida por Wi-Fi não confiável durante o teste (ver DECISAO-01)"
                    equipamentoLocalExplicaMelhor -> "suprimida: equipamento local já aponta causa mais específica (ver LOCAL-EQUIP-*)"
                    else -> null
                },
            )
        }
        if (internetAtencao) {
            candidatos += Achado(
                DiagnosticResult(
                    id = "DECISAO-02b",
                    titulo = "Atenção na Qualidade da Internet",
                    status = DiagnosticStatus.attention,
                    evidencia = "alertas=${internetResultados.filter { it.status == DiagnosticStatus.attention }.map { it.id }}",
                    mensagemUsuario = if (emRedeMovel) {
                        "A rede móvel está estável, mas alguns indicadores de internet merecem atenção."
                    } else {
                        "O Wi-Fi está bom, mas alguns indicadores de internet merecem atenção."
                    },
                    recomendacao = if (emRedeMovel) {
                        "Monitore a conexão. Se o problema persistir, teste em outro local ou horário, ou entre em contato com a operadora."
                    } else {
                        "Monitore a conexão. Se o problema persistir, reinicie o roteador ou entre em contato com o provedor."
                    },
                    categoria = CAT,
                    podeConcluir = false,
                ),
                confianca = 0.6,
                ativo = !wifiRuim && !equipamentoLocalExplicaMelhor,
                motivoDescarte = when {
                    wifiRuim -> "suprimida por Wi-Fi não confiável durante o teste (ver DECISAO-01)"
                    equipamentoLocalExplicaMelhor -> "suprimida: equipamento local já aponta causa mais específica (ver LOCAL-EQUIP-*)"
                    else -> null
                },
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
                    categoriaOrigem = "wifi",
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
                    categoriaOrigem = "wifi",
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
                limitacoesEquipamentoLocal = limitacoesLocalDevice(localDevice),
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
            limitacoesEquipamentoLocal = limitacoesLocalDevice(localDevice),
        )
    }

    /**
     * Reduz a confiança de uma regra LOCAL-EQUIP-* quando o driver que produziu
     * o snapshot não é [SupportLevel.LAB_VALIDATED] — dado experimental/inferido
     * pesa menos no desempate por score, nunca some, nunca é tratado como certeza
     * (regra de produto do GH#542: "dados experimentais devem ter peso menor").
     */
    private fun confiancaEquipamentoLocal(base: Double, supportLevel: SupportLevel?): Double =
        when (supportLevel) {
            SupportLevel.LAB_VALIDATED -> base
            SupportLevel.PARSER_IMPORTED, SupportLevel.INFERRED_FAMILY -> base * 0.7
            SupportLevel.UNKNOWN, null -> base * 0.5
        }

    /**
     * Limitações do equipamento local a declarar ao usuário — nunca como achado
     * competindo pelo posto de "principal" (não deve mascarar "Conexão Sem
     * Problemas" quando a internet está de fato saudável), mas sempre presente
     * como nota honesta quando aplicável. Distingue explicitamente "não
     * disponível"/"não suportado" de "problema detectado" (regra de produto do
     * GH#542).
     */
    private fun limitacoesLocalDevice(localDevice: SafeLocalDeviceContext?): List<String> {
        if (localDevice == null) return emptyList()
        val limitacoes = mutableListOf<String>()
        if (localDevice.deviceType == DeviceType.ROUTER &&
            localDevice.statusFibra == LocalDeviceSectionStatus.NAO_SUPORTADO
        ) {
            limitacoes += "Este roteador não informa dados da fibra óptica — não é possível confirmar o " +
                "estado do link óptico por este equipamento."
        }
        if (localDevice.supportLevel == SupportLevel.PARSER_IMPORTED ||
            localDevice.supportLevel == SupportLevel.INFERRED_FAMILY
        ) {
            limitacoes += "Os dados deste equipamento vêm de um driver experimental/inferido e podem não " +
                "ser totalmente precisos."
        }
        if (localDevice.connectionStatus == LocalDeviceSectionStatus.INDISPONIVEL) {
            limitacoes += "Não foi possível se comunicar com o equipamento local nesta leitura."
        }
        return limitacoes
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
    /** Limitações honestas do equipamento de rede local (ONT/roteador) a
     *  declarar ao usuário — ex.: "este roteador não informa fibra", "suporte
     *  experimental" (GH#542). Nunca compete pelo posto de achado principal —
     *  não deve mascarar "Conexão Sem Problemas" quando a internet está
     *  saudável. Vazio quando não há equipamento local ou nenhuma limitação
     *  se aplica. */
    val limitacoesEquipamentoLocal: List<String> = emptyList(),
)
