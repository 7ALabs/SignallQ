package io.signallq.app.feature.devices

/**
 * Lógica pura de prioridade/resolução de nome e fabricante de dispositivos de rede.
 *
 * Extraída do [ScannerDispositivosAndroid] para permitir testes unitários sem Context
 * e sem dependência de Android runtime.
 *
 * Pipeline de NOME (melhor → pior):
 *   friendlyName SSDP(XML) > nome amigável mDNS TXT/instância (jmDNS) > hostname reverso > fallback
 *
 * Pipeline de FABRICANTE (melhor → pior):
 *   manufacturer UPnP(XML) > fabricante mDNS TXT > OUI(MAC) > null
 *
 * Nomes "genéricos" (lista [NOMES_GENERICOS]) são ignorados e tratados como null
 * na resolução de prioridade.
 */
internal object NamingPrioridade {

    /** Nomes que não carregam informação útil — tratados como ausentes na priorização. */
    val NOMES_GENERICOS = setOf(
        "Dispositivo não identificado",
        "Host ativo",
        "Serviço mDNS",
        "Dispositivo SSDP",
    )

    /**
     * Resolve o melhor nome de exibição disponível para um dispositivo.
     *
     * @param nomeSsdpXml friendlyName extraído do XML UPnP (fonte mais confiável para smart home)
     * @param nomeMdns nome de instância ou TXT fn/name do jmDNS
     * @param nomeHostname hostname reverso do DNS
     * @param fallback nome de último recurso (ex: IP ou "Host ativo")
     */
    fun resolverNome(
        nomeSsdpXml: String?,
        nomeMdns: String?,
        nomeHostname: String?,
        fallback: String = "Host ativo",
    ): String {
        val ssdp = nomeSsdpXml?.takeIf { it.isNotBlank() && it !in NOMES_GENERICOS }
        if (ssdp != null) return ssdp

        val mdns = nomeMdns?.takeIf { it.isNotBlank() && it !in NOMES_GENERICOS }
        if (mdns != null) return mdns

        val hostname = nomeHostname?.takeIf { it.isNotBlank() && it !in NOMES_GENERICOS }
        if (hostname != null) return hostname

        return fallback
    }

    /**
     * Resolve o melhor fabricante disponível para um dispositivo.
     *
     * @param fabricanteUpnpXml manufacturer extraído do XML UPnP — mais específico
     * @param fabricanteMdns fabricante extraído de TXT records mDNS (mf= ou manufacturer=)
     * @param fabricanteOui fabricante inferido do OUI do MAC address
     */
    fun resolverFabricante(
        fabricanteUpnpXml: String?,
        fabricanteMdns: String?,
        fabricanteOui: String?,
    ): String? {
        return fabricanteUpnpXml?.takeIf { it.isNotBlank() }
            ?: fabricanteMdns?.takeIf { it.isNotBlank() }
            ?: fabricanteOui?.takeIf { it.isNotBlank() }
    }
}
