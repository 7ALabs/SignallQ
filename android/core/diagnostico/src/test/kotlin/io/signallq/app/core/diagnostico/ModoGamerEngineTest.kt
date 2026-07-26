package io.signallq.app.core.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes do [ModoGamerEngine] (Feature #550, issue #1476) — cobre as 6 categorias
 * (faixa ok/atenção/crítica por métrica priorizada), dados insuficientes (nunca inventa
 * evidência), a evidência informativa de device e o catálogo de 9 jogos + fallback.
 */
class ModoGamerEngineTest {

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

    // ── FPS competitivo (latência + jitter + perda) ──────────────────────────

    @Test
    fun `fps competitivo fica ok dentro da faixa`() {
        val r = ModoGamerEngine.avaliar(
            CategoriaJogoModoGamer.FPS_COMPETITIVO,
            DeviceJogo.PC,
            DiagnosticInput(internet = internet(latencia = 40.0, jitter = 3.0, perda = 0.0)),
        )
        assertEquals(DiagnosticStatus.ok, r.status)
        assertTrue(r.acoes.isEmpty())
    }

    @Test
    fun `fps competitivo fica critica com latencia alta`() {
        val r = ModoGamerEngine.avaliar(
            CategoriaJogoModoGamer.FPS_COMPETITIVO,
            DeviceJogo.PC,
            DiagnosticInput(internet = internet(latencia = 250.0)),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
        assertTrue(r.acoes.isNotEmpty())
    }

    @Test
    fun `fps competitivo inconclusivo sem dados de internet`() {
        val r = ModoGamerEngine.avaliar(CategoriaJogoModoGamer.FPS_COMPETITIVO, DeviceJogo.PC, null)
        assertEquals(DiagnosticStatus.inconclusive, r.status)
        assertTrue(r.dadosInsuficientes)
        // Mesmo sem métricas, a linha de contexto do device continua presente.
        assertEquals(1, r.evidencias.size)
        assertEquals("Conexão do teste", r.evidencias.first().label)
    }

    // ── Battle royale (latência + jitter + download) ─────────────────────────

    @Test
    fun `battle royale fica atencao com download no limite`() {
        val r = ModoGamerEngine.avaliar(
            CategoriaJogoModoGamer.BATTLE_ROYALE,
            DeviceJogo.ANDROID,
            DiagnosticInput(internet = internet(download = 30.0)),
        )
        assertEquals(DiagnosticStatus.attention, r.status)
    }

    // ── MOBA (jitter + latência + perda) ──────────────────────────────────────

    @Test
    fun `moba fica critica com jitter alto`() {
        val r = ModoGamerEngine.avaliar(
            CategoriaJogoModoGamer.MOBA,
            DeviceJogo.PC,
            DiagnosticInput(internet = internet(jitter = 40.0)),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
    }

    // ── Casual (download + latência) ──────────────────────────────────────────

    @Test
    fun `casual fica ok com download e latencia bons`() {
        val r = ModoGamerEngine.avaliar(
            CategoriaJogoModoGamer.CASUAL,
            DeviceJogo.SWITCH,
            DiagnosticInput(internet = internet(download = 80.0, latencia = 50.0)),
        )
        assertEquals(DiagnosticStatus.ok, r.status)
        assertEquals(2, r.evidencias.size - 1) // -1 = linha de device
    }

    // ── Cloud gaming (download + bufferbloat + latência) ──────────────────────

    @Test
    fun `cloud gaming fica critica com bufferbloat severo`() {
        val r = ModoGamerEngine.avaliar(
            CategoriaJogoModoGamer.CLOUD_GAMING,
            DeviceJogo.TV_CLOUD,
            DiagnosticInput(internet = internet(bufferbloat = 150.0)),
        )
        assertEquals(DiagnosticStatus.critical, r.status)
    }

    // ── Outro (fallback, mesma régua de FPS_COMPETITIVO) ──────────────────────

    @Test
    fun `outro fica atencao com latencia no limite`() {
        val r = ModoGamerEngine.avaliar(
            CategoriaJogoModoGamer.OUTRO,
            DeviceJogo.IPHONE,
            DiagnosticInput(internet = internet(latencia = 170.0)),
        )
        assertEquals(DiagnosticStatus.attention, r.status)
    }

    // ── Evidência de device (sempre presente, nunca influencia severidade) ────

    @Test
    fun `evidencia de device usa o label do device selecionado`() {
        val r = ModoGamerEngine.avaliar(
            CategoriaJogoModoGamer.FPS_COMPETITIVO,
            DeviceJogo.PLAYSTATION,
            DiagnosticInput(internet = internet()),
        )
        val evidenciaDevice = r.evidencias.last()
        assertEquals("Conexão do teste", evidenciaDevice.label)
        assertEquals("PS5 / PS4", evidenciaDevice.valorExibido)
        assertEquals(MetricStatus.inconclusivo, evidenciaDevice.status)
    }

    @Test
    fun `device nunca eleva status mesmo com metricas ruins`() {
        val r = ModoGamerEngine.avaliar(
            CategoriaJogoModoGamer.FPS_COMPETITIVO,
            DeviceJogo.XBOX,
            DiagnosticInput(internet = internet(latencia = 20.0, jitter = 2.0, perda = 0.0)),
        )
        assertEquals(DiagnosticStatus.ok, r.status)
    }

    // ── Catálogo de jogos ───────────────────────────────────────────────────

    @Test
    fun `catalogo tem exatamente 9 jogos com ids unicos`() {
        assertEquals(9, CatalogoJogosModoGamer.jogos.size)
        assertEquals(9, CatalogoJogosModoGamer.jogos.map { it.gameId }.toSet().size)
    }

    @Test
    fun `catalogo resolve jogo catalogado por id`() {
        val jogo = CatalogoJogosModoGamer.porId("valorant")
        assertEquals("Valorant", jogo?.nome)
        assertEquals(CategoriaJogoModoGamer.FPS_COMPETITIVO, jogo?.categoria)
    }

    @Test
    fun `catalogo devolve null para jogo fora do catalogo (nunca erro)`() {
        assertEquals(null, CatalogoJogosModoGamer.porId("jogo-inexistente-xyz"))
    }

    @Test
    fun `todas as 6 categorias tem avaliador proprio (nenhuma lanca excecao)`() {
        CategoriaJogoModoGamer.entries.forEach { categoria ->
            val r = ModoGamerEngine.avaliar(categoria, DeviceJogo.PC, DiagnosticInput(internet = internet()))
            assertEquals(categoria, r.categoria)
        }
    }
}
