package io.veloo.app.core.network

import android.content.Context

object CoreNetworkModulo {
    fun criarMonitorRede(context: Context): MonitorRede {
        return MonitorRedeAndroid(context)
    }

    fun criarNetworkCapabilitiesProvider(context: Context): NetworkCapabilitiesProvider {
        return NetworkCapabilitiesProviderImpl(context)
    }
}
