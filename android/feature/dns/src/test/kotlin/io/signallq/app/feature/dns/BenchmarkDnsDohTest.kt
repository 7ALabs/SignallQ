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

    // ── GH#1212 item 4 — mediana correta (antes era sorted[size/2], que pra 2 amostras
    // devolve a MAIOR, não uma mediana real) ──────────────────────────────────────────

    @Test
    fun `mediana com quantidade par de amostras faz media dos dois centrais`() {
        // sorted[size/2] antigo devolveria 20.0 (o segundo, maior) em vez da media real.
        assertEquals(15.0, benchmark.calcularMediana(listOf(10.0, 20.0))!!, 0.0001)
    }

    @Test
    fun `mediana com quantidade impar de amostras usa o valor central`() {
        assertEquals(20.0, benchmark.calcularMediana(listOf(30.0, 10.0, 20.0))!!, 0.0001)
    }

    @Test
    fun `mediana de lista vazia e nula`() {
        assertEquals(null, benchmark.calcularMediana(emptyList()))
    }

    // ── GH#1212 item 5 — taxa de sucesso sobre tentativas avaliadas reais, nao mais
    // hardcoded sobre 2 ──────────────────────────────────────────────────────────────

    @Test
    fun `taxa de sucesso usa tentativas avaliadas reais, nao denominador fixo`() {
        // 4 sucessos em 5 tentativas avaliadas = 80%, nao mais fixo sobre "2".
        assertEquals(80.0, benchmark.taxaSucesso(sucessos = 4, tentativasAvaliadas = 5), 0.0001)
    }

    @Test
    fun `taxa de sucesso zero quando nao ha tentativas avaliadas`() {
        assertEquals(0.0, benchmark.taxaSucesso(sucessos = 0, tentativasAvaliadas = 0), 0.0001)
    }

    // ── GH#1212 item 6 — validacao semantica do corpo da resposta DNS, nao so HTTP 200 ──

    private fun cabecalhoDns(
        rcode: Int,
        ancount: Int,
    ): ByteArray {
        val bytes = ByteArray(12)
        bytes[3] = rcode.toByte()
        bytes[6] = ((ancount shr 8) and 0xFF).toByte()
        bytes[7] = (ancount and 0xFF).toByte()
        return bytes
    }

    @Test
    fun `resposta com RCODE NOERROR e answer valida como sucesso`() {
        val resultado = benchmark.validarRespostaDnsBinaria(cabecalhoDns(rcode = 0, ancount = 1))
        assertEquals(ValidacaoDns.Valida, resultado)
    }

    @Test
    fun `resposta NXDOMAIN nao e tratada como sucesso mesmo com HTTP 200`() {
        val resultado = benchmark.validarRespostaDnsBinaria(cabecalhoDns(rcode = 3, ancount = 0))
        assertEquals(ValidacaoDns.Nxdomain, resultado)
    }

    @Test
    fun `resposta SERVFAIL nao e tratada como sucesso`() {
        val resultado = benchmark.validarRespostaDnsBinaria(cabecalhoDns(rcode = 2, ancount = 0))
        assertEquals(ValidacaoDns.Servfail, resultado)
    }

    @Test
    fun `resposta NOERROR sem nenhum answer nao e sucesso`() {
        val resultado = benchmark.validarRespostaDnsBinaria(cabecalhoDns(rcode = 0, ancount = 0))
        assertEquals(ValidacaoDns.SemResposta, resultado)
    }

    @Test
    fun `corpo menor que cabecalho DNS e malformado`() {
        val resultado = benchmark.validarRespostaDnsBinaria(ByteArray(4))
        assertEquals(ValidacaoDns.Malformada, resultado)
    }
}
