package io.linka.app.kotlin.feature.history

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import io.linka.app.kotlin.core.database.MedicaoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// v1.0 — Layout basico de texto plano via PdfDocument.
// TODO v2.0: Renderizar HTML/CSS via WebView.createPrintDocumentAdapter() para layout rico.
// TODO v2.0: Suporte a multiplas paginas para historicos longos.

private const val PAGINA_LARGURA = 595  // A4 em pontos (72dpi)
private const val PAGINA_ALTURA = 842   // A4 em pontos (72dpi)
private const val MARGEM = 50f
private const val ALTURA_LINHA = 18f
private const val MAX_LINHAS_POR_PAGINA = 36 // aproximado para A4 com fonte 11

/**
 * Exporta historico de medicoes para PDF usando [PdfDocument] (API Android).
 *
 * Versao 1.0: layout simples com tabela de texto plano.
 * Maxima de [MAX_LINHAS_POR_PAGINA] linhas por pagina — historicos longos
 * terao as primeiras medicoes truncadas na v1.0.
 */
class ExportadorHistoricoPDF {

    private val formatadorDataHora = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    private val paintTitulo = Paint().apply { textSize = 18f; isFakeBoldText = true }
    private val paintSubtitulo = Paint().apply { textSize = 11f; isFakeBoldText = true }
    private val paintTexto = Paint().apply { textSize = 11f }

    suspend fun exportar(
        medicoes: List<MedicaoEntity>,
        arquivo: File,
    ): Boolean = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGINA_LARGURA, PAGINA_ALTURA, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var y = MARGEM + 30f

            // Cabecalho
            canvas.drawText("Histórico de Medições — Linka", MARGEM, y, paintTitulo)
            y += 30f

            // Cabecalho da tabela
            canvas.drawText(
                padEnd("Data/Hora", 18) + padEnd("DL(Mbps)", 10) + padEnd("UL(Mbps)", 10) + padEnd("Lat(ms)", 10) + "Fonte",
                MARGEM, y, paintSubtitulo,
            )
            y += ALTURA_LINHA + 4f

            // Linha separadora
            canvas.drawLine(MARGEM, y, PAGINA_LARGURA - MARGEM, y, paintTexto)
            y += 10f

            // Dados — limite por pagina na v1.0
            val medicoesParaExibir = medicoes.take(MAX_LINHAS_POR_PAGINA)
            medicoesParaExibir.forEach { medicao ->
                val dataHora = formatadorDataHora.format(Date(medicao.timestampEpochMs))
                val dl = medicao.downloadMbps?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
                val ul = medicao.uploadMbps?.let { String.format(Locale.US, "%.1f", it) } ?: "-"
                val lat = medicao.latencyMs?.let { String.format(Locale.US, "%.0f", it) } ?: "-"
                val fonte = medicao.fonte ?: "-"

                canvas.drawText(
                    padEnd(dataHora, 18) + padEnd(dl, 10) + padEnd(ul, 10) + padEnd(lat, 10) + fonte,
                    MARGEM, y, paintTexto,
                )
                y += ALTURA_LINHA
            }

            // Rodape com aviso se truncado
            if (medicoes.size > MAX_LINHAS_POR_PAGINA) {
                y += 10f
                canvas.drawText(
                    "* Mostrando ${MAX_LINHAS_POR_PAGINA} de ${medicoes.size} medicoes. Exporte em CSV para historico completo.",
                    MARGEM, y, paintTexto.apply { textSize = 9f },
                )
            }

            pdfDocument.finishPage(page)

            FileOutputStream(arquivo).use { stream ->
                pdfDocument.writeTo(stream)
            }
            true
        } catch (e: Exception) {
            false
        } finally {
            pdfDocument.close()
        }
    }

    private fun padEnd(text: String, length: Int): String =
        text.padEnd(length).take(length)
}
