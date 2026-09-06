package io.signallq.app.feature.speedtest.connectivity

import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.contracts.connectivity.ConnectivityDiagnosis
import io.signallq.app.core.network.contracts.connectivity.ConnectivityStatus
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import io.signallq.app.core.network.contracts.topologia.NivelConfianca
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testa [indicaAusenciaDeInternetParaBloquearSpeedtest] isoladamente (GH#1512, 3ª revisão
 * adversarial -- a política de bloqueio do Speedtest não tinha nenhum teste direto).
 */
class ConnectivityBlockingPolicyTest {
    private fun diagnostico(
        status: ConnectivityStatus,
        confidence: NivelConfianca = NivelConfianca.ALTA,
        androidInternetCapability: Boolean = true,
        androidValidated: Boolean = false,
    ) = ConnectivityDiagnosis(
        transport = EstadoConexao.wifi,
        wifiConnected = true,
        localAddressAvailable = true,
        gatewayConfigured = true,
        gatewayReachable = ProbeResult.Success(),
        dnsConfigured = true,
        dnsReachable = ProbeResult.Success(),
        externalIpReachable = ProbeResult.Success(),
        hostnameReachable = ProbeResult.Success(),
        androidInternetCapability = androidInternetCapability,
        androidValidated = androidValidated,
        captivePortalDetected = false,
        mobileFallbackAvailable = false,
        status = status,
        confidence = confidence,
        evidence = emptyList(),
        startedAtEpochMs = 0L,
        finishedAtEpochMs = 0L,
    )

    @Test
    fun `wifi sem internet sempre bloqueia independente da confianca`() {
        assertTrue(diagnostico(ConnectivityStatus.WIFI_WITHOUT_INTERNET, confidence = NivelConfianca.MEDIA).indicaAusenciaDeInternetParaBloquearSpeedtest())
        assertTrue(diagnostico(ConnectivityStatus.WIFI_WITHOUT_INTERNET, confidence = NivelConfianca.BAIXA).indicaAusenciaDeInternetParaBloquearSpeedtest())
    }

    @Test
    fun `rota externa indisponivel bloqueia`() {
        assertTrue(diagnostico(ConnectivityStatus.EXTERNAL_ROUTE_FAILURE).indicaAusenciaDeInternetParaBloquearSpeedtest())
    }

    @Test
    fun `captive portal bloqueia`() {
        assertTrue(diagnostico(ConnectivityStatus.CAPTIVE_PORTAL).indicaAusenciaDeInternetParaBloquearSpeedtest())
    }

    @Test
    fun `dns failure com confianca alta bloqueia`() {
        assertTrue(diagnostico(ConnectivityStatus.DNS_FAILURE, confidence = NivelConfianca.ALTA).indicaAusenciaDeInternetParaBloquearSpeedtest())
    }

    @Test
    fun `dns failure com confianca media -- sem servidor configurado, zero sondagem -- nao bloqueia`() {
        // GH#1512 (3a revisao, achado de revisao) -- caso concreto que motivou a correcao:
        // ausencia de configuracao de DNS produz confianca MEDIA sem nenhuma sondagem
        // ativa ter rodado; nao e evidencia forte o suficiente para negar o Speedtest.
        assertFalse(diagnostico(ConnectivityStatus.DNS_FAILURE, confidence = NivelConfianca.MEDIA).indicaAusenciaDeInternetParaBloquearSpeedtest())
    }

    @Test
    fun `gateway unreachable com confianca alta e android corroborando bloqueia`() {
        assertTrue(
            diagnostico(
                ConnectivityStatus.GATEWAY_UNREACHABLE,
                confidence = NivelConfianca.ALTA,
                androidInternetCapability = true,
                androidValidated = false,
            ).indicaAusenciaDeInternetParaBloquearSpeedtest(),
        )
    }

    @Test
    fun `gateway unreachable com confianca alta mas android validando a rede nao bloqueia`() {
        // Sem corroboracao do proprio Android, um proxy TCP isolado (portas 53_80_443 do
        // roteador) nao e evidencia suficiente -- roteador legitimamente saudavel pode
        // simplesmente nao aceitar essas sondagens.
        assertFalse(
            diagnostico(
                ConnectivityStatus.GATEWAY_UNREACHABLE,
                confidence = NivelConfianca.ALTA,
                androidInternetCapability = true,
                androidValidated = true,
            ).indicaAusenciaDeInternetParaBloquearSpeedtest(),
        )
    }

    @Test
    fun `gateway unreachable com confianca alta e capacidade de internet do android ausente tambem bloqueia`() {
        // GH#1512 (4a revisao, achado de revisao) -- getNetworkCapabilities() devolvendo
        // null (rede sumiu no meio da checagem) zera os DOIS flags do Android
        // (androidInternetCapability=false, androidValidated=false). Isso e "nenhum sinal
        // do Android", nao "Android discordando" -- a condicao anterior (que exigia
        // androidInternetCapability=true) excluia esse caso por engano.
        assertTrue(
            diagnostico(
                ConnectivityStatus.GATEWAY_UNREACHABLE,
                confidence = NivelConfianca.ALTA,
                androidInternetCapability = false,
                androidValidated = false,
            ).indicaAusenciaDeInternetParaBloquearSpeedtest(),
        )
    }

    @Test
    fun `gateway unreachable com confianca media nao bloqueia mesmo com corroboracao`() {
        assertFalse(
            diagnostico(
                ConnectivityStatus.GATEWAY_UNREACHABLE,
                confidence = NivelConfianca.MEDIA,
                androidInternetCapability = true,
                androidValidated = false,
            ).indicaAusenciaDeInternetParaBloquearSpeedtest(),
        )
    }

    @Test
    fun `internet available nunca bloqueia`() {
        assertFalse(diagnostico(ConnectivityStatus.INTERNET_AVAILABLE).indicaAusenciaDeInternetParaBloquearSpeedtest())
    }

    @Test
    fun `partial connectivity nao bloqueia`() {
        assertFalse(diagnostico(ConnectivityStatus.PARTIAL_CONNECTIVITY).indicaAusenciaDeInternetParaBloquearSpeedtest())
    }

    @Test
    fun `inconclusive nao bloqueia`() {
        assertFalse(diagnostico(ConnectivityStatus.INCONCLUSIVE).indicaAusenciaDeInternetParaBloquearSpeedtest())
    }

    @Test
    fun `wifi disconnected nao bloqueia`() {
        assertFalse(diagnostico(ConnectivityStatus.WIFI_DISCONNECTED).indicaAusenciaDeInternetParaBloquearSpeedtest())
    }
}
