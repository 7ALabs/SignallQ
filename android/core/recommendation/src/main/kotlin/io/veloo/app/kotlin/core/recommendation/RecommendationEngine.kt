package io.signallq.app.core.recommendation

import io.signallq.app.core.recommendation.catalog.RecommendationCatalog

/**
 * Motor de decisao de recomendacoes do SignallQ (issue #790).
 *
 * Desacoplado do motor de diagnostico: recebe um [RecommendationRequest] ja estruturado
 * (tags, metricas, contexto de rede, historico) e devolve recomendacoes elegiveis ranqueadas.
 * Nao depende de Compose/UI, Room ou qualquer SDK de analytics/anuncio -- quem integra decide
 * como carregar o historico e para onde mandar os eventos de [io.signallq.app.core.recommendation.analytics].
 *
 * Estrategia de decisao (issue #790):
 * 1. Recomendacao gratuita (free_tip/tutorial/configuration) quando resolve o problema.
 * 2. Produto afiliado apenas com forte relacao com o diagnostico (ver [RecommendationFlags.minAffiliateMatchRatio]).
 * 3. Servico/parceiro/operadora quando mais adequado que produto.
 * 4. AdMob nativo (fallback) apenas se nada contextual for elegivel.
 *
 * O motor sempre retorna, no maximo, UMA recomendacao monetizada por chamada de [choose] --
 * a regra "nunca afiliado + AdMob simultaneos" e garantida estruturalmente porque so existe
 * um card por diagnostico.
 */
