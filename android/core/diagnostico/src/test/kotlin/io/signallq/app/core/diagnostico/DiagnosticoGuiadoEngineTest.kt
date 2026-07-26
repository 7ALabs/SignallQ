package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes do [DiagnosticoGuiadoEngine] (Feature #550, issue #1475) — cobre os 7
 * objetivos fechados: faixa ok/atenção/crítica por métrica priorizada, dados
 * insuficientes (nunca inventa evidência) e o branching por resposta que de fato
 * muda a avaliação (jogos por cabo, velocidade vs. plano contratado, Wi-Fi vs.
 * operadora).
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

    // ── Internet cai ou oscila (perda + jitter) ──────────────────────────────

    @Test
    fun `internet cai oscila fica ok sem perda nem jitter fora da faixa`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.INTERNET_CAI_OSCILA,
            emptyList(),
            DiagnosticInput(internet = internet(perda = 0.0, jitter = 3.0)),
        )
        assertEquals(DiagnosticStatus.ok, r.status)
        assertTrue(r.acoes.isEmpty())
    }

    @Test
    fun `internet cai oscila fica critica com perda alta`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.INTERNET_CAI_OSCILA,
            emptyList(),
            DiagnosticInput(internet = internet(perda = 3.8, jitter = 3.0)),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
        assertEquals(2, r.evidencias.size)
        assertTrue(r.acoes.isNotEmpty())
    }

    @Test
    fun `internet cai oscila fica inconclusiva sem dados de internet`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.INTERNET_CAI_OSCILA,
            emptyList(),
            null,
        )
        assertEquals(DiagnosticStatus.inconclusive, r.status)
        assertTrue(r.dadosInsuficientes)
        assertTrue(r.evidencias.isEmpty())
        assertTrue(r.acoes.isEmpty())
    }

    // ── Vídeos travam (bufferbloat + download) ───────────────────────────────

    @Test
    fun `videos travam fica atencao com bufferbloat moderado`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.VIDEOS_TRAVAM,
            emptyList(),
            DiagnosticInput(internet = internet(bufferbloat = 60.0)),
        )
        assertEquals(DiagnosticStatus.attention, r.status)
    }

    @Test
    fun `videos travam fica critica com bufferbloat severo`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.VIDEOS_TRAVAM,
            emptyList(),
            DiagnosticInput(internet = internet(bufferbloat = 210.0)),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
    }

    // ── Jogos com lag (latência + jitter + perda, Wi-Fi condicionado à resposta) ─

    @Test
    fun `jogos com lag fica critica com latencia alta`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.JOGOS_COM_LAG,
            listOf(0), // "Wi-Fi"
            DiagnosticInput(
                internet = internet(latencia = 187.0, jitter = 44.0),
                wifi = WifiDiagnosticInput(rssiDbm = -50, linkSpeedMbps = 300, frequenciaMhz = 5200),
            ),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
        assertTrue(r.evidencias.any { it.label == "Sinal Wi-Fi" })
        assertTrue(r.acoes.any { it.contains("cabo", ignoreCase = true) })
    }

    @Test
    fun `jogos com lag por cabo nao usa sinal wifi como evidencia nem recomenda cabo`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.JOGOS_COM_LAG,
            listOf(1), // "Cabo de rede"
            DiagnosticInput(
                internet = internet(latencia = 150.0, jitter = 40.0),
                wifi = WifiDiagnosticInput(rssiDbm = -85, linkSpeedMbps = 20, frequenciaMhz = 2400),
            ),
        )
        assertTrue(r.evidencias.none { it.label == "Sinal Wi-Fi" })
        assertTrue(r.acoes.none { it.contains("cabo", ignoreCase = true) && it.contains("em vez de Wi-Fi") })
    }

    @Test
    fun `jogos com lag fica ok dentro das faixas de fps competitivo`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.JOGOS_COM_LAG,
            listOf(0),
            DiagnosticInput(
                internet = internet(latencia = 30.0, jitter = 5.0, perda = 0.0),
                wifi = WifiDiagnosticInput(rssiDbm = -45, linkSpeedMbps = 300, frequenciaMhz = 5200),
            ),
        )
        assertEquals(DiagnosticStatus.ok, r.status)
    }

    // ── Chamadas congelam (jitter + perda + upload) ──────────────────────────

    @Test
    fun `chamadas congelam fica critica com jitter alto`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.CHAMADAS_CONGELAM,
            emptyList(),
            DiagnosticInput(internet = internet(jitter = 38.0)),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
    }

    // ── Sites demoram (DNS quando disponível, latência geral como fallback) ──

    @Test
    fun `sites demoram usa latencia dns quando disponivel`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.SITES_DEMORAM,
            emptyList(),
            DiagnosticInput(
                internet = internet(latencia = 54.0),
                dns = DnsDiagnosticInput(currentDnsLatencyMs = 220),
            ),
        )
        assertEquals(1, r.evidencias.size)
        assertEquals("Latência DNS", r.evidencias.first().label)
        assertEquals(DiagnosticStatus.attention, r.status)
    }

    @Test
    fun `sites demoram cai para latencia geral sem dado de dns`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.SITES_DEMORAM,
            emptyList(),
            DiagnosticInput(internet = internet(latencia = 54.0), dns = null),
        )
        assertEquals("Latência geral", r.evidencias.first().label)
    }

    // ── Velocidade não chega (download vs. contratado) ───────────────────────

    @Test
    fun `velocidade nao chega compara com plano contratado quando informado`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.VELOCIDADE_NAO_CHEGA,
            emptyList(),
            DiagnosticInput(internet = internet(download = 38.0), velocidadeContratadaMbps = 100),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
        assertEquals("Download vs. contratado", r.evidencias.first().label)
    }

    @Test
    fun `velocidade nao chega cai para regua generica sem plano informado`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.VELOCIDADE_NAO_CHEGA,
            emptyList(),
            DiagnosticInput(internet = internet(download = 15.0), velocidadeContratadaMbps = null),
        )
        assertEquals("Download", r.evidencias.first().label)
    }

    // ── Wi-Fi vs. operadora (sinal medido + auto-relato) ─────────────────────

    @Test
    fun `wifi vs operadora aponta wifi quando melhora muito sem wifi`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.WIFI_VS_OPERADORA,
            listOf(0), // "Sim, melhora muito"
            DiagnosticInput(
                connectionType = ConnectionType.wifi,
                internet = internet(),
                wifi = WifiDiagnosticInput(rssiDbm = -50, linkSpeedMbps = 300, frequenciaMhz = 5200),
            ),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
        assertTrue(r.evidencias.any { it.label == "Relato: melhora sem Wi-Fi" })
    }

    @Test
    fun `wifi vs operadora fica ok quando nao muda nada e sinal bom`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.WIFI_VS_OPERADORA,
            listOf(2), // "Não muda nada"
            DiagnosticInput(
                connectionType = ConnectionType.wifi,
                internet = internet(),
                wifi = WifiDiagnosticInput(rssiDbm = -50, linkSpeedMbps = 300, frequenciaMhz = 5200),
            ),
        )
        assertEquals(DiagnosticStatus.ok, r.status)
    }

    @Test
    fun `wifi vs operadora sem resposta ainda nao testei nao adiciona evidencia de relato`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.WIFI_VS_OPERADORA,
            listOf(3), // "Ainda não testei"
            DiagnosticInput(
                connectionType = ConnectionType.wifi,
                internet = internet(),
                wifi = WifiDiagnosticInput(rssiDbm = -50, linkSpeedMbps = 300, frequenciaMhz = 5200),
            ),
        )
        assertTrue(r.evidencias.none { it.label == "Relato: melhora sem Wi-Fi" })
    }

    // ── Perguntas: 7 objetivos, roteiro sempre fechado (sem texto livre) ─────

    @Test
    fun `todo objetivo tem pelo menos uma pergunta com pelo menos duas opcoes`() {
        ObjetivoDiagnostico.entries.forEach { objetivo ->
            val perguntas = PerguntasDiagnosticoGuiado.perguntas(objetivo)
            assertTrue("objetivo $objetivo sem pergunta", perguntas.isNotEmpty())
            perguntas.forEach { pergunta ->
                assertTrue("pergunta '${pergunta.texto}' com menos de 2 opcoes", pergunta.opcoes.size >= 2)
            }
        }
    }
}
