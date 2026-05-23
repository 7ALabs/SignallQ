package io.linka.app.kotlin.core.network

import android.content.Context

object CoreNetworkModulo {
    fun criarMonitorRede(context: Context): MonitorRede {
        return MonitorRedeAndroid(context)
    }
}
