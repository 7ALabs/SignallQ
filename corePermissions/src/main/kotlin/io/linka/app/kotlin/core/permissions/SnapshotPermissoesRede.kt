package io.linka.app.kotlin.core.permissions

data class SnapshotPermissoesRede(
    val localizacaoFina: EstadoPermissao,
    val nearbyWifi: EstadoPermissao,
) {
    fun estaAptoParaScanRede(): Boolean {
        return localizacaoFina == EstadoPermissao.concedida &&
            nearbyWifi == EstadoPermissao.concedida
    }
}

