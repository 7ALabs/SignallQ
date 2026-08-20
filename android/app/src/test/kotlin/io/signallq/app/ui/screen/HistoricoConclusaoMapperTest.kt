package io.signallq.app.ui.screen

import io.signallq.app.core.database.MedicaoEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Caracterização do mapper de conclusão/objetivo do Histórico (issue #1669, épico #1647).
 *
 * O caso mais crítico é o registro **legado**: gravado antes de vereditos, gargalo e texto de
 * diagnóstico existirem (ou antes de rulesVersion/executionId, GH#1228). O mapper nunca pode
 * lançar exceção nem devolver algo ilegível para essas linhas -- é a garantia de "registros
 * antigos continuam legíveis sem migration destrutiva".
 */
class HistoricoConclusaoMapperTest {
    private fun medicaoLegada(
        downloadMbps: Double? = null,
        status: String = "completed",
        fonte: String? = null,
        vereditoStreaming: String? = null,
        vereditoGamer: String? = null,
        vereditoVideoChamada: String? = null,
        gargaloPrimario: String? = null,
        diagnosticoTexto: String? = null,
        diagnosticoOrigem: String? = null,
        diagnosticoProblemas: String? = null,
    ) = MedicaoEntity(
        id = "legado-1",
        timestampEpochMs = 1_700_000_000_000L,
        connectionType = "wifi",
        connectionTypeStart = null,
        connectionTypeEnd = null,
        contaminado = false,
        speedtestMode = null,
        specVersion = null,
        downloadMbps = downloadMbps,
        uploadMbps = null,
        latencyMs = null,
        jitterMs = null,
        perdaPercentual = null,
        bufferbloatMs = null,
        packetLossSource = null,
        vereditoStreaming = vereditoStreaming,
        vereditoGamer = vereditoGamer,
        vereditoVideoChamada = vereditoVideoChamada,
        gargaloPrimario = gargaloPrimario,
        fonte = fonte,
        diagnosticoTexto = diagnosticoTexto,
        diagnosticoOrigem = diagnosticoOrigem,
        diagnosticoProblemas = diagnosticoProblemas,
        status = status,
        // GH#1228: registros anteriores à Fase 3 (migração 15→16) recebem exatamente estes
        // valores default -- não são hipotéticos, é o dado real que existe hoje no banco.
        executionId = "legacy-legado-1",
        rulesVersion = "legacy-unversioned",
    )

    @Test
    fun `registro totalmente legado sem veredito gargalo ou diagnostico cai no fallback de velocidade`() {
        val m = medicaoLegada(downloadMbps = 45.0)

        val resultado = conclusaoDaMedicao(m)

        assertEquals("Teste de velocidade", resultado.objetivo)
        assertEquals("Velocidade boa", resultado.conclusao)
        assertEquals(TomConclusao.POSITIVO, resultado.tom)
    }

    @Test
    fun `registro legado sem nenhuma metrica nao lanca excecao e devolve fallback neutro`() {
        val m = medicaoLegada(downloadMbps = null)

        val resultado = conclusaoDaMedicao(m)

        assertEquals("Sem dados suficientes", resultado.conclusao)
        assertEquals(TomConclusao.NEUTRO, resultado.tom)
    }

    @Test
    fun `velocidade lenta gera tom negativo`() {
        val resultado = conclusaoDaMedicao(medicaoLegada(downloadMbps = 4.0))

        assertEquals("Velocidade lenta", resultado.conclusao)
        assertEquals(TomConclusao.NEGATIVO, resultado.tom)
    }

    @Test
    fun `gargalo primario tem prioridade sobre velocidade crua quando nao ha veredito`() {
        val resultado = conclusaoDaMedicao(medicaoLegada(downloadMbps = 90.0, gargaloPrimario = "bufferbloat"))

        assertEquals("Gargalo identificado: Bufferbloat", resultado.conclusao)
        assertEquals(TomConclusao.ATENCAO, resultado.tom)
    }

    @Test
    fun `gargalo none e tratado como ausencia de gargalo`() {
        val resultado = conclusaoDaMedicao(medicaoLegada(downloadMbps = 120.0, gargaloPrimario = "none"))

        assertEquals("Velocidade excelente", resultado.conclusao)
    }

    @Test
    fun `veredito de streaming tem prioridade sobre gargalo`() {
        val resultado =
            conclusaoDaMedicao(
                medicaoLegada(vereditoStreaming = "good", gargaloPrimario = "download"),
            )

        assertEquals("Bom para streaming", resultado.conclusao)
        assertEquals(TomConclusao.POSITIVO, resultado.tom)
    }

    @Test
    fun `veredito ruim gera tom negativo`() {
        val resultado = conclusaoDaMedicao(medicaoLegada(vereditoGamer = "poor"))

        assertEquals("Ruim para jogos", resultado.conclusao)
        assertEquals(TomConclusao.NEGATIVO, resultado.tom)
    }

    @Test
    fun `texto de diagnostico tem prioridade e usa apenas a primeira frase`() {
        val m =
            medicaoLegada(
                diagnosticoTexto = "Sua conexão está instável. Isso pode afetar chamadas de vídeo.",
                diagnosticoOrigem = "ia",
                vereditoStreaming = "good",
            )

        val resultado = conclusaoDaMedicao(m)

        assertEquals("Diagnóstico por IA", resultado.objetivo)
        assertEquals("Sua conexão está instável", resultado.conclusao)
    }

    @Test
    fun `diagnostico com problemas listados usa tom de atencao`() {
        val m =
            medicaoLegada(
                diagnosticoTexto = "Rede com bom desempenho geral.",
                diagnosticoProblemas = "latencia alta;jitter",
            )

        assertEquals(TomConclusao.ATENCAO, conclusaoDaMedicao(m).tom)
    }

    @Test
    fun `status contaminado sobrepoe qualquer veredito ou diagnostico`() {
        val m =
            medicaoLegada(
                status = "contaminated",
                vereditoStreaming = "good",
                diagnosticoTexto = "Tudo certo.",
            )

        val resultado = conclusaoDaMedicao(m)

        assertEquals("Rede mudou durante o teste", resultado.conclusao)
        assertEquals(TomConclusao.ATENCAO, resultado.tom)
    }

    @Test
    fun `status failed timeout e inconclusive tem conclusao propria`() {
        assertEquals("Teste não concluído", conclusaoDaMedicao(medicaoLegada(status = "failed")).conclusao)
        assertEquals(
            "Tempo esgotado antes de concluir",
            conclusaoDaMedicao(medicaoLegada(status = "timeout")).conclusao,
        )
        assertEquals(
            "Evidência insuficiente para concluir",
            conclusaoDaMedicao(medicaoLegada(status = "inconclusive")).conclusao,
        )
    }

    @Test
    fun `objetivo reconhece fonte orbit como diagnostico por ia mesmo sem texto`() {
        val resultado = conclusaoDaMedicao(medicaoLegada(fonte = "orbit", downloadMbps = 50.0))

        assertEquals("Diagnóstico por IA", resultado.objetivo)
    }

    @Test
    fun `objetivo reconhece fonte monitor quando nao ha diagnostico`() {
        assertEquals("Monitoramento contínuo", objetivoDaMedicao(medicaoLegada(fonte = "monitor")))
    }
}
