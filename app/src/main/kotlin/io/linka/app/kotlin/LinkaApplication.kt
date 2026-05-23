package io.linka.app.kotlin

import android.app.Application
import io.linka.app.kotlin.notificacao.LinkaNotificationHelper

class LinkaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LinkaNotificationHelper.criarCanais(this)
    }
}
