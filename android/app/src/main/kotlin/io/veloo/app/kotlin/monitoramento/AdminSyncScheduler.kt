package io.signallq.app.monitoramento

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Agendador do [AdminSyncWorker].
 *
 * Agenda dois workers:
 *  1. One-shot retroativo — roda uma vez ao iniciar, com retry automatico.
 *     Politica KEEP: nao substitui se ja estiver agendado (idempotente em reinicializacoes).
 *  2. Periodico — mantém novos registros sincronizados a cada 6h.
 *     Politica KEEP: nao substitui periodo se ja existir.
 *
 * URL e key de ingest sao injetadas diretamente pelo Hilt no Worker —
 * nao e mais necessario passá-las via inputData.
 */
internal object AdminSyncScheduler {
    private const val WORK_NAME_RETROATIVO = "admin_retroactive_sync"
    private const val WORK_NAME_PERIODICO = "admin_periodic_sync"

    fun agendar(context: Context) {
        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        // One-shot retroativo: roda uma vez com retry exponencial a partir de 30s
        val retroativoRequest =
            OneTimeWorkRequestBuilder<AdminSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME_RETROATIVO,
            ExistingWorkPolicy.KEEP,
            retroativoRequest,
        )

        // Periodico: a cada 6h, mantem novos registros sincronizados
        val periodicoRequest =
            PeriodicWorkRequestBuilder<AdminSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_PERIODICO,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicoRequest,
        )
    }
}
