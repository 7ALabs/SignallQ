package io.signallq.app.core.database.provider

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProviderDirectoryCacheDao {
    @Query("SELECT * FROM provider_directory_cache WHERE cacheKey = :cacheKey LIMIT 1")
    suspend fun buscarPorChave(cacheKey: String): ProviderDirectoryCacheEntity?

    /** REPLACE por cacheKey -- resultado novo da mesma consulta sempre sobrescreve o anterior. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(entidade: ProviderDirectoryCacheEntity)
}
