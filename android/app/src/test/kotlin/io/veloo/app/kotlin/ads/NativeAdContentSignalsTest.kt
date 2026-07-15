package io.signallq.app.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Issue #555 -- garante que o sinal contextual enviado ao AdMob nunca carrega dado
 * pessoal/de device, mesmo que um chamador futuro tente passar algo assim.
 */
class NativeAdContentSignalsTest {
    @Test
    fun `cada slot tem um topico distinto sem tags`() {
        val urls = AdSlot.entries.map { NativeAdContentSignals.forSlot(it).contentUrl }
        assertEquals(urls.size, urls.distinct().size)
        urls.forEach { assertTrue(it.startsWith("https://signallq.app/contexto-anuncio/")) }
    }

    @Test
    fun `tags validas viram neighboring content urls`() {
        val signal = NativeAdContentSignals.forSlot(AdSlot.RESULTADO, setOf("wifi_fraco", "bufferbloat_alto"))
        assertEquals(2, signal.neighboringContentUrls.size)
        signal.neighboringContentUrls.forEach { assertTrue(it.contains("resultado-teste")) }
    }

    @Test
    fun `nunca inclui SSID BSSID IP MAC ou identificador de device`() {
        // Payloads realistas de dado de device -- todos tem caractere fora de [a-z0-9-]
        // (":", ".", ",", espaco), entao a sanitizacao os descarta inteiros; nenhum
        // deveria sobreviver como neighboring content url.
        val tagsProibidas =
            setOf(
                "ssid:MinhaCasa5G",
                "bssid:AA:BB:CC:DD:EE:FF",
                "ip:192.168.1.10",
                "mac:00:11:22:33:44:55",
                "imei:123456789012345",
                "imsi:310150123456789",
                "deviceId:abc-123",
                "GPS -23.5,-46.6",
            )
        val signal = NativeAdContentSignals.forSlot(AdSlot.HISTORICO, tagsProibidas)

        assertTrue(
            "nenhum payload de device deveria sobreviver a sanitizacao: ${signal.neighboringContentUrls}",
            signal.neighboringContentUrls.isEmpty(),
        )
        val valoresCrus = listOf("minhacasa5g", "aa:bb:cc:dd:ee:ff", "192.168.1.10", "123456789012345", "-23.5,-46.6")
        val tudo = (listOf(signal.contentUrl) + signal.neighboringContentUrls).joinToString(" ").lowercase()
        valoresCrus.forEach { valor ->
            assertFalse("sinal do anuncio nao pode conter o valor cru '$valor': $tudo", tudo.contains(valor))
        }
    }

    @Test
    fun `limita a no maximo 3 tags vizinhas`() {
        val muitasTags = (1..10).map { "tag-$it" }.toSet()
        val signal = NativeAdContentSignals.forSlot(AdSlot.VELOCIDADE, muitasTags)
        assertTrue(signal.neighboringContentUrls.size <= 3)
    }

    @Test
    fun `tags com espaco ou pontuacao sao descartadas`() {
        val signal = NativeAdContentSignals.forSlot(AdSlot.DISPOSITIVOS, setOf("tag valida-1", "tag com espaco", "tag:com:dois:pontos"))
        assertTrue(signal.neighboringContentUrls.isEmpty())
    }

    @Test
    fun `underscore e normalizado para hifen -- vocabulario real do DiagnosticTag`() {
        val signal = NativeAdContentSignals.forSlot(AdSlot.RESULTADO, setOf("wifi_fraco"))
        assertEquals(1, signal.neighboringContentUrls.size)
        assertTrue(signal.neighboringContentUrls.first().endsWith("wifi-fraco"))
    }
}
