package io.veloo.app.core.network.contracts.wifi.channel

enum class Band { GHZ_24, GHZ_5, GHZ_6 }

enum class ChannelWidth(val mhz: Int) {
    W20(20), W40(40), W80(80), W160(160), W320(320)
}

// Uma rede vizinha ja normalizada — sem dependencia de android.*
data class Neighbor(
    val bssid: String,
    val band: Band,
    val centerFreqMhz: Int,   // centro do segmento principal (centerFreq0 do ScanResult)
    val centerFreq1Mhz: Int?, // segundo segmento apenas para 80+80; null caso contrario
    val width: ChannelWidth,
    val rssiDbm: Int,         // negativo, ex: -67
)

data class ChannelScore(
    val band: Band,
    val channel: Int,         // canal de controle (primary)
    val width: ChannelWidth,  // largura avaliada para esse candidato
    val score: Double,        // interferencia acumulada em mW; MENOR = melhor
    val overlappingAps: Int,
    val strongestNeighborDbm: Int?,
    val isDfs: Boolean,
    val isPsc: Boolean,
    val recommended: Boolean,
)

data class EvalConfig(
    val targetWidth24: ChannelWidth = ChannelWidth.W20,
    val targetWidth5: ChannelWidth = ChannelWidth.W80,
    val targetWidth6: ChannelWidth = ChannelWidth.W160,
    val allow24Overlapping: Boolean = false,
    val avoidDfs: Boolean = true,
    val dfsPenalty: Double = 1.3,
    val preferPsc: Boolean = true,
    val excludeBssids: Set<String> = emptySet(),
)
