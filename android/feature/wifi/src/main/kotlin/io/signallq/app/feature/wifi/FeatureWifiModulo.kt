package io.signallq.app.feature.wifi

import android.content.Context
import io.signallq.app.core.network.wifi.ScannerRedesWifi

object FeatureWifiModulo {
    fun criarMontarResumoWifiUseCase(): MontarResumoWifiUseCase = MontarResumoWifiUseCase()

    fun criarScannerRedesWifi(context: Context): ScannerRedesWifi = ScannerRedesWifi(context)
}

