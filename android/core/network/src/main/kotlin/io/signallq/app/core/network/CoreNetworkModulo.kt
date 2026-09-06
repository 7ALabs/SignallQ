package io.signallq.app.core.network

import android.content.Context

object CoreNetworkModulo {
    fun criarMonitorRede(context: Context): MonitorRede = MonitorRedeAndroid(context)

    fun criarNetworkCapabilitiesProvider(context: Context): NetworkCapabilitiesProvider = NetworkCapabilitiesProviderImpl(context)
}
