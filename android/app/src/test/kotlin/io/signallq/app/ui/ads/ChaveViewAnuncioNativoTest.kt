package io.signallq.app.ui.ads

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import com.google.android.gms.ads.nativead.NativeAd
import io.mockk.mockk
import io.signallq.app.ui.component.ads.NativeAdSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Chave de reconstrução da árvore de Views do anúncio nativo — issue #1699.
 *
 * O defeito corrigido: `NativeAdCard`/`NativeAdRow`/`NativeAdListRow` chaveavam o `AndroidView`
 * só pelo anúncio, mas o `factory` captura **anúncio e cores** no closure. Trocar o tema com o app
 * aberto deixava o texto preso ao tema anterior — headline preto sobre card preto. Não é
 * cosmético: ninguém clica num anúncio ilegível, e nada registra erro, então a receita cai sem
 * gerar sinal.
 *
 * **O que estes testes cobrem, e o que deliberadamente não cobrem.** Eles asseguram que a *chave*
 * distingue os estados que precisam reconstruir a árvore. Não asseguram que o `AndroidView`
 * reexecuta o `factory` quando a chave muda — isso é contrato do Compose, e testá-lo aqui seria
 * testar o framework, não este código. A ligação entre as duas coisas é a linha
 * `key(ChaveViewAnuncioNativo(...))` nos três componentes, e essa é verificação de revisão.
 */
class ChaveViewAnuncioNativoTest {
    private val claro = Color(0xFF111111)
    private val claroSecundario = Color(0xFF666666)
    private val escuro = Color(0xFFFFFFFF)
    private val escuroSecundario = Color(0xFFAAAAAA)

    private val anuncio: NativeAd = mockk(relaxed = true)
    private val outroAnuncio: NativeAd = mockk(relaxed = true)
    private val densidade = Density(density = 2f, fontScale = 1f)

    /** Chave com os valores neutros; cada teste varia só o campo que está exercitando. */
    private fun chave(
        anuncio: NativeAd? = this.anuncio,
        densidade: Density = this.densidade,
        primaria: Color = claro,
        secundaria: Color = claroSecundario,
        terciaria: Color? = null,
        origem: NativeAdSource? = null,
    ) = ChaveViewAnuncioNativo(anuncio, densidade, primaria, secundaria, terciaria, origem)

    @Test
    fun `mesma combinacao produz a mesma chave`() {
        // Sem isto, a chave mudaria a cada recomposição e a árvore de Views seria recriada a toa —
        // o que no AdMob significa re-registrar as role views a cada frame.
        assertEquals(
            chave(),
            chave(),
        )
    }

    @Test
    fun `trocar o tema muda a chave`() {
        // ESTE é o bug da #1699. Mutante que este teste mata: remover as cores da chave (voltar a
        // `key(nativeAd)`). O mesmo anúncio, com cores de tema diferentes, tem que produzir chaves
        // diferentes — senão o `factory` não reexecuta e as cores capturadas ficam presas.
        assertNotEquals(
            chave(),
            chave(primaria = escuro, secundaria = escuroSecundario),
        )
    }

    @Test
    fun `mudar so a cor primaria ja muda a chave`() {
        // Cobre o campo isoladamente: um mutante que incluísse só `corTextoSecundario` na chave
        // passaria pelo teste anterior (as duas mudam juntas na troca de tema) e falharia aqui.
        assertNotEquals(
            chave(),
            chave(primaria = escuro),
        )
    }

    @Test
    fun `mudar so a cor secundaria ja muda a chave`() {
        // Simétrico do anterior, pelo mesmo motivo.
        assertNotEquals(
            chave(),
            chave(secundaria = escuroSecundario),
        )
    }

    @Test
    fun `anuncio novo muda a chave`() {
        // Preserva o comportamento que já existia antes da #1699 e que motivou o `key(nativeAd)`
        // original: sem recriar, o card mostraria o texto do anúncio anterior.
        assertNotEquals(
            chave(),
            chave(anuncio = outroAnuncio),
        )
    }

    @Test
    fun `anuncio nulo e distinto de anuncio carregado`() {
        // A transição "sem anúncio -> anúncio carregado" também precisa reconstruir.
        assertNotEquals(
            chave(anuncio = null),
            chave(),
        )
    }

    @Test
    fun `anuncio e tema mudando juntos muda a chave`() {
        // Caso combinado: anúncio novo carregando no mesmo frame de uma troca de tema.
        assertNotEquals(
            chave(),
            chave(anuncio = outroAnuncio, primaria = escuro, secundaria = escuroSecundario),
        )
    }

    // ─── Campos que faltavam na primeira versão (achado de Caio, PR #1716) ─────

    @Test
    fun `mudar a densidade muda a chave`() {
        // `density` é capturado nos TRÊS factories e não estava na chave. E o gatilho é real:
        // `AndroidManifest.xml:50` declara `density` em `configChanges`, então mexer em
        // Configurações > Tela > Tamanho da exibição NÃO recria a Activity — recompõe. Sem isto,
        // todo `layoutParams` em px fica calculado na densidade antiga.
        assertNotEquals(chave(), chave(densidade = Density(density = 3f, fontScale = 1f)))
    }

    @Test
    fun `mudar so a escala de fonte muda a chave`() {
        // `Density` carrega densidade E fontScale; `fontScale` também está em `configChanges`.
        // Um mutante que comparasse só `density.density` passaria pelo teste anterior.
        assertNotEquals(chave(), chave(densidade = Density(density = 2f, fontScale = 1.3f)))
    }

    @Test
    fun `mudar a cor terciaria muda a chave`() {
        // Capturada só por `NativeAdRow` (tint do chevron), e ausente da primeira versão da chave.
        // Hoje o defeito está latente porque `SignallQTheme` define secundária e terciária como o
        // mesmo `onSurfaceVariant` — as duas mudam juntas. Quando o Design System 2.0 diferenciar
        // as duas, o tint do chevron regrediria em silêncio sem este campo.
        assertNotEquals(chave(terciaria = claroSecundario), chave(terciaria = escuroSecundario))
    }

    @Test
    fun `mudar a origem do anuncio muda a chave`() {
        // `source` alimenta o `AdBadge`, que é o disclosure OBRIGATÓRIO do AdMob
        // ("Patrocinado"/"Parceiro"/"Simulado"). Constante nos 5 call sites hoje, então está
        // latente — mas badge velho deixa de ser estética e vira política se um dia vier de estado.
        assertNotEquals(
            chave(origem = NativeAdSource.ADMOB),
            chave(origem = NativeAdSource.SIMULATED),
        )
    }

    @Test
    fun `campos opcionais nulos nao quebram a igualdade`() {
        // `NativeAdCard` não captura terciária nem origem, então passa null nos dois. Duas chaves
        // idênticas com nulos precisam continuar iguais — senão o card recriaria a árvore de Views
        // a cada recomposição.
        assertEquals(chave(terciaria = null, origem = null), chave(terciaria = null, origem = null))
    }
}
