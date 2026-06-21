package io.veloo.app.feature.wifi.channel

import kotlin.math.max
import kotlin.math.min

// Funcoes movidas para coreNetwork/contracts — mantidas aqui como delegates para nao quebrar imports existentes.

fun freqToChannel(freqMhz: Int): Pair<Band, Int>? =
    io.veloo.app.core.network.contracts.wifi.channel.freqToChannel(freqMhz)

fun channelToCenterFreq(band: Band, channel: Int, width: ChannelWidth): Int =
    io.veloo.app.core.network.contracts.wifi.channel.channelToCenterFreq(band, channel, width)

fun neighborSpans(neighbor: Neighbor): List<Pair<Int, Int>> =
    io.veloo.app.core.network.contracts.wifi.channel.neighborSpans(neighbor)

fun candidateSpan(band: Band, channel: Int, width: ChannelWidth): Pair<Int, Int> =
    io.veloo.app.core.network.contracts.wifi.channel.candidateSpan(band, channel, width)

fun dbmToMw(rssiDbm: Int): Double =
    io.veloo.app.core.network.contracts.wifi.channel.dbmToMw(rssiDbm)

fun overlapMhz(apLo: Int, apHi: Int, cLo: Int, cHi: Int): Int =
    io.veloo.app.core.network.contracts.wifi.channel.overlapMhz(apLo, apHi, cLo, cHi)
