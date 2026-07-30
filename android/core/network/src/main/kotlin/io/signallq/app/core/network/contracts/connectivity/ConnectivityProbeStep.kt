package io.signallq.app.core.network.contracts.connectivity

/** Etapa da sequência de diagnóstico local de conectividade (GH#1512), na ordem executada. */
enum class ConnectivityProbeStep {
    WIFI_TRANSPORT,
    ANDROID_CAPABILITIES,
    LINK_PROPERTIES,
    LOCAL_ADDRESS,
    GATEWAY_CONFIG,
    GATEWAY_REACHABILITY,
    DNS_CONFIG,
    DNS_REACHABILITY,
    EXTERNAL_IP_REACHABILITY,
    HOSTNAME_REACHABILITY,
    CAPTIVE_PORTAL,
    MOBILE_FALLBACK,
}

/** Um item de evidência coletado durante o diagnóstico — etapa + resultado tipado + detalhe opcional. */
data class ConnectivityEvidence(
    val step: ConnectivityProbeStep,
    val result: ProbeResult,
    val detail: String? = null,
)
