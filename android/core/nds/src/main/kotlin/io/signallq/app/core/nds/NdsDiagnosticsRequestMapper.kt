package io.signallq.app.core.nds

import io.signallq.app.core.diagnostico.BandaWifi
import io.signallq.app.core.diagnostico.ConnectionType
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.MobileDiagnosticInput
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
 * `natStatus`, `velocidadeContratadaMbps`, `localDevice`,
 * `deviceGamingSelecionado` — o NDS nao pede nenhum destes ainda. NAO confundir
 * `deviceGamingSelecionado` (device/console especifico, ex.: "playstation") com o sinal
 * "diagnostico roda dentro do Modo Gamer": este ultimo TEM campo correspondente no NDS
 * (`profile="gamer"`, decidido em #1746 secao 3b) e ja e aceito por este mapper via
 * [perfilGamer] — o gap historico (issue #1762) era o chamador em
 * `NdsDiagnosticRepository.evaluate` nunca repassar esse parametro, nao a ausencia do campo.
 *
 * ## `wifiScan` (ADR-018, NDS-Snapshot-02 — issue #1834)
 * O bloco opcional `wifiScan` roda o `ChannelEvaluator` (`:coreNetwork`) sobre as
 * redes vizinhas do scan via [toNdsWifiScanInfo] e carrega tanto o resultado
 * calculado (congestionamento/melhor canal) quanto a evidencia bruta (redes
 * vizinhas com canal/frequencia/RSSI/largura) que o originou — o NDS precisa
 * poder explicar por que um canal foi considerado congestionado, nao so receber
 * a conclusao. Fica `null` apenas quando `DiagnosticInput.wifiScan` e null ou nao
 * carrega nenhuma evidencia util (nem redes vizinhas, nem canal conectado).
 *
 * ## `mobile` (ADR-018 bloco 10 — issue #1837)
 * Bloco opcional montado por [toNdsMobileInfo] a partir de `DiagnosticInput.mobile`.
 * Fica `null` sem conexao movel, sem snapshot de `MonitorTelephony`, sem
 * `READ_PHONE_STATE` (`MobileDiagnosticInput.capturaReduzida = true` — mesma
 * familia de gap de permissao da issue #1735) ou sem nenhuma evidencia de sinal.
 * Nunca carrega Cell ID/TAC/MCC/MNC (proibido pela issue-mae sem revisao de
 * privacidade dedicada).
 *
 * ## Gap documentado: `dns.hijacked`
 * Ainda nao ha coleta desse dado no app (ADR-017, pendencias em aberto) — fica
 * sempre `null` aqui, mesmo com bloco `dns` presente.
 *
 * ## `historical` (ADR-018 secao 13, NDS-Snapshot-06 — issue #1838)
 * O bloco opcional `historical` carrega medias 7d/30d e degradacao calculada
 * de forma deterministica via [toNdsHistoricalInfo], que roda sobre
 * `DiagnosticInput.historico` (`HistoricalDiagnosticInput`, ja existente e
 * populado por quem consulta `MedicaoDao` em `:app` antes de montar o
 * `DiagnosticInput`). Fica `null` quando nao ha nenhum teste registrado nas
 * duas janelas (usuario novo) — nunca zeros inventados.
 *
 * `context.subcategory` e opcional no contrato v2. Quando a tela tiver um recorte
 * canônico, ele é preservado; a ausência dele não impede a avaliação v2.
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
    val qualityInfo = internet?.let {
        val loadedLatencyMs =
            it.latencyMs?.let { latency -> it.bufferbloatMs?.let { bufferbloat -> latency + bufferbloat } }
        NdsQualityInfo(
            latencyMs = it.latencyMs,
            jitterMs = it.jitterMs,
            packetLossPercent = it.perdaPercentual,
            loadedLatencyMs = loadedLatencyMs,
            bufferbloatMs = it.bufferbloatMs,
        )
    }
    val dnsInfo = dns?.let {
        NdsDnsInfo(
            primary = it.currentDnsIp,
            responseTimeMs = it.currentDnsLatencyMs,
        )
    }
    val gatewayInfo =
        if (internet?.rttGatewayMs != null || wifi?.dispositivosNaRede != null) {
            NdsGatewayInfo(
                rttGatewayMs = internet?.rttGatewayMs,
                connectedDevices = wifi?.dispositivosNaRede,
            )
        } else {
            null
        }
    val wifiScanInfo = wifiScan.toNdsWifiScanInfo(wifi?.banda())
    val mobileInfo = toNdsMobileInfo(mobile)
    val historicalInfo = historico.toNdsHistoricalInfo()

    return NdsDiagnosticsRequest(
        requestId = executionId.ifBlank { UUID.randomUUID().toString() },
        app = NdsAppInfo(id = NDS_APP_ID, version = appVersion),
        profile = ndsProfile(perfilGamer, this.context?.objective),
        capabilities = ndsCapabilities(
            wifi = wifiInfo,
            fiber = fiberInfo,
            wifiScan = wifiScanInfo,
            mobile = mobileInfo,
            historical = historicalInfo,
        ) +
            listOfNotNull(
                "usage_profiles".takeIf {
                    this.context?.objective in setOf("JOGOS_COM_LAG", "VIDEOS_TRAVAM", "CHAMADAS_CONGELAM", "SITES_DEMORAM")
                },
            ),
        connection = NdsConnectionInfo(
            type = ndsConnectionType(connectionType),
            ssid = wifi?.ssid,
            bssid = wifi?.bssidMascarado,
        ),
        wifi = wifiInfo,
        wifiScan = wifiScanInfo,
        speed = speedInfo,
        quality = qualityInfo,
        dns = dnsInfo,
        gateway = gatewayInfo,
        fiber = fiberInfo,
        mobile = mobileInfo,
        historical = historicalInfo,
        context = this.context?.let { context ->
            NdsDiagnosticContext(
                reportedProblem = context.reportedProblem,
                objective = context.objective,
                subcategory = context.subcategory,
                symptoms = context.symptoms,
                answers = context.answers,
            )
        },
    )
}

/**
 * Bloco `mobile` (ADR-018 bloco 10, issue #1837). `null` quando:
 * - `mobile` e null (fora de conexao movel, ou `MonitorTelephony` sem snapshot);
 * - `mobile.capturaReduzida` e true — snapshot capturado SEM `READ_PHONE_STATE`
 *   (GH#1662/#1735): so operadora/mcc/mnc estariam disponiveis, sem nenhuma
 *   medicao real de sinal. A regra do #1837 e "ausencia de permissao resulta em
 *   bloco omitido, nao em zeros" — omitir o bloco inteiro em vez de mandar so a
 *   operadora sem nenhum dado de sinal;
 * - nenhum dos seis campos ficaria preenchido (nenhuma evidencia util).
 *
 * Cell ID/TAC/MCC/MNC NUNCA entram aqui — proibido pela issue-mae (#1832 secao 4)
 * sem revisao explicita de privacidade.
 */
private fun toNdsMobileInfo(mobile: MobileDiagnosticInput?): NdsMobileInfo? {
    if (mobile == null || mobile.capturaReduzida) return null
    val info = NdsMobileInfo(
        operator = mobile.carrierName,
        technology = mobile.mobileTechnology,
        rsrpDbm = mobile.rsrpDbm,
        rsrqDb = mobile.rsrqDb,
        sinrDb = mobile.sinrDb,
        band = mobile.band,
    )
    val temEvidencia = info.operator != null || info.technology != null ||
        info.rsrpDbm != null || info.rsrqDb != null || info.sinrDb != null || info.band != null
    return info.takeIf { temEvidencia }
}

private fun ndsProfile(perfilGamer: Boolean, objective: String?): String? = when {
    perfilGamer -> "gamer"
    objective == "JOGOS_COM_LAG" -> "gamer"
    objective == "VIDEOS_TRAVAM" -> "streaming"
    objective == "CHAMADAS_CONGELAM" -> "video_call"
    objective == "SITES_DEMORAM" -> "navigation"
    else -> null
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
