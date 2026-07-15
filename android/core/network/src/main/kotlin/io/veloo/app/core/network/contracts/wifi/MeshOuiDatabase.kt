package io.signallq.app.core.network.contracts.wifi

import io.signallq.app.core.network.contracts.topologia.PapelTopologia
import io.signallq.app.core.network.topologia.oui.OuiCatalog

/**
 * Wrapper fino sobre [OuiCatalog] — mantido para não quebrar call sites existentes
 * ([io.signallq.app.feature.wifi.TopologiaWifiEngine], [io.signallq.app.feature.diagnostico.RecommendationEngine]).
 *
 * Até a Fase 1 do épico #975 (issue #978) este objeto tinha duas listas paralelas
 * (`MESH_NO_OUIS`/`GATEWAY_ISP_OUIS`) com um conflito de curadoria não documentado: o OUI da
 * Intelbras (`C46E1F`/`6C5AB0`) estava cadastrado como mesh E gateway ISP simultaneamente. A
 * base real (fabricante + papéis possíveis de topologia) agora vive em [OuiCatalog], onde esse
 * mesmo OUI é um único registro com `papeisPossiveis = {ROTEADOR, NO_MESH}` — a decisão de qual
 * papel vale em cada contexto é do motor de topologia (Fase 2A), não deste wrapper.
 */
object MeshOuiDatabase {

    fun isMeshNo(oui: String): Boolean =
        OuiCatalog.lookup(oui)?.papeisPossiveis?.contains(PapelTopologia.NO_MESH) == true

    fun isGatewayIsp(oui: String): Boolean =
        OuiCatalog.lookup(oui)?.papeisPossiveis?.contains(PapelTopologia.ROTEADOR) == true
}
