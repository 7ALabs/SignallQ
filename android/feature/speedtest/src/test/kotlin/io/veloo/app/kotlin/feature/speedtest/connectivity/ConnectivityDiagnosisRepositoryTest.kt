package io.signallq.app.feature.speedtest.connectivity

import io.signallq.app.core.database.connectivity.ConnectivityDiagnosisHistoryDao
import io.signallq.app.core.database.connectivity.ConnectivityDiagnosisHistoryEntity
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.connectivity.ConnectivityDiagnosisSource
import io.signallq.app.core.network.contracts.connectivity.ConnectivityDiagnosis
import io.signallq.app.core.network.contracts.connectivity.ConnectivityEvidence
import io.signallq.app.core.network.contracts.connectivity.ConnectivityProbeStep
import io.signallq.app.core.network.contracts.connectivity.ConnectivityStatus
import io.signallq.app.core.network.contracts.connectivity.ProbeFailureReason
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import io.signallq.app.core.network.contracts.topologia.NivelConfianca
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Fake local em vez de mock lib -- coreNetwork/featureSpeedtest nao tem mockk/mockito-kotlin
 *  nas dependencias de teste (mesma limitacao documentada em GatewayLatencyMeasurerTest). */
private class FakeConnectivityDiagnosisHistoryDao : ConnectivityDiagnosisHistoryDao {
    val registrados = mutableListOf<ConnectivityDiagnosisHistoryEntity>()
    override suspend fun registrar(entrada: ConnectivityDiagnosisHistoryEntity) {
        registrados += entrada
    }
    override suspend fun buscarRecentes(limite: Int): List<ConnectivityDiagnosisHistoryEntity> =
        registrados.sortedByDescending { it.startedAtEpochMs }.take(limite)
}

class ConnectivityDiagnosisRepositoryTest {

    private fun diagnostico(
        status: ConnectivityStatus = ConnectivityStatus.WIFI_WITHOUT_INTERNET,
        externalIpReachable: ProbeResult = ProbeResult.Failure(ProbeFailureReason.HOST_UNREACHABLE),
        hostnameReachable: ProbeResult = ProbeResult.NotExecuted("dns nao confirmou"),
    ) = ConnectivityDiagnosis(
        transport = EstadoConexao.wifi,
        wifiConnected = true,
        localAddressAvailable = true,
        gatewayConfigured = true,
        gatewayReachable = ProbeResult.Success(),
        dnsConfigured = true,
        dnsReachable = ProbeResult.Success(),
        externalIpReachable = externalIpReachable,
        hostnameReachable = hostnameReachable,
        androidInternetCapability = true,
        androidValidated = false,
        captivePortalDetected = false,
        mobileFallbackAvailable = true,
        status = status,
        confidence = NivelConfianca.MEDIA,
        evidence = listOf(
            ConnectivityEvidence(ConnectivityProbeStep.GATEWAY_REACHABILITY, ProbeResult.Success()),
            ConnectivityEvidence(ConnectivityProbeStep.HOSTNAME_REACHABILITY, hostnameReachable),
        ),
        startedAtEpochMs = 1_000L,
        finishedAtEpochMs = 2_000L,
    )

    @Test
    fun `diagnostico e persistido sem SSID BSSID IP ou DNS bruto`() = runTest {
        val entity = diagnostico().toHistoryEntity()

        val camposTexto = listOf(
            entity.transport, entity.status, entity.confidence,
            entity.gatewayReachableOutcome, entity.dnsReachableOutcome,
            entity.externalIpReachableOutcome, entity.hostnameReachableOutcome,
            entity.incompleteReason.orEmpty(),
        ).joinToString(" ")

        // Nao deve conter formato de IP (algo.algo.algo.algo) em nenhum campo persistido.
        assertTrue(
            "campos persistidos nao devem conter endereco IP",
            !Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}""").containsMatchIn(camposTexto),
        )
        assertEquals("WIFI_WITHOUT_INTERNET", entity.status)
        assertEquals("HOSTNAME_REACHABILITY: dns nao confirmou", entity.incompleteReason)
    }

    @Test
    fun `diagnostico completo nao gera incompleteReason`() = runTest {
        val completo = diagnostico(
            status = ConnectivityStatus.INTERNET_AVAILABLE,
            externalIpReachable = ProbeResult.Success(),
            hostnameReachable = ProbeResult.Success(),
        )
        val entity = completo.toHistoryEntity()
        assertNull(entity.incompleteReason)
    }

    @Test
    fun `resultado de diagnostico antigo e descartado apos diagnostico mais novo concluir primeiro`() = runTest {
        val dao = FakeConnectivityDiagnosisHistoryDao()
        var chamadas = 0
        val runner = object : ConnectivityDiagnosisSource {
            override fun existeRedeWifiAtiva(): Boolean = true
            override suspend fun diagnosticar(): ConnectivityDiagnosis {
                chamadas++
                return if (chamadas == 1) {
                    diagnostico(status = ConnectivityStatus.GATEWAY_UNREACHABLE)
                } else {
                    diagnostico(status = ConnectivityStatus.INTERNET_AVAILABLE)
                }
            }
        }
        val repository = ConnectivityDiagnosisRepositoryImpl(runner, dao)

        repository.diagnosticar()
        val ultimo = repository.diagnosticar()

        assertEquals(ConnectivityStatus.INTERNET_AVAILABLE, ultimo.status)
        assertEquals(ConnectivityStatus.INTERNET_AVAILABLE, repository.ultimoDiagnostico.value?.status)
        // Ambas as tentativas sao persistidas no historico (nenhuma e silenciosamente perdida).
        assertEquals(2, dao.registrados.size)
    }
}
