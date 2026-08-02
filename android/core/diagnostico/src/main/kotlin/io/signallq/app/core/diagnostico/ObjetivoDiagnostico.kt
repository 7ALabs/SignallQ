package io.signallq.app.core.diagnostico

/**
 * Os 7 objetivos fechados do diagnóstico guiado (Feature #550, issue #1475). Cada
 * objetivo abre um roteiro próprio de perguntas fechadas — nunca chat livre, ver
 * [PerguntaFechada] — e prioriza um subconjunto diferente de métricas do
 * [DiagnosticInput] em [DiagnosticoGuiadoEngine]. Mesma copy/ordem do protótipo
 * #1474 (`diagnostico-guiado.jsx`, array `OBJETIVOS`), issue #1483.
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
 * Roteiro fixo de perguntas por objetivo — mesma copy do protótipo #1474.
 * [DiagnosticoGuiadoEngine] usa o índice da opção escolhida (não o texto) para as
 * poucas perguntas que de fato mudam a avaliação — ver kdoc de cada `avaliarXxx`
 * em [DiagnosticoGuiadoEngine] para o mapeamento exato de qual pergunta/índice
 * influencia o quê.
 */
object PerguntasDiagnosticoGuiado {

    fun perguntas(objetivo: ObjetivoDiagnostico): List<PerguntaFechada> =
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
                    PerguntaFechada(
                        texto = "O que você percebe quando acontece?",
                        opcoes =
                            listOf(
                                "A rede Wi-Fi desaparece",
                                "Continua conectado, mas a internet para",
                                "Desconecta e reconecta sozinho",
                                "Não sei dizer",
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
                    PerguntaFechada(
                        texto = "Isso acontece em qual conexão?",
                        opcoes = listOf("Wi-Fi", "Dados móveis", "Nas duas", "Ainda não consegui comparar"),
                    ),
                )
            ObjetivoDiagnostico.JOGOS_COM_LAG ->
                listOf(
                    PerguntaFechada(
                        texto = "Em qual conexão você joga?",
                        opcoes = listOf("Wi-Fi", "Cabo de rede", "Dados móveis"),
                    ),
                    PerguntaFechada(
                        texto = "Com que frequência isso acontece?",
                        opcoes =
                            listOf(
                                "Quase sempre",
                                "Só em horário de pico",
                                "De vez em quando, sem padrão",
                            ),
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
                    PerguntaFechada(
                        texto = "Piora se outra pessoa em casa também estiver usando a internet?",
                        opcoes = listOf("Sim, bastante", "Um pouco", "Não muda", "Ainda não testei"),
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
                    PerguntaFechada(
                        texto = "O problema é mais forte em qual conexão?",
                        opcoes = listOf("Wi-Fi", "Dados móveis", "Nas duas igual"),
                    ),
                )
            ObjetivoDiagnostico.VELOCIDADE_NAO_CHEGA ->
                listOf(
                    PerguntaFechada(
                        texto = "Você já comparou com outro teste?",
                        opcoes = listOf("Testei apenas no SignallQ", "Comparei com outro aplicativo ou site"),
                    ),
                    PerguntaFechada(
                        texto = "Como você fez este teste?",
                        opcoes =
                            listOf(
                                "Com um cabo de rede",
                                "Perto do roteador, pelo Wi-Fi",
                                "Longe do roteador, pelo Wi-Fi",
                            ),
                    ),
                )
            ObjetivoDiagnostico.WIFI_VS_OPERADORA ->
                listOf(
                    PerguntaFechada(
                        texto = "A internet melhora quando você desliga o Wi-Fi e usa a rede móvel?",
                        opcoes = listOf("Sim, melhora muito", "Sim, um pouco", "Não muda nada", "Ainda não testei"),
                    ),
                    PerguntaFechada(
                        texto = "Outros aparelhos também apresentam esse problema?",
                        opcoes = listOf("Sim, todos", "Só alguns", "Não, só este aparelho"),
                    ),
                )
        }
}
