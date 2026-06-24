package io.veloo.app.featureflags

import io.veloo.app.core.datastore.FeatureFlagStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

/**
 * Busca feature flags do worker publico e persiste via FeatureFlagStore.
 *
 * Endpoint: GET {workerBase}/feature-flags
 * Resposta: {"flags":[{"key":"ai_diagnosis_enabled","enabled":true,"scope":"public",...}]}
 *
 * Fallback em qualquer erro (offline, timeout, parse): todos os flags = true.
 * Nao bloqueia o startup — chamado em coroutine de fundo.
 */
class FeatureFlagRepository(
    private val workerBaseUrl: String,
    private val prefs: FeatureFlagStore,
) {
    companion object {
        private const val TIMEOUT_MS = 8_000
        private val DEFAULTS = mapOf(
            "ai_diagnosis_enabled" to true,
            "speedtest_enabled" to true,
            "fibra_module_enabled" to true,
        )
    }

    /**
     * Busca flags do worker e persiste em DataStore.
     * Silencioso em caso de erro — fallback true aplicado automaticamente.
     */
    suspend fun sincronizarFlags() {
        withContext(Dispatchers.IO) {
            try {
                val url = buildFlagsUrl()
                val conn = URL(url).openConnection().apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                }
                val json = conn.getInputStream().bufferedReader().use { it.readText() }
                val flagsMap = parsarFlags(json)
                if (flagsMap.isNotEmpty()) {
                    prefs.salvarFeatureFlags(flagsMap)
                }
            } catch (_: Exception) {
                // offline ou erro de rede — mantém valor anterior ou fallback no read
            }
        }
    }

    /**
     * Lê os flags persistidos. Retorna defaults para qualquer flag ausente.
     */
    suspend fun lerFlags(): Map<String, Boolean> {
        val salvo = prefs.buscarFeatureFlags()
        if (salvo.isEmpty()) return DEFAULTS
        return DEFAULTS + salvo
    }

    private fun buildFlagsUrl(): String {
        val base = workerBaseUrl
            .removeSuffix("/ai-diagnosis")
            .let { if (it.endsWith("/")) it.dropLast(1) else it }
        return "$base/feature-flags"
    }

    private fun parsarFlags(json: String): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        val root = JSONObject(json)
        val arr = root.optJSONArray("flags") ?: return emptyMap()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val scope = item.optString("scope", "")
            if (scope == "internal") continue // nao expor flags internas
            val key = item.optString("key", "")
            if (key.isBlank()) continue
            result[key] = item.optBoolean("enabled", true)
        }
        return result
    }
}
