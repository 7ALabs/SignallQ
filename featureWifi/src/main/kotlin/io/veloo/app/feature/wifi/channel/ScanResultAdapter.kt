package io.veloo.app.feature.wifi.channel

import android.net.wifi.ScanResult

// Adapter Android — arquivo separado do core testável.
// Converte ScanResult (API Android) para Neighbor (modelo core puro).
fun ScanResult.toNeighbor(): Neighbor? {
    val (band, _) = freqToChannel(frequency) ?: return null

    val width = when (channelWidth) {
        ScanResult.CHANNEL_WIDTH_20MHZ -> ChannelWidth.W20
        ScanResult.CHANNEL_WIDTH_40MHZ -> ChannelWidth.W40
        ScanResult.CHANNEL_WIDTH_80MHZ -> ChannelWidth.W80
        ScanResult.CHANNEL_WIDTH_160MHZ -> ChannelWidth.W160
        ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> ChannelWidth.W80 // 80+80; usa centerFreq0 + centerFreq1
        5 -> ChannelWidth.W320                                        // CHANNEL_WIDTH_320MHZ (API 33+)
        else -> ChannelWidth.W20
    }

    // Para W20, centerFreq0 pode ser 0 ou coincidir com frequency; usa frequency nesses casos.
    // Para W40/W80/W160/W320, centerFreq0 é o centro do segmento bondado (≠ frequency).
    val centerFreqMhz = if (centerFreq0 != 0 && centerFreq0 != frequency) centerFreq0 else frequency

    // 80+80: centerFreq1 é o centro do segundo segmento de 80 MHz; ignorado para outros modos.
    val centerFreq1Mhz: Int? =
        if (channelWidth == ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ && centerFreq1 != 0) {
            centerFreq1
        } else {
            null
        }

    return Neighbor(
        bssid = BSSID ?: return null,
        band = band,
        centerFreqMhz = centerFreqMhz,
        centerFreq1Mhz = centerFreq1Mhz,
        width = width,
        rssiDbm = level,
    )
}
