package io.signallq.app.ui.relatorio

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import io.signallq.app.core.relatorio.exportarHtmlComoPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Renderer PDF ÚNICO do app consumidor — GH#1219.
 *
 * Substitui os dois geradores `PdfDocument`/`Canvas` (`ResultadoPdfGenerator` e o gerador
 * privado de `LaudoScreen`), que tinham paginação, truncamento de texto, disclaimer e
 * tratamento de métrica ausente divergentes entre si. Ambos os fluxos agora montam um
 * [RelatorioDiagnosticoSnapshot] a partir do seu próprio estado consistente e delegam pra
 * cá — o HTML é gerado por [RelatorioDiagnosticoHtmlBuilder] (pura, testável) e a
 * paginação real fica com `exportarHtmlComoPdf` (`:core:relatorio`, WebView).
 *
 * Não implementa (fora do escopo desta correção, ver pendências registradas na issue):
 * política de limpeza de PDFs temporários acumulados, e toggle de usuário pra incluir
 * dados sensíveis sem máscara (default é sempre mascarado — decisão de produto/design
 * ainda pendente pra liberar a opção).
 */
object RelatorioDiagnosticoExporter {
    suspend fun gerarECompartilhar(
        context: Context,
        snapshot: RelatorioDiagnosticoSnapshot,
        nomeArquivoPrefixo: String = "relatorio_signallq",
    ) {
        val uri =
            withContext(Dispatchers.IO) {
                val dir =
                    context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                        ?: context.cacheDir.resolve("relatorios").also { it.mkdirs() }
                val arquivo = File(dir, "${nomeArquivoPrefixo}_${System.currentTimeMillis()}.pdf")
                val html = RelatorioDiagnosticoHtmlBuilder.gerarHtml(snapshot)
                val sucesso = exportarHtmlComoPdf(html = html, arquivo = arquivo, context = context)
                if (!sucesso) throw IllegalStateException("Falha ao gerar PDF via WebView")
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo)
            }
        withContext(Dispatchers.Main) {
            val intent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(
                Intent.createChooser(intent, "Compartilhar relatório").also {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }
}
