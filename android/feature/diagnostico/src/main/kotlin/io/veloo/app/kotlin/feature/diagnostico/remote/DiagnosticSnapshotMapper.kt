package io.signallq.app.feature.diagnostico.remote

import io.signallq.app.feature.diagnostico.BandaWifi
import io.signallq.app.feature.diagnostico.ConnectionType
import io.signallq.app.feature.diagnostico.DiagnosticInput
import io.signallq.app.feature.diagnostico.banda
import org.json.JSONArray
import org.json.JSONObject

/**
 * Converte [DiagnosticInput] (contrato local, Kotlin) para o JSON do contrato
 * `DiagnosticSnapshot` esperado por `POST /diagnostic/evaluate` no worker
 * `signallq-diagnostic` (`integrations/cloudflare/signallq-diagnostic-worker/src/contracts.ts`).
 *
 * Mapeia SO os campos que o motor remoto de fato le (`bundled-ruleset.ts`,
 * `score-engine.ts`, `diagnostic-report.ts` — conferido por grep antes de
 * implementar, GH#962). Nao envia nada que o worker descarta silenciosamente.
 *
 * `schemaVersion` fixo em [REMOTE_DIAGNOSTIC_SNAPSHOT_SCHEMA_VERSION] — maior
 * valor de `minimumSchemaVersion` usado no ruleset bundled do worker hoje
 * (6), garantindo que nenhuma regra seja pulada por versao insuficiente.
 */
internal object DiagnosticSnapshotMapper {

    const val REMOTE_DIAGNOSTIC_SNAPSHOT_SCHEMA_VERSION = 6

    fun toJson(input: DiagnosticInput): JSONObject {
        val o = JSONObject()
        o.put("schemaVersion", REMOTE_DIAGNOSTIC_SNAPSHOT_SCHEMA_VERSION)

        o.put("connection", connectionJson(input))
        wifiJson(input)?.let { o.put("wifi", it) }
        wifiScanJson(input)?.let { o.put("wifiScan", it) }
        speedJson(input)?.let { o.put("speed", it) }
        qualityJson(input)?.let { o.put("quality", it) }
        dnsJson(input)?.let { o.put("dns", it) }
        fiberJson(input)?.let { o.put("fiber", it) }
        mobileJson(input)?.let { o.put("mobile", it) }
        historicalJson(input)?.let { o.put("historical", it) }
        gatewayJson(input)?.let { o.put("gateway", it) }

        return o
    }

    private fun connectionType(tipo: ConnectionType): String = when (tipo) {
        ConnectionType.wifi -> "WIFI"
        ConnectionType.mobile -> "MOBILE"
        ConnectionType.ethernet -> "ETHERNET"
        ConnectionType.desconectado -> "DISCONNECTED"
        ConnectionType.desconhecido -> "UNKNOWN"
    }

    /**
     * `hasInternet` so e afirmado quando ha evidencia real: [DiagnosticInput.internet]
     * presente mas sem NENHUMA metrica de velocidade (download/upload) sugere teste
     * que falhou por falta de internet -> `false`. Sem [DiagnosticInput.internet]
     * (nenhum teste rodado) o campo fica ausente -> o worker nao assume nada.
     */
    private fun connectionJson(input: DiagnosticInput): JSONObject {
        val o = JSONObject()
        o.put("type", connectionType(input.connectionType))
        val internet = input.internet
        if (internet != null && internet.downloadMbps == null && internet.uploadMbps == null) {
            o.put("hasInternet", false)
        }
        return o
    }

    private fun bandaToWorkerString(banda: BandaWifi): String? = when (banda) {
        BandaWifi.ghz24 -> "2_4_GHZ"
        BandaWifi.ghz5 -> "5_GHZ"
        BandaWifi.desconhecida -> null
    }

    private fun wifiJson(input: DiagnosticInput): JSONObject? {
        val wifi = input.wifi ?: return null
        val o = JSONObject()
        bandaToWorkerString(wifi.banda())?.let { o.put("band", it) }
        wifi.rssiDbm?.let { o.put("rssiDbm", it) }
        wifi.frequenciaMhz?.let { o.put("frequencyMhz", it) }
        wifi.linkSpeedMbps?.let { o.put("linkSpeedMbps", it) }
        wifi.is5GhzCapable?.let { o.put("has5GhzAvailable", it) }
        wifi.dispositivosNaRede?.let { o.put("devicesOnNetwork", it) }
        return o
    }

