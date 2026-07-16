package io.signallq.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Regressão GH#411 — operadora "Oi" era resolvida/exibida como "Nio" porque o
 * matcher usava `String.contains` puro sobre termos curtos, permitindo colisão
 * de substring entre nomes parecidos (ex.: "oi" dentro de outra palavra).
 *
 * A Oi (fibra/fixo) foi rebrandeada para Nio — não existe mais como marca separada
 * no catálogo (2026-07-14). "oi"/"oi fibra"/"telemar" agora resolvem para o cadastro
 * "nio", cobrindo dado legado de roteador/API que ainda reporta o nome antigo.
 */
class BancoOperadorasTest {
    @Test
    fun `ISP oi resolve para operadora Nio (rebrand), nunca um cadastro Oi separado`() {
        val resultado = BancoOperadoras.resolver("Oi")
        assertEquals("nio", resultado?.id)
        assertEquals("Nio Fibra", resultado?.nome)
    }

    @Test
    fun `ISP oi fibra resolve para operadora Nio (rebrand)`() {
        val resultado = BancoOperadoras.resolver("Oi Fibra")
        assertEquals("nio", resultado?.id)
    }

    @Test
    fun `ISP telemar resolve para operadora Nio (rebrand)`() {
        val resultado = BancoOperadoras.resolver("Telemar Norte Leste S.A")
        assertEquals("nio", resultado?.id)
    }

    @Test
    fun `ISP nio resolve para operadora Nio, nunca Oi`() {
        val resultado = BancoOperadoras.resolver("Nio")
        assertEquals("nio", resultado?.id)
        assertEquals("Nio Fibra", resultado?.nome)
    }

    @Test
    fun `ISP nio internet resolve para operadora Nio`() {
        val resultado = BancoOperadoras.resolver("Nio Internet Ltda")
        assertEquals("nio", resultado?.id)
    }

    @Test
    fun `nome que contem nio como substring sem ser palavra inteira nao resolve para Nio`() {
        // "condomínio" contém a substring "nio", mas não é a palavra "nio" isolada.
        val resultado = BancoOperadoras.resolver("Provedor Condominio Digital")
        assertNull(resultado)
    }

    @Test
    fun `nome vazio ou nulo retorna null`() {
        assertNull(BancoOperadoras.resolver(null))
        assertNull(BancoOperadoras.resolver(""))
        assertNull(BancoOperadoras.resolver("   "))
    }

    @Test
    fun `ISP vivo resolve corretamente e nao colide com outras operadoras`() {
        val resultado = BancoOperadoras.resolver("Telefonica Brasil S.A")
        assertEquals("vivo_fibra", resultado?.id)
    }

    @Test
    fun `ISP giga mais com caractere especial no termo resolve corretamente`() {
        val resultado = BancoOperadoras.resolver("Giga+ Fibra Sumicity")
        assertEquals("giga_mais", resultado?.id)
    }

    @Test
    fun `todas as operadoras do catalogo possuem site oficial preenchido`() {
        BancoOperadoras.lista.forEach { operadora ->
            assertEquals(true, operadora.site.startsWith("https://"))
        }
    }

    @Test
    fun `operadora identificada resolve com site oficial correto`() {
        val resultado = BancoOperadoras.resolver("Vivo")
        assertEquals("https://www.vivo.com.br", resultado?.site)
    }

    // ─── resolverMovel — nomes de operadora móvel chegam concatenados, sem separador ──

    @Test
    fun `nome de operadora movel concatenado TIMBRASIL resolve para TIM`() {
        val resultado = BancoOperadoras.resolverMovel("TIMBRASIL")
        assertEquals("tim_live", resultado?.id)
    }

    @Test
    fun `nome de operadora movel VIVO resolve para Vivo`() {
        val resultado = BancoOperadoras.resolverMovel("VIVO")
        assertEquals("vivo_fibra", resultado?.id)
    }

    @Test
    fun `nome de operadora movel Claro BR resolve para Claro`() {
        val resultado = BancoOperadoras.resolverMovel("Claro BR")
        assertEquals("claro_net", resultado?.id)
    }

    @Test
    fun `nome de operadora movel OI nao resolve (Nio e fibra-only, sem produto movel)`() {
        // A Nio (rebrand da Oi fixa) nao tem produto movel conhecido sob a mesma marca --
        // "Oi Movel" seria uma operadora separada, de grupo diferente, nao cadastrada aqui.
        assertNull(BancoOperadoras.resolverMovel("OI"))
    }

    @Test
    fun `nome de operadora movel desconhecida nao resolve`() {
        assertNull(BancoOperadoras.resolverMovel("Algum MVNO"))
    }

    @Test
    fun `nome de operadora movel vazio ou nulo retorna null`() {
        assertNull(BancoOperadoras.resolverMovel(null))
        assertNull(BancoOperadoras.resolverMovel(""))
        assertNull(BancoOperadoras.resolverMovel("   "))
    }
}
