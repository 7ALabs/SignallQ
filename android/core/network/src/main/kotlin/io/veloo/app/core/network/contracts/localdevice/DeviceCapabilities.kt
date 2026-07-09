package io.signallq.app.core.network.contracts.localdevice

/**
 * Capacidades declaradas pelo driver do equipamento — a UI deve se adaptar a
 * partir daqui (GH#544), nunca checando `vendor`/`modelo` na tela.
 *
 * Cada flag indica se a seção correspondente do [LocalNetworkDeviceSnapshot]
 * pode vir preenchida para este equipamento. Uma flag `true` não garante que
 * o campo veio preenchido nesta leitura específica (login pode ter falhado
 * parcialmente) — só que o driver é capaz de produzir esse dado quando a
 * leitura funciona. Dado ausente numa leitura que deveria suportar a seção
 * vira [DeviceWarning], nunca erro genérico.
 */
data class DeviceCapabilities(
    val suportaFibra: Boolean = false,
    val suportaWan: Boolean = false,
    val suportaWifi: Boolean = false,
    val suportaLan: Boolean = false,
    val suportaClientes: Boolean = false,
    val suportaDiagnosticoNativo: Boolean = false,
)
