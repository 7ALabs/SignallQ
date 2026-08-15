package io.signallq.app.feature.diagnostico.topology.lan

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Cobre a tabela de decisão de [classificar] — função pura, sem I/O real (nenhum socket,
 * nenhuma rede). Mesmo estilo de [io.signallq.app.core.diagnostico.topology.NatClassifierTest].
 */
class StunNatProbeClassificationTest {

    private val portaLocal = 50000
    private fun resultado(porta: Int) = StunBindingResult(enderecoMapeado = "203.0.113.10", portaMapeada = porta)

    @Test
    fun `classificar retorna BLOQUEADO quando os dois servidores nao respondem`() {
        assertEquals(NatUdpTipo.BLOQUEADO, classificar(portaLocal, null, null))
    }

    @Test
    fun `classificar retorna NAO_VERIFICADO quando so o primeiro servidor responde`() {
        assertEquals(NatUdpTipo.NAO_VERIFICADO, classificar(portaLocal, resultado(portaLocal), null))
    }

    @Test
    fun `classificar retorna NAO_VERIFICADO quando so o segundo servidor responde`() {
        assertEquals(NatUdpTipo.NAO_VERIFICADO, classificar(portaLocal, null, resultado(portaLocal)))
    }

    @Test
    fun `classificar retorna ABERTO quando as duas portas mapeadas sao iguais a porta local`() {
        assertEquals(
            NatUdpTipo.ABERTO,
            classificar(portaLocal, resultado(portaLocal), resultado(portaLocal)),
        )
    }

    @Test
    fun `classificar retorna MODERADO quando as portas mapeadas sao iguais entre si mas diferentes da porta local`() {
        assertEquals(
            NatUdpTipo.MODERADO,
            classificar(portaLocal, resultado(50123), resultado(50123)),
        )
    }

    @Test
    fun `classificar retorna RESTRITO quando as portas mapeadas diferem entre os dois servidores`() {
        assertEquals(
            NatUdpTipo.RESTRITO,
            classificar(portaLocal, resultado(50123), resultado(50456)),
        )
    }
}
