package io.signallq.app.ui.ads

import androidx.compose.ui.graphics.Color
import com.google.android.gms.ads.nativead.NativeAd
import io.mockk.mockk
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

    @Test
    fun `mesma combinacao produz a mesma chave`() {
        // Sem isto, a chave mudaria a cada recomposição e a árvore de Views seria recriada a toa —
        // o que no AdMob significa re-registrar as role views a cada frame.
        assertEquals(
            ChaveViewAnuncioNativo(anuncio, claro, claroSecundario),
            ChaveViewAnuncioNativo(anuncio, claro, claroSecundario),
        )
    }

    @Test
    fun `trocar o tema muda a chave`() {
        // ESTE é o bug da #1699. Mutante que este teste mata: remover as cores da chave (voltar a
        // `key(nativeAd)`). O mesmo anúncio, com cores de tema diferentes, tem que produzir chaves
        // diferentes — senão o `factory` não reexecuta e as cores capturadas ficam presas.
        assertNotEquals(
            ChaveViewAnuncioNativo(anuncio, claro, claroSecundario),
            ChaveViewAnuncioNativo(anuncio, escuro, escuroSecundario),
        )
    }

    @Test
    fun `mudar so a cor primaria ja muda a chave`() {
        // Cobre o campo isoladamente: um mutante que incluísse só `corTextoSecundario` na chave
        // passaria pelo teste anterior (as duas mudam juntas na troca de tema) e falharia aqui.
        assertNotEquals(
            ChaveViewAnuncioNativo(anuncio, claro, claroSecundario),
            ChaveViewAnuncioNativo(anuncio, escuro, claroSecundario),
        )
    }

    @Test
    fun `mudar so a cor secundaria ja muda a chave`() {
        // Simétrico do anterior, pelo mesmo motivo.
        assertNotEquals(
            ChaveViewAnuncioNativo(anuncio, claro, claroSecundario),
            ChaveViewAnuncioNativo(anuncio, claro, escuroSecundario),
        )
    }

    @Test
    fun `anuncio novo muda a chave`() {
        // Preserva o comportamento que já existia antes da #1699 e que motivou o `key(nativeAd)`
        // original: sem recriar, o card mostraria o texto do anúncio anterior.
        assertNotEquals(
            ChaveViewAnuncioNativo(anuncio, claro, claroSecundario),
            ChaveViewAnuncioNativo(outroAnuncio, claro, claroSecundario),
        )
    }

    @Test
    fun `anuncio nulo e distinto de anuncio carregado`() {
        // A transição "sem anúncio -> anúncio carregado" também precisa reconstruir.
        assertNotEquals(
            ChaveViewAnuncioNativo(null, claro, claroSecundario),
            ChaveViewAnuncioNativo(anuncio, claro, claroSecundario),
        )
    }

    @Test
    fun `anuncio e tema mudando juntos muda a chave`() {
        // Caso combinado: anúncio novo carregando no mesmo frame de uma troca de tema.
        assertNotEquals(
            ChaveViewAnuncioNativo(anuncio, claro, claroSecundario),
            ChaveViewAnuncioNativo(outroAnuncio, escuro, escuroSecundario),
        )
    }
}
