@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package io.signallq.app.feature.history

import android.os.Bundle
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import java.io.File

/**
 * Helper para escrever o output de um [PrintDocumentAdapter] diretamente em um [File].
 *
 * Necessário porque [WebView.createPrintDocumentAdapter()] retorna um adapter que
 * precisa de um [ParcelFileDescriptor] — não escreve diretamente em OutputStream.
 *
 * minSdk do projeto = 24 — PrintDocumentAdapter disponível desde API 19.
 */
internal object PdfPrintHelper {

    /**
     * Aciona o ciclo completo do [PrintDocumentAdapter] e escreve o resultado em [arquivo].
     * Deve ser chamado na Main thread (WebView requer UI thread).
     *
     * @param adapter  Adapter obtido via WebView.createPrintDocumentAdapter()
     * @param arquivo  Arquivo de destino (deve existir ou seu pai deve existir)
     * @param callback Chamado com `true` em sucesso, `false` em falha
     */
    private const val TIMEOUT_MS = 10_000L

    fun imprimir(
        adapter: PrintDocumentAdapter,
        arquivo: File,
        callback: (Boolean) -> Unit,
    ) {
        val handler = Handler(Looper.getMainLooper())
        var respondeu = false

        // Dispara callback(false) se onWrite nunca completar dentro do timeout
        val timeoutRunnable = Runnable {
            if (!respondeu) {
                respondeu = true
                callback(false)
            }
        }
        handler.postDelayed(timeoutRunnable, TIMEOUT_MS)

        // Wrapper que garante: cancela timeout e chama callback uma única vez
        val callbackSeguro: (Boolean) -> Unit = { sucesso ->
            if (!respondeu) {
                respondeu = true
                handler.removeCallbacks(timeoutRunnable)
                callback(sucesso)
            }
        }

        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        adapter.onLayout(
            /* oldAttributes = */ null,
            /* newAttributes = */ printAttributes,
            /* cancellationSignal = */ CancellationSignal(),
            /* callback = */ object : PrintDocumentAdapter.LayoutResultCallback() {
                override fun onLayoutFinished(info: PrintDocumentInfo, changed: Boolean) {
                    try {
                        val fd = ParcelFileDescriptor.open(
                            arquivo,
                            ParcelFileDescriptor.MODE_READ_WRITE or
                                ParcelFileDescriptor.MODE_CREATE or
                                ParcelFileDescriptor.MODE_TRUNCATE,
                        )
                        adapter.onWrite(
                            /* pages = */ arrayOf(PageRange.ALL_PAGES),
                            /* destination = */ fd,
                            /* cancellationSignal = */ CancellationSignal(),
                            /* callback = */ object : PrintDocumentAdapter.WriteResultCallback() {
                                override fun onWriteFinished(pages: Array<out PageRange>) {
                                    fd.close()
                                    callbackSeguro(true)
                                }

                                override fun onWriteFailed(error: CharSequence?) {
                                    runCatching { fd.close() }
                                    callbackSeguro(false)
                                }

                                override fun onWriteCancelled() {
                                    runCatching { fd.close() }
                                    callbackSeguro(false)
                                }
                            },
                        )
                    } catch (e: Exception) {
                        callbackSeguro(false)
                    }
                }

                override fun onLayoutFailed(error: CharSequence?) {
                    callbackSeguro(false)
                }

                override fun onLayoutCancelled() {
                    callbackSeguro(false)
                }
            },
            /* extras = */ Bundle(),
        )
    }
}
