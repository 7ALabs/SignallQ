package io.signallq.app.feature.diagnostico.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Identidade/contato de um provedor resolvido a partir do diretorio remoto do
 * worker `signallq-diagnostic` (`ProviderRecord`, GH#965) — cauda longa
 * (regionais/menores nao catalogados localmente). NUNCA usado para as ~12
 * operadoras principais, que continuam 100% locais via `OperadoraLogoCatalog`/
 * `BancoOperadoras` (`:app`, nao alterados por esta issue).
 */
data class RemoteProviderInfo(
    val providerId: String,
    val displayName: String,
    val logoUrl: String?,
    val sacPhone: String?,
    val technicalSupportPhone: String?,
    val whatsappUrl: String?,
    val websiteUrl: String?,
    val customerAreaUrl: String?,
    val ombudsmanPhone: String?,
)

/**
 * Client do diretorio remoto de provedores (rotas `GET /providers/...`) — GH#965.
 *
 * Mesma estrategia de timeout curto + fallback silencioso de
 * [RemoteDiagnosticRepository] (ver kdoc la): qualquer falha (sem rede,
 * timeout, 404, JSON invalido) devolve `null` em vez de lancar excecao. O
 * caller (`:app`, resolver de identidade de operadora) decide o fallback
 * final — este repository NUNCA decide UI/copy de fallback sozinho.
 */
class ProviderDirectoryRepository(
    private val baseUrl: String,
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build(),
) {

    /** Busca por id exato (quando ja se sabe o `providerId`, ex.: vindo de deteccao previa). */
    suspend fun findById(providerId: String): RemoteProviderInfo? =
        get("/providers/${URLEncoder.encode(providerId, "UTF-8")}")?.let(::parseProvider)

    /**
     * Busca por nome bruto de ISP (ex.: `SnapshotRede`/`TelephonyManager`) — o
     * mesmo tipo de entrada que [io.signallq.app.ui.BancoOperadoras.resolver]
     * ja consome localmente. Usa `/providers/search`, pega o primeiro
     * resultado (ordenado por relevancia no worker). `null` quando nao ha
     * nenhum match ou a chamada falha.
     */
    suspend fun searchByName(rawName: String): RemoteProviderInfo? {
        if (rawName.isBlank()) return null
        val url = baseUrl.trimEnd('/') + "/providers/search?q=" + URLEncoder.encode(rawName, "UTF-8")
        val json = getRaw(url) ?: return null
        val items = json.optJSONArray("items") ?: return null
        if (items.length() == 0) return null
        val first = items.optJSONObject(0) ?: return null
        return parseProvider(first)
    }

    private suspend fun get(path: String): JSONObject? =
        getRaw(baseUrl.trimEnd('/') + path)

    private suspend fun getRaw(url: String): JSONObject? {
        if (url.toHttpUrlOrNull() == null) return null
        return withContext(Dispatchers.IO) {
            withTimeoutOrNull(5_000L) {
                try {
                    val request = Request.Builder().url(url).get().build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@use null
                        val text = response.body?.string()
                        if (text.isNullOrBlank()) return@use null
                        try {
                            JSONObject(text)
                        } catch (t: Throwable) {
                            Timber.w(t, "ProviderDirectoryRepository: JSON invalido")
                            null
                        }
                    }
                } catch (t: Throwable) {
                    Timber.w(t, "ProviderDirectoryRepository: falha de rede — ${t::class.simpleName}")
                    null
                }
            }
        }
    }

    private fun parseProvider(o: JSONObject): RemoteProviderInfo? {
        val id = o.optString("id", "")
        if (id.isEmpty()) return null
        val logo = o.optJSONObject("logo")
        val support = o.optJSONObject("support")
        return RemoteProviderInfo(
            providerId = id,
            displayName = o.optString("displayName", id),
            logoUrl = logo?.optStringOrNull("url"),
            sacPhone = support?.optStringOrNull("sacPhone"),
            technicalSupportPhone = support?.optStringOrNull("technicalSupportPhone"),
            whatsappUrl = support?.optStringOrNull("whatsappUrl"),
            websiteUrl = support?.optStringOrNull("websiteUrl"),
            customerAreaUrl = support?.optStringOrNull("customerAreaUrl"),
            ombudsmanPhone = support?.optStringOrNull("ombudsmanPhone"),
        )
    }
}

private fun JSONObject.optStringOrNull(name: String): String? {
    if (!has(name) || isNull(name)) return null
    val s = optString(name, "")
    return s.ifBlank { null }
}
