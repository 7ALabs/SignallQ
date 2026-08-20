package io.signallq.app.ui.screen

import io.signallq.app.ui.ContatoOperadora
import io.signallq.app.ui.OperatorScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GH#1662 (Task 2.0.14, épico #1647) — cobre as decisões de produto de 2026-08-19 que não
 * dependem de Compose: (1) fallback de contato quando a operadora não é identificada no
 * catálogo local, sem esconder nem desabilitar o botão; (2) subtítulo do cabeçalho não expõe
 * mais RSRP em dBm antes da conclusão (siglas só nos detalhes, spec design 2.0 §4.3/4.4).
 */
class SinalMovelContatoOperadoraTest {
    private val vivo =
        ContatoOperadora(
            id = "vivo_fibra",
            nome = "Vivo",
            grupo = "Vivo / Telefônica",
            detectarPor = listOf("vivo"),
            sac = "10315",
            whatsapp = "11999151515",
            site = "https://www.vivo.com.br",
            scope = OperatorScope.NATIONAL,
        )

    // ── contatoOperadoraUrl ─────────────────────────────────────────────────────

    @Test
    fun `operadora resolvida no catalogo local usa o site cadastrado`() {
        val url = contatoOperadoraUrl(vivo, "Vivo")
        assertEquals("https://www.vivo.com.br", url)
    }

    @Test
    fun `operadora identificada mas fora do catalogo local cai em busca generica com o nome`() {
        val url = contatoOperadoraUrl(null, "Operadora Desconhecida XPTO")
        assertTrue(url.startsWith("https://www.google.com/search?q="))
        assertTrue(url.contains("Operadora"))
    }

    @Test
    fun `operadora nao identificada pelo Android cai em busca generica sem nome`() {
        val url = contatoOperadoraUrl(null, null)
        assertEquals(
            "https://www.google.com/search?q=central+de+atendimento+operadora+de+celular",
            url,
        )
    }

    @Test
    fun `fallback nunca fica nulo ou vazio -- botao de contato sempre tem destino`() {
        assertTrue(contatoOperadoraUrl(null, null).isNotBlank())
        assertTrue(contatoOperadoraUrl(null, "Qualquer Operadora").isNotBlank())
        assertTrue(contatoOperadoraUrl(vivo, "Vivo").isNotBlank())
    }

    // ── rotuloBotaoContatoOperadora ─────────────────────────────────────────────

    @Test
    fun `rotulo nomeia a operadora quando identificada`() {
        assertEquals("Falar com a Vivo", rotuloBotaoContatoOperadora("Vivo"))
    }

    @Test
    fun `rotulo fica generico quando operadora nao identificada`() {
        assertEquals("Falar com sua operadora", rotuloBotaoContatoOperadora(null))
    }

    // ── resumoCabecalhoMovel — conclusão precede siglas (spec design 2.0 §4.3) ──

    @Test
    fun `cabecalho normal mostra tecnologia, nunca RSRP em dBm`() {
        val dados = DadosSinalMovel(rsrpDbm = -85, rsrqDb = -11, sinrDb = 8, tecnologia = "4G", radioDesligado = false)
        val resumo = resumoCabecalhoMovel(dados, capturaReduzida = false)
        assertEquals("4G", resumo)
        assertTrue("Cabeçalho não pode conter RSRP em dBm antes da conclusão", !resumo.contains("dBm"))
    }

    @Test
    fun `cabecalho sem tecnologia cai em rotulo generico`() {
        val dados = DadosSinalMovel(rsrpDbm = null, rsrqDb = null, sinrDb = null, tecnologia = null, radioDesligado = false)
        assertEquals("Rede móvel", resumoCabecalhoMovel(dados, capturaReduzida = false))
    }

    @Test
    fun `cabecalho em captura reduzida explica que faltam detalhes por causa da permissao`() {
        val dados = DadosSinalMovel(rsrpDbm = null, rsrqDb = null, sinrDb = null, tecnologia = null, radioDesligado = false)
        val resumo = resumoCabecalhoMovel(dados, capturaReduzida = true)
        assertEquals("Detalhes completos exigem permissão de telefone", resumo)
    }

    @Test
    fun `cabecalho com radio desligado prevalece sobre captura reduzida`() {
        val dados = DadosSinalMovel(rsrpDbm = null, rsrqDb = null, sinrDb = null, tecnologia = null, radioDesligado = true)
        assertEquals(
            "Modo avião ativo · rádio celular desligado",
            resumoCabecalhoMovel(dados, capturaReduzida = true),
        )
    }
}
