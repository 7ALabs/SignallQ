package io.signallq.app.feature.diagnostico.topology.lan

import io.signallq.app.core.diagnostico.topology.model.DeviceInfo
import io.signallq.app.core.network.contracts.wifi.RedeVizinha

class MeshDetector(private val ouiLookup: OuiVendorLookup) {

    // Retorna lista de satélites mesh identificados (sem o nó gateway)
    fun detectSatellites(
        networks: List<RedeVizinha>,
        gatewayMac: String?
    ): List<DeviceInfo> {
        // Agrupa por SSID; candidatos a mesh = mesmo SSID + múltiplos BSSIDs + mesmo prefixo OUI
        val groups = networks.filter { it.ssid != null }
            .groupBy { it.ssid }

        return groups.flatMap { (_, nets) ->
            if (nets.size < 2) return@flatMap emptyList()
            val ouis = nets.map { it.oui.uppercase() }.distinct()
            // Mesh heurística: todos os nós têm o mesmo OUI (mesma marca)
            if (ouis.size != 1) return@flatMap emptyList()
            // Exclui o nó que é o gateway
            nets.filter { net ->
                gatewayMac == null || !net.bssid.equals(gatewayMac, ignoreCase = true)
            }.map { net ->
                DeviceInfo(
                    ip = null,
                    mac = net.bssid,
                    vendor = ouiLookup.lookup(net.bssid),
                    friendlyName = null,
                    manufacturer = null,
                    model = null
                )
            }
        }.distinctBy { it.mac }
    }
}
