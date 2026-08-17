package io.signallq.app.ui.screen

import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.feature.speedtest.MeasurementStatus
import io.signallq.app.ui.component.corContainer
import io.signallq.app.ui.component.corConteudo
import io.signallq.app.ui.lightTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes da ponte `MeasurementStatus → ContinuidadeMedicao` — GH#1705, rodada 2 da PR #1723.
 *
 * Existem porque o bloqueio B4 de Caio mostrou quatro mutantes sobrevivendo à suíte, e um deles
 * era **o argumento central da PR**: fixar `corContainer` em `warningContainer` para todos os
 * status — exatamente o defeito que a fatia dizia ter removido — passava verde. Remover um defeito
 * sem deixar nada impedindo alguém de recolocá-lo não é remover.
 *
 * A ponte é decisão desta fatia, não herança de `DiagnosticStatusUi` (ver o cabeçalho de
 * `ContinuidadeMedicao.kt`, ressalva RS1), e por isso é testada campo a campo.
 */
class ContinuidadeMedicaoTest {
    private val naoCompletos =
        MeasurementStatus.entries.filter { it != MeasurementStatus.COMPLETE }

    @Test
    fun `completo nao tem continuidade`() {
        assertNull(continuidadeDaMedicao(MeasurementStatus.COMPLETE))
    }

    // Mutante M1 do parecer: `corContainer(c)` fixo em `warningContainer` para todos.
    // Mutante M2: `INCONCLUSIVE` apresentado como `attention` em vez de `inconclusive`.
    //
    // Os dois sobreviviam. O segundo é a confusão entre "problema detectado" e "não medi" que o
    // GH#1228 P0-8 resolveu para os outros consumidores — e que a #1705 alegava estar corrigindo.
    @Test
    fun `inconclusivo nao se apresenta com o mesmo peso visual de um problema detectado`() {
        val inconclusivo = continuidadeDaMedicao(MeasurementStatus.INCONCLUSIVE)!!
        val contaminado = continuidadeDaMedicao(MeasurementStatus.CONTAMINATED)!!

        assertEquals(DiagnosticStatus.inconclusive, inconclusivo.statusVisual)
        assertEquals(DiagnosticStatus.attention, contaminado.statusVisual)
        assertTrue(
            "ausência de dado e problema detectado não podem compartilhar o mesmo status visual",
            inconclusivo.statusVisual != contaminado.statusVisual,
        )
    }

    @Test
    fun `cancelado se apresenta como informacao, nao como alarme`() {
        val cancelado = continuidadeDaMedicao(MeasurementStatus.CANCELLED)!!

        assertEquals(DiagnosticStatus.info, cancelado.statusVisual)
    }

    // Mutante M4 do parecer: copiar a `explicacao` de um status para outro. Sobrevivia porque só o
    // título estava travado por teste, e o corpo do texto é o que de fato instrui a pessoa.
    @Test
    fun `cada status tem titulo e explicacao proprios`() {
        val continuidades = naoCompletos.map { continuidadeDaMedicao(it)!! }

        assertEquals(
            "títulos repetidos entre status",
            continuidades.size,
            continuidades.map { it.titulo }.toSet().size,
        )
        assertEquals(
            "explicações repetidas entre status",
            continuidades.size,
            continuidades.map { it.explicacao }.toSet().size,
        )
    }

    /**
     * O critério de aceite literal da #1705: "nenhum dos 4 estados termina sem ação possível". Era
     * o beco sem saída do banner anterior, que era só texto.
     */
    @Test
    fun `nenhum status termina sem acao`() {
        naoCompletos.forEach { status ->
            val continuidade = continuidadeDaMedicao(status)
            assertNotNull("status $status ficou sem continuidade", continuidade)
            assertTrue("status $status ficou sem ação", continuidade!!.rotuloAcao.isNotBlank())
        }
    }

    // Bloqueio B3. `PARTIAL` sai de duas causas achatadas num valor só; quando vem do 429, o
    // download foi derrubado pelo NOSSO rate limit e o número está artificialmente baixo. Mostrar
    // conclusão ali faz o motor dizer que a pessoa recebe uma fração do plano e o card de ações
    // mandá-la acionar a operadora — com um número que estragamos.
    @Test
    fun `parcial so mostra conclusao quando o download e confiavel`() {
        val comDownloadBom = continuidadeDaMedicao(MeasurementStatus.PARTIAL, downloadConfiavel = true)!!
        val comDownloadEstragado = continuidadeDaMedicao(MeasurementStatus.PARTIAL, downloadConfiavel = false)!!

        assertTrue(comDownloadBom.permiteVerConclusaoParcial)
        assertFalse(comDownloadEstragado.permiteVerConclusaoParcial)
    }

    // Ressalva RS2: `CANCELLED` não gera `ResultadoSpeedtest` (o coordinator o mapeia para
    // "failed"), então não há conclusão parcial a preservar — e a copy não pode prometer que há.
    @Test
    fun `cancelado nao promete conclusao parcial que nao existe`() {
        val cancelado = continuidadeDaMedicao(MeasurementStatus.CANCELLED)!!

        assertFalse(cancelado.permiteVerConclusaoParcial)
        assertFalse(
            "não prometer que guardamos medição que o executor não produz",
            cancelado.explicacao.contains("Guardei"),
        )
    }

    // Mutantes M1 e M5 do parecer: fixar `corContainer`/`corConteudo` em `warningContainer`/
    // `onWarningContainer` para todos os status. Os dois sobreviviam — e M1 é literalmente o
    // defeito que esta fatia alegava ter removido do banner anterior.
    //
    // O que continua descoberto é o CALL SITE (alguém parar de chamar `coresDaContinuidade` dentro
    // do Composable). Mesma classe de resíduo declarada na PR #1719 para o `AppShell`.
    @Test
    fun `a cor vem do mapeamento canonico, nao de um valor fixo`() {
        val c = lightTokens()

        val inconclusivo = coresDaContinuidade(continuidadeDaMedicao(MeasurementStatus.INCONCLUSIVE)!!, c)
        val contaminado = coresDaContinuidade(continuidadeDaMedicao(MeasurementStatus.CONTAMINATED)!!, c)
        val cancelado = coresDaContinuidade(continuidadeDaMedicao(MeasurementStatus.CANCELLED)!!, c)

        // Iguais ao mapeamento canônico — não a uma cor escolhida aqui.
        assertEquals(DiagnosticStatus.attention.corContainer(c), contaminado.container)
        assertEquals(DiagnosticStatus.attention.corConteudo(c), contaminado.conteudo)
        assertEquals(DiagnosticStatus.inconclusive.corContainer(c), inconclusivo.container)

        // E de fato distintos entre si: uma cor fixa para todos passaria em tudo acima se o
        // mapeamento canônico colapsasse, então a distinção é asserida explicitamente.
        assertTrue(
            "inconclusivo e contaminado não podem dividir a mesma cor de container",
            inconclusivo.container != contaminado.container,
        )
        assertTrue(
            "cancelado e contaminado não podem dividir a mesma cor de container",
            cancelado.container != contaminado.container,
        )
    }

    // Ressalva RS3: vocabulário do público, não o nosso. "amostras" é jargão interno.
    @Test
    fun `nenhuma explicacao usa jargao interno`() {
        val jargao = listOf("amostra", "bufferbloat", "jitter", "throughput", "payload")

        naoCompletos.forEach { status ->
            val texto = continuidadeDaMedicao(status)!!.explicacao.lowercase()
            jargao.forEach { termo ->
                assertFalse("status $status vazou '$termo': \"$texto\"", texto.contains(termo))
            }
        }
    }
}
