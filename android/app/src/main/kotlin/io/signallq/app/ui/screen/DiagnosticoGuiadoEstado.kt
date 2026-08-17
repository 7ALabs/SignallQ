package io.signallq.app.ui.screen

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.core.diagnostico.PerguntasDiagnosticoGuiado

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
 * As cinco rotas do fluxo guiado — **cinco, não sete**.
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
    val objetivo: ObjetivoDiagnostico? = null,
    val passo: Int = 0,
    val respostas: List<Int?> = emptyList(),
    val pilha: List<DiagnosticoGuiadoRota> = emptyList(),
) {
    /** Rota corrente, ou `null` quando o usuário ainda está escolhendo o objetivo. */
    val rotaAtual: DiagnosticoGuiadoRota? get() = pilha.lastOrNull()

    /**
     * O invariante que a PR #1708 quebrou. Sem objetivo escolhido não pode haver passo avançado,
     * resposta registrada nem rota empilhada — e com objetivo, [passo] não pode apontar além do
     * roteiro dele, que é o que fazia `perguntas[passo]` estourar.
     *
     * `passo == perguntas.size` é válido: significa "roteiro terminado", estado transitório entre
     * responder a última pergunta e empilhar [DiagnosticoGuiadoRota.Resultado].
     */
    val coerente: Boolean
        get() =
            if (objetivo == null) {
                passo == 0 && respostas.isEmpty() && pilha.isEmpty()
            } else {
                passo >= 0 &&
                    passo <= PerguntasDiagnosticoGuiado.perguntas(objetivo).size &&
                    respostas.size <= PerguntasDiagnosticoGuiado.perguntas(objetivo).size
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

    /** Avança para [rota]. Repetição é permitida — o fluxo é cíclico. */
    fun empilhar(rota: DiagnosticoGuiadoRota): DiagnosticoGuiadoEstado = copy(pilha = pilha + rota)

    /**
     * Recua um passo interno. Devolve `null` quando não há mais o que recuar — que é o sinal para
     * o `RegistrarBackDoOverlay` responder `false` e deixar o shell fechar o overlay inteiro.
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
         */
        val Saver: Saver<DiagnosticoGuiadoEstado, List<String>> =
            Saver(
                save = { estado ->
                    listOf(
                        estado.objetivo?.name ?: SEM_OBJETIVO,
                        estado.passo.toString(),
                        estado.respostas.joinToString(",") { (it ?: RESPOSTA_NAO_RESPONDIDA).toString() },
                        estado.pilha.joinToString(",") { it.name },
                    )
                },
                restore = { valores ->
                    val nomeObjetivo = valores.getOrNull(0).orEmpty()
                    DiagnosticoGuiadoEstado(
                        objetivo =
                            if (nomeObjetivo == SEM_OBJETIVO) {
                                null
                            } else {
                                ObjetivoDiagnostico.entries.firstOrNull { it.name == nomeObjetivo }
                            },
                        passo = valores.getOrNull(1)?.toIntOrNull() ?: 0,
                        respostas =
                            valores
                                .getOrNull(2)
                                .orEmpty()
                                .split(',')
                                .filter(String::isNotBlank)
                                .map { texto ->
                                    texto.toIntOrNull()?.takeIf { it != RESPOSTA_NAO_RESPONDIDA }
                                },
                        pilha =
                            valores
                                .getOrNull(3)
                                .orEmpty()
                                .split(',')
                                .filter(String::isNotBlank)
                                .mapNotNull { nome ->
                                    DiagnosticoGuiadoRota.entries.firstOrNull { it.name == nome }
                                },
                    ).saneado()
                },
            )
    }
}
