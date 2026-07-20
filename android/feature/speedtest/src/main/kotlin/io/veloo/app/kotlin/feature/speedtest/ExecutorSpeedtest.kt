package io.signallq.app.feature.speedtest

import kotlinx.coroutines.flow.StateFlow

interface ExecutorSpeedtest {
    val snapshotFlow: StateFlow<SnapshotExecucaoSpeedtest>

    suspend fun executar(
        modo: ModoSpeedtest = ModoSpeedtest.fast,
        connectionType: String? = null,
        connectionTypeProvider: (() -> String?)? = null,
        tecnologiaProvider: (() -> String?)? = null,
        /** GH#1221 RF-01 — resolve se a rede ATUAL (no momento do teste, nao na criacao do
         *  singleton) e metered/movel, para o executor escolher o perfil de pool HTTP
         *  correto (menos conexoes em rede movel). Quando nulo, mantem o valor passado na
         *  construcao do executor (comportamento anterior, usado por callers que nao
         *  monitoram rede — ex.: SignallQ Pro). */
        isMobileProvider: (() -> Boolean)? = null,
    )

    fun cancelar()
}
