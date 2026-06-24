package io.veloo.app.feature.diagnostico.ingest

import io.veloo.app.core.database.MedicaoEntity
import io.veloo.app.core.database.chat.ChatSessionEntity

// ---------------------------------------------------------------------------
// Utilitarios de serialização — usados por SignallQOrchestrator ao montar payloads.
// ---------------------------------------------------------------------------

/**
 * Converte frequência em MHz para string de banda Wi-Fi.
 *
 * Retorna null se [frequenciaMhz] for null, <= 0 (Samsung One UI apos reconexao)
 * ou fora dos ranges conhecidos — nunca aborta o envio.
 */
fun frequenciaMhzParaBanda(frequenciaMhz: Int?): String? = when {
    frequenciaMhz == null || frequenciaMhz <= 0 -> null
    frequenciaMhz in 2412..2484 -> "wifi_2.4GHz"
    frequenciaMhz in 5170..5825 -> "wifi_5GHz"
    frequenciaMhz in 5925..7125 -> "wifi_6GHz"
    else -> "wifi"
}

/**
 * Converte ID interno de issue do engine de diagnostico para label legivel em snake_case.
 *
 * Se nao houver mapeamento, normaliza o proprio ID para snake_case — nenhum issue e descartado.
 */
fun idParaIssueLabel(id: String): String = when {
    id.contains("RSSI", ignoreCase = true) || id.contains("SINAL", ignoreCase = true) -> "sinal_fraco"
    id.contains("LATENCIA", ignoreCase = true) || id.contains("LATENCY", ignoreCase = true) -> "alta_latencia"
    id.contains("DNS", ignoreCase = true) -> "falha_dns"
    id.contains("JITTER", ignoreCase = true) -> "jitter_alto"
    id.contains("PACKET", ignoreCase = true) || id.contains("PERDA", ignoreCase = true) -> "perda_de_pacotes"
    id.contains("UPLOAD", ignoreCase = true) -> "upload_lento"
    id.contains("DOWNLOAD", ignoreCase = true) -> "download_lento"
    id.contains("FIBRA", ignoreCase = true) || id.contains("GPON", ignoreCase = true) -> "problema_fibra"
    id.contains("GATEWAY", ignoreCase = true) -> "gateway_inacessivel"
    id.contains("BUFFERBLOAT", ignoreCase = true) -> "bufferbloat"
    id.contains("CANAL", ignoreCase = true) || id.contains("CHANNEL", ignoreCase = true) -> "interferencia_canal_wifi"
    id.contains("BAND", ignoreCase = true) -> "problema_banda"
    else -> id.lowercase().replace("-", "_").replace(" ", "_")
}

// ---------------------------------------------------------------------------

/**
 * Payload enviado ao endpoint POST /ingest/diagnostic do signallq-admin-worker.
 *
 * Todos os campos opcionais (exceto [id]) sao omitidos se null — o worker
 * preenche defaults no lado dele. Fire-and-forget: falha de envio e ignorada.
 */
data class DiagnosticIngestPayload(
    /** UUID gerado no inicio da sessao de diagnostico. Obrigatorio. */
    val id: String,
    /** Unix epoch em segundos. */
    val createdAt: Long = System.currentTimeMillis() / 1000,
    /** "wifi", "4g", "5g", "ethernet" ou null. */
    val networkType: String? = null,
    /** "completed", "failed" ou "partial". */
    val status: String? = null,
    /** Score 0-100 calculado pelo engine local. */
    val score: Int? = null,
    val downloadMbps: Float? = null,
    val uploadMbps: Float? = null,
    val latencyMs: Int? = null,
    val jitterMs: Int? = null,
    val packetLoss: Float? = null,
    /** Lista de problemas identificados, ex: ["alta_latencia", "sinal_fraco"]. */
    val issues: List<String> = emptyList(),
    /** Operadora movel ou ISP identificado, ex: "Claro", "Vivo". Null se desconhecido. */
    val operator: String? = null,
    /** Ex: "Samsung Galaxy S23", "Motorola Moto G84". */
    val deviceModel: String? = null,
    /** Ex: "Android 14". */
    val osVersion: String? = null,
    /** Versao do app, ex: "0.21.0". */
    val appVersion: String? = null,
    /** Resumo gerado pela IA ao final do diagnostico. Vazio se IA nao foi chamada ou falhou. */
    val aiSummaryReport: String = "",
    /** "production" ou "staging" — derivado de BuildConfig.DEBUG. */
    val environment: String? = null,
    /** Canal de distribuicao: "play_store", "sideload" ou "unknown". */
    val distChannel: String? = null,
    /** Tipo de build: "release", "debug". */
    val buildType: String? = null,
    /** versionCode do app (inteiro). */
    val versionCode: Int? = null,
    /** UUID anonimo persistente do dispositivo. Sem PII. */
    val deviceId: String? = null,
)

