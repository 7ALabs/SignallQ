package io.signallq.app.core.network.contracts.topologia

/**
 * Um sinal individual observado e usado para compor uma [ClassificacaoTopologia].
 *
 * @param valorBruto valor observado, sem interpretação (ex.: o próprio OUI "C46E1F", o SSID
 * cru, o RSSI em dBm como string) — quem lê a evidência decide como formatar/traduzir.
 */
data class Evidencia(
    val tipo: TipoEvidencia,
    val valorBruto: String,
    val peso: PesoEvidencia,
)
