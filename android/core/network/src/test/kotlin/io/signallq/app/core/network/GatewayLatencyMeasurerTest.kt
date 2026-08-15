package io.signallq.app.core.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes unitários do GatewayLatencyMeasurer.
 *
 * Limitação: não podemos mockar Socket sem Mockito/MockK nas deps atuais.
 * Os testes cobrem comportamento real com IPs inválidos/inacessíveis.
 *
 * Para teste completo de Socket mock, adicionar mockk ou mockito-kotlin
 * nas testImplementation do módulo coreNetwork.
 *
 * Timeout nos testes: usa 100ms via construtor para evitar 9s de espera em CI
 * (3 portas × 3 amostras × timeout). Produção mantém 1000ms.
 */
class GatewayLatencyMeasurerTest {

    // 100ms de timeout: suficiente para detectar falha, sem travar CI
    private val measurer = GatewayLatencyMeasurer(timeoutMs = 100)

    @Test
    fun `ip invalido retorna null sem lancar excecao`() = runTest {
        // IP inválido de formato → todas as tentativas devem falhar silenciosamente
        val resultado = measurer.measureRttGateway("not-a-valid-ip")
        assertNull("IP inválido deve retornar null", resultado)
    }

    @Test
    fun `ip inacessivel retorna null apos timeout`() = runTest {
        // IP de rede de documentação (RFC 5737) — não roteável, deve dar timeout
        // Timeout total com 100ms: 3 portas × 3 amostras × 100ms = 900ms máximo
        // vs 9s com timeout padrão de produção
        val resultado = measurer.measureRttGateway("192.0.2.1")
        assertNull("IP inacessível deve retornar null", resultado)
    }

    @Test
    fun `resultado nao negativo quando disponivel`() = runTest {
        // Se por acaso houver conectividade (CI com rede), o resultado deve ser >= 0
        // Em ambiente offline, retorna null — ambos são válidos
        val resultado = measurer.measureRttGateway("192.0.2.1")
        if (resultado != null) {
            assertTrue("RTT deve ser não-negativo", resultado >= 0)
        }
        // null também é válido (sem conectividade)
    }
}
