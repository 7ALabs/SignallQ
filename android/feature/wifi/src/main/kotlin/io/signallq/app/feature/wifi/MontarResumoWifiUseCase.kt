package io.signallq.app.feature.wifi

import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede

class MontarResumoWifiUseCase {
    fun executar(snapshotRede: SnapshotRede): ResumoWifi {
        return when (snapshotRede.estadoConexao) {
            EstadoConexao.wifi ->
                ResumoWifi(
                    titulo = "WiFi conectado",
                    detalhe = montarDetalheWifi(snapshotRede),
                )
            EstadoConexao.movel ->
                ResumoWifi(
                    titulo = "Rede móvel ativa",
                    detalhe = "Velocidade pode consumir franquia de dados.",
                )
            EstadoConexao.ethernet ->
                ResumoWifi(
                    titulo = "Ethernet ativa",
                    detalhe = "Conexão cabeada detectada.",
                )
            EstadoConexao.desconectado ->
                ResumoWifi(
                    titulo = "Sem conexão",
                    detalhe = "Conecte em WiFi para continuar o diagnóstico.",
                )
            EstadoConexao.desconhecido ->
                ResumoWifi(
                    titulo = "Conexão não identificada",
                    detalhe = "Valide o tipo de rede antes do teste.",
                )
        }
    }

    private fun montarDetalheWifi(snapshotRede: SnapshotRede): String {
        val wifi = snapshotRede.wifiLinkSnapshot
        val ssid = wifi?.ssid ?: "ssidNaoDisponivel"
        val bssid = wifi?.bssid ?: "bssidNaoDisponivel"
        val rssi = wifi?.rssiDbm?.toString() ?: "rssiNaoDisponivel"
        val linkSpeed = wifi?.linkSpeedMbps?.toString() ?: "linkSpeedNaoDisponivel"
        val frequencia = wifi?.frequenciaMhz?.toString() ?: "freqNaoDisponivel"
        return "ssid=$ssid bssid=$bssid rssi=$rssi link=$linkSpeed freq=$frequencia"
    }
}
