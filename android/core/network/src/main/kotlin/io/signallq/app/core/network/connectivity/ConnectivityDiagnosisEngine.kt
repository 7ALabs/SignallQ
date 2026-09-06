package io.signallq.app.core.network.connectivity

import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.contracts.connectivity.ConnectivityDiagnosis
import io.signallq.app.core.network.contracts.connectivity.ConnectivityEvidence
import io.signallq.app.core.network.contracts.connectivity.ConnectivityProbeStep
import io.signallq.app.core.network.contracts.connectivity.ProbeFailureReason
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Motor puro de diagnóstico local de conectividade (GH#1512) — combina as sondagens
 * (gateway -> DNS -> rota externa) em um [ConnectivityDiagnosis] único, sem depender de
 * Worker, IA, Firebase ou qualquer serviço externo do produto.
 *
 * Sequência: cada etapa só roda se a anterior confirmou sucesso — nunca assume sucesso
 * implícito por uma etapa não executada. Cada etapa tem timeout próprio
 * ([stepTimeoutMs]); [globalTimeoutMs] é um teto de segurança adicional para toda a
 * sequência, cobrindo qualquer chamada bloqueante que não respeite seu timeout individual.
 * Cancelamento do coroutine chamador é propagado normalmente (não é capturado aqui).
 */
class ConnectivityDiagnosisEngine(
    private val gatewayProbe: GatewayProbe,
    private val dnsProbe: DnsProbe,
    private val externalIpProbe: ExternalIpProbe,
    private val hostnameProbe: HostnameProbe,
    private val stepTimeoutMs: Long = STEP_TIMEOUT_MS_DEFAULT,
    private val globalTimeoutMs: Long = GLOBAL_TIMEOUT_MS_DEFAULT,
    private val dnsHostnamesParaTeste: List<String> = DNS_HOSTNAMES_PADRAO,
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    companion object {
        const val STEP_TIMEOUT_MS_DEFAULT = 2500L
        const val GLOBAL_TIMEOUT_MS_DEFAULT = 8000L
        val DNS_HOSTNAMES_PADRAO = listOf("connectivitycheck.gstatic.com", "cloudflare.com")
    }

    suspend fun diagnosticar(context: ConnectivityDiagnosisContext): ConnectivityDiagnosis {
        val iniciadoEm = now()

        if (!context.wifiConnected) {
            return montarDiagnostico(
                context = context,
                gatewayReachable = ProbeResult.NotExecuted("Wi-Fi desconectado"),
                dnsReachable = ProbeResult.NotExecuted("Wi-Fi desconectado"),
                externalIpReachable = ProbeResult.NotExecuted("Wi-Fi desconectado"),
                hostnameReachable = ProbeResult.NotExecuted("Wi-Fi desconectado"),
                captivePortalSuspeito = false,
                evidencias =
                    listOf(
                        ConnectivityEvidence(
                            ConnectivityProbeStep.WIFI_TRANSPORT,
                            ProbeResult.Failure(ProbeFailureReason.UNKNOWN),
                            "Sem transporte Wi-Fi ativo",
                        ),
                    ),
                iniciadoEm = iniciadoEm,
            )
        }

        val evidencias = mutableListOf<ConnectivityEvidence>()
        var gatewayResultado: ProbeResult = ProbeResult.NotExecuted("etapa nao alcancada antes do timeout global")
        var dnsResultado: ProbeResult = ProbeResult.NotExecuted("gateway nao confirmou alcance")
        var externalIpResultado: ProbeResult = ProbeResult.NotExecuted("DNS nao confirmou resolucao")
        var hostnameResultado: ProbeResult = ProbeResult.NotExecuted("DNS nao confirmou resolucao")
        var captivePortalSuspeito = false

        val concluiuDentroDoPrazo =
            withTimeoutOrNull(globalTimeoutMs) {
                gatewayResultado =
                    if (context.gatewayIp == null) {
                        ProbeResult.Unavailable("sem rota de gateway configurada")
                    } else {
                        withTimeoutOrNull(stepTimeoutMs) { gatewayProbe.probe(context.gatewayIp) }
                            ?: ProbeResult.Timeout(stepTimeoutMs)
                    }
                evidencias += ConnectivityEvidence(ConnectivityProbeStep.GATEWAY_REACHABILITY, gatewayResultado)

                if (gatewayResultado is ProbeResult.Success) {
                    dnsResultado =
                        if (context.dnsServers.isEmpty()) {
                            ProbeResult.Unavailable("nenhum servidor DNS configurado")
                        } else {
                            withTimeoutOrNull(stepTimeoutMs) { dnsProbe.probe(dnsHostnamesParaTeste) }
                                ?: ProbeResult.Timeout(stepTimeoutMs)
                        }
                    evidencias += ConnectivityEvidence(ConnectivityProbeStep.DNS_REACHABILITY, dnsResultado)

                    if (dnsResultado is ProbeResult.Success) {
                        val resultadoParalelo =
                            withTimeoutOrNull(stepTimeoutMs) {
                                coroutineScope {
                                    val ipDeferred = async { externalIpProbe.probe() }
                                    val hostDeferred = async { hostnameProbe.probe() }
                                    ipDeferred.await() to hostDeferred.await()
                                }
                            }
                        if (resultadoParalelo != null) {
                            externalIpResultado = resultadoParalelo.first
                            hostnameResultado = resultadoParalelo.second.result
                            captivePortalSuspeito = resultadoParalelo.second.captivePortalSuspeito
                        } else {
                            externalIpResultado = ProbeResult.Timeout(stepTimeoutMs)
                            hostnameResultado = ProbeResult.Timeout(stepTimeoutMs)
                        }
                        evidencias += ConnectivityEvidence(ConnectivityProbeStep.EXTERNAL_IP_REACHABILITY, externalIpResultado)
                        evidencias += ConnectivityEvidence(ConnectivityProbeStep.HOSTNAME_REACHABILITY, hostnameResultado)
                    }
                }
                true
            }

        if (concluiuDentroDoPrazo == null) {
            // Timeout global -- qualquer etapa que ainda estivesse com valor "nao executado"
            // vira Timeout explicito (motivo real: estourou o prazo, nao "nunca foi tentada").
            if (gatewayResultado is ProbeResult.NotExecuted) gatewayResultado = ProbeResult.Timeout(globalTimeoutMs)
            if (dnsResultado is ProbeResult.NotExecuted) dnsResultado = ProbeResult.Timeout(globalTimeoutMs)
            if (externalIpResultado is ProbeResult.NotExecuted) externalIpResultado = ProbeResult.Timeout(globalTimeoutMs)
            if (hostnameResultado is ProbeResult.NotExecuted) hostnameResultado = ProbeResult.Timeout(globalTimeoutMs)
        }

        return montarDiagnostico(
            context = context,
            gatewayReachable = gatewayResultado,
            dnsReachable = dnsResultado,
            externalIpReachable = externalIpResultado,
            hostnameReachable = hostnameResultado,
            captivePortalSuspeito = captivePortalSuspeito,
            evidencias = evidencias,
            iniciadoEm = iniciadoEm,
            globalTimeoutExceeded = concluiuDentroDoPrazo == null,
        )
    }

    private fun montarDiagnostico(
        context: ConnectivityDiagnosisContext,
        gatewayReachable: ProbeResult,
        dnsReachable: ProbeResult,
        externalIpReachable: ProbeResult,
        hostnameReachable: ProbeResult,
        captivePortalSuspeito: Boolean,
        evidencias: List<ConnectivityEvidence>,
        iniciadoEm: Long,
        globalTimeoutExceeded: Boolean = false,
    ): ConnectivityDiagnosis {
        val captivePortalDetectado = context.androidCaptivePortalCapability || captivePortalSuspeito
        val outcome =
            ConnectivityProbeOutcome(
                wifiConnected = context.wifiConnected,
                localAddressAvailable = context.localAddressAvailable,
                gatewayConfigured = context.gatewayIp != null,
                gatewayReachable = gatewayReachable,
                dnsConfigured = context.dnsServers.isNotEmpty(),
                dnsReachable = dnsReachable,
                externalIpReachable = externalIpReachable,
                hostnameReachable = hostnameReachable,
                androidInternetCapability = context.androidInternetCapability,
                androidValidated = context.androidValidated,
                captivePortalDetected = captivePortalDetectado,
                globalTimeoutExceeded = globalTimeoutExceeded,
            )
        val resolucao = ConnectivityStatusResolver.resolve(outcome)

        return ConnectivityDiagnosis(
            transport = if (context.wifiConnected) EstadoConexao.wifi else EstadoConexao.desconectado,
            wifiConnected = context.wifiConnected,
            localAddressAvailable = context.localAddressAvailable,
            gatewayConfigured = outcome.gatewayConfigured,
            gatewayReachable = gatewayReachable,
            dnsConfigured = outcome.dnsConfigured,
            dnsReachable = dnsReachable,
            externalIpReachable = externalIpReachable,
            hostnameReachable = hostnameReachable,
            androidInternetCapability = context.androidInternetCapability,
            androidValidated = context.androidValidated,
            captivePortalDetected = captivePortalDetectado,
            mobileFallbackAvailable = context.mobileFallbackAvailable,
            status = resolucao.status,
            confidence = resolucao.confidence,
            evidence = evidencias,
            startedAtEpochMs = iniciadoEm,
            finishedAtEpochMs = now(),
        )
    }
}
