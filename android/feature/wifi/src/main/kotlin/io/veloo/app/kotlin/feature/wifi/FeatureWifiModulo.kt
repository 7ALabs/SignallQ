package io.signallq.app.feature.wifi

import android.content.Context

object FeatureWifiModulo {
    fun criarMontarResumoWifiUseCase(): MontarResumoWifiUseCase = MontarResumoWifiUseCase()

    fun criarScannerRedesWifi(context: Context): ScannerRedesWifi = ScannerRedesWifi(context)
}

