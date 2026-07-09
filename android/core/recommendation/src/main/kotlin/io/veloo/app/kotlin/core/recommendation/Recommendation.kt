package io.signallq.app.core.recommendation

/**
 * Item de catalogo (remoto ou fallback local). Representa uma recomendacao candidata,
 * ainda nao decidida -- a decisao final (com score e motivo) e o [RecommendationDecision].
 *
 * @param tags tags de diagnostico que esta recomendacao resolve. Vazio = generica (usada
 *   apenas pelo fallback de AdMod nativo, que nao depende de contexto).
 * @param applicableNetworkTypes tipos de rede em que a recomendacao faz sentido. Vazio = qualquer.
 * @param basePriority prioridade configurada no catalogo (0-100), usada como desempate de score.
 * @param cooldownHours intervalo minimo, em horas, entre duas exibicoes da mesma recomendacao.
 * @param maxPerDay / maxPerWeek limite de frequencia por janela de tempo.
 */
data class Recommendation(
    val id: String,
    val type: RecommendationType,
    val title: String,
    val tags: Set<DiagnosticTag> = emptySet(),
    val applicableNetworkTypes: Set<NetworkContextType> = emptySet(),
    val basePriority: Int = 50,
    val monetized: Boolean = type.monetized,
    val ruleOrigin: String = "local_fallback",
    val cooldownHours: Int = 0,
    val maxPerDay: Int = Int.MAX_VALUE,
    val maxPerWeek: Int = Int.MAX_VALUE,
)
