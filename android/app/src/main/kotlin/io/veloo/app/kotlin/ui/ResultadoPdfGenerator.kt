package io.signallq.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import io.signallq.app.feature.diagnostico.SnapshotDiagnostico
import io.signallq.app.feature.speedtest.ResultadoSpeedtest
import io.signallq.app.ui.screen.AnalisadorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exporta o resultado do teste (tela "Resultado do teste", GH#536) como PDF completo:
 * resultado geral, data/hora, tipo de rede, métricas completas, diagnóstico IA,
 * recomendações, detalhes técnicos e disclaimer Anatel discreto. Substitui o antigo
 * compartilhamento por imagem ([ResultadoBitmapGenerator], removido) — o botão de
 * compartilhar da tela agora sempre gera o laudo em PDF.
 */
object ResultadoPdfGenerator {
    private const val PAGE_WIDTH = 595f
    private const val PAGE_HEIGHT = 842f
    private const val MARGEM = 40f
    private const val COL_VALOR = 220f
    private const val LIMITE_INFERIOR = PAGE_HEIGHT - 50f

    suspend fun gerarECompartilhar(
        context: Context,
        resultado: ResultadoSpeedtest,
        snapshotDiagnostico: SnapshotDiagnostico,
        analisadorState: AnalisadorState,
        ispInfo: IspInfo?,
        operadoraMovel: String?,
        localizacaoServidor: String?,
    ) {
        val uri =
            withContext(Dispatchers.IO) {
                gerarPdf(context, resultado, snapshotDiagnostico, analisadorState, ispInfo, operadoraMovel, localizacaoServidor)
            }
        withContext(Dispatchers.Main) {
            compartilhar(context, uri)
        }
    }

    private fun gerarPdf(
        context: Context,
        resultado: ResultadoSpeedtest,
        snapshotDiagnostico: SnapshotDiagnostico,
        analisadorState: AnalisadorState,
        ispInfo: IspInfo?,
        operadoraMovel: String?,
        localizacaoServidor: String?,
    ): Uri {
        val document = PdfDocument()
        val writer = PaginaPdfWriter(document)

        val paintTitulo =
            Paint().apply {
                textSize = 20f
                isFakeBoldText = true
                color = LkColors.Light.textPrimary.toArgb()
            }
        val paintSubtitulo =
            Paint().apply {
                textSize = 10f
                color = LkColors.Light.textSecondary.toArgb()
            }
        val paintSecao =
            Paint().apply {
                textSize = 12f
                isFakeBoldText = true
                color = LkColors.Light.textPrimary.toArgb()
            }
        val paintLabel =
            Paint().apply {
                textSize = 10f
                color = LkColors.Light.textSecondary.toArgb()
            }
        val paintValor =
            Paint().apply {
                textSize = 10f
                color = LkColors.Light.textPrimary.toArgb()
            }
        val paintLinha =
            Paint().apply {
                color = LkColors.Light.border.toArgb()
                strokeWidth = 0.5f
                style = Paint.Style.STROKE
            }
        val paintFooter =
            Paint().apply {
                textSize = 8f
                color = LkColors.Light.textTertiary.toArgb()
            }

        fun truncar(
            texto: String,
            max: Int = 78,
        ) = if (texto.length > max) texto.take(max - 1) + "…" else texto

        fun secao(
            titulo: String,
            linhas: List<Pair<String, String>>,
        ) {
            if (linhas.isEmpty()) return
            writer.ensureSpace(20f + linhas.size * 16f)
            writer.drawText(titulo, MARGEM, paintSecao)
            writer.avancar(16f)
            linhas.forEach { (label, valor) ->
                writer.drawText(label, MARGEM, paintLabel)
                writer.drawText(truncar(valor), COL_VALOR, paintValor)
                writer.avancar(16f)
            }
            writer.avancar(6f)
            writer.drawLine(MARGEM, PAGE_WIDTH - MARGEM, paintLinha)
            writer.avancar(16f)
        }

        val dataHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(resultado.timestampEpochMs))
        val tipoRede =
            when {
                resultado.connectionType.equals("wifi", ignoreCase = true) -> "Wi-Fi"
                resultado.connectionType.equals("movel", ignoreCase = true) ->
                    "Rede móvel" + (resultado.tecnologia?.let { " ($it)" } ?: "")
                else -> "Não identificado"
            }

        // Cabeçalho
        writer.drawText("SignallQ — Resultado do teste", MARGEM, paintTitulo)
        writer.avancar(18f)
        writer.drawText("Diagnóstico de velocidade e conexão", MARGEM, paintSubtitulo)
        writer.avancar(14f)
        writer.drawText("Data: $dataHora   |   Tipo de rede: $tipoRede", MARGEM, paintSubtitulo)
        writer.avancar(14f)
        writer.drawLine(MARGEM, PAGE_WIDTH - MARGEM, paintLinha)
        writer.avancar(18f)

        // Resultado geral
        val relatorio = snapshotDiagnostico.relatorio
        val decisao = relatorio?.decisao
        if (decisao != null) {
            secao(
                "RESULTADO GERAL",
                buildList {
                    add("Veredito" to truncar(decisao.titulo))
                    add("Pontuação" to "${relatorio.scoreConexao}/100 · ${relatorio.veredito}")
                    if (decisao.mensagemUsuario.isNotBlank()) add("Resumo" to truncar(decisao.mensagemUsuario, 90))
                },
            )
        }

        // Métricas completas
        secao(
            "MÉTRICAS",
            buildList {
                add("Download" to "%.1f Mbps".format(resultado.downloadMbps))
                add(
                    "Upload" to
                        if (resultado.uploadNaoDetectado) "não detectado" else "%.1f Mbps".format(resultado.uploadMbps),
                )
                add("Latência" to "%.0f ms".format(resultado.latenciaMs))
                add("Oscilação (jitter)" to "%.0f ms".format(resultado.jitterMs))
                add("Perda de pacotes" to "%.1f%%".format(resultado.perdaPercentual))
                add("Bufferbloat" to "%.0f ms".format(resultado.bufferbloatMs))
            },
        )

        // Diagnóstico IA
        val analiseEspecifica = analisadorState as? AnalisadorState.Resultado
        if (decisao != null || analiseEspecifica != null) {
            secao(
                "DIAGNÓSTICO IA",
                buildList {
                    decisao?.recomendacao?.let { add("Recomendação" to truncar(it, 90)) }
                    if (analiseEspecifica != null) {
                        add("Problema analisado" to truncar(analiseEspecifica.texto, 90))
                        analiseEspecifica.acoes.take(3).forEach { acao ->
                            add("Ação recomendada" to truncar("${acao.titulo}: ${acao.descricao}", 90))
                        }
                    }
                },
            )
        }

        // Detalhes técnicos
        secao(
            "DETALHES TÉCNICOS",
            buildList {
                add("Pico Download" to "%.1f Mbps".format(resultado.peakDownloadMbps))
                add("Pico Upload" to "%.1f Mbps".format(resultado.peakUploadMbps))
                add("Latência c/ carga ↓" to "%.0f ms".format(resultado.latencyDownloadMs))
                add("Latência c/ carga ↑" to "%.0f ms".format(resultado.latencyUploadMs))
                if (resultado.stabilityScore in 0.0..1.0) add("Estabilidade" to "%.0f%%".format(resultado.stabilityScore * 100))
                resultado.dnsLatencyMs?.let { dns ->
                    val provedor = resultado.dnsProvider ?: resultado.dnsResolverIp
                    add("DNS" + (provedor?.let { " ($it)" } ?: "") to "$dns ms")
                }
                if (!localizacaoServidor.isNullOrBlank()) add("Servidor" to localizacaoServidor)
            },
        )

        // Rede
        secao(
            "REDE",
            buildList {
                (ispInfo?.isp ?: operadoraMovel)?.takeIf { it.isNotBlank() }?.let { add("Operadora" to it) }
            },
        )

        // Rodapé Anatel discreto + identificação SignallQ
        writer.ensureSpace(40f)
        writer.avancar(4f)
        writer.drawText(
            "Resultados de velocidade podem diferir do contratado conforme regulação Anatel vigente.",
            MARGEM,
            paintFooter,
        )
        writer.avancar(12f)
        writer.drawText("Gerado pelo app SignallQ", MARGEM, paintFooter)

        writer.finalizar()

        val dir =
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: context.cacheDir.resolve("resultados").also { it.mkdirs() }
        val arquivo = File(dir, "resultado_signallq_${System.currentTimeMillis()}.pdf")
        FileOutputStream(arquivo).use { document.writeTo(it) }
        document.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo)
    }

    private fun compartilhar(
        context: Context,
        uri: Uri,
    ) {
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
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

    /** Gerencia quebra de página do PDF — o laudo tem mais seções que o Laudo de
     * diagnóstico (que cabe numa página só), então cada bloco pode empurrar pra
     * uma nova página quando não há espaço suficiente. */
    private class PaginaPdfWriter(
        private val document: PdfDocument,
    ) {
        private var pageNumber = 1
        private var page = novaPagina()
        private var canvas = page.canvas
        private var y = 55f

        private fun novaPagina(): PdfDocument.Page {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageNumber).create()
            return document.startPage(pageInfo)
        }

        fun ensureSpace(alturaNecessaria: Float) {
            if (y + alturaNecessaria > LIMITE_INFERIOR) {
                document.finishPage(page)
                pageNumber++
                page = novaPagina()
                canvas = page.canvas
                y = 55f
            }
        }

        fun drawText(
            texto: String,
            x: Float,
            paint: Paint,
        ) = canvas.drawText(texto, x, y, paint)

        fun drawLine(
            x1: Float,
            x2: Float,
            paint: Paint,
        ) = canvas.drawLine(x1, y, x2, y, paint)

        fun avancar(delta: Float) {
            y += delta
        }

        fun finalizar() {
            document.finishPage(page)
        }
    }
}
