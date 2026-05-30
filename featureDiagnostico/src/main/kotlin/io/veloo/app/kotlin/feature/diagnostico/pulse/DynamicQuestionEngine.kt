package io.veloo.app.feature.diagnostico.pulse

import io.veloo.app.feature.diagnostico.DiagnosticReport
import io.veloo.app.feature.diagnostico.DiagnosticStatus

class DynamicQuestionEngine {

    // ---- Árvore de perguntas ----

    private sealed interface Node {
        data class Question(val node: QuestionNode, val children: Map<String, Node>) : Node
        data object Leaf : Node
    }

    private val tree: Map<String, Node> = mapOf(
        "internet_lenta" to Node.Question(
            node = QuestionNode(
                id = "internet_lenta_q1",
                texto = "Quando a lentidão ocorre?",
                opcoes = listOf(
                    OpcaoResposta("sempre", "Sempre / o dia todo", "Lentidão constante, independente do horário."),
                    OpcaoResposta("horarios", "Em horários específicos", "Lentidão em horários de pico (noite, fim de semana)."),
                    OpcaoResposta("so_certos_apps", "Só em certos apps ou sites", "Problema localizado em aplicativos ou serviços específicos."),
                ),
            ),
            children = mapOf(
                "sempre" to Node.Question(
                    node = QuestionNode(
                        id = "internet_lenta_q2a",
                        texto = "Em quais situações você percebe mais?",
                        opcoes = listOf(
                            OpcaoResposta("web", "Navegar na web / redes sociais", "Lentidão em navegação web e redes sociais."),
                            OpcaoResposta("video", "Assistir vídeo (YouTube, Netflix)", "Lentidão ao carregar ou assistir vídeos em streaming."),
                            OpcaoResposta("download", "Baixar arquivos grandes", "Lentidão ao fazer downloads."),
                            OpcaoResposta("tudo", "Em todas as situações igualmente", "Lentidão generalizada em todas as atividades."),
                        ),
                    ),
                    children = mapOf(
                        "web" to Node.Leaf,
                        "video" to Node.Leaf,
                        "download" to Node.Leaf,
                        "tudo" to Node.Leaf,
                    ),
                ),
                "horarios" to Node.Question(
                    node = QuestionNode(
                        id = "internet_lenta_q2b",
                        texto = "Qual horário é pior?",
                        opcoes = listOf(
                            OpcaoResposta("noite", "Após as 18h / noite", "Horário de pico noturno, quando muitos vizinhos usam a internet."),
                            OpcaoResposta("fim_semana", "Fim de semana", "Maior uso da rede no fim de semana."),
                            OpcaoResposta("dia_util", "Durante o dia útil", "Lentidão em horário comercial, pode indicar problema de contrato."),
                        ),
                    ),
                    children = mapOf("noite" to Node.Leaf, "fim_semana" to Node.Leaf, "dia_util" to Node.Leaf),
                ),
                "so_certos_apps" to Node.Question(
                    node = QuestionNode(
                        id = "internet_lenta_q2c",
                        texto = "Qual aplicativo ou site apresenta o problema?",
                        opcoes = listOf(
                            OpcaoResposta("youtube", "YouTube", "Lentidão específica no YouTube."),
                            OpcaoResposta("whatsapp", "WhatsApp", "Lentidão ao enviar/receber mensagens ou mídia no WhatsApp."),
                            OpcaoResposta("redes_sociais", "Instagram / TikTok / Reels", "Lentidão em conteúdo de vídeo curto nas redes sociais."),
                            OpcaoResposta("trabalho", "Apps de trabalho (Teams, Drive, Zoom)", "Lentidão em ferramentas de produtividade e colaboração."),
                        ),
                    ),
                    children = mapOf(
                        "youtube" to Node.Leaf, "whatsapp" to Node.Leaf,
                        "redes_sociais" to Node.Leaf, "trabalho" to Node.Leaf,
                    ),
                ),
            ),
        ),
        "jogos_travando" to Node.Question(
            node = QuestionNode(
                id = "jogos_q1",
                texto = "Qual dispositivo você usa para jogar?",
                opcoes = listOf(
                    OpcaoResposta("console", "Console (PlayStation / Xbox / Nintendo)", "Jogo em console doméstico."),
                    OpcaoResposta("pc", "PC / Computador", "Jogo em computador pessoal."),
                    OpcaoResposta("celular", "Celular / Tablet", "Jogo em dispositivo móvel."),
                ),
            ),
            children = mapOf(
                "console" to Node.Question(
                    node = QuestionNode(
                        id = "jogos_q2_console",
                        texto = "O console está conectado como?",
                        opcoes = listOf(
                            OpcaoResposta("cabo", "Cabo Ethernet (cabo de rede)", "Console conectado via cabo direto ao roteador."),
                            OpcaoResposta("wifi_console", "Wi-Fi", "Console conectado via Wi-Fi."),
                        ),
                    ),
                    children = mapOf("cabo" to Node.Leaf, "wifi_console" to Node.Leaf),
                ),
                "pc" to Node.Question(
                    node = QuestionNode(
                        id = "jogos_q2_pc",
                        texto = "O PC está conectado como?",
                        opcoes = listOf(
                            OpcaoResposta("cabo_pc", "Cabo Ethernet", "PC conectado via cabo direto ao roteador."),
                            OpcaoResposta("wifi_pc", "Wi-Fi", "PC conectado via Wi-Fi."),
                        ),
                    ),
                    children = mapOf("cabo_pc" to Node.Leaf, "wifi_pc" to Node.Leaf),
                ),
                "celular" to Node.Question(
                    node = QuestionNode(
                        id = "jogos_q2_celular",
                        texto = "Qual tipo de conexão no celular?",
                        opcoes = listOf(
                            OpcaoResposta("wifi_cel", "Wi-Fi doméstico", "Celular conectado ao Wi-Fi de casa."),
                            OpcaoResposta("dados", "Dados móveis (4G/5G)", "Celular usando dados móveis da operadora."),
                        ),
                    ),
                    children = mapOf("wifi_cel" to Node.Leaf, "dados" to Node.Leaf),
                ),
            ),
        ),
        "streaming_ruim" to Node.Question(
            node = QuestionNode(
                id = "streaming_q1",
                texto = "Qual serviço de streaming apresenta problema?",
                opcoes = listOf(
                    OpcaoResposta("netflix", "Netflix", "Problema no Netflix."),
                    OpcaoResposta("youtube_str", "YouTube", "Problema no YouTube."),
                    OpcaoResposta("prime", "Prime Video / Disney+", "Problema em Prime Video ou Disney+."),
                    OpcaoResposta("outro_str", "Outro serviço", "Problema em outro serviço de streaming."),
                ),
            ),
            children = mapOf(
                "netflix" to Node.Question(
                    node = QuestionNode(
                        id = "streaming_q2_netflix",
                        texto = "Qual é o problema no Netflix?",
                        opcoes = listOf(
                            OpcaoResposta("qualidade", "Qualidade de imagem baixa / borrada", "Streaming em resolução baixa, imagem pouco nítida."),
                            OpcaoResposta("trava", "Trava / pausa constantemente", "Vídeo para e fica carregando (buffering)."),
                            OpcaoResposta("demora", "Demora muito para iniciar", "Longo tempo de carregamento antes do vídeo começar."),
                        ),
                    ),
                    children = mapOf("qualidade" to Node.Leaf, "trava" to Node.Leaf, "demora" to Node.Leaf),
                ),
                "youtube_str" to Node.Question(
                    node = QuestionNode(
                        id = "streaming_q2_youtube",
                        texto = "Qual é o problema no YouTube?",
                        opcoes = listOf(
                            OpcaoResposta("qualidade_yt", "Qualidade de imagem baixa", "Vídeo em resolução baixa mesmo em Wi-Fi."),
                            OpcaoResposta("trava_yt", "Trava / buffering", "Vídeo para para carregar."),
                            OpcaoResposta("demora_yt", "Demora para carregar", "Carregamento lento de vídeos."),
                        ),
                    ),
                    children = mapOf("qualidade_yt" to Node.Leaf, "trava_yt" to Node.Leaf, "demora_yt" to Node.Leaf),
                ),
                "prime" to Node.Question(
                    node = QuestionNode(
                        id = "streaming_q2_prime",
                        texto = "Qual é o problema?",
                        opcoes = listOf(
                            OpcaoResposta("qualidade_prime", "Qualidade baixa", "Imagem borrada ou em baixa resolução."),
                            OpcaoResposta("trava_prime", "Trava / buffering", "Vídeo interrompido frequentemente."),
                            OpcaoResposta("demora_prime", "Demora para iniciar", "Tempo longo de carregamento."),
                        ),
                    ),
                    children = mapOf("qualidade_prime" to Node.Leaf, "trava_prime" to Node.Leaf, "demora_prime" to Node.Leaf),
                ),
                "outro_str" to Node.Leaf,
            ),
        ),
        "wifi_oscilando" to Node.Question(
            node = QuestionNode(
                id = "wifi_q1",
                texto = "Em qual área da casa o Wi-Fi oscila?",
                opcoes = listOf(
                    OpcaoResposta("proximo", "Perto do roteador", "Oscilação mesmo próximo ao roteador."),
                    OpcaoResposta("longe", "Cômodo distante do roteador", "Sinal fraco em áreas mais afastadas."),
                    OpcaoResposta("toda_casa", "Em toda a casa igualmente", "Oscilação generalizada."),
                ),
            ),
            children = mapOf(
                "proximo" to Node.Question(
                    node = QuestionNode(
                        id = "wifi_q2_proximo",
                        texto = "Quando a oscilação acontece?",
                        opcoes = listOf(
                            OpcaoResposta("sempre_osc", "Sempre / constantemente", "Wi-Fi instável o tempo todo."),
                            OpcaoResposta("horarios_osc", "Em horários específicos", "Problema em horários de pico."),
                            OpcaoResposta("ao_mover", "Ao mover o dispositivo", "Queda de sinal ao andar pela casa."),
                        ),
                    ),
                    children = mapOf("sempre_osc" to Node.Leaf, "horarios_osc" to Node.Leaf, "ao_mover" to Node.Leaf),
                ),
                "longe" to Node.Question(
                    node = QuestionNode(
                        id = "wifi_q2_longe",
                        texto = "Qual a distância aproximada do roteador?",
                        opcoes = listOf(
                            OpcaoResposta("um_comodo", "1 cômodo de distância", "Separado por uma parede/porta."),
                            OpcaoResposta("dois_comodos", "2 ou mais cômodos", "Distância considerável, várias paredes."),
                            OpcaoResposta("andar", "Andar diferente (casa com andares)", "Sinal precisando atravessar laje/piso."),
                        ),
                    ),
                    children = mapOf("um_comodo" to Node.Leaf, "dois_comodos" to Node.Leaf, "andar" to Node.Leaf),
                ),
                "toda_casa" to Node.Leaf,
            ),
        ),
        "chamadas_ruins" to Node.Question(
            node = QuestionNode(
                id = "chamadas_q1",
                texto = "Que tipo de chamada apresenta problema?",
                opcoes = listOf(
                    OpcaoResposta("videochamada", "Videochamada (Meet, Zoom, Teams)", "Problema em chamadas de vídeo por internet."),
                    OpcaoResposta("voz_voip", "Áudio pelo WhatsApp ou Telegram", "Problema em chamadas de voz via app."),
                    OpcaoResposta("celular_op", "Chamada pela operadora (ligação celular)", "Problema em ligação telefônica comum."),
                ),
            ),
            children = mapOf(
                "videochamada" to Node.Question(
                    node = QuestionNode(
                        id = "chamadas_q2_video",
                        texto = "Qual é o problema principal na videochamada?",
                        opcoes = listOf(
                            OpcaoResposta("voz_cortada", "Voz cortada / robótica", "Áudio com cortes, atraso ou distorção."),
                            OpcaoResposta("video_travado", "Vídeo travado / pixelado", "Imagem congelando ou com baixa qualidade."),
                            OpcaoResposta("cai_chamada", "A chamada cai frequentemente", "Desconexão durante a chamada."),
                        ),
                    ),
                    children = mapOf("voz_cortada" to Node.Leaf, "video_travado" to Node.Leaf, "cai_chamada" to Node.Leaf),
                ),
                "voz_voip" to Node.Question(
                    node = QuestionNode(
                        id = "chamadas_q2_voip",
                        texto = "Qual é o problema no áudio?",
                        opcoes = listOf(
                            OpcaoResposta("demora_conectar", "Demora para conectar", "Leva muito tempo para a chamada estabelecer."),
                            OpcaoResposta("voz_cortada_wpp", "Voz cortada durante a ligação", "Áudio com interrupções."),
                            OpcaoResposta("cai_wpp", "A chamada cai", "Desconexão durante a conversa."),
                        ),
                    ),
                    children = mapOf("demora_conectar" to Node.Leaf, "voz_cortada_wpp" to Node.Leaf, "cai_wpp" to Node.Leaf),
                ),
                "celular_op" to Node.Leaf,
            ),
        ),
        "nao_sei_explicar" to Node.Question(
            node = QuestionNode(
                id = "nao_sei_q1",
                texto = "Qual é a sensação geral com a internet?",
                opcoes = listOf(
                    OpcaoResposta("lento_geral", "Tudo parece mais lento que o normal", "Percepção geral de lentidão."),
                    OpcaoResposta("cai_volta", "A internet cai e volta sozinha", "Reconexões frequentes e inesperadas."),
                    OpcaoResposta("so_dispositivo", "Só meu dispositivo tem problema", "Outros dispositivos funcionam, mas o meu não."),
                ),
            ),
            children = mapOf(
                "lento_geral" to Node.Leaf,
                "cai_volta" to Node.Question(
                    node = QuestionNode(
                        id = "nao_sei_q2_cai",
                        texto = "Com que frequência a internet cai?",
                        opcoes = listOf(
                            OpcaoResposta("varias_dia", "Várias vezes ao dia", "Quedas muito frequentes, impacto alto no uso."),
                            OpcaoResposta("uma_dia", "Uma ou duas vezes ao dia", "Quedas moderadas."),
                            OpcaoResposta("raramente", "Raramente (uma vez por semana ou menos)", "Quedas esporádicas."),
                        ),
                    ),
                    children = mapOf("varias_dia" to Node.Leaf, "uma_dia" to Node.Leaf, "raramente" to Node.Leaf),
                ),
                "so_dispositivo" to Node.Leaf,
            ),
        ),
    )

