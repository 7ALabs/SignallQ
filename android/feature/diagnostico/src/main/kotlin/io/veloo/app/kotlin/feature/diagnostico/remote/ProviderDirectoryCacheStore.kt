package io.signallq.app.feature.diagnostico.remote

import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Ultima resposta valida do diretorio remoto de provedores, persistida por chave de
 * consulta -- nivel 2 (cauda longa) do fallback de identidade de operadora (GH#965,
 * cache adicionado em GH#1462). Nunca uma sincronizacao da tabela `providers` inteira
 * (regra explicita do corpo de #951) -- so o resultado de consultas individuais
 * (`findById`/`searchByName`) realmente feitas pelo app.
 *
 * [cacheVersion]/[cacheExpiresAt] em [RemoteProviderInfo] sao os metadados publicados
 * pelo worker (contrato `ProviderRecord`, ver `provider-directory.ts`) -- guardados aqui
 * so como metadado, nao decidem sozinhos a expiracao local. [expiresAtMs] e quem decide:
 * TTL local (ver `ProviderDirectoryRepository.CACHE_TTL_MS`) contado a partir de
 * [syncedAtMs]. Cache dentro do TTL e usado como fallback em falha de rede; cache alem
 * do TTL e tratado como inexistente (cai pro catalogo local embarcado) -- ver kdoc de
 * [ProviderDirectoryRepository].
 */
data class CachedProviderEntry(
    val info: RemoteProviderInfo,
    val syncedAtMs: Long,
    val expiresAtMs: Long,
) {
    fun isValidAt(nowMs: Long): Boolean = nowMs <= expiresAtMs
}

/**
 * Persistencia do diretorio remoto de provedores por chave de consulta (nivel 2 do
 * fallback de identidade de operadora). Contrato deliberadamente pequeno, mesmo
 * principio de [RulesetCacheStore]: [load] nunca lanca (corrupcao vira `null`, nunca
 * crash), [save] so promove o conteudo novo depois de valida-lo por completo -- download
 * incompleto/invalido nunca substitui o cache valido existente.
 *
 * Sem o nivel `.previous`/rollback de [FileRulesetCacheStore]: dado de contato/logo de
 * provedor de cauda longa e reconstruivel numa proxima consulta bem-sucedida, nao e
 * critico como um ruleset de diagnostico (decisao de escopo de GH#1462).
 */
interface ProviderDirectoryCacheStore {
    fun load(key: String): CachedProviderEntry?
    fun save(key: String, entry: CachedProviderEntry): Boolean
}

/** Usado quando nao ha [ProviderDirectoryCacheStore] real configurado (ex.: construcao de
 *  teste sem Context/Hilt). Degrada para o fallback de 2 niveis anterior -- REMOTE (sob
 *  demanda) -> fallback generico -- nunca quebra por falta de cache. */
object NoOpProviderDirectoryCacheStore : ProviderDirectoryCacheStore {
    override fun load(key: String): CachedProviderEntry? = null
    override fun save(key: String, entry: CachedProviderEntry): Boolean = false
}

/**
 * Implementacao em disco (arquivo privado do app, `filesDir` -- nunca `cacheDir`, mesmo
 * raciocinio de [FileRulesetCacheStore]: este dado e usado como fallback de continuidade
 * quando a rede falha, o SO nao deveria poder limpa-lo sob pressao de storage).
 *
 * Um unico arquivo JSON guarda todas as entradas (mapa `chave -> entrada`), pois o
 * volume esperado e pequeno (provedores de cauda longa realmente consultados nesta
 * instalacao, nunca a base inteira). Escrita atomica: serializa o mapa inteiro (entrada
 * nova/atualizada + entradas ja existentes) para um arquivo `.tmp`, releh e revalida
 * antes de promover via `Files.move(..., REPLACE_EXISTING)` -- nada e sobrescrito antes
 * da validacao completa, entao uma escrita corrompida nunca derruba entradas ja validas
 * de outros provedores.
 */
class FileProviderDirectoryCacheStore(private val cacheDir: File) : ProviderDirectoryCacheStore {

    private val current: File get() = File(cacheDir, CURRENT_FILE_NAME)
    private val tmp: File get() = File(cacheDir, TMP_FILE_NAME)

    override fun load(key: String): CachedProviderEntry? {
        val root = readValidRoot(current) ?: return null
        val obj = root.optJSONObject(key) ?: return null
        return parseEntry(obj)
    }

    override fun save(key: String, entry: CachedProviderEntry): Boolean {
        return try {
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                Timber.w("FileProviderDirectoryCacheStore: nao foi possivel criar diretorio de cache")
                return false
            }

            val root = readValidRoot(current) ?: JSONObject()
            root.put(key, serializeEntry(entry))
            val wrapper = root.toString()

            tmp.writeText(wrapper, StandardCharsets.UTF_8)

            // Releh e revalida o que foi escrito antes de promover -- nunca confia que a
            // escrita em disco terminou intacta so porque nao lancou excecao.
            val writtenBack = readValidRoot(tmp)
            if (writtenBack == null || parseEntry(writtenBack.optJSONObject(key) ?: JSONObject()) == null) {
                Timber.w("FileProviderDirectoryCacheStore: escrita em .tmp falhou na revalidacao, cache atual preservado")
                tmp.delete()
                return false
            }

            Files.move(tmp.toPath(), current.toPath(), StandardCopyOption.REPLACE_EXISTING)
            true
        } catch (t: Throwable) {
            Timber.w(t, "FileProviderDirectoryCacheStore: falha ao persistir diretorio de provedores, cache atual preservado")
            tmp.delete()
            false
        }
    }

    private fun readValidRoot(file: File): JSONObject? {
        if (!file.exists()) return null
        return try {
            JSONObject(file.readText(StandardCharsets.UTF_8))
        } catch (t: Throwable) {
            Timber.w(t, "FileProviderDirectoryCacheStore: falha ao ler/parsear ${file.name}, tratando como corrompido")
            null
        }
    }

    private fun parseEntry(o: JSONObject): CachedProviderEntry? {
        return try {
            val info = RemoteProviderInfo(
                providerId = o.getString("providerId"),
                displayName = o.getString("displayName"),
                logoUrl = o.optStringOrNull("logoUrl"),
                sacPhone = o.optStringOrNull("sacPhone"),
                technicalSupportPhone = o.optStringOrNull("technicalSupportPhone"),
                whatsappUrl = o.optStringOrNull("whatsappUrl"),
                websiteUrl = o.optStringOrNull("websiteUrl"),
                customerAreaUrl = o.optStringOrNull("customerAreaUrl"),
                ombudsmanPhone = o.optStringOrNull("ombudsmanPhone"),
                cacheVersion = if (o.has("cacheVersion") && !o.isNull("cacheVersion")) o.optInt("cacheVersion") else null,
                cacheExpiresAt = o.optStringOrNull("cacheExpiresAt"),
            )
            CachedProviderEntry(
                info = info,
                syncedAtMs = o.getLong("syncedAtMs"),
                expiresAtMs = o.getLong("expiresAtMs"),
            )
        } catch (t: Throwable) {
            Timber.w(t, "FileProviderDirectoryCacheStore: entrada de cache corrompida, ignorando")
            null
        }
    }

    private fun serializeEntry(entry: CachedProviderEntry): JSONObject =
        JSONObject()
            .put("providerId", entry.info.providerId)
            .put("displayName", entry.info.displayName)
            .put("logoUrl", entry.info.logoUrl ?: JSONObject.NULL)
            .put("sacPhone", entry.info.sacPhone ?: JSONObject.NULL)
            .put("technicalSupportPhone", entry.info.technicalSupportPhone ?: JSONObject.NULL)
            .put("whatsappUrl", entry.info.whatsappUrl ?: JSONObject.NULL)
            .put("websiteUrl", entry.info.websiteUrl ?: JSONObject.NULL)
            .put("customerAreaUrl", entry.info.customerAreaUrl ?: JSONObject.NULL)
            .put("ombudsmanPhone", entry.info.ombudsmanPhone ?: JSONObject.NULL)
            .put("cacheVersion", entry.info.cacheVersion ?: JSONObject.NULL)
            .put("cacheExpiresAt", entry.info.cacheExpiresAt ?: JSONObject.NULL)
            .put("syncedAtMs", entry.syncedAtMs)
            .put("expiresAtMs", entry.expiresAtMs)

    private companion object {
        const val CURRENT_FILE_NAME = "provider_directory_cache.json"
        const val TMP_FILE_NAME = "provider_directory_cache.tmp.json"
    }
}

private fun JSONObject.optStringOrNull(name: String): String? {
    if (!has(name) || isNull(name)) return null
    val s = optString(name, "")
    return s.ifBlank { null }
}
