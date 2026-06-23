package io.veloo.app.monitoramento

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.veloo.app.core.database.MedicaoDao
import io.veloo.app.core.database.chat.ChatSessionDao
import io.veloo.app.core.datastore.PreferenciasAppRepository
import io.veloo.app.feature.diagnostico.ingest.AdminIngestRepository
import io.veloo.app.feature.diagnostico.ingest.toIngestPayload

/**
 * Worker de sync retroativo de medicoes e sessoes de IA para o signallq-admin-worker.
 *
 * Estrategia:
 *  - Le checkpoint (epoch ms) do DataStore para cada tipo.
 *  - Consulta Room por registros mais novos que o checkpoint.
 *  - Envia em batches de 50 — atualiza checkpoint apos cada batch bem-sucedido.
 *  - Filtra medicoes com contaminado == true (nunca envia ao admin).
 *  - Retry automatico ate 3 tentativas com backoff exponencial (configurado no WorkRequest).
 *
 * Dependencias injetadas via HiltWorkerFactory — URL e key nao precisam mais ser
 * passados via inputData.
 */
@HiltWorker
internal class AdminSyncWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted params: WorkerParameters,
        private val preferenciasAppRepository: PreferenciasAppRepository,
        private val medicaoDao: MedicaoDao,
        private val chatSessionDao: ChatSessionDao,
        private val adminIngestRepository: AdminIngestRepository,
    ) : CoroutineWorker(appContext, params) {
        internal companion object {
            const val TAG = "AdminSyncWorker"
            const val BATCH_SIZE = 50
        }

        override suspend fun doWork(): Result {
            Log.d(TAG, "Iniciando sync retroativo (tentativa ${runAttemptCount + 1})")
            return try {
                syncMedicoes()
                syncChatSessions()
                Log.d(TAG, "Sync retroativo concluido com sucesso")
                Result.success()
            } catch (e: Exception) {
                Log.w(TAG, "Sync retroativo falhou: ${e.message}")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        }

        /**
         * Sincroniza medicoes nao contaminadas desde o ultimo checkpoint.
         * Atualiza o checkpoint apos cada batch para garantir progresso incremental.
         */
        private suspend fun syncMedicoes() {
            val lastEpoch = preferenciasAppRepository.buscarAdminSyncMedicaoLastEpochMs()
            Log.d(TAG, "syncMedicoes: checkpoint=$lastEpoch")

            val pendentes =
                medicaoDao
                    .buscarDesde(lastEpoch)
                    .filter { !it.contaminado }
                    .sortedBy { it.timestampEpochMs }

            if (pendentes.isEmpty()) {
                Log.d(TAG, "syncMedicoes: nenhuma medicao pendente")
                return
            }

            Log.d(TAG, "syncMedicoes: ${pendentes.size} medicoes pendentes")

            pendentes.chunked(BATCH_SIZE).forEach { batch ->
                batch.forEach { medicao ->
                    adminIngestRepository.sendDiagnostic(medicao.toIngestPayload())
                }
                val maxEpoch = batch.maxOf { it.timestampEpochMs }
                preferenciasAppRepository.salvarAdminSyncMedicaoLastEpochMs(maxEpoch)
                Log.d(TAG, "syncMedicoes: batch de ${batch.size} enviado, checkpoint=$maxEpoch")
            }
        }

        /**
         * Sincroniza sessoes de chat concluidas (status=completed, nomeModelo != null)
         * desde o ultimo checkpoint.
         */
        private suspend fun syncChatSessions() {
            val lastEpoch = preferenciasAppRepository.buscarAdminSyncChatLastEpochMs()
            Log.d(TAG, "syncChatSessions: checkpoint=$lastEpoch")

            val pendentes = chatSessionDao.buscarCompletasDesde(lastEpoch)

            if (pendentes.isEmpty()) {
                Log.d(TAG, "syncChatSessions: nenhuma sessao pendente")
                return
            }

            Log.d(TAG, "syncChatSessions: ${pendentes.size} sessoes pendentes")

            pendentes.chunked(BATCH_SIZE).forEach { batch ->
                batch.forEach { sessao ->
                    adminIngestRepository.sendAiUsage(sessao.toIngestPayload())
                }
                val maxEpoch = batch.maxOf { it.criadoEmEpochMs }
                preferenciasAppRepository.salvarAdminSyncChatLastEpochMs(maxEpoch)
                Log.d(TAG, "syncChatSessions: batch de ${batch.size} enviado, checkpoint=$maxEpoch")
            }
        }
    }
