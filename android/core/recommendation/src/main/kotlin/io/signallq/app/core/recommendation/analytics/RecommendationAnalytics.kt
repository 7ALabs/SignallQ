package io.signallq.app.core.recommendation.analytics

import io.signallq.app.core.recommendation.RecommendationDecision
import io.signallq.app.core.recommendation.RecommendationFeedbackType
import io.signallq.app.core.recommendation.RecommendationType

/** Nome dos eventos de analytics gerados pelo Recommendation Engine, conforme issue #790. */
enum class RecommendationAnalyticsEventName(
    val eventName: String,
) {
    ELIGIBLE("recommendation_eligible"),
    SHOWN("recommendation_shown"),
    CLICKED("recommendation_clicked"),
    DISMISSED("recommendation_dismissed"),
    FEEDBACK("recommendation_feedback"),
    FALLBACK_AD_SHOWN("recommendation_fallback_ad_shown"),
}

/**
 * Payload de analytics gerado a partir de uma [RecommendationDecision].
 *
 * ## Nao carrega `matchedTags`, e a ausencia e o ponto (GH#1703/#1717)
 *
 * Ate 2026-08-17 este payload trazia `matchedTags: Set<DiagnosticTag>`, e o tracker do Firebase
 * enviava a lista como `matched_tags` -- isto e, a CONCLUSAO do diagnostico da pessoa
 * (`wifi_fraco`, `velocidade_abaixo_do_contratado`) saindo do aparelho para o Google, junto de
 * `score` e `diagnostic_id`. Um dos eventos que carregava isso e `recommendation_fallback_ad_shown`.
 *
 * A politica de privacidade descreve o Firebase Analytics como "eventos anonimos de uso (telas
 * visitadas, acoes realizadas)". Conclusao de diagnostico nao e nem uma coisa nem outra. Luiz
 * decidiu tirar o rotulo em vez de alargar o texto.
 *
 * O campo saiu **do payload**, nao so da linha do tracker: enquanto ele existia aqui, reintroduzir
 * o vazamento era uma linha no Firebase e a suite inteira passava verde -- mutante rodado por Caio
 * na rodada 4 da PR #1717. Sem o campo, reabrir vira erro de compilacao. E o mesmo argumento que
 * `NativeAdContentSignal` registra para o construtor privado: garantia que uma politica publicada
 * sustenta nao pode morar em comentario.
 *
 * O funil de recomendacao segue mensuravel por [recommendationId], [type], [score], [ruleOrigin],
 * [diagnosticId], [monetized] e [feedback].
 *
 * ## O que NAO se perde, e a versao anterior deste KDoc afirmava que sim
 *
 * A redacao anterior dizia que "o que se perde e quebrar a metrica por tipo de problema da pessoa".
 * E falso, e Caio mostrou por que (rodada 5 da PR #1717): [recommendationId] e proxy deterministico
 * da tag.
 *
 * - `RecommendationEngine.passesTagRelevance` so elege candidato cujas tags **intersectam** as do
 *   relatorio da pessoa (excecao: candidato de `tags` vazio);
 * - `LocalRecommendationCatalog` mapeia id -> tag **1:1**: `free_tip_reposicionar_roteador` ->
 *   `WIFI_FRACO`, `configuration_trocar_dns` -> `DNS_LENTO`,
 *   `operator_offer_upgrade_plano` -> `VELOCIDADE_ABAIXO_DO_CONTRATADO`, e assim por diante. So o
 *   `native_ad_fallback_default` e neutro.
 *
 * Quem tiver os eventos e o catalogo -- que viaja dentro do APK -- recupera a conclusao do
 * diagnostico. E o mesmo dado, escrito com outro alfabeto.
 *
 * Tirar [matchedTags] continua valendo: reduz exposicao de forma estrita e remove o rotulo
 * explicito. Mas nao fecha o canal derivado, e este KDoc nao pode dar a entender que fecha.
 * Fechar (ou aceitar por escrito) e decisao de produto em aberto -- ver a issue #1730.
 */
data class RecommendationAnalyticsPayload(
    val eventName: RecommendationAnalyticsEventName,
    val recommendationId: String,
    val type: RecommendationType,
    val score: Double,
    val diagnosticId: String?,
    val monetized: Boolean,
    val ruleOrigin: String,
    val feedback: RecommendationFeedbackType? = null,
)

/** Implementado por quem consome o engine (ex: featureDiagnostico) para enviar ao Firebase Analytics. */
fun interface RecommendationAnalyticsTracker {
    fun track(payload: RecommendationAnalyticsPayload)
}

/** Monta o payload de analytics para uma decisao do engine, sem acoplar o engine a nenhum SDK. */
fun RecommendationDecision.toAnalyticsPayload(
    eventName: RecommendationAnalyticsEventName,
    diagnosticId: String?,
    feedback: RecommendationFeedbackType? = null,
): RecommendationAnalyticsPayload =
    RecommendationAnalyticsPayload(
        eventName = eventName,
        recommendationId = recommendation.id,
        type = type,
        score = score,
        diagnosticId = diagnosticId,
        monetized = monetized,
        ruleOrigin = ruleOrigin,
        feedback = feedback,
    )
