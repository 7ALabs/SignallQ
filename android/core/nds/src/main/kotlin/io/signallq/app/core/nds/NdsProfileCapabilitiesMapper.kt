package io.signallq.app.core.nds

/**
 * Regra `profile`/`capabilities` do payload NDS (ADR-017, decisao registrada
 * em #1746 secao 3b — NDS-02a, issue #1747).
 *
 * `capabilities` sempre inclui `scoring`+`ai`; `wifi`/`fiber` sao incluidos
 * quando o bloco correspondente existe no request — MESMO criterio que ja
 * decide se o bloco e omitido do JSON em [NdsDiagnosticsRequest.toJson] (um
 * bloco nulo nunca vira chave no JSON; aqui, um bloco nulo tambem nunca entra
 * em `capabilities`).
 *
 * Lembrete de implementacao (ADR-017): os modulos retornados pelo NDS em
 * `results[]` NAO mapeiam 1:1 com `capabilities` pedidas — o cliente deve
 * sempre buscar por `module`, nunca assumir presenca/posicao so porque foi
 * pedida aqui.
 */
fun ndsCapabilities(
    incluiWifi: Boolean,
    incluiFiber: Boolean,
    incluiWifiScan: Boolean = false,
): List<String> {
    val capabilities = mutableListOf("scoring", "ai")
    if (incluiWifi) capabilities += "wifi"
    if (incluiFiber) capabilities += "fiber"
    // ADR-018 — "wifi_scan" e o vocabulario ja aceito pelo NDS (ADR-017, contrato
    // canonico PR #12) para a capability de evidencia de scan de canal.
    if (incluiWifiScan) capabilities += "wifi_scan"
    return capabilities
}

/** Sobrecarga de conveniencia — deriva `incluiWifi`/`incluiFiber`/`incluiWifiScan`
 *  diretamente dos blocos opcionais do request, sem o chamador repetir `!= null`. */
fun ndsCapabilities(
    wifi: NdsWifiInfo?,
    fiber: NdsFiberInfo?,
    wifiScan: NdsWifiScanInfo? = null,
): List<String> = ndsCapabilities(
    incluiWifi = wifi != null,
    incluiFiber = fiber != null,
    incluiWifiScan = wifiScan != null,
)

/**
 * `profile` do payload NDS. `"gamer"` somente dentro do fluxo Modo Gamer;
 * fora dele, campo **omitido** (`null`) — nao `"general"`.
 *
 * PENDENCIA REGISTRADA (nao decidida aqui — confirmacao de contrato externo,
 * nao decisao de produto do Luiz/Claudete, conforme a issue #1747): o unico
 * valor confirmado no exemplo do ADR-017 e `"gamer"`. Nao sabemos se o NDS
 * aceita `"general"` literalmente, aceita campo omitido, ou espera outro
 * vocabulario — o `signallq-diagnostic-worker` (sendo descontinuado) usava
 * `navegacao/streaming/videochamada/jogos/trabalho`, vocabulario DIFERENTE de
 * `"gamer"`, entao nao presumimos heranca direta. Implementado aqui com a
 * leitura mais defensavel (campo omitido, ja que e opcional) ate quem mantem o
 * NDS confirmar o valor certo para "nao-gamer". NAO travar em producao sem
 * essa confirmacao.
 */
fun ndsProfile(dentroDoModoGamer: Boolean): String? = if (dentroDoModoGamer) "gamer" else null
