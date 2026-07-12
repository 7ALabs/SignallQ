package io.signallq.app.ui.component.ads

import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
 * Card cheio de anuncio nativo -- usado em Resultado do diagnostico e Historico
 * (issue #555). Omitido por completo quando [nativeAd] e null (fetch ainda nao
 * completou, Remote Config desligado ou falha de carregamento) -- nunca renderiza
 * placeholder/caixa vazia, o layout ao redor recompoe sem buraco.
 *
 * Headline/body/CTA vem do proprio [NativeAd] carregado do AdMob (criativo real
 * servido pelo ad network) -- nunca texto hardcoded aqui, isso violaria a politica
 * de anuncio nativo do AdMob.
 */
@Composable
fun NativeAdCard(
    nativeAd: NativeAd?,
    source: NativeAdSource,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (nativeAd == null) return
    val c = LocalLkTokens.current
    val density = LocalDensity.current
    val textPrimary = c.textPrimary
    val textSecondary = c.textSecondary

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .dashedBorder(color = c.border, cornerRadius = LkRadius.card)
                .padding(LkSpacing.lg),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AdBadge(source = source)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dispensar anuncio",
                        tint = c.textTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(LkSpacing.sm))

            // key(nativeAd): forca o AndroidView a reconstruir a arvore de Views do zero
            // quando um NativeAd novo carrega -- o factory captura headline/body/cta no
            // closure em tempo de criacao, entao precisa recriar (nao so re-`update`) a
            // cada anuncio novo para nunca mostrar texto de um anuncio anterior.
            key(nativeAd) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        val iconChip =
                            buildRoleComposeView(context) {
                                NativeAdIconChip(nativeAd = nativeAd, size = ICON_SIZE)
                            }.apply {
                                layoutParams = LinearLayout.LayoutParams(density.dpToPx(ICON_SIZE), density.dpToPx(ICON_SIZE))
                            }

                        val headlineComposeView =
                            buildRoleComposeView(context) {
                                Text(
                                    text = nativeAd.headline.orEmpty(),
                                    fontSize = 14.sp,
                                    lineHeight = 19.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary,
                                )
                            }
                        val bodyComposeView =
                            buildRoleComposeView(context) {
                                Text(
                                    text = nativeAd.body.orEmpty(),
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = textSecondary,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                        val textColumn =
                            LinearLayout(context).apply {
                                orientation = LinearLayout.VERTICAL
                                layoutParams =
                                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                                        marginStart = density.dpToPx(LkSpacing.md)
                                    }
                                addView(headlineComposeView)
                                addView(bodyComposeView)
                            }

                        val topRow =
                            LinearLayout(context).apply {
                                orientation = LinearLayout.HORIZONTAL
                                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                                addView(iconChip)
                                addView(textColumn)
                            }

                        val ctaComposeView =
                            buildRoleComposeView(context) {
                                NativeAdCtaButton(label = nativeAd.callToAction ?: "Ver oferta")
                            }.apply {
                                layoutParams =
                                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                        topMargin = density.dpToPx(LkSpacing.md)
                                    }
                            }

                        val root =
                            LinearLayout(context).apply {
                                orientation = LinearLayout.VERTICAL
                                addView(topRow)
                                addView(ctaComposeView)
                            }

                        NativeAdView(context).apply {
                            addView(root)
                            iconView = iconChip
                            headlineView = headlineComposeView
                            bodyView = bodyComposeView
                            callToActionView = ctaComposeView
                            setNativeAd(nativeAd)
                        }
                    },
                )
            }
        }
    }
}

private val ICON_SIZE: Dp = 44.dp

private fun androidx.compose.ui.unit.Density.dpToPx(dp: Dp): Int = with(this) { dp.roundToPx() }
