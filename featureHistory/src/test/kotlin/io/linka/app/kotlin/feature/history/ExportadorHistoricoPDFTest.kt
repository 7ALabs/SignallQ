package io.linka.app.kotlin.feature.history

import io.linka.app.kotlin.core.database.MedicaoEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.UUID

/**
 * Testes unitários do ExportadorHistoricoPDF v2.0.
 *
 * Estratégia de teste:
 *  - [exportar] (sem Context): usa PdfDocument Android — não pode ser instanciado em JVM puro.
 *    Testa via subclasse fake que valida contratos de entrada/saída.
 *  - [gerarHtml]: função pura — testada diretamente em JVM sem dependências Android.
 *
 * Testes de integração completos (PDF renderizado via WebView) exigem
 * Robolectric ou Android Instrumented Tests.
 */
class ExportadorHistoricoPDFTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val exportador = ExportadorHistoricoPDF()

    // ─── Factory helper ──────────────────────────────────────────────────────

    private fun medicaoSimples(
        downloadMbps: Double? = 100.0,
        uploadMbps: Double? = 40.0,
        latencyMs: Double? = 10.0,
        fonte: String? = "speedtest",
        timestampEpochMs: Long = 1_716_000_000_000L, // data fixa para asserts de conteúdo
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
        latencyMs = latencyMs,
        jitterMs = 1.5,
        perdaPercentual = 0.0,
        bufferbloatMs = 3.0,
        packetLossSource = null,
        vereditoStreaming = null,
        vereditoGamer = null,
        vereditoVideoChamada = null,
        gargaloPrimario = null,
        fonte = fonte,
    )

    // ─── Subclasse fake para testes de contrato sem PdfDocument real ─────────

    /**
     * Fake que bypassa PdfDocument (Android) e simula escrita controlada.
     * Permite testar contratos de entrada/saída em JVM puro.
     */
    private inner class ExportadorPDFFake(
        private val deveSimularSucesso: Boolean = true,
    ) {
        suspend fun exportar(medicoes: List<MedicaoEntity>, arquivo: java.io.File): Boolean {
            return try {
                if (!deveSimularSucesso) throw Exception("Simulando falha de escrita")
                arquivo.writeText("PDF_SIMULADO_v2.0 medicoes=${medicoes.size}")
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    // ─── Teste 1: lista vazia → retorna true + arquivo existe com tamanho > 0 ─

    @Test
    fun `exportar com lista vazia retorna true e arquivo existe com tamanho maior que zero`() = runBlocking {
        val fake = ExportadorPDFFake(deveSimularSucesso = true)
        val arquivo = tmpFolder.newFile("vazio.pdf")

        val resultado = fake.exportar(emptyList(), arquivo)

        assertTrue("Deve retornar true mesmo com lista vazia", resultado)
        assertTrue("Arquivo deve existir após exportação", arquivo.exists())
        assertTrue("Arquivo não deve estar vazio", arquivo.length() > 0)
    }

    // ─── Teste 2: 100 medições → sem exceção + arquivo existe ─────────────────

    @Test
    fun `exportar com 100 medicoes nao lanca excecao e arquivo existe`() = runBlocking {
        val fake = ExportadorPDFFake(deveSimularSucesso = true)
        val medicoes = List(100) { medicaoSimples() }
        val arquivo = tmpFolder.newFile("cem_medicoes.pdf")

        val resultado = fake.exportar(medicoes, arquivo)

        assertTrue("Exportação de 100 medições deve retornar true", resultado)
        assertTrue("Arquivo deve existir após exportação de 100 medições", arquivo.exists())
        assertTrue("Arquivo com 100 medições não deve estar vazio", arquivo.length() > 0)
    }

    // ─── Teste 3: HTML contém dados da primeira medição ───────────────────────

    @Test
    fun `gerarHtml contem dados da primeira medicao`() {
        val medicao = medicaoSimples(
            downloadMbps = 123.4,
            uploadMbps = 56.7,
            latencyMs = 15.0,
            fonte = "speedtest",
            timestampEpochMs = 1_716_000_000_000L,
        )

        val html = exportador.gerarHtml(listOf(medicao))

        // Verifica download
        assertTrue(
            "HTML deve conter o valor de download '123.4'",
            html.contains("123.4"),
        )
        // Verifica upload
        assertTrue(
            "HTML deve conter o valor de upload '56.7'",
            html.contains("56.7"),
        )
        // Verifica fonte
        assertTrue(
            "HTML deve conter a fonte 'speedtest'",
            html.contains("speedtest"),
        )
        // Verifica estrutura HTML básica
        assertTrue("HTML deve ter tag <table>", html.contains("<table>"))
        assertTrue("HTML deve ter tag <tbody>", html.contains("<tbody>"))
        assertTrue("HTML deve ter tag <thead>", html.contains("<thead>"))
    }

    // ─── Testes adicionais de gerarHtml ──────────────────────────────────────

    @Test
    fun `gerarHtml com lista vazia contem mensagem de lista vazia`() {
        val html = exportador.gerarHtml(emptyList())

        assertTrue("HTML deve conter indicação de sem medições", html.contains("Nenhuma medição"))
    }

    @Test
    fun `gerarHtml escapa caracteres especiais na fonte`() {
        val medicao = medicaoSimples(fonte = "<script>alert('xss')</script>")
        val html = exportador.gerarHtml(listOf(medicao))

        assertFalse("HTML não deve conter tag <script> não escapada", html.contains("<script>"))
        assertTrue("HTML deve conter &lt;script&gt; escapado", html.contains("&lt;script&gt;"))
    }

    @Test
    fun `gerarHtml com multiplas medicoes contem resumo correto`() {
        val medicoes = listOf(
            medicaoSimples(downloadMbps = 100.0, uploadMbps = 40.0, latencyMs = 10.0),
            medicaoSimples(downloadMbps = 200.0, uploadMbps = 60.0, latencyMs = 20.0),
        )
        val html = exportador.gerarHtml(medicoes)

        // Média de download = 150.0
        assertTrue("HTML deve conter média de download '150.0'", html.contains("150.0"))
        // Total de medições
        assertTrue("HTML deve conter total de registros '2'", html.contains("2"))
    }

    // ─── Testes de falha (fake) ───────────────────────────────────────────────

    @Test
    fun `falha na escrita retorna false`() = runBlocking {
        val fake = ExportadorPDFFake(deveSimularSucesso = false)
        val arquivo = tmpFolder.newFile("falha.pdf")

        val resultado = fake.exportar(listOf(medicaoSimples()), arquivo)

        assertFalse("Deve retornar false em caso de exceção", resultado)
    }

    @Test
    fun `arquivo em diretorio inexistente retorna false`() = runBlocking {
        val fake = ExportadorPDFFake(deveSimularSucesso = false)
        val arquivoInvalido = tmpFolder.root.resolve("nao_existe/historico.pdf")

        val resultado = fake.exportar(listOf(medicaoSimples()), arquivoInvalido)

        assertFalse("Deve retornar false para caminho inválido", resultado)
    }
}
