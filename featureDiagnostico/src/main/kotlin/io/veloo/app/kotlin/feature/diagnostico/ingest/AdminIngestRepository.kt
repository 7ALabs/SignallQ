package io.veloo.app.feature.diagnostico.ingest

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "AdminIngestRepository"

/**
 * Envia telemetria de diagnostico e uso de IA para o signallq-admin-worker.
 *
 * Fire-and-forget: todas as falhas sao logadas com [Log.w] e ignoradas.
 * Nenhuma excecao propaga para o chamador — ingest nunca bloqueia o fluxo principal.
 *
 * Autenticacao: Bearer [ingestKey] (INGEST_KEY — chave com scope limitado a /ingest/,
 * diferente do ADMIN_SECRET usado pelo painel web). Vazar INGEST_KEY nao da acesso
 * de leitura aos dados do painel.
 *
 * @param baseUrl URL base do admin worker, ex: "https://signallq-admin.giammattey-luiz.workers.dev"
 * @param ingestKey Chave de autenticacao para endpoints /ingest/ (BuildConfig.ADMIN_INGEST_KEY)
 * @param client OkHttpClient com timeout adequado para telemetria (curto — e best-effort)
 */
class AdminIngestRepository(
    private val baseUrl: String,
    private val ingestKey: String,
    private val client: OkHttpClient,
) {
    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    /**
     * Envia payload de diagnostico concluido. Fire-and-forget.
     * Nao lanca excecao em nenhum cenario.
     */
    suspend fun sendDiagnostic(payload: DiagnosticIngestPayload) {
        if (baseUrl.isBlank() || ingestKey.isBlank()) {
            Log.w(TAG, "sendDiagnostic ignorado: baseUrl ou ingestKey nao configurados")
            return
        }
        runCatching {
            withContext(Dispatchers.IO) {
                val body = payload.toJson().toString()
                    .toRequestBody(mediaTypeJson)
                val req = Request.Builder()
                    .url(baseUrl.trimEnd('/') + "/ingest/diagnostic")
                    .addHeader("Authorization", "Bearer $ingestKey")
                    .post(body)
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "sendDiagnostic HTTP ${resp.code} — id=${payload.id}")
                    } else {
                        Log.d(TAG, "sendDiagnostic ok — id=${payload.id}")
                    }
                }
            }
        }.onFailure { t ->
            Log.w(TAG, "sendDiagnostic falhou (ignorando): ${t.message}")
        }
    }

    /**
     * Envia payload de uso de IA. Fire-and-forget.
     * Nao lanca excecao em nenhum cenario.
     */
    suspend fun sendAiUsage(payload: AiUsageIngestPayload) {
        if (baseUrl.isBlank() || ingestKey.isBlank()) {
            Log.w(TAG, "sendAiUsage ignorado: baseUrl ou ingestKey nao configurados")
            return
        }
        runCatching {
            withContext(Dispatchers.IO) {
                val body = payload.toJson().toString()
                    .toRequestBody(mediaTypeJson)
                val req = Request.Builder()
                    .url(baseUrl.trimEnd('/') + "/ingest/ai-usage")
                    .addHeader("Authorization", "Bearer $ingestKey")
                    .post(body)
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "sendAiUsage HTTP ${resp.code} — id=${payload.id}")
                    } else {
                        Log.d(TAG, "sendAiUsage ok — id=${payload.id} model=${payload.model}")
                    }
                }
            }
        }.onFailure { t ->
            Log.w(TAG, "sendAiUsage falhou (ignorando): ${t.message}")
        }
    }

    // ---- Serialização ----

    private fun DiagnosticIngestPayload.toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("created_at", createdAt)
        networkType?.let { o.put("network_type", it) }
        status?.let { o.put("status", it) }
        score?.let { o.put("score", it) }
        downloadMbps?.let { o.put("download_mbps", it) }
        uploadMbps?.let { o.put("upload_mbps", it) }
        latencyMs?.let { o.put("latency_ms", it) }
        jitterMs?.let { o.put("jitter_ms", it) }
        packetLoss?.let { o.put("packet_loss", it) }
        if (issues.isNotEmpty()) {
            val arr = JSONArray()
            issues.forEach { arr.put(it) }
            o.put("issues", arr)
        }
        operator?.let { o.put("operator", it) }
        deviceModel?.let { o.put("device_model", it) }
        osVersion?.let { o.put("os_version", it) }
        appVersion?.let { o.put("app_version", it) }
        if (aiSummaryReport.isNotBlank()) o.put("ai_summary_report", aiSummaryReport)
        environment?.let { o.put("environment", it) }
        distChannel?.let { o.put("dist_channel", it) }
        buildType?.let { o.put("build_type", it) }
        versionCode?.let { o.put("version_code", it) }
        deviceId?.let { o.put("device_id", it) }
        return o
    }

    private fun AiUsageIngestPayload.toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("model", model)
        sessionId?.let { o.put("session_id", it) }
        o.put("created_at", createdAt)
        o.put("prompt_tokens", promptTokens)
        o.put("completion_tokens", completionTokens)
        o.put("total_tokens", totalTokens)
        costUsd?.let { o.put("cost_usd", it) }
        environment?.let { o.put("environment", it) }
        distChannel?.let { o.put("dist_channel", it) }
        buildType?.let { o.put("build_type", it) }
        versionCode?.let { o.put("version_code", it) }
        deviceId?.let { o.put("device_id", it) }
        return o
    }
}
