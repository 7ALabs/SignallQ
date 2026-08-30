package io.signallq.app.ui.screen

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.core.diagnostico.PerguntasDiagnosticoGuiado

enum class EntradaAssist { Padrao, ComDadosRecentes, VideoOuChamada }

enum class TipoMidiaAssist { VIDEO, CHAMADA }

sealed interface EstadoChamadaNds {
    data object EmCurso : EstadoChamadaNds

    data class Sucesso(
        val relatorio: io.signallq.app.core.diagnostico.DiagnosticReport,
    ) : EstadoChamadaNds

    data object Falhou : EstadoChamadaNds
}

// Estado do fluxo guiado 2.0 — issue #1704 (2.0.09b), épico #1647.
//
// Substitui os quatro `remember` independentes que a PR #1708 tentou persistir e foi reprovada por
// isso (bloqueio B3 do parecer de Caio): com quatro slots `rememberSaveable` separados, o
// `SaverObjetivoDiagnostico` podia devolver `null` para nome de enum desconhecido — de propósito,
// para sobreviver à evolução do enum — enquanto passo, respostas e a flag de resultado restauravam
// intactos. O invariante quebrava e produzia dois desfechos reais: `perguntas[passo]` estourando,
// ou o usuário caindo direto no resultado com respostas de OUTRA jornada, sem nada denunciando.
//
// A correção prescrita foi um saver sobre UM objeto, não sobre quatro campos. Aqui o estado é
// indivisível por construção: não existe combinação inválida a restaurar porque não existem slots
// separados, e [DiagnosticoGuiadoEstado.coerente] é a rede que trava o resto.

/**
 * As seis rotas do fluxo guiado — **seis, não sete**.
 *
 * `insufficient` e `recoverable-error`, que a issue #1657 listava como rotas, são **variações
 * dentro das telas**, não destinos: a spec §9 os classifica como estados globais (e sobre "erro
 * transitório" diz "preserva contexto", que é o oposto de navegar), o `COVERAGE.md` do protótipo
 * afirma que não devem virar destinos permanentes, e há um argumento que independe dos dois
 * documentos — se erro é destino, o back a partir do erro volta para a coisa que acabou de falhar
 * e a redispara, virando laço.
 *
 * Estas rotas **não** entram em `AppShellOverlay`. A pilha de overlays é set-like (`open()` ignora
 * duplicata) e este fluxo é cíclico — `Resultado → Orientacao → Reteste → Comparacao → Orientacao`
 * de novo. Além disso overlays acumulam em vez de substituir. Ver
 * `docs_ai/technical/appshell-overlay-registry.md`, seção "Delegação de back".
 */
enum class DiagnosticoGuiadoRota {
    /** Chamada direta ao NDS em curso, ou erro explícito de acesso ao serviço. */
    Processando,

    /** Plano montado e medição em andamento. */
    Analise,

    /** Conclusão compreensível, causa provável, confiança e limite. */
    Resultado,

    /** Uma ação principal com justificativa, mais alternativas secundárias. */
    Orientacao,

    /** Novo teste **vinculado** à análise anterior — não é recomeçar do zero. */
    Reteste,

    /** Veredito melhorou / não mudou / comparação inconclusiva. */
    Comparacao,
}

/**
 * Estado completo do fluxo, restaurado como **unidade atômica**.
 *
 * [pilha] é a back-stack interna: o topo é a rota corrente, e recuar é remover o último. Cíclica
 * por natureza, então admite repetição — diferente da pilha de overlays do shell.
 */
