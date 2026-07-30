package io.signallq.app.core.network.connectivity

import io.signallq.app.core.network.contracts.connectivity.ProbeResult

/** Sonda o gateway local — testa alcance, não implica DNS nem rota externa. */
fun interface GatewayProbe {
    suspend fun probe(gatewayIp: String): ProbeResult
}

/** Sonda resolução DNS pela rede sob análise — testa apenas resolução de nome. */
fun interface DnsProbe {
    suspend fun probe(hostnames: List<String>): ProbeResult
}

/** Sonda alcance externo por IP puro (sem depender de DNS). */
fun interface ExternalIpProbe {
    suspend fun probe(): ProbeResult
}

/** Resultado de [HostnameProbe] — inclui suspeita de captive portal detectada via HTTP. */
data class HostnameProbeOutcome(
    val result: ProbeResult,
    val captivePortalSuspeito: Boolean,
)

/** Sonda alcance externo por hostname (HTTP) — pode revelar captive portal via redirect/corpo inesperado. */
fun interface HostnameProbe {
    suspend fun probe(): HostnameProbeOutcome
}
