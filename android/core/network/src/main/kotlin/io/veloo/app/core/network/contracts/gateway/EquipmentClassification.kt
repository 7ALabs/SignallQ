package io.signallq.app.core.network.contracts.gateway

/**
 * Resultado da classificacao de um equipamento local — GH#545.
 *
 * [fibraCapable] e a unica capability exposta nesta issue (as demais
 * capabilities por equipamento sao escopo do contrato normalizado
 * `LocalNetworkDeviceSnapshot`/`DeviceCapabilities`, GH#546).
 */
data class EquipmentClassification(
    val deviceType: DeviceType,
    val supportLevel: SupportLevel,
    /** Id do driver do catalogo que casou com a evidencia, ou null se nao houve casamento (`UNKNOWN_*`). */
    val driverId: String?,
    /** True somente quando [deviceType] == [DeviceType.ONT_GPON] e o driver declara suporte a fibra. */
    val fibraCapable: Boolean,
    /** Score de confianca da evidencia usada para decidir o casamento, entre 0.0 e 1.0. */
    val confidenceScore: Double,
)
