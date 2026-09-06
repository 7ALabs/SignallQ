package io.signallq.app.ui.ads

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.Density
import com.google.android.gms.ads.nativead.NativeAd
import io.signallq.app.ui.component.ads.NativeAdSource

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
 * O `factory` do `AndroidView` captura, **no momento da criação**, tudo que usa. `AndroidView` só
 * reexecuta o `factory` quando a `key()` em volta dele muda — então **tudo que o closure captura
 * precisa estar aqui**. Campo capturado e ausente da chave = valor congelado no estado antigo,
 * sem erro, sem log.
 *
 * Antes da #1699 a chave era só o anúncio. O sintoma era **headline preto sobre card preto**: o
 * usuário trocava o tema com o app aberto, o conteúdo da `ComposeView` filha não recompunha, e o
 * texto ficava preso ao tema anterior. Não é cosmético — ninguém clica num anúncio ilegível, e
 * nada registra erro, então a receita cai sem gerar sinal.
 *
 * ## O inventário, medido e não presumido
 *
 * A primeira versão desta correção incluiu só as duas cores de texto e o KDoc afirmava que eram
 * "os únicos campos que o closure captura". **Era falso** — achado de Caio na revisão da PR #1716.
 * Varrendo os três `factory`:
 *
 * | Componente | Captura |
 * |---|---|
 * | `NativeAdCard` | anúncio, `density`, primária, secundária |
 * | `NativeAdRow` | anúncio, `density`, `source`, primária, secundária, **terciária** |
 * | `NativeAdListRow` | anúncio, `density`, `source`, primária, secundária |
 *
 * **Chave uniforme, e nenhum campo com valor padrão.** Uma versão intermediária desta correção
 * deixava `corTextoTerciario` e `origem` como `null` por padrão, para cada componente passar só o
 * que captura. Parecia economia e **é zero**, medido: `origem` é a constante `NativeAdSource.ADMOB`
 * nos cinco call sites, e `corTextoTerciario` é o mesmo `onSurfaceVariant` da secundária
 * (`SignallQTheme.kt:85-86`) — mesmo depois do Design System 2.0 separar as duas, terciária só
 * muda em troca de tema, quando primária e secundária já mudaram. Zero recriação a mais nos dois
 * casos.
 *
 * O custo do valor padrão, esse é concreto: com ele, esquecer um campo capturado **compila em
 * silêncio** — que é a forma exata do bug da #1699, reintroduzida como afordância de API. Sem ele,
 * omitir vira erro de construtor. Achado de Caio na rodada 2 da PR #1716: eu tinha essa garantia
 * na mão e a entreguei ao default, três parágrafos depois de escrever aqui por que ela importa.
 *
 * ## Por que `density` importa, apesar de parecer improvável
 *
 * `AndroidManifest.xml:50` declara `density` em `configChanges`. A Activity **não** é recriada
 * quando o usuário mexe em Configurações → Tela → Tamanho da exibição: recompõe. Sem `density` na
 * chave, todo `layoutParams` em px — chip do ícone, margens, altura do `MediaView` — fica
 * calculado na densidade antiga. Gatilho raro, defeito real, mesma classe do bug original.
 *
 * ## Por que `source` importa, mesmo sendo constante hoje
 *
 * Os cinco call sites passam `NativeAdSource.ADMOB` fixo, então não há como ficar velho agora. Mas
 * `AdBadge` renderiza o **disclosure obrigatório** do AdMob ("Patrocinado"/"Parceiro"/"Simulado").
 * Se um dia o `source` vier de estado, badge velho deixa de ser estética e vira política.
 *
 * ## O que este tipo NÃO garante
 *
 * Não impede que alguém reverta um call site para `key(nativeAd)` — `key()` é `vararg keys: Any?`
 * e aceita qualquer coisa, então isso **compila**. A primeira versão desta PR afirmava o
 * contrário; Caio testou e derrubou. O tipo serve para tornar a regra testável e para concentrar o
 * inventário num lugar só, não para o compilador cobrar.
 */
data class ChaveViewAnuncioNativo(
    val anuncio: NativeAd?,
    val densidade: Density,
    val corTextoPrimario: Color,
    val corTextoSecundario: Color,
    val corTextoTerciario: Color,
    val origem: NativeAdSource,
)
