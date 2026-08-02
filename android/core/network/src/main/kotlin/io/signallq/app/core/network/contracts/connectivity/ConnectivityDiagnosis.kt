package io.signallq.app.core.network.contracts.connectivity

import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.contracts.topologia.NivelConfianca

/**
 * Diagnóstico local e determinístico de conectividade (GH#1512) — não depende de Worker,
 * IA, Firebase, catálogo remoto ou qualquer outro serviço externo do produto.
 *
 * Separa explicitamente enlace local, gateway, DNS e rota externa em vez de tratar a
 * presença do Wi-Fi como prova de internet. Reaproveita [EstadoConexao] (transporte já
 * usado por [io.signallq.app.core.network.SnapshotRede]) e [NivelConfianca] (confiança já
 * usada pelo motor de topologia) em vez de duplicar esses vocabulários.
 *
 * @param mobileFallbackAvailable presença de rede móvel como alternativa — nunca usada
 *   para substituir ou mascarar o resultado do Wi-Fi sob análise.
 */
data class ConnectivityDiagnosis(
    val transport: EstadoConexao,
    val wifiConnected: Boolean,
    val localAddressAvailable: Boolean,
    val gatewayConfigured: Boolean,
    val gatewayReachable: ProbeResult,
    val dnsConfigured: Boolean,
    val dnsReachable: ProbeResult,
    val externalIpReachable: ProbeResult,
    val hostnameReachable: ProbeResult,
    val androidInternetCapability: Boolean,
    val androidValidated: Boolean,
    val captivePortalDetected: Boolean,
    val mobileFallbackAvailable: Boolean,
    val status: ConnectivityStatus,
    val confidence: NivelConfianca,
    val evidence: List<ConnectivityEvidence>,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
)
