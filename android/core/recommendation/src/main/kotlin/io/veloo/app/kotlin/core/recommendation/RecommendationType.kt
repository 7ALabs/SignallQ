package io.signallq.app.core.recommendation

/**
 * Tipos de recomendacao suportados pelo motor. A ordem dos valores reflete a prioridade
 * de exibicao definida na estrategia de decisao da issue #790: recomendacao gratuita
 * primeiro, monetizacao contextual depois, AdMob nativo como ultimo recurso.
 */
enum class RecommendationType {
    FREE_TIP,
    TUTORIAL,
    CONFIGURATION,
    AFFILIATE_PRODUCT,
    PARTNER_OFFER,
    OPERATOR_OFFER,
    NATIVE_AD_FALLBACK,
    ;

    val monetized: Boolean
        get() = this == AFFILIATE_PRODUCT || this == PARTNER_OFFER || this == OPERATOR_OFFER || this == NATIVE_AD_FALLBACK
}
