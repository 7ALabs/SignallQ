package io.signallq.app.core.relatorio

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume

private const val TIMEOUT_CARREGAMENTO_MS = 10_000L

/**
 * Motor de paginação HTML→PDF via WebView.createPrintDocumentAdapter(). Extraído de
 * ExportadorHistoricoPDF.exportarComWebView() (:featureHistory, issue #1157 Fase 1b) — o motor
 * não conhece o schema de dado do chamador, só recebe o [html] já pronto. Quem gera o HTML
 * (layout/dado específico do consumidor ou, no futuro, do laudo técnico do Pro) fica fora deste
 * módulo — ver [io.signallq.app.feature.history] `gerarHtml`.
 *
 * Requer Context Android (UI thread para criar WebView). Paginação automática gerenciada pelo
 * WebView.
 */
@SuppressLint("SetJavaScriptEnabled")
suspend fun exportarHtmlComoPdf(
    html: String,
    arquivo: File,
    context: Context,
): Boolean {
    return try {
        val resultado = withContext(Dispatchers.Main) {
            withTimeoutOrNull(TIMEOUT_CARREGAMENTO_MS) {
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
        resultado ?: false // timeout → falha graceful
    } catch (e: Exception) {
        false
    }
}