@Immutable
data class DiagnosticoGuiadoEstado(
    val entrada: EntradaAssist = EntradaAssist.Padrao,
    val tipoMidia: TipoMidiaAssist? = null,
    val objetivo: ObjetivoDiagnostico? = null,
    val passo: Int = 0,
    val respostas: List<Int?> = emptyList(),
    val pilha: List<DiagnosticoGuiadoRota> = emptyList(),
    /**
     * Texto livre digitado quando [objetivo] é
     * [io.signallq.app.core.diagnostico.ObjetivoDiagnostico.OUTRO_PROBLEMA] — até
     * [LIMITE_RELATO_LIVRE_DIAGNOSTICO] caracteres, truncado no próprio `onValueChange` da tela
     * (ver `DiagnosticoGuiadoRelatoLivreSection`). **Nunca** usado por [DiagnosticoGuiadoEngine]
     * para decidir status/causa — só viaja como contexto adicional no payload da IA
     * (`relatoLivreUsuario` em `DiagnosisAiContext`, módulo `:feature:diagnostico`). `null`
     * para todo objetivo que não seja `OUTRO_PROBLEMA`, e também quando o usuário pula sem
     * escrever nada.
     */
    val relatoLivre: String? = null,
) {
    /** Rota corrente, ou `null` quando o usuário ainda está escolhendo o objetivo. */
    val rotaAtual: DiagnosticoGuiadoRota? get() = pilha.lastOrNull()

    /**
     * O invariante que a PR #1708 quebrou. Sem objetivo escolhido não pode haver passo avançado,
     * resposta registrada nem rota empilhada — e com objetivo, [passo] tem que apontar para uma
     * pergunta que existe, que é o que fazia `perguntas[passo]` estourar.
     *
     * **Limite é `passo < perguntas.size`, não `<=`.** Uma versão anterior desta PR usava `<=` e
     * documentava `passo == perguntas.size` como "roteiro terminado, estado transitório". Isso era
     * invenção minha: `DiagnosticoGuiadoScreen.kt:300` faz
     * `if (passo < perguntas.size - 1) passo += 1 else mostrarResultado = true` — ao responder a
     * última pergunta o passo **não** incrementa, o fluxo troca de tela. O estado nunca é
     * produzido, e sancioná-lo era pior que inútil: `recuar()` desempilha a rota sem mexer no
     * passo, então um consumidor que confiasse no `<=` cairia em `perguntas[perguntas.size]`.
     * Achado B4 do parecer de Caio na PR #1713.
     *
     * **[respostas] é medido contra o roteiro, e não existe bound cruzado com [passo].** Uma
     * versão intermediária desta PR usou `respostas.size <= passo + 1`, derivado de
     * `DiagnosticoGuiadoScreen.kt:293-297` (`while (size <= passo) add(null)`). Aquela relação vale
     * **no instante em que o usuário responde** e **não sobrevive ao back** — que é justamente o
     * que esta issue existe para modelar.
     *
     * O que faltava olhar é a resposta selecionada na pergunta. O avanço acontece no próprio
     * clique da opção, então o usuário chega ao passo 1 com uma resposta, responde, fica com duas,
     * e voltar o devolve ao passo 0
     * **preservando as duas** — comportamento desejado (`respostas.getOrNull(passo)` re-seleciona
     * a anterior). Com o bound cruzado, `recuar()` produzia estado incoerente a partir de estado
     * coerente, e `saneado()` apagava a jornada inteira se o processo morresse ali. Regressão
     * real, achada por Caio no B5 — e o "estado nonsense" que o B2 descrevia (*restaurado na
     * pergunta 0 com duas respostas*) é, na verdade, **o usuário que apertou voltar**.
     *
     * A relação verdadeira seria `respostas.size <= maiorPassoAlcancado + 1`, e esse campo não
     * existe aqui. Não vale acrescentá-lo: o teto do roteiro já barra o dano original
     * (`perguntas[passo]` estourar).
     *
     * **Risco residual assumido:** um roteiro que ganhe uma pergunta numa versão futura aceita
     * save antigo mais curto. O motor recebe `respostas.filterNotNull()` e degrada em vez de
     * estourar. Impedir isso exigiria validar conteúdo contra o catálogo de perguntas, que acopla
     * o modelo de estado ao catálogo e se paga em toda restauração — fora de escopo por decisão.
     * Se algum roteiro mudar de verdade, a mitigação é **bump de chave do saver**, não predicado.
     */
    val coerente: Boolean
        get() =
            if (objetivo == null) {
                passo == 0 && respostas.isEmpty() && pilha.isEmpty()
            } else {
                // OUTRO_PROBLEMA não tem pergunta fechada (`perguntas` vazio) — o único `passo`
                // coerente para ele é 0, e não `passo < 0` (sempre falso). Os demais objetivos
                // continuam com o teto `passo < perguntas.size` de sempre.
                val totalPerguntas = PerguntasDiagnosticoGuiado.perguntas(objetivo).size
                val passoCoerente = if (totalPerguntas == 0) passo == 0 else passo in 0 until totalPerguntas
                passoCoerente && respostas.size <= totalPerguntas
            }

    /**
     * Devolve um estado garantidamente coerente. Usado na restauração: em vez de confiar no que
     * veio do `Bundle`, o fluxo volta ao início quando o conteúdo não fecha.
     *
     * O caso que motiva isto é concreto: o app é atualizado, um objetivo sai do enum, e o estado
     * salvo aponta para um nome que não existe mais. Voltar ao início perde a jornada — mas perder
     * a jornada é o resultado **honesto**; o alternativo é diagnosticar com respostas que não
     * correspondem às perguntas mostradas.
     */
    fun saneado(): DiagnosticoGuiadoEstado = if (coerente) this else DiagnosticoGuiadoEstado()

    /**
     * Avança para [rota]. Repetição é permitida — o fluxo é cíclico.
     *
     * Não valida: `DiagnosticoGuiadoEstado().empilhar(Resultado)` produz estado incoerente e
     * ninguém reclama. [coerente] é **rede de restauração, não invariante de construção** — só é
     * consultado na fronteira do saver. Estado vivo incoerente sobreviveria até a próxima morte de
     * processo, e aí seria apagado. Quem construir o fluxo é responsável por só empilhar a partir
     * de estado válido. Risco residual 1 do parecer de Caio na PR #1713.
     */
    fun empilhar(rota: DiagnosticoGuiadoRota): DiagnosticoGuiadoEstado = copy(pilha = pilha + rota)

    /**
     * Transição de avanço que troca o topo em vez de empilhar sobre ele — issue #1720.
     *
     * `Analise → Resultado` (medição concluída) e `Resultado → Analise` (CTA "testar
     * novamente") são o mesmo tipo de evento: a rota anterior deixou de existir, não é um passo
     * intermediário para onde [recuar] deva voltar. Empilhar de verdade só acontece quando
     * [pilha] está vazia — ao entrar em `Analise` pela primeira vez a partir do roteiro de
     * perguntas, onde [recuar] precisa devolver a `[]` (a última pergunta), não a um destino que
     * nunca existiu.
     */
    fun irPara(rota: DiagnosticoGuiadoRota): DiagnosticoGuiadoEstado =
        copy(pilha = if (pilha.isEmpty()) pilha + rota else pilha.dropLast(1) + rota)

    /**
     * Recua um passo interno. Devolve `null` quando não há mais o que recuar — que é o sinal para
     * o `RegistrarBackDoOverlay` responder `false` e deixar o shell fechar o overlay inteiro.
     *
     * **Obrigações da fiação, sem correspondente aqui.** `onResetarAnalisador()` é efeito colateral
     * e não pertence a um data class — mas some da vista se não estiver escrito. Sem ele, o
     * analisador de IA fica quente. São **três** call sites hoje em `DiagnosticoGuiadoScreen.kt`,
     * e cada um exige tratamento próprio:
     *
     * | Linha | Contexto | Coberto por este `recuar()`? |
     * |---|---|---|
     * | `:192` | dentro de `voltarUmPasso()`, ao sair do resultado | não — mapeia para `pilha.dropLast(1)` |
     * | `:250` | `LaunchedEffect` ao **entrar** no resultado, antes de `onAnalisarProblema` | não — é efeito de entrada, independente da navegação |
     * | `:262` | `onEscolherOutraSituacao`, reset completo a partir do resultado | não — mapeia para o reset total, e é o mais fácil de esquecer |
     *
     * O risco residual 2 do parecer de Caio na PR #1713 citava só o `:192`. O levantamento da
     * parte 4/4 (comentário na #1704) encontrou os outros dois — o `:262` em particular não estava
     * documentado em lugar nenhum.
     */
    fun recuar(): DiagnosticoGuiadoEstado? =
        when {
            pilha.isNotEmpty() -> copy(pilha = pilha.dropLast(1))
            objetivo != null && passo > 0 -> copy(passo = passo - 1)
            objetivo != null -> DiagnosticoGuiadoEstado()
            else -> null
        }

    companion object {
        /** Sentinela para "pergunta ainda não respondida" — negativa porque índice de opção é sempre >= 0. */
        const val RESPOSTA_NAO_RESPONDIDA = -1

        private const val SEM_OBJETIVO = ""

        /**
         * Saver **único** para o estado inteiro. É a diferença estrutural em relação à PR #1708:
         * um slot, uma restauração, nenhuma combinação parcial possível.
         *
         * Formato: `[nomeObjetivo, passo, respostasCsv, pilhaCsv]`. Enum vai por **nome**, nunca
         * por ordinal — reordenar o enum não pode ressuscitar a análise apontando para outro
         * problema. Nome desconhecido cai no [saneado], que devolve o estado inicial.
         *
         * `relatoLivre` entrou como **7º elemento**, aditivo: `formatoAtual` continua checando
         * `size >= 6`, então um estado salvo por uma versão anterior (sem o 7º elemento) restaura
         * normalmente com `relatoLivre = null` via `getOrNull(6)`.
         */
        val Saver: Saver<DiagnosticoGuiadoEstado, List<String>> =
            Saver(
                save = { estado ->
                    listOf(
                        estado.entrada.name,
                        estado.tipoMidia?.name.orEmpty(),
                        estado.objetivo?.name ?: SEM_OBJETIVO,
                        estado.passo.toString(),
                        estado.respostas.joinToString(",") { (it ?: RESPOSTA_NAO_RESPONDIDA).toString() },
                        estado.pilha.joinToString(",") { it.name },
                        estado.relatoLivre.orEmpty(),
                    )
                },
                restore = { valores ->
                    val formatoAtual = valores.size >= 6
                    val nomeEntrada = if (formatoAtual) valores.getOrNull(0).orEmpty() else EntradaAssist.Padrao.name
                    val nomeMidia = if (formatoAtual) valores.getOrNull(1).orEmpty() else ""
                    val nomeObjetivo = valores.getOrNull(if (formatoAtual) 2 else 0).orEmpty()
                    val tokensPilha =
                        valores
                            .getOrNull(if (formatoAtual) 5 else 3)
                            .orEmpty()
                            .split(',')
                            .filter(String::isNotBlank)
                    val pilhaRestaurada =
                        tokensPilha.map { nome ->
                            DiagnosticoGuiadoRota.entries.firstOrNull { it.name == nome }
                        }
                    // Fail-closed, igual ao objetivo: token de rota que não mapeia derruba o
                    // estado inteiro, não é descartado em silêncio. Achado B3 de Caio na PR #1713 —
                    // a versão anterior usava `mapNotNull` aqui e nuke total no objetivo, duas
                    // políticas OPOSTAS para o mesmo evento (evolução de enum entre versões), no
                    // mesmo saver. E a permissiva era a errada: `"Analise,Resultado"` com
                    // `Resultado` renomeado restaurava `[Analise]`, e o usuário que estava lendo o
                    // resultado acordava na medição — que redispara sozinha. No meio da pilha era
                    // pior: `[Resultado, X, Comparacao]` virava um histórico de navegação que
                    // nunca existiu, com `recuar()` andando por ele.
                    if (pilhaRestaurada.any { it == null }) {
                        DiagnosticoGuiadoEstado()
                    } else {
                        DiagnosticoGuiadoEstado(
                            objetivo =
                                if (nomeObjetivo == SEM_OBJETIVO) {
                                    null
                                } else {
                                    ObjetivoDiagnostico.entries.firstOrNull { it.name == nomeObjetivo }
                                },
                            entrada = EntradaAssist.entries.firstOrNull { it.name == nomeEntrada } ?: EntradaAssist.Padrao,
                            tipoMidia = TipoMidiaAssist.entries.firstOrNull { it.name == nomeMidia },
                            passo = valores.getOrNull(if (formatoAtual) 3 else 1)?.toIntOrNull() ?: 0,
                            respostas =
                                valores
                                    .getOrNull(if (formatoAtual) 4 else 2)
                                    .orEmpty()
                                    .split(',')
                                    .filter(String::isNotBlank)
                                    .map { texto ->
                                        // `>= 0` em vez de `!= RESPOSTA_NAO_RESPONDIDA`: cobre o
                                        // sentinela E qualquer negativo que atravessaria como
                                        // índice de opção válido. Subconjunto grátis da validação
                                        // de conteúdo, sugerido no B2.
                                        texto.toIntOrNull()?.takeIf { it >= 0 }
                                    },
                            pilha = pilhaRestaurada.filterNotNull(),
                            relatoLivre = valores.getOrNull(6)?.takeIf { it.isNotEmpty() },
                        ).saneado()
                    }
                },
            )
    }
}
