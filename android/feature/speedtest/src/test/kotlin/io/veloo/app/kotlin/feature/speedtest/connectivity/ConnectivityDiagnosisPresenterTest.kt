package io.signallq.app.feature.speedtest.connectivity

import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.contracts.connectivity.ConnectivityDiagnosis
import io.signallq.app.core.network.contracts.connectivity.ConnectivityStatus
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import io.signallq.app.core.network.contracts.topologia.NivelConfianca
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectivityDiagnosisPresenterTest {

    private fun diagnostico(status: ConnectivityStatus) = ConnectivityDiagnosis(
        transport = EstadoConexao.wifi,
        wifiConnected = true,
        localAddressAvailable = true,
        gatewayConfigured = true,
        gatewayReachable = ProbeResult.Success(),
        dnsConfigured = true,
        dnsReachable = ProbeResult.Success(),
        externalIpReachable = ProbeResult.Success(),
        hostnameReachable = ProbeResult.Success(),
        androidInternetCapability = true,
        androidValidated = true,
        captivePortalDetected = false,
        mobileFallbackAvailable = false,
        status = status,
        confidence = NivelConfianca.ALTA,
        evidence = emptyList(),
        startedAtEpochMs = 0,
        finishedAtEpochMs = 1,
    )

    @Test
    fun `wifi sem internet usa texto honesto e nao acusa a operadora`() {
        val mensagem = ConnectivityDiagnosisPresenter.apresentar(diagnostico(ConnectivityStatus.WIFI_WITHOUT_INTERNET))
        assertEquals(
            "Você está conectado ao Wi-Fi, mas essa rede não está conseguindo acessar a internet.",
            mensagem.mensagem,
        )
        assertTrue("nao deve afirmar que a operadora esta fora do ar", !mensagem.mensagem.contains("operadora", ignoreCase = true))
    }

    @Test
    fun `captive portal sugere apenas abrir o portal de login`() {
        val mensagem = ConnectivityDiagnosisPresenter.apresentar(diagnostico(ConnectivityStatus.CAPTIVE_PORTAL))
        assertEquals(listOf(ConnectivityAction.ABRIR_PORTAL_LOGIN), mensagem.acoes)
    }

    @Test
    fun `inconclusivo admite honestamente que a causa nao foi identificada`() {
        val mensagem = ConnectivityDiagnosisPresenter.apresentar(diagnostico(ConnectivityStatus.INCONCLUSIVE))
        assertTrue(mensagem.mensagem.contains("não foi possível identificar a causa"))
    }

    @Test
    fun `internet disponivel nao sugere nenhuma acao`() {
        val mensagem = ConnectivityDiagnosisPresenter.apresentar(diagnostico(ConnectivityStatus.INTERNET_AVAILABLE))
        assertTrue(mensagem.acoes.isEmpty())
    }
}