    // ---- API pública ----

    fun getFeedbackChips(): List<OpcaoResposta> {
        return listOf(
            OpcaoResposta(
                "feedback_persistente",
                "Problema persistente",
                "O usuário relata que o problema descrito no diagnóstico continua ocorrendo.",
            ),
            OpcaoResposta(
                "feedback_resolvido",
                "Problema resolvido",
                "O usuário relata que o problema foi solucionado.",
            ),
            OpcaoResposta(
                "feedback_nao_wifi",
                "Não é Wi-Fi",
                "O usuário esclarece que o problema NÃO é relacionado ao Wi-Fi.",
            ),
            OpcaoResposta(
                "feedback_nao_internet",
                "Não é internet",
                "O usuário esclarece que o problema NÃO é de conexão com a internet.",
            ),
            OpcaoResposta(
                "feedback_util",
                "Informação útil",
                "O usuário encontrou a análise útil e relevante ao seu problema.",
            ),
        )
    }

    fun getInitialChips(report: DiagnosticReport?): List<OpcaoResposta> {
        val base = listOf(
            OpcaoResposta("internet_lenta", "Internet lenta", "O usuário relata que a internet está lenta."),
            OpcaoResposta("jogos_travando", "Jogos travando", "O usuário relata travamentos ou lag em jogos online."),
            OpcaoResposta("streaming_ruim", "Streaming ruim", "O usuário relata problemas ao assistir vídeos online."),
            OpcaoResposta("wifi_oscilando", "Wi-Fi oscilando", "O usuário relata instabilidade no sinal Wi-Fi."),
            OpcaoResposta("chamadas_ruins", "Problema em chamadas", "O usuário relata problemas em chamadas de vídeo ou voz."),
            OpcaoResposta("nao_sei_explicar", "Não sei explicar", "O usuário tem dificuldade em descrever o problema."),
        )
        if (report == null) return base

        // Reordena por relevância do diagnóstico local
        val prioridade = mutableListOf<String>()
        if (report.temCritico || report.decisao.status == DiagnosticStatus.critical) {
            prioridade += "internet_lenta"
        }
        if (report.wifiResultados.any { it.status == DiagnosticStatus.attention || it.status == DiagnosticStatus.critical }) {
            prioridade += "wifi_oscilando"
        }
        if (report.dnsResultados.any { it.status == DiagnosticStatus.attention || it.status == DiagnosticStatus.critical }) {
            prioridade += "streaming_ruim"
        }
        prioridade += base.map { it.id }.filter { it !in prioridade }
        return prioridade.mapNotNull { id -> base.find { it.id == id } }
    }

