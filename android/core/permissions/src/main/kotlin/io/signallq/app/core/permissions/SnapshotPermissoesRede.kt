package io.signallq.app.core.permissions

data class SnapshotPermissoesRede(
    val localizacaoFina: EstadoPermissao,
    val nearbyWifi: EstadoPermissao,
) {
    fun estaAptoParaScanRede(): Boolean =
        localizacaoFina == EstadoPermissao.concedida &&
            nearbyWifi == EstadoPermissao.concedida
}
