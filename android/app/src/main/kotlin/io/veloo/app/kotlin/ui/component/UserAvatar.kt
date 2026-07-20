package io.signallq.app.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LocalLkTokens

// GH#936: extraido originalmente de PerfilEditSheet.kt (existia como `internal fun UserAvatar` la
// dentro). GH#1078: movido para ui/component/ porque ja era consumido por dois arquivos de tela
// (AjustesScreen.kt e PerfilEditSheet.kt) -- nao era um componente exclusivo de uma unica tela.
// Fallback ("?") e estilo (gradiente) convergidos com ProfileAvatarButton.kt via o nucleo
// compartilhado em AvatarNucleo.kt. Decodificacao de bitmap tambem passou a ser assincrona
// (Dispatchers.IO), antes rodava sincrona na main thread dentro de um `remember`.

@Composable
internal fun UserAvatar(
    fotoUri: String?,
    fallbackInitial: Char?,
    size: Dp,
    onClick: (() -> Unit)? = null,
) {
    val c = LocalLkTokens.current
    val context = LocalContext.current
    val bitmap: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, key1 = fotoUri) {
        value = fotoUri?.let { uriStr -> decodificarBitmapPerfil(context, uriStr) }
    }
    val profileBrush = Brush.linearGradient(colors = listOf(c.primary, c.secondary))
    Box(
        modifier =
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(brush = profileBrush)
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val bitmapAtual = bitmap
        if (bitmapAtual != null) {
            Image(
                bitmap = bitmapAtual,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
            )
        } else {
            Text(
                text = fallbackInitial?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.W700,
                color = LkColors.signallQTextOnDark,
            )
        }
        if (onClick != null) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(2.dp, MaterialTheme.colorScheme.surfaceContainerLow, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
