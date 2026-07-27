package io.signallq.app.ui.screen

import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Issue #1503 — descoberta progressiva de Ferramentas.
 *
 * Camada A: mapeamento objetivo -> ferramenta sugerida ([ferramentaSugerida]).
 * Camada B: particionamento do hub em "Mais usadas" / "Todas as ferramentas"
 * ([CatalogoFerramentas]).
 *
 * Cobertura puramente de dados (enums/listas) — não toca Composable nem ícone, por
 * isso roda como JUnit puro, sem Robolectric.
 */
class TipoFerramentaTest {
    @Test
    fun `sites demoram sugere DNS`() {
        assertEquals(TipoFerramenta.DNS, ObjetivoDiagnostico.SITES_DEMORAM.ferramentaSugerida())
    }

    @Test
    fun `velocidade nao chega sugere DNS`() {
        assertEquals(TipoFerramenta.DNS, ObjetivoDiagnostico.VELOCIDADE_NAO_CHEGA.ferramentaSugerida())
    }

    @Test
    fun `internet cai ou oscila sugere Monitoramento`() {
        assertEquals(TipoFerramenta.MONITORAMENTO, ObjetivoDiagnostico.INTERNET_CAI_OSCILA.ferramentaSugerida())
    }

    @Test
    fun `wifi vs operadora sugere Sinal WiFi`() {
        assertEquals(TipoFerramenta.SINAL_WIFI, ObjetivoDiagnostico.WIFI_VS_OPERADORA.ferramentaSugerida())
    }

    @Test
    fun `objetivos sem mapeamento forte nao sugerem ferramenta`() {
        assertNull(ObjetivoDiagnostico.VIDEOS_TRAVAM.ferramentaSugerida())
        assertNull(ObjetivoDiagnostico.JOGOS_COM_LAG.ferramentaSugerida())
        assertNull(ObjetivoDiagnostico.CHAMADAS_CONGELAM.ferramentaSugerida())
    }

    @Test
    fun `todos os objetivos existentes estao cobertos pelo when (nenhum esquecido)`() {
        // O compilador já garante exaustividade do `when` em ferramentaSugerida() — este
        // teste trava a contagem esperada pra acusar se um objetivo novo aparecer sem
        // decisão de produto explícita sobre mapear ou não pra uma ferramenta.
        assertEquals(7, ObjetivoDiagnostico.entries.size)
    }

    @Test
    fun `mais usadas tem DNS, Modo Jogos e Monitoramento nesta ordem`() {
        assertEquals(
            listOf(TipoFerramenta.DNS, TipoFerramenta.MODO_JOGOS, TipoFerramenta.MONITORAMENTO),
            CatalogoFerramentas.maisUsadas,
        )
    }

    @Test
    fun `restante nao duplica nenhum item de mais usadas`() {
        val intersecao = CatalogoFerramentas.restante.intersect(CatalogoFerramentas.maisUsadas.toSet())
        assertTrue(intersecao.isEmpty())
    }

    @Test
    fun `restante cobre as outras 5 ferramentas na ordem canonica`() {
        assertEquals(
            listOf(
                TipoFerramenta.DISPOSITIVOS,
                TipoFerramenta.EQUIPAMENTO_INTERNET,
                TipoFerramenta.PING,
                TipoFerramenta.LAUDO,
                TipoFerramenta.SINAL_WIFI,
            ),
            CatalogoFerramentas.restante,
        )
    }

    @Test
    fun `todas as 8 ferramentas aparecem exatamente uma vez entre as duas secoes`() {
        val todasAsSecoes = CatalogoFerramentas.maisUsadas + CatalogoFerramentas.restante
        assertEquals(TipoFerramenta.entries.size, todasAsSecoes.size)
        assertEquals(TipoFerramenta.entries.toSet(), todasAsSecoes.toSet())
    }
}
