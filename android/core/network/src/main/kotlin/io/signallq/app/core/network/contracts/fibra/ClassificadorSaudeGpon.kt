package io.signallq.app.core.network.contracts.fibra

// Limiares ITU-T G.984 / IEEE 802.3ah
object ClassificadorSaudeGpon {

    fun classificar(isUp: Boolean, rxPowerDbm: Double, txPowerDbm: Double, temperatureCelsius: Double): GponSaudeStatus {
        if (!isUp) return GponSaudeStatus.ruim
        if (rxPowerDbm == 0.0 && txPowerDbm == 0.0 && temperatureCelsius == 0.0) {
            return GponSaudeStatus.ruim
        }
        val pior = maxOf(
            classificarRx(rxPowerDbm),
            classificarTx(txPowerDbm),
            classificarTemp(temperatureCelsius),
        )
        return pior
    }

    // RX Power: boa >= -23 dBm | regular [-27, -23) | ruim < -27 dBm
    fun classificarRx(rxDbm: Double): GponSaudeStatus = when {
        rxDbm == 0.0 -> GponSaudeStatus.ruim
        rxDbm >= -23.0 -> GponSaudeStatus.boa
        rxDbm >= -27.0 -> GponSaudeStatus.regular
        else -> GponSaudeStatus.ruim
    }

    // TX Power: boa [+0.5, +5] dBm | regular [-1, +0.5) | ruim < -1 dBm
    fun classificarTx(txDbm: Double): GponSaudeStatus = when {
        txDbm == 0.0 -> GponSaudeStatus.ruim
        txDbm in 0.5..5.0 -> GponSaudeStatus.boa
        txDbm >= -1.0 -> GponSaudeStatus.regular
        else -> GponSaudeStatus.ruim
    }

    // Temperatura: boa < 65 C | regular [65, 75] | ruim > 75 C
    fun classificarTemp(tempC: Double): GponSaudeStatus = when {
        tempC == 0.0 -> GponSaudeStatus.ruim
        tempC < 65.0 -> GponSaudeStatus.boa
        tempC <= 75.0 -> GponSaudeStatus.regular
        else -> GponSaudeStatus.ruim
    }
}

// Compara status pela severidade (ruim > regular > boa) para calcular o pior caso.
private fun maxOf(vararg statuses: GponSaudeStatus): GponSaudeStatus {
    val ordem = listOf(GponSaudeStatus.boa, GponSaudeStatus.regular, GponSaudeStatus.ruim)
    return statuses.maxByOrNull { ordem.indexOf(it) } ?: GponSaudeStatus.ruim
}
