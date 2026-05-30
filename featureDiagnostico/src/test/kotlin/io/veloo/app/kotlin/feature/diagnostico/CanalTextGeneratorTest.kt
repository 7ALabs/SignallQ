package io.veloo.app.feature.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Test

class CanalTextGeneratorTest {

    private val strings = CanalStrings.PadraoPortugues

    private fun dado(
        canal: Int,
        count: Int,
        nivel: NivelCongestionamento = NivelCongestionamento.livre,
        ehCanalAtual: Boolean = false,
        ehCanalRecomendado: Boolean = false,
    ) = DadoCanal(
        canal = canal,
        count = count,
        countProprios = 0,
        countTerceiros = count,
        maxRssiDbm = if (count > 0) -60 else null,
        nivel = nivel,
        ehCanalAtual = ehCanalAtual,
        ehCanalRecomendado = ehCanalRecomendado,
    )

    // ── sem dados ─────────────────────────────────────────────────────────────

    @Test
    fun `sem redes retorna mensagem de sem dados`() {
        val snapshot = SnapshotEspectroCanal(
            dadosPorCanal = listOf(dado(1, 0), dado(6, 0), dado(11, 0)),
            canalAtual = null,
            canalRecomendado = null,
            motivoRecomendacao = null,
            banda = "2.4GHz",
        )
        assertEquals(strings.semDados(), CanalTextGenerator.gerarTexto(snapshot, strings))
    }

    // ── faixa quase vazia ────────────────────────────────────────────────────

    @Test
    fun `menos de 5 redes retorna faixa quase vazia`() {
        val snapshot = SnapshotEspectroCanal(
            dadosPorCanal = listOf(
                dado(1, 2, NivelCongestionamento.livre, ehCanalAtual = true),
                dado(6, 1, NivelCongestionamento.livre),
                dado(11, 0),
            ),
            canalAtual = 1,
            canalRecomendado = 11,
            motivoRecomendacao = null,
            banda = "2.4GHz",
        )
        assertEquals(strings.bandaQuaseVazia("2.4GHz"), CanalTextGenerator.gerarTexto(snapshot, strings))
    }

    @Test
    fun `faixa quase vazia usa a banda correta no texto`() {
        val snapshot = SnapshotEspectroCanal(
            dadosPorCanal = listOf(
                dado(36, 1, NivelCongestionamento.livre, ehCanalAtual = true),
                dado(40, 2, NivelCongestionamento.livre),
            ),
            canalAtual = 36,
            canalRecomendado = null,
            motivoRecomendacao = null,
            banda = "5GHz",
        )
        assertEquals(strings.bandaQuaseVazia("5GHz"), CanalTextGenerator.gerarTexto(snapshot, strings))
    }

    // ── congestionamento por canal ────────────────────────────────────────────

    @Test
    fun `canal atual congestionado com alternativa retorna congestionamento por canal`() {
        val snapshot = SnapshotEspectroCanal(
            dadosPorCanal = listOf(
                dado(1, 8, NivelCongestionamento.congestionado, ehCanalAtual = true),
                dado(6, 2, NivelCongestionamento.livre, ehCanalRecomendado = true),
                dado(11, 3, NivelCongestionamento.moderado),
            ),
            canalAtual = 1,
            canalRecomendado = 6,
            motivoRecomendacao = null,
            banda = "2.4GHz",
        )
        assertEquals(
            strings.canalAtualCongestionado(1, 6),
            CanalTextGenerator.gerarTexto(snapshot, strings),
        )
    }

    @Test
    fun `canal atual congestionado mas sem alternativa nao retorna congestionamento por canal`() {
        val snapshot = SnapshotEspectroCanal(
            dadosPorCanal = listOf(
                dado(1, 8, NivelCongestionamento.congestionado, ehCanalAtual = true, ehCanalRecomendado = true),
                dado(6, 7, NivelCongestionamento.congestionado),
                dado(11, 6, NivelCongestionamento.congestionado),
            ),
            canalAtual = 1,
            canalRecomendado = 1,
            motivoRecomendacao = null,
            banda = "2.4GHz",
        )
        val resultado = CanalTextGenerator.gerarTexto(snapshot, strings)
        // Canal recomendado == canal atual, então não é "congestionamento por canal"
        // Maioria congestionada → faixa congestionada
        assertEquals(strings.bandaCongestionada("2.4GHz"), resultado)
    }

