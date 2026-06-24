package io.veloo.app.core.telephony

import android.content.Context

object CoreTelephonyModulo {
    fun criarMonitorTelephony(context: Context): MonitorTelephony {
        return MonitorTelephonyImpl(context)
    }
}
