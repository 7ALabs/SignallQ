package io.signallq.app.feature.speedtest

object FeatureSpeedtestModulo {
    /**
     * @param latencyProbeUrl URL do worker dedicado de latência (GH#1118). Quando não
     * informado, [ExecutorSpeedtestCloudflare] cai no default (host público CDN) — mesmo
     * comportamento de antes da correção.
     */
    fun criarExecutorSpeedtest(
        isMobile: Boolean = false,
        latencyProbeUrl: String? = null,
    ): ExecutorSpeedtest {
        return if (latencyProbeUrl.isNullOrBlank()) {
            ExecutorSpeedtestCloudflare(isMobile)
        } else {
            ExecutorSpeedtestCloudflare(isMobile, latencyProbeUrl)
        }
    }
}

