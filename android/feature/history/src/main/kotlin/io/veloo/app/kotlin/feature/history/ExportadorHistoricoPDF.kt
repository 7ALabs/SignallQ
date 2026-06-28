package io.signallq.app.feature.history

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.webkit.WebView
import android.webkit.WebViewClient
import io.signallq.app.core.database.MedicaoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

// v2.0 — Layout rico: HTML/CSS embutido + paginação automática via WebView ou PdfDocument multi-página.
// Assinatura original mantida para compatibilidade com testes JVM existentes.
// Overload com Context usa WebView.createPrintDocumentAdapter() para layout rico completo.

private const val PAGINA_LARGURA_PX = 595   // A4 em pontos (72dpi)
private const val PAGINA_ALTURA_PX = 842    // A4 em pontos (72dpi)
private const val MARGEM = 40f
private const val ALTURA_LINHA = 16f
private const val LINHAS_CABECALHO = 5      // título + subtítulo + data + header tabela + separador
private const val LINHAS_UTEIS_POR_PAGINA = 44 // ~(842 - 2*40) / 16

/**
 * Exporta histórico de medições para PDF.
 *
 * Versão 2.0:
 *  - [exportar] (sem Context): PdfDocument multi-página com layout melhorado — compatível com JVM tests.
 *  - [exportarComWebView] (com Context): WebView.createPrintDocumentAdapter() para HTML/CSS rico.
 *  - [gerarHtml]: função interna pura e testável — gera o template HTML/CSS.
 */
class ExportadorHistoricoPDF {

