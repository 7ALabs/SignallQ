package io.signallq.app.core.network.contracts.topologia

/**
 * Registra duas [Evidencia] que discordam entre si (ex.: SSID indica mesh, OUI indica gateway
 * ISP). O motor decide como resolver (Fase 2A prioriza OUI por ser sinal de hardware); este
 * contrato só guarda o desacordo para auditoria/depuração, não a resolução.
 */
data class ConflitoSinal(
    val evidenciaA: Evidencia,
    val evidenciaB: Evidencia,
    val descricao: String,
)
