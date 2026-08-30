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

data class NdsQualityInfo(
    val latencyMs: Double? = null,
    val jitterMs: Double? = null,
    val packetLossPercent: Double? = null,
    val loadedLatencyMs: Double? = null,
    val bufferbloatMs: Double? = null,
)

data class NdsDnsInfo(
    val primary: String? = null,
    val responseTimeMs: Int? = null,
    val hijacked: Boolean? = null,
)

data class NdsGatewayInfo(
    val rttGatewayMs: Int? = null,
    val connectedDevices: Int? = null,
)

data class NdsFiberInfo(
    val rxPowerDbm: Double? = null,
    val txPowerDbm: Double? = null,
    val temperatureC: Double? = null,
    val voltageV: Double? = null,
)

data class NdsDiagnosticContext(
    val reportedProblem: String? = null,
    val objective: String? = null,
    /** Recorte estruturado opcional do objetivo aceito pelo contrato v2 do NDS. */
    val subcategory: String? = null,
    val symptoms: List<String> = emptyList(),
    val answers: Map<String, String> = emptyMap(),
)

/**
 * Payload de `POST /v1/diagnostics/evaluate` — schema completo documentado no
 * ADR-017. Cada bloco (`connection`, `wifi`, `wifiScan`, `speed`, `quality`,
 * `dns`, `gateway`, `fiber`) e opcional: quando `null`, o campo e OMITIDO do
 * JSON — o NDS trata
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
    val quality: NdsQualityInfo? = null,
    val dns: NdsDnsInfo? = null,
    val gateway: NdsGatewayInfo? = null,
    val fiber: NdsFiberInfo? = null,
    val context: NdsDiagnosticContext? = null,
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
        context?.let { c ->
            root.put("context", JSONObject().apply {
                c.reportedProblem?.takeIf(String::isNotBlank)?.let { put("reported_problem", it) }
                c.objective?.takeIf(String::isNotBlank)?.let { put("objective", it) }
                c.subcategory?.takeIf(String::isNotBlank)?.let { put("subcategory", it) }
                if (c.symptoms.isNotEmpty()) put("symptoms", JSONArray(c.symptoms))
                if (c.answers.isNotEmpty()) put("answers", JSONObject(c.answers))
            })
        }
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
        quality?.let { q ->
            root.put(
                "quality",
                JSONObject().apply {
                    q.latencyMs?.let { put("latencyMs", it) }
                    q.jitterMs?.let { put("jitterMs", it) }
                    q.packetLossPercent?.let { put("packetLossPercent", it) }
                    q.loadedLatencyMs?.let { put("loadedLatencyMs", it) }
                    q.bufferbloatMs?.let { put("bufferbloatMs", it) }
                },
            )
        }
        dns?.let { d ->
            root.put(
                "dns",
                JSONObject().apply {
                    d.primary?.let { put("primary", it) }
                    d.responseTimeMs?.let { put("latencyMs", it) }
                    d.hijacked?.let { put("hijacked", it) }
                },
            )
        }
        gateway?.let { g ->
            root.put(
                "gateway",
                JSONObject().apply {
                    g.rttGatewayMs?.let { put("rttGatewayMs", it) }
                    g.connectedDevices?.let { put("connectedDevices", it) }
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
