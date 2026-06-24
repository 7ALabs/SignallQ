package io.veloo.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicaoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(medicao: MedicaoEntity)

    @Query("SELECT * FROM medicao ORDER BY timestampEpochMs DESC")
    fun observarTodas(): Flow<List<MedicaoEntity>>

    @Query("SELECT * FROM medicao ORDER BY timestampEpochMs DESC LIMIT :limite")
    fun observarUltimas(limite: Int): Flow<List<MedicaoEntity>>

    @Query("SELECT * FROM medicao WHERE speedtestMode = :modo ORDER BY timestampEpochMs DESC LIMIT :limite")
    fun observarPorModo(
        modo: String,
        limite: Int,
    ): Flow<List<MedicaoEntity>>

    @Query("SELECT * FROM medicao WHERE timestampEpochMs >= :timestampMin ORDER BY timestampEpochMs DESC LIMIT :limite")
    fun observarDesde(
        timestampMin: Long,
        limite: Int,
    ): Flow<List<MedicaoEntity>>

    @Query("SELECT * FROM medicao WHERE contaminado = 1 AND timestampEpochMs >= :timestampMin ORDER BY timestampEpochMs DESC LIMIT :limite")
    fun observarContaminadasDesde(
        timestampMin: Long,
        limite: Int,
    ): Flow<List<MedicaoEntity>>

    @Query("SELECT * FROM medicao WHERE speedtestMode = :modo AND timestampEpochMs >= :timestampMin ORDER BY timestampEpochMs DESC LIMIT :limite")
    fun observarPorModoDesde(
        modo: String,
        timestampMin: Long,
        limite: Int,
    ): Flow<List<MedicaoEntity>>

    @Query(
        "SELECT * FROM medicao " +
            "WHERE timestampEpochMs >= :timestampMin " +
            "AND (:modo IS NULL OR speedtestMode = :modo) " +
            "AND (:apenasContaminado = 0 OR contaminado = 1) " +
            "ORDER BY timestampEpochMs DESC LIMIT :limite",
    )
    fun observarFiltrado(
        timestampMin: Long,
        modo: String?,
        apenasContaminado: Int,
        limite: Int,
    ): Flow<List<MedicaoEntity>>

    @Query("SELECT * FROM medicao WHERE timestampEpochMs >= :timestampMin ORDER BY timestampEpochMs DESC")
    suspend fun buscarDesde(timestampMin: Long): List<MedicaoEntity>

    @Query("SELECT * FROM medicao ORDER BY timestampEpochMs DESC")
    suspend fun buscarTodas(): List<MedicaoEntity>

    @Query("DELETE FROM medicao")
    suspend fun deletarTodos()
}
