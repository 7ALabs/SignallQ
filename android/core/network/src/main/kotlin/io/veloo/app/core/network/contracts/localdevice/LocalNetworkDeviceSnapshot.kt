package io.signallq.app.core.network.contracts.localdevice

/**
 * Contrato normalizado de leitura de equipamento de rede local (ONT/roteador)
 * — permite ao SignallQ consumir dados de Nokia GPON, TP-Link Archer e
 * futuros parsers externos sem acoplar UI/motor de diagnóstico ao driver
 * específico (GH#546, epic #547).
 *
 * Regras obrigatórias deste contrato:
 * - [fiber] é nullable — TP-Link e outros roteadores sem fibra produzem
 *   `fiber = null`, nunca uma seção "vazia" fingindo ter fibra.
 * - A UI deve decidir o que mostrar a partir de [capabilities], nunca
 *   checando [vendor] ou [modelo] diretamente.
 * - Dado ausente é modelado como ausência (campo nullable / lista vazia) ou
 *   [warnings] explícito — nunca deve chegar como exception genérica na UI.
 * - Nenhum campo aqui carrega senha, cookie ou token de sessão. Isso é
 *   reforçado pelos modelos de cada seção (nenhum deles tem campo de
 *   credencial).
 * - Este snapshot completo NUNCA deve ser enviado direto para IA, analytics
 *   ou logs técnicos — ele ainda carrega MAC/IP completos e a lista crua de
 *   [clientes]. Use sempre [LocalDeviceSafeFilter.filtrar] para obter o
 *   payload allowlisted ([SafeLocalDeviceContext]), único formato permitido
 *   nesses destinos (GH#541).
 */
data class LocalNetworkDeviceSnapshot(
    val deviceType: DeviceType,
    val supportLevel: SupportLevel,
    val capabilities: DeviceCapabilities,
    val vendor: String?,
    val modelo: String?,
    val firmwareVersion: String?,
    val fiber: FiberSnapshot?,
    val wan: WanSnapshot?,
    val wifi: WifiSnapshot?,
    val lan: LanSnapshot?,
    val clientes: List<ClientSnapshot> = emptyList(),
    val warnings: List<DeviceWarning> = emptyList(),
    val freshness: DataFreshness,
)
