package io.signallq.app.core.recommendation

/**
 * Flags remotas que controlam o comportamento do engine sem exigir release do app.
 *
 * @param minAffiliateMatchRatio fracao minima de tags casadas (tags da recomendacao que
 *   tambem estao no diagnostico) para uma recomendacao monetizada contextual (afiliado,
 *   parceiro, operadora) ser elegivel. Evita "produto afiliado sem relacao clara" -- regra
 *   obrigatoria da issue #790.
 */
data class RecommendationFlags(
    val affiliateEnabled: Boolean = true,
    val partnerOffersEnabled: Boolean = true,
    val operatorOffersEnabled: Boolean = true,
    val nativeAdFallbackEnabled: Boolean = true,
    val minAffiliateMatchRatio: Double = 0.5,
)

/** Contexto estruturado de entrada do Recommendation Engine para um diagnostico. */
data class RecommendationRequest(
    val tags: Set<DiagnosticTag>,
    val network: NetworkContextType,
    val metrics: DiagnosticMetrics = DiagnosticMetrics(),
    val isp: String? = null,
    val device: DeviceContext? = null,
    val history: List<RecommendationHistoryEntry> = emptyList(),
    val flags: RecommendationFlags = RecommendationFlags(),
    val diagnosticId: String? = null,
)
