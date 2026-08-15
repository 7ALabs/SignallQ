package io.signallq.app.feature.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TendenciaCalculadorTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun resumo(
        totalMedicoes: Int = 5,
        ultimoDownloadMbps: Double? = 100.0,
        mediaDownloadMbps5: Double? = 100.0,
    ): ResumoHistorico = ResumoHistorico(
        totalMedicoes = totalMedicoes,
        ultimaMedicaoEpochMs = null,
        ultimoDownloadMbps = ultimoDownloadMbps,
        ultimoUploadMbps = null,
        ultimaLatenciaMs = null,
        ultimoJitterMs = null,
        ultimaPerdaPercentual = null,
        ultimoBufferbloatMs = null,
        mediaDownloadMbps5 = mediaDownloadMbps5,
        mediaUploadMbps5 = null,
        mediaLatenciaMs5 = null,
        quantidadeContaminadas5 = 0,
        ultimasMedicoes = emptyList(),
    )

    // ── Casos que retornam null ───────────────────────────────────────────────

    @Test
    fun `retorna null quando totalMedicoes e menor que 2`() {
        val resultado = calcularTendencia(resumo(totalMedicoes = 1))
        assertNull(resultado)
    }

    @Test
    fun `retorna null quando ultimoDownloadMbps e null`() {
        val resultado = calcularTendencia(resumo(ultimoDownloadMbps = null))
        assertNull(resultado)
    }

    @Test
    fun `retorna null quando mediaDownloadMbps5 e null`() {
        val resultado = calcularTendencia(resumo(mediaDownloadMbps5 = null))
        assertNull(resultado)
    }

    @Test
    fun `retorna null quando media e zero`() {
        val resultado = calcularTendencia(resumo(ultimoDownloadMbps = 50.0, mediaDownloadMbps5 = 0.0))
        assertNull(resultado)
    }

    // ── Casos de tendência ────────────────────────────────────────────────────

    @Test
    fun `retorna MELHOROU com 15 quando delta e positivo 15 por cento`() {
        // ultimo = 115, media = 100 → delta = +15%
        val resultado = calcularTendencia(resumo(ultimoDownloadMbps = 115.0, mediaDownloadMbps5 = 100.0))
        assertEquals(Pair(TendenciaEstado.MELHOROU, 15), resultado)
    }

    @Test
    fun `retorna PIOROU com 20 quando delta e negativo 20 por cento`() {
        // ultimo = 80, media = 100 → delta = -20%
        val resultado = calcularTendencia(resumo(ultimoDownloadMbps = 80.0, mediaDownloadMbps5 = 100.0))
        assertEquals(Pair(TendenciaEstado.PIOROU, 20), resultado)
    }

    @Test
    fun `retorna ESTAVEL com 5 quando delta e positivo 5 por cento`() {
        // ultimo = 105, media = 100 → delta = +5%
        val resultado = calcularTendencia(resumo(ultimoDownloadMbps = 105.0, mediaDownloadMbps5 = 100.0))
        assertEquals(Pair(TendenciaEstado.ESTAVEL, 5), resultado)
    }

    @Test
    fun `retorna ESTAVEL quando delta e exatamente mais 10 por cento (nao maior que 10)`() {
        // ultimo = 110, media = 100 → delta = +10.0 exato → NÃO é > 10.0 → ESTAVEL
        val resultado = calcularTendencia(resumo(ultimoDownloadMbps = 110.0, mediaDownloadMbps5 = 100.0))
        assertEquals(Pair(TendenciaEstado.ESTAVEL, 10), resultado)
    }

    @Test
    fun `retorna ESTAVEL quando delta e exatamente menos 10 por cento (nao menor que menos 10)`() {
        // ultimo = 90, media = 100 → delta = -10.0 exato → NÃO é < -10.0 → ESTAVEL
        val resultado = calcularTendencia(resumo(ultimoDownloadMbps = 90.0, mediaDownloadMbps5 = 100.0))
        assertEquals(Pair(TendenciaEstado.ESTAVEL, 10), resultado)
    }
}
