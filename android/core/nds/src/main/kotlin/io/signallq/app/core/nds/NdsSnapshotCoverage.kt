package io.signallq.app.core.nds

import io.signallq.app.core.diagnostico.ConnectionType

/**
 * Telemetria de cobertura do snapshot NDS (NDS-Snapshot-12, issue #1844, épico #1832 seção 17
 * "Observabilidade"). Sem esta camada não dá pra saber em produção quantos usuários realmente
 * enviam os blocos novos do payload (ADR-018) nem auditar a cobertura real do que chega ao NDS.
 *
 * Este arquivo é intencionalmente uma adição nova — não toca em
 * [NdsDiagnosticsRequest]/[toNdsDiagnosticsRequest] (mudança concorrente da issue #1842,
 * proveniência/`source`, na mesma branch `main`). A análise abaixo só lê os campos públicos já
 * expostos por [NdsDiagnosticsRequest] e por [ConnectionType]/`capturaReduzida`
 * (`MobileDiagnosticInput`), sem depender de nenhum campo novo dessas issues.
 */

/** Versão do contrato `DiagnosticSnapshot` (ADR-018) — não é a versão do app nem do documento da
 *  ADR. Incrementar apenas quando um bloco novo ou uma mudança incompatível do schema justificar
 *  segmentar a telemetria por versão de payload. */
const val NDS_SNAPSHOT_SCHEMA_VERSION = "1"

/** Um dos blocos opcionais do payload NDS (ADR-018) — `execution`/`app` ficam fora porque são
 *  sempre presentes (não são um sinal de cobertura variável). */
enum class NdsSnapshotBlock(val jsonKey: String) {
    CONNECTION("connection"),
    WIFI("wifi"),
    WIFI_SCAN("wifiScan"),
    SPEED("speed"),
    QUALITY("quality"),
    DNS("dns"),
    GATEWAY("gateway"),
    FIBER("fiber"),
    MOBILE("mobile"),
    HISTORICAL("historical"),
    LOCAL_EQUIPMENT("localEquipment"),
    PLAN("plan"),
    CONTEXT("context"),
}

/**
 * Estado de um bloco nesta execução. [missingReason] é sempre `null` quando [present] é `true`;
 * vocabulário fechado desta fatia (não é o vocabulário do contrato NDS): `"no_measurement"`,
 * `"no_data"`, `"not_wifi"`, `"not_mobile"`, `"no_permission"`, `"no_fiber_equipment"`,
 * `"no_equipment_detected"`, `"insufficient_history"`, `"not_informed_by_user"`, `"no_context"`.
 */
data class NdsBlockCoverage(
    val block: NdsSnapshotBlock,
    val present: Boolean,
    /** `true` quando a ausência deste bloco reduz materialmente a confiança que o NDS pode
     *  depositar no diagnóstico desta execução — ver [NdsSnapshotCoverage.missingCriticalBlocks]. */
    val critical: Boolean,
    val missingReason: String? = null,
)

/**
 * Resultado da análise de cobertura de uma [NdsDiagnosticsRequest] já montada. Não recalcula nada
 * do payload em si — só descreve, para fins de telemetria/depuração, o que foi montado.
 */
data class NdsSnapshotCoverage(
    val blocks: List<NdsBlockCoverage>,
    /** Contagem de campos-folha não nulos em todo o payload (todos os blocos, incluindo os
     *  sempre-presentes `app`/`locale`) — mede a riqueza do snapshot, não só quantos blocos
     *  existem. Um bloco presente com todos os campos nulos ainda soma pouco aqui. */
    val fieldsPresentCount: Int,
) {
    val blocksPresent: List<String> get() = blocks.filter { it.present }.map { it.block.jsonKey }

    val missingCriticalBlocks: List<String> get() =
        blocks.filter { !it.present && it.critical }.map { it.block.jsonKey }

    /**
     * Formato de log de debug exigido pela issue #1844 — uma linha por bloco, `bloco=present` ou
     * `bloco=missing:motivo`. Quem chama decide se/onde emite (build de debug apenas — nunca em
     * release, ver [io.signallq.app.feature.diagnostico.nds.NdsDiagnosticRepository]).
     */
    fun toDebugLogLines(): List<String> = blocks.map { coverage ->
        if (coverage.present) {
            "${coverage.block.jsonKey}=present"
        } else {
            "${coverage.block.jsonKey}=missing:${coverage.missingReason ?: "unknown"}"
        }
    }
}

