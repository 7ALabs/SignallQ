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
    /** Metadados publicados pelo worker (`ProviderRecord.cacheVersion`/`cacheExpiresAt`,
     *  `provider-directory.ts`) -- guardados so como informacao (telemetria/debug do
     *  cache local, GH#1462). A decisao de expiracao do cache local em disco usa TTL
     *  proprio (ver [ProviderDirectoryRepository]), nao estes dois campos. */
    val cacheVersion: Int? = null,
    val cacheExpiresAt: String? = null,
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
 * ## Cache local por consulta (GH#1462)
 * [cacheStore] guarda o ultimo resultado valido de cada consulta individual
 * (`findById`/`searchByName`), nunca a base inteira (regra explicita do corpo
 * de #951). Ordem em [findById]/[searchByName]:
 * 1. Tenta a rede. Sucesso -> grava no cache (chave da consulta) e retorna.
 * 2. Falha de rede -> tenta o cache local daquela mesma chave. Dentro do TTL
 *    ([cacheTtlMs], default [DEFAULT_CACHE_TTL_MS]) -> retorna o cache como
 *    "ultimo dado valido conhecido". Sem cache ou cache alem do TTL -> `null`,
 *    igual ao comportamento anterior (o caller cai pro fallback final).
 *
 * Download incompleto/invalido nunca substitui um cache valido existente —
 * so gravamos quando [parseProvider] produziu um [RemoteProviderInfo] completo
 * (mesmo principio de [FileRulesetCacheStore]/GH#1450).
 */
class ProviderDirectoryRepository(
    private val baseUrl: String,
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .writeTimeout(3, TimeUnit.SECONDS)
            .build(),
    private val cacheStore: ProviderDirectoryCacheStore = NoOpProviderDirectoryCacheStore,
    private val cacheTtlMs: Long = DEFAULT_CACHE_TTL_MS,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {

    /** Busca por id exato (quando ja se sabe o `providerId`, ex.: vindo de deteccao previa). */
    suspend fun findById(providerId: String): RemoteProviderInfo? {
        val key = "id:$providerId"
        val remote = get("/providers/${URLEncoder.encode(providerId, "UTF-8")}")?.let(::parseProvider)
        return remote?.also { salvarCache(key, it) } ?: buscarCache(key)
    }

    /**
     * Busca por nome bruto de ISP (ex.: `SnapshotRede`/`TelephonyManager`) — o
     * mesmo tipo de entrada que [io.signallq.app.ui.BancoOperadoras.resolver]
     * ja consome localmente. Usa `/providers/search`, pega o primeiro
     * resultado (ordenado por relevancia no worker). `null` quando nao ha
     * nenhum match ou a chamada falha (apos tentar o cache local — ver kdoc
     * da classe).
     */
    suspend fun searchByName(rawName: String): RemoteProviderInfo? {
        if (rawName.isBlank()) return null
        val key = "search:${rawName.trim().lowercase()}"
        val url = baseUrl.trimEnd('/') + "/providers/search?q=" + URLEncoder.encode(rawName, "UTF-8")
        val remote = getRaw(url)?.let { json ->
            val items = json.optJSONArray("items")
            val first = if (items != null && items.length() > 0) items.optJSONObject(0) else null
            first?.let(::parseProvider)
        }
        return remote?.also { salvarCache(key, it) } ?: buscarCache(key)
    }

    private fun salvarCache(key: String, info: RemoteProviderInfo) {
        try {
            val now = nowProvider()
            cacheStore.save(
                key = key,
                entry = CachedProviderEntry(info = info, syncedAtMs = now, expiresAtMs = now + cacheTtlMs),
            )
        } catch (t: Throwable) {
            Timber.w(t, "ProviderDirectoryRepository: falha ao persistir cache local para '$key'")
        }
    }

    /** `null` quando nao ha cache para [key] ou o cache existente esta alem do TTL — nesse
     *  caso o comportamento e igual a nao ter cache nenhum (cai pro fallback do caller). */
    private fun buscarCache(key: String): RemoteProviderInfo? {
        val cached = try {
            cacheStore.load(key)
        } catch (t: Throwable) {
            Timber.w(t, "ProviderDirectoryRepository: falha ao ler cache local para '$key'")
            null
        } ?: return null

        if (!cached.isValidAt(nowProvider())) {
            Timber.i("ProviderDirectoryRepository: cache de '$key' alem do TTL, tratando como inexistente")
            return null
        }

        Timber.i("ProviderDirectoryRepository: usando cache local de '$key' (sincronizado em ${cached.syncedAtMs})")
        return cached.info
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
            cacheVersion = if (o.has("cacheVersion") && !o.isNull("cacheVersion")) o.optInt("cacheVersion") else null,
            cacheExpiresAt = o.optStringOrNull("cacheExpiresAt"),
        )
    }

    private companion object {
        /** TTL do cache local por consulta (GH#1462) -- sugestao inicial de produto
         *  (24-48h), ajustavel via [cacheTtlMs] no construtor sem mudar o contrato. */
        const val DEFAULT_CACHE_TTL_MS = 48 * 60 * 60 * 1000L
    }
}

private fun JSONObject.optStringOrNull(name: String): String? {
    if (!has(name) || isNull(name)) return null
    val s = optString(name, "")
    return s.ifBlank { null }
}
