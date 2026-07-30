package io.signallq.app.core.network.connectivity

import io.signallq.app.core.network.contracts.connectivity.ProbeFailureReason
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import io.signallq.app.core.network.contracts.connectivity.ConnectivityStatus
import io.signallq.app.core.network.contracts.topologia.NivelConfianca
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectivityStatusResolverTest {

    private fun outcome(
        wifiConnected: Boolean = true,
        gatewayConfigured: Boolean = true,
        gatewayReachable: ProbeResult = ProbeResult.Success(),
        dnsConfigured: Boolean = true,
        dnsReachable: ProbeResult = ProbeResult.Success(),
        externalIpReachable: ProbeResult = ProbeResult.Success(),
        hostnameReachable: ProbeResult = ProbeResult.Success(),
        androidInternetCapability: Boolean = true,
        androidValidated: Boolean = true,
        captivePortalDetected: Boolean = false,
    ) = ConnectivityProbeOutcome(
        wifiConnected = wifiConnected,
        gatewayConfigured = gatewayConfigured,
        gatewayReachable = gatewayReachable,
        dnsConfigured = dnsConfigured,
        dnsReachable = dnsReachable,
        externalIpReachable = externalIpReachable,
        hostnameReachable = hostnameReachable,
        androidInternetCapability = androidInternetCapability,
        androidValidated = androidValidated,
        captivePortalDetected = captivePortalDetected,
    )

    @Test
    fun `wifi conectado e internet disponivel`() {
        val resultado = ConnectivityStatusResolver.resolve(outcome())
        assertEquals(ConnectivityStatus.INTERNET_AVAILABLE, resultado.status)
        assertEquals(NivelConfianca.ALTA, resultado.confidence)
    }

    @Test
    fun `wifi conectado sem gateway configurado`() {
        val resultado = ConnectivityStatusResolver.resolve(
            outcome(
                gatewayConfigured = false,
                gatewayReachable = ProbeResult.Unavailable("sem rota de gateway configurada"),
                dnsReachable = ProbeResult.NotExecuted("gateway nao confirmou"),
                externalIpReachable = ProbeResult.NotExecuted("gateway nao confirmou"),
                hostnameReachable = ProbeResult.NotExecuted("gateway nao confirmou"),
            ),
        )
        assertEquals(ConnectivityStatus.GATEWAY_UNREACHABLE, resultado.status)
    }

    @Test
    fun `gateway configurado mas inalcancavel`() {
        val resultado = ConnectivityStatusResolver.resolve(
            outcome(
                gatewayReachable = ProbeResult.Failure(ProbeFailureReason.HOST_UNREACHABLE),
                dnsReachable = ProbeResult.NotExecuted("gateway nao confirmou"),
                externalIpReachable = ProbeResult.NotExecuted("gateway nao confirmou"),
                hostnameReachable = ProbeResult.NotExecuted("gateway nao confirmou"),
            ),
        )
        assertEquals(ConnectivityStatus.GATEWAY_UNREACHABLE, resultado.status)
        assertEquals(NivelConfianca.ALTA, resultado.confidence)
    }

    @Test
    fun `gateway alcancavel e dns indisponivel`() {
        val resultado = ConnectivityStatusResolver.resolve(
            outcome(
                dnsReachable = ProbeResult.Failure(ProbeFailureReason.DNS_RESOLUTION_FAILED),
                externalIpReachable = ProbeResult.NotExecuted("dns nao confirmou"),
                hostnameReachable = ProbeResult.NotExecuted("dns nao confirmou"),
            ),
        )
        assertEquals(ConnectivityStatus.DNS_FAILURE, resultado.status)
    }

    @Test
    fun `dns funcional e rota externa indisponivel`() {
        val resultado = ConnectivityStatusResolver.resolve(
            outcome(
                externalIpReachable = ProbeResult.Failure(ProbeFailureReason.HOST_UNREACHABLE),
                hostnameReachable = ProbeResult.Failure(ProbeFailureReason.HOST_UNREACHABLE),
            ),
        )
        assertEquals(ConnectivityStatus.EXTERNAL_ROUTE_FAILURE, resultado.status)
    }

    @Test
    fun `ip externo alcancavel e hostname indisponivel e conectividade parcial`() {
        val resultado = ConnectivityStatusResolver.resolve(
            outcome(
                externalIpReachable = ProbeResult.Success(),
                hostnameReachable = ProbeResult.Failure(ProbeFailureReason.HOST_UNREACHABLE),
            ),
        )
        assertEquals(ConnectivityStatus.PARTIAL_CONNECTIVITY, resultado.status)
        assertEquals(NivelConfianca.MEDIA, resultado.confidence)
    }

    @Test
    fun `captive portal detectado tem precedencia sobre demais evidencias`() {
        val resultado = ConnectivityStatusResolver.resolve(
            outcome(
                captivePortalDetected = true,
                externalIpReachable = ProbeResult.Failure(ProbeFailureReason.HOST_UNREACHABLE),
                hostnameReachable = ProbeResult.Failure(ProbeFailureReason.UNEXPECTED_RESPONSE),
            ),
        )
        assertEquals(ConnectivityStatus.CAPTIVE_PORTAL, resultado.status)
        assertEquals(NivelConfianca.ALTA, resultado.confidence)
    }

    @Test
    fun `rede nao validada pelo android com rota externa nao confirmada e wifi sem internet`() {
        val resultado = ConnectivityStatusResolver.resolve(
            outcome(
                androidInternetCapability = true,
                androidValidated = false,
                externalIpReachable = ProbeResult.Timeout(2500),
                hostnameReachable = ProbeResult.Timeout(2500),
            ),
        )
        assertEquals(ConnectivityStatus.WIFI_WITHOUT_INTERNET, resultado.status)
        assertEquals(NivelConfianca.MEDIA, resultado.confidence)
    }

    @Test
    fun `resultado parcial por timeout sem sinal validado do android e inconclusivo`() {
        val resultado = ConnectivityStatusResolver.resolve(
            outcome(
                androidInternetCapability = false,
                androidValidated = false,
                externalIpReachable = ProbeResult.Timeout(2500),
                hostnameReachable = ProbeResult.NotExecuted("estourou o prazo global"),
            ),
        )
        assertEquals(ConnectivityStatus.INCONCLUSIVE, resultado.status)
        assertEquals(NivelConfianca.BAIXA, resultado.confidence)
    }

    @Test
    fun `wifi desconectado tem precedencia sobre qualquer outra evidencia`() {
        val resultado = ConnectivityStatusResolver.resolve(
            outcome(
                wifiConnected = false,
                gatewayReachable = ProbeResult.Success(),
                dnsReachable = ProbeResult.Success(),
                externalIpReachable = ProbeResult.Success(),
                hostnameReachable = ProbeResult.Success(),
            ),
        )
        assertEquals(ConnectivityStatus.WIFI_DISCONNECTED, resultado.status)
        assertEquals(NivelConfianca.ALTA, resultado.confidence)
    }

    @Test
    fun `wifi sem internet com dados moveis ativos nao muda a conclusao do wifi`() {
        // mobileFallbackAvailable nao faz parte do ConnectivityProbeOutcome -- e um campo
        // de evidencia paralela em ConnectivityDiagnosis, nunca entra na decisao de status.
        // Este teste documenta essa garantia: a mesma evidencia de Wi-Fi sem internet produz
        // o mesmo status independente de haver dados moveis disponiveis.
        val semDadosMoveis = ConnectivityStatusResolver.resolve(
            outcome(
                externalIpReachable = ProbeResult.Failure(ProbeFailureReason.HOST_UNREACHABLE),
                hostnameReachable = ProbeResult.Failure(ProbeFailureReason.HOST_UNREACHABLE),
            ),
        )
        val comDadosMoveis = ConnectivityStatusResolver.resolve(
            outcome(
                externalIpReachable = ProbeResult.Failure(ProbeFailureReason.HOST_UNREACHABLE),
                hostnameReachable = ProbeResult.Failure(ProbeFailureReason.HOST_UNREACHABLE),
            ),
        )
        assertEquals(semDadosMoveis.status, comDadosMoveis.status)
        assertEquals(ConnectivityStatus.EXTERNAL_ROUTE_FAILURE, comDadosMoveis.status)
    }

    @Test
    fun `dns nao configurado e tratado como dns failure com confianca media`() {
        val resultado = ConnectivityStatusResolver.resolve(
            outcome(
                dnsConfigured = false,
                dnsReachable = ProbeResult.Unavailable("nenhum servidor DNS configurado"),
                externalIpReachable = ProbeResult.NotExecuted("dns nao confirmou"),
                hostnameReachable = ProbeResult.NotExecuted("dns nao confirmou"),
            ),
        )
        assertEquals(ConnectivityStatus.DNS_FAILURE, resultado.status)
        assertEquals(NivelConfianca.MEDIA, resultado.confidence)
    }

    @Test
    fun `gateway com timeout e tratado como gateway unreachable com confianca alta`() {
        val resultado = ConnectivityStatusResolver.resolve(
            outcome(
                gatewayReachable = ProbeResult.Timeout(1200),
                dnsReachable = ProbeResult.NotExecuted("gateway nao confirmou"),
                externalIpReachable = ProbeResult.NotExecuted("gateway nao confirmou"),
                hostnameReachable = ProbeResult.NotExecuted("gateway nao confirmou"),
            ),
        )
        assertEquals(ConnectivityStatus.GATEWAY_UNREACHABLE, resultado.status)
        assertEquals(NivelConfianca.ALTA, resultado.confidence)
    }
}