    fun getNextQuestion(chipId: String, history: List<QuestionAnswer>): QuestionNode? {
        val root = tree[chipId] ?: return null
        return resolveNextQuestion(root, history, 0)
    }

    private fun resolveNextQuestion(node: Node, history: List<QuestionAnswer>, depth: Int): QuestionNode? {
        return when (node) {
            is Node.Leaf -> null
            is Node.Question -> {
                if (depth >= history.size) return node.node
                val answer = history[depth]
                val next = node.children[answer.answerId] ?: return null
                resolveNextQuestion(next, history, depth + 1)
            }
        }
    }

    fun buildContextContribution(question: QuestionNode, answer: OpcaoResposta): String =
        "Pergunta: '${question.texto}' → Resposta: '${answer.label}'. ${answer.contextoParaIA}"

    fun isFeedbackChip(chipId: String): Boolean =
        chipId.startsWith("feedback_")

    fun isLeafAnswer(chipId: String, answerId: String, history: List<QuestionAnswer>): Boolean {
        val root = tree[chipId] ?: return true
        return resolveIsLeaf(root, history, answerId, 0)
    }

    private fun resolveIsLeaf(node: Node, history: List<QuestionAnswer>, targetAnswerId: String, depth: Int): Boolean {
        return when (node) {
            is Node.Leaf -> true
            is Node.Question -> {
                if (depth >= history.size) {
                    // Estamos na questão atual — verificar se a resposta leva a Leaf
                    val child = node.children[targetAnswerId] ?: return true
                    child is Node.Leaf
                } else {
                    val answer = history[depth]
                    val next = node.children[answer.answerId] ?: return true
                    resolveIsLeaf(next, history, targetAnswerId, depth + 1)
                }
            }
        }
    }
}