class RecommendationEngine(
    private val catalog: RecommendationCatalog,
    private val now: () -> Long = System::currentTimeMillis,
) {

    /** Lista ranqueada de recomendacoes elegiveis para o contexto informado. */
    fun rank(request: RecommendationRequest): List<RecommendationDecision> {
        val nowMs = now()
        val allCandidates = catalog.all()

        val contextual = allCandidates
            .asSequence()
            .filter { it.type != RecommendationType.NATIVE_AD_FALLBACK }
            .filter { passesNetwork(it, request) }
            .filter { passesFlags(it, request.flags) }
            .filter { passesTagRelevance(it, request.tags) }
            .filter { passesAffiliateMatchThreshold(it, request) }
            .filter { passesHistory(it, request.history, nowMs) }
            .map { toDecision(it, request, nowMs) }
            .toList()

        val eligible = contextual.ifEmpty {
            allCandidates
                .asSequence()
                .filter { it.type == RecommendationType.NATIVE_AD_FALLBACK }
                .filter { request.flags.nativeAdFallbackEnabled }
                .filter { passesHistory(it, request.history, nowMs) }
                .map { toDecision(it, request, nowMs) }
                .toList()
        }

        return eligible.sortedWith(compareBy({ it.priorityTier }, { -it.score }))
    }

    /** A unica recomendacao a exibir para o diagnostico, ou null se nada for elegivel. */
    fun choose(request: RecommendationRequest): RecommendationDecision? = rank(request).firstOrNull()

    private fun passesNetwork(candidate: Recommendation, request: RecommendationRequest): Boolean =
        candidate.applicableNetworkTypes.isEmpty() || request.network in candidate.applicableNetworkTypes

    private fun passesFlags(candidate: Recommendation, flags: RecommendationFlags): Boolean = when (candidate.type) {
        RecommendationType.AFFILIATE_PRODUCT -> flags.affiliateEnabled
        RecommendationType.PARTNER_OFFER -> flags.partnerOffersEnabled
        RecommendationType.OPERATOR_OFFER -> flags.operatorOffersEnabled
        RecommendationType.NATIVE_AD_FALLBACK -> flags.nativeAdFallbackEnabled
        RecommendationType.FREE_TIP, RecommendationType.TUTORIAL, RecommendationType.CONFIGURATION -> true
    }

    private fun passesTagRelevance(candidate: Recommendation, requestTags: Set<DiagnosticTag>): Boolean =
        candidate.tags.isEmpty() || candidate.tags.intersect(requestTags).isNotEmpty()

    /** Bloqueia monetizacao contextual sem relacao clara com o diagnostico -- regra obrigatoria da issue #790. */
    private fun passesAffiliateMatchThreshold(candidate: Recommendation, request: RecommendationRequest): Boolean {
        if (candidate.type !in MONETIZED_CONTEXTUAL_TYPES) return true
        return matchRatio(candidate, request.tags) >= request.flags.minAffiliateMatchRatio
    }

    /** Cooldown, limites de frequencia, ocultacao pelo usuario e nao-repeticao em sequencia. */
    private fun passesHistory(candidate: Recommendation, history: List<RecommendationHistoryEntry>, nowMs: Long): Boolean {
        val entriesForId = history.filter { it.recommendationId == candidate.id }
        if (entriesForId.any { it.feedback == RecommendationFeedbackType.HIDE }) return false

        val mostRecentOverall = history.maxByOrNull { it.shownAt }
        if (mostRecentOverall?.recommendationId == candidate.id) return false

        val lastShown = entriesForId.maxByOrNull { it.shownAt }
        if (lastShown != null && candidate.cooldownHours > 0) {
            val cooldownMs = candidate.cooldownHours * HOUR_MS
            if (nowMs - lastShown.shownAt < cooldownMs) return false
        }

        val dayCount = entriesForId.count { nowMs - it.shownAt < DAY_MS }
        if (dayCount >= candidate.maxPerDay) return false

        val weekCount = entriesForId.count { nowMs - it.shownAt < WEEK_MS }
        if (weekCount >= candidate.maxPerWeek) return false

        return true
    }

    private fun toDecision(candidate: Recommendation, request: RecommendationRequest, nowMs: Long): RecommendationDecision {
        val matched = candidate.tags.intersect(request.tags)
        val ratio = matchRatio(candidate, request.tags)
        val feedbackAdjustment = feedbackAdjustment(candidate.id, request.history)
        val score = (ratio * TAG_MATCH_WEIGHT) + candidate.basePriority + feedbackAdjustment
        return RecommendationDecision(
            recommendation = candidate,
            matchedTags = matched,
            score = score,
            priorityTier = tierOf(candidate.type),
            reason = buildReason(candidate, matched, ratio),
            trackingId = "${candidate.id}-$nowMs",
        )
    }

    private fun feedbackAdjustment(id: String, history: List<RecommendationHistoryEntry>): Double =
        history.filter { it.recommendationId == id }.sumOf {
            when (it.feedback) {
                RecommendationFeedbackType.HELPFUL -> HELPFUL_BONUS
                RecommendationFeedbackType.NOT_HELPFUL -> NOT_HELPFUL_PENALTY
                RecommendationFeedbackType.DISMISSED -> DISMISSED_PENALTY
                RecommendationFeedbackType.CLICKED -> CLICKED_BONUS
                RecommendationFeedbackType.HIDE, null -> 0.0
            }
        }

    private fun matchRatio(candidate: Recommendation, requestTags: Set<DiagnosticTag>): Double {
        if (candidate.tags.isEmpty()) return 1.0
        val matched = candidate.tags.intersect(requestTags)
        return matched.size.toDouble() / candidate.tags.size
    }

    private fun tierOf(type: RecommendationType): Int = when (type) {
        RecommendationType.FREE_TIP, RecommendationType.TUTORIAL, RecommendationType.CONFIGURATION -> 0
        RecommendationType.AFFILIATE_PRODUCT, RecommendationType.PARTNER_OFFER, RecommendationType.OPERATOR_OFFER -> 1
        RecommendationType.NATIVE_AD_FALLBACK -> 2
    }

    private fun buildReason(candidate: Recommendation, matched: Set<DiagnosticTag>, ratio: Double): String {
        if (matched.isEmpty()) return "Sem recomendacao contextual elegivel; usando fallback ${candidate.type}."
        val tagList = matched.joinToString(", ") { it.id }
        val matchPercent = (ratio * PERCENT).toInt()
        return "Casou com tags: $tagList ($matchPercent% de match)."
    }

    private companion object {
        const val TAG_MATCH_WEIGHT = 100.0
        const val PERCENT = 100
        const val HOUR_MS = 3_600_000L
        const val DAY_MS = 24 * HOUR_MS
        const val WEEK_MS = 7 * DAY_MS
        const val HELPFUL_BONUS = 15.0
        const val NOT_HELPFUL_PENALTY = -25.0
        const val DISMISSED_PENALTY = -10.0
        const val CLICKED_BONUS = 5.0

        val MONETIZED_CONTEXTUAL_TYPES = setOf(
            RecommendationType.AFFILIATE_PRODUCT,
            RecommendationType.PARTNER_OFFER,
            RecommendationType.OPERATOR_OFFER,
        )
    }
}
