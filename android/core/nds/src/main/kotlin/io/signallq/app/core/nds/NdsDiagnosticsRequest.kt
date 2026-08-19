package io.signallq.app.core.nds

import org.json.JSONArray
import org.json.JSONObject

data class NdsAppInfo(
    val id: String,
    val version: String,
)

data class NdsConnectionInfo(
    /** Ex.: "WIFI", "MOBILE". Vocabulario do NDS — string livre, nao enum, para
     *  nao travar o cliente quando o servidor aceitar um valor novo. */
    val type: String,
    val ssid: String? = null,
    val bssid: String? = null,
)

data class NdsWifiInfo(
    val rssi: Int? = null,
    val band: String? = null,
    val channel: Int? = null,
    val linkSpeed: Int? = null,
    val standard: String? = null,
)

data class NdsWifiScanInfo(
    val channelCongestion: Int? = null,
    val bestChannel: Int? = null,
)

data class NdsSpeedInfo(
    val pingMs: Double? = null,
    val jitterMs: Double? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val packetLossPercent: Double? = null,
)

data class NdsDnsInfo(
    val primary: String? = null,
    val responseTimeMs: Int? = null,
    val hijacked: Boolean? = null,
)

data class NdsFiberInfo(
    val rxPowerDbm: Double? = null,
    val txPowerDbm: Double? = null,
    val temperatureC: Double? = null,
    val voltageV: Double? = null,
)

/**
 * Payload de `POST /v1/diagnostics/evaluate` — schema completo documentado no
 * ADR-017. Cada bloco (`connection`, `wifi`, `wifiScan`, `speed`, `dns`,
 * `fiber`) e opcional: quando `null`, o campo e OMITIDO do JSON — o NDS trata
 * ausencia de bloco como "sem dado disponivel", nunca como zero/vazio.
 *
 * Nomes de chave JSON seguem exatamente o exemplo do ADR-017 (mistura
 * `snake_case`/`camelCase` real do contrato — nao uniformizado aqui porque o
 * servidor espera as chaves como estao).
 */
data class NdsDiagnosticsRequest(
    val requestId: String,
    val app: NdsAppInfo,
    val locale: String = "pt-BR",
    val profile: String? = null,
    val capabilities: List<String> = emptyList(),
    val connection: NdsConnectionInfo? = null,
    val wifi: NdsWifiInfo? = null,
    val wifiScan: NdsWifiScanInfo? = null,
    val speed: NdsSpeedInfo? = null,
    val dns: NdsDnsInfo? = null,
    val fiber: NdsFiberInfo? = null,
) {
    internal fun toJson(): JSONObject {
        val root = JSONObject()
        root.put("request_id", requestId)
        root.put(
            "app",
            JSONObject().apply {
                put("id", app.id)
                put("version", app.version)
            },
        )
        root.put("locale", locale)
        profile?.let { root.put("profile", it) }
        if (capabilities.isNotEmpty()) {
            root.put("capabilities", JSONArray(capabilities))
        }
        connection?.let { c ->
            root.put(
                "connection",
                JSONObject().apply {
                    put("type", c.type)
                    c.ssid?.let { put("ssid", it) }
                    c.bssid?.let { put("bssid", it) }
                },
            )
        }
        wifi?.let { w ->
            root.put(
                "wifi",
                JSONObject().apply {
                    w.rssi?.let { put("rssi", it) }
                    w.band?.let { put("band", it) }
                    w.channel?.let { put("channel", it) }
                    w.linkSpeed?.let { put("linkSpeed", it) }
                    w.standard?.let { put("standard", it) }
                },
            )
        }
        wifiScan?.let { ws ->
            root.put(
                "wifiScan",
                JSONObject().apply {
                    ws.channelCongestion?.let { put("channelCongestion", it) }
                    ws.bestChannel?.let { put("bestChannel", it) }
                },
            )
        }
        speed?.let { s ->
            root.put(
                "speed",
                JSONObject().apply {
                    s.pingMs?.let { put("ping_ms", it) }
                    s.jitterMs?.let { put("jitter_ms", it) }
                    s.downloadMbps?.let { put("download_mbps", it) }
                    s.uploadMbps?.let { put("upload_mbps", it) }
                    s.packetLossPercent?.let { put("packet_loss_percent", it) }
                },
            )
        }
        dns?.let { d ->
            root.put(
                "dns",
                JSONObject().apply {
                    d.primary?.let { put("primary", it) }
                    d.responseTimeMs?.let { put("responseTime_ms", it) }
                    d.hijacked?.let { put("hijacked", it) }
                },
            )
        }
        fiber?.let { f ->
            root.put(
                "fiber",
                JSONObject().apply {
                    f.rxPowerDbm?.let { put("rxPower_dbm", it) }
                    f.txPowerDbm?.let { put("txPower_dbm", it) }
                    f.temperatureC?.let { put("temperature_c", it) }
                    f.voltageV?.let { put("voltage_v", it) }
                },
            )
        }
        return root
    }
}
