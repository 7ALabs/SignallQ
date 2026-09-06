package io.signallq.app.core.network.contracts.localdevice

/**
 * Nível de confiança/maturidade do driver que produziu o
 * [LocalNetworkDeviceSnapshot] — usado pela UI para decidir se mostra o dado
 * como definitivo ou como "experimental" (GH#538/#539/#540).
 */
enum class SupportLevel {
    /** Validado fisicamente em laboratório pelo time SignallQ. */
    LAB_VALIDATED,

    /** Importado de parser externo (ex: NetHAL) sem validação física própria. */
    PARSER_IMPORTED,

    /** Inferido por semelhança de família de firmware/protocolo, não testado. */
    INFERRED_FAMILY,

    /** Nenhum driver reconhece o equipamento. */
    UNKNOWN,
}
