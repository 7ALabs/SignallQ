package io.signallq.app.core.network.connectivity

import io.signallq.app.core.network.contracts.connectivity.ConnectivityStatus
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import io.signallq.app.core.network.contracts.topologia.NivelConfianca

/**
 * Entrada pura (sem Android) para [ConnectivityStatusResolver.resolve] — mesmos campos
 * de evidência de [io.signallq.app.core.network.contracts.connectivity.ConnectivityDiagnosis],
 * sem transporte/timestamps (irrelevantes para a decisão de estado).
 */
data class ConnectivityProbeOutcome(
    val wifiConnected: Boolean,
    val gatewayConfigured: Boolean,
    val gatewayReachable: ProbeResult,
    val dnsConfigured: Boolean,
    val dnsReachable: ProbeResult,
    val externalIpReachable: ProbeResult,
    val hostnameReachable: ProbeResult,
    val androidInternetCapability: Boolean,
    val androidValidated: Boolean,
    val captivePortalDetected: Boolean,
    /** `true` quando o teto de segurança global do motor estourou antes de uma etapa
     *  concluir (ver [ConnectivityDiagnosisEngine]) -- distingue um [ProbeResult.Timeout]
     *  que a sondagem realmente sofreu de um [ProbeResult.Timeout] que é só o rótulo dado a
     *  uma etapa nunca alcançada. Etapa nunca alcançada nunca deve virar evidência de
     *  confiança ALTA (GH#1512, achado de revisão). */
    val globalTimeoutExceeded: Boolean = false,
)

data class ConnectivityStatusResolution(
    val status: ConnectivityStatus,
    val confidence: NivelConfianca,
)

/**
 * Combina as evidências de [ConnectivityProbeOutcome] em um [ConnectivityStatus] único —
 * função pura, sem I/O, sem Android. Único ponto de decisão do diagnóstico local (GH#1512):
 * a UI e a persistência consomem o resultado daqui, nunca recalculam a regra.
 *
 * Precedência (mais específico primeiro): transporte -> captive portal -> gateway -> DNS ->
 * rota externa -> internet ok. Cada camada só é avaliada se a anterior confirmou sucesso —
 * uma sondagem [ProbeResult.NotExecuted]/[ProbeResult.Unavailable]/[ProbeResult.Timeout] nunca
 * é tratada como sucesso implícito.
 */
object ConnectivityStatusResolver {

    fun resolve(outcome: ConnectivityProbeOutcome): ConnectivityStatusResolution {
        if (!outcome.wifiConnected) {
            return ConnectivityStatusResolution(ConnectivityStatus.WIFI_DISCONNECTED, NivelConfianca.ALTA)
        }

        if (outcome.captivePortalDetected) {
            return ConnectivityStatusResolution(ConnectivityStatus.CAPTIVE_PORTAL, NivelConfianca.ALTA)
        }

        val gatewaySucesso = outcome.gatewayReachable is ProbeResult.Success
        if (!outcome.gatewayConfigured || !gatewaySucesso) {
            val confianca = when {
                !outcome.gatewayConfigured -> NivelConfianca.MEDIA
                // Timeout causado pelo teto global (etapa nunca terminou de rodar) e uma
                // evidencia fraca, mesmo que o tipo seja Timeout -- so vira ALTA quando a
                // propria sondagem estourou seu timeout individual de verdade.
                outcome.gatewayReachable is ProbeResult.Timeout && outcome.globalTimeoutExceeded -> NivelConfianca.BAIXA
                outcome.gatewayReachable is ProbeResult.Failure || outcome.gatewayReachable is ProbeResult.Timeout -> NivelConfianca.ALTA
                else -> NivelConfianca.BAIXA // NotExecuted/Unavailable — evidencia fraca
            }
            return ConnectivityStatusResolution(ConnectivityStatus.GATEWAY_UNREACHABLE, confianca)
        }

        val dnsSucesso = outcome.dnsReachable is ProbeResult.Success
        if (!outcome.dnsConfigured || !dnsSucesso) {
            val confianca = when {
                !outcome.dnsConfigured -> NivelConfianca.MEDIA
                outcome.dnsReachable is ProbeResult.Timeout && outcome.globalTimeoutExceeded -> NivelConfianca.BAIXA
                outcome.dnsReachable is ProbeResult.Failure || outcome.dnsReachable is ProbeResult.Timeout -> NivelConfianca.ALTA
                else -> NivelConfianca.BAIXA
            }
            return ConnectivityStatusResolution(ConnectivityStatus.DNS_FAILURE, confianca)
        }

        val externoOk = outcome.externalIpReachable is ProbeResult.Success
        val hostnameOk = outcome.hostnameReachable is ProbeResult.Success

        if (externoOk && hostnameOk) {
            return ConnectivityStatusResolution(ConnectivityStatus.INTERNET_AVAILABLE, NivelConfianca.ALTA)
        }

        if (externoOk != hostnameOk) {
            // uma sondagem confirmou alcance, a outra nao -- sinal misto, nao internet plena
            return ConnectivityStatusResolution(ConnectivityStatus.PARTIAL_CONNECTIVITY, NivelConfianca.MEDIA)
        }

        // nem IP externo nem hostname confirmaram alcance
        val ambasFalharamAtivamente =
            outcome.externalIpReachable is ProbeResult.Failure && outcome.hostnameReachable is ProbeResult.Failure
        if (ambasFalharamAtivamente) {
            return ConnectivityStatusResolution(ConnectivityStatus.EXTERNAL_ROUTE_FAILURE, NivelConfianca.ALTA)
        }

        // sondagens externas nao concluiram (timeout/not-executed/unavailable) em vez de falhar
        // ativamente -- se o proprio Android ja confirma "sem internet validada", ainda assim
        // e uma conclusao honesta de "wifi sem internet", so que com confianca media (nao temos
        // as duas sondagens ativas confirmando a causa exata).
        if (outcome.androidInternetCapability && !outcome.androidValidated) {
            return ConnectivityStatusResolution(ConnectivityStatus.WIFI_WITHOUT_INTERNET, NivelConfianca.MEDIA)
        }

        return ConnectivityStatusResolution(ConnectivityStatus.INCONCLUSIVE, NivelConfianca.BAIXA)
    }
}
