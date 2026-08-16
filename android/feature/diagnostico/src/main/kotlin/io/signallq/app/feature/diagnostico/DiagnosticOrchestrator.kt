package io.signallq.app.feature.diagnostico

import io.signallq.app.core.diagnostico.ConnectionType
import io.signallq.app.core.diagnostico.DiagnosticArea
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticRunner
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.FibraDiagnosticInput
import io.signallq.app.core.diagnostico.InternetDiagnosticInput
import io.signallq.app.core.diagnostico.WifiDiagnosticInput
import io.signallq.app.core.network.AnalyticsHelper
import io.signallq.app.core.network.NoOpAnalyticsHelper
import io.signallq.app.feature.diagnostico.remote.RemoteDiagnosticRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * [remoteDiagnosticRepository] — GH#969 ligou o motor remoto (worker
 * `signallq-diagnostic`) ao fluxo real de diagnostico. Desde GH#1444 (shadow
 * mode, parte de #952) este orquestrador chama
 * [RemoteDiagnosticRepository.evaluateShadow] (motor LOCAL sempre autoritativo
 * — o que a UI mostra — com avaliacao remota em paralelo so para comparacao/
 * telemetria), NAO mais [RemoteDiagnosticRepository.evaluate] (remoto-primeiro).
 * Ver kdoc de [RemoteDiagnosticRepository] para a distincao completa entre os
 * dois metodos e por que a troca aconteceu.
 */
class DiagnosticOrchestrator(
    private val analyticsHelper: AnalyticsHelper = NoOpAnalyticsHelper,
    private val remoteDiagnosticRepository: RemoteDiagnosticRepository =
        RemoteDiagnosticRepository(baseUrl = BuildConfig.DIAGNOSTIC_WORKER_URL),
) {

    private val mutableSnapshotFlow = MutableStateFlow(
        SnapshotDiagnostico(
            estado = EstadoDiagnostico.idle,
            relatorio = null,
            erroMensagem = null,
        ),
    )

    val snapshotFlow: StateFlow<SnapshotDiagnostico> = mutableSnapshotFlow.asStateFlow()

    private val emExecucao = AtomicBoolean(false)
    private val proximaGeracao = AtomicLong(0L)

    suspend fun executar(
        internetInput: InternetDiagnosticInput?,
        wifiInput: WifiDiagnosticInput?,
        fibraInput: FibraDiagnosticInput? = null,
        // GH#1228 (Fase 3) — id da execucao de origem (ver DiagnosticInput.executionId).
        // Default "" preserva os chamadores que ainda nao propagam.
        executionId: String = "",
    ) {
        // Compatibilidade: fluxo legado do app.
        val tipo =
            when {
                wifiInput != null -> ConnectionType.wifi
                else -> ConnectionType.desconhecido
            }
        executar(
            DiagnosticInput(
                connectionType = tipo,
                internet = internetInput,
                wifi = wifiInput,
                fibra = fibraInput,
                executionId = executionId,
            ),
        )
    }

    suspend fun executar(
        input: DiagnosticInput,
        enabledAreas: Set<DiagnosticArea> = DiagnosticArea.entries.toSet(),
    ) = executarPreparando(enabledAreas) { input }

    /**
     * Inclui a preparação do input no ciclo canônico e rejeita solicitações concorrentes.
     * Retorna `false` quando já existe uma execução em andamento.
     */
    suspend fun executarPreparando(
        enabledAreas: Set<DiagnosticArea> = DiagnosticArea.entries.toSet(),
        prepararInput: suspend () -> DiagnosticInput,
    ): Boolean {
        if (!emExecucao.compareAndSet(false, true)) return false
        val geracao = proximaGeracao.incrementAndGet()
        mutableSnapshotFlow.value =
            SnapshotDiagnostico(EstadoDiagnostico.executando, null, null, geracao = geracao)
        return try {
            executarProtegido(prepararInput(), enabledAreas, geracao)
            true
        } catch (cancelamento: CancellationException) {
            mutableSnapshotFlow.value =
                SnapshotDiagnostico(EstadoDiagnostico.cancelado, null, null, geracao = geracao)
            throw cancelamento
        } catch (t: Throwable) {
            Timber.e(t, "erro no diagnostico: ${t.message}")
            mutableSnapshotFlow.value =
                SnapshotDiagnostico(
                    estado = EstadoDiagnostico.erro,
                    relatorio = null,
                    erroMensagem = t.message ?: "erroDiagnostico",
                    geracao = geracao,
                )
            true
        } finally {
            emExecucao.set(false)
        }
    }

    private suspend fun executarProtegido(
        input: DiagnosticInput,
        enabledAreas: Set<DiagnosticArea>,
        geracao: Long,
    ) {
        analyticsHelper.registrarDiagIniciado(
            tipoConexao = input.connectionType.name,
            areasHabilitadas = enabledAreas.joinToString(",") { it.name.lowercase() },
            temSpeedtest = input.internet != null,
        )
        Timber.i(
            "iniciando diagnostico tipo=${input.connectionType} dl=${input.internet?.downloadMbps} ul=${input.internet?.uploadMbps} lat=${input.internet?.latencyMs} rssi=${input.wifi?.rssiDbm} fibra=${input.fibra?.isUp} dnsMs=${input.dns?.currentDnsLatencyMs}",
        )

        val relatorio = remoteDiagnosticRepository.evaluateShadow(input, enabledAreas)

        Timber.i(
            "diagnostico concluido decisao=${relatorio.decisao.id}(${relatorio.decisao.status}) " +
                "wifi=${relatorio.wifiResultados.map { "${it.id}:${it.status}" }} " +
                "internet=${relatorio.internetResultados.map { "${it.id}:${it.status}" }} " +
                "mobile=${relatorio.mobileResultados.map { "${it.id}:${it.status}" }} " +
                "fibra=${relatorio.fibraResultados.map { "${it.id}:${it.status}" }} " +
                "dns=${relatorio.dnsResultados.map { "${it.id}:${it.status}" }} " +
                "hist=${relatorio.historicoResultados.map { "${it.id}:${it.status}" }}",
        )

        mutableSnapshotFlow.value =
            SnapshotDiagnostico(
                estado = EstadoDiagnostico.concluido,
                relatorio = relatorio,
                erroMensagem = null,
                input = input,
                geracao = geracao,
            )

        val todosResultados =
            relatorio.wifiResultados + relatorio.internetResultados + relatorio.mobileResultados +
                relatorio.fibraResultados + relatorio.dnsResultados + relatorio.historicoResultados +
                relatorio.wifiCanalResultados + relatorio.redeResultados
        analyticsHelper.registrarDiagConcluido(
            tipoConexao = input.connectionType.name,
            statusGeral = relatorio.decisao.status.name,
            decisaoId = relatorio.decisao.id,
            scoreConexao = relatorio.scoreConexao.toLong(),
            confianca = relatorio.confianca,
            nResultadosCriticos = todosResultados.count { it.status == DiagnosticStatus.critical }.toLong(),
            nResultadosAttention = todosResultados.count { it.status == DiagnosticStatus.attention }.toLong(),
        )
    }
}
