package io.linka.app.kotlin.feature.speedtest

object FeatureSpeedtestModulo {
    fun criarExecutorSpeedtest(isMobile: Boolean = false): ExecutorSpeedtest {
        return ExecutorSpeedtestCloudflare(isMobile)
    }
}

