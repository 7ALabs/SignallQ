package io.signallq.app.feature.diagnostico.topology.lan

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GatewayResolver(context: Context) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    @Suppress("DEPRECATION")
    private val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    suspend fun resolve(): String? = withContext(Dispatchers.IO) {
        resolveViaLinkProperties() ?: resolveViaDhcpFallback()
    }

    @SuppressLint("MissingPermission")
    private fun resolveViaLinkProperties(): String? {
        return try {
            val network = cm.activeNetwork ?: return null
            val props = cm.getLinkProperties(network) ?: return null
            props.routes
                .firstOrNull { it.isDefaultRoute && it.gateway != null }
                ?.gateway
                ?.hostAddress
                ?.takeIf { it.isNotBlank() }
        } catch (_: Exception) { null }
    }

    // DhcpInfo.gateway é int little-endian — deprecado desde API 31, usado só como fallback
    // quando getLinkProperties retorna null (sem rede ativa ou API bug em alguns OEMs).
    @Suppress("DEPRECATION")
    private fun resolveViaDhcpFallback(): String? {
        return try {
            val gw = wm.dhcpInfo?.gateway ?: return null
            if (gw == 0) return null
            "${gw and 0xFF}.${(gw shr 8) and 0xFF}.${(gw shr 16) and 0xFF}.${(gw shr 24) and 0xFF}"
        } catch (_: Exception) { null }
    }
}