    private fun wifiScanJson(input: DiagnosticInput): JSONObject? {
        val scan = input.wifiScan ?: return null
        val o = JSONObject()
        scan.conectadoCanal?.let { o.put("connectedChannel", it) }
        if (scan.redes.isNotEmpty()) {
            val arr = JSONArray()
            scan.redes.forEach { rede ->
                val ro = JSONObject()
                rede.canal?.let { ro.put("channel", it) }
                rede.frequenciaMhz?.let { ro.put("frequencyMhz", it) }
                rede.rssiDbm?.let { ro.put("rssiDbm", it) }
                rede.ssid?.let { ro.put("ssid", it) }
                arr.put(ro)
            }
            o.put("networks", arr)
        }
        return o
    }

    private fun speedJson(input: DiagnosticInput): JSONObject? {
        val internet = input.internet ?: return null
        if (internet.downloadMbps == null && internet.uploadMbps == null) return null
        val o = JSONObject()
        internet.downloadMbps?.let { o.put("downloadMbps", it) }
        internet.uploadMbps?.let { o.put("uploadMbps", it) }
        return o
    }

    private fun qualityJson(input: DiagnosticInput): JSONObject? {
        val internet = input.internet ?: return null
        val o = JSONObject()
        internet.latencyMs?.let { o.put("latencyMs", it) }
        internet.jitterMs?.let { o.put("jitterMs", it) }
        internet.perdaPercentual?.let { o.put("packetLossPercent", it) }
        // loadedLatencyMs = latencia sob carga = latencia idle + delta de bufferbloat
        // ja medido localmente. O worker deriva bufferbloat do delta entre os dois
        // campos (nao ha campo "bufferbloatMs" direto no contrato remoto).
        if (internet.latencyMs != null && internet.bufferbloatMs != null) {
            o.put("loadedLatencyMs", internet.latencyMs + internet.bufferbloatMs)
        }
        if (o.length() == 0) return null
        return o
    }

    private fun dnsJson(input: DiagnosticInput): JSONObject? {
        val dns = input.dns ?: return null
        if (dns.currentDnsLatencyMs == null && dns.currentDnsName == null) return null
        val o = JSONObject()
        dns.currentDnsLatencyMs?.let { o.put("latencyMs", it) }
        dns.currentDnsName?.let { o.put("currentProvider", it) }
        return o
    }

    private fun fiberJson(input: DiagnosticInput): JSONObject? {
        val fibra = input.fibra ?: return null
        if (!fibra.isUp && fibra.rxPowerDbm == null && fibra.txPowerDbm == null && fibra.temperatureCelsius == null) {
            return null
        }
        val o = JSONObject()
        fibra.rxPowerDbm?.let { o.put("rxPowerDbm", it) }
        fibra.txPowerDbm?.let { o.put("txPowerDbm", it) }
        fibra.temperatureCelsius?.let { o.put("temperatureCelsius", it) }
        return o
    }

    private fun mobileJson(input: DiagnosticInput): JSONObject? {
        val mobile = input.mobile ?: return null
        val o = JSONObject()
        mobile.mobileTechnology?.let { o.put("technology", it) }
        mobile.rsrpDbm?.let { o.put("rsrpDbm", it) }
        mobile.rsrqDb?.let { o.put("rsrqDb", it) }
        mobile.sinrDb?.let { o.put("sinrDb", it) }
        mobile.carrierName?.let { o.put("operatorName", it) }
        if (o.length() == 0) return null
        return o
    }

    private fun historicalJson(input: DiagnosticInput): JSONObject? {
        val h = input.historico ?: return null
        val o = JSONObject()
        o.put("testsCount7d", h.testsCount7d)
        o.put("testsCount30d", h.testsCount30d)
        h.avgDownload7d?.let { o.put("avgDownload7d", it) }
        h.avgDownload30d?.let { o.put("avgDownload30d", it) }
        h.avgUpload7d?.let { o.put("avgUpload7d", it) }
        h.avgUpload30d?.let { o.put("avgUpload30d", it) }
        h.avgPing7d?.let { o.put("avgPing7d", it) }
        h.avgPing30d?.let { o.put("avgPing30d", it) }
        h.avgDns7d?.let { o.put("avgDns7d", it) }
        h.avgDns30d?.let { o.put("avgDns30d", it) }
        h.worstTimeWindow?.let { o.put("worstTimeWindow", it) }
        h.bestTimeWindow?.let { o.put("bestTimeWindow", it) }
        return o
    }

    private fun gatewayJson(input: DiagnosticInput): JSONObject? {
        val rtt = input.internet?.rttGatewayMs ?: return null
        val o = JSONObject()
        o.put("rttMs", rtt)
        return o
    }
}
