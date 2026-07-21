package io.signallq.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * GH#1226 item E — ponto único pra abrir WhatsApp/discador/navegador/Play Store a partir da
 * tela Operadoras. Antes, cada botão chamava `context.startActivity()` direto, sem tratar
 * `ActivityNotFoundException` (WhatsApp não instalado, sem navegador, sem Play Store) nem
 * URI inválida — qualquer um desses cenários derrubava o app. Agora sempre protegido, com
 * feedback amigável via Toast em vez de crash.
 */
object ExternalActionLauncher {
    /** Abre uma URI genérica (site, WhatsApp, market://) via `ACTION_VIEW`. */
    fun abrirView(
        context: Context,
        uri: String?,
    ): Boolean = abrir(context, uri) { parsed -> Intent(Intent.ACTION_VIEW, parsed) }

    /** Abre o discador (não liga direto) com o telefone informado, via `ACTION_DIAL`. */
    fun abrirDiscador(
        context: Context,
        telefone: String?,
    ): Boolean = abrir(context, telefone?.let { "tel:$it" }) { parsed -> Intent(Intent.ACTION_DIAL, parsed) }

    private fun abrir(
        context: Context,
        uri: String?,
        criarIntent: (Uri) -> Intent,
    ): Boolean {
        if (uri.isNullOrBlank()) return false
        return try {
            val parsed = runCatching { Uri.parse(uri) }.getOrNull()
            if (parsed == null) {
                Timber.w("ExternalActionLauncher: URI invalida -- $uri")
                mostrarErro(context, "Link inválido.")
                return false
            }
            context.startActivity(criarIntent(parsed))
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: ActivityNotFoundException) {
            Timber.w(e, "ExternalActionLauncher: nenhum app compativel para $uri")
            mostrarErro(context, "Nenhum aplicativo disponível para essa ação.")
            false
        } catch (e: Throwable) {
            Timber.w(e, "ExternalActionLauncher: falha ao abrir $uri")
            mostrarErro(context, "Não foi possível abrir esse link.")
            false
        }
    }

    private fun mostrarErro(
        context: Context,
        mensagem: String,
    ) {
        Toast.makeText(context, mensagem, Toast.LENGTH_SHORT).show()
    }
}
