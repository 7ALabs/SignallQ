package io.signallq.app.notificacao

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.signallq.app.MainActivity
import io.signallq.app.R

object SignallQNotificationHelper {
    private const val CANAL_ID = "linka_monitoramento"
    private const val PREFS_NAME = "linka_notif_cooldown"
    private const val KEY_CONTAGEM_DIA = "contagem_dia"
    private const val KEY_DATA_CONTAGEM = "data_contagem"
    private const val MAX_POR_DIA = 3

    private const val ID_LATENCIA = 1
    private const val ID_DNS = 2
    private const val ID_WIFI_FRACO = 3
    private const val ID_SEM_INTERNET = 4
    private const val ID_DISPOSITIVO_NOVO = 5

    private val cooldownMs =
        mapOf(
            ID_LATENCIA to 4 * 60 * 60 * 1000L,
            ID_DNS to 4 * 60 * 60 * 1000L,
            ID_WIFI_FRACO to 8 * 60 * 60 * 1000L,
            ID_SEM_INTERNET to 30 * 60 * 1000L,
            ID_DISPOSITIVO_NOVO to 60 * 60 * 1000L, // 1h entre notificacoes de dispositivo novo
        )

    fun criarCanais(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal =
            NotificationChannel(
                CANAL_ID,
                "Monitoramento de rede",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Alertas sobre a qualidade da sua conexão" }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(canal)
    }

    fun notificarLatenciaAlta(
        context: Context,
        latenciaMs: Long,
    ) {
        disparar(
            context = context,
            id = ID_LATENCIA,
            titulo = "Conexão lenta detectada",
            corpo = "Latência alta detectada: ${latenciaMs}ms (normal: <150ms)",
        )
    }

    fun notificarDnsLento(
        context: Context,
        dnsMs: Long,
    ) {
        disparar(
            context = context,
            id = ID_DNS,
            titulo = "DNS com lentidão",
            corpo = "DNS lento: ${dnsMs}ms — pode causar lentidão ao abrir sites",
        )
    }

    fun notificarWifiFraco(
        context: Context,
        rssi: Int,
    ) {
        disparar(
            context = context,
            id = ID_WIFI_FRACO,
            titulo = "Sinal Wi-Fi fraco",
            corpo = "Sinal Wi-Fi fraco: ${rssi}dBm — aproxime-se do roteador",
        )
    }

    fun notificarSemInternet(context: Context) {
        disparar(
            context = context,
            id = ID_SEM_INTERNET,
            titulo = "Sem conexão com a internet",
            corpo = "Sem internet — detectado pelo monitoramento SignallQ",
        )
    }

    fun notificarDispositivoNovo(
        context: Context,
        mac: String,
    ) {
        disparar(
            context = context,
            id = ID_DISPOSITIVO_NOVO,
            titulo = "Novo dispositivo na rede",
            corpo = "Um dispositivo desconhecido foi detectado na sua rede Wi-Fi ($mac). Toque para ver detalhes.",
        )
    }

    private fun disparar(
        context: Context,
        id: Int,
        titulo: String,
        corpo: String,
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val agora = System.currentTimeMillis()

        // verificar cooldown por tipo
        val ultimoDisparo = prefs.getLong("ultimo_$id", 0L)
        val cooldown = cooldownMs[id] ?: return
        if (agora - ultimoDisparo < cooldown) return

        // verificar teto diário
        val hoje = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date(agora))
        val dataContagem = prefs.getString(KEY_DATA_CONTAGEM, "")
        val contagem = if (dataContagem == hoje) prefs.getInt(KEY_CONTAGEM_DIA, 0) else 0
        if (contagem >= MAX_POR_DIA) return

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val notificacao =
            NotificationCompat
                .Builder(context, CANAL_ID)
                .setSmallIcon(R.drawable.ic_notification_signallq)
                .setContentTitle(titulo)
                .setContentText(corpo)
                .setStyle(NotificationCompat.BigTextStyle().bigText(corpo))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .addAction(0, "Ver diagnóstico", pendingIntent)
                .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notificacao)
            prefs
                .edit()
                .putLong("ultimo_$id", agora)
                .putString(KEY_DATA_CONTAGEM, hoje)
                .putInt(KEY_CONTAGEM_DIA, contagem + 1)
                .apply()
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS não concedida — silencioso
        }
    }
}
