package io.signallq.app.ui.ads

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView

/**
 * Cria um [ComposeView] com conteudo Compose normal (tokens LK, mesma fonte/cor de
 * qualquer outra tela do app) para ser registrado como um "role view" do
 * [com.google.android.gms.ads.nativead.NativeAdView] (headline/body/icon/CTA) --
 * issue #555.
 *
 * O AdMob exige que o headline, corpo, icone e CTA do anuncio nativo estejam
 * registrados como Views reais dentro do NativeAdView (rastreio de impressao/clique
 * e exigencia de politica) -- um ComposeView e uma View Android valida, entao a
 * pintura pode continuar 100% Compose sem violar essa exigencia.
 */
fun buildRoleComposeView(
    context: Context,
    content: @Composable () -> Unit,
): ComposeView =
    ComposeView(context).apply {
        setContent(content)
    }
