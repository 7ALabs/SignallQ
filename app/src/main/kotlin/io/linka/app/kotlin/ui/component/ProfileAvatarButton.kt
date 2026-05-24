package io.linka.app.kotlin.ui.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.linka.app.kotlin.ui.LkColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ProfileAvatarButton(
    nomeUsuario: String,
    fotoUri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val fotoBitmap: ImageBitmap? by produceState<ImageBitmap?>(initialValue = null, key1 = fotoUri) {
        value =
            fotoUri?.let { uriStr ->
                withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver
                            .openInputStream(uriStr.toUri())
                            ?.use { stream -> BitmapFactory.decodeStream(stream)?.asImageBitmap() }
                    }.getOrNull()
                }
            }
    }
    val profileBrush = Brush.linearGradient(colors = listOf(LkColors.accent, LkColors.accentBlue))

    Box(
        modifier =
            modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(brush = profileBrush)
                .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = fotoBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().clip(CircleShape),
            )
        } else {
            Text(
                text = nomeUsuario.firstOrNull()?.uppercaseChar()?.toString() ?: "L",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W700,
                color = LkColors.linkaTextOnDark,
            )
        }
    }
}
