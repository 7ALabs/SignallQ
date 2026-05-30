package io.veloo.app.feature.fibra

data class DeviceInfoFibra(
    val model: String,
    val manufacturer: String,
    val serialNumber: String,
    val firmwareVersion: String,
    val hardwareVersion: String,
    val uptimeSeconds: Int,
) {
    fun formatarUptime(): String {
        if (uptimeSeconds <= 0) return "—"
        val d = uptimeSeconds / 86400
        val h = (uptimeSeconds % 86400) / 3600
        val m = (uptimeSeconds % 3600) / 60
        return when {
            d > 0 -> "${d}d ${h.toString().padStart(2, '0')}h"
            h > 0 -> "${h}h ${m.toString().padStart(2, '0')}min"
            m > 0 -> "${m}min"
            else -> "${uptimeSeconds}s"
        }
    }
}
