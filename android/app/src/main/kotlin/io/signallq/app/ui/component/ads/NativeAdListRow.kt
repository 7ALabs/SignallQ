package io.signallq.app.ui.component.ads

import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.ads.buildRoleComposeView

/**
 * Linha de anuncio nativo dentro da propria lista de dispositivos conectados
 * (issue #555, feedback do Luiz em 2026-07-12 -- nunca na secao INFRAESTRUTURA).
 * Omitida por completo quando [nativeAd] e null.
 */
@Composable
fun NativeAdListRow(
    nativeAd: NativeAd?,
    source: NativeAdSource,
    modifier: Modifier = Modifier,
) {
    if (nativeAd == null) return
    val c = LocalLkTokens.current
    val density = LocalDensity.current
    val textPrimary = c.textPrimary
    val textSecondary = c.textSecondary

    key(nativeAd) {
        AndroidView(
            modifier = modifier.background(c.textTertiary.copy(alpha = 0.04f)),
            factory = { context ->
                val iconChip =
                    buildRoleComposeView(context) {
                        NativeAdIconChip(nativeAd = nativeAd, size = LIST_ICON_SIZE)
                    }.apply {
                        layoutParams = LinearLayout.LayoutParams(density.dpToPx(LIST_ICON_SIZE), density.dpToPx(LIST_ICON_SIZE))
                    }

                val headlineComposeView =
                    buildRoleComposeView(context) {
                        Text(
                            text = nativeAd.headline.orEmpty(),
                            fontSize = 14.sp,
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
                val textColumn =
                    LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams =
                            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                                marginStart = density.dpToPx(LkSpacing.md)
                                marginEnd = density.dpToPx(LkSpacing.md)
                            }
                        addView(headlineComposeView)
                        addView(bodyComposeView)
                    }

                val badgeComposeView =
                    buildRoleComposeView(context) {
                        AdBadge(source = source)
                    }

                val root =
                    LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        setPadding(
                            density.dpToPx(LkSpacing.lg),
                            density.dpToPx(13.dp),
                            density.dpToPx(LkSpacing.lg),
                            density.dpToPx(13.dp),
                        )
                        addView(iconChip)
                        addView(textColumn)
                        addView(badgeComposeView)
                    }

                NativeAdView(context).apply {
                    addView(root)
                    iconView = iconChip
                    headlineView = headlineComposeView
                    bodyView = bodyComposeView
                    // Mesmo padrao do prototipo: a linha inteira e clicavel (sem CTA
                    // textual separado, so o icone + badge de disclosure).
                    callToActionView = root
                    setNativeAd(nativeAd)
                }
            },
        )
    }
}

private val LIST_ICON_SIZE: Dp = 40.dp

private fun androidx.compose.ui.unit.Density.dpToPx(dp: Dp): Int = with(this) { dp.roundToPx() }
