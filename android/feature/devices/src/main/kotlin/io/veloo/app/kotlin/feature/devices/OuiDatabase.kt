package io.signallq.app.feature.devices

import io.signallq.app.core.network.topologia.oui.OuiCatalog

/**
 * Wrapper fino sobre [OuiCatalog] — mantido para não quebrar call sites existentes
 * ([ScannerDispositivosAndroid]). A base de dados real (~130 prefixos OUI curados manualmente,
 * fabricante + papéis possíveis de topologia) foi movida para
 * `:coreNetwork/topologia/oui/OuiCatalog.kt` na Fase 1 do épico #975 (issue #978), unificada com
 * o que antes era `MeshOuiDatabase` (coreNetwork).
 */
internal object OuiDatabase {
    fun lookupFabricante(mac: String?): String? = OuiCatalog.lookup(mac)?.fabricante
}
