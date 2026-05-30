package io.veloo.app.feature.devices

import kotlinx.coroutines.flow.StateFlow

interface ScannerDispositivos {
    val snapshotFlow: StateFlow<SnapshotScanDispositivos>

    suspend fun iniciarScan(profundo: Boolean = true)
}
