package io.linka.app.kotlin.feature.devices

import kotlinx.coroutines.flow.StateFlow

interface ScannerDispositivos {
    val snapshotFlow: StateFlow<SnapshotScanDispositivos>

    suspend fun iniciarScan(profundo: Boolean = true)
}
