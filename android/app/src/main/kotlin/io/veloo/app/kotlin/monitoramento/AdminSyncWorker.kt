package io.signallq.app.monitoramento

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.signallq.app.BuildConfig
import io.signallq.app.analytics.distributionChannel
import io.signallq.app.analytics.environmentFor
import io.signallq.app.core.database.MedicaoDao
import io.signallq.app.core.database.chat.ChatSessionDao
import io.signallq.app.core.database.recommendation.RecommendationHistoryDao
import io.signallq.app.core.datastore.PreferenciasAppRepository
import io.signallq.app.feature.diagnostico.ingest.AdminIngestRepository
import io.signallq.app.feature.diagnostico.ingest.toIngestPayload

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
        private val recommendationHistoryDao: RecommendationHistoryDao,
        private val adminIngestRepository: AdminIngestRepository,
    ) : CoroutineWorker(appContext, params) {
        internal companion object {
            const val TAG = "AdminSyncWorker"
            const val BATCH_SIZE = 50
        }

        override suspend fun doWork(): Result {
            Log.d(TAG, "Iniciando sync retroativo (tentativa ${runAttemptCount + 1})")
            return try {
                val distChannel = distributionChannel(applicationContext)
                val environment = environmentFor(distChannel)
                val buildType = BuildConfig.BUILD_TYPE
                val versionCode = BuildConfig.VERSION_CODE
                val deviceId =
                    runCatching {
                        preferenciasAppRepository.buscarOuGerarAnonDeviceId()
                    }.getOrDefault("unknown")
                val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                val osVersion = "Android ${Build.VERSION.RELEASE}"
                val appVersion = BuildConfig.VERSION_NAME
                syncMedicoes(environment, distChannel, buildType, versionCode, deviceId, deviceModel, osVersion, appVersion)
                syncChatSessions(environment, distChannel, buildType, versionCode, deviceId)
                syncRecommendationFeedback(environment, distChannel, buildType, versionCode, deviceId, appVersion)
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
        private suspend fun syncMedicoes(
            environment: String,
            distChannel: String,
            buildType: String,
            versionCode: Int,
            deviceId: String,
            deviceModel: String,
            osVersion: String,
            appVersion: String,
        ) {
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
                    adminIngestRepository.sendDiagnostic(
                        medicao.toIngestPayload(
                            environment = environment,
                            distChannel = distChannel,
                            buildType = buildType,
                            versionCode = versionCode,
                            deviceId = deviceId,
                            deviceModel = deviceModel,
                            osVersion = osVersion,
                            appVersion = appVersion,
                        ),
                    )
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
        private suspend fun syncChatSessions(
            environment: String,
            distChannel: String,
            buildType: String,
            versionCode: Int,
            deviceId: String,
        ) {
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
                    adminIngestRepository.sendAiUsage(
                        sessao.toIngestPayload(
                            environment = environment,
                            distChannel = distChannel,
                            buildType = buildType,
                            versionCode = versionCode,
                            deviceId = deviceId,
                        ),
                    )
                }
                val maxEpoch = batch.maxOf { it.criadoEmEpochMs }
                preferenciasAppRepository.salvarAdminSyncChatLastEpochMs(maxEpoch)
                Log.d(TAG, "syncChatSessions: batch de ${batch.size} enviado, checkpoint=$maxEpoch")
            }
        }

        /**
         * Sincroniza feedback (util/nao util/ocultar) de recomendacoes do Recommendation
         * Engine desde o ultimo checkpoint -- design-tobe-alinhamento, tela 1a. A persistencia
         * local (Room, issue #812) ja existia e funcionava; só faltava entrar no sync
         * retroativo pro signallq-admin-worker.
         */
        private suspend fun syncRecommendationFeedback(
            environment: String,
            distChannel: String,
            buildType: String,
            versionCode: Int,
            deviceId: String,
            appVersion: String,
        ) {
            val lastEpoch = preferenciasAppRepository.buscarAdminSyncRecommendationFeedbackLastEpochMs()
            Log.d(TAG, "syncRecommendationFeedback: checkpoint=$lastEpoch")

            val pendentes = recommendationHistoryDao.buscarComFeedbackDesde(lastEpoch)

            if (pendentes.isEmpty()) {
                Log.d(TAG, "syncRecommendationFeedback: nenhum feedback pendente")
                return
            }

            Log.d(TAG, "syncRecommendationFeedback: ${pendentes.size} feedbacks pendentes")

            pendentes.chunked(BATCH_SIZE).forEach { batch ->
                batch.forEach { entrada ->
                    adminIngestRepository.sendAnalyticsEvent(
                        entrada.toIngestPayload(
                            appVersion = appVersion,
                            environment = environment,
                            distChannel = distChannel,
                            buildType = buildType,
                            versionCode = versionCode,
                            deviceId = deviceId,
                        ),
                    )
                }
                val maxEpoch = batch.mapNotNull { it.feedbackAtEpochMs }.maxOrNull() ?: lastEpoch
                preferenciasAppRepository.salvarAdminSyncRecommendationFeedbackLastEpochMs(maxEpoch)
                Log.d(TAG, "syncRecommendationFeedback: batch de ${batch.size} enviado, checkpoint=$maxEpoch")
            }
        }
    }