/**
 * Analisa quais blocos de [request] estão presentes/ausentes e por quê, a partir do mesmo
 * `DiagnosticInput` que originou o request (via [connectionType]/[mobileCapturaReduzida] — os
 * únicos dois sinais de contexto que este módulo precisa, sem reimportar `DiagnosticInput`
 * inteiro). Blocos críticos: `connection` e `speed` sempre; `wifi` quando a conexão atual é
 * Wi-Fi; `mobile` quando é rede móvel — sem um desses, o NDS não tem o sinal mínimo de rádio para
 * a conexão em uso. `wifiScan`/`dns`/`gateway`/`fiber`/`historical`/`localEquipment`/`plan`/
 * `context`/`quality` enriquecem o diagnóstico, mas a ausência deles não é crítica.
 */
fun analyzeNdsSnapshotCoverage(
    request: NdsDiagnosticsRequest,
    connectionType: ConnectionType,
    mobileCapturaReduzida: Boolean,
): NdsSnapshotCoverage {
    val isWifi = connectionType == ConnectionType.wifi
    val isMobile = connectionType == ConnectionType.mobile

    val blocks = listOf(
        NdsBlockCoverage(
            block = NdsSnapshotBlock.CONNECTION,
            present = request.connection != null,
            critical = true,
            missingReason = "no_data".takeIf { request.connection == null },
        ),
        NdsBlockCoverage(
            block = NdsSnapshotBlock.SPEED,
            present = request.speed != null,
            critical = true,
            missingReason = "no_measurement".takeIf { request.speed == null },
        ),
        NdsBlockCoverage(
            block = NdsSnapshotBlock.QUALITY,
            present = request.quality != null,
            critical = false,
            missingReason = "no_measurement".takeIf { request.quality == null },
        ),
        NdsBlockCoverage(
            block = NdsSnapshotBlock.WIFI,
            present = request.wifi != null,
            critical = isWifi,
            missingReason = if (request.wifi != null) null else if (!isWifi) "not_wifi" else "no_data",
        ),
        NdsBlockCoverage(
            block = NdsSnapshotBlock.WIFI_SCAN,
            present = request.wifiScan != null,
            critical = false,
            missingReason = if (request.wifiScan != null) null else if (!isWifi) "not_wifi" else "no_permission",
        ),
        NdsBlockCoverage(
            block = NdsSnapshotBlock.MOBILE,
            present = request.mobile != null,
            critical = isMobile,
            missingReason = if (request.mobile != null) {
                null
            } else if (!isMobile) {
                "not_mobile"
            } else if (mobileCapturaReduzida) {
                "no_permission"
            } else {
                "no_data"
            },
        ),
        NdsBlockCoverage(
            block = NdsSnapshotBlock.DNS,
            present = request.dns != null,
            critical = false,
            missingReason = "no_data".takeIf { request.dns == null },
        ),
        NdsBlockCoverage(
            block = NdsSnapshotBlock.GATEWAY,
            present = request.gateway != null,
            critical = false,
            missingReason = "no_data".takeIf { request.gateway == null },
        ),
        NdsBlockCoverage(
            block = NdsSnapshotBlock.FIBER,
            present = request.fiber != null,
            critical = false,
            missingReason = "no_fiber_equipment".takeIf { request.fiber == null },
        ),
        NdsBlockCoverage(
            block = NdsSnapshotBlock.HISTORICAL,
            present = request.historical != null,
            critical = false,
            missingReason = "insufficient_history".takeIf { request.historical == null },
        ),
        NdsBlockCoverage(
            block = NdsSnapshotBlock.LOCAL_EQUIPMENT,
            present = request.localEquipment != null,
            critical = false,
            missingReason = "no_equipment_detected".takeIf { request.localEquipment == null },
        ),
        NdsBlockCoverage(
            block = NdsSnapshotBlock.PLAN,
            present = request.plan != null,
            critical = false,
            missingReason = "not_informed_by_user".takeIf { request.plan == null },
        ),
        NdsBlockCoverage(
            block = NdsSnapshotBlock.CONTEXT,
            present = request.context != null,
            critical = false,
            missingReason = "no_context".takeIf { request.context == null },
        ),
    )

    return NdsSnapshotCoverage(blocks = blocks, fieldsPresentCount = countPresentFields(request))
}

