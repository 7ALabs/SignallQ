package io.signallq.app.core.network.contracts.localdevice

/**
 * Estado de um rádio Wi-Fi (2.4GHz/5GHz) reportado pelo próprio equipamento.
 *
 * Deliberadamente **não** modela senha/PSK — o field-map do TP-Link
 * (`wireless_2g_psk_key`) confirma que o firmware devolve a senha Wi-Fi em
 * texto plano, e este contrato nunca deve carregar esse campo até a UI/IA.
 */
data class WifiRadioSnapshot(
    val banda: String,
    val ssid: String?,
    val canal: Int?,
    val larguraCanal: String?,
    val potenciaTx: String?,
    val criptografia: String?,
    val habilitado: Boolean?,
)

data class WifiSnapshot(
    val radios: List<WifiRadioSnapshot> = emptyList(),
)
