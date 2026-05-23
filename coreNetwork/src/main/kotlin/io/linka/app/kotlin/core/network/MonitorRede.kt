package io.linka.app.kotlin.core.network

import kotlinx.coroutines.flow.StateFlow

interface MonitorRede {
    val snapshotFlow: StateFlow<SnapshotRede>

    fun iniciar()

    fun encerrar()
}

