package io.signallq.app.core.network.connectivity

/**
 * Entrada já resolvida (independente de Android) para [ConnectivityDiagnosisEngine] —
 * a camada Android ([ConnectivityDiagnosisRunner]) é responsável por capturar a rede
 * Wi-Fi sob análise, suas [android.net.NetworkCapabilities] e [android.net.LinkProperties]
 * e preencher este contexto antes de chamar o motor.
 */
data class ConnectivityDiagnosisContext(
    val wifiConnected: Boolean,
    val localAddressAvailable: Boolean,
    val gatewayIp: String?,
    val dnsServers: List<String>,
    val androidInternetCapability: Boolean,
    val androidValidated: Boolean,
    val androidCaptivePortalCapability: Boolean,
    val mobileFallbackAvailable: Boolean,
)
