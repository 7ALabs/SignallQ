package io.signallq.app.ui.component.ads

import android.graphics.Canvas
import android.graphics.Path
import android.view.View
import android.widget.FrameLayout
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
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.ads.ChaveViewAnuncioNativo
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
 *
 * Issue #1356: o AdMob native ad validator (popup de debug "1 implementation issue
 * found") apontava [MediaView] ausente -- o SDK exige que todo `NativeAdView` que usa
 * criativo com imagem/video registre um `mediaView` (`nativeAdView.mediaView`), mesmo
 * quando o layout tambem exibe o icone do anunciante separadamente. Sem isso, o SDK
 * nao tem onde renderizar o asset principal do criativo e sinaliza a implementacao
 * como incompleta.
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
                .background(c.surfaceContainer)
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
                        contentDescription = "Fechar anúncio",
                        tint = c.textTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.height(LkSpacing.sm))

            // necessidade, recriando a arvore de Views a toa.
            // GH#1699 — a chave tem que conter TUDO que o `factory` captura no closure, senão o
            // valor congela no estado antigo sem erro nem log. O inventário por componente e o
            // porquê de cada campo estão no KDoc de `ChaveViewAnuncioNativo` — fonte única.
            key(ChaveViewAnuncioNativo(nativeAd, density, textPrimary, textSecondary)) {
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

                        // MediaView: asset principal do criativo (imagem/video) -- exigido pelo
                        // SDK sempre que o nativeAd tem midia (ver KDoc da funcao, issue #1356).
                        // Altura fixa em MEDIA_VIEW_MAX_HEIGHT (issue #1505) -- antes era derivada
                        // do aspectRatio do criativo sem teto, o que deixava a imagem gigante
                        // (~320dp) em criativos quase quadrados. O MediaView do SDK escala e
                        // enquadra o criativo preservando proporcao dentro dos bounds informados
                        // (sem distorcer), entao um teto fixo nao fere a politica do AdMob.
                        // Cantos arredondados via clip manual de Canvas (View Android, nao
                        // Composable) porque Modifier.clip nao se aplica a uma View interoperada
                        // dentro de LinearLayouts manuais. O clip vai no FrameLayout WRAPPER, nao
                        // na MediaView diretamente. Validado em device/emulador real (issue #1506
                        // follow-up) em tres rodadas ate achar a combinacao que realmente corta:
                        // 1) `clipToOutline`/`ViewOutlineProvider` (na MediaView e depois no
                        //    FrameLayout ao redor) -- nao cortava nada, cantos retos.
                        // 2) `dispatchDraw` sobrescrito com `Canvas.clipPath` -- tambem nao
                        //    cortava; `setBackgroundColor` de diagnostico confirmou que o proprio
                        //    background do FrameLayout desenhava SEM nenhum corte, porque
                        //    `View.draw()` desenha o background (Step 1) antes de chamar
                        //    `dispatchDraw()` (Step 4) -- o clip aplicado so dentro de
                        //    `dispatchDraw` nunca cobria o desenho inteiro da View.
                        // 3) Sobrescrever `draw(Canvas)` inteiro (nao so `dispatchDraw`), com
                        //    `clipPath` envolvendo `super.draw(canvas)` -- clipa background E
                        //    filhos juntos. So essa combinacao (`draw()` + `clipPath` +
                        //    `setLayerType(LAYER_TYPE_SOFTWARE, ...)`, esse ultimo pra garantir que
                        //    o clipPath seja honrado independente de peculiaridade de canvas
                        //    hardware-accelerated) cortou os 4 cantos de fato -- confirmado pixel a
                        //    pixel em screenshot real (curva mensuravel, nao só reta).
                        val mediaCornerRadiusPx = density.dpToPx(LkRadius.input).toFloat()
                        val mediaView =
                            MediaView(context).apply {
                                mediaContent = nativeAd.mediaContent
                                layoutParams =
                                    FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                            }
                        val mediaViewContainer =
                            object : FrameLayout(context) {
                                private val clipPath = Path()

                                override fun draw(canvas: Canvas) {
                                    clipPath.reset()
                                    clipPath.addRoundRect(
                                        0f,
                                        0f,
                                        width.toFloat(),
                                        height.toFloat(),
                                        mediaCornerRadiusPx,
                                        mediaCornerRadiusPx,
                                        Path.Direction.CW,
                                    )
                                    val saveCount = canvas.save()
                                    canvas.clipPath(clipPath)
                                    super.draw(canvas)
                                    canvas.restoreToCount(saveCount)
                                }
                            }.apply {
                                layoutParams =
                                    LinearLayout
                                        .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, density.dpToPx(MEDIA_VIEW_MAX_HEIGHT))
                                        .apply { topMargin = density.dpToPx(LkSpacing.sm) }
                                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                                addView(mediaView)
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
                                addView(mediaViewContainer)
                                addView(ctaComposeView)
                            }

                        NativeAdView(context).apply {
                            addView(root)
                            iconView = iconChip
                            headlineView = headlineComposeView
                            bodyView = bodyComposeView
                            this.mediaView = mediaView
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

// Teto fixo de altura do MediaView (issue #1505) -- evita que criativos quase quadrados
// dominem o card (era ~320dp sem teto, derivado do aspectRatio). Largura continua
// MATCH_PARENT; o SDK do AdMob escala/enquadra o criativo preservando proporcao dentro
// deste limite, sem distorcer.
private val MEDIA_VIEW_MAX_HEIGHT: Dp = 120.dp

private fun androidx.compose.ui.unit.Density.dpToPx(dp: Dp): Int = with(this) { dp.roundToPx() }
