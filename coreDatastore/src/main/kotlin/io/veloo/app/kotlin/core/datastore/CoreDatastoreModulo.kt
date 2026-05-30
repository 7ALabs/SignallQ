package io.veloo.app.core.datastore

import android.content.Context

object CoreDatastoreModulo {
    fun criarPreferenciasAppRepository(context: Context): PreferenciasAppRepository {
        return PreferenciasAppRepository(context.applicationContext)
    }
}
