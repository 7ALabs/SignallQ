package io.linka.app.kotlin.speedtest

import io.linka.app.kotlin.core.database.MedicaoDao
import io.linka.app.kotlin.core.database.MedicaoEntity
import io.linka.app.kotlin.core.network.MonitorRede
import io.linka.app.kotlin.core.telephony.MonitorTelephony
import io.linka.app.kotlin.feature.speedtest.EstadoExecucaoSpeedtest
import io.linka.app.kotlin.feature.speedtest.ExecutorSpeedtest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Responsável único por persistir resultados do speedtest no Room.
 *
 * Observa [ExecutorSpeedtest.snapshotFlow], detecta estado concluído, monta a
 * [MedicaoEntity] completa (com operadoraMovel via [MonitorTelephony]) e salva.
 *
 * Guard interno [ultimoResultadoPersistidoEpochMs] garante que o mesmo resultado
 * não seja salvo mais de uma vez, mesmo que o snapshotFlow emita múltiplas vezes
 * com o mesmo timestamp.
 *
 * Centraliza a persistência que antes estava duplicada em MainViewModel e
 * ChatDiagnosticoIaViewModel (issues #184 e #185).
 */
@Singleton
class SpeedtestPersistenceCoordinator
    @Inject
    constructor(
        private val executorSpeedtest: ExecutorSpeedtest,
        private val medicaoDao: MedicaoDao,
        private val monitorTelephony: MonitorTelephony,
        private val monitorRede: MonitorRede,
        private val applicationScope: CoroutineScope,
    ) {
        private var ultimoResultadoPersistidoEpochMs: Long? = null

        /**
         * Inicia a observação do snapshotFlow do speedtest.
         * Deve ser chamado uma única vez na inicialização do app (Application ou AppModule).
         * Idempotente — chamadas adicionais são ignoradas pois o CoroutineScope é o applicationScope.
         */
        fun iniciar() {
            applicationScope.launch {
                executorSpeedtest.snapshotFlow.collect { snapshot ->
                    if (snapshot.estado != EstadoExecucaoSpeedtest.concluido) return@collect
                    val resultado = snapshot.resultado ?: return@collect
                    if (ultimoResultadoPersistidoEpochMs == resultado.timestampEpochMs) return@collect

                    ultimoResultadoPersistidoEpochMs = resultado.timestampEpochMs

                    try {
                        medicaoDao.salvar(
                            MedicaoEntity(
                                id = UUID.randomUUID().toString(),
                                timestampEpochMs = resultado.timestampEpochMs,
                                connectionType = monitorRede.snapshotFlow.value.estadoConexao.name,
                                connectionTypeStart = resultado.connectionTypeStart,
                                connectionTypeEnd = resultado.connectionTypeEnd,
                                contaminado = resultado.contaminado,
                                speedtestMode = resultado.modo.name,
                                specVersion = resultado.specVersion,
                                downloadMbps = resultado.downloadMbps,
                                uploadMbps = resultado.uploadMbps,
                                latencyMs = resultado.latenciaMs,
                                jitterMs = resultado.jitterMs,
                                perdaPercentual = resultado.perdaPercentual,
                                bufferbloatMs = resultado.bufferbloatMs,
                                packetLossSource = resultado.packetLossSource,
                                vereditoStreaming = resultado.diagnosticoQualidade.vereditoStreaming.name,
                                vereditoGamer = resultado.diagnosticoQualidade.vereditoGamer.name,
                                vereditoVideoChamada = resultado.diagnosticoQualidade.vereditoVideoChamada.name,
                                gargaloPrimario = resultado.diagnosticoQualidade.gargaloPrimario.name,
                                operadoraMovel = monitorTelephony.snapshotFlow.value?.operadora,
                            ),
                        )
                        Timber.d("SpeedtestPersistenceCoordinator: salvo ts=${resultado.timestampEpochMs}")
                    } catch (e: Exception) {
                        Timber.e(e, "SpeedtestPersistenceCoordinator: falha ao salvar medicao")
                    }
                }
            }
        }
    }
