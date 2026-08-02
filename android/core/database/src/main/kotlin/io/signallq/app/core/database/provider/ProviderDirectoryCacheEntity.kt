package io.signallq.app.core.database.provider

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cache local de um resultado individual do diretorio remoto de provedores (worker
 * `signallq-diagnostic`, `GET /providers/...`) -- GH#1462, parte de #951.
 *
 * Tabela: provider_directory_cache
 *
 * Nao e uma copia da tabela `providers` do worker -- so guarda, por [cacheKey], a
 * ultima resposta valida ja recebida (findById/searchByName/futuro findByAsn, cada
 * metodo com seu proprio formato de chave em `ProviderDirectoryRepository`,
 * `:featureDiagnostico`). Este modulo (`:coreDatabase`) nao conhece
 * `RemoteProviderInfo` nem qualquer tipo de `:featureDiagnostico` -- a conversao
 * fica inteiramente do lado do consumidor, preservando a direcao de dependencia
 * (`:featureDiagnostico` -> `:coreDatabase`, nunca o contrario).
 *
 * @param cacheKey chave logica da consulta que originou esta entrada (ex.:
 *   `"id:<providerId>"`, `"search:<nome normalizado>"`).
 * @param cacheVersion / @param cacheExpiresAtMs vem literalmente do contrato do worker
 *   (`ProviderRecord.cacheVersion`/`cacheExpiresAt`, `provider-directory.ts`) -- nunca
 *   inventados aqui. `cacheExpiresAtMs` nulo quando o worker nao informou expiracao;
 *   nesse caso a entrada nunca e tratada como expirada.
 * @param fetchedAtEpochMs quando esta linha foi gravada localmente (debug/telemetria,
 *   nao usado para decidir expiracao -- isso e `cacheExpiresAtMs`).
 */
@Entity(tableName = "provider_directory_cache")
data class ProviderDirectoryCacheEntity(
    @PrimaryKey
    val cacheKey: String,
    val providerId: String,
    val displayName: String,
    val logoUrl: String?,
    val sacPhone: String?,
    val technicalSupportPhone: String?,
    val whatsappUrl: String?,
    val websiteUrl: String?,
    val customerAreaUrl: String?,
    val ombudsmanPhone: String?,
    val status: String?,
    val cacheVersion: Int?,
    val cacheExpiresAtMs: Long?,
    val fetchedAtEpochMs: Long,
)