/**
 * Payload enviado ao endpoint POST /ingest/ai-usage do signallq-admin-worker.
 *
 * Correlaciona com [DiagnosticIngestPayload] via [sessionId].
 * Fire-and-forget: falha de envio e ignorada.
 */
data class AiUsageIngestPayload(
    /** UUID unico por chamada de IA. Obrigatorio. */
    val id: String,
    /** Nome do modelo usado, ex: "@cf/qwen/qwen3-30b-a3b-fp8". Obrigatorio. */
    val model: String,
    /** UUID do diagnostico correspondente para correlacao. */
    val sessionId: String? = null,
    /** Unix epoch em segundos. */
    val createdAt: Long = System.currentTimeMillis() / 1000,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = promptTokens + completionTokens,
    /** Custo em USD. Null = worker calcula fallback baseado em tokens. */
    val costUsd: Double? = null,
    /** "production" ou "staging". */
    val environment: String? = null,
    /** Canal de distribuicao: "play_store", "sideload" ou "unknown". */
    val distChannel: String? = null,
    /** Tipo de build: "release", "debug". */
    val buildType: String? = null,
    /** versionCode do app. */
    val versionCode: Int? = null,
    /** UUID anonimo persistente do dispositivo. Sem PII. */
    val deviceId: String? = null,
)

// ---------------------------------------------------------------------------
// Mapeamento de entidades Room para payloads de ingest (sync retroativo)
// ---------------------------------------------------------------------------

/**
 * Converte [MedicaoEntity] para [DiagnosticIngestPayload].
 *
 * Precondição: chamador DEVE garantir que [MedicaoEntity.contaminado] == false
 * antes de invocar. Esta funcao nao valida — filtragem e responsabilidade do chamador.
 */
fun MedicaoEntity.toIngestPayload(
    environment: String? = null,
    distChannel: String? = null,
    buildType: String? = null,
    versionCode: Int? = null,
    deviceId: String? = null,
) = DiagnosticIngestPayload(
    id = id,
    createdAt = timestampEpochMs / 1000,
    networkType = connectionType,
    status = "completed",
    score = null,
    downloadMbps = downloadMbps?.toFloat(),
    uploadMbps = uploadMbps?.toFloat(),
    latencyMs = latencyMs?.toInt(),
    jitterMs = jitterMs?.toInt(),
    packetLoss = perdaPercentual?.toFloat(),
    issues = gargaloPrimario?.takeIf { it.isNotBlank() }
        ?.let { listOf(idParaIssueLabel(it)) } ?: emptyList(),
    operator = operadoraMovel?.takeIf { it.isNotBlank() },
    environment = environment,
    distChannel = distChannel,
    buildType = buildType,
    versionCode = versionCode,
    deviceId = deviceId,
)

/**
 * Converte [ChatSessionEntity] para [AiUsageIngestPayload].
 *
 * Precondição: chamador DEVE garantir que [ChatSessionEntity.status] == "completed"
 * e [ChatSessionEntity.nomeModelo] != null antes de invocar.
 */
fun ChatSessionEntity.toIngestPayload(
    environment: String? = null,
    distChannel: String? = null,
    buildType: String? = null,
    versionCode: Int? = null,
    deviceId: String? = null,
) = AiUsageIngestPayload(
    id = id,
    model = nomeModelo ?: "unknown",
    sessionId = null,
    createdAt = criadoEmEpochMs / 1000,
    promptTokens = 0,
    completionTokens = 0,
    totalTokens = 0,
    costUsd = null,
    environment = environment,
    distChannel = distChannel,
    buildType = buildType,
    versionCode = versionCode,
    deviceId = deviceId,
)
