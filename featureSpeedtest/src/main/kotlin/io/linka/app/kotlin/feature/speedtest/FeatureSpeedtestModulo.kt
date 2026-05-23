package io.linka.app.kotlin.feature.speedtest

object FeatureSpeedtestModulo {
    fun criarExecutorSpeedtest(): ExecutorSpeedtest {
        return ExecutorSpeedtestCloudflare()
    }
}

