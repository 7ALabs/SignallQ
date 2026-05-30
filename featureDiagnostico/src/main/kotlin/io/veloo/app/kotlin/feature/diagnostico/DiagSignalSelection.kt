package io.veloo.app.feature.diagnostico

data class DiagSignalSelection(
    val velocidade: Boolean = true,
    val wifiSinal: Boolean = true,
    val latencia: Boolean = true,
    val fibra: Boolean = true,
    val dns: Boolean = false,
) {
    fun toEnabledAreas(): Set<DiagnosticArea> = buildSet {
        if (velocidade) add(DiagnosticArea.VELOCIDADE)
        if (wifiSinal) add(DiagnosticArea.WIFI_SINAL)
        if (latencia) add(DiagnosticArea.LATENCIA)
        if (fibra) add(DiagnosticArea.FIBRA)
        if (dns) add(DiagnosticArea.DNS)
    }

    val anySelected: Boolean get() = velocidade || wifiSinal || latencia || fibra || dns
}
