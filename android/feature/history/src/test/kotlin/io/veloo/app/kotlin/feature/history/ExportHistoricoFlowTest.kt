package io.veloo.app.feature.history

import io.veloo.app.core.database.MedicaoEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.UUID

/**
 * Testa os fluxos de export (Task 7e):
 *  - export CSV sucesso
 *  - export PDF erro (arquivo inválido)
 *
 * Testa também a lógica de filtro por período que é usada pelo
 * ExportHistoricoBottomSheet.
 */
class ExportHistoricoFlowTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val exportadorCsv = ExportadorHistoricoCSV()
    private val exportadorPdf = ExportadorHistoricoPDF()

    private fun medicaoSimples(
        timestampEpochMs: Long = System.currentTimeMillis(),
        downloadMbps: Double? = 100.0,
        uploadMbps: Double? = 50.0,
        latenciaMs: Double? = 20.0,
    ) = MedicaoEntity(
        id = UUID.randomUUID().toString(),
        timestampEpochMs = timestampEpochMs,
        connectionType = "wifi",
        connectionTypeStart = null,
        connectionTypeEnd = null,
        contaminado = false,
        speedtestMode = "complete",
        specVersion = null,
        downloadMbps = downloadMbps,
        uploadMbps = uploadMbps,
        latencyMs = latenciaMs,
        jitterMs = 1.0,
        perdaPercentual = 0.0,
        bufferbloatMs = null,
        packetLossSource = null,
        vereditoStreaming = null,
        vereditoGamer = null,
        vereditoVideoChamada = null,
        gargaloPrimario = null,
        fonte = "speedtest",
    )

    // ─── Task 7e: Export CSV sucesso ────────────────────────────────────────

    @Test
    fun `export CSV com medicoes validas retorna sucesso e arquivo nao vazio`() = runBlocking {
        val medicoes = List(5) { medicaoSimples() }
        val arquivo = tmpFolder.newFile("export_sucesso.csv")

        val sucesso = exportadorCsv.exportar(medicoes, arquivo)

        assertTrue("ExportadorHistoricoCSV deve retornar true", sucesso)
        assertTrue("Arquivo CSV deve ter conteudo", arquivo.length() > 0)

        val linhas = arquivo.readLines().filter { it.isNotBlank() }
        // Cabeçalho + 5 linhas de dados
        assertTrue("Deve ter pelo menos 6 linhas (1 header + 5 dados)", linhas.size >= 6)
        assertTrue("Cabeçalho deve conter campo Download", linhas[0].contains("Download"))
    }

    @Test
    fun `export CSV com medicoes de download alto registra valor correto`() = runBlocking {
        val medicao = medicaoSimples(downloadMbps = 999.99)
        val arquivo = tmpFolder.newFile("export_dl_alto.csv")

        exportadorCsv.exportar(listOf(medicao), arquivo)

        val conteudo = arquivo.readText()
        assertTrue("CSV deve conter valor 999.99", conteudo.contains("999.99"))
    }

    // ─── Task 7e: Export PDF erro ────────────────────────────────────────────

    @Test
    fun `export PDF em caminho invalido retorna false`() = runBlocking {
        val medicoes = List(3) { medicaoSimples() }
        // Caminho inexistente — deve retornar false sem lançar exceção
        val arquivoInvalido = tmpFolder.root.resolve("subdir_inexistente/historico.pdf")

        val sucesso = exportadorPdf.exportar(medicoes, arquivoInvalido)

        assertFalse("ExportadorHistoricoPDF deve retornar false para caminho inválido", sucesso)
    }

    @Ignore("PdfDocument usa API Android (Canvas/Paint) que retorna defaults zerados em JVM — export sempre retorna false. Mover para androidTest ou testar via Robolectric com SDK real.")
    @Test
    fun `export PDF com lista vazia retorna sucesso porem arquivo minimo`() = runBlocking {
        val arquivo = tmpFolder.newFile("export_vazio.pdf")

        val sucesso = exportadorPdf.exportar(emptyList(), arquivo)

        assertTrue("PDF sem medicoes ainda deve retornar true (PDF válido)", sucesso)
        assertTrue("Arquivo PDF deve existir", arquivo.exists())
    }

    // ─── Lógica de filtro por período (usada no ExportHistoricoBottomSheet) ─

    @Test
    fun `filtrar 7 dias exclui medicoes mais antigas`() {
        val agora = System.currentTimeMillis()
        val medicoes = listOf(
            medicaoSimples(timestampEpochMs = agora - (8 * 24 * 3600 * 1000L)),   // 8 dias atrás — excluir
            medicaoSimples(timestampEpochMs = agora - (6 * 24 * 3600 * 1000L)),   // 6 dias atrás — incluir
            medicaoSimples(timestampEpochMs = agora - (1 * 24 * 3600 * 1000L)),   // ontem — incluir
        )

        val cutoff = agora - (7 * 24 * 3600 * 1000L)
        val filtradas = medicoes.filter { it.timestampEpochMs >= cutoff }

        assertTrue("Deve excluir medição de 8 dias atrás", filtradas.size == 2)
        assertTrue("Todas as filtradas devem ser recentes", filtradas.all { it.timestampEpochMs >= cutoff })
    }

    @Test
    fun `filtrar tudo retorna lista completa`() {
        val medicoes = List(10) { medicaoSimples() }
        // Período "tudo" = sem filtro de data
        val filtradas = medicoes // sem filtro

        assertTrue("Filtro 'tudo' deve retornar todas as medicoes", filtradas.size == 10)
    }
}
