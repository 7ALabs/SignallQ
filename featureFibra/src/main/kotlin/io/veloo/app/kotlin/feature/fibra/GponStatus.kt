package io.veloo.app.feature.fibra

data class GponStatus(
    val status: String,
    val mode: String,
    val rxPowerDbm: Double,
    val txPowerDbm: Double,
    val temperatureCelsius: Double,
    val serial: String,
    val voltageV: Double,
    val laserCurrentMa: Double,
) {
    val isUp: Boolean get() = status.lowercase() == "up"
}
