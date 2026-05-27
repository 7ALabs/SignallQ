package io.linka.app.kotlin.ui.screen

import io.linka.app.kotlin.feature.diagnostico.DiagnosticResult
import io.linka.app.kotlin.feature.fibra.DeviceInfoFibra
import io.linka.app.kotlin.feature.fibra.EstadoFibra
import io.linka.app.kotlin.feature.fibra.GponStatus
import io.linka.app.kotlin.feature.fibra.PppStatus
import io.linka.app.kotlin.feature.fibra.SnapshotFibra
import io.linka.app.kotlin.feature.fibra.WanStatus

sealed interface FibraModemUiState {

    object SemWifi : FibraModemUiState

    object SemCredenciais : FibraModemUiState

    object Conectando : FibraModemUiState

    data class Concluido(
        val gpon: GponStatus,
        val deviceInfo: DeviceInfoFibra?,
        val wan: WanStatus?,
        val ppp: PppStatus?,
        val interpretacoes: List<DiagnosticResult>,
    ) : FibraModemUiState

    data class Erro(
        val chave: String,
    ) : FibraModemUiState
}

fun mapearSnapshotFibra(
    snapshot: SnapshotFibra?,
    temWifi: Boolean,
    temCredenciais: Boolean,
): FibraModemUiState {
    if (!temWifi) return FibraModemUiState.SemWifi
    if (!temCredenciais) return FibraModemUiState.SemCredenciais
    if (snapshot == null) return FibraModemUiState.Conectando

    return when (snapshot.estado) {
        EstadoFibra.idle,
        EstadoFibra.conectando,
        -> FibraModemUiState.Conectando

        EstadoFibra.concluido -> {
            val gpon = snapshot.gpon
                ?: return FibraModemUiState.Erro("fibra.sem_gpon")
            FibraModemUiState.Concluido(
                gpon = gpon,
                deviceInfo = snapshot.deviceInfo,
                wan = snapshot.wan,
                ppp = snapshot.ppp,
                interpretacoes = emptyList(),
            )
        }

        EstadoFibra.erro -> FibraModemUiState.Erro(
            chave = snapshot.erroMensagem ?: "fibra.erro_desconhecido",
        )
    }
}
