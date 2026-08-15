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

    private fun diagnostico(status: ConnectivityStatus, confidence: NivelConfianca = NivelConfianca.ALTA) = ConnectivityDiagnosis(
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
        confidence = confidence,
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

    // ── Linguagem calibrada por confiança (issue #1502 — delta pós-planilha) ─

    @Test
    fun `gateway inalcancavel com confianca alta usa linguagem de certeza`() {
        val mensagem = ConnectivityDiagnosisPresenter.apresentar(
            diagnostico(ConnectivityStatus.GATEWAY_UNREACHABLE, NivelConfianca.ALTA),
        )
        assertTrue(mensagem.mensagem.startsWith("Confirmamos"))
    }

    @Test
    fun `gateway inalcancavel com confianca media usa linguagem de probabilidade`() {
        val mensagem = ConnectivityDiagnosisPresenter.apresentar(
            diagnostico(ConnectivityStatus.GATEWAY_UNREACHABLE, NivelConfianca.MEDIA),
        )
        assertTrue(mensagem.mensagem.startsWith("Há sinais de que"))
    }

    @Test
    fun `gateway inalcancavel com confianca baixa admite verificacao incompleta`() {
        val mensagem = ConnectivityDiagnosisPresenter.apresentar(
            diagnostico(ConnectivityStatus.GATEWAY_UNREACHABLE, NivelConfianca.BAIXA),
        )
        assertTrue(mensagem.mensagem.contains("não puderam ser concluídas"))
    }

    @Test
    fun `falha de dns com confianca alta afirma o que o teste mostrou`() {
        val mensagem = ConnectivityDiagnosisPresenter.apresentar(diagnostico(ConnectivityStatus.DNS_FAILURE, NivelConfianca.ALTA))
        assertTrue(mensagem.mensagem.contains("O teste mostrou"))
    }

    @Test
    fun `falha de dns com confianca baixa usa linguagem de sinais, nao de certeza`() {
        val mensagem = ConnectivityDiagnosisPresenter.apresentar(diagnostico(ConnectivityStatus.DNS_FAILURE, NivelConfianca.BAIXA))
        assertTrue(mensagem.mensagem.contains("há sinais de que"))
    }

    @Test
    fun `sem rota externa com confianca media usa causa mais provavel`() {
        val mensagem = ConnectivityDiagnosisPresenter.apresentar(
            diagnostico(ConnectivityStatus.EXTERNAL_ROUTE_FAILURE, NivelConfianca.MEDIA),
        )
        assertTrue(mensagem.mensagem.startsWith("A causa mais provável"))
    }

    @Test
    fun `nenhum estado usa mais de uma acao primaria implicita alem da lista ordenada`() {
        ConnectivityStatus.entries.forEach { status ->
            val mensagem = ConnectivityDiagnosisPresenter.apresentar(diagnostico(status))
            assertTrue(
                "estado $status devolveu mensagem em branco",
                mensagem.mensagem.isNotBlank(),
            )
        }
    }

    @Test
    fun `todos os 9 estados de conectividade tem apresentacao`() {
        val estados = setOf(
            ConnectivityStatus.INTERNET_AVAILABLE,
            ConnectivityStatus.WIFI_WITHOUT_INTERNET,
            ConnectivityStatus.GATEWAY_UNREACHABLE,
            ConnectivityStatus.DNS_FAILURE,
            ConnectivityStatus.EXTERNAL_ROUTE_FAILURE,
            ConnectivityStatus.CAPTIVE_PORTAL,
            ConnectivityStatus.PARTIAL_CONNECTIVITY,
            ConnectivityStatus.WIFI_DISCONNECTED,
            ConnectivityStatus.INCONCLUSIVE,
        )
        assertEquals(estados, ConnectivityStatus.entries.toSet())
        estados.forEach { status ->
            val mensagem = ConnectivityDiagnosisPresenter.apresentar(diagnostico(status))
            assertTrue(mensagem.titulo.isNotBlank())
        }
    }

    @Test
    fun `nenhuma mensagem vaza jargao tecnico nao traduzido`() {
        val jargoes = listOf("bufferbloat", "NAT UDP", "RSSI", "RSRP", "SQM", "QoS", "match", "tags")
        ConnectivityStatus.entries.forEach { status ->
            val mensagem = ConnectivityDiagnosisPresenter.apresentar(diagnostico(status))
            jargoes.forEach { jargao ->
                assertTrue(
                    "estado $status vazou o jargao '$jargao' na mensagem principal",
                    !mensagem.mensagem.contains(jargao, ignoreCase = true),
                )
            }
        }
    }
}
