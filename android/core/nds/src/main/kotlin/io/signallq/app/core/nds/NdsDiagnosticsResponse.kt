package io.signallq.app.core.nds

import org.json.JSONArray
import org.json.JSONObject

/** Resposta de sucesso (200 OK) de `POST /v1/diagnostics/evaluate` (ADR-017). */
data class NdsDiagnosticsResponse(
    val recommendation: NdsNextBestAction?,
    val results: List<NdsModuleResult>,
    val traces: List<NdsTrace>,
    /** Compatibilidade com o shape textual anterior do contrato NDS. */
    val recommendationText: String? = null,
) {
    /**
     * Busca um item de [results] por `module`. **Nunca assuma posicao fixa**:
     * os modulos retornados nao mapeiam 1:1 com as `capabilities` pedidas no
     * request — o ADR-017 confirmou `diagnostics.wifi` presente mesmo quando
     * so `["scoring", "ai"]` foi pedido.
     */
    fun resultFor(module: String): NdsModuleResult? = results.firstOrNull { it.module == module }
}

/** Recomendação determinística do NDS. `steps` executa a mesma ação única recomendada. */
data class NdsNextBestAction(
    val id: String,
    val type: String,
    val title: String,
    val description: String,
    val sourceFindingIds: List<String>,
    val steps: List<String>,
)

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
            recommendation = root.optJSONObject("recommendation")?.let(::parseRecommendation),
            recommendationText = root.opt("recommendation")
                ?.takeIf { it is String }
                ?.let { it as String }
                ?.takeIf(String::isNotBlank),
            results = root.optJSONArray("results")?.let(::parseResults) ?: emptyList(),
            traces = root.optJSONArray("traces")?.let(::parseTraces) ?: emptyList(),
        )
    }

    private fun parseRecommendation(obj: JSONObject): NdsNextBestAction? {
        val id = obj.optStringOrNull("id") ?: return null
        val description = obj.optStringOrNull("description") ?: return null
        return NdsNextBestAction(
            id = id,
            type = obj.optString("type", ""),
            title = obj.optString("title", ""),
            description = description,
            sourceFindingIds = stringListFrom(obj.optJSONArray("source_finding_ids")),
            steps = stringListFrom(obj.optJSONArray("steps")),
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
