package io.signallq.app.feature.diagnostico.topology.lan

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * O fixture de XOR-MAPPED-ADDRESS abaixo é o "Sample IPv4 Response" do RFC 5769 §2.2
 * (Binding Success Response com atributos SOFTWARE + XOR-MAPPED-ADDRESS = 192.0.2.1:32853,
 * transaction ID `b7e7a701bc34d686fa87dfae`) — o vetor de teste padrão usado por praticamente
 * toda implementação STUN de referência para validar o XOR de porta/endereço do RFC 5389
 * §15.2. Reproduzido aqui só com SOFTWARE + XOR-MAPPED-ADDRESS (sem MESSAGE-INTEGRITY/
 * FINGERPRINT, que este codec não valida). O fixture de MAPPED-ADDRESS legado (sem XOR) foi
 * montado manualmente — não há um vetor RFC 5769 equivalente para esse atributo — com um
 * endereço/porta arbitrários, já que a codificação legada é byte a byte direta (sem XOR).
 */
class StunMessageCodecTest {

    private val transactionIdRfc5769 =
        byteArrayOf(
            0xb7.toByte(), 0xe7.toByte(), 0xa7.toByte(), 0x01,
            0xbc.toByte(), 0x34, 0xd6.toByte(), 0x86.toByte(),
            0xfa.toByte(), 0x87.toByte(), 0xdf.toByte(), 0xae.toByte(),
        )

    /** Binding Success Response com SOFTWARE + XOR-MAPPED-ADDRESS = 192.0.2.1:32853. */
    private val respostaXorMappedAddress =
        byteArrayOf(
            // header: tipo=0x0101, length=0x001c (28 bytes de corpo)
            0x01, 0x01, 0x00, 0x1c,
            // magic cookie
            0x21, 0x12, 0xa4.toByte(), 0x42,
            // transaction ID
            0xb7.toByte(), 0xe7.toByte(), 0xa7.toByte(), 0x01,
            0xbc.toByte(), 0x34, 0xd6.toByte(), 0x86.toByte(),
            0xfa.toByte(), 0x87.toByte(), 0xdf.toByte(), 0xae.toByte(),
            // SOFTWARE (0x8022, length=11) — testa que o parser pula atributo desconhecido
            0x80.toByte(), 0x22, 0x00, 0x0b,
            0x74, 0x65, 0x73, 0x74,
            0x20, 0x76, 0x65, 0x63,
            0x74, 0x6f, 0x72, 0x20,
            // XOR-MAPPED-ADDRESS (0x0020, length=8): família IPv4, porta e endereço com XOR
            0x00, 0x20, 0x00, 0x08,
            0x00, 0x01, 0xa1.toByte(), 0x47,
            0xe1.toByte(), 0x12, 0xa6.toByte(), 0x43,
        )

    /** Binding Success Response só com MAPPED-ADDRESS legado (sem XOR) = 203.0.113.5:8080. */
    private val transactionIdLegado = ByteArray(12) { (it + 1).toByte() }
    private val respostaMappedAddressLegado =
        byteArrayOf(
            // header: tipo=0x0101, length=0x000c (12 bytes de corpo)
            0x01, 0x01, 0x00, 0x0c,
            0x21, 0x12, 0xa4.toByte(), 0x42,
            *transactionIdLegado,
            // MAPPED-ADDRESS (0x0001, length=8): família IPv4, porta=8080, endereço 203.0.113.5 — sem XOR
            0x00, 0x01, 0x00, 0x08,
            0x00, 0x01, 0x1f, 0x90.toByte(),
            0xcb.toByte(), 0x00, 0x71, 0x05,
        )

    @Test
    fun `buildBindingRequest produz header de 20 bytes com tipo comprimento zero magic cookie e transaction id`() {
        val request = StunMessageCodec.buildBindingRequest(transactionIdRfc5769)

        assertEquals(20, request.size)
        assertArrayEquals(byteArrayOf(0x00, 0x01), request.copyOfRange(0, 2)) // tipo: Binding Request
        assertArrayEquals(byteArrayOf(0x00, 0x00), request.copyOfRange(2, 4)) // comprimento: sem atributos
        assertArrayEquals(byteArrayOf(0x21, 0x12, 0xa4.toByte(), 0x42), request.copyOfRange(4, 8)) // magic cookie
        assertArrayEquals(transactionIdRfc5769, request.copyOfRange(8, 20))
    }

    @Test
    fun `buildBindingRequest rejeita transaction id com tamanho diferente de 12 bytes`() {
        try {
            StunMessageCodec.buildBindingRequest(ByteArray(10))
            error("deveria ter lancado IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // esperado
        }
    }

    @Test
    fun `parseBindingResponse extrai IP e porta do XOR-MAPPED-ADDRESS desfazendo o XOR do RFC 5389`() {
        val resultado =
            StunMessageCodec.parseBindingResponse(
                respostaXorMappedAddress,
                respostaXorMappedAddress.size,
                transactionIdRfc5769,
            )

        assertEquals(StunBindingResult(enderecoMapeado = "192.0.2.1", portaMapeada = 32853), resultado)
    }

    @Test
    fun `parseBindingResponse cai para MAPPED-ADDRESS legado quando nao ha XOR-MAPPED-ADDRESS`() {
        val resultado =
            StunMessageCodec.parseBindingResponse(
                respostaMappedAddressLegado,
                respostaMappedAddressLegado.size,
                transactionIdLegado,
            )

        assertEquals(StunBindingResult(enderecoMapeado = "203.0.113.5", portaMapeada = 8080), resultado)
    }

    @Test
    fun `parseBindingResponse retorna null quando o transaction id nao bate`() {
        val transactionIdErrado = ByteArray(12) { 0x00 }

        val resultado =
            StunMessageCodec.parseBindingResponse(
                respostaXorMappedAddress,
                respostaXorMappedAddress.size,
                transactionIdErrado,
            )

        assertNull(resultado)
    }

    @Test
    fun `parseBindingResponse retorna null quando o tipo nao e BINDING_SUCCESS`() {
        val respostaComTipoErrado = respostaXorMappedAddress.copyOf()
        respostaComTipoErrado[0] = 0x01
        respostaComTipoErrado[1] = 0x11 // tipo inexistente, != 0x0101

        val resultado =
            StunMessageCodec.parseBindingResponse(
                respostaComTipoErrado,
                respostaComTipoErrado.size,
                transactionIdRfc5769,
            )

        assertNull(resultado)
    }

    @Test
    fun `parseBindingResponse retorna null quando o magic cookie nao bate`() {
        val respostaComCookieErrado = respostaXorMappedAddress.copyOf()
        respostaComCookieErrado[4] = 0x00

        val resultado =
            StunMessageCodec.parseBindingResponse(
                respostaComCookieErrado,
                respostaComCookieErrado.size,
                transactionIdRfc5769,
            )

        assertNull(resultado)
    }

    @Test
    fun `parseBindingResponse retorna null quando o pacote e menor que o header`() {
        val pacoteCurto = ByteArray(10)

        assertNull(StunMessageCodec.parseBindingResponse(pacoteCurto, pacoteCurto.size, transactionIdRfc5769))
    }
}
