package io.veloo.app.feature.speedtest

object FeatureSpeedtestModulo {
    fun criarExecutorSpeedtest(isMobile: Boolean = false): ExecutorSpeedtest {
        return ExecutorSpeedtestCloudflare(isMobile)
    }
}

