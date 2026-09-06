package io.signallq.app.core.database.connectivity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConnectivityDiagnosisHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registrar(entrada: ConnectivityDiagnosisHistoryEntity)

    @Query("SELECT * FROM connectivity_diagnosis_history ORDER BY startedAtEpochMs DESC LIMIT :limite")
    suspend fun buscarRecentes(limite: Int = 20): List<ConnectivityDiagnosisHistoryEntity>
}
