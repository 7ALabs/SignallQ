package io.linka.app.kotlin.feature.devices

object FeatureDevicesModulo {
    fun criarScannerDispositivos(
        context: android.content.Context,
    ): ScannerDispositivos {
        return ScannerDispositivosAndroid(context.applicationContext)
    }
}
