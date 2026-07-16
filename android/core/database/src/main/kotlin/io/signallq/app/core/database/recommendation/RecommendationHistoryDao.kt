package io.signallq.app.core.database.recommendation

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecommendationHistoryDao {

    /** Registra a exibicao de uma recomendacao. REPLACE por id evita duplicar a mesma decisao. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registrarExibicao(entrada: RecommendationHistoryEntity)

    /**
     * Historico recente, suficiente para o `RecommendationEngine` aplicar cooldown/
     * maxPerDay/maxPerWeek do catalogo atual (o maior hoje e 7 dias).
     */
    @Query("SELECT * FROM recommendation_history WHERE shownAtEpochMs >= :desdeEpochMs ORDER BY shownAtEpochMs DESC")
    suspend fun buscarRecente(desdeEpochMs: Long): List<RecommendationHistoryEntity>

    @Query(
        "UPDATE recommendation_history SET feedback = :feedback, feedbackAtEpochMs = :feedbackAtEpochMs " +
            "WHERE id = :id",
    )
    suspend fun atualizarFeedback(id: String, feedback: String, feedbackAtEpochMs: Long)

    /** Feedback (util/nao util/ocultar) registrado desde o checkpoint -- usado pelo sync
     *  retroativo para o signallq-admin-worker (design-tobe-alinhamento, tela 1a). Só
     *  entradas com feedback de fato dado (`feedbackAtEpochMs` preenchido); exibições sem
     *  feedback não sincronizam (nada de novo pro admin analisar). */
    @Query(
        "SELECT * FROM recommendation_history WHERE feedbackAtEpochMs IS NOT NULL " +
            "AND feedbackAtEpochMs >= :desdeEpochMs ORDER BY feedbackAtEpochMs ASC",
    )
    suspend fun buscarComFeedbackDesde(desdeEpochMs: Long): List<RecommendationHistoryEntity>
}
