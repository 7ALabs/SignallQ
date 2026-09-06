package io.signallq.app.core.nds

// =============================================================================
// Decoders tipados para os modulos confirmados no ADR-017.
// =============================================================================
// NdsModuleResult.result e generico (Map<String, Any?>) porque o shape varia
// por modulo. As funcoes abaixo dao uma leitura tipada e tolerante para os 3
// modulos confirmados; modulo desconhecido simplesmente nao tem decoder ainda
// — continua acessivel via NdsModuleResult.result bruto. Nao lancam excecao:
// campo ausente ou de tipo inesperado devolve null (para o resultado inteiro)
// ou um default seguro (para listas/numeros dentro dele), no mesmo espirito
// tolerante do parser de AiDiagnosisRepository.

private fun Map<String, Any?>.stringOrNull(key: String): String? = this[key] as? String

private fun Map<String, Any?>.intOrNull(key: String): Int? = (this[key] as? Number)?.toInt()

private fun Map<String, Any?>.booleanOrNull(key: String): Boolean? = this[key] as? Boolean

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.mapOrNull(key: String): Map<String, Any?>? = this[key] as? Map<String, Any?>

private fun Map<String, Any?>.stringListOrEmpty(key: String): List<String> =
    (this[key] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

/** `result` do modulo `"scoring"` (module_version `"1.1.0"` no exemplo do ADR-017). */
data class NdsScoringResult(
    val score: Int,
    val veredicto: String,
    val tipoConexao: String,
    val observedDimensions: Int,
    val dimensoes: List<String>,
)

/** `null` se [NdsModuleResult.module] nao for `"scoring"` ou faltar campo obrigatorio. */
fun NdsModuleResult.asScoring(): NdsScoringResult? {
    if (module != "scoring") return null
    val score = result.intOrNull("score") ?: return null
    val veredicto = result.stringOrNull("veredicto") ?: return null
    return NdsScoringResult(
        score = score,
        veredicto = veredicto,
        tipoConexao = result.stringOrNull("tipo_conexao") ?: "",
        observedDimensions = result.intOrNull("observed_dimensions") ?: 0,
        dimensoes = result.stringListOrEmpty("dimensoes"),
    )
}

data class NdsAiExplanation(
    val tituloAmigavel: String,
    val resumoTecnicoTraduzido: String,
)

/** `result` do modulo `"ai"` (module_version `"1.5.0"` no exemplo do ADR-017). */
data class NdsAiResult(
    val tokensUsed: Int,
    val aiModelUsed: String,
    val fallbackUsed: Boolean,
    val explanationSource: String,
    val explanationStatus: String,
    val explanation: NdsAiExplanation?,
    val sourceFindingIds: List<String>,
)

/** `null` se [NdsModuleResult.module] nao for `"ai"`. */
fun NdsModuleResult.asAi(): NdsAiResult? {
    if (module != "ai") return null
    val explanationMap = result.mapOrNull("explanation")
    val explanation =
        explanationMap?.let { e ->
            val titulo = e.stringOrNull("titulo_amigavel")
            val resumo = e.stringOrNull("resumo_tecnico_traduzido")
            if (titulo != null && resumo != null) NdsAiExplanation(titulo, resumo) else null
        }
    return NdsAiResult(
        tokensUsed = result.intOrNull("tokens_used") ?: 0,
        aiModelUsed = result.stringOrNull("ai_model_used") ?: "",
        fallbackUsed = result.booleanOrNull("fallback_used") ?: false,
        explanationSource = result.stringOrNull("explanation_source") ?: "",
        explanationStatus = result.stringOrNull("explanation_status") ?: "",
        explanation = explanation,
        sourceFindingIds = result.stringListOrEmpty("source_finding_ids"),
    )
}

/** `result` do modulo `"diagnostics.wifi"` (module_version `"1.0.0"` no exemplo do ADR-017). */
data class NdsWifiDiagnosticsResult(
    val matchedRules: List<String>,
)

/** `null` se [NdsModuleResult.module] nao for `"diagnostics.wifi"`. */
fun NdsModuleResult.asWifiDiagnostics(): NdsWifiDiagnosticsResult? {
    if (module != "diagnostics.wifi") return null
    return NdsWifiDiagnosticsResult(matchedRules = result.stringListOrEmpty("matched_rules"))
}
