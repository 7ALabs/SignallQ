package io.signallq.app.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * GH#1502 (revisão independente da PR #1515) -- cobre [formatarIdentificacaoServidorDns],
 * a correção da construção "Tempo para localizar sites (8.8.8.8)": o servidor DNS nunca
 * deve parecer parte da frase principal do rótulo, e sim uma linha secundária opcional.
 */
class DetalhesTecnicosScreenTest {
    @Test
    fun `sem nome e sem ip nao produz sublinha`() {
        assertNull(formatarIdentificacaoServidorDns(nome = null, ip = null))
    }

    @Test
    fun `nome e ip em branco tambem nao produzem sublinha`() {
        assertNull(formatarIdentificacaoServidorDns(nome = "  ", ip = ""))
    }

    @Test
    fun `so ipv4 disponivel mostra apenas o ip`() {
        assertEquals("Servidor DNS: 8.8.8.8", formatarIdentificacaoServidorDns(nome = null, ip = "8.8.8.8"))
    }

    @Test
    fun `so ipv6 disponivel mostra apenas o ip sem tratamento especial`() {
        assertEquals(
            "Servidor DNS: 2001:4860:4860::8888",
            formatarIdentificacaoServidorDns(nome = null, ip = "2001:4860:4860::8888"),
        )
    }

    @Test
    fun `so nome conhecido disponivel mostra apenas o nome`() {
        assertEquals("Servidor DNS: Google DNS", formatarIdentificacaoServidorDns(nome = "Google DNS", ip = null))
    }

    @Test
    fun `nome e ip disponiveis mostram nome com ip entre parenteses numa linha so`() {
        assertEquals(
            "Servidor DNS: Google DNS (8.8.8.8)",
            formatarIdentificacaoServidorDns(nome = "Google DNS", ip = "8.8.8.8"),
        )
    }

    @Test
    fun `nome e ip identicos nao duplicam o valor`() {
        assertEquals(
            "Servidor DNS: 8.8.8.8",
            formatarIdentificacaoServidorDns(nome = "8.8.8.8", ip = "8.8.8.8"),
        )
    }

    @Test
    fun `rotulo nunca contem parenteses vazios`() {
        val resultado = formatarIdentificacaoServidorDns(nome = null, ip = null)
        assertNull(resultado)
        val comIp = formatarIdentificacaoServidorDns(nome = null, ip = "1.1.1.1")
        assertEquals(false, comIp?.contains("()"))
    }
}
