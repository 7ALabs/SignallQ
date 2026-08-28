package io.signallq.app.diagnosticooffline

import io.mockk.mockk
import io.signallq.app.core.network.connectivity.ConnectivityProbeBinding
import io.signallq.app.core.network.connectivity.HostnameProbeOutcome
import io.signallq.app.core.network.contracts.connectivity.ProbeFailureReason
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre a lógica de mapeamento/diferenciação do wiring real (issue #1811, Task 4/4) sem
 * depender de `android.net.Network`/Robolectric -- [ContextoRedeDiagnosticoOffline] e as
 * fábricas de sondagem são injetadas com fakes puros JVM (mesmo padrão de teste de
 * `ConnectivityDiagnosisEngineTest`).
 */
class DiagnosticoOfflineExecutorRealTest {
    private val bindingFake = mockk<ConnectivityProbeBinding>(relaxed = true)

    private fun criarExecutor(
        contexto: ContextoRedeDiagnosticoOffline? =
            ContextoRedeDiagnosticoOffline(
                binding = bindingFake,
                gatewayIp = "192.168.0.1",
                dnsServers = listOf("192.168.0.1"),
            ),
        gateway: ProbeResult = ProbeResult.Success(),
        dnsRede: ProbeResult = ProbeResult.Success(),
        doh: ProbeResult = ProbeResult.Success(),
        rotaExterna: ProbeResult = ProbeResult.Success(),
        hostname: HostnameProbeOutcome = HostnameProbeOutcome(ProbeResult.Success(), captivePortalSuspeito = false),
    ) = DiagnosticoOfflineExecutorReal(
        context = mockk(relaxed = true),
        criarGatewayProbe = { { gateway } },
        criarDnsProbe = { { dnsRede } },
        criarDohProbe = { { doh } },
        criarExternalIpProbe = { { rotaExterna } },
        criarHostnameProbe = { { hostname } },
        obterContexto = { contexto },
    )

    @Test
    fun `sem rede wifi capturavel qualquer etapa falha com motivo claro`() =
        runTest {
            val executor = criarExecutor(contexto = null)

            val resultado = executor.executar(EtapaDiagnosticoOffline.GATEWAY) as ResultadoEtapaDiagnosticoOffline.Falha

            assertEquals(EtapaDiagnosticoOffline.GATEWAY, resultado.etapa)
            assertEquals("sem rede Wi-Fi ativa para diagnosticar", resultado.motivo)
        }

    @Test
    fun `gateway sem ip configurado falha sem chamar a sonda`() =
        runTest {
            val contexto = ContextoRedeDiagnosticoOffline(bindingFake, gatewayIp = null, dnsServers = emptyList())
            val executor = criarExecutor(contexto = contexto)

            val resultado = executor.executar(EtapaDiagnosticoOffline.GATEWAY) as ResultadoEtapaDiagnosticoOffline.Falha

            assertEquals("gateway não configurado nesta rede", resultado.motivo)
        }

    @Test
    fun `gateway alcancavel produz sucesso`() =
        runTest {
            val executor = criarExecutor(gateway = ProbeResult.Success(42))

            val resultado = executor.executar(EtapaDiagnosticoOffline.GATEWAY)

            assertTrue(resultado is ResultadoEtapaDiagnosticoOffline.Sucesso)
            assertEquals(EtapaDiagnosticoOffline.GATEWAY, resultado.etapa)
        }

    @Test
    fun `gateway com timeout produz falha com motivo de timeout`() =
        runTest {
            val executor = criarExecutor(gateway = ProbeResult.Timeout(1200))

            val resultado = executor.executar(EtapaDiagnosticoOffline.GATEWAY) as ResultadoEtapaDiagnosticoOffline.Falha

            assertEquals("sem resposta dentro do tempo limite", resultado.motivo)
        }

    @Test
    fun `dns sem servidor configurado falha sem chamar nenhuma sonda`() =
        runTest {
            val contexto = ContextoRedeDiagnosticoOffline(bindingFake, gatewayIp = "192.168.0.1", dnsServers = emptyList())
            val executor = criarExecutor(contexto = contexto)

            val resultado = executor.executar(EtapaDiagnosticoOffline.DNS) as ResultadoEtapaDiagnosticoOffline.Falha

            assertEquals("nenhum servidor DNS configurado nesta rede", resultado.motivo)
        }

    @Test
    fun `dns da rede resolve produz sucesso sem precisar do fallback DoH`() =
        runTest {
            val executor = criarExecutor(dnsRede = ProbeResult.Success())

            val resultado = executor.executar(EtapaDiagnosticoOffline.DNS)

            assertTrue(resultado is ResultadoEtapaDiagnosticoOffline.Sucesso)
        }

    @Test
    fun `dns da rede falha mas DoH resolve diferencia resolvedor da rede quebrado`() =
        runTest {
            val executor =
                criarExecutor(
                    dnsRede = ProbeResult.Failure(ProbeFailureReason.DNS_RESOLUTION_FAILED),
                    doh = ProbeResult.Success(),
                )

            val resultado = executor.executar(EtapaDiagnosticoOffline.DNS) as ResultadoEtapaDiagnosticoOffline.Falha

            assertTrue(resultado.motivo?.contains("DoH") == true)
            assertTrue(resultado.motivo?.contains("resolvedor DNS desta rede") == true)
        }

    @Test
    fun `dns da rede falha e DoH tambem falha indica ausencia de resolucao total`() =
        runTest {
            val executor =
                criarExecutor(
                    dnsRede = ProbeResult.Failure(ProbeFailureReason.DNS_RESOLUTION_FAILED),
                    doh = ProbeResult.Failure(ProbeFailureReason.HOST_UNREACHABLE),
                )

            val resultado = executor.executar(EtapaDiagnosticoOffline.DNS) as ResultadoEtapaDiagnosticoOffline.Falha

            assertEquals("sem resolução DNS -- nem pelo resolvedor da rede nem por DoH externo", resultado.motivo)
        }

    @Test
    fun `rota externa alcancavel produz sucesso`() =
        runTest {
            val executor = criarExecutor(rotaExterna = ProbeResult.Success())

            val resultado = executor.executar(EtapaDiagnosticoOffline.ROTA_EXTERNA)

            assertTrue(resultado is ResultadoEtapaDiagnosticoOffline.Sucesso)
        }

    @Test
    fun `rota externa inalcancavel produz falha com motivo descritivo`() =
        runTest {
            val executor = criarExecutor(rotaExterna = ProbeResult.Failure(ProbeFailureReason.HOST_UNREACHABLE))

            val resultado = executor.executar(EtapaDiagnosticoOffline.ROTA_EXTERNA) as ResultadoEtapaDiagnosticoOffline.Falha

            assertEquals("host inalcançável", resultado.motivo)
        }

    @Test
    fun `hostname sem captive portal e sucesso produz sucesso`() =
        runTest {
            val executor = criarExecutor(hostname = HostnameProbeOutcome(ProbeResult.Success(), captivePortalSuspeito = false))

            val resultado = executor.executar(EtapaDiagnosticoOffline.HOSTNAME_CAPTIVE_PORTAL)

            assertTrue(resultado is ResultadoEtapaDiagnosticoOffline.Sucesso)
        }

    @Test
    fun `hostname com captive portal suspeito produz falha mesmo com probe result sucesso`() =
        runTest {
            val executor = criarExecutor(hostname = HostnameProbeOutcome(ProbeResult.Success(), captivePortalSuspeito = true))

            val resultado = executor.executar(EtapaDiagnosticoOffline.HOSTNAME_CAPTIVE_PORTAL) as ResultadoEtapaDiagnosticoOffline.Falha

            assertTrue(resultado.motivo?.contains("portal cativo") == true)
        }

    @Test
    fun `hostname inalcancavel sem captive portal produz falha com motivo da sonda`() =
        runTest {
            val executor =
                criarExecutor(
                    hostname = HostnameProbeOutcome(ProbeResult.Failure(ProbeFailureReason.UNEXPECTED_RESPONSE), captivePortalSuspeito = false),
                )

            val resultado = executor.executar(EtapaDiagnosticoOffline.HOSTNAME_CAPTIVE_PORTAL) as ResultadoEtapaDiagnosticoOffline.Falha

            assertEquals("resposta inesperada", resultado.motivo)
        }
}
