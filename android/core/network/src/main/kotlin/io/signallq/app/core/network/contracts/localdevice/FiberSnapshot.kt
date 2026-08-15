package io.signallq.app.core.network.contracts.localdevice

/**
 * Dados ópticos de fibra (GPON), presentes só em ONT (ex: Nokia). Roteadores
 * sem fibra (ex: TP-Link) nunca produzem esta seção — ver
 * [LocalNetworkDeviceSnapshot.fiber], que é nullable por contrato.
 *
 * Todos os campos são nullable individualmente: uma leitura parcial (ex.
 * temperatura indisponível numa tela específica do firmware) não deve
 * derrubar a seção inteira.
 */
data class FiberSnapshot(
    val linkAtivo: Boolean?,
    val rxPowerDbm: Double?,
    val txPowerDbm: Double?,
    val temperaturaCelsius: Double?,
    val tensaoV: Double?,
    val correnteLaserMa: Double?,
    val serialOnt: String?,
)
