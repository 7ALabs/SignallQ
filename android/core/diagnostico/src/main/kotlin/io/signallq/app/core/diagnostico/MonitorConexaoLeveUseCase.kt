package io.signallq.app.core.diagnostico

import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede

data class StatusTempoReal(
    val veredito: String,
    val motivo: String
)

class MonitorConexaoLeveUseCase {
    
    fun calcularStatus(snapshotRede: SnapshotRede): StatusTempoReal {
        if (!snapshotRede.conectado) {
            return StatusTempoReal("Offline", "Sem conexão de internet ativa.")
        }
        
        return when (snapshotRede.estadoConexao) {
            EstadoConexao.wifi -> {
                val rssi = snapshotRede.wifiLinkSnapshot?.rssiDbm
                when {
                    rssi == null -> StatusTempoReal("Conectado", "Conectado ao Wi-Fi, mas sinal não mensurável.")
                    rssi >= -60 -> StatusTempoReal("Excelente", "Sinal Wi-Fi forte e conexões estáveis.")
                    rssi >= -70 -> StatusTempoReal("Bom", "Sinal Wi-Fi bom para navegação normal.")
                    rssi >= -80 -> StatusTempoReal("Regular", "Sinal Wi-Fi pode apresentar oscilações e lentidão.")
                    else -> StatusTempoReal("Fraco", "Sinal Wi-Fi muito fraco, internet pode não funcionar.")
                }
            }
            EstadoConexao.movel -> {
                StatusTempoReal("Bom", "Conectado aos dados móveis.")
            }
            EstadoConexao.ethernet -> StatusTempoReal("Excelente", "Conexão cabeada geralmente é muito estável.")
            else -> StatusTempoReal("Conectado", "Conectado à internet.")
        }
    }
}
