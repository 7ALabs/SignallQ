package io.signallq.app.core.network.contracts.gateway

/**
 * Entrada de exibicao publica de compatibilidade — apenas o que interessa ao
 * usuario final (vendor/modelo/tipo). Os detalhes de fingerprint do
 * [DeviceDriverProfile] (patterns/rotas/IPs canonicos) sao implementacao
 * interna, nunca texto de UI/documentacao.
 */
data class PublicCompatibilityEntry(
    val driverId: String,
    val vendor: String,
    val modelo: String,
    val deviceType: DeviceType,
)

/**
 * Lista publica de compatibilidade de equipamentos — GH#539, epic #547.
 *
 * Regra de produto: qualquer superficie que exponha "quais equipamentos o
 * SignallQ suporta" (tela de compatibilidade, documentacao publica, changelog
 * de release) NUNCA pode apresentar equipamento validado fisicamente
 * ([SupportLevel.LAB_VALIDATED]) misturado com equipamento importado ou
 * inferido ([SupportLevel.PARSER_IMPORTED] / [SupportLevel.INFERRED_FAMILY])
 * na mesma colecao. Por isso este catalogo expoe duas listas
 * estruturalmente separadas ([validado] e [experimental]) em vez de uma
 * lista unica achatada com uma flag — um consumidor que so ler [validado]
 * fisicamente nao consegue enxergar entradas experimentais.
 *
 * [SupportLevel.UNKNOWN] nunca aparece aqui: nao e uma entrada curada do
 * catalogo, e sim o fallback do [EquipmentClassifier] para equipamento sem
 * driver — nao faz sentido em uma lista de compatibilidade.
 */
data class PublicCompatibilityCatalog(
    val validado: List<PublicCompatibilityEntry>,
    val experimental: List<PublicCompatibilityEntry>,
) {
    companion object {
        fun montar(catalog: List<DeviceDriverProfile> = DeviceDriverCatalog.entries): PublicCompatibilityCatalog {
            val porSuporte = catalog.groupBy { it.supportLevel }

            val validado = (porSuporte[SupportLevel.LAB_VALIDATED] ?: emptyList())
                .paraEntradasPublicas()

            val experimental = (
                (porSuporte[SupportLevel.PARSER_IMPORTED] ?: emptyList()) +
                    (porSuporte[SupportLevel.INFERRED_FAMILY] ?: emptyList())
                ).paraEntradasPublicas()

            return PublicCompatibilityCatalog(validado = validado, experimental = experimental)
        }

        private fun List<DeviceDriverProfile>.paraEntradasPublicas(): List<PublicCompatibilityEntry> =
            map { perfil ->
                PublicCompatibilityEntry(
                    driverId = perfil.driverId,
                    vendor = perfil.vendor,
                    modelo = perfil.displayModel ?: perfil.driverId,
                    deviceType = perfil.deviceType,
                )
            }
    }
}
