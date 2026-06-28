package io.signallq.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import io.signallq.app.feature.diagnostico.DiagnosticStatus
import io.signallq.app.feature.speedtest.ResultadoSpeedtest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ResultadoBitmapGenerator {
    suspend fun gerarECompartilhar(
        context: Context,
        resultado: ResultadoSpeedtest,
        diagnosticoHeadline: String?,
        diagnosticoStatus: DiagnosticStatus?,
    ) {
        val uri =
            withContext(Dispatchers.IO) {
                val bitmap = gerarBitmap(resultado, diagnosticoHeadline, diagnosticoStatus)
                salvarEmCache(context, bitmap)
            }
        withContext(Dispatchers.Main) {
            compartilhar(context, uri)
        }
    }

    private fun gerarBitmap(
        resultado: ResultadoSpeedtest,
        headline: String?,
        status: DiagnosticStatus?,
    ): Bitmap {
        val width = 1080
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Cor de fundo por severidade
        val bgColor =
            when (status) {
                DiagnosticStatus.ok -> Color.argb(38, 34, 197, 94) // success ~15%
                DiagnosticStatus.attention -> Color.argb(38, 234, 179, 8) // warning ~15%
                DiagnosticStatus.critical -> Color.argb(38, 239, 68, 68) // error ~15%
                else -> Color.argb(255, 30, 30, 36) // neutro escuro
            }
        canvas.drawColor(bgColor)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Logo "SignallQ SpeedTest"
        paint.color = Color.argb(180, 255, 255, 255)
        paint.textSize = 36f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("SignallQ SpeedTest", 64f, 72f, paint)

        // Download label + valor
        paint.color = Color.WHITE
        paint.textSize = 28f
        canvas.drawText("Download", 64f, 160f, paint)
        paint.textSize = 96f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("%.1f".format(resultado.downloadMbps), 64f, 280f, paint)
        paint.textSize = 32f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Mbps", 64f, 320f, paint)

        // Upload label + valor
        paint.textSize = 28f
        canvas.drawText("Upload", 580f, 160f, paint)
        paint.textSize = 96f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val uploadText = if (resultado.uploadNaoDetectado) "—" else "%.1f".format(resultado.uploadMbps)
        canvas.drawText(uploadText, 580f, 280f, paint)
        paint.textSize = 32f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        if (!resultado.uploadNaoDetectado) canvas.drawText("Mbps", 580f, 320f, paint)

        // Latência + Jitter
        paint.textSize = 28f
        canvas.drawText("Atraso", 64f, 390f, paint)
        paint.textSize = 52f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("%.0f ms".format(resultado.latenciaMs), 64f, 450f, paint)

        paint.textSize = 28f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Variacao", 400f, 390f, paint)
        paint.textSize = 52f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("%.0f ms".format(resultado.jitterMs), 400f, 450f, paint)

        // Divisor
        paint.color = Color.argb(80, 255, 255, 255)
        paint.strokeWidth = 2f
        canvas.drawLine(64f, 480f, 1016f, 480f, paint)

        // Headline diagnóstico
        if (!headline.isNullOrBlank()) {
            paint.color = Color.WHITE
            paint.textSize = 36f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val headlineTruncada = if (headline.length > 60) headline.take(57) + "..." else headline
            canvas.drawText(headlineTruncada, 64f, 540f, paint)
        }

        // Data/hora
        val formatter = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale("pt", "BR"))
        val dataHora = formatter.format(Date(resultado.timestampEpochMs))
        paint.color = Color.argb(160, 255, 255, 255)
        paint.textSize = 28f
        canvas.drawText(dataHora, 64f, 584f, paint)

        return bitmap
    }

    private fun salvarEmCache(
        context: Context,
        bitmap: Bitmap,
    ): Uri {
        val shareDir = File(context.cacheDir, "share").also { it.mkdirs() }
        // Limpar PNGs antigos
        shareDir.listFiles()?.forEach { it.delete() }
        val file = File(shareDir, "resultado_signallq.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun compartilhar(
        context: Context,
        uri: Uri,
    ) {
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(
            Intent.createChooser(intent, "Compartilhar resultado").also {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }
}
