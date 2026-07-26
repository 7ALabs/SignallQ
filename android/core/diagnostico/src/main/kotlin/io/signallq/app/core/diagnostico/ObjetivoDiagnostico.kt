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
        titulo = "Internet cai ou oscila",
        subtitulo = "A conexão para de funcionar ou fica instável por alguns segundos",
    ),
    VIDEOS_TRAVAM(
        titulo = "Vídeos travam ou dão buffer",
        subtitulo = "Streaming trava, fica \"carregando\" ou perde qualidade sozinho",
    ),
    JOGOS_COM_LAG(
        titulo = "Jogos com lag ou ping alto",
        subtitulo = "Atraso, travadas ou desconexões durante partidas online",
    ),
    CHAMADAS_CONGELAM(
        titulo = "Chamadas de vídeo congelam",
        subtitulo = "Imagem trava, áudio corta ou a chamada cai",
    ),
    SITES_DEMORAM(
        titulo = "Sites demoram para carregar",
        subtitulo = "Páginas demoram, ficam \"girando\" ou falham ao abrir",
    ),
    VELOCIDADE_NAO_CHEGA(
        titulo = "Velocidade não chega no contratado",
        subtitulo = "O teste mostra menos do que você paga pela sua operadora",
    ),
    WIFI_VS_OPERADORA(
        titulo = "Não sei se é o Wi-Fi ou a operadora",
        subtitulo = "Quer descobrir se o problema é do roteador ou da internet contratada",
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
                                "Só durante uso pesado (jogos, streaming)",
                                "Só em alguns cômodos da casa",
                            ),
                    ),
                    PerguntaFechada(
                        texto = "O que você percebe quando acontece?",
                        opcoes =
                            listOf(
                                "O Wi-Fi some da lista de redes",
                                "Continua conectado mas trava",
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
                                "Fica \"carregando\" (buffer)",
                                "Cai a qualidade sozinho",
                                "Trava e o app fecha",
                                "Só em determinado app",
                            ),
                    ),
                    PerguntaFechada(
                        texto = "Isso acontece em qual conexão?",
                        opcoes = listOf("Wi-Fi", "Dados móveis", "Nas duas", "Só percebi agora"),
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
                                "Direto, quase sempre",
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
                                "Imagem congela, áudio segue",
                                "Áudio corta ou fica robótico",
                                "A chamada cai e reconecta",
                                "Os dois travam juntos",
                            ),
                    ),
                    PerguntaFechada(
                        texto = "Piora se outra pessoa em casa também estiver usando a internet?",
                        opcoes = listOf("Sim, bastante", "Um pouco", "Não muda", "Não testei"),
                    ),
                )
            ObjetivoDiagnostico.SITES_DEMORAM ->
                listOf(
                    PerguntaFechada(
                        texto = "Isso acontece com...",
                        opcoes =
                            listOf(
                                "Qualquer site",
                                "Só sites específicos",
                                "Só a primeira página (depois melhora)",
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
                        opcoes = listOf("Só testei pelo SignallQ", "Comparei com outro app ou site"),
                    ),
                    PerguntaFechada(
                        texto = "Este teste foi feito...",
                        opcoes =
                            listOf(
                                "Perto do roteador, por cabo",
                                "Perto do roteador, por Wi-Fi",
                                "Longe do roteador, por Wi-Fi",
                            ),
                    ),
                )
            ObjetivoDiagnostico.WIFI_VS_OPERADORA ->
                listOf(
                    PerguntaFechada(
                        texto = "O problema muda se você desligar o Wi-Fi e usar dados móveis?",
                        opcoes = listOf("Sim, melhora muito", "Sim, um pouco", "Não muda nada", "Ainda não testei"),
                    ),
                    PerguntaFechada(
                        texto = "Outros dispositivos em casa têm o mesmo problema?",
                        opcoes = listOf("Sim, todos", "Só alguns", "Não, só este aparelho"),
                    ),
                )
        }
}
