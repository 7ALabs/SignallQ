package io.signallq.app.core.network.contracts.gateway

/**
 * Evidencia coletada na Fase 1 (fingerprint passivo, sem credencial) sobre o
 * equipamento local — GH#545, epic GH#547.
 *
 * Entradas possiveis descritas na issue: gateway local, rotas HTTP conhecidas,
 * HTML/API de identificacao, headers, marca/modelo, resposta de login,
 * catalogo interno de drivers. Este contrato cobre as entradas de fingerprint
 * passivo; a resposta de login (Fase 2) e tratada por [loginResponseMarker]
 * quando disponivel, mas nao e obrigatoria para classificar.
 */
data class EquipmentFingerprintEvidence(
    /** IP do gateway na rede local do usuario (ex.: "192.168.1.1"). */
    val gatewayIp: String? = null,
    /** Marca sinalizada por alguma fonte (header, HTML, banner UPnP...). */
    val vendorHint: String? = null,
    /** Modelo sinalizado por alguma fonte. */
    val modelHint: String? = null,
    /** Texto de header HTTP (`Server`) ou titulo/HTML da pagina de identificacao/login. */
    val httpBanner: String? = null,
    /** Rotas HTTP conhecidas que responderam ao ping passivo (ex.: "device_status.cgi"). */
    val knownRoutesDetected: Set<String> = emptySet(),
    /** Marcador da resposta de login quando ja disponivel (ex.: header `X-SID`, HTTP 299). Opcional — Fase 2. */
    val loginResponseMarker: String? = null,
)
