package io.signallq.app.core.diagnostico

import kotlin.math.abs

/**
 * Shadow mode (GH#1444, parte de #952) — compara o [DiagnosticReport] local
 * (motor embarcado, autoritativo para o usuario) com o mesmo snapshot avaliado
 * em paralelo pelo worker remoto, e classifica a divergencia entre os dois.
 *
 * Puro e deterministico (sem IO, sem rede) — quem dispara a avaliacao remota e
 * decide o que fazer com [Comparison.classification] e responsabilidade de
 * quem chama [classify] (ver `RemoteDiagnosticRepository.evaluateShadow` em
 * `:featureDiagnostico`). Este objeto nunca altera nenhum dos dois relatorios,
 * so os compara.
 *
 * ## Classificacoes (vocabulario fixado por #952, nao inventar novo aqui)
 * - [Classification.LOCAL_ERROR] / [Classification.REMOTE_ERROR]: um dos dois
 *   lados nao produziu relatorio (motor local nunca deveria falhar em producao —
 *   [DiagnosticRunner.run] e puro e sempre retorna algo — mas o parametro aceita
 *   nulo para o classificador continuar generico e testavel isoladamente).
 * - [Classification.EXACT_MATCH]: mesmo status, mesma origem da decisao
 *   ([DiagnosticResult.categoriaOrigem]) e score dentro de [EXACT_SCORE_TOLERANCE_PONTOS].
 * - [Classification.EQUIVALENT_RESULT]: mesmo "grupo" de severidade (ver [grupo])
 *   e score dentro de [EQUIVALENT_SCORE_TOLERANCE_PONTOS], mesmo com status ou
 *   origem exatos diferentes (ex.: local=ok/wifi, remoto=info/dns — os dois "sem
 *   problema real", só o motivo textual difere).
 * - [Classification.MINOR_DIVERGENCE]: grupos de severidade adjacentes (ex.:
 *   saudavel vs atencao, atencao vs critico, ou um dos dois inconclusivo).
 * - [Classification.MAJOR_DIVERGENCE]: qualquer coisa mais distante que isso
 *   (ex.: local diz saudavel, remoto diz critico) — o caso que mais importa
 *   detectar, pois indica que um dos dois motores erraria a decisao do usuario
 *   se fosse autoritativo.
 */
object DiagnosticDivergenceClassifier {

    enum class Classification(val wireValue: String) {
        EXACT_MATCH("EXACT_MATCH"),
        EQUIVALENT_RESULT("EQUIVALENT_RESULT"),
        MINOR_DIVERGENCE("MINOR_DIVERGENCE"),
        MAJOR_DIVERGENCE("MAJOR_DIVERGENCE"),
        REMOTE_ERROR("REMOTE_ERROR"),
        LOCAL_ERROR("LOCAL_ERROR"),
    }

    /** Resumo comparavel dos dois lados — o que efetivamente vira o registro
     *  sanitizado enviado ao worker (ver `DiagnosticDivergenceReporter`). */
    data class Comparison(
        val classification: Classification,
        val localStatus: DiagnosticStatus?,
        val remoteStatus: DiagnosticStatus?,
        val localScore: Int?,
        val remoteScore: Int?,
        val localFlow: String?,
        val remoteFlow: String?,
    )

    private const val EXACT_SCORE_TOLERANCE_PONTOS = 5
    private const val EQUIVALENT_SCORE_TOLERANCE_PONTOS = 20

    fun classify(local: DiagnosticReport?, remote: DiagnosticReport?): Comparison {
        val localStatus = local?.decisao?.status
        val remoteStatus = remote?.decisao?.status
        val localScore = local?.scoreConexao
        val remoteScore = remote?.scoreConexao
        val localFlow = local?.decisao?.categoriaOrigem
        val remoteFlow = remote?.decisao?.categoriaOrigem

        val classification = when {
            local == null -> Classification.LOCAL_ERROR
            remote == null -> Classification.REMOTE_ERROR
            else -> classifyBothPresent(
                localStatus = localStatus!!,
                remoteStatus = remoteStatus!!,
                localScore = localScore!!,
                remoteScore = remoteScore!!,
                localFlow = localFlow,
                remoteFlow = remoteFlow,
            )
        }

        return Comparison(
            classification = classification,
            localStatus = localStatus,
            remoteStatus = remoteStatus,
            localScore = localScore,
            remoteScore = remoteScore,
            localFlow = localFlow,
            remoteFlow = remoteFlow,
        )
    }

    private fun classifyBothPresent(
        localStatus: DiagnosticStatus,
        remoteStatus: DiagnosticStatus,
        localScore: Int,
        remoteScore: Int,
        localFlow: String?,
        remoteFlow: String?,
    ): Classification {
        val scoreDelta = abs(localScore - remoteScore)
        val groupGap = abs(grupo(localStatus) - grupo(remoteStatus))

        return when {
            localStatus == remoteStatus && localFlow == remoteFlow && scoreDelta <= EXACT_SCORE_TOLERANCE_PONTOS ->
                Classification.EXACT_MATCH
            groupGap == 0 && scoreDelta <= EQUIVALENT_SCORE_TOLERANCE_PONTOS ->
                Classification.EQUIVALENT_RESULT
            groupGap <= 1 ->
                Classification.MINOR_DIVERGENCE
            else ->
                Classification.MAJOR_DIVERGENCE
        }
    }

    /** Grupo de severidade — vocabulario proprio deste classificador (nao e
     *  [DiagnosticStatus] nem [DiagnosticEvaluationStatus]), so para medir a
     *  "distancia" entre duas classificacoes. `inconclusive` fica isolado: um
     *  motor que nao conseguiu concluir nada nunca e equivalente a um motor que
     *  concluiu "saudavel", mesmo que os dois nao apontem problema algum. */
    private fun grupo(status: DiagnosticStatus): Int = when (status) {
        DiagnosticStatus.inconclusive -> -1
        DiagnosticStatus.ok -> 0
        DiagnosticStatus.info -> 0
        DiagnosticStatus.attention -> 1
        DiagnosticStatus.critical -> 2
    }
}
