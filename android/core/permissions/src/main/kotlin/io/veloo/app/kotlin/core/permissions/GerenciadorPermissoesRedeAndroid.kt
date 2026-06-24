package io.veloo.app.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class GerenciadorPermissoesRedeAndroid(
    context: Context,
) : GerenciadorPermissoesRede {
    private val applicationContext = context.applicationContext

    override fun avaliar(): SnapshotPermissoesRede {
        val localizacaoFina =
            if (possuiPermissao(Manifest.permission.ACCESS_FINE_LOCATION)) {
                EstadoPermissao.concedida
            } else {
                EstadoPermissao.negada
            }

        val nearbyWifi =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                EstadoPermissao.concedida
            } else if (possuiPermissao(Manifest.permission.NEARBY_WIFI_DEVICES)) {
                EstadoPermissao.concedida
            } else {
                EstadoPermissao.negada
            }

        return SnapshotPermissoesRede(
            localizacaoFina = localizacaoFina,
            nearbyWifi = nearbyWifi,
        )
    }

    override fun listarPermissoesPendentes(): List<String> {
        val snapshot = avaliar()
        val pendentes = mutableListOf<String>()
        if (snapshot.localizacaoFina == EstadoPermissao.negada) {
            pendentes.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (snapshot.nearbyWifi == EstadoPermissao.negada) {
            pendentes.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        return pendentes
    }

    private fun possuiPermissao(permissao: String): Boolean {
        return ContextCompat.checkSelfPermission(
            applicationContext,
            permissao,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

