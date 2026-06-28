package io.signallq.app.feature.diagnostico.topology.correlation

import io.signallq.app.feature.diagnostico.topology.model.NatStatus

object NatClassifier {

    fun isPrivate(ip: String): Boolean {
        // RFC1918: 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
        val octets = parseIpv4(ip) ?: return false
        return when {
            octets[0] == 10 -> true
            octets[0] == 172 && octets[1] in 16..31 -> true
            octets[0] == 192 && octets[1] == 168 -> true
            else -> false
        }
    }

    fun isCgnatRange(ip: String): Boolean {
        // RFC6598: 100.64.0.0/10
        val octets = parseIpv4(ip) ?: return false
        return octets[0] == 100 && octets[1] in 64..127
    }

    fun classify(wanIp: String?, publicIp: String?): NatStatus {
        if (wanIp == null) return NatStatus.UNKNOWN
        return when {
            isCgnatRange(wanIp) -> NatStatus.CGNAT
            isPrivate(wanIp) -> NatStatus.DOUBLE_NAT_OR_CGNAT
            publicIp != null && wanIp != publicIp -> NatStatus.DOUBLE_NAT_OR_CGNAT
            else -> NatStatus.DIRECT_PUBLIC
        }
    }

    private fun parseIpv4(ip: String): IntArray? {
        val parts = ip.trim().split(".")
        if (parts.size != 4) return null
        return try {
            IntArray(4) { parts[it].toInt().also { v -> require(v in 0..255) } }
        } catch (_: Exception) { null }
    }
}
