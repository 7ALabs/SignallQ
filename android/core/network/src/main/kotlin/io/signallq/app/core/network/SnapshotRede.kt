package io.signallq.app.core.network

data class SnapshotRede(
    val estadoConexao: EstadoConexao,
    val conectado: Boolean,
    val timestampEpochMs: Long,
    val wifiLinkSnapshot: WifiLinkSnapshot?,
    val privateDnsAtivo: Boolean,
    val privateDnsHostname: String?,
    val dnsServidores: List<String>,
    val locationAtivado: Boolean = true,
    // #838 — calculado a partir do mesmo NetworkCapabilities ja obtido pelo callback
    // continuo do ConnectivityManager (onAvailable/onCapabilitiesChanged), em vez de uma
    // consulta avulsa no momento do toque em "Iniciar" (fragil logo apos abrir o app/trocar
    // de tab, quando o NetworkCapabilities ainda pode nao estar totalmente assentado).
    val metered: Boolean = false,
) {
    companion object {
        fun desconectado(timestampEpochMs: Long): SnapshotRede {
            return SnapshotRede(
                estadoConexao = EstadoConexao.desconectado,
                conectado = false,
                timestampEpochMs = timestampEpochMs,
                wifiLinkSnapshot = null,
                privateDnsAtivo = false,
                privateDnsHostname = null,
                dnsServidores = emptyList(),
            )
        }
    }
}
