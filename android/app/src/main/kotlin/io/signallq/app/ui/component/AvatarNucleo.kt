package io.signallq.app.ui.component

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// GH#1078: nucleo compartilhado entre ProfileAvatarButton.kt e UserAvatar.kt -- antes cada um
// decodificava o bitmap do avatar e resolvia o fallback textual de forma independente (um caia em
// "L" hardcoded, o outro em "?"). Decisao: manter os dois composables separados (contratos
// diferentes -- botao fixo de topbar vs. avatar editavel de tamanho variavel com badge de camera),
// mas convergir aqui a logica que realmente era identica (decodificacao de URI e fallback).

internal suspend fun decodificarBitmapPerfil(
    context: Context,
    uriStr: String,
): ImageBitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver
                .openInputStream(uriStr.toUri())
                ?.use { stream -> BitmapFactory.decodeStream(stream)?.asImageBitmap() }
        }.getOrNull()
    }

internal fun inicialFallbackAvatar(nome: String?): String = nome?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
