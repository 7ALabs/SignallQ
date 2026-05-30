package io.veloo.app.feature.devices

object FeatureDevicesModulo {
    fun criarScannerDispositivos(
        context: android.content.Context,
    ): ScannerDispositivos {
        return ScannerDispositivosAndroid(context.applicationContext)
    }
}
