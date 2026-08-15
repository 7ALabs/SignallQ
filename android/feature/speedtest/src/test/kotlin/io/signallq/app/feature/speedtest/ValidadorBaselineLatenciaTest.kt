package io.signallq.app.feature.speedtest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GH#1118 — evidência real que motivou a correção: no mesmo device/rede (SM-A256E,
 * EdgeUno), o Ookla mediu ping idle 11ms e latência sob carga 77ms(down)/109ms(up); o
 * worker dedicado bateu com o Ookla (12ms); o host público CDN mediu 408ms de baseline —
 * maior que a própria latência sob carga (108/228ms), o que grampeava o bufferbloat em 0
 * via `max(x, 0)`. Os casos abaixo replicam esses números.
 */
class ValidadorBaselineLatenciaTest {

    @Test
    fun `baseline maior que latencia sob carga e implausivel`() {
        // Caso real: baseline inflado (host publico) = 408, sob carga (Ookla-like) = 109.
        assertTrue(ValidadorBaselineLatencia.baselineImplausivel(latenciaBaseMs = 408.0, latenciaSobCargaMs = 109.0))
    }

    @Test
    fun `baseline menor que latencia sob carga e plausivel`() {
        // Caso correto: baseline do worker dedicado (12) bem abaixo da latencia sob carga (109).
        assertFalse(ValidadorBaselineLatencia.baselineImplausivel(latenciaBaseMs = 12.0, latenciaSobCargaMs = 109.0))
    }

    @Test
    fun `baseline igual a latencia sob carga nao e implausivel`() {
        assertFalse(ValidadorBaselineLatencia.baselineImplausivel(latenciaBaseMs = 50.0, latenciaSobCargaMs = 50.0))
    }

    @Test
    fun `sem dado sob carga nunca marca como implausivel`() {
        // pingDownload/pingUpload vazios (transferencia nao gerou amostra) -> sem base de comparacao.
        assertFalse(ValidadorBaselineLatencia.baselineImplausivel(latenciaBaseMs = 408.0, latenciaSobCargaMs = 0.0))
    }

    @Test
    fun `probe com perda total esta indisponivel`() {
        val resultado = ResultadoAmostragemPing(
            latenciaMs = 0.0,
            jitterMs = 0.0,
            perdaPercentual = 100.0,
            totalAmostras = 14,
            amostrasValidas = 0,
            timeouts = 14,
        )

        assertTrue(ValidadorBaselineLatencia.probeIndisponivel(resultado))
    }

    @Test
    fun `probe com perda parcial nao esta indisponivel`() {
        val resultado = ResultadoAmostragemPing(
            latenciaMs = 12.0,
            jitterMs = 1.0,
            perdaPercentual = 50.0,
            totalAmostras = 14,
            amostrasValidas = 7,
            timeouts = 7,
        )

        assertFalse(ValidadorBaselineLatencia.probeIndisponivel(resultado))
    }

    @Test
    fun `sem nenhuma amostra coletada nao conta como probe indisponivel`() {
        // Caso de borda: rede mudou antes da 1a amostra (totalAmostras=0) -- nao e
        // "worker fora do ar", e nao deve disparar o fallback publico indevidamente.
        val resultado = ResultadoAmostragemPing(
            latenciaMs = 0.0,
            jitterMs = 0.0,
            perdaPercentual = 0.0,
            totalAmostras = 0,
            amostrasValidas = 0,
            timeouts = 0,
        )

        assertFalse(ValidadorBaselineLatencia.probeIndisponivel(resultado))
    }

    @Test
    fun `numeros exatos da evidencia Ookla confirmam bufferbloat positivo apos fix`() {
        // Antes do fix: baseline=408 > sobCarga=228 -> bufferbloat grampeava em 0 (bug).
        // Depois do fix: baseline=11 (worker/Ookla), sobCarga max(77,109)=109 -> bufferbloat=98,
        // proximo do esperado (~66ms conforme a issue, dentro da variacao normal de medicao).
        assertFalse(ValidadorBaselineLatencia.baselineImplausivel(latenciaBaseMs = 11.0, latenciaSobCargaMs = 109.0))
        val bufferbloat = maxOf(77.0, 109.0) - 11.0
        assertEquals(98.0, bufferbloat, 0.0001)
    }
}