    private val formatadorDataHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val formatadorDataRelatorio = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault())

    // ─── Painters ─────────────────────────────────────────────────────────────

    private val paintTitulo = Paint().apply {
        textSize = 20f
        isFakeBoldText = true
        color = Color.parseColor("#1565C0")
    }
    private val paintSubtitulo = Paint().apply {
        textSize = 10f
        color = Color.parseColor("#757575")
    }
    private val paintCabecalhoTabela = Paint().apply {
        textSize = 10f
        isFakeBoldText = true
        color = Color.WHITE
    }
    private val paintCabecalhoFundo = Paint().apply {
        color = Color.parseColor("#1565C0")
    }
    private val paintTextoDado = Paint().apply {
        textSize = 9f
        color = Color.parseColor("#212121")
    }
    private val paintFundoAlternado = Paint().apply {
        color = Color.parseColor("#F5F5F5")
    }
    private val paintLinhaSeparador = Paint().apply {
        color = Color.parseColor("#E0E0E0")
        strokeWidth = 0.5f
    }
    private val paintRodape = Paint().apply {
        textSize = 8f
        color = Color.parseColor("#9E9E9E")
    }
    private val paintPaginacao = Paint().apply {
        textSize = 8f
        color = Color.parseColor("#757575")
        textAlign = Paint.Align.RIGHT
    }

    // ─── Colunas da tabela ────────────────────────────────────────────────────

    private val colDataX = MARGEM
    private val colDlX = MARGEM + 140f
    private val colUlX = MARGEM + 215f
    private val colLatX = MARGEM + 290f
    private val colJitterX = MARGEM + 355f
    private val colFonteX = MARGEM + 415f
    private val larguraColunas = PAGINA_LARGURA_PX - MARGEM * 2

    // ─── API pública sem Context (compatível com testes JVM) ──────────────────

    /**
     * Exporta usando [PdfDocument] multi-página com layout tabular melhorado.
     * Compatível com testes JVM (não usa WebView nem Context Android de UI).
     */
    suspend fun exportar(
        medicoes: List<MedicaoEntity>,
        arquivo: File,
    ): Boolean = withContext(Dispatchers.IO) {
        val pdf = PdfDocument()
        try {
            val chunks = medicoes.chunked(LINHAS_UTEIS_POR_PAGINA)
            val totalPaginas = maxOf(1, chunks.size)

            if (chunks.isEmpty()) {
                // PDF vazio mas válido — ao menos uma página
                val pageInfo = PdfDocument.PageInfo.Builder(PAGINA_LARGURA_PX, PAGINA_ALTURA_PX, 1).create()
                val page = pdf.startPage(pageInfo)
                desenharCabecalho(page.canvas, 1, totalPaginas, medicoes.size)
                desenharRodape(page.canvas, medicoes.size)
                pdf.finishPage(page)
            } else {
                chunks.forEachIndexed { idx, chunk ->
                    val numeroPagina = idx + 1
                    val pageInfo = PdfDocument.PageInfo.Builder(PAGINA_LARGURA_PX, PAGINA_ALTURA_PX, numeroPagina).create()
                    val page = pdf.startPage(pageInfo)
                    val canvas = page.canvas

                    var y = desenharCabecalho(canvas, numeroPagina, totalPaginas, medicoes.size)
                    y = desenharHeaderTabela(canvas, y)
                    chunk.forEachIndexed { linhaIdx, medicao ->
                        y = desenharLinhaTabela(canvas, medicao, linhaIdx, y)
                    }
                    desenharRodape(canvas, medicoes.size)
                    pdf.finishPage(page)
                }
            }

            FileOutputStream(arquivo).use { stream ->
                pdf.writeTo(stream)
            }
            true
        } catch (e: Exception) {
            false
        } finally {
            pdf.close()
        }
    }

    // ─── API com Context — WebView rich HTML (overload v2.0) ──────────────────

    /**
     * Exporta usando WebView.createPrintDocumentAdapter() para layout HTML/CSS rico.
     * Requer Context Android (UI thread para criar WebView).
     * Paginação automática gerenciada pelo WebView.
     */
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun exportarComWebView(
        medicoes: List<MedicaoEntity>,
        arquivo: File,
        context: Context,
    ): Boolean {
        return try {
            val html = gerarHtml(medicoes)

            val resultado = withContext(Dispatchers.Main) {
                withTimeoutOrNull(10_000L) {
                    suspendCancellableCoroutine { cont ->
                        val webView = WebView(context).apply {
                            settings.javaScriptEnabled = false
                            setBackgroundColor(Color.WHITE)
                        }

                        webView.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                if (!cont.isActive) return

                                try {
                                    // minSdk=24 > LOLLIPOP(21) — createPrintDocumentAdapter(String) sempre disponível
                                    val printAdapter = view?.createPrintDocumentAdapter("historico_signallq")

                                    if (printAdapter == null) {
                                        cont.resume(false)
                                        return
                                    }

                                    // Escreve o PDF usando PdfPrint helper
                                    PdfPrintHelper.imprimir(printAdapter, arquivo) { sucesso ->
                                        cont.resume(sucesso)
                                    }
                                } catch (e: Exception) {
                                    cont.resume(false)
                                }
                            }
                        }

                        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                    }
                }
            }
            resultado ?: false  // timeout → falha graceful
        } catch (e: Exception) {
            false
        }
    }

    // ─── Geração de HTML (pura — testável em JVM) ─────────────────────────────

    /**
     * Gera o HTML completo com CSS embutido para o relatório de medições.
     * Função pura — sem dependências Android, testável em JVM.
     */
    internal fun gerarHtml(medicoes: List<MedicaoEntity>): String {
        val dataGeracao = formatadorDataRelatorio.format(Date())
        val linhasHtml = medicoes.joinToString("\n") { medicao ->
            val dataHora = formatadorDataHora.format(Date(medicao.timestampEpochMs))
            val dl = medicao.downloadMbps?.let { String.format(Locale.US, "%.1f", it) } ?: "—"
            val ul = medicao.uploadMbps?.let { String.format(Locale.US, "%.1f", it) } ?: "—"
            val lat = medicao.latencyMs?.let { String.format(Locale.US, "%.0f", it) } ?: "—"
            val fonte = medicao.fonte?.let { escapeHtml(it) } ?: "—"
            """        <tr>
          <td>$dataHora</td>
          <td class="num">$dl Mbps</td>
          <td class="num">$ul Mbps</td>
          <td class="num">$lat ms</td>
          <td>$fonte</td>
        </tr>"""
        }

        val totalMedicoes = medicoes.size
        val dlMedia = medicoes.mapNotNull { it.downloadMbps }.let { vals ->
            if (vals.isEmpty()) "—" else String.format(Locale.US, "%.1f Mbps", vals.average())
        }
        val ulMedia = medicoes.mapNotNull { it.uploadMbps }.let { vals ->
            if (vals.isEmpty()) "—" else String.format(Locale.US, "%.1f Mbps", vals.average())
        }
        val latMedia = medicoes.mapNotNull { it.latencyMs }.let { vals ->
            if (vals.isEmpty()) "—" else String.format(Locale.US, "%.0f ms", vals.average())
        }

        return """<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Relatório SignallQ</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Arial', sans-serif; font-size: 12px; color: #212121; margin: 24px; background: #fff; }

    .header { margin-bottom: 20px; border-bottom: 2px solid #1565C0; padding-bottom: 12px; }
    .header h1 { color: #1565C0; font-size: 22px; font-weight: bold; margin-bottom: 4px; }
    .header .subtitle { color: #757575; font-size: 11px; }

    .resumo { display: flex; gap: 16px; margin-bottom: 20px; }
    .resumo-card { background: #E3F2FD; border-radius: 6px; padding: 10px 16px; flex: 1; }
    .resumo-card .label { font-size: 10px; color: #1565C0; text-transform: uppercase; font-weight: bold; }
    .resumo-card .valor { font-size: 16px; font-weight: bold; color: #0D47A1; margin-top: 2px; }

    table { width: 100%; border-collapse: collapse; font-size: 11px; }
    thead tr { background: #1565C0; color: white; }
    thead th { padding: 9px 8px; text-align: left; font-weight: bold; font-size: 11px; }
    thead th.num { text-align: right; }
    tbody tr:nth-child(even) { background: #F5F5F5; }
    tbody tr:hover { background: #E3F2FD; }
    tbody td { padding: 7px 8px; border-bottom: 1px solid #E0E0E0; }
    tbody td.num { text-align: right; font-family: monospace; }

    .footer { margin-top: 20px; font-size: 9px; color: #9E9E9E; text-align: center; border-top: 1px solid #E0E0E0; padding-top: 8px; }
    .empty { text-align: center; padding: 32px; color: #757575; font-size: 13px; }

    @media print {
      body { margin: 0; }
      thead { display: table-header-group; }
      tbody tr { page-break-inside: avoid; }
    }
  </style>
</head>
<body>
  <div class="header">
    <h1>Relatório de Medições — SignallQ</h1>
    <p class="subtitle">Gerado em: $dataGeracao &nbsp;|&nbsp; Total de registros: $totalMedicoes</p>
  </div>

  <div class="resumo">
    <div class="resumo-card">
      <div class="label">Download médio</div>
      <div class="valor">$dlMedia</div>
    </div>
    <div class="resumo-card">
      <div class="label">Upload médio</div>
      <div class="valor">$ulMedia</div>
    </div>
    <div class="resumo-card">
      <div class="label">Latência média</div>
      <div class="valor">$latMedia</div>
    </div>
  </div>

  <table>
    <thead>
      <tr>
        <th>Data/Hora</th>
        <th class="num">Download</th>
        <th class="num">Upload</th>
        <th class="num">Latência</th>
        <th>Fonte</th>
      </tr>
    </thead>
    <tbody>
${if (medicoes.isEmpty()) "      <tr><td colspan=\"5\" class=\"empty\">Nenhuma medição registrada.</td></tr>" else linhasHtml}
    </tbody>
  </table>

  <div class="footer">
    SignallQ — monitoramento inteligente de rede &nbsp;|&nbsp; Relatório gerado automaticamente
  </div>
</body>
</html>"""
    }

    // ─── Desenho interno (PdfDocument multi-página) ───────────────────────────

    private fun desenharCabecalho(
        canvas: android.graphics.Canvas,
        paginaAtual: Int,
        totalPaginas: Int,
        totalMedicoes: Int,
    ): Float {
        var y = MARGEM + 24f

        canvas.drawText("Relatório de Medições — SignallQ", MARGEM, y, paintTitulo)
        y += 6f
        canvas.drawLine(MARGEM, y, PAGINA_LARGURA_PX - MARGEM, y, paintCabecalhoFundo.apply { strokeWidth = 1.5f })
        y += 10f

        if (paginaAtual == 1) {
            val dataAtual = formatadorDataRelatorio.format(Date())
            canvas.drawText("Gerado em: $dataAtual  |  Total de registros: $totalMedicoes", MARGEM, y, paintSubtitulo)
            y += 18f
        } else {
            // páginas seguintes: só indicador de continuação
            canvas.drawText("(continuação)", MARGEM, y, paintSubtitulo)
            y += 14f
        }

        // Número da página no canto superior direito
        canvas.drawText(
            "Pág. $paginaAtual / $totalPaginas",
            PAGINA_LARGURA_PX - MARGEM,
            MARGEM + 10f,
            paintPaginacao,
        )

        return y
    }

    private fun desenharHeaderTabela(canvas: android.graphics.Canvas, yInicial: Float): Float {
        val alturaHeader = ALTURA_LINHA + 8f
        canvas.drawRect(MARGEM, yInicial, PAGINA_LARGURA_PX - MARGEM, yInicial + alturaHeader, paintCabecalhoFundo)
        val yTexto = yInicial + alturaHeader - 5f
        canvas.drawText("Data/Hora", colDataX + 4f, yTexto, paintCabecalhoTabela)
        canvas.drawText("DL (Mbps)", colDlX, yTexto, paintCabecalhoTabela)
        canvas.drawText("UL (Mbps)", colUlX, yTexto, paintCabecalhoTabela)
        canvas.drawText("Lat (ms)", colLatX, yTexto, paintCabecalhoTabela)
        canvas.drawText("Jitter", colJitterX, yTexto, paintCabecalhoTabela)
        canvas.drawText("Fonte", colFonteX, yTexto, paintCabecalhoTabela)
        return yInicial + alturaHeader
    }

    private fun desenharLinhaTabela(
        canvas: android.graphics.Canvas,
        medicao: MedicaoEntity,
        linhaIdx: Int,
        yInicial: Float,
    ): Float {
        val alturaLinha = ALTURA_LINHA + 4f

        if (linhaIdx % 2 == 1) {
            canvas.drawRect(MARGEM, yInicial, PAGINA_LARGURA_PX - MARGEM, yInicial + alturaLinha, paintFundoAlternado)
        }

        val yTexto = yInicial + alturaLinha - 5f
        val dataHora = formatadorDataHora.format(Date(medicao.timestampEpochMs))
        val dl = medicao.downloadMbps?.let { String.format(Locale.US, "%.1f", it) } ?: "—"
        val ul = medicao.uploadMbps?.let { String.format(Locale.US, "%.1f", it) } ?: "—"
        val lat = medicao.latencyMs?.let { String.format(Locale.US, "%.0f", it) } ?: "—"
        val jitter = medicao.jitterMs?.let { String.format(Locale.US, "%.1f", it) } ?: "—"
        val fonte = (medicao.fonte ?: "—").take(12)

        canvas.drawText(dataHora, colDataX + 4f, yTexto, paintTextoDado)
        canvas.drawText(dl, colDlX, yTexto, paintTextoDado)
        canvas.drawText(ul, colUlX, yTexto, paintTextoDado)
        canvas.drawText(lat, colLatX, yTexto, paintTextoDado)
        canvas.drawText(jitter, colJitterX, yTexto, paintTextoDado)
        canvas.drawText(fonte, colFonteX, yTexto, paintTextoDado)

        val yLinhaSep = yInicial + alturaLinha
        canvas.drawLine(MARGEM, yLinhaSep, PAGINA_LARGURA_PX - MARGEM, yLinhaSep, paintLinhaSeparador)

        return yLinhaSep
    }

    private fun desenharRodape(canvas: android.graphics.Canvas, totalMedicoes: Int) {
        val yRodape = PAGINA_ALTURA_PX - MARGEM + 10f
        canvas.drawLine(MARGEM, yRodape - 10f, PAGINA_LARGURA_PX - MARGEM, yRodape - 10f, paintLinhaSeparador)
        canvas.drawText(
            "SignallQ — monitoramento inteligente de rede  |  $totalMedicoes medições exportadas",
            MARGEM,
            yRodape,
            paintRodape,
        )
    }

    // ─── Utilitários ──────────────────────────────────────────────────────────

    private fun escapeHtml(text: String): String =
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
