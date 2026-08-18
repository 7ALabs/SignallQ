package io.signallq.app.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * O sinal contextual enviado ao AdMob — issue #555, reduzido pela #1703/#1717.
 *
 * A suíte anterior cobria a **sanitização** dos marcadores de diagnóstico: garantia de que SSID,
 * BSSID, IP, MAC e afins nunca sobreviveriam como `neighboringContentUrls`. Ela cumpria o papel, e
 * o que mudou não foi a qualidade dela — foi a decisão de não enviar marcador nenhum (GH#1717,
 * decisão de Luiz em 2026-08-17), depois de a própria PR mostrar que o mecanismo estava inerte:
 * as URLs são sintéticas e `signallq.app` sequer é domínio registrado.
 *
 * Com os marcadores fora, a garantia fica mais forte e mais barata de provar: o sinal é **função
 * apenas do slot**, e slot é fixo por tela.
 *
 * A primeira versão deste KDoc dizia que "não existe caminho por onde dado de usuário entre" — e
 * era falso, porque o  tinha construtor público e qualquer call site podia montar a URL
 * à mão (bloqueio B8 de Caio na PR #1717, provado com SSID e IP passando pela suíte inteira). O
 * construtor virou privado e `forSlot` é a única entrada, então agora a frase se sustenta: o
 * caminho não é filtrado, ele não compila.
 *
 * Os payloads realistas de dado de device continuam aqui, agora asserindo o invariante novo: por
 * mais que a tela saiba deles, nada disso alcança a URL.
 */
class NativeAdContentSignalsTest {
    @Test
    fun `cada slot tem um topico distinto`() {
        val urls = AdSlot.entries.map { NativeAdContentSignal.forSlot(it).contentUrl }

        assertEquals("topicos repetidos entre slots", urls.size, urls.distinct().size)
        urls.forEach { assertTrue(it.startsWith("https://signallq.app/contexto-anuncio/")) }
    }

    // Mutante que este teste mata: fazer o sinal depender de qualquer coisa além do slot. Com a
    // assinatura atual isso nem compila — e essa é a proteção. O teste registra o invariante para
    // quem for tentado a reintroduzir o parâmetro: a política publicada declara que apenas o
    // tópico da tela é enviado, então mexer aqui exige mexer nela antes.
    @Test
    fun `o sinal e funcao apenas do slot`() {
        AdSlot.entries.forEach { slot ->
            assertEquals(
                "chamadas repetidas para o mesmo slot precisam dar o mesmo sinal",
                NativeAdContentSignal.forSlot(slot).contentUrl,
                NativeAdContentSignal.forSlot(slot).contentUrl,
            )
        }
    }

    @Test
    fun `nenhum dado de device alcanca a url do anuncio`() {
        // Payloads realistas do que a tela de fato conhece. Antes da #1717 eles eram descartados
        // por sanitização; agora não há por onde entrarem.
        val valoresCrus =
            listOf(
                "minhacasa5g",
                "aa:bb:cc:dd:ee:ff",
                "192.168.1.10",
                "00:11:22:33:44:55",
                "123456789012345",
                "-23.5,-46.6",
                // Conclusões de diagnóstico — o que saía como `neighboringContentUrls` até a #1717.
                "wifi-fraco",
                "velocidade-abaixo-do-contratado",
                "bufferbloat-alto",
            )

        val tudo = AdSlot.entries.joinToString(" ") { NativeAdContentSignal.forSlot(it).contentUrl }.lowercase()

        valoresCrus.forEach { valor ->
            assertFalse("sinal do anuncio nao pode conter '$valor': $tudo", tudo.contains(valor))
        }
    }
}
