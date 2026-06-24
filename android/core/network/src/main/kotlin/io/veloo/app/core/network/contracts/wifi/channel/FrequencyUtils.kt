package io.veloo.app.core.network.contracts.wifi.channel

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// Regra 1 — frequencia → (banda, canal de controle)
fun freqToChannel(freqMhz: Int): Pair<Band, Int>? = when {
    freqMhz == 2484 -> Band.GHZ_24 to 14
    freqMhz in 2412..2472 -> Band.GHZ_24 to ((freqMhz - 2407) / 5)
    freqMhz in 5170..5835 -> Band.GHZ_5 to ((freqMhz - 5000) / 5)
    freqMhz in 5955..7115 -> Band.GHZ_6 to ((freqMhz - 5950) / 5)
    else -> null
}

// Regra 1 — canal de controle → centro espectral para a largura alvo
fun channelToCenterFreq(band: Band, channel: Int, width: ChannelWidth): Int = when (band) {
    Band.GHZ_24 -> 2407 + channel * 5
    Band.GHZ_5 -> alignToGrid(
        primaryFreq = 5000 + channel * 5,
        baseFreq = 5170,    // menor frequencia valida no padrao (ch 34)
        widthMhz = width.mhz,
    )
    Band.GHZ_6 -> alignToGrid(
        primaryFreq = 5950 + channel * 5,
        baseFreq = 5955,    // ch 1 em 6 GHz
        widthMhz = width.mhz,
    )
}

// Alinha primaryFreq ao centro do segmento W-MHz que o contem, contando a partir de baseFreq
private fun alignToGrid(primaryFreq: Int, baseFreq: Int, widthMhz: Int): Int {
    val relPos = primaryFreq - baseFreq
    val segIdx = relPos / widthMhz
    return baseFreq + segIdx * widthMhz + widthMhz / 2
}

// Regra 2 — spans espectrais [lo, hi) ocupados por um AP vizinho
// Retorna 2 spans para 80+80, 1 span para os demais
fun neighborSpans(neighbor: Neighbor): List<Pair<Int, Int>> {
    val half = neighbor.width.mhz / 2
    val mainSpan = (neighbor.centerFreqMhz - half) to (neighbor.centerFreqMhz + half)
    return if (neighbor.centerFreq1Mhz != null) {
        // Regra 2 — segundo segmento de 80 MHz para 80+80
        val sec = (neighbor.centerFreq1Mhz - 40) to (neighbor.centerFreq1Mhz + 40)
        listOf(mainSpan, sec)
    } else {
        listOf(mainSpan)
    }
}

// Regra 2 — span espectral [lo, hi) de um canal candidato com largura W
fun candidateSpan(band: Band, channel: Int, width: ChannelWidth): Pair<Int, Int> {
    val center = channelToCenterFreq(band, channel, width)
    val half = width.mhz / 2
    return (center - half) to (center + half)
}

// Regra 3 — dBm → potencia linear (mW); acumulacao sempre no dominio linear
fun dbmToMw(rssiDbm: Int): Double = 10.0.pow(rssiDbm / 10.0)

// Regra 4 — sobreposicao espectral em MHz entre dois spans [lo, hi)
fun overlapMhz(apLo: Int, apHi: Int, cLo: Int, cHi: Int): Int =
    max(0, min(apHi, cHi) - max(apLo, cLo))
