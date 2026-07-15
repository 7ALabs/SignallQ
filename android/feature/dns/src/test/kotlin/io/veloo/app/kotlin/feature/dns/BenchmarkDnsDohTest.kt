package io.signallq.app.feature.dns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class BenchmarkDnsDohTest {
    private val benchmark = BenchmarkDnsDoh()

    @Test
    fun combinarResultadosMantemProvedoresQueFalharam() {
        val sistema =
            ResultadoBenchmarkDns(
                nomeProvedor = "Google DNS",
                hostConsulta = "example.com",
                tempoMs = 14.0,
                amostrasMs = listOf(13.0, 14.0),
                tentativas = 3,
                sucessos = 2,
                taxaSucessoPercentual = 100.0,
                erroMensagem = null,
                gradeRapidez = "A",
            )
        val cloudflare =
            ResultadoBenchmarkDns(
                nomeProvedor = "Cloudflare",
                hostConsulta = "example.com",
                tempoMs = 9.0,
                amostrasMs = listOf(9.0, 10.0),
                tentativas = 3,
                sucessos = 2,
                taxaSucessoPercentual = 100.0,
                erroMensagem = null,
                gradeRapidez = "A",
            )
        val quad9Falhou =
            ResultadoBenchmarkDns(
                nomeProvedor = "Quad9",
                hostConsulta = "example.com",
                tempoMs = null,
                amostrasMs = emptyList(),
                tentativas = 3,
                sucessos = 0,
                taxaSucessoPercentual = 0.0,
                erroMensagem = "semResposta",
                gradeRapidez = null,
            )

        val resultados = benchmark.combinarResultados(sistema, listOf(quad9Falhou, cloudflare))

        assertEquals(listOf("Cloudflare", "Google DNS", "Quad9"), resultados.map { it.nomeProvedor })
        assertTrue(resultados.last().erroMensagem != null)
    }

    @Test
    fun construirDnsQueryBase64UrlGeraPayloadValido() {
        val encoded = benchmark.construirDnsQueryBase64Url("example.com")
        val padding =
            when (encoded.length % 4) {
                0 -> ""
                2 -> "=="
                3 -> "="
                else -> throw IllegalArgumentException("Base64 URL-safe invalido: $encoded")
            }
        val decoded = Base64.UrlSafe.decode(encoded + padding)

        assertTrue(encoded.isNotBlank())
        assertTrue(encoded.contains('/').not())
        assertTrue(encoded.contains('=').not())
        assertEquals(29, decoded.size)
    }
}
