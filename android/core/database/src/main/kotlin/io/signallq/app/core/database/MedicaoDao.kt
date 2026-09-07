package io.signallq.app.core.database

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

    /**
     * Base explícita para o veredito do Modo gamer. Não cai na última medição global: exige a
     * mesma identidade de rede, janela temporal indicada pelo chamador e integridade completa.
     * Linhas antigas sem `networkId` ficam naturalmente fora da consulta.
     */
    @Query(
        "SELECT * FROM medicao " +
            "WHERE networkId = :networkId " +
            "AND status = 'completed' " +
            "AND timestampEpochMs >= :timestampMin " +
            "ORDER BY timestampEpochMs DESC LIMIT 1",
    )
    suspend fun buscarUltimaCompletaNaRedeDesde(
        networkId: String,
        timestampMin: Long,
    ): MedicaoEntity?

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

    @Query("DELETE FROM medicao WHERE id = :id")
    suspend fun deletarPorId(id: String)

    @Query(
        "UPDATE medicao SET diagnosticoTexto = :texto, diagnosticoOrigem = :origem, " +
            "diagnosticoProblemas = :problemas WHERE id = :id",
    )
    suspend fun atualizarDiagnostico(
        id: String,
        texto: String?,
        origem: String?,
        problemas: String?,
    )

    @Query("UPDATE medicao SET score = :score WHERE id = :id")
    suspend fun atualizarScore(
        id: String,
        score: Double,
    )

    /**
     * GH#1707 (Task 2.0.09e) — última medição concluída na MESMA rede ([networkId]), anterior a
     * [antesDoTimestamp] e diferente de [excluirId] (a própria medição de origem da comparação).
     * Usada pra decidir se um reteste tem par comparável (spec §8.8): condições equivalentes só
     * quando o [networkId] bate — nunca compara redes diferentes com aviso, declara o limite.
     *
     * `networkId IS NOT NULL` exclui tanto medições sem sinal de rede estável (Ethernet) quanto
     * linhas persistidas antes da migração 18→19 — as duas hoje têm `networkId = NULL`, e nenhuma
     * das duas é comparável por definição.
     */
    @Query(
        "SELECT * FROM medicao " +
            "WHERE networkId IS NOT NULL AND networkId = :networkId " +
            "AND id != :excluirId AND timestampEpochMs < :antesDoTimestamp " +
            "AND status = 'completed' " +
            "ORDER BY timestampEpochMs DESC LIMIT 1",
    )
    suspend fun buscarUltimaComparavelNaRede(
        networkId: String,
        excluirId: String,
        antesDoTimestamp: Long,
    ): MedicaoEntity?
}
