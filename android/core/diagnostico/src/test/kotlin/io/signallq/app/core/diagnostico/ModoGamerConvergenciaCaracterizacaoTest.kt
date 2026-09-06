package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Caracterização do comportamento ANTES da convergência pedida pela issue #1667 (Task 2.0.19,
 * épico #1647) — trava o resultado atual de [DiagnosticoGuiadoEngine.avaliar] para
 * [ObjetivoDiagnostico.JOGOS_COM_LAG] e de [ModoGamerEngine.avaliar] para as categorias que
 * medem as mesmas 3 dimensões (latência + jitter + perda: [CategoriaJogoModoGamer.FPS_COMPETITIVO]
 * e [CategoriaJogoModoGamer.OUTRO]) para os estados exigidos pela issue: jogo selecionado,
 * genérico/fallback, servidor indisponível (sem dado — equivalente a `input = null` no motor
 * local, que nunca fala com rede), timeout (mesmo tratamento — nenhuma métrica chega), cancelado
 * (nível de ViewModel, coberto por [io.signallq.app.modogamer.ModoGamerViewModelTest]) e dados
 * parciais (só parte das métricas presentes).
 *
 * Depois de extrair a construção compartilhada das 3 dimensões (issue #1667, critério "entrada
 * guiada e ferramenta convergem no mesmo engine"), esta suíte deve continuar passando sem
 * alteração — comportamento idêntico, só a duplicação de código muda.
 */
class ModoGamerConvergenciaCaracterizacaoTest {

    private fun internetCompleto(
        latencia: Double? = 40.0,
        jitter: Double? = 3.0,
        perda: Double? = 0.0,
    ) = InternetDiagnosticInput(
        downloadMbps = 100.0,
        uploadMbps = 20.0,
        latencyMs = latencia,
        jitterMs = jitter,
        perdaPercentual = perda,
    )

    // ── Jogo selecionado (catalogado) — entrada guiada PROBLEMAS_VIDEO_JOGOS por cabo ──────────────


    @Test
    fun `guiado jogos com lag fica critica com latencia alta`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.PROBLEMAS_VIDEO_JOGOS,
            input = DiagnosticInput(internet = internetCompleto(latencia = 250.0)),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
        assertTrue(r.acoes.isNotEmpty())
    }

    // ── Modo gamer direto — mesmas 3 dimensões (FPS_COMPETITIVO/OUTRO) ─────────────────────
    @Test
    fun `modo gamer fps competitivo fica ok com as mesmas metricas boas`() {
        val r = ModoGamerEngine.avaliar(
            CategoriaJogoModoGamer.FPS_COMPETITIVO,
            DeviceJogo.PC,
            DiagnosticInput(internet = internetCompleto()),
        )
        assertEquals(DiagnosticStatus.ok, r.status)
    }

    @Test
    fun `modo gamer outro fallback fica critica com a mesma latencia alta`() {
        val r = ModoGamerEngine.avaliar(
            CategoriaJogoModoGamer.OUTRO,
            DeviceJogo.ANDROID,
            DiagnosticInput(internet = internetCompleto(latencia = 250.0)),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
    }

    // ── Servidor indisponível / timeout — motor local nunca fala com rede, então o único
    // sinal desses dois estados que chega até aqui é "sem input" (camada de rede em `:app`
    // decide não popular DiagnosticInput quando o teste falha) ────────────────────────────
    @Test
    fun `guiado jogos com lag sem input (servidor indisponivel ou timeout) fica inconclusivo`() {
        val r = DiagnosticoGuiadoEngine.avaliar(ObjetivoDiagnostico.PROBLEMAS_VIDEO_JOGOS, input = null)
        assertEquals(DiagnosticStatus.inconclusive, r.status)
        assertTrue(r.dadosInsuficientes)
        assertTrue(r.evidencias.isEmpty())
        assertTrue(r.acoes.isEmpty())
    }

    @Test
    fun `modo gamer sem input (servidor indisponivel ou timeout) fica inconclusivo`() {
        val r = ModoGamerEngine.avaliar(CategoriaJogoModoGamer.FPS_COMPETITIVO, DeviceJogo.PC, null)
        assertEquals(DiagnosticStatus.inconclusive, r.status)
        assertTrue(r.dadosInsuficientes)
    }

    // ── Dados parciais — só parte das métricas presentes ───────────────────────────────────
    @Test
    fun `guiado jogos com lag com dados parciais usa so a metrica disponivel`() {
        val r = DiagnosticoGuiadoEngine.avaliar(
            ObjetivoDiagnostico.PROBLEMAS_VIDEO_JOGOS,
            input = DiagnosticInput(
                internet = InternetDiagnosticInput(
                    downloadMbps = null,
                    uploadMbps = null,
                    latencyMs = 40.0,
                    jitterMs = null,
                    perdaPercentual = null,
                ),
            ),
        )
        assertEquals(1, r.evidencias.size)
        assertEquals("Tempo de resposta com a rede ocupada", r.evidencias.first().label)
    }

    @Test
    fun `modo gamer com dados parciais usa so a metrica disponivel`() {
        val r = ModoGamerEngine.avaliar(
            CategoriaJogoModoGamer.FPS_COMPETITIVO,
            DeviceJogo.PC,
            DiagnosticInput(
                internet = InternetDiagnosticInput(
                    downloadMbps = null,
                    uploadMbps = null,
                    latencyMs = null,
                    jitterMs = 3.0,
                    perdaPercentual = null,
                ),
            ),
        )
        // 1 metrica (jitter) + 1 evidencia de device
        assertEquals(2, r.evidencias.size)
    }
}
