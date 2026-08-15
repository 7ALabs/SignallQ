package io.signallq.app.ui.component.ads

import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.ads.buildRoleComposeView

/**
 * Linha compacta de anuncio nativo -- usada na tela de Velocidade (estado idle),
 * abaixo do card "Ultimo resultado" (issue #555). Omitida por completo quando
 * [nativeAd] e null.
 */
@Composable
fun NativeAdRow(
    nativeAd: NativeAd?,
    source: NativeAdSource,
    modifier: Modifier = Modifier,
) {
    if (nativeAd == null) return
    val c = LocalLkTokens.current
    val density = LocalDensity.current
    val textPrimary = c.textPrimary
    val textSecondary = c.textSecondary
    val textTertiary = c.textTertiary

    key(nativeAd) {
        AndroidView(
            modifier =
                modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(c.surfaceContainer)
                    .dashedBorder(color = c.border, cornerRadius = LkRadius.card),
            factory = { context ->
                val iconChip =
                    buildRoleComposeViewSized(context, density, ROW_ICON_SIZE) {
                        NativeAdIconChip(nativeAd = nativeAd, size = ROW_ICON_SIZE)
                    }

                val headlineBodyColumn =
                    LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams =
                            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                                marginStart = density.dpToPx(LkSpacing.md)
                                marginEnd = density.dpToPx(LkSpacing.sm)
                            }
                        addView(
                            buildRoleComposeView(context) {
                                AdBadge(source = source, modifier = Modifier.padding(bottom = 5.dp))
                            },
                        )
                    }

                val headlineComposeView =
                    buildRoleComposeView(context) {
                        Text(
                            text = nativeAd.headline.orEmpty(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                val bodyComposeView =
                    buildRoleComposeView(context) {
                        Text(
                            text = nativeAd.body.orEmpty(),
                            fontSize = 11.sp,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                headlineBodyColumn.addView(headlineComposeView)
                headlineBodyColumn.addView(bodyComposeView)

                val chevronComposeView =
                    buildRoleComposeView(context) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = nativeAd.callToAction ?: "Ver oferta",
                            tint = textTertiary,
                        )
                    }

                val root =
                    LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(density.dpToPx(14.dp), density.dpToPx(14.dp), density.dpToPx(14.dp), density.dpToPx(14.dp))
                        addView(iconChip)
                        addView(headlineBodyColumn)
                        addView(chevronComposeView)
                    }

                NativeAdView(context).apply {
                    addView(root)
                    iconView = iconChip
                    headlineView = headlineComposeView
                    bodyView = bodyComposeView
                    // Linha inteira funciona como CTA (mesmo comportamento do prototipo:
                    // `onClick` no container inteiro, sem botao separado) -- registra o
                    // root como callToActionView alem dos roles individuais.
                    callToActionView = root
                    setNativeAd(nativeAd)
                }
            },
        )
    }
}

private val ROW_ICON_SIZE: Dp = 38.dp

private fun androidx.compose.ui.unit.Density.dpToPx(dp: Dp): Int = with(this) { dp.roundToPx() }

private fun buildRoleComposeViewSized(
    context: android.content.Context,
    density: androidx.compose.ui.unit.Density,
    size: Dp,
    content: @Composable () -> Unit,
) = buildRoleComposeView(context, content).apply {
    layoutParams = LinearLayout.LayoutParams(density.dpToPx(size), density.dpToPx(size))
}
