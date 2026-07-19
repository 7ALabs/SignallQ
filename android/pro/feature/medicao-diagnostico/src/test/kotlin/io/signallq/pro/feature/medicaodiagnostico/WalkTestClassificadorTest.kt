package io.signallq.pro.feature.medicaodiagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WalkTestClassificadorTest {
    @Test
    fun `classifica rssi excelente a partir de -50 dBm`() {
        assertEquals(QualidadeRssi.EXCELENTE, classificarRssi(-50))
        assertEquals(QualidadeRssi.EXCELENTE, classificarRssi(-30))
    }

    @Test
    fun `classifica rssi boa entre -60 e -51 dBm`() {
        assertEquals(QualidadeRssi.BOA, classificarRssi(-51))
        assertEquals(QualidadeRssi.BOA, classificarRssi(-60))
    }

    @Test
    fun `classifica rssi regular entre -70 e -61 dBm`() {
        assertEquals(QualidadeRssi.REGULAR, classificarRssi(-61))
        assertEquals(QualidadeRssi.REGULAR, classificarRssi(-70))
    }

    @Test
    fun `classifica rssi fraca abaixo de -70 dBm`() {
        assertEquals(QualidadeRssi.FRACA, classificarRssi(-71))
        assertEquals(QualidadeRssi.FRACA, classificarRssi(-90))
    }

    @Test
    fun `delta candidato nulo quando melhora menos que o minimo`() {
        assertNull(calcularDeltaPontoCandidato(rssiAtual = -60, rssiMinSessao = -65))
    }

    @Test
    fun `delta candidato presente quando melhora atinge o minimo`() {
        assertEquals(8, calcularDeltaPontoCandidato(rssiAtual = -60, rssiMinSessao = -68))
    }

    @Test
    fun `delta candidato presente quando melhora excede o minimo`() {
        assertEquals(15, calcularDeltaPontoCandidato(rssiAtual = -55, rssiMinSessao = -70))
    }

    @Test
    fun `formata duracao de sessao em mm-ss`() {
        assertEquals("00:00", formatarDuracaoSessao(0))
        assertEquals("00:09", formatarDuracaoSessao(9_000))
        assertEquals("04:12", formatarDuracaoSessao((4 * 60 + 12) * 1_000L))
        assertEquals("59:59", formatarDuracaoSessao((59 * 60 + 59) * 1_000L))
    }
}
