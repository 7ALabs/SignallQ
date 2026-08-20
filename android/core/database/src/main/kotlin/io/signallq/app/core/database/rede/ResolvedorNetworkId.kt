package io.signallq.app.core.database.rede

/**
 * GH#1227 item 3/RF-A — deriva um `ConnectionProfile.networkId` estável a partir dos sinais de
 * rede disponíveis. Função pura (sem Android/Hilt) pra permitir teste unitário isolado — quem
 * chama já resolveu SSID/BSSID via `WifiManager`/`ConnectivityManager`.
 *
 * Promovido de `feature/settings` pra `core/database` na issue #1707 (Task 2.0.09e, épico #1647):
 * `MedicaoEntity.networkId` (comparação de reteste, spec §8.8) precisa do mesmo resolvedor que
 * `ConnectionProfile.networkId` (issue #1227) já usava, e `core/database` é o módulo comum que
 * tanto `feature/settings` quanto `feature/history` (via `:coreDatabase`) já alcançam — evita um
 * segundo resolvedor concorrente com regra própria (ver `.claude/rules/higiene-e-padronizacao-
 * repositorio.md`, §4.9).
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