/**
 * Conta campos-folha não nulos em [request]. Espelha propositalmente a mesma árvore de presença
 * que [NdsDiagnosticsRequest.toJson] usa para decidir o que serializar — duplicação aceita porque
 * `toJson()` é `internal` (não visível fora do módulo `core/nds`) e esta contagem é só telemetria,
 * nunca o contrato em si; ver dívida registrada na PR desta issue (#1844) se algum dia valer a
 * pena consolidar as duas em uma única fonte.
 */
private fun countPresentFields(request: NdsDiagnosticsRequest): Int {
    var count = 3 // app.id + app.version + locale, sempre presentes
    request.profile?.let { count++ }
    if (request.capabilities.isNotEmpty()) count++

    request.connection?.let { c ->
        count++ // type
        count += listOfNotNull(c.ssid, c.bssid, c.natStatus).size
    }
    request.wifi?.let { w ->
        count += listOfNotNull(w.rssi, w.band, w.channel, w.linkSpeed, w.standard).size
    }
    request.wifiScan?.let { ws ->
        count += listOfNotNull(ws.connectedChannel, ws.channelCongestion, ws.bestChannel, ws.neighborCount, ws.algorithmVersion).size
        if (ws.neighbors.isNotEmpty()) count++
    }
    request.speed?.let { s ->
        count += listOfNotNull(s.pingMs, s.jitterMs, s.downloadMbps, s.uploadMbps, s.packetLossPercent).size
    }
    request.quality?.let { q ->
        count += listOfNotNull(q.latencyMs, q.jitterMs, q.packetLossPercent, q.loadedLatencyMs, q.bufferbloatMs).size
    }
    request.dns?.let { d ->
        count += listOfNotNull(d.primary, d.responseTimeMs, d.hijacked, d.providerName, d.bestName, d.bestLatencyMs, d.grade).size
        count++ // comparisonAvailable sempre serializado (nao-nulo na origem)
        count += listOfNotNull(
            d.coherenceAlertLevel,
            d.coherenceConsecutiveDivergences,
            d.coherenceDivergenceRatePercent,
            d.privateDnsActive,
            d.privateDnsHostname,
        ).size
    }
    request.gateway?.let { g ->
        count += listOfNotNull(g.rttGatewayMs, g.connectedDevices).size
    }
    request.fiber?.let { f ->
        count += listOfNotNull(f.rxPowerDbm, f.txPowerDbm, f.temperatureC, f.voltageV).size
    }
    request.mobile?.let { m ->
        count += listOfNotNull(m.operator, m.technology, m.rsrpDbm, m.rsrqDb, m.sinrDb, m.band).size
    }
    request.historical?.let { h ->
        count += 2 // testsCount7d/testsCount30d, sempre serializados
        count += listOfNotNull(
            h.avgDownload7d, h.avgUpload7d, h.avgPing7d, h.avgDns7d,
            h.avgDownload30d, h.avgUpload30d, h.avgPing30d, h.avgDns30d,
            h.degradationDetected, h.degradationPercent, h.worstTimeWindow, h.bestTimeWindow,
        ).size
    }
    request.localEquipment?.let { le ->
        count += listOfNotNull(le.vendor, le.model, le.firmwareVersion).size
        // deviceType/supportLevel/connectionStatus/fiberStatus/wanStatus/wifiStatus/lanStatus/
        // connectedClients sao nao-nulos na origem, sempre serializados quando o bloco existe.
        count += 8
    }
    request.plan?.let { p ->
        p.contractedSpeedMbps?.let { count++ }
    }
    request.context?.let { c ->
        count += listOfNotNull(
            c.reportedProblem?.takeIf(String::isNotBlank),
            c.objective?.takeIf(String::isNotBlank),
            c.subcategory?.takeIf(String::isNotBlank),
        ).size
        if (c.symptoms.isNotEmpty()) count++
        if (c.answers.isNotEmpty()) count++
    }
    return count
}
