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

/**
 * Evidência de uma rede vizinha do scan Wi-Fi (ADR-018, bloco `wifiScan`). BSSID
 * NUNCA entra aqui — só identifica a rede internamente para o `ChannelEvaluator`,
 * nunca é enviado ao NDS (rede de terceiros, sem justificativa de produto para
 * expor). Todo campo é opcional individualmente: o scan pode reportar canal e RSSI
 * sem largura confiável, por exemplo.
 */
data class NdsWifiNeighborInfo(
    val channel: Int? = null,
    val frequencyMhz: Int? = null,
    val rssiDbm: Int? = null,
    val widthMhz: Int? = null,
)

/**
 * Bloco `wifiScan` do payload NDS (ADR-018). Carrega tanto o resultado calculado
 * (`channelCongestion`/`bestChannel`) quanto a evidência bruta (`neighbors`) que o
 * originou — o NDS precisa poder explicar por que um canal foi considerado
 * congestionado, não só receber a conclusão (#1832 seção 3).
 */
data class NdsWifiScanInfo(
    val connectedChannel: Int? = null,
    val channelCongestion: Int? = null,
    val bestChannel: Int? = null,
    /** Quantidade de redes vizinhas do scan. `0` é valor legítimo (scan rodou, zero
     *  vizinhas encontradas) — distinto do bloco inteiro ser omitido (scan não rodou). */
    val neighborCount: Int? = null,
    val neighbors: List<NdsWifiNeighborInfo> = emptyList(),
    /** Método/versão do algoritmo de avaliação de canal usado (`ChannelEvaluator`,
     *  `:coreNetwork`). Null quando nenhum canal foi avaliado. */
    val algorithmVersion: String? = null,
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

/**
 * Bloco `mobile` do payload NDS (ADR-018, bloco 10 — issue #1837). Chaves em
 * `snake_case` para casar com o exemplo publicado no #1832 seção 4. Nunca carrega
 * Cell ID/TAC/MCC/MNC — proibido explicitamente pela issue-mãe sem revisão de
 * privacidade dedicada (allowlist fechada nestes seis campos).
 */
data class NdsMobileInfo(
    val operator: String? = null,
    val technology: String? = null,
    val rsrpDbm: Int? = null,
    val rsrqDb: Int? = null,
    val sinrDb: Int? = null,
    val band: String? = null,
)

data class NdsFiberInfo(
    val rxPowerDbm: Double? = null,
    val txPowerDbm: Double? = null,
    val temperatureC: Double? = null,
    val voltageV: Double? = null,
)

/**
 * Bloco `localEquipment` do payload NDS (ADR-018 bloco 12 — issue #1839). Espelha
 * EXATAMENTE a allowlist de `SafeLocalDeviceContext`
 * (`core/network/.../contracts/localdevice/LocalDeviceSafeFilter.kt`) — nunca o
 * `LocalNetworkDeviceSnapshot` bruto. `LocalDeviceSafeFilter` já é o único ponto
 * de conversão permitido para IA/analytics/logs; este bloco só transcreve os
 * campos que ele já expõe, sem reabrir a superfície de dados que o filtro fecha.
 *
 * Os enums de status/tipo/suporte viram string via `.name` (vocabulário
 * `UPPER_SNAKE_CASE` do próprio Kotlin: ex. `ONT_GPON`, `LAB_VALIDATED`,
 * `ATENCAO`) — não existe tabela de tradução fixada pela ADR-018 para outro
 * vocabulário (o JSON ilustrativo da issue-mãe #1832, com `"ONT"`/`"FULL"`/
 * `"UP"`, é só exemplo; inventar uma tradução não documentada seria pior que
 * transcrever o nome real do enum já em uso pelo app.
 *
 * Nunca carrega: senha, serial completo, credenciais, payload HTML do
 * equipamento, token de sessão, MAC completo, ou qualquer campo fora desta
 * lista — mesma regra que `LocalDeviceSafeFilter` já impõe.
 */
data class NdsLocalEquipmentInfo(
    val vendor: String? = null,
    val model: String? = null,
    val firmwareVersion: String? = null,
    val deviceType: String,
    val supportLevel: String,
    val connectionStatus: String,
    val fiberStatus: String,
    val wanStatus: String,
    val wifiStatus: String,
    val lanStatus: String,
    val connectedClients: Int,
)

/**
 * Bloco `historical` do payload NDS (ADR-018 seção 13, NDS-Snapshot-06 — issue
 * #1838). Médias 7d/30d do histórico local (`MedicaoDao`) e degradação
 * calculada de forma determinística — o NDS/IA usa isso para diferenciar
 * incidente isolado de degradação recorrente (#1832 seção 5).
 *
 * `testsCount7d`/`testsCount30d` sempre serializam, mesmo `0` — contagem de
 * testes é um fato real (ADR-018), não ausência de dado. Os demais campos
 * (médias, degradação, janelas de horário) são individualmente omitidos
 * quando o dado correspondente não existir. O bloco inteiro fica `null`
 * (nunca chega a este tipo) quando não há nenhum teste no período — ver
 * `HistoricalDiagnosticInput.toNdsHistoricalInfo()`.
 */
data class NdsHistoricalInfo(
    val testsCount7d: Int = 0,
    val avgDownload7d: Double? = null,
    val avgUpload7d: Double? = null,
    val avgPing7d: Double? = null,
    val avgDns7d: Double? = null,
    val testsCount30d: Int = 0,
    val avgDownload30d: Double? = null,
    val avgUpload30d: Double? = null,
    val avgPing30d: Double? = null,
    val avgDns30d: Double? = null,
    val degradationDetected: Boolean? = null,
    val degradationPercent: Double? = null,
    val worstTimeWindow: String? = null,
    val bestTimeWindow: String? = null,
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
 * ADR-017 (contrato observado no NDS) e detalhado campo a campo no ADR-018
 * (nome/tipo/opcionalidade/origem por bloco). Cada bloco (`connection`, `wifi`,
 * `wifiScan`, `speed`, `quality`, `dns`, `gateway`, `fiber`, `mobile`,
 * `localEquipment`) e opcional: quando `null`, o campo e OMITIDO do JSON — o NDS
 * trata ausencia de bloco como "sem dado disponivel", nunca como zero/vazio.
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
    val mobile: NdsMobileInfo? = null,
    val historical: NdsHistoricalInfo? = null,
    val localEquipment: NdsLocalEquipmentInfo? = null,
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
                    ws.connectedChannel?.let { put("connectedChannel", it) }
                    ws.channelCongestion?.let { put("channelCongestion", it) }
                    ws.bestChannel?.let { put("bestChannel", it) }
                    ws.neighborCount?.let { put("neighborCount", it) }
                    if (ws.neighbors.isNotEmpty()) {
                        put(
                            "neighbors",
                            JSONArray(
                                ws.neighbors.map { n ->
                                    JSONObject().apply {
                                        n.channel?.let { put("channel", it) }
                                        n.frequencyMhz?.let { put("frequencyMhz", it) }
                                        n.rssiDbm?.let { put("rssiDbm", it) }
                                        n.widthMhz?.let { put("widthMhz", it) }
                                    }
                                },
                            ),
                        )
                    }
                    ws.algorithmVersion?.let { put("algorithmVersion", it) }
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
        mobile?.let { m ->
            root.put(
                "mobile",
                JSONObject().apply {
                    m.operator?.let { put("operator", it) }
                    m.technology?.let { put("technology", it) }
                    m.rsrpDbm?.let { put("rsrp_dbm", it) }
                    m.rsrqDb?.let { put("rsrq_db", it) }
                    m.sinrDb?.let { put("sinr_db", it) }
                    m.band?.let { put("band", it) }
                },
            )
        }
        historical?.let { h ->
            root.put(
                "historical",
                JSONObject().apply {
                    put("tests_7d", h.testsCount7d)
                    h.avgDownload7d?.let { put("avg_download_7d", it) }
                    h.avgUpload7d?.let { put("avg_upload_7d", it) }
                    h.avgPing7d?.let { put("avg_ping_7d", it) }
                    h.avgDns7d?.let { put("avg_dns_7d", it) }
                    put("tests_30d", h.testsCount30d)
                    h.avgDownload30d?.let { put("avg_download_30d", it) }
                    h.avgUpload30d?.let { put("avg_upload_30d", it) }
                    h.avgPing30d?.let { put("avg_ping_30d", it) }
                    h.avgDns30d?.let { put("avg_dns_30d", it) }
                    h.degradationDetected?.let { put("degradation_detected", it) }
                    h.degradationPercent?.let { put("degradation_percent", it) }
                    h.worstTimeWindow?.let { put("worstTimeWindow", it) }
                    h.bestTimeWindow?.let { put("bestTimeWindow", it) }
                },
            )
        }
        localEquipment?.let { le ->
            root.put(
                "localEquipment",
                JSONObject().apply {
                    le.vendor?.let { put("vendor", it) }
                    le.model?.let { put("model", it) }
                    le.firmwareVersion?.let { put("firmwareVersion", it) }
                    put("deviceType", le.deviceType)
                    put("supportLevel", le.supportLevel)
                    put("connectionStatus", le.connectionStatus)
                    put("fiberStatus", le.fiberStatus)
                    put("wanStatus", le.wanStatus)
                    put("wifiStatus", le.wifiStatus)
                    put("lanStatus", le.lanStatus)
                    put("connectedClients", le.connectedClients)
                },
            )
        }
        return root
    }
}
