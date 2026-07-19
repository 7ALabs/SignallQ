package io.signallq.pro.core.database.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(entidade: LocalEntity)

    @Query("SELECT * FROM local WHERE clienteId = :clienteId ORDER BY criadoEmEpochMs ASC")
    fun observarPorCliente(clienteId: String): Flow<List<LocalEntity>>

    /** MVP0: cliente tem exatamente um local -- usado para resolver o local implicito da
     *  visita quando so existe um (issue #1166). */
    @Query("SELECT * FROM local WHERE clienteId = :clienteId ORDER BY criadoEmEpochMs ASC LIMIT 1")
    suspend fun buscarPrimeiroPorCliente(clienteId: String): LocalEntity?

    @Query("SELECT * FROM local WHERE id = :id LIMIT 1")
    suspend fun buscarPorId(id: String): LocalEntity?
}
