package io.linka.app.kotlin.feature.wifi

import android.content.Context

object FeatureWifiModulo {
    fun criarMontarResumoWifiUseCase(): MontarResumoWifiUseCase = MontarResumoWifiUseCase()

    fun criarScannerRedesWifi(context: Context): ScannerRedesWifi = ScannerRedesWifi(context)
}

