@file:Suppress("ForbiddenImport") // Usa android.util.Log apenas para constantes de prioridade (Log.WARN), não para logging direto.

package io.linka.app.kotlin.logging

import android.util.Log
import timber.log.Timber

/**
 * Timber.Tree para builds de release.
 * Filtra apenas WARN e ERROR — INFO, DEBUG e VERBOSE são descartados silenciosamente.
 *
 * Futuramente pode encaminhar para Crashlytics ou outro backend de logging.
 */
internal class ReleaseTree : Timber.Tree() {
    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        if (priority < Log.WARN) {
            return
        }
        // Por agora silencioso em produção.
        // TODO: encaminhar para Crashlytics quando integrado:
        //   FirebaseCrashlytics.getInstance().log("[$tag] $message")
        //   t?.let { FirebaseCrashlytics.getInstance().recordException(it) }
    }
}
