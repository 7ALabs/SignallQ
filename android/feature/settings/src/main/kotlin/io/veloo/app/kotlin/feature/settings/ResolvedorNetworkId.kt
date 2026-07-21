package io.signallq.app.feature.settings

/**
 * GH#1227 item 3/RF-A — deriva um [ConnectionProfile.networkId] estável a partir dos sinais de
 * rede disponíveis. Função pura (sem Android/Hilt) pra permitir teste unitário isolado — quem
 * chama já resolveu SSID/BSSID via `WifiManager`/`ConnectivityManager` (feature/settings não
 * depende de coreNetwork por enquanto; se essa necessidade crescer, promover pra core comum).
 *
 * Prioridade de estabilidade (do mais pro menos estável):
 * 1. BSSID (MAC do ponto de acesso) — não muda entre reconexões na mesma rede física, mesmo
 *    que o SSID seja alterado pelo dono do roteador.
 * 2. SSID sozinho — usado só quando BSSID não está disponível (Android 8+ exige permissão de
 *    localização pra ler BSSID real; sem ela, o SO devolve valor placeholder). Menos estável
 *    (nomes de rede podem se repetir, e trocar o nome do roteador gera um id novo), mas ainda
 *    assim é vinculado à rede — nunca cai pra um id global.
 * 3. Rede móvel — usa o identificador de operadora/SIM (não há SSID/BSSID em rede móvel).
 *
 * Retorna `null` quando não há nenhum sinal estável disponível (ex.: Ethernet sem identificação
 * adicional) — o chamador decide o fallback (ex.: não persistir perfil, ou usar um id fixo
 * documentado como "sem-rede-identificada", nunca reaproveitar outro networkId por engano).
 */
object ResolvedorNetworkId {
    private const val PREFIXO_WIFI_BSSID = "wifi-bssid:"
    private const val PREFIXO_WIFI_SSID = "wifi-ssid:"
    private const val PREFIXO_MOVEL = "movel:"

    fun paraWifi(
        ssid: String?,
        bssid: String?,
    ): String? {
        val bssidValido = bssid?.trim()?.takeIf { it.isNotBlank() && it != "02:00:00:00:00:00" }
        if (bssidValido != null) return "$PREFIXO_WIFI_BSSID${bssidValido.lowercase()}"
        val ssidValido = ssid?.trim()?.takeIf { it.isNotBlank() }
        return ssidValido?.let { "$PREFIXO_WIFI_SSID$it" }
    }

    fun paraRedeMovel(operadoraOuIccid: String?): String? =
        operadoraOuIccid?.trim()?.takeIf { it.isNotBlank() }?.let { "$PREFIXO_MOVEL$it" }
}
