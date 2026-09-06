package io.signallq.app.analytics

import android.content.Context
import android.os.Build

/**
 * Identifica o canal de distribuicao da instalacao atual (SIG-143).
 *
 * "production" so quando instalado via Play Store (play_store). Firebase App
 * Distribution chega como "sideload" -> homologacao. Extraido de AdminSyncWorker
 * (unico consumidor original) para ser reaproveitado por CompositeAnalyticsTracker
 * (GH#759) sem duplicar a logica.
 */
fun distributionChannel(context: Context): String =
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val info = context.packageManager.getInstallSourceInfo(context.packageName)
            when (info.initiatingPackageName) {
                "com.android.vending" -> "play_store"
                null -> "sideload"
                else -> info.initiatingPackageName ?: "unknown"
            }
        } else {
            @Suppress("DEPRECATION")
            when (context.packageManager.getInstallerPackageName(context.packageName)) {
                "com.android.vending" -> "play_store"
                null -> "sideload"
                else -> "unknown"
            }
        }
    } catch (e: Exception) {
        "unknown"
    }

/** "production" apenas para play_store, "staging" para qualquer outro canal (SIG-143). */
fun environmentFor(distChannel: String): String =
    if (distChannel == "play_store") "production" else "staging"
