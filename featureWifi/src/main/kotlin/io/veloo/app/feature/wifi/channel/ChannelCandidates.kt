package io.veloo.app.feature.wifi.channel

// Funcoes movidas para coreNetwork/contracts — mantidas aqui como delegates para nao quebrar imports existentes.

fun candidateChannels(
    band: Band,
    width: ChannelWidth,
    avoidDfs: Boolean,
    allow24Overlapping: Boolean,
    preferPsc: Boolean,
): List<Int> = io.veloo.app.core.network.contracts.wifi.channel.candidateChannels(
    band, width, avoidDfs, allow24Overlapping, preferPsc,
)

fun isDfs5Ghz(channel: Int): Boolean =
    io.veloo.app.core.network.contracts.wifi.channel.isDfs5Ghz(channel)

fun isPsc6Ghz(channel: Int): Boolean =
    io.veloo.app.core.network.contracts.wifi.channel.isPsc6Ghz(channel)
