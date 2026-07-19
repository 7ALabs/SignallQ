package io.signallq.pro.core.database.walktest

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PontoWalkTestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(entidade: PontoWalkTestEntity)

    @Query("SELECT * FROM ponto_walktest_pro WHERE ambienteId = :ambienteId ORDER BY criadoEmEpochMs DESC")
    fun observarPorAmbiente(ambienteId: String): Flow<List<PontoWalkTestEntity>>
}
