package io.signallq.app.core.network.contracts.gateway

/**
 * Classifica o equipamento local (gateway) em [DeviceType]/[SupportLevel] a
 * partir de evidencia de fingerprint passivo — GH#545, epic GH#547.
 *
 * ## Score de evidencia
 * Cada entrada do catalogo e pontuada contra a evidencia recebida:
 * - Header/banner (`httpBanner` casando `bannerPatterns`): **+0,25**
 * - IP canonico do gateway (`gatewayIp` em `canonicalGatewayIps`): **+0,20**
 * - Presenca no catalogo — marca/modelo (`vendorHint`/`modelHint` casando
 *   `modelPatterns`) OU rota HTTP conhecida (`knownRoutesDetected` casando
 *   `routeSignatures`): **+0,10**
 * - Piso minimo de **0,10** para qualquer entrada com pelo menos um sinal.
 *
 * A entrada com maior score vence; em empate, a ordem do catalogo decide
 * (entradas especificas/`LAB_VALIDATED` sao listadas antes de entradas
 * genericas em [DeviceDriverCatalog]).
 *
 * ## Por que SupportLevel nunca e promovido aqui
 * O score decide **qual** driver casou, nunca **quao confiavel** ele e — o
 * [SupportLevel] retornado e sempre o valor ja declarado (e curado
 * manualmente) na entrada do catalogo. Isso e o que garante o criterio de
 * aceite "parser externo nao validado fisicamente nao pode virar
 * LAB_VALIDATED": so entra como LAB_VALIDATED o que ja nasceu assim no
 * catalogo.
 *
 * ## Fallback seguro
 * Equipamento sem casamento no catalogo nunca lanca excecao — vira
 * [DeviceType.UNKNOWN_SUPPORTED] (se houve algum sinal de superficie
 * administrativa) ou [DeviceType.UNKNOWN_UNSUPPORTED] (nenhum sinal), sempre
 * com [SupportLevel.UNKNOWN] e sem capability de fibra.
 */
object EquipmentClassifier {

    private const val PESO_BANNER = 0.25
    private const val PESO_IP_CANONICO = 0.20
    private const val PESO_PRESENCA_CATALOGO = 0.10
    private const val PISO_MINIMO = 0.10

    fun classificar(
        evidence: EquipmentFingerprintEvidence,
        catalog: List<DeviceDriverProfile> = DeviceDriverCatalog.entries,
    ): EquipmentClassification = try {
        val melhorCasamento = catalog
            .map { perfil -> perfil to scoreParaPerfil(evidence, perfil) }
            .filter { (_, score) -> score > 0.0 }
            .maxByOrNull { (_, score) -> score }

        if (melhorCasamento == null) {
            classificacaoDesconhecida(evidence)
        } else {
            val (perfil, score) = melhorCasamento
            EquipmentClassification(
                deviceType = perfil.deviceType,
                supportLevel = perfil.supportLevel,
                driverId = perfil.driverId,
                // Defesa em profundidade: so ONT_GPON pode carregar capability de fibra,
                // mesmo que uma entrada futura do catalogo declare fibraCapable errado.
                fibraCapable = perfil.deviceType == DeviceType.ONT_GPON && perfil.fibraCapable,
                confidenceScore = score,
            )
        }
    } catch (_: Throwable) {
        classificacaoDesconhecida(evidence)
    }

    private fun classificacaoDesconhecida(evidence: EquipmentFingerprintEvidence): EquipmentClassification {
        val houveSinalDeSuperficieAdministrativa =
            !evidence.httpBanner.isNullOrBlank() ||
                evidence.knownRoutesDetected.isNotEmpty() ||
                !evidence.loginResponseMarker.isNullOrBlank()

        val deviceType = if (houveSinalDeSuperficieAdministrativa) {
            DeviceType.UNKNOWN_SUPPORTED
        } else {
            DeviceType.UNKNOWN_UNSUPPORTED
        }

        return EquipmentClassification(
            deviceType = deviceType,
            supportLevel = SupportLevel.UNKNOWN,
            driverId = null,
            fibraCapable = false,
            confidenceScore = 0.0,
        )
    }

    private fun scoreParaPerfil(evidence: EquipmentFingerprintEvidence, perfil: DeviceDriverProfile): Double {
        val casouBanner = perfil.bannerPatterns.isNotEmpty() &&
            contemIgnorandoCaixa(evidence.httpBanner, perfil.bannerPatterns)

        val casouIp = evidence.gatewayIp != null && perfil.canonicalGatewayIps.contains(evidence.gatewayIp)

        val casouModelo = perfil.modelPatterns.isNotEmpty() &&
            (contemIgnorandoCaixa(evidence.modelHint, perfil.modelPatterns) || contemIgnorandoCaixa(evidence.vendorHint, perfil.modelPatterns))

        val casouRota = perfil.routeSignatures.isNotEmpty() &&
            evidence.knownRoutesDetected.any { detectada -> perfil.routeSignatures.any { assinatura -> detectada.contains(assinatura, ignoreCase = true) } }

        var score = 0.0
        if (casouBanner) score += PESO_BANNER
        if (casouIp) score += PESO_IP_CANONICO
        if (casouModelo || casouRota) score += PESO_PRESENCA_CATALOGO

        return if (score > 0.0) maxOf(score, PISO_MINIMO) else 0.0
    }

    private fun contemIgnorandoCaixa(valor: String?, padroes: List<String>): Boolean {
        if (valor.isNullOrBlank()) return false
        return padroes.any { padrao -> valor.contains(padrao, ignoreCase = true) }
    }
}
