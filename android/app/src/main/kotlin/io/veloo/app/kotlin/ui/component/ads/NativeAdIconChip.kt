package io.signallq.app.ui.component.ads

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LocalLkTokens

/**
 * Icone/logo do anunciante -- vem de [NativeAd.getIcon] quando o criativo servido
 * pelo AdMob traz esse asset. Alguns anuncios nativos nao trazem icone; nesse caso
 * cai num icone generico neutro, NUNCA numa letra-marca inventada (isso seria
 * simular um anunciante que nao existe -- o prototipo da Lia usa letra so como
 * placeholder de design, aqui o dado tem que ser real).
 */
@Composable
fun NativeAdIconChip(
    nativeAd: NativeAd,
    size: Dp,
) {
    val c = LocalLkTokens.current
    val icon = nativeAd.icon
    // Raio de canto proporcional ao tamanho do chip (~27%, mesma proporcao do card
    // cheio do prototipo: 12dp em 44dp) -- escala corretamente nas variantes menores
    // (row/list row) sem precisar de um valor fixo por variante.
    val cornerRadius = size * 0.27f

    Box(
        modifier =
            Modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
                .background(if (icon == null) LkColors.accentBlue.copy(alpha = 0.14f) else c.bgSecondary),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setImageDrawable(icon.drawable)
                    }
                },
                modifier = Modifier.size(size),
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Campaign,
                contentDescription = null,
                tint = LkColors.accentBlue,
                modifier = Modifier.size(size / 2),
            )
        }
    }
}
