package io.signallq.app.feature.diagnostico.remote

import io.signallq.app.core.database.provider.ProviderDirectoryCacheDao
import io.signallq.app.core.database.provider.ProviderDirectoryCacheEntity
import timber.log.Timber

/**
 * Ultimo [RemoteProviderInfo] valido conhecido para uma chave de consulta -- cache local
 * do nivel 2 (diretorio remoto) da cadeia de resolucao de operadora (GH#1462, parte de
 * #951). [isExpired] usa [RemoteProviderInfo.cacheExpiresAtMs] (vindo literalmente do
 * worker); uma entrada expirada continua sendo devolvida por [ProviderDirectoryCache.get]
 * -- e o proprio [ProviderDirectoryRepository] quem decide usa-la como "ultimo dado
 * valido" quando uma consulta nova falha (criterio de aceite de #1462).
 */
data class CachedProviderEntry(
    val info: RemoteProviderInfo,
) {
    fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean {
        val expiresAt = info.cacheExpiresAtMs ?: return false
        return nowMs >= expiresAt
    }
}

/**
 * Cache local por resultado individual do diretorio remoto de provedores -- GH#1462,
 * parte de #951. Nunca um snapshot da tabela inteira (regra explicita de #951: "nao
 * baixar a base inteira") -- so guarda, por [key], a ultima resposta valida de UMA
 * consulta (`findById`/`searchByName`/futuro `findByAsn`, GH#1463).
 *
 * [key] e uma string opaca para este contrato -- [ProviderDirectoryRepository] decide o
 * formato por metodo de consulta (ver `keyForId`/`keyForSearchByName` la). Desenhado
 * deliberadamente generico o bastante para GH#1463 (`findByAsn`) reusar a mesma
 * implementacao sem depender do fluxo de busca por nome (decisao de escopo de #1462).
 */
interface ProviderDirectoryCache {
    /** `null` quando nunca houve cache para [key]. Pode devolver entrada expirada --
     *  ver kdoc de [CachedProviderEntry]. Nunca lanca excecao. */
    suspend fun get(key: String): CachedProviderEntry?

    /** Sobrescreve a entrada existente para [key], se houver. Nunca lanca excecao --
     *  falha de escrita e logada e ignorada (cache e otimizacao, nao dado critico). */
    suspend fun put(
        key: String,
        info: RemoteProviderInfo,
    )
}

/** Usado quando nao ha [ProviderDirectoryCache] real configurado (ex.: construcao de
 *  teste sem Hilt/Room). Degrada para o comportamento anterior a #1462 -- falha de rede
 *  cai direto no fallback generico, sem nivel intermediario de cache. */
object NoOpProviderDirectoryCache : ProviderDirectoryCache {
    override suspend fun get(key: String): CachedProviderEntry? = null

    override suspend fun put(
        key: String,
        info: RemoteProviderInfo,
    ) {}
}

/** Implementacao real via Room ([ProviderDirectoryCacheDao], `:coreDatabase`). Falha de
 *  leitura/escrita no banco nunca propaga -- so loga e devolve `null`/ignora, mesma
 *  postura de resiliencia do restante deste pacote (ver kdoc de
 *  [ProviderDirectoryRepository]). */
class RoomProviderDirectoryCache(
    private val dao: ProviderDirectoryCacheDao,
) : ProviderDirectoryCache {
    override suspend fun get(key: String): CachedProviderEntry? =
        try {
            dao.buscarPorChave(key)?.let(::toCachedEntry)
        } catch (t: Throwable) {
            Timber.w(t, "RoomProviderDirectoryCache: falha ao ler cache para $key")
            null
        }

    override suspend fun put(
        key: String,
        info: RemoteProviderInfo,
    ) {
        try {
            dao.salvar(toEntity(key, info))
        } catch (t: Throwable) {
            Timber.w(t, "RoomProviderDirectoryCache: falha ao gravar cache para $key")
        }
    }

    private fun toCachedEntry(entity: ProviderDirectoryCacheEntity): CachedProviderEntry =
        CachedProviderEntry(
            RemoteProviderInfo(
                providerId = entity.providerId,
                displayName = entity.displayName,
                logoUrl = entity.logoUrl,
                sacPhone = entity.sacPhone,
                technicalSupportPhone = entity.technicalSupportPhone,
                whatsappUrl = entity.whatsappUrl,
                websiteUrl = entity.websiteUrl,
                customerAreaUrl = entity.customerAreaUrl,
                ombudsmanPhone = entity.ombudsmanPhone,
                status = entity.status,
                cacheVersion = entity.cacheVersion,
                cacheExpiresAtMs = entity.cacheExpiresAtMs,
            ),
        )

    private fun toEntity(
        key: String,
        info: RemoteProviderInfo,
    ): ProviderDirectoryCacheEntity =
        ProviderDirectoryCacheEntity(
            cacheKey = key,
            providerId = info.providerId,
            displayName = info.displayName,
            logoUrl = info.logoUrl,
            sacPhone = info.sacPhone,
            technicalSupportPhone = info.technicalSupportPhone,
            whatsappUrl = info.whatsappUrl,
            websiteUrl = info.websiteUrl,
            customerAreaUrl = info.customerAreaUrl,
            ombudsmanPhone = info.ombudsmanPhone,
            status = info.status,
            cacheVersion = info.cacheVersion,
            cacheExpiresAtMs = info.cacheExpiresAtMs,
            fetchedAtEpochMs = System.currentTimeMillis(),
        )
}
