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
)
