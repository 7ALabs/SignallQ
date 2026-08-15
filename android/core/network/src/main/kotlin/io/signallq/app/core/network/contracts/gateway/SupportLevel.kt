package io.signallq.app.core.network.contracts.gateway

/**
 * Nivel de confianca/suporte do driver associado a um [DeviceType] — GH#545,
 * epic GH#547.
 *
 * IMPORTANTE: [LAB_VALIDATED] so pode ser atribuido a entradas do
 * [DeviceDriverCatalog] curadas manualmente apos validacao fisica do
 * equipamento (ver skill `reconhecimento-equipamento-rede`). O classificador
 * ([EquipmentClassifier]) nunca promove um driver para um nivel de suporte
 * maior do que o declarado no catalogo — score de evidencia decide **qual**
 * entrada casou, nunca **quao confiavel** ela e.
 */
enum class SupportLevel {
    /** Validado fisicamente em equipamento real pelo time SignallQ. */
    LAB_VALIDATED,

    /** Driver/parser importado de fonte externa (ex.: NetHAL), sem validacao fisica direta do SignallQ. */
    PARSER_IMPORTED,

    /** Inferido por familia/protocolo semelhante a um driver conhecido, sem parser dedicado. */
    INFERRED_FAMILY,

    /** Sem informacao de suporte — equipamento nao identificado ou fora do catalogo. */
    UNKNOWN,
}
