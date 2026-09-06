package io.signallq.app.core.diagnostico

/**
 * Os 7 objetivos fechados do diagnóstico guiado (Feature #550, issue #1475), mais
 * [OUTRO_PROBLEMA] — opção de saída para quem não se reconhece em nenhum dos 7 (issue de
 * melhoria do Assist, 2026-08). Cada um dos 7 objetivos fechados abre um roteiro próprio de
 * **1 pergunta fechada** — nunca chat livre, ver [PerguntaFechada] — e prioriza um
 * subconjunto diferente de métricas do [DiagnosticInput] em [DiagnosticoGuiadoEngine]. Mesma
 * copy/ordem do protótipo #1474 (`diagnostico-guiado.jsx`, array `OBJETIVOS`), issue #1483.
 * Roteiro reduzido de 2 para 1 pergunta por objetivo (2026-08) — ver kdoc de
 * [PerguntasDiagnosticoGuiado] para o racional da consolidação.
 *
 * [OUTRO_PROBLEMA] é diferente dos demais: [PerguntasDiagnosticoGuiado.perguntas] devolve
 * lista vazia para ele — em vez de pergunta fechada, a tela mostra um campo de texto livre
 * (até 200 caracteres, ver `DiagnosticoGuiadoEstado.relatoLivre`). Esse texto **nunca** é lido
 * por [DiagnosticoGuiadoEngine] para decidir status/causa (regra de produto inalterada: motor
 * só decide a partir de métricas reais) — ele só viaja como contexto adicional no payload da
 * IA (`relatoLivreUsuario` em `DiagnosisAiContext`, módulo `:feature:diagnostico`).
 */
enum class ObjetivoDiagnostico(
    val titulo: String,
    val subtitulo: String,
) {
    INTERNET_CAI_OSCILA(
        titulo = "A internet cai ou fica instável",
        subtitulo = "A conexão para de funcionar ou oscila por alguns segundos.",
    ),
    VIDEOS_TRAVAM(
        titulo = "Vídeos travam ou ficam carregando",
        subtitulo = "Os vídeos travam, ficam carregando ou perdem qualidade.",
    ),
    JOGOS_COM_LAG(
        titulo = "Jogos atrasam ou travam",
        subtitulo = "Há atrasos, travadas ou quedas durante as partidas.",
    ),
    CHAMADAS_CONGELAM(
        titulo = "Chamadas de vídeo travam",
        subtitulo = "A imagem trava, o áudio corta ou a chamada cai.",
    ),
    SITES_DEMORAM(
        titulo = "Sites demoram para abrir",
        subtitulo = "As páginas demoram ou não conseguem abrir.",
    ),
    VELOCIDADE_NAO_CHEGA(
        titulo = "A velocidade está abaixo do plano",
        subtitulo = "O teste mostra uma velocidade menor que a do seu plano.",
    ),
    WIFI_VS_OPERADORA(
        titulo = "Não sei onde está o problema",
        subtitulo = "Vamos verificar se o problema está no Wi-Fi ou na operadora.",
    ),

    /** Ver kdoc da classe — sem pergunta fechada própria, mostra campo de texto livre. */
    OUTRO_PROBLEMA(
        titulo = "Outro problema",
        subtitulo = "Descreva com suas palavras o que está acontecendo.",
    ),
}

/**
 * Pergunta fechada do diagnóstico guiado — sempre single-select entre [opcoes],
 * nunca campo de texto livre (regra de produto da Feature #550: "perguntas
 * fechadas, não chat livre").
 */
data class PerguntaFechada(
    val texto: String,
    val opcoes: List<String>,
)

/**
 * Roteiro fixo de perguntas por objetivo — mesma copy do protótipo #1474 para a
 * pergunta que sobreviveu à consolidação (issue de redução "muitas perguntas,
 * difícil escolher", 2026-08). [DiagnosticoGuiadoEngine] usa o índice da opção
 * escolhida (não o texto) para as poucas perguntas que de fato mudam a avaliação —
 * ver kdoc de cada `avaliarXxx` em [DiagnosticoGuiadoEngine] para o mapeamento
 * exato de qual pergunta/índice influencia o quê.
 *
 * ## Consolidação de 2 perguntas fechadas para 1 (2026-08)
 * Cada objetivo tinha 2 perguntas fechadas fixas. Auditoria do
 * [DiagnosticoGuiadoEngine] mostrou que ele **nunca leu o índice da 2ª pergunta**
 * em nenhum dos 7 objetivos — só [ObjetivoDiagnostico.JOGOS_COM_LAG] e
 * [ObjetivoDiagnostico.WIFI_VS_OPERADORA] usam `respostas.getOrNull(0)`, e a
 * pergunta que eles leem já era a 1ª da lista. Não há, portanto, nenhum objetivo
 * em que a consolidação perca sinal usado pelo motor — os 7 foram reduzidos a 1
 * pergunta, sem exceção. Critério de escolha de qual pergunta manter:
 * 1. a pergunta cujo índice o motor lê (quando existe: JOGOS_COM_LAG,
 *    WIFI_VS_OPERADORA);
 * 2. entre as demais, a mais diagnóstica por natureza (sintoma > meta-pergunta);
 * 3. eliminar a pergunta redundante com uma métrica que o app já mede sozinho
 *    ([DiagnosticInput.connectionType] cobre "isso acontece em qual conexão?" —
 *    caso de [ObjetivoDiagnostico.VIDEOS_TRAVAM] e [ObjetivoDiagnostico.SITES_DEMORAM]).
 *
 * A pergunta única de cada objetivo é a candidata natural a "subcategoria" no
 * payload da IA (ver `AiObjetivoDiagnosticoFactory` em `:feature:diagnostico`),
 * espelhando o par objective+subcategory que o NDS v2 já usa em outros produtos —
 * sem migrar o motor local para o NDS, que segue fora de escopo.
 */
