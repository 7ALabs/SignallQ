package io.signallq.app.core.nds

import org.json.JSONArray
import org.json.JSONObject

/** Resposta de sucesso (200 OK) de `POST /v1/diagnostics/evaluate` (ADR-017). */
data class NdsDiagnosticsResponse(
    val recommendation: String?,
    val results: List<NdsModuleResult>,
    val traces: List<NdsTrace>,
) {
    /**
     * Busca um item de [results] por `module`. **Nunca assuma posicao fixa**:
     * os modulos retornados nao mapeiam 1:1 com as `capabilities` pedidas no
     * request — o ADR-017 confirmou `diagnostics.wifi` presente mesmo quando
     * so `["scoring", "ai"]` foi pedido.
     */
    fun resultFor(module: String): NdsModuleResult? = results.firstOrNull { it.module == module }
}

/**
 * Um item de `results[]`. [result] e [cards] ficam como estrutura JSON
 * generica (`Map`/`List` decodificados) porque o shape de `result` varia por
 * modulo e o NDS pode introduzir modulos novos sem aviso previo. Hoje so
 * `diagnostics.wifi`, `scoring` e `ai` tem decoder tipado — ver
 * [NdsModuleResult.asWifiDiagnostics], [NdsModuleResult.asScoring],
 * [NdsModuleResult.asAi]. Modulo desconhecido continua acessivel via [result]
 * bruto sem exigir mudanca neste contrato.
 */
data class NdsModuleResult(
    val module: String,
    val moduleVersion: String,
    val requestId: String,
    val warnings: List<String>,
    val missingInputs: List<String>,
    val result: Map<String, Any?>,
    val cards: List<Map<String, Any?>>,
)

data class NdsTrace(
    val module: String,
    val durationMs: Long,
    val status: String,
    val source: String? = null,
)

/** Parser tolerante do corpo 2xx de `/v1/diagnostics/evaluate`. Item malformado
 *  dentro de `results`/`traces` e descartado individualmente, nunca derruba o
 *  parse da resposta inteira. */
internal object NdsResponseParser {
    fun parse(raw: String): NdsDiagnosticsResponse {
        val root = JSONObject(raw)
        return NdsDiagnosticsResponse(
            recommendation = root.optStringOrNull("recommendation"),
            results = root.optJSONArray("results")?.let(::parseResults) ?: emptyList(),
            traces = root.optJSONArray("traces")?.let(::parseTraces) ?: emptyList(),
        )
    }

    private fun parseResults(arr: JSONArray): List<NdsModuleResult> {
        val list = mutableListOf<NdsModuleResult>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val module = item.optStringOrNull("module") ?: continue
            list.add(
                NdsModuleResult(
                    module = module,
                    moduleVersion = item.optString("module_version", ""),
                    requestId = item.optString("request_id", ""),
                    warnings = stringListFrom(item.optJSONArray("warnings")),
                    missingInputs = stringListFrom(item.optJSONArray("missing_inputs")),
                    result = item.optJSONObject("result")?.toKotlinMap() ?: emptyMap(),
                    cards = mapListFrom(item.optJSONArray("cards")),
                ),
            )
        }
        return list
    }

    private fun parseTraces(arr: JSONArray): List<NdsTrace> {
        val list = mutableListOf<NdsTrace>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            list.add(
                NdsTrace(
                    module = item.optString("module", ""),
                    durationMs = item.optLong("duration_ms", 0L),
                    status = item.optString("status", ""),
                    source = item.optStringOrNull("source"),
                ),
            )
        }
        return list
    }

    private fun stringListFrom(arr: JSONArray?): List<String> =
        arr?.toKotlinList()?.filterIsInstance<String>() ?: emptyList()

    @Suppress("UNCHECKED_CAST")
    private fun mapListFrom(arr: JSONArray?): List<Map<String, Any?>> =
        arr?.toKotlinList()?.filterIsInstance<Map<String, Any?>>() ?: emptyList()
}
