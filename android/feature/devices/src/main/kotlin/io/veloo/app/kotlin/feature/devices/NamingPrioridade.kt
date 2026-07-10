package io.signallq.app.feature.devices

import io.signallq.app.core.network.contracts.localdevice.ClientSnapshot

/**
 * Lógica pura de prioridade/resolução de nome e fabricante de dispositivos de rede.
 *
 * Extraída do [ScannerDispositivosAndroid] para permitir testes unitários sem Context
 * e sem dependência de Android runtime.
 *
 * Pipeline de NOME (melhor → pior):
 *   ClientSnapshot.hostname (routerActive, ver [resolverNomeRouterActive]) > friendlyName
 *   SSDP(XML) > nome amigável mDNS TXT/instância (jmDNS) > hostname reverso > fallback
 *
 * O degrau `routerActive` é resolvido FORA de [resolverNome] — é um bypass por MAC
 * (issue #839), não um parâmetro a mais dessa função. Ele vem de outra categoria de
 * dado (o próprio equipamento confirmando o cliente conectado), não de inferência da
 * varredura passiva, por isso não compete com SSDP/mDNS/hostname reverso dentro da
 * mesma função: quando há match, ele já vence antes de [resolverNome] ser chamado.
 *
 * Pipeline de FABRICANTE (melhor → pior):
 *   manufacturer UPnP(XML) > fabricante mDNS TXT > OUI(MAC) > null
 *
 * Nomes "genéricos" (lista [NOMES_GENERICOS]) são ignorados e tratados como null
 * na resolução de prioridade.
 */
object NamingPrioridade {

    /**
     * Fonte para nome obtido por **leitura ativa** do gateway/roteador (epic #525,
     * SIG-358/359/360/361 — leitura ativa do gateway). Produzido por
     * [resolverNomeRouterActive] quando o MAC do dispositivo bate com um
     * [ClientSnapshot] reportado pelo próprio equipamento (issue #839). O valor
     * já era consumido pela UI (ícone/label/cor em `DispositivosScreen.kt`) desde
     * antes de ter produtor real — ver issue #532.
     */
    const val FONTE_NOME_ROUTER_ACTIVE = "routerActive"

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
     * Resolve nome via **leitura ativa do gateway** (issue #839) — bypass de [resolverNome],
     * não parte dele. Casa [macDispositivo] (normalizado) contra a lista de [ClientSnapshot]
     * reportada pelo próprio equipamento e retorna o hostname do roteador quando:
     *  1. Existe um [ClientSnapshot] com o mesmo MAC normalizado.
     *  2. O hostname desse cliente não é nulo, não é branco e não está em [NOMES_GENERICOS].
     *
     * Retorna `null` em qualquer outro caso (sem MAC, sem match, hostname ausente/genérico) —
     * o chamador deve manter o nome/fonte já resolvidos pela varredura passiva, sem
     * "meio-confirmado": ou o selo acende com nome real, ou não acende.
     *
     * [clientesGateway] já deve vir filtrada pelo chamador por `capabilities.suportaClientes`
     * (lista vazia quando não há leitura ativa disponível nesta sessão) — esta função não
     * conhece [io.signallq.app.core.network.contracts.localdevice.DeviceCapabilities].
     */
    fun resolverNomeRouterActive(
        macDispositivo: String?,
        clientesGateway: List<ClientSnapshot>,
    ): String? {
        val macNormalizado = normalizarMac(macDispositivo) ?: return null
        val cliente = clientesGateway.firstOrNull { normalizarMac(it.mac) == macNormalizado } ?: return null
        return cliente.hostname?.takeIf { it.isNotBlank() && it !in NOMES_GENERICOS }
    }

    /** Normaliza MAC para comparação: lowercase, sem separador (`:`/`-`). */
    private fun normalizarMac(mac: String?): String? =
        mac?.lowercase(java.util.Locale.ROOT)
            ?.replace(":", "")
            ?.replace("-", "")
            ?.takeIf { it.isNotBlank() }

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

    /**
     * Rótulo de fallback quando não há hostname/nome resolvido para o dispositivo.
     *
     * Usado como ÚLTIMO recurso, depois que mDNS/SSDP/reverse-DNS já tiveram chance
     * real de resolver o nome (ver [ScannerDispositivosAndroid.iniciarScan]). NÃO resolve
     * NetBIOS — pilha de protocolo à parte (NBNS/UDP porta 137), ausência total de
     * tentativa de implementação, não bug em código existente; ver nota no topo deste
     * arquivo e em [ScannerDispositivosAndroid] linha do enriquecimento final.
     *
     * Usa apenas o fabricante já inferido via OUI do MAC — quando disponível,
     * "Dispositivo <Fabricante>" (ex.: "Dispositivo Samsung"); sem fabricante,
     * "Dispositivo desconhecido" (issue #219 — usuário leigo precisa de um rótulo que
     * comunique "não identificado", não um "Dispositivo" seco que soa como nome próprio).
     */
    fun rotuloFallbackGenerico(fabricante: String?): String {
        val f = fabricante?.takeIf { it.isNotBlank() }
        return if (f != null) "Dispositivo $f" else "Dispositivo desconhecido"
    }

    /** [android.os.Build.MANUFACTURER] vem em lowercase (ex: "samsung") — capitaliza para exibição. */
    fun capitalizarFabricante(manufacturer: String?): String? =
        manufacturer?.trim()?.takeIf { it.isNotBlank() }
            ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString() }

    /**
     * Nome de exibição para o próprio aparelho ("Este aparelho"): "<Fabricante> <Modelo>"
     * quando ambos disponíveis (ex.: "Samsung SM-A256E"), caindo para o que estiver
     * disponível. Usado para não depender de descoberta de rede no próprio device —
     * o app já sabe quem ele é via [android.os.Build].
     *
     * @param modelo tipicamente [android.os.Build.MODEL]
     * @param fabricante tipicamente [capitalizarFabricante] de [android.os.Build.MANUFACTURER]
     */
    fun nomeAmigavelDoDevice(modelo: String?, fabricante: String?): String {
        val m = modelo?.trim()?.takeIf { it.isNotBlank() }
        val f = fabricante?.trim()?.takeIf { it.isNotBlank() }
        return when {
            f != null && m != null && !m.startsWith(f, ignoreCase = true) -> "$f $m"
            m != null -> m
            f != null -> f
            else -> "Este aparelho"
        }
    }
}
