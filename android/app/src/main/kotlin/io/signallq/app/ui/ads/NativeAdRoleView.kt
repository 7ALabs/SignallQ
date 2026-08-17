package io.signallq.app.ui.ads

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import com.google.android.gms.ads.nativead.NativeAd

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

/**
 * Chave de reconstrução da árvore de Views de um anúncio nativo — issue #1699.
 *
 * O `factory` do `AndroidView` captura, **no momento da criação**, tudo que usa: o anúncio (para
 * preencher headline/corpo/CTA) e as cores de texto. `AndroidView` só reexecuta o `factory` quando
 * a `key()` em volta dele muda — então tudo que o closure captura precisa estar nesta chave.
 *
 * Antes da #1699 a chave era só o anúncio, e faltava a metade das cores. O sintoma era **headline
 * preto sobre card preto**: o usuário trocava o tema com o app aberto, o conteúdo da `ComposeView`
 * filha não recompunha, e o texto ficava preso ao tema anterior. Não é cosmético — ninguém clica
 * num anúncio ilegível, e nada registra erro, então a receita cai sem gerar sinal.
 *
 * Só as duas cores, e não o [io.signallq.app.ui.LkTokens] inteiro: são os únicos campos que o
 * closure captura, e `LkTokens` tem dezenas, o que recriaria a árvore de Views à toa.
 *
 * Existe como tipo nomeado — em vez de `key(nativeAd, textPrimary, textSecondary)` solto nos três
 * componentes — para que a regra fique **testável**: verificar que a chave muda quando o tema muda
 * é uma asserção honesta sobre a decisão. Verificar que o `factory` do `AndroidView` reexecutou
 * seria teste do Compose, não do nosso código.
 */
data class ChaveViewAnuncioNativo(
    val anuncio: NativeAd?,
    val corTextoPrimario: Color,
    val corTextoSecundario: Color,
)
