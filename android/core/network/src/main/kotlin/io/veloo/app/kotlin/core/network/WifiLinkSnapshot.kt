package io.veloo.app.core.network

data class WifiLinkSnapshot(
    val ssid: String?,
    val bssid: String?,
    val rssiDbm: Int?,
    val linkSpeedMbps: Int?,
    val frequenciaMhz: Int?,
    val padraoWifi: String?,
)

