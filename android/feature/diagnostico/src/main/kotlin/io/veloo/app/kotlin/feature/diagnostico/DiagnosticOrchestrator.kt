package io.signallq.app.feature.diagnostico

import io.signallq.app.core.network.AnalyticsHelper
import io.signallq.app.core.network.NoOpAnalyticsHelper
import io.signallq.app.feature.diagnostico.remote.RemoteDiagnosticRepository
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [remoteDiagnosticRepository] — GH#969: liga o motor remoto (worker
 * `signallq-diagnostic`) ao fluxo real de diagnostico. [RemoteDiagnosticRepository.evaluate]
 * ja resolve toda a estrategia remoto-primeiro/fallback-local internamente (timeout curto,
 * qualquer falha cai pro [DiagnosticRunner] local) — este orquestrador so delega, nao
 * duplica a decisao remoto-vs-local.
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

    suspend fun executar(
        internetInput: InternetDiagnosticInput?,
        wifiInput: WifiDiagnosticInput?,
        fibraInput: FibraDiagnosticInput? = null,
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
            ),
        )
    }

    suspend fun executar(
        input: DiagnosticInput,
        enabledAreas: Set<DiagnosticArea> = DiagnosticArea.entries.toSet(),
    ) {
        analyticsHelper.registrarDiagIniciado(
            tipoConexao = input.connectionType.name,
            areasHabilitadas = enabledAreas.joinToString(",") { it.name.lowercase() },
            temSpeedtest = input.internet != null,
        )
        try {
            Timber.i(
                "iniciando diagnostico tipo=${input.connectionType} dl=${input.internet?.downloadMbps} ul=${input.internet?.uploadMbps} lat=${input.internet?.latencyMs} rssi=${input.wifi?.rssiDbm} fibra=${input.fibra?.isUp} dnsMs=${input.dns?.currentDnsLatencyMs}",
            )

            val relatorio = remoteDiagnosticRepository.evaluate(input, enabledAreas)

            Timber.i(
                "diagnostico concluido decisao=${relatorio.decisao.id}(${relatorio.decisao.status}) " +
                    "wifi=${relatorio.wifiResultados.map { "${it.id}:${it.status}" }} " +
                    "internet=${relatorio.internetResultados.map { "${it.id}:${it.status}" }} " +
                    "mobile=${relatorio.mobileResultados.map { "${it.id}:${it.status}" }} " +
                    "fibra=${relatorio.fibraResultados.map { "${it.id}:${it.status}" }} " +
                    "dns=${relatorio.dnsResultados.map { "${it.id}:${it.status}" }} " +
                    "hist=${relatorio.historicoResultados.map { "${it.id}:${it.status}" }}",
            )

            mutableSnapshotFlow.value = SnapshotDiagnostico(
                estado = EstadoDiagnostico.concluido,
                relatorio = relatorio,
                erroMensagem = null,
                input = input,
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
        } catch (t: Throwable) {
            Timber.e(t, "erro no diagnostico: ${t.message}")
            mutableSnapshotFlow.value = SnapshotDiagnostico(
                estado = EstadoDiagnostico.erro,
                relatorio = null,
                erroMensagem = t.message ?: "erroDiagnostico",
            )
        }
    }
}
