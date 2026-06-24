package io.veloo.app.feature.wifi

object TopologiaWifiEngine {

    fun classificar(
        redes: List<RedeVizinha>,
        connectedBssid: String?,
        gatewayOui: String? = null
    ): List<RedeClassificada> {
        if (redes.isEmpty()) return emptyList()

        // Agrupar por SSID para análise de topologia
        val porSsid = redes.groupBy { it.ssid }

        return redes.map { rede ->
            val redesMesmoSsid = porSsid[rede.ssid] ?: listOf(rede)
            classificarRede(rede, redesMesmoSsid, connectedBssid, gatewayOui)
        }
    }

    private fun classificarRede(
        rede: RedeVizinha,
        redesMesmoSsid: List<RedeVizinha>,
        connectedBssid: String?,
        gatewayOui: String?
    ): RedeClassificada {
        val oui = rede.oui.uppercase()
        val isUnico = redesMesmoSsid.size == 1
        val ouisNoGrupo = redesMesmoSsid.map { it.oui.uppercase() }.toSet()
        val todosOuiIguais = ouisNoGrupo.size == 1

        // Caso 1: AP conhecido como mesh node
        if (MeshOuiDatabase.isMeshNo(oui)) {
            return if (isUnico || todosOuiIguais) {
                if (rede.bssid == connectedBssid || MeshOuiDatabase.isGatewayIsp(oui)) {
                    RedeClassificada(rede, TipoTopologia.ROTEADOR_MESH, ConfiancaTopologia.ALTA, "OUI de sistema mesh — nó principal provável")
                } else {
                    RedeClassificada(rede, TipoTopologia.NO_MESH, ConfiancaTopologia.ALTA, "OUI de sistema mesh")
                }
            } else {
                RedeClassificada(rede, TipoTopologia.NO_MESH, ConfiancaTopologia.ALTA, "OUI de sistema mesh — múltiplos nós detectados")
            }
        }

        // Caso 2: Gateway/roteador ISP
        if (MeshOuiDatabase.isGatewayIsp(oui)) {
            return RedeClassificada(rede, TipoTopologia.ROTEADOR, ConfiancaTopologia.ALTA, "OUI de gateway ISP brasileiro")
        }

        // Caso 3: Múltiplas redes com mesmo SSID
        if (redesMesmoSsid.size > 1) {
            return if (todosOuiIguais) {
                // Mesmo OUI → sistema mesh (marca própria)
                RedeClassificada(rede, TipoTopologia.NO_MESH, ConfiancaTopologia.MEDIA, "Múltiplos BSSIDs, mesmo OUI — sistema mesh provável")
            } else {
                // OUIs diferentes → repetidor
                // O mais forte (maior RSSI) é provavelmente o roteador principal
                val maisForte = redesMesmoSsid.maxByOrNull { it.rssiDbm }
                if (rede.bssid == maisForte?.bssid) {
                    RedeClassificada(rede, TipoTopologia.ROTEADOR, ConfiancaTopologia.MEDIA, "Sinal mais forte do grupo — roteador principal provável")
                } else {
                    RedeClassificada(rede, TipoTopologia.REPETIDOR, ConfiancaTopologia.MEDIA, "OUI diferente do principal — repetidor/extensor provável")
                }
            }
        }

        // Caso 4: Rede única — roteador padrão
        return RedeClassificada(rede, TipoTopologia.ROTEADOR, ConfiancaTopologia.BAIXA, "Rede única")
    }

    fun agrupar(redesClassificadas: List<RedeClassificada>): List<GrupoRedeWifi> {
        return redesClassificadas
            .groupBy { it.rede.ssid }
            .map { (ssid, redes) -> GrupoRedeWifi(ssid ?: "", redes.sortedByDescending { it.rede.rssiDbm }) }
            .sortedByDescending { it.redes.maxOfOrNull { r -> r.rede.rssiDbm } ?: Int.MIN_VALUE }
    }
}
