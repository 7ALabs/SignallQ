package io.veloo.app.feature.history

import io.veloo.app.core.database.MedicaoDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ObservadorHistoricoRoom(
    medicaoDao: MedicaoDao,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ObservadorHistorico {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    override val resumoFlow: StateFlow<ResumoHistorico> =
        medicaoDao
            .observarTodas()
            .map { medicoes ->
                val ultima = medicoes.firstOrNull()
                val janela5 = medicoes.take(5)
                ResumoHistorico(
                    totalMedicoes = medicoes.size,
                    ultimaMedicaoEpochMs = ultima?.timestampEpochMs,
                    ultimoDownloadMbps = ultima?.downloadMbps,
                    ultimoUploadMbps = ultima?.uploadMbps,
                    ultimaLatenciaMs = ultima?.latencyMs,
                    ultimoJitterMs = ultima?.jitterMs,
                    ultimaPerdaPercentual = ultima?.perdaPercentual,
                    ultimoBufferbloatMs = ultima?.bufferbloatMs,
                    mediaDownloadMbps5 = mediaNaoNula(janela5.mapNotNull { it.downloadMbps }),
                    mediaUploadMbps5 = mediaNaoNula(janela5.mapNotNull { it.uploadMbps }),
                    mediaLatenciaMs5 = mediaNaoNula(janela5.mapNotNull { it.latencyMs }),
                    quantidadeContaminadas5 = janela5.count { it.contaminado },
                    ultimasMedicoes =
                        medicoes.take(3).map {
                            ItemHistoricoRecente(
                                timestampEpochMs = it.timestampEpochMs,
                                speedtestMode = it.speedtestMode,
                                downloadMbps = it.downloadMbps,
                                uploadMbps = it.uploadMbps,
                                latenciaMs = it.latencyMs,
                                contaminado = it.contaminado,
                            )
                        },
                )
            }.stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue =
                    ResumoHistorico(
                        totalMedicoes = 0,
                        ultimaMedicaoEpochMs = null,
                        ultimoDownloadMbps = null,
                        ultimoUploadMbps = null,
                        ultimaLatenciaMs = null,
                        ultimoJitterMs = null,
                        ultimaPerdaPercentual = null,
                        ultimoBufferbloatMs = null,
                        mediaDownloadMbps5 = null,
                        mediaUploadMbps5 = null,
                        mediaLatenciaMs5 = null,
                        quantidadeContaminadas5 = 0,
                        ultimasMedicoes = emptyList(),
                    ),
            )

    fun cancel() { scope.cancel() }

    private fun mediaNaoNula(valores: List<Double>): Double? {
        if (valores.isEmpty()) return null
        return valores.average()
    }
}
