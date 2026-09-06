package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes do [DiagnosticoGuiadoEngine] para os 4 macro objetivos.
 */
class DiagnosticoGuiadoEngineTest {

    private fun internet(
        download: Double? = 100.0,
        upload: Double? = 20.0,
        latencia: Double? = 20.0,
        jitter: Double? = 5.0,
        perda: Double? = 0.0,
        bufferbloat: Double? = 10.0,
    ) = InternetDiagnosticInput(
        downloadMbps = download,
        uploadMbps = upload,
        latencyMs = latencia,
        jitterMs = jitter,
        perdaPercentual = perda,
        bufferbloatMs = bufferbloat,
    )

    // ── INSTABILIDADE_QUEDAS (perda + jitter) ──────────────────────────────

    @Test
    fun `instabilidade fica ok sem perda nem jitter fora da faixa`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.INSTABILIDADE_QUEDAS,
            DiagnosticInput(internet = internet(perda = 0.0, jitter = 3.0)),
        )
        assertEquals(DiagnosticStatus.ok, r.status)
        assertTrue(r.acoes.isEmpty())
    }

    @Test
    fun `instabilidade fica critica com perda alta`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.INSTABILIDADE_QUEDAS,
            DiagnosticInput(internet = internet(perda = 3.8, jitter = 3.0)),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
        assertEquals(2, r.evidencias.size)
        assertTrue(r.acoes.isNotEmpty())
    }

    @Test
    fun `instabilidade fica inconclusiva sem dados de internet`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.INSTABILIDADE_QUEDAS,
            null,
        )
        assertEquals(DiagnosticStatus.inconclusive, r.status)
        assertTrue(r.dadosInsuficientes)
        assertTrue(r.evidencias.isEmpty())
        assertTrue(r.acoes.isEmpty())
    }

    // ── PROBLEMAS_VIDEO_JOGOS ───────────────────────────────

    @Test
    fun `problemas video jogos fica atencao com bufferbloat moderado`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.PROBLEMAS_VIDEO_JOGOS,
            DiagnosticInput(internet = internet(bufferbloat = 60.0)),
        )
        assertEquals(DiagnosticStatus.attention, r.status)
    }

    @Test
    fun `problemas video jogos fica critica com latencia alta`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.PROBLEMAS_VIDEO_JOGOS,
            DiagnosticInput(
                internet = internet(latencia = 187.0, jitter = 44.0),
                wifi = WifiDiagnosticInput(rssiDbm = -50, linkSpeedMbps = 300, frequenciaMhz = 5200),
                connectionType = ConnectionType.wifi
            ),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
        assertTrue(r.evidencias.any { it.label == "Força do sinal Wi-Fi" })
    }

    @Test
    fun `problemas video jogos fica ok dentro das faixas`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.PROBLEMAS_VIDEO_JOGOS,
            DiagnosticInput(
                internet = internet(latencia = 30.0, jitter = 5.0, perda = 0.0),
                wifi = WifiDiagnosticInput(rssiDbm = -45, linkSpeedMbps = 300, frequenciaMhz = 5200),
                connectionType = ConnectionType.wifi
            ),
        )
        assertEquals(DiagnosticStatus.ok, r.status)
    }

    // ── LENTIDAO_GERAL (DNS + latência + download) ──

    @Test
    fun `lentidao geral usa latencia dns quando disponivel`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.LENTIDAO_GERAL,
            DiagnosticInput(
                internet = internet(latencia = 54.0),
                dns = DnsDiagnosticInput(currentDnsLatencyMs = 220),
            ),
        )
        // 1 (dns) + 1 (download ok)
        assertEquals(2, r.evidencias.size)
        assertTrue(r.evidencias.any { it.label == "Tempo para localizar sites" })
        assertEquals(DiagnosticStatus.attention, r.status)
    }
    
    @Test
    fun `lentidao geral verifica plano contratado quando informado`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.LENTIDAO_GERAL,
            DiagnosticInput(internet = internet(download = 38.0), velocidadeContratadaMbps = 100),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
        assertTrue(r.evidencias.any { it.label == "Velocidade recebida do plano" })
    }

    // ── OUTRO_PROBLEMA ─────────────────────

    @Test
    fun `outro problema avalia com metricas gerais`() {
        val resultado =
            DiagnosticoGuiadoEngine.avaliar(
                ObjetivoDiagnostico.OUTRO_PROBLEMA,
                input = DiagnosticInput(internet = internet(latencia = 200.0, jitter = 80.0, perda = 5.0)),
            )
        assertTrue(resultado.evidencias.isNotEmpty())
        assertEquals(DiagnosticStatus.critical, resultado.status)
    }

    @Test
    fun `outro problema sem nenhuma metrica fica inconclusivo, sem inventar evidencia`() {
        val resultado = DiagnosticoGuiadoEngine.avaliar(ObjetivoDiagnostico.OUTRO_PROBLEMA, null)
        assertTrue(resultado.dadosInsuficientes)
        assertTrue(resultado.evidencias.isEmpty())
        assertEquals(DiagnosticStatus.inconclusive, resultado.status)
    }
}
