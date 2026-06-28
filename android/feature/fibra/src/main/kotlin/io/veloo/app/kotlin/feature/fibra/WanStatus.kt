package io.signallq.app.feature.fibra

data class WanStatus(
    val externalIp: String,
    val gateway: String,
    val primaryDns: String,
    val secondaryDns: String,
    val vlanId: String,
    val interfaceName: String,
    val pppoeConcentrator: String,
    val connectionType: String,
    val connectionUptimeSeconds: Int,
) {
    fun formatarUptime(): String {
        if (connectionUptimeSeconds <= 0) return "—"
        val d = connectionUptimeSeconds / 86400
        val h = (connectionUptimeSeconds % 86400) / 3600
        val m = (connectionUptimeSeconds % 3600) / 60
        return when {
            d > 0 -> "${d}d ${h}h ${m}min"
            h > 0 -> "${h}h ${m}min"
            else -> "${m}min"
        }
    }
}
