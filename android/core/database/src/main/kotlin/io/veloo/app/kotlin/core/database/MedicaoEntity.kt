package io.veloo.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicao")
data class MedicaoEntity(
    @PrimaryKey
    val id: String,
    val timestampEpochMs: Long,
    val connectionType: String,
    val connectionTypeStart: String?,
    val connectionTypeEnd: String?,
    val contaminado: Boolean,
    val speedtestMode: String?,
    val specVersion: String?,
    val downloadMbps: Double?,
    val uploadMbps: Double?,
    val latencyMs: Double?,
    val jitterMs: Double?,
    val perdaPercentual: Double?,
    val bufferbloatMs: Double?,
    val packetLossSource: String?,
    val vereditoStreaming: String?,
    val vereditoGamer: String?,
    val vereditoVideoChamada: String?,
    val gargaloPrimario: String?,
    val fonte: String? = null,
    val operadoraMovel: String? = null,
    val diagnosticoTexto: String? = null,
    val diagnosticoOrigem: String? = null,
    val diagnosticoProblemas: String? = null,
    /** Score 0–100 calculado pelo engine local após o diagnóstico. Null enquanto diagnóstico não foi executado. */
    val score: Double? = null,
    /** Status da medição: "completed", "failed", "partial" ou "timeout". */
    val status: String = "completed",
)
