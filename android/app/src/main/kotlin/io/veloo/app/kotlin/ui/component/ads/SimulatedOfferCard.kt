package io.signallq.app.ui.component.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import io.signallq.app.ui.component.LkSurfaceCard

/**
 * Placeholder visual do slot de monetizacao enquanto o AdMob real nao esta ligado.
 * TODO: substituir estes componentes simulados pelos componentes reais do AdMob.
 */
@Composable
fun SimulatedOfferCard(
    title: String,
    body: String,
    cta: String,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current

    LkSurfaceCard(modifier = modifier.fillMaxWidth(), outlined = true) {
        AdBadge(source = NativeAdSource.SIMULATED)
        Spacer(Modifier.height(LkSpacing.sm))
        Row(
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(c.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Router,
                    contentDescription = null,
                    tint = c.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                )
                Spacer(Modifier.height(LkSpacing.xs))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(LkSpacing.md))
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.card),
            colors = ButtonDefaults.buttonColors(containerColor = c.primary),
        ) {
            Text(text = cta, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W600)
        }
    }
}

@Composable
fun SimulatedOfferRow(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current

    LkSurfaceCard(modifier = modifier.fillMaxWidth(), outlined = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(c.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Campaign,
                    contentDescription = null,
                    tint = c.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = LkSpacing.md, end = LkSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
            ) {
                AdBadge(source = NativeAdSource.SIMULATED)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = c.textTertiary,
            )
        }
    }
}

@Composable
fun SimulatedOfferListRow(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current

    LkSurfaceCard(
        modifier = modifier.fillMaxWidth().clickable { },
        outlined = true,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(c.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Router,
                    contentDescription = null,
                    tint = c.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = LkSpacing.md)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AdBadge(source = NativeAdSource.SIMULATED)
        }
    }
}
