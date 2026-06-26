package io.veloo.app.featureflags

import io.veloo.app.core.datastore.FeatureFlagStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * Busca feature flags do Admin Worker e persiste via FeatureFlagStore.
 *
 * Endpoint primario (SIG-13): GET {adminWorkerBase}/flags
 * Resposta: {"flags":[{"key":"feature_speedtest","enabled":true}]}
 *
 * Endpoint legado: GET {adminWorkerBase}/feature-flags
 * Resposta: {"flags":[{"key":"ai_diagnosis_enabled","enabled":true,"scope":"public"}]}
 *
 * Ambos sao consumidos e mesclados — flags do /flags tem precedencia sobre legado.
 * Fallback em qualquer erro: todos os flags = true.
 */
class FeatureFlagRepository(
    private val adminWorkerBaseUrl: String,
    private val prefs: FeatureFlagStore,
) {
    companion object {
        private const val TIMEOUT_MS = 8_000
        private val DEFAULTS =
            mapOf(
                // Flags SIG-13 — endpoint /flags
                "feature_speedtest" to true,
                "feature_wifi" to true,
                "feature_diagnostico_ia" to true,
                "feature_dns" to true,
                "feature_fibra" to true,
                "feature_devices" to true,
                // Flags legadas — endpoint /feature-flags
                "ai_diagnosis_enabled" to true,
                "speedtest_enabled" to true,
                "fibra_module_enabled" to true,
            )
    }

    /**
     * Busca flags de ambos os endpoints e persiste em DataStore.
     * Silencioso em caso de erro — fallback true aplicado automaticamente.
     */
    suspend fun sincronizarFlags() {
        withContext(Dispatchers.IO) {
            val mesclado = mutableMapOf<String, Boolean>()
            // Legado primeiro, depois /flags sobrescreve onde houver conflito
            mesclado.putAll(buscarEndpoint(buildLegacyUrl(), parsarFlagsLegado))
            mesclado.putAll(buscarEndpoint(buildFlagsUrl(), parsarFlagsSig13))
            if (mesclado.isNotEmpty()) {
                prefs.salvarFeatureFlags(mesclado)
            }
        }
    }

    /**
     * Le os flags persistidos. Retorna defaults para qualquer flag ausente.
     */
    suspend fun lerFlags(): Map<String, Boolean> {
        val salvo = prefs.buscarFeatureFlags()
        if (salvo.isEmpty()) return DEFAULTS
        return DEFAULTS + salvo
    }

    private fun buildBaseUrl(): String =
        adminWorkerBaseUrl
            .removeSuffix("/ai-diagnosis")
            .trimEnd('/')

    // GET /flags — schema SIG-13: {flags:[{key, enabled}]}
    private fun buildFlagsUrl(): String = "${buildBaseUrl()}/flags"

    // GET /feature-flags — schema legado: {flags:[{key, enabled, scope}]}
    private fun buildLegacyUrl(): String = "${buildBaseUrl()}/feature-flags"

    private fun buscarEndpoint(
        url: String,
        parser: (String) -> Map<String, Boolean>,
    ): Map<String, Boolean> =
        try {
            val conn =
                URL(url).openConnection().apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                }
            val json = conn.getInputStream().bufferedReader().use { it.readText() }
            parser(json)
        } catch (_: Exception) {
            emptyMap()
        }

    // Parser SIG-13: sem scope, sem filtro
    private val parsarFlagsSig13: (String) -> Map<String, Boolean> = { json ->
        val result = mutableMapOf<String, Boolean>()
        runCatching {
            val arr = JSONObject(json).optJSONArray("flags") ?: return@runCatching
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val key = item.optString("key", "")
                if (key.isBlank()) continue
                result[key] = item.optBoolean("enabled", true)
            }
        }
        result
    }

    // Parser legado: filtra scope internal
    private val parsarFlagsLegado: (String) -> Map<String, Boolean> = { json ->
        val result = mutableMapOf<String, Boolean>()
        runCatching {
            val arr = JSONObject(json).optJSONArray("flags") ?: return@runCatching
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                if (item.optString("scope", "") == "internal") continue
                val key = item.optString("key", "")
                if (key.isBlank()) continue
                result[key] = item.optBoolean("enabled", true)
            }
        }
        result
    }
}
