package io.signallq.app.feature.speedtest

enum class VereditoUso {
    good,
    acceptable,
    poor,
}

enum class GargaloPrimario {
    none,
    latency,
    upload,
    bufferbloat,
    packetLoss,
}

data class DiagnosticoQualidadeSpeedtest(
    val vereditoStreaming: VereditoUso,
    val vereditoGamer: VereditoUso,
    val vereditoVideoChamada: VereditoUso,
    val gargaloPrimario: GargaloPrimario,
)

