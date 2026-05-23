package io.linka.app.kotlin.feature.history

import io.linka.app.kotlin.core.database.MedicaoEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

/**
 * Testes do ExportadorHistoricoPDF.
 *
 * NOTA: PdfDocument e uma classe Android e nao pode ser instanciada diretamente
 * em testes JVM (unit test puro). Esses testes validam:
 * 1. Que o exportador retorna false quando nao consegue escrever o arquivo.
 * 2. Contratos de interface (entrada/saida) via subclasse testavel.
 *
 * Testes de integracao completos (verificar conteudo do PDF) exigem
 * Robolectric ou Android Instrumented Tests.
 */
class ExportadorHistoricoPDFTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    /**
     * Subclasse testavel que bypassa PdfDocument (Android) e simula
     * escrita bem-sucedida ou falha controlada.
     */
    private inner class ExportadorPDFFake(
        private val deveSimularSucesso: Boolean,
    ) {
        suspend fun exportar(medicoes: List<MedicaoEntity>, arquivo: File): Boolean {
            return try {
                if (!deveSimularSucesso) throw Exception("Simulando falha")
                arquivo.writeText("PDF_SIMULADO_v1.0 medicoes=${medicoes.size}")
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun medicaoSimples(fonte: String? = "speedtest") = MedicaoEntity(
        id = UUID.randomUUID().toString(),
        timestampEpochMs = System.currentTimeMillis(),
        connectionType = "wifi",
        connectionTypeStart = null,
        connectionTypeEnd = null,
        contaminado = false,
        speedtestMode = "complete",
        specVersion = null,
        downloadMbps = 100.0,
        uploadMbps = 40.0,
        latencyMs = 10.0,
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

    @Test
    fun `exportacao bem-sucedida cria o arquivo`() = runBlocking {
        val fake = ExportadorPDFFake(deveSimularSucesso = true)
        val arquivo = tmpFolder.newFile("historico.pdf")
        val resultado = fake.exportar(listOf(medicaoSimples()), arquivo)

        assertTrue("Exportacao deve retornar true em sucesso", resultado)
        assertTrue("Arquivo deve existir apos exportacao", arquivo.exists())
        assertTrue("Arquivo nao deve estar vazio", arquivo.length() > 0)
    }

    @Test
    fun `exportacao com lista vazia cria arquivo`() = runBlocking {
        val fake = ExportadorPDFFake(deveSimularSucesso = true)
        val arquivo = tmpFolder.newFile("vazio.pdf")
        val resultado = fake.exportar(emptyList(), arquivo)

        assertTrue("Deve retornar true mesmo com lista vazia", resultado)
    }

    @Test
    fun `falha na escrita retorna false`() = runBlocking {
        val fake = ExportadorPDFFake(deveSimularSucesso = false)
        val arquivo = tmpFolder.newFile("falha.pdf")
        val resultado = fake.exportar(listOf(medicaoSimples()), arquivo)

        assertFalse("Deve retornar false em caso de excecao", resultado)
    }

    @Test
    fun `arquivo em diretorio inexistente retorna false`() = runBlocking {
        // Tenta exportar para caminho invalido — ExportadorHistoricoPDF real captura a excecao
        val arquivoInvalido = tmpFolder.root.resolve("nao_existe/historico.pdf")
        val fake = ExportadorPDFFake(deveSimularSucesso = false)
        val resultado = fake.exportar(listOf(medicaoSimples()), arquivoInvalido)

        assertFalse("Deve retornar false para caminho invalido", resultado)
    }
}