    @Test
    fun `faixa 5GHz com canal congestionado e alternativa retorna congestionamento por canal`() {
        val snapshot = SnapshotEspectroCanal(
            dadosPorCanal = listOf(
                dado(36, 8, NivelCongestionamento.congestionado, ehCanalAtual = true),
                dado(40, 7, NivelCongestionamento.congestionado),
                dado(44, 7, NivelCongestionamento.congestionado),
                dado(48, 2, NivelCongestionamento.livre, ehCanalRecomendado = true),
            ),
            canalAtual = 36,
            canalRecomendado = 48,
            motivoRecomendacao = null,
            banda = "5GHz",
        )
        assertEquals(
            strings.canalAtualCongestionado(36, 48),
            CanalTextGenerator.gerarTexto(snapshot, strings),
        )
    }

    // ── faixa congestionada ──────────────────────────────────────────────────

    @Test
    fun `maioria dos canais congestionados retorna faixa congestionada`() {
        val snapshot = SnapshotEspectroCanal(
            dadosPorCanal = listOf(
                dado(1, 7, NivelCongestionamento.congestionado),
                dado(6, 8, NivelCongestionamento.congestionado),
                dado(11, 6, NivelCongestionamento.congestionado, ehCanalAtual = true, ehCanalRecomendado = true),
            ),
            canalAtual = 11,
            canalRecomendado = 11,
            motivoRecomendacao = null,
            banda = "2.4GHz",
        )
        assertEquals(strings.bandaCongestionada("2.4GHz"), CanalTextGenerator.gerarTexto(snapshot, strings))
    }

    @Test
    fun `exatamente metade dos canais congestionados nao e faixa congestionada`() {
        val snapshot = SnapshotEspectroCanal(
            dadosPorCanal = listOf(
                dado(1, 7, NivelCongestionamento.congestionado),
                dado(6, 4, NivelCongestionamento.moderado, ehCanalAtual = true),
                dado(11, 3, NivelCongestionamento.moderado),
                dado(4, 7, NivelCongestionamento.congestionado),
            ),
            canalAtual = 6,
            canalRecomendado = null,
            motivoRecomendacao = null,
            banda = "2.4GHz",
        )
        val resultado = CanalTextGenerator.gerarTexto(snapshot, strings)
        // 2 de 4 canais congestionados (50%), não maioria → não é faixa congestionada
        assert(resultado != strings.bandaCongestionada("2.4GHz"))
    }

    // ── canal recomendado ────────────────────────────────────────────────────

    @Test
    fun `canal recomendado livre retorna mensagem de recomendado livre`() {
        val snapshot = SnapshotEspectroCanal(
            dadosPorCanal = listOf(
                dado(1, 5, NivelCongestionamento.moderado, ehCanalAtual = true),
                dado(6, 3, NivelCongestionamento.moderado),
                dado(11, 0, NivelCongestionamento.livre, ehCanalRecomendado = true),
            ),
            canalAtual = 1,
            canalRecomendado = 11,
            motivoRecomendacao = null,
            banda = "2.4GHz",
        )
        assertEquals(
            strings.canalRecomendadoLivre(11, "2.4GHz"),
            CanalTextGenerator.gerarTexto(snapshot, strings),
        )
    }

    @Test
    fun `canal recomendado nao livre retorna mensagem de recomendado moderado`() {
        val snapshot = SnapshotEspectroCanal(
            dadosPorCanal = listOf(
                dado(1, 6, NivelCongestionamento.congestionado, ehCanalAtual = true, ehCanalRecomendado = true),
                dado(6, 5, NivelCongestionamento.moderado),
                dado(11, 4, NivelCongestionamento.moderado),
            ),
            canalAtual = 1,
            canalRecomendado = 1,
            motivoRecomendacao = null,
            banda = "2.4GHz",
        )
        // canalAtual == canalRecomendado → não é "congestionamento por canal"
        // 1 de 3 congestionados (33%) → não é faixa congestionada
        // canalRec level = congestionado (não livre) → canalRecomendadoModerado
        assertEquals(
            strings.canalRecomendadoModerado(1, "2.4GHz"),
            CanalTextGenerator.gerarTexto(snapshot, strings),
        )
    }

