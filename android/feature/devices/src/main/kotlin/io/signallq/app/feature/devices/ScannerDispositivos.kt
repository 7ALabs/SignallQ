package io.signallq.app.feature.devices

import io.signallq.app.core.network.contracts.localdevice.ClientSnapshot
import kotlinx.coroutines.flow.StateFlow

interface ScannerDispositivos {
    val snapshotFlow: StateFlow<SnapshotScanDispositivos>

    /**
     * @param clientesGateway lista de [ClientSnapshot] da leitura ativa mais recente do
     *   gateway/roteador (issue #839), já filtrada pelo chamador por
     *   `capabilities.suportaClientes` — vazia (padrão) quando não há leitura ativa
     *   disponível nesta sessão. Usada só para nomear dispositivos já descobertos pela
     *   varredura passiva por MAC; não adiciona dispositivo novo à lista.
     */
    suspend fun iniciarScan(profundo: Boolean = true, clientesGateway: List<ClientSnapshot> = emptyList())
}
