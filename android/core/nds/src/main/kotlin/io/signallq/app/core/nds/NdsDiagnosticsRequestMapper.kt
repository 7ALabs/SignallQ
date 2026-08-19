package io.signallq.app.core.nds

import io.signallq.app.core.diagnostico.BandaWifi
import io.signallq.app.core.diagnostico.ConnectionType
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.banda
import java.util.UUID

/** `app.id` do payload NDS — identificador tecnico preservado do app (`AGENTS.md`,
 *  "Identificadores tecnicos"), confirmado nos testes de NDS-01 (#1745). */
private const val NDS_APP_ID = "io.signallq.app"

/**
 * Ponte `DiagnosticInput -> NdsDiagnosticsRequest` (NDS-02k, issue #1759, item 4 —
 * secao 2a do inventario). Funcao pura, sem I/O. Vive em `core/nds` (nao em
 * `feature/diagnostico`) porque este modulo ja depende de `core/diagnostico`
 * (dependencia temporaria documentada em `core/nds/build.gradle.kts`) e o
 * `NdsClient`/`NdsDiagnosticsRequest` que ela alimenta tambem moram aqui.
 *
 * [appVersion] vem do `BuildConfig` do chamador (`core/nds` nao conhece a versao
 * do app consumer). [requestId] reusa `DiagnosticInput.executionId` (GH#1228 Fase 3)
 * quando propagado; gera um UUID novo quando o chamador nao propagou (default `""`).
 *
 * ## Campos de `DiagnosticInput` sem correspondente no schema do NDS (fora por design)
 * `natStatus`, `velocidadeContratadaMbps`, `historico`, `localDevice`,
 * `deviceGamingSelecionado` — o NDS nao pede nenhum destes ainda. NAO confundir
 * `deviceGamingSelecionado` (device/console especifico, ex.: "playstation") com o sinal
 * "diagnostico roda dentro do Modo Gamer": este ultimo TEM campo correspondente no NDS
 * (`profile="gamer"`, decidido em #1746 secao 3b) e ja e aceito por este mapper via
 * [perfilGamer] — o gap historico (issue #1762) era o chamador em
 * `NdsDiagnosticRepository.evaluate` nunca repassar esse parametro, nao a ausencia do campo.
 *
 * ## Gap documentado: `wifiScan`
 * O bloco opcional `wifiScan` do NDS pede congestionamento/melhor-canal calculados
 * por `ChannelEvaluator` (`:coreNetwork`, ja usado por [mapWifiScanToNds] nos seams
 * NDS-02b/e) — dado que `DiagnosticInput.wifiScan` (so `redes`/`conectadoCanal`/
 * `conectadoBanda`) nao carrega. Este mapper NUNCA roda o evaluator (fora do escopo
 * de uma funcao pura de traducao de shape) — `wifiScan` fica sempre `null` aqui.
 * Registrado no inventario da issue #1759, nao e bug desta fatia.
 *
 * ## Gap documentado: `dns.hijacked`
 * Ainda nao ha coleta desse dado no app (ADR-017, pendencias em aberto) — fica
 * sempre `null` aqui, mesmo com bloco `dns` presente.
 */
fun DiagnosticInput.toNdsDiagnosticsRequest(
    appVersion: String,
    perfilGamer: Boolean = false,
): NdsDiagnosticsRequest {
    val wifiInfo = wifi?.let {
        NdsWifiInfo(
            rssi = it.rssiDbm,
            band = ndsBandaWifi(it.banda()),
            channel = it.canal,
            linkSpeed = it.linkSpeedMbps,
            standard = it.wifiStandard,
        )
    }
    val fiberInfo = fibra?.let {
        NdsFiberInfo(
            rxPowerDbm = it.rxPowerDbm,
            txPowerDbm = it.txPowerDbm,
            temperatureC = it.temperatureCelsius,
        )
    }
    val speedInfo = internet?.let {
        NdsSpeedInfo(
            pingMs = it.latencyMs,
            jitterMs = it.jitterMs,
            downloadMbps = it.downloadMbps,
            uploadMbps = it.uploadMbps,
            packetLossPercent = it.perdaPercentual,
        )
    }
    val dnsInfo = dns?.let {
        NdsDnsInfo(
            primary = it.currentDnsIp,
            responseTimeMs = it.currentDnsLatencyMs,
        )
    }

    return NdsDiagnosticsRequest(
        requestId = executionId.ifBlank { UUID.randomUUID().toString() },
        app = NdsAppInfo(id = NDS_APP_ID, version = appVersion),
        profile = ndsProfile(perfilGamer),
        capabilities = ndsCapabilities(wifi = wifiInfo, fiber = fiberInfo),
        connection = NdsConnectionInfo(
            type = ndsConnectionType(connectionType),
            ssid = wifi?.ssid,
            bssid = wifi?.bssidMascarado,
        ),
        wifi = wifiInfo,
        wifiScan = null,
        speed = speedInfo,
        dns = dnsInfo,
        fiber = fiberInfo,
    )
}

private fun ndsBandaWifi(banda: BandaWifi): String? = when (banda) {
    BandaWifi.ghz24 -> "2.4GHz"
    BandaWifi.ghz5 -> "5GHz"
    BandaWifi.desconhecida -> null
}

/** Vocabulario livre (nao enum) do bloco `connection.type` — mesmo criterio de
 *  [NdsConnectionInfo.type]: string simples, nunca trava o cliente quando o
 *  servidor aceitar um valor novo. */
private fun ndsConnectionType(tipo: ConnectionType): String = when (tipo) {
    ConnectionType.wifi -> "WIFI"
    ConnectionType.mobile -> "MOBILE"
    ConnectionType.ethernet -> "ETHERNET"
    ConnectionType.desconectado -> "DISCONNECTED"
    ConnectionType.desconhecido -> "UNKNOWN"
}
