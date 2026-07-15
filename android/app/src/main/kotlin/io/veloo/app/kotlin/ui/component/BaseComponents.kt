package io.signallq.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens

@Composable
fun LkSurfaceCard(
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalLkTokens.current
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.surfaceContainer)
                .then(
                    if (outlined) {
                        Modifier.border(1.dp, c.outlineVariant, RoundedCornerShape(LkRadius.card))
                    } else {
                        Modifier
                    },
                ).padding(LkSpacing.base),
        content = content,
    )
}

@Composable
fun LkSectionOverline(
    text: String,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = c.onSurfaceVariant,
    )
}

@Composable
fun LkPillBadge(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        modifier =
            modifier
                .clip(RoundedCornerShape(4.dp))
                .background(containerColor)
                .padding(horizontal = LkSpacing.sm, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = contentColor,
    )
}

@Composable
fun LkStatusDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
    )
}

@Composable
fun LkSheetSectionTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = c.onSurface,
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = c.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun LkInlineBulletText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Box(
            modifier =
                Modifier
                    .padding(top = 5.dp)
                    .size(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.onSurfaceVariant),
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant,
        )
    }
}

@Composable
fun LkInfoCallout(
    icon: ImageVector,
    text: String,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = c.onSurfaceVariant,
        )
    }
}

@Composable
fun LkNumberedStep(
    number: Int,
    text: String,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Box(
            modifier =
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(c.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = c.primary,
            )
        }
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = c.onSurfaceVariant,
        )
    }
}

@Composable
fun LkSheetInfoRow(
    label: String,
    value: String,
    valueColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = LkSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = c.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor ?: c.onSurface,
        )
    }
}

@Composable
fun LkSheetDivider(modifier: Modifier = Modifier) {
    val c = LocalLkTokens.current
    HorizontalDivider(modifier = modifier, color = c.outlineVariant, thickness = 1.dp)
}

@Composable
fun LkSheetFrame(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalLkTokens.current
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(topStart = LkRadius.sheet, topEnd = LkRadius.sheet))
                .background(c.surfaceContainerLow),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(c.outlineVariant),
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 32.dp),
            content = content,
        )
    }
}
