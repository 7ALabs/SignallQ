package io.signallq.app.ui.screen

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Caracterização do validador de destino customizado do Ping (issue #1665, opção avançada
 * escondida por decisão de produto do Luiz 2026-08-19). A regra de "privado/local é inválido"
 * é a mesma já usada por `DnsScreen`/`feature/dns` ([io.signallq.app.feature.dns.DetectorEnderecoIpPrivado])
 * — este teste não reimplementa aquela regra, só confirma que o Ping a reaproveita.
 */
class ValidadorDestinoPingTest {
    @Test
    fun `normalizar remove esquema, caminho e porta`() {
        assertEquals("exemplo.com", ValidadorDestinoPing.normalizar("https://exemplo.com/caminho?x=1"))
        assertEquals("exemplo.com", ValidadorDestinoPing.normalizar("http://exemplo.com:8080/"))
        assertEquals("exemplo.com", ValidadorDestinoPing.normalizar("  exemplo.com  "))
    }

    @Test
    fun `entrada vazia e invalida`() =
        runTest {
            val resultado = ValidadorDestinoPing.validar("   ")
            assertTrue(resultado is ResultadoValidacaoDestinoPing.Invalido)
        }

    @Test
    fun `entrada com caracteres invalidos e invalida`() =
        runTest {
            val resultado = ValidadorDestinoPing.validar("não é um host!!")
            assertTrue(resultado is ResultadoValidacaoDestinoPing.Invalido)
        }

    @Test
    fun `endereco IP privado e invalido`() =
        runTest {
            val resultado = ValidadorDestinoPing.validar("192.168.0.1")
            assertTrue(resultado is ResultadoValidacaoDestinoPing.Invalido)
        }

    @Test
    fun `loopback e invalido`() =
        runTest {
            val resultado = ValidadorDestinoPing.validar("127.0.0.1")
            assertTrue(resultado is ResultadoValidacaoDestinoPing.Invalido)
        }

    @Test
    fun `localhost resolve para loopback e e invalido`() =
        runTest {
            val resultado = ValidadorDestinoPing.validar("localhost")
            assertTrue(resultado is ResultadoValidacaoDestinoPing.Invalido)
        }

    @Test
    fun `IP publico conhecido e valido`() =
        runTest {
            val resultado = ValidadorDestinoPing.validar("1.1.1.1")
            assertTrue(resultado is ResultadoValidacaoDestinoPing.Valido)
            assertEquals("1.1.1.1", (resultado as ResultadoValidacaoDestinoPing.Valido).hostNormalizado)
        }

    @Test
    fun `dominio bem formado e valido, mesmo sem resolver (RFC 2606)`() =
        runTest {
            // "teste.invalid" nunca resolve (reservado por RFC 2606, mesmo domínio usado por
            // PingExecutorTest) — o teste não depende de rede/DNS reais disponíveis no ambiente
            // de CI: falha de resolução não é tratada como "privado", só como "não verificável".
            val resultado = ValidadorDestinoPing.validar("https://teste.invalid/caminho")
            assertTrue(resultado is ResultadoValidacaoDestinoPing.Valido)
            assertEquals("teste.invalid", (resultado as ResultadoValidacaoDestinoPing.Valido).hostNormalizado)
        }
}
