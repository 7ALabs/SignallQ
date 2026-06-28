package io.signallq.app.feature.diagnostico

data class WifiDiagnosticInput(
    val rssiDbm: Int?,
    val linkSpeedMbps: Int?,
    val frequenciaMhz: Int?,
    val ssid: String? = null,
    val bssidMascarado: String? = null,
    val canal: Int? = null,
    val larguraCanalMhz: Int? = null,
    val wifiStandard: String? = null,
    val linkSpeedDownMbps: Int? = null,
    val linkSpeedUpMbps: Int? = null,
    val gatewayIp: String? = null,
    val localIp: String? = null,
    val routerType: RouterType? = null,
    val dispositivosNaRede: Int? = null,
)

data class InternetDiagnosticInput(
    val downloadMbps: Double?,
    val uploadMbps: Double?,
    val latencyMs: Double?,
    val jitterMs: Double?,
    val perdaPercentual: Double?,
    val bufferbloatMs: Double? = null,
    val testMode: String? = null,
    val serverName: String? = null,
    val serverRegion: String? = null,
    val serverHost: String? = null,
    val testDurationMs: Long? = null,
    val qualidadeUso: SpeedtestQualityInput? = null,
    /** RTT TCP para o gateway local (porta 80/443/53). Null se não disponível
     *  (emulador, Doze Mode, gateway não responde TCP). */
    val rttGatewayMs: Int? = null,
)

data class FibraDiagnosticInput(
    val rxPowerDbm: Double? = null,
    val txPowerDbm: Double? = null,
    val temperatureCelsius: Double? = null,
    val isUp: Boolean,
)

data class MobileDiagnosticInput(
    val carrierName: String? = null,
    val mobileTechnology: String? = null,
    val signalStrengthDbm: Int? = null,
    val signalQualityPercent: Int? = null,
    val band: String? = null,
    val publicIp: String? = null,
)

data class DnsDiagnosticInput(
    val currentDnsIp: String? = null,
    val currentDnsName: String? = null,
    val currentDnsLatencyMs: Int? = null,
    val bestDnsNameFromComparison: String? = null,
    val bestDnsLatencyMsFromComparison: Int? = null,
    val dnsGrade: String? = null,
    val dnsComparisonAvailable: Boolean = false,
)

data class HistoricalDiagnosticInput(
    val avgDownload7d: Double? = null,
    val avgUpload7d: Double? = null,
    val avgPing7d: Double? = null,
    val avgDns7d: Double? = null,
    val testsCount7d: Int = 0,
    val avgDownload30d: Double? = null,
    val avgUpload30d: Double? = null,
    val avgPing30d: Double? = null,
    val avgDns30d: Double? = null,
    val testsCount30d: Int = 0,
    val degradationDetected: Boolean? = null,
    val degradationPercent: Double? = null,
    val worstTimeWindow: String? = null,
    val bestTimeWindow: String? = null,
)

data class WifiScanDiagnosticInput(
    val redes: List<RedeWifiVizinha> = emptyList(),
    val conectadoCanal: Int? = null,
    val conectadoBanda: BandaWifi? = null,
)

data class RedeWifiVizinha(
    val canal: Int?,
    val rssiDbm: Int?,
    val frequenciaMhz: Int?,
    val ssid: String? = null,
    val bssid: String? = null,
)

data class SpeedtestQualityInput(
    val vereditoStreaming: String? = null,
    val vereditoGamer: String? = null,
    val vereditoVideochamada: String? = null,
    val gargaloPrimario: String? = null,
    val severidadeBufferbloat: String? = null,
)

enum class ConnectionType { wifi, mobile, ethernet, desconectado, desconhecido }

enum class RouterType { roteador, mesh, extensor, desconhecido }

enum class BandaWifi { ghz24, ghz5, desconhecida }

fun WifiDiagnosticInput.banda(): BandaWifi = when {
    frequenciaMhz == null -> BandaWifi.desconhecida
    frequenciaMhz < 3000 -> BandaWifi.ghz24
    else -> BandaWifi.ghz5
}

data class DiagnosticInput(
    val connectionType: ConnectionType = ConnectionType.desconhecido,
    val internet: InternetDiagnosticInput? = null,
    val wifi: WifiDiagnosticInput? = null,
    val fibra: FibraDiagnosticInput? = null,
    val mobile: MobileDiagnosticInput? = null,
    val dns: DnsDiagnosticInput? = null,
    val historico: HistoricalDiagnosticInput? = null,
    val wifiScan: WifiScanDiagnosticInput? = null,
)
