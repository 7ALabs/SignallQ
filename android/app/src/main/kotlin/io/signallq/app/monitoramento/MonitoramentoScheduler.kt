package io.signallq.app.monitoramento

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

internal object MonitoramentoScheduler {
    private const val WORK_TAG = "linka_monitoramento_passivo"

    /**
     * Frequência real do WorkManager (issue #1666, Task 2.0.18) — usada também pela UI
     * (`MonitoramentoSheet.kt`) para comunicar honestamente de quanto em quanto tempo a
     * checagem acontece de fato, em vez de linguagem vaga tipo "acompanhamos sempre".
     * Alterar aqui exige alterar a comunicação na UI junto — não são fontes independentes.
     */
    const val INTERVALO_MINUTOS = 30

    fun agendar(context: Context) {
        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

        val request =
            PeriodicWorkRequestBuilder<MonitoramentoWorker>(INTERVALO_MINUTOS.toLong(), TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .addTag(WORK_TAG)
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(WORK_TAG, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun cancelar(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
    }
}
