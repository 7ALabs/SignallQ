package io.linka.app.kotlin

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.linka.app.kotlin.notificacao.LinkaNotificationHelper

@HiltAndroidApp
class LinkaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LinkaNotificationHelper.criarCanais(this)
    }
}
