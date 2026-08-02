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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
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
    // GH#1464 (parte de #951) — status de curadoria do registro no worker
    // (`PENDING_REVIEW`/`DRAFT`/`AUTO_DISCOVERED`/`VERIFIED`/`STALE`/`DISABLED`, vocabulario
    // fixo, definido la — nunca reinterpretado aqui). So `VERIFIED` e considerado dado
    // confirmado pela UI; qualquer outro valor (ou nulo) e tratado como "nao verificado".
    val status: String?,
    // GH#1462 (parte de #951) — versao/expiracao do cache local, vindas literalmente do
    // contrato do worker (`ProviderRecord.cacheVersion`/`cacheExpiresAt`,
    // `provider-directory.ts`). `cacheExpiresAtMs` nulo quando o worker nao informou
    // `cacheExpiresAt` ou o valor nao e um instante ISO-8601 parseavel — nesse caso a
    // entrada de cache correspondente nunca e tratada como expirada (ver
    // [CachedProviderEntry.isExpired]).
    val cacheVersion: Int? = null,
    val cacheExpiresAtMs: Long? = null,
)

/**
 * Client do diretorio remoto de provedores (rotas `GET /providers/...`) — GH#965.
 *
 * Mesma estrategia de timeout curto + fallback silencioso de
 * [RemoteDiagnosticRepository] (ver kdoc la): qualquer falha (sem rede,
 * timeout, 404, JSON invalido) devolve `null` em vez de lancar excecao. O
 * caller (`:app`, resolver de identidade de operadora) decide o fallback
 * final — este repository NUNCA decide UI/copy de fallback sozinho.
 *
 * ## Cache local (GH#1462, parte de #951)
 * Cada metodo publico grava, no sucesso de rede, o resultado em [cache] (por chave de
 * consulta — [keyForId]/[keyForSearchByName]); em falha de rede (sem rede, timeout,
 * 404, JSON invalido — [getRaw] devolve `null` para todos esses casos), consulta o
 * cache ANTES de devolver `null` pro caller. Uma entrada de cache expirada ainda e
 * devolvida como "ultimo dado valido conhecido" — so a chamada de rede com sucesso
 * sobrescreve o cache, nunca a expiracao sozinha o invalida (criterio de aceite de
 * #1462). `searchByName` com resultado vazio (`items: []`) e uma resposta valida e
 * autoritativa do worker — NAO cai pro cache, so falha de rede cai.
 */
class ProviderDirectoryRepository(
    private val baseUrl: String,
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build(),
    private val cache: ProviderDirectoryCache = NoOpProviderDirectoryCache,
) {

    /** Busca por id exato (quando ja se sabe o `providerId`, ex.: vindo de deteccao previa). */
    suspend fun findById(providerId: String): RemoteProviderInfo? {
        val key = keyForId(providerId)
        val fresh = get("/providers/${URLEncoder.encode(providerId, "UTF-8")}")?.let(::parseProvider)
        if (fresh != null) {
            cache.put(key, fresh)
            return fresh
        }
        return fromCache(key)
    }

    /**
     * Busca por nome bruto de ISP (ex.: `SnapshotRede`/`TelephonyManager`) — o
     * mesmo tipo de entrada que [io.signallq.app.ui.BancoOperadoras.resolver]
     * ja consome localmente. Usa `/providers/search`, pega o primeiro
     * resultado (ordenado por relevancia no worker). `null` quando nao ha
     * nenhum match ou a chamada falha.
     */
    suspend fun searchByName(rawName: String): RemoteProviderInfo? {
        if (rawName.isBlank()) return null
        val key = keyForSearchByName(rawName)
        val url = baseUrl.trimEnd('/') + "/providers/search?q=" + URLEncoder.encode(rawName, "UTF-8")
        val json = getRaw(url) ?: return fromCache(key)
        val items = json.optJSONArray("items") ?: return null
        if (items.length() == 0) return null
        val first = items.optJSONObject(0) ?: return null
        val fresh = parseProvider(first) ?: return null
        cache.put(key, fresh)
        return fresh
    }

    /** Ultimo dado valido conhecido para [key] — usado quando a chamada de rede falha
     *  (sem rede, timeout, 404, JSON invalido). `null` quando nunca houve cache. Uma
     *  entrada expirada ainda e devolvida (ver kdoc de classe). */
    private suspend fun fromCache(key: String): RemoteProviderInfo? {
        val cached = cache.get(key) ?: return null
        if (cached.isExpired()) {
            Timber.i("ProviderDirectoryRepository: falha de rede, usando cache expirado para $key")
        } else {
            Timber.i("ProviderDirectoryRepository: falha de rede, usando cache valido para $key")
        }
        return cached.info
    }

    private fun keyForId(providerId: String): String = "id:$providerId"

    private fun keyForSearchByName(rawName: String): String = "search:${rawName.trim().lowercase()}"

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
            status = o.optStringOrNull("status"),
            // GH#1462 (parte de #951) — `cacheVersion` e um inteiro simples; ausencia/valor
            // invalido vira `null` (nunca inventado). `cacheExpiresAt` e ISO-8601 UTC
            // (`ProviderRecord.cacheExpiresAt`, worker) — parseado por [parseIsoInstantMs];
            // formato inesperado tambem vira `null` (entrada de cache nunca expira sozinha).
            cacheVersion = if (o.has("cacheVersion") && !o.isNull("cacheVersion")) o.optInt("cacheVersion") else null,
            cacheExpiresAtMs = o.optStringOrNull("cacheExpiresAt")?.let(::parseIsoInstantMs),
        )
    }
}

private fun JSONObject.optStringOrNull(name: String): String? {
    if (!has(name) || isNull(name)) return null
    val s = optString(name, "")
    return s.ifBlank { null }
}

/**
 * Parseia um instante ISO-8601 UTC no formato exato devolvido pelo worker
 * `signallq-diagnostic` (`new Date().toISOString()`, ex.: `"2026-07-21T00:00:00.000Z"`)
 * para epoch millis. `null` em qualquer formato inesperado — nunca lanca excecao.
 *
 * Nao usa `java.time.Instant` (API 26+) — este modulo nao tem core library
 * desugaring habilitado e `minSdk` do app e 24.
 */
private fun parseIsoInstantMs(iso: String): Long? =
    try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(iso)
            ?.time
    } catch (t: Throwable) {
        Timber.w(t, "ProviderDirectoryRepository: cacheExpiresAt em formato inesperado: $iso")
        null
    }
