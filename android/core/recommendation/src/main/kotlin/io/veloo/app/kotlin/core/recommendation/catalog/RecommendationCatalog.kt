package io.signallq.app.core.recommendation.catalog

import io.signallq.app.core.recommendation.Recommendation

/**
 * Fonte de recomendacoes candidatas para o [io.signallq.app.core.recommendation.RecommendationEngine].
 *
 * Uma implementacao remota (fora do escopo da issue #790) buscaria o catalogo configurado
 * no backend/Remote Config. Ate existir, o app usa [LocalRecommendationCatalog] como fallback.
 */
fun interface RecommendationCatalog {
    fun all(): List<Recommendation>
}
