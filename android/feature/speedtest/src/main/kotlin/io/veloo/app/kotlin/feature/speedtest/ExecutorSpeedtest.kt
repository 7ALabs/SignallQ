package io.signallq.app.feature.speedtest

import kotlinx.coroutines.flow.StateFlow

interface ExecutorSpeedtest {
    val snapshotFlow: StateFlow<SnapshotExecucaoSpeedtest>

    suspend fun executar(
        modo: ModoSpeedtest = ModoSpeedtest.fast,
        connectionType: String? = null,
        connectionTypeProvider: (() -> String?)? = null,
        tecnologiaProvider: (() -> String?)? = null,
    )

    fun cancelar()
}