    @Test
    fun `canal recomendado moderado quando nivel e moderado`() {
        val snapshot = SnapshotEspectroCanal(
            dadosPorCanal = listOf(
                dado(1, 5, NivelCongestionamento.moderado, ehCanalAtual = true),
                dado(6, 5, NivelCongestionamento.moderado),
                dado(11, 4, NivelCongestionamento.moderado, ehCanalRecomendado = true),
            ),
            canalAtual = 1,
            canalRecomendado = 11,
            motivoRecomendacao = null,
            banda = "2.4GHz",
        )
        // Canal atual moderado (não congestionado) → não é "congestionamento por canal"
        // Nenhum congestionado → não é faixa congestionada
        // Canal rec nivel = moderado → canalRecomendadoModerado
        assertEquals(
            strings.canalRecomendadoModerado(11, "2.4GHz"),
            CanalTextGenerator.gerarTexto(snapshot, strings),
        )
    }

    // ── motivoRecomendacao ────────────────────────────────────────────────────

    @Test
    fun `canal recomendado livre com motivoRecomendacao concatena sufixo`() {
        val motivo = "Canal livre — sem congestionamento"
        val snapshot = SnapshotEspectroCanal(
            dadosPorCanal = listOf(
                dado(1, 5, NivelCongestionamento.moderado, ehCanalAtual = true),
                dado(6, 3, NivelCongestionamento.moderado),
                dado(11, 0, NivelCongestionamento.livre, ehCanalRecomendado = true),
            ),
            canalAtual = 1,
            canalRecomendado = 11,
            motivoRecomendacao = motivo,
            banda = "2.4GHz",
        )
        val esperado = "${strings.canalRecomendadoLivre(11, "2.4GHz")}\n$motivo"
        assertEquals(esperado, CanalTextGenerator.gerarTexto(snapshot, strings))
    }

    @Test
    fun `canal recomendado moderado sem motivoRecomendacao nao altera texto`() {
        val snapshot = SnapshotEspectroCanal(
            dadosPorCanal = listOf(
                dado(1, 5, NivelCongestionamento.moderado, ehCanalAtual = true),
                dado(6, 5, NivelCongestionamento.moderado),
                dado(11, 4, NivelCongestionamento.moderado, ehCanalRecomendado = true),
            ),
            canalAtual = 1,
            canalRecomendado = 11,
            motivoRecomendacao = null,
            banda = "2.4GHz",
        )
        assertEquals(
            strings.canalRecomendadoModerado(11, "2.4GHz"),
            CanalTextGenerator.gerarTexto(snapshot, strings),
        )
    }

    // ── strings customizadas ──────────────────────────────────────────────────

    @Test
    fun `aceita strings customizadas para localizacao`() {
        val stringsEn = CanalStrings(
            bandaCongestionada = { banda -> "Band $banda is congested." },
            bandaQuaseVazia = { banda -> "Band $banda is nearly empty." },
            canalAtualCongestionado = { cur, rec -> "Channel $cur congested, try $rec." },
            canalRecomendadoLivre = { canal, banda -> "Channel $canal is free in $banda." },
            canalRecomendadoModerado = { canal, banda -> "Try channel $canal in $banda." },
            semDados = { "No data available." },
        )
        val snapshot = SnapshotEspectroCanal(
            dadosPorCanal = listOf(dado(1, 0), dado(6, 0)),
            canalAtual = null,
            canalRecomendado = null,
            motivoRecomendacao = null,
            banda = "2.4GHz",
        )
        assertEquals("No data available.", CanalTextGenerator.gerarTexto(snapshot, stringsEn))
    }
}
