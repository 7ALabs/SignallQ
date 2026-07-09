package io.signallq.app.ui

import io.signallq.app.core.network.contracts.wifi.RedeVizinha

/**
 * GH#531 — resume as bandas Wi-Fi (2,4G/5G/6G) do gateway conectado, casando
 * pelo SSID da rede atual dentro do scan de redes vizinhas já coletado pelo
 * app (ScannerRedesWifi). Um roteador dual/tri-band que reusa o mesmo SSID em
 * cada rádio aparece aqui como múltiplas entradas com o mesmo `ssid` e bandas
 * distintas — mesma premissa usada por TopologiaWifiEngine para detectar
 * "mesmo AP em bandas distintas".
 *
 * Não depende de scraping da interface admin do roteador (GH#527/#530, ainda
 * inexistente) — usa só o que o próprio Android já enxerga no scan de vizinhança.
 * Retorna null quando não há SSID conectado ou nenhuma banda foi encontrada.
 */
fun resumoBandasWifi(
    redes: List<RedeVizinha>,
    ssidConectado: String?,
): String? {
    if (ssidConectado.isNullOrBlank()) return null
    val bandasDetectadas = redes.filter { it.ssid == ssidConectado }.map { it.banda }.toSet()
    if (bandasDetectadas.isEmpty()) return null
    val ordemLabel = linkedMapOf("2.4GHz" to "2,4G", "5GHz" to "5G", "6GHz" to "6G")
    val labels = ordemLabel.filterKeys { it in bandasDetectadas }.values
    return labels.takeIf { it.isNotEmpty() }?.joinToString(" + ")
}
