package io.veloo.app.core.network.contracts.wifi.channel

// Regra 5 — conjunto de canais candidatos (canais de controle) por banda, largura e flags
fun candidateChannels(
    band: Band,
    width: ChannelWidth,
    avoidDfs: Boolean,
    allow24Overlapping: Boolean,
    preferPsc: Boolean,
): List<Int> = when (band) {
    Band.GHZ_24 -> candidates24(allow24Overlapping)
    Band.GHZ_5 -> candidates5(width, avoidDfs)
    Band.GHZ_6 -> candidates6(preferPsc)
}

private fun candidates24(allow24Overlapping: Boolean): List<Int> =
    // Regra 5 — apenas 1/6/11 por padrao; allow24Overlapping libera 1-13
    if (allow24Overlapping) (1..13).toList() else listOf(1, 6, 11)

private fun candidates5(width: ChannelWidth, avoidDfs: Boolean): List<Int> {
    // Regra 5 — canais primarios padrao por largura (menor canal de cada grupo bondado)
    // Regra 6 — DFS = canais 52-144; excluidos quando avoidDfs=true
    val nonDfs: List<Int> = when (width) {
        ChannelWidth.W20 -> listOf(36, 40, 44, 48, 149, 153, 157, 161, 165)
        ChannelWidth.W40 -> listOf(36, 44, 149, 157)
        ChannelWidth.W80 -> listOf(36, 149)
        ChannelWidth.W160 -> listOf(36)
        ChannelWidth.W320 -> emptyList()
    }
    val dfs: List<Int> = when (width) {
        ChannelWidth.W20 -> listOf(52, 56, 60, 64, 100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 144)
        ChannelWidth.W40 -> listOf(52, 60, 100, 108, 116, 124, 132, 140)
        ChannelWidth.W80 -> listOf(52, 100, 116, 132)
        ChannelWidth.W160 -> listOf(100)
        ChannelWidth.W320 -> emptyList()
    }
    return if (avoidDfs) nonDfs else nonDfs + dfs
}

private fun candidates6(preferPsc: Boolean): List<Int> {
    // Regra 5 — PSC (Preferred Scanning Channels): um a cada 16 canais = 80 MHz
    val psc = listOf(5, 21, 37, 53, 69, 85, 101, 117, 133, 149, 165, 181, 197, 213, 229)
    if (preferPsc) return psc
    // Todos os canais 20 MHz em 6 GHz (passo de 4 canais = 20 MHz)
    return (1..233 step 4).toList()
}

// Regra 6 — verifica se canal pertence a faixa DFS de 5 GHz (52-144)
fun isDfs5Ghz(channel: Int): Boolean = channel in 52..144

// Regra 5 — verifica se canal e PSC em 6 GHz
private val PSC_6GHZ = setOf(5, 21, 37, 53, 69, 85, 101, 117, 133, 149, 165, 181, 197, 213, 229)

fun isPsc6Ghz(channel: Int): Boolean = channel in PSC_6GHZ
