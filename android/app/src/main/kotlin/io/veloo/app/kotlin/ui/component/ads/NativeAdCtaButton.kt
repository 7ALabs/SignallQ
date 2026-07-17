package io.signallq.app.ui.component.ads

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LocalLkTokens

/**
 * CTA do anuncio nativo -- registrado como `callToActionView` do NativeAdView,
 * entao o clique e tratado inteiramente pelo SDK do AdMob (registra o clique e
 * abre o destino do anunciante). Nao tem `onClick` proprio de proposito.
 *
 * Nunca usa o violeta solido de marca (reservado para CTA primario organico do
 * app -- Iniciar teste, Conversar com IA): contorno accent, fundo transparente,
 * mesma regra visual do prototipo da Lia.
 */
@Composable
fun NativeAdCtaButton(label: String) {
    val c = LocalLkTokens.current
    Text(
        text = label,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.button))
                .border(BorderStroke(1.dp, c.primary.copy(alpha = 0.35f)), RoundedCornerShape(LkRadius.button))
                .padding(vertical = 11.dp),
        textAlign = TextAlign.Center,
        color = c.primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
    )
}
