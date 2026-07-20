package io.signallq.app.feature.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** GH#1212 item 8/12/13 — vencedor real vs. empate técnico vs. dados insuficientes. */
class AvaliadorRecomendacaoDnsTest {

    private fun resultado(
        nome: String,
        tempoMs: Double?,
        taxaSucesso: Double = 100.0,
        gatewayLocal: Boolean = false,
    ) = ResultadoBenchmarkDns(
        nomeProvedor = nome,
        hostConsulta = "example.com",
        tempoMs = tempoMs,
        amostrasMs = emptyList(),
        tentativas = 6,
        sucessos = 5,
        taxaSucessoPercentual = taxaSucesso,
        erroMensagem = null,
        gradeRapidez = null,
        isGatewayLocal = gatewayLocal,
    )

    @Test
    fun `diferenca maior que a margem produz vencedor claro`() {
        val resultados = listOf(resultado("Cloudflare", 9.0), resultado("Google DNS", 25.0))
        val recomendacao = AvaliadorRecomendacaoDns.avaliar(resultados)
        assertTrue(recomendacao is RecomendacaoDns.Vencedor)
        assertEquals("Cloudflare", (recomendacao as RecomendacaoDns.Vencedor).resultado.nomeProvedor)
    }

    @Test
    fun `diferenca de 1ms nao produz vencedor -- empate tecnico`() {
        val resultados = listOf(resultado("Cloudflare", 23.0), resultado("Google DNS", 24.0))
        val recomendacao = AvaliadorRecomendacaoDns.avaliar(resultados)
        assertTrue(recomendacao is RecomendacaoDns.EmpateTecnico)
        assertEquals(2, (recomendacao as RecomendacaoDns.EmpateTecnico).candidatos.size)
    }

    @Test
    fun `candidato com taxa de sucesso baixa nao disputa recomendacao`() {
        val resultados =
            listOf(
                resultado("Cloudflare", 5.0, taxaSucesso = 40.0),
                resultado("Google DNS", 30.0, taxaSucesso = 100.0),
            )
        val recomendacao = AvaliadorRecomendacaoDns.avaliar(resultados)
        assertTrue(recomendacao is RecomendacaoDns.Vencedor)
        // Cloudflare seria o mais rapido, mas a taxa de sucesso baixa o desqualifica.
        assertEquals("Google DNS", (recomendacao as RecomendacaoDns.Vencedor).resultado.nomeProvedor)
    }

    @Test
    fun `gateway local nunca entra na disputa mesmo sendo o mais rapido`() {
        val resultados =
            listOf(
                resultado("Roteador da rede", 1.0, gatewayLocal = true),
                resultado("Cloudflare", 20.0),
            )
        val recomendacao = AvaliadorRecomendacaoDns.avaliar(resultados)
        assertTrue(recomendacao is RecomendacaoDns.Vencedor)
        assertEquals("Cloudflare", (recomendacao as RecomendacaoDns.Vencedor).resultado.nomeProvedor)
    }

    @Test
    fun `sem candidatos confiaveis retorna dados insuficientes`() {
        val resultados = listOf(resultado("Cloudflare", null), resultado("Google DNS", null))
        val recomendacao = AvaliadorRecomendacaoDns.avaliar(resultados)
        assertEquals(RecomendacaoDns.SemDadosSuficientes, recomendacao)
    }
}
