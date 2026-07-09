package io.signallq.app.core.recommendation.catalog

import io.signallq.app.core.recommendation.DiagnosticTag
import io.signallq.app.core.recommendation.NetworkContextType
import io.signallq.app.core.recommendation.Recommendation
import io.signallq.app.core.recommendation.RecommendationType

/**
 * Catalogo minimo embarcado no app, usado quando o catalogo remoto ainda nao existe ou
 * esta indisponivel. Cobre os tipos de recomendacao previstos na issue #790 com um exemplo
 * por categoria -- suficiente para o engine funcionar fim a fim sem depender de rede.
 */
class LocalRecommendationCatalog : RecommendationCatalog {

    override fun all(): List<Recommendation> = ITEMS

    companion object {
        private val ITEMS = listOf(
            Recommendation(
                id = "free_tip_reposicionar_roteador",
                type = RecommendationType.FREE_TIP,
                title = "Reposicione seu roteador",
                tags = setOf(DiagnosticTag.WIFI_FRACO),
                applicableNetworkTypes = setOf(NetworkContextType.WIFI),
                basePriority = 70,
            ),
            Recommendation(
                id = "configuration_trocar_dns",
                type = RecommendationType.CONFIGURATION,
                title = "Troque o DNS da sua rede",
                tags = setOf(DiagnosticTag.DNS_LENTO),
                basePriority = 65,
            ),
            Recommendation(
                id = "tutorial_reduzir_bufferbloat",
                type = RecommendationType.TUTORIAL,
                title = "Como reduzir o bufferbloat ativando QoS no roteador",
                tags = setOf(DiagnosticTag.BUFFERBLOAT_ALTO),
                basePriority = 60,
            ),
            Recommendation(
                id = "affiliate_repetidor_wifi",
                type = RecommendationType.AFFILIATE_PRODUCT,
                title = "Repetidor Wi-Fi recomendado para o seu caso",
                tags = setOf(DiagnosticTag.SINAL_BAIXO),
                applicableNetworkTypes = setOf(NetworkContextType.WIFI),
                basePriority = 40,
                cooldownHours = 72,
                maxPerWeek = 2,
            ),
            Recommendation(
                id = "partner_offer_upgrade_roteador",
                type = RecommendationType.PARTNER_OFFER,
                title = "Roteador parceiro com melhor cobertura para sua casa",
                tags = setOf(DiagnosticTag.MUITOS_DISPOSITIVOS),
                applicableNetworkTypes = setOf(NetworkContextType.WIFI),
                basePriority = 45,
                cooldownHours = 72,
                maxPerWeek = 2,
            ),
            Recommendation(
                id = "operator_offer_upgrade_plano",
                type = RecommendationType.OPERATOR_OFFER,
                title = "Planos com mais velocidade disponiveis para o seu endereco",
                tags = setOf(DiagnosticTag.VELOCIDADE_ABAIXO_DO_CONTRATADO),
                basePriority = 50,
                cooldownHours = 168,
                maxPerWeek = 1,
            ),
            Recommendation(
                id = "native_ad_fallback_default",
                type = RecommendationType.NATIVE_AD_FALLBACK,
                title = "Anuncio nativo (fallback)",
                tags = emptySet(),
                basePriority = 0,
                cooldownHours = 4,
            ),
        )
    }
}
