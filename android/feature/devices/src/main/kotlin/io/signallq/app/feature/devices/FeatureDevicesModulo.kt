package io.signallq.app.feature.devices

import okhttp3.OkHttpClient

object FeatureDevicesModulo {
    fun criarScannerDispositivos(
        context: android.content.Context,
        okHttpClient: OkHttpClient,
    ): ScannerDispositivos {
        return ScannerDispositivosAndroid(context.applicationContext, okHttpClient)
    }
}
