@file:Suppress("ForbiddenImport") // Usa android.util.Log apenas para constantes de prioridade (Log.WARN), não para logging direto.

package io.signallq.app.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.signallq.app.core.network.AnalyticsTracker
import timber.log.Timber

/**
 * Timber.Tree para builds de release.
 * Filtra apenas WARN e ERROR.
 * Encaminha para Crashlytics e registra feature_crash via AnalyticsTracker.
 */
internal class ReleaseTree(
    private val analyticsTracker: AnalyticsTracker,
) : Timber.Tree() {
    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        if (priority < Log.WARN) return

        FirebaseCrashlytics.getInstance().log("[$tag] $message")
        t?.let { FirebaseCrashlytics.getInstance().recordException(it) }

        if (priority >= Log.ERROR) {
            // tag e o modulo/feature — ex: "SpeedtestViewModel", "DiagnosticoViewModel"
            val featureId = tag?.lowercase()?.replace("viewmodel", "")?.trim() ?: "unknown"
            val errorType = t?.javaClass?.simpleName ?: "LoggedError"
            analyticsTracker.registrarFeatureCrash(featureId, errorType)
        }
    }
}