object PerguntasDiagnosticoGuiado {
    /**
     * @param tipoConexao tipo de conexão do teste mais recente
     * ([DiagnosticInput.connectionType]). Usado só pelos roteiros que citam a conexão
     * ativa ([ObjetivoDiagnostico.VELOCIDADE_NAO_CHEGA] e
     * [ObjetivoDiagnostico.WIFI_VS_OPERADORA]), para não perguntar sobre
     * roteador/canal Wi-Fi quando o teste foi feito em rede móvel — bug relatado:
     * roteiro sempre citava Wi-Fi mesmo com o teste em rede móvel. `null` (ou
     * qualquer valor diferente de [ConnectionType.mobile]) mantém a copy histórica
     * voltada a Wi-Fi/cabo.
     */
    fun perguntas(
        objetivo: ObjetivoDiagnostico,
        tipoConexao: ConnectionType? = null,
    ): List<PerguntaFechada> =
        when (objetivo) {
            ObjetivoDiagnostico.INTERNET_CAI_OSCILA ->
                listOf(
                    PerguntaFechada(
                        texto = "Quando isso costuma acontecer?",
                        opcoes =
                            listOf(
                                "A qualquer momento",
                                "Em horários de pico",
                                "Quando várias pessoas ou aplicativos usam a internet",
                                "Só em alguns cômodos da casa",
                            ),
                    ),
                )
            ObjetivoDiagnostico.VIDEOS_TRAVAM ->
                listOf(
                    PerguntaFechada(
                        texto = "O que acontece quando o vídeo trava?",
                        opcoes =
                            listOf(
                                "Fica carregando",
                                "Cai a qualidade sozinho",
                                "O vídeo trava e o aplicativo fecha",
                                "Só em determinado aplicativo",
                            ),
                    ),
                )
            ObjetivoDiagnostico.JOGOS_COM_LAG ->
                listOf(
                    PerguntaFechada(
                        texto = "Em qual conexão você joga?",
                        opcoes = listOf("Wi-Fi", "Cabo de rede", "Dados móveis"),
                    ),
                )
            ObjetivoDiagnostico.CHAMADAS_CONGELAM ->
                listOf(
                    PerguntaFechada(
                        texto = "O que costuma acontecer na chamada?",
                        opcoes =
                            listOf(
                                "A imagem trava, mas o áudio continua",
                                "O áudio corta ou fica distorcido",
                                "A chamada cai e reconecta",
                                "A imagem e o áudio travam",
                            ),
                    ),
                )
            ObjetivoDiagnostico.SITES_DEMORAM ->
                listOf(
                    PerguntaFechada(
                        texto = "Isso acontece em quais sites?",
                        opcoes =
                            listOf(
                                "Qualquer site",
                                "Só sites específicos",
                                "Só na primeira página. Depois melhora.",
                            ),
                    ),
                )
            ObjetivoDiagnostico.VELOCIDADE_NAO_CHEGA ->
                listOf(perguntaComoFezOTeste(tipoConexao))
            ObjetivoDiagnostico.WIFI_VS_OPERADORA ->
                listOf(perguntaMelhoraTrocandoConexao(tipoConexao))
            // Sem pergunta fechada — a tela mostra texto livre em vez desta lista. Ver kdoc de
            // ObjetivoDiagnostico.OUTRO_PROBLEMA.
            ObjetivoDiagnostico.OUTRO_PROBLEMA -> emptyList()
        }

    private fun perguntaComoFezOTeste(tipoConexao: ConnectionType?): PerguntaFechada =
        if (tipoConexao == ConnectionType.mobile) {
            PerguntaFechada(
                texto = "Como você fez este teste?",
                opcoes =
                    listOf(
                        "Do lado de fora, com boa cobertura",
                        "Dentro de casa ou de um prédio",
                        "Em movimento (carro, ônibus etc.)",
                    ),
            )
        } else {
            PerguntaFechada(
                texto = "Como você fez este teste?",
                opcoes =
                    listOf(
                        "Com um cabo de rede",
                        "Perto do roteador, pelo Wi-Fi",
                        "Longe do roteador, pelo Wi-Fi",
                    ),
            )
        }

    private fun perguntaMelhoraTrocandoConexao(tipoConexao: ConnectionType?): PerguntaFechada =
        PerguntaFechada(
            texto =
                if (tipoConexao == ConnectionType.mobile) {
                    "A internet melhora quando você troca os dados móveis pelo Wi-Fi?"
                } else {
                    "A internet melhora quando você desliga o Wi-Fi e usa a rede móvel?"
                },
            opcoes = listOf("Sim, melhora muito", "Sim, um pouco", "Não muda nada", "Ainda não testei"),
        )
}
