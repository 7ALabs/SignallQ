package io.signallq.app.core.network.connectivity

import io.signallq.app.core.network.contracts.connectivity.ConnectivityStatus
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectivityDiagnosisEngineTest {

    private fun contexto(
        wifiConnected: Boolean = true,
        gatewayIp: String? = "192.168.0.1",
        dnsServers: List<String> = listOf("8.8.8.8"),
        androidInternetCapability: Boolean = true,
        androidValidated: Boolean = true,
        androidCaptivePortalCapability: Boolean = false,
        mobileFallbackAvailable: Boolean = false,
    ) = ConnectivityDiagnosisContext(
        wifiConnected = wifiConnected,
        localAddressAvailable = true,
        gatewayIp = gatewayIp,
        dnsServers = dnsServers,
        androidInternetCapability = androidInternetCapability,
        androidValidated = androidValidated,
        androidCaptivePortalCapability = androidCaptivePortalCapability,
        mobileFallbackAvailable = mobileFallbackAvailable,
    )

    private fun engineFeliz(
        stepTimeoutMs: Long = 2500,
        globalTimeoutMs: Long = 8000,
        gatewayProbe: GatewayProbe = GatewayProbe { ProbeResult.Success() },
        dnsProbe: DnsProbe = DnsProbe { ProbeResult.Success() },
        externalIpProbe: ExternalIpProbe = ExternalIpProbe { ProbeResult.Success() },
        hostnameProbe: HostnameProbe = HostnameProbe { HostnameProbeOutcome(ProbeResult.Success(), false) },
    ) = ConnectivityDiagnosisEngine(
        gatewayProbe = gatewayProbe,
        dnsProbe = dnsProbe,
        externalIpProbe = externalIpProbe,
        hostnameProbe = hostnameProbe,
        stepTimeoutMs = stepTimeoutMs,
        globalTimeoutMs = globalTimeoutMs,
    )

    @Test
    fun `wifi sem internet com dados moveis ativos produz o mesmo status independente do fallback`() = runTest {
        // GH#1512 -- teste real (nao tautologico): mobileFallbackAvailable e o UNICO campo
        // que varia entre as duas execucoes; o motor completo roda dos dois lados.
        val engine = engineFeliz(
            externalIpProbe = ExternalIpProbe { ProbeResult.Failure(io.signallq.app.core.network.contracts.connectivity.ProbeFailureReason.HOST_UNREACHABLE) },
            hostnameProbe = HostnameProbe {
                HostnameProbeOutcome(ProbeResult.Failure(io.signallq.app.core.network.contracts.connectivity.ProbeFailureReason.HOST_UNREACHABLE), false)
            },
        )

        val semDadosMoveis = engine.diagnosticar(contexto(mobileFallbackAvailable = false))
        val comDadosMoveis = engine.diagnosticar(contexto(mobileFallbackAvailable = true))

        assertEquals(semDadosMoveis.status, comDadosMoveis.status)
        assertEquals(ConnectivityStatus.EXTERNAL_ROUTE_FAILURE, comDadosMoveis.status)
        assertEquals(semDadosMoveis.confidence, comDadosMoveis.confidence)
        // mobileFallbackAvailable e preservado como evidencia paralela (para a UI oferecer
        // "testar em outra rede"), mas nunca influencia a conclusao acima.
        assertTrue(comDadosMoveis.mobileFallbackAvailable)
        assertTrue(!semDadosMoveis.mobileFallbackAvailable)
    }

    @Test
    fun `cadeia completa de sucesso produz internet disponivel e evidencias das quatro etapas`() = runTest {
        val engine = engineFeliz()
        val diagnostico = engine.diagnosticar(contexto())

        assertEquals(ConnectivityStatus.INTERNET_AVAILABLE, diagnostico.status)
        assertEquals(4, diagnostico.evidence.size)
        assertTrue(diagnostico.gatewayReachable is ProbeResult.Success)
        assertTrue(diagnostico.dnsReachable is ProbeResult.Success)
        assertTrue(diagnostico.externalIpReachable is ProbeResult.Success)
        assertTrue(diagnostico.hostnameReachable is ProbeResult.Success)
    }

    @Test
    fun `wifi desconectado nao chama nenhuma sonda`() = runTest {
        var gatewayChamado = false
        val engine = engineFeliz(gatewayProbe = GatewayProbe { gatewayChamado = true; ProbeResult.Success() })

        val diagnostico = engine.diagnosticar(contexto(wifiConnected = false))

        assertEquals(ConnectivityStatus.WIFI_DISCONNECTED, diagnostico.status)
        assertTrue("nenhuma sonda deveria ser chamada quando o wifi esta desconectado", !gatewayChamado)
    }

    @Test
    fun `falha no gateway impede chamadas de dns e rota externa`() = runTest {
        var dnsChamado = false
        var externalIpChamado = false
        val engine = engineFeliz(
            gatewayProbe = GatewayProbe { ProbeResult.Failure(io.signallq.app.core.network.contracts.connectivity.ProbeFailureReason.HOST_UNREACHABLE) },
            dnsProbe = DnsProbe { dnsChamado = true; ProbeResult.Success() },
            externalIpProbe = ExternalIpProbe { externalIpChamado = true; ProbeResult.Success() },
        )

        val diagnostico = engine.diagnosticar(contexto())

        assertEquals(ConnectivityStatus.GATEWAY_UNREACHABLE, diagnostico.status)
        assertTrue("DNS nao deveria ser sondado apos falha do gateway", !dnsChamado)
        assertTrue("rota externa nao deveria ser sondada apos falha do gateway", !externalIpChamado)
        assertTrue(diagnostico.dnsReachable is ProbeResult.NotExecuted)
        assertTrue(diagnostico.externalIpReachable is ProbeResult.NotExecuted)
    }

    @Test
    fun `sonda que excede o timeout do passo vira Timeout explicito`() = runTest {
        val engine = engineFeliz(
            stepTimeoutMs = 50,
            gatewayProbe = GatewayProbe {
                delay(5_000)
                ProbeResult.Success()
            },
        )

        val diagnostico = engine.diagnosticar(contexto())

        assertEquals(ConnectivityStatus.GATEWAY_UNREACHABLE, diagnostico.status)
        assertTrue(diagnostico.gatewayReachable is ProbeResult.Timeout)
    }

    @Test
    fun `timeout global marca etapas nao concluidas como Timeout em vez de NotExecuted`() = runTest {
        val engine = engineFeliz(
            stepTimeoutMs = 10_000, // maior que o global -- forca o teto global a agir primeiro
            globalTimeoutMs = 50,
            gatewayProbe = GatewayProbe {
                delay(5_000)
                ProbeResult.Success()
            },
        )

        val diagnostico = engine.diagnosticar(contexto())

        assertTrue(diagnostico.gatewayReachable is ProbeResult.Timeout)
        assertTrue(diagnostico.dnsReachable is ProbeResult.Timeout)
        assertTrue(diagnostico.externalIpReachable is ProbeResult.Timeout)
        assertTrue(diagnostico.hostnameReachable is ProbeResult.Timeout)
        // GH#1512 (achado de revisao) -- uma etapa nunca alcancada nao pode virar
        // evidencia de confianca ALTA so porque o rotulo e "Timeout".
        assertEquals(io.signallq.app.core.network.contracts.topologia.NivelConfianca.BAIXA, diagnostico.confidence)
    }

    @Test
    fun `cancelamento durante sondagem interrompe o diagnostico sem produzir resultado`() = runTest {
        var chegouNoDns = false
        val engine = engineFeliz(
            stepTimeoutMs = 60_000,
            globalTimeoutMs = 60_000,
            dnsProbe = DnsProbe {
                chegouNoDns = true
                delay(60_000) // fica "em sondagem" ate o job pai ser cancelado
                ProbeResult.Success()
            },
        )

        var completouNormalmente = false
        var foiCancelado = false
        val job: Job = launch {
            try {
                engine.diagnosticar(contexto())
                completouNormalmente = true
            } catch (_: CancellationException) {
                foiCancelado = true
            }
        }

        // roda so ate a proxima suspensao (delay dentro do dnsProbe), sem avancar o
        // relogio virtual ate o fim do delay -- garante que a sondagem ainda esta "em
        // andamento" no momento em que cancelamos, em vez de deixa-la completar.
        runCurrent()
        job.cancelAndJoin()

        assertTrue("dns deveria ter sido alcancado antes do cancelamento", chegouNoDns)
        assertTrue("diagnostico nao deveria completar normalmente apos cancelamento", !completouNormalmente)
        assertTrue("cancelamento deveria propagar CancellationException", foiCancelado)
    }
}
