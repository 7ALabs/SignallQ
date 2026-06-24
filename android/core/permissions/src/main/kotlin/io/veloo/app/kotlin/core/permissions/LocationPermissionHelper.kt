package io.veloo.app.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Helper para solicitar e verificar permissões de localização.
 */
object LocationPermissionHelper {

    /**
     * Verifica se a permissão de localização foi concedida.
     *
     * Tenta ACCESS_FINE_LOCATION primeiro, depois fallback para ACCESS_COARSE_LOCATION.
     */
    fun temPermissaoLocalizacao(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocation) return true

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return coarseLocation
    }

    /**
     * Lista de permissões de localização a solicitar (em ordem de preferência).
     *
     * ACCESS_FINE_LOCATION é preferida, mas qualquer uma das duas é aceitável.
     */
    fun permissoesAoSolicitar(): Array<String> = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
}
