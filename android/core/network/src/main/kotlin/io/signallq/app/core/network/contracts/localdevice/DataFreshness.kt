package io.signallq.app.core.network.contracts.localdevice

/**
 * Marca de frescor da leitura — permite à UI/motor de diagnóstico decidir se
 * um snapshot antigo ainda é confiável para compor um veredito.
 */
data class DataFreshness(
    val capturadoEmEpochMs: Long,
    val expirado: Boolean = false,
)
