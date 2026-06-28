package io.signallq.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ApelidoDispositivoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(entidade: ApelidoDispositivoEntity)

    @Query("SELECT * FROM apelido_dispositivo")
    fun observarTodos(): Flow<List<ApelidoDispositivoEntity>>

    @Query("SELECT * FROM apelido_dispositivo")
    suspend fun buscarTodos(): List<ApelidoDispositivoEntity>

    /**
     * Registra um MAC sem apelido para que nao seja notificado novamente.
     * Usa IGNORE — se o MAC ja existir (ja tem apelido), nao sobrescreve.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserirSilencioso(entidade: ApelidoDispositivoEntity)
}
