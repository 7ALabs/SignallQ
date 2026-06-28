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
