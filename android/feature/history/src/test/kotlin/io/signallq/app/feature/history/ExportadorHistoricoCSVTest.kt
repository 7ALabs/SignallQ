package io.signallq.app.feature.history

import io.signallq.app.core.database.MedicaoEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.UUID

class ExportadorHistoricoCSVTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val exportador = ExportadorHistoricoCSV()

    private fun medicaoSimples(
        timestampEpochMs: Long = System.currentTimeMillis(),
        downloadMbps: Double? = 50.0,
        uploadMbps: Double? = 20.0,
        latenciaMs: Double? = 15.0,
        jitterMs: Double? = 2.0,
        perdaPercentual: Double? = 0.0,
        bufferbloatMs: Double? = 5.0,
        fonte: String? = "speedtest",
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
        jitterMs = jitterMs,
        perdaPercentual = perdaPercentual,
        bufferbloatMs = bufferbloatMs,
        packetLossSource = null,
        vereditoStreaming = null,
        vereditoGamer = null,
        vereditoVideoChamada = null,
        gargaloPrimario = null,
        fonte = fonte,
    )

    @Test
    fun `lista vazia gera arquivo com apenas o cabecalho`() = runBlocking {
        val arquivo = tmpFolder.newFile("historico_vazio.csv")
        val resultado = exportador.exportar(emptyList(), arquivo)

        assertTrue("Deve retornar true para lista vazia", resultado)
        val linhas = arquivo.readLines()
        assertEquals("Deve ter exatamente 1 linha (cabecalho)", 1, linhas.size)
        assertTrue("Cabecalho deve conter 'Download'", linhas[0].contains("Download"))
    }

    @Test
    fun `lista com 3 medicoes gera 4 linhas (cabecalho + 3 dados)`() = runBlocking {
        val medicoes = List(3) { medicaoSimples() }
        val arquivo = tmpFolder.newFile("historico_3.csv")
        val resultado = exportador.exportar(medicoes, arquivo)

        assertTrue("Deve retornar true", resultado)
        // Remove linhas em branco do final (appendLine adiciona newline no ultimo)
        val linhas = arquivo.readLines().filter { it.isNotBlank() }
        assertEquals("Deve ter 4 linhas (1 cabecalho + 3 dados)", 4, linhas.size)
    }

    @Test
    fun `medicao com valores null gera campos vazios no CSV`() = runBlocking {
        val medicao = medicaoSimples(
            downloadMbps = null,
            uploadMbps = null,
            latenciaMs = null,
            fonte = "monitor",
        )
        val arquivo = tmpFolder.newFile("historico_null.csv")
        exportador.exportar(listOf(medicao), arquivo)

        val conteudo = arquivo.readText()
        // Deve ter virgulas consecutivas para campos null
        assertTrue(
            "Campos null devem gerar campos vazios: $conteudo",
            conteudo.contains(",,"),
        )
    }

    @Test
    fun `arquivo invalido retorna false`() = runBlocking {
        // Tenta escrever em diretorio inexistente
        val arquivoInvalido = tmpFolder.root.resolve("nao_existe/historico.csv")
        val resultado = exportador.exportar(listOf(medicaoSimples()), arquivoInvalido)
        assertFalse("Deve retornar false quando arquivo nao pode ser criado", resultado)
    }

    @Test
    fun `medicao de monitor tem campo fonte preenchido`() = runBlocking {
        val medicao = medicaoSimples(fonte = "monitor", downloadMbps = null, uploadMbps = null)
        val arquivo = tmpFolder.newFile("historico_monitor.csv")
        exportador.exportar(listOf(medicao), arquivo)

        val linhas = arquivo.readLines().filter { it.isNotBlank() }
        assertTrue("Linha de dados deve terminar com 'monitor'", linhas[1].endsWith("monitor"))
    }
}
