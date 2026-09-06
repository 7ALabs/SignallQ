package io.signallq.app.feature.history

import io.signallq.app.core.database.MedicaoDao
import io.signallq.app.core.database.MedicaoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object FeatureHistoryModulo {
    fun criarObservadorHistorico(medicaoDao: MedicaoDao): ObservadorHistorico = ObservadorHistoricoRoom(medicaoDao)

    fun observarUltimas(
        medicaoDao: MedicaoDao,
        limite: Int = 10,
    ): Flow<List<ItemHistoricoRecente>> = medicaoDao.observarUltimas(limite).map { itens -> itens.map { it.toItemHistoricoRecente() } }

    fun observarPorModo(
        medicaoDao: MedicaoDao,
        modo: String,
        limite: Int = 10,
    ): Flow<List<ItemHistoricoRecente>> = medicaoDao.observarPorModo(modo, limite).map { itens -> itens.map { it.toItemHistoricoRecente() } }

    fun observarDesde(
        medicaoDao: MedicaoDao,
        timestampMin: Long,
        limite: Int = 50,
    ): Flow<List<ItemHistoricoRecente>> = medicaoDao.observarDesde(timestampMin, limite).map { itens -> itens.map { it.toItemHistoricoRecente() } }

    fun observarContaminadasDesde(
        medicaoDao: MedicaoDao,
        timestampMin: Long,
        limite: Int = 50,
    ): Flow<List<ItemHistoricoRecente>> = medicaoDao.observarContaminadasDesde(timestampMin, limite).map { itens -> itens.map { it.toItemHistoricoRecente() } }

    fun observarPorModoDesde(
        medicaoDao: MedicaoDao,
        modo: String,
        timestampMin: Long,
        limite: Int = 50,
    ): Flow<List<ItemHistoricoRecente>> = medicaoDao.observarPorModoDesde(modo, timestampMin, limite).map { itens -> itens.map { it.toItemHistoricoRecente() } }

    fun observarFiltrado(
        medicaoDao: MedicaoDao,
        timestampMin: Long,
        modo: String?,
        apenasContaminado: Boolean,
        limite: Int = 50,
    ): Flow<List<ItemHistoricoRecente>> {
        val flagContaminado = if (apenasContaminado) 1 else 0
        return medicaoDao
            .observarFiltrado(
                timestampMin = timestampMin,
                modo = modo,
                apenasContaminado = flagContaminado,
                limite = limite,
            ).map { itens -> itens.map { it.toItemHistoricoRecente() } }
    }

    private fun MedicaoEntity.toItemHistoricoRecente(): ItemHistoricoRecente =
        ItemHistoricoRecente(
            timestampEpochMs = timestampEpochMs,
            speedtestMode = speedtestMode,
            downloadMbps = downloadMbps,
            uploadMbps = uploadMbps,
            latenciaMs = latencyMs,
            contaminado = contaminado,
        )
}
