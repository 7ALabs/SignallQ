package io.veloo.app.feature.wifi.channel

// Funcao movida para coreNetwork/contracts — mantida aqui como delegate para nao quebrar imports existentes.

fun evaluateChannels(
    neighbors: List<Neighbor>,
    config: EvalConfig = EvalConfig(),
): Map<Band, List<ChannelScore>> =
    io.veloo.app.core.network.contracts.wifi.channel.evaluateChannels(neighbors, config)
