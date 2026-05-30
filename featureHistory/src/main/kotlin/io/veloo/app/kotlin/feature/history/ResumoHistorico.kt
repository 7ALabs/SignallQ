package io.veloo.app.feature.history

data class ResumoHistorico(
    val totalMedicoes: Int,
    val ultimaMedicaoEpochMs: Long?,
    val ultimoDownloadMbps: Double?,
    val ultimoUploadMbps: Double?,
    val ultimaLatenciaMs: Double?,
    val ultimoJitterMs: Double?,
    val ultimaPerdaPercentual: Double?,
    val ultimoBufferbloatMs: Double?,
    val mediaDownloadMbps5: Double?,
    val mediaUploadMbps5: Double?,
    val mediaLatenciaMs5: Double?,
    val quantidadeContaminadas5: Int,
    val ultimasMedicoes: List<ItemHistoricoRecente>,
)

data class ItemHistoricoRecente(
    val timestampEpochMs: Long,
    val speedtestMode: String?,
    val downloadMbps: Double?,
    val uploadMbps: Double?,
    val latenciaMs: Double?,
    val contaminado: Boolean,
)
