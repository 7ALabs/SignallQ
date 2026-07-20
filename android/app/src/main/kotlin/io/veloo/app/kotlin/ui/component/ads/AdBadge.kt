package io.signallq.app.ui.component.ads

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens

/**
 * Disclosure obrigatorio de anuncio nativo (Google native-ad UX guidelines) -- issue #555.
 *
 * Nunca omitido, nunca disfarcado de componente organico do app: icone + rotulo em
 * UPPERCASE dentro de um pill com contorno, mesmo padrao visual do protótipo da Lia
 * (`.claude/design-specs/2026-07-12-monetizacao-nativa-ads/ads.jsx`).
 */
@Composable
fun AdBadge(
    source: NativeAdSource,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val isPartner = source == NativeAdSource.PARTNER
    val isSimulated = source == NativeAdSource.SIMULATED
    val tone =
        when {
            isSimulated -> c.warning
            isPartner -> c.secondary
            else -> c.textTertiary
        }
    val label =
        when {
            isSimulated -> "SIMULADO"
            isPartner -> "Parceiro"
            else -> "Patrocinado"
        }

    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(LkRadius.pill))
                .border(BorderStroke(1.dp, c.border), RoundedCornerShape(LkRadius.pill))
                .padding(horizontal = LkSpacing.sm, vertical = LkSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector =
                when {
                    isSimulated -> Icons.Outlined.Campaign
                    isPartner -> Icons.Outlined.Storefront
                    else -> Icons.Outlined.Campaign
                },
            contentDescription = null,
            tint = tone,
            modifier = Modifier.size(12.dp).padding(end = LkSpacing.xs),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = tone,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
