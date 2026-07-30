package io.signallq.app.feature.speedtest.connectivity

import io.signallq.app.core.database.connectivity.ConnectivityDiagnosisHistoryDao
import io.signallq.app.core.database.connectivity.ConnectivityDiagnosisHistoryEntity
import io.signallq.app.core.network.connectivity.ConnectivityDiagnosisSource
import io.signallq.app.core.network.contracts.connectivity.ConnectivityDiagnosis
import io.signallq.app.core.network.contracts.connectivity.ProbeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Ponte entre o motor local de diagnóstico de conectividade (core:network, GH#1512) e o
 * fluxo do Consumer: executa o diagnóstico, persiste o resultado sanitizado e expõe o
 * último resultado para quem consumir (Speedtest hoje, Histórico/motor canônico #1228
 * no futuro — mesmo contrato [ConnectivityDiagnosis], sem criar um segundo motor).
 */
interface ConnectivityDiagnosisRepository {
    val ultimoDiagnostico: StateFlow<ConnectivityDiagnosis?>

    /** Checagem síncrona e barata (sem I/O) de que existe uma rede Wi-Fi neste momento --
     *  ver [ConnectivityDiagnosisSource.existeRedeWifiAtiva]. Quem consome este repositório
     *  deve chamar isto ANTES de [diagnosticar] para decidir se vale a pena interromper um
     *  fluxo que depende de internet -- nunca usar `MonitorRede`/rede default para essa
     *  decisão, que pode já ter migrado para dados móveis (GH#1512). */
    fun existeRedeWifiAtiva(): Boolean

    /** Executa o diagnóstico local e persiste o resultado. Descarta a publicação se uma
     *  chamada mais recente já tiver concluído primeiro (rede pode ter mudado no meio). */
    suspend fun diagnosticar(): ConnectivityDiagnosis
}

class ConnectivityDiagnosisRepositoryImpl(
    private val runner: ConnectivityDiagnosisSource,
    private val historyDao: ConnectivityDiagnosisHistoryDao,
) : ConnectivityDiagnosisRepository {

    private val mutableUltimoDiagnostico = MutableStateFlow<ConnectivityDiagnosis?>(null)
    override val ultimoDiagnostico: StateFlow<ConnectivityDiagnosis?> = mutableUltimoDiagnostico.asStateFlow()

    override fun existeRedeWifiAtiva(): Boolean = runner.existeRedeWifiAtiva()

    /** Geração monotônica -- garante que um diagnóstico antigo que termine depois de um
     *  mais novo (ex.: usuário trocou de rede no meio da sondagem) nunca sobrescreve o
     *  resultado mais recente. */
    private val geracaoAtual = AtomicInteger(0)

    override suspend fun diagnosticar(): ConnectivityDiagnosis {
        val minhaGeracao = geracaoAtual.incrementAndGet()
        val diagnostico = runner.diagnosticar()

        if (geracaoAtual.get() == minhaGeracao) {
            mutableUltimoDiagnostico.value = diagnostico
        }
        // Persistencia sempre acontece (historico honesto de toda tentativa), mesmo quando
        // uma chamada mais nova ja tomou o StateFlow -- so a exibicao "ao vivo" e descartada.
        historyDao.registrar(diagnostico.toHistoryEntity())
        return diagnostico
    }
}

internal fun ConnectivityDiagnosis.toHistoryEntity(): ConnectivityDiagnosisHistoryEntity {
    val etapasNaoExecutadas = evidence
        .filter { it.result is ProbeResult.NotExecuted || it.result is ProbeResult.Unavailable }
        .joinToString(separator = "; ") { evidencia ->
            val motivo = when (val resultado = evidencia.result) {
                is ProbeResult.NotExecuted -> resultado.reason
                is ProbeResult.Unavailable -> resultado.reason
                else -> null
            }
            "${evidencia.step.name}${motivo?.let { ": $it" } ?: ""}"
        }

    return ConnectivityDiagnosisHistoryEntity(
        id = UUID.randomUUID().toString(),
        startedAtEpochMs = startedAtEpochMs,
        finishedAtEpochMs = finishedAtEpochMs,
        transport = transport.name,
        status = status.name,
        confidence = confidence.name,
        wifiConnected = wifiConnected,
        localAddressAvailable = localAddressAvailable,
        gatewayConfigured = gatewayConfigured,
        gatewayReachableOutcome = gatewayReachable.outcomeName(),
        dnsConfigured = dnsConfigured,
        dnsReachableOutcome = dnsReachable.outcomeName(),
        externalIpReachableOutcome = externalIpReachable.outcomeName(),
        hostnameReachableOutcome = hostnameReachable.outcomeName(),
        androidInternetCapability = androidInternetCapability,
        androidValidated = androidValidated,
        captivePortalDetected = captivePortalDetected,
        mobileFallbackAvailable = mobileFallbackAvailable,
        incompleteReason = etapasNaoExecutadas.ifBlank { null },
    )
}

/** Nome estável do tipo selado -- não usa `::class.simpleName` para não depender de
 *  regras de ofuscação/minify preservarem o nome da classe em builds de release. */
internal fun ProbeResult.outcomeName(): String = when (this) {
    is ProbeResult.Success -> "Success"
    is ProbeResult.Failure -> "Failure"
    is ProbeResult.Timeout -> "Timeout"
    is ProbeResult.NotExecuted -> "NotExecuted"
    is ProbeResult.Unavailable -> "Unavailable"
}
