package io.veloo.app.feature.diagnostico

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "SignallQDiagnostico"

class DiagnosticOrchestrator {

    private val mutableSnapshotFlow = MutableStateFlow(
        SnapshotDiagnostico(
            estado = EstadoDiagnostico.idle,
            relatorio = null,
            erroMensagem = null,
        ),
    )

    val snapshotFlow: StateFlow<SnapshotDiagnostico> = mutableSnapshotFlow.asStateFlow()

    fun executar(
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

    fun executar(
        input: DiagnosticInput,
        enabledAreas: Set<DiagnosticArea> = DiagnosticArea.entries.toSet(),
    ) {
        try {
            Log.i(
                TAG,
                "iniciando diagnostico tipo=${input.connectionType} dl=${input.internet?.downloadMbps} ul=${input.internet?.uploadMbps} lat=${input.internet?.latencyMs} rssi=${input.wifi?.rssiDbm} fibra=${input.fibra?.isUp} dnsMs=${input.dns?.currentDnsLatencyMs}",
            )

            val relatorio = DiagnosticRunner.run(input, enabledAreas)

            Log.i(
                TAG,
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
        } catch (t: Throwable) {
            Log.e(TAG, "erro no diagnostico: ${t.message}", t)
            mutableSnapshotFlow.value = SnapshotDiagnostico(
                estado = EstadoDiagnostico.erro,
                relatorio = null,
                erroMensagem = t.message ?: "erroDiagnostico",
            )
        }
    }
}
