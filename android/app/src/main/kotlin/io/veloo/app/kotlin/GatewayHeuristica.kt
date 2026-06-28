package io.signallq.app

import io.signallq.app.feature.wifi.RedeVizinha
import io.signallq.app.ui.ConnectionNodeType

// Tokens exatos para mesh — suficientemente específicos para match por substring simples.
private val SSID_MESH_EXATOS = listOf("MESH", "DECO", "EERO", "VELOP")

// "ORBI" é específico o suficiente, mas "ORBISAT" existe. Exigimos que seja token inicial.
private val SSID_MESH_PREFIXO = listOf("ORBI")

// Tokens de extensor que precisam ser palavras isoladas para evitar falsos positivos
// (ex: "ORANGE" → contém "RANGE"; "ORANGE" → contém "EXT" não, mas "RANGE" sim).
private val SSID_EXTENSOR_EXATOS = listOf("EXTENSOR", "REPEATER")
private val SSID_EXTENSOR_TOKEN = listOf("EXT", "RANGE")

// Threshold mínimo inclusivo: RSSI >= -75 dBm é aceito.
internal const val RSSI_MESH_MINIMO = -75

/** Separa o SSID em tokens alfanuméricos (qualquer não-alfanumérico é delimitador). */
private fun tokenize(upper: String): Set<String> = upper.split(Regex("[^A-Z0-9]")).filter { it.isNotEmpty() }.toSet()

fun inferirTipoGatewayPorScan(
    ssid: String,
    redesVizinhas: List<RedeVizinha>,
): ConnectionNodeType {
    if (ssid.isEmpty()) return ConnectionNodeType.WifiRouter

    val upper = ssid.uppercase()
    val tokens = tokenize(upper)

    // Mesh por substring exata (termos muito específicos e raramente em SSIDs comuns)
    if (SSID_MESH_EXATOS.any { upper.contains(it) }) return ConnectionNodeType.WifiMesh

    // "ORBI" é específico o suficiente como token isolado, mas "ORBISAT" não deve dar match.
    // Verificamos se algum token é exatamente "ORBI" (não apenas um prefixo de outro token).
    if (SSID_MESH_PREFIXO.any { tokens.contains(it) }) return ConnectionNodeType.WifiMesh

    // Extensor por substring exata (termos longos e específicos)
    if (SSID_EXTENSOR_EXATOS.any { upper.contains(it) }) return ConnectionNodeType.WifiExtender

    // Extensor por token isolado (EXT, RANGE — evita ORANGE, GRANGE, EXTERIOR etc.)
    if (SSID_EXTENSOR_TOKEN.any { tokens.contains(it) }) return ConnectionNodeType.WifiExtender

    val ssidNorm = ssid.trim().lowercase()
    val bssidsComMesmoSsid =
        redesVizinhas
            .filter { it.ssid?.trim()?.lowercase() == ssidNorm && ssidNorm.isNotEmpty() }
            .filter { it.rssiDbm >= RSSI_MESH_MINIMO }
            .map { it.bssid }
            .distinct()

    if (bssidsComMesmoSsid.size >= 2) return ConnectionNodeType.WifiMesh

    return ConnectionNodeType.WifiRouter
}
