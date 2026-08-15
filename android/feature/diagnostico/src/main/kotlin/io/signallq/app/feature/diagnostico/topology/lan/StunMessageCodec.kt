package io.signallq.app.feature.diagnostico.topology.lan

/**
 * Codec de mensagens STUN (RFC 5389) — lógica pura de bytes, sem I/O, usada por
 * [StunNatProbe] para montar o Binding Request e interpretar o Binding Response.
 *
 * Cobre só o necessário para a detecção simplificada de NAT Type da aba Jogos: um
 * Binding Request sem atributos e a leitura de XOR-MAPPED-ADDRESS/MAPPED-ADDRESS
 * (IPv4) do Binding Response. Não implementa autenticação (MESSAGE-INTEGRITY),
 * fingerprint (FINGERPRINT) nem os demais tipos de mensagem do RFC — fora do
 * escopo desta issue (#1200).
 */
object StunMessageCodec {
    private const val BINDING_REQUEST = 0x0001
    private const val BINDING_SUCCESS = 0x0101
    private const val MAGIC_COOKIE = 0x2112A442.toInt()
    private const val ATTR_MAPPED_ADDRESS = 0x0001
    private const val ATTR_XOR_MAPPED_ADDRESS = 0x0020
    private const val FAMILY_IPV4 = 0x01
    private const val HEADER_SIZE = 20
    private const val TRANSACTION_ID_SIZE = 12

    /**
     * Monta um Binding Request de 20 bytes (só o header, sem atributos):
     * tipo (2) + comprimento=0 (2) + magic cookie (4) + transaction ID (12).
     */
    fun buildBindingRequest(transactionId: ByteArray): ByteArray {
        require(transactionId.size == TRANSACTION_ID_SIZE) {
            "transactionId deve ter $TRANSACTION_ID_SIZE bytes, veio ${transactionId.size}"
        }
        val buffer = ByteArray(HEADER_SIZE)
        writeUShort(buffer, 0, BINDING_REQUEST)
        writeUShort(buffer, 2, 0) // comprimento do corpo — sem atributos
        writeUInt(buffer, 4, MAGIC_COOKIE)
        System.arraycopy(transactionId, 0, buffer, 8, TRANSACTION_ID_SIZE)
        return buffer
    }

    /**
     * Interpreta um Binding Response. Retorna `null` quando: o pacote é menor que o
     * header, o tipo não é BINDING_SUCCESS, o magic cookie não bate, o transaction ID
     * não bate com o request original (proteção contra resposta de request antigo ou
     * spoof), ou nenhum atributo de endereço IPv4 suportado foi encontrado.
     */
    fun parseBindingResponse(bytes: ByteArray, length: Int, transactionId: ByteArray): StunBindingResult? {
        if (length < HEADER_SIZE) return null

        val tipo = readUShort(bytes, 0)
        if (tipo != BINDING_SUCCESS) return null

        val magicCookie = readUInt(bytes, 4)
        if (magicCookie != MAGIC_COOKIE) return null

        for (i in 0 until TRANSACTION_ID_SIZE) {
            if (bytes[8 + i] != transactionId[i]) return null
        }

        val comprimentoCorpo = readUShort(bytes, 2)
        val fimCorpo = minOf(HEADER_SIZE + comprimentoCorpo, length)

        var xorMapped: StunBindingResult? = null
        var mappedLegado: StunBindingResult? = null

        var offset = HEADER_SIZE
        while (offset + 4 <= fimCorpo) {
            val attrType = readUShort(bytes, offset)
            val attrLength = readUShort(bytes, offset + 2)
            val valorInicio = offset + 4
            val valorFim = valorInicio + attrLength
            if (valorFim > fimCorpo) break // atributo declarado maior que o corpo — pacote malformado

            when (attrType) {
                ATTR_XOR_MAPPED_ADDRESS ->
                    xorMapped = parseXorMappedAddress(bytes, valorInicio, attrLength)
                ATTR_MAPPED_ADDRESS ->
                    mappedLegado = parseMappedAddress(bytes, valorInicio, attrLength)
            }

            // Atributos são alinhados em múltiplos de 4 bytes (padding no fim do valor).
            val attrLengthComPadding = attrLength + ((4 - (attrLength % 4)) % 4)
            offset = valorInicio + attrLengthComPadding
        }

        return xorMapped ?: mappedLegado
    }

    /** RFC 5389 §15.2 — porta e endereço vêm com XOR contra o magic cookie. */
    private fun parseXorMappedAddress(bytes: ByteArray, inicio: Int, tamanho: Int): StunBindingResult? {
        if (tamanho < 8) return null
        val family = bytes[inicio + 1].toInt() and 0xFF
        if (family != FAMILY_IPV4) return null // IPv6 fora de escopo

        val portaXor = readUShort(bytes, inicio + 2)
        val porta = portaXor xor ((MAGIC_COOKIE ushr 16) and 0xFFFF)

        val enderecoBytes = ByteArray(4)
        for (i in 0 until 4) {
            val byteCookie = (MAGIC_COOKIE ushr (24 - (i * 8))) and 0xFF
            enderecoBytes[i] = ((bytes[inicio + 4 + i].toInt() and 0xFF) xor byteCookie).toByte()
        }

        return StunBindingResult(enderecoMapeado = enderecoBytes.paraIpv4Texto(), portaMapeada = porta)
    }

    /** MAPPED-ADDRESS legado (RFC 3489) — sem XOR, formato de fallback. */
    private fun parseMappedAddress(bytes: ByteArray, inicio: Int, tamanho: Int): StunBindingResult? {
        if (tamanho < 8) return null
        val family = bytes[inicio + 1].toInt() and 0xFF
        if (family != FAMILY_IPV4) return null

        val porta = readUShort(bytes, inicio + 2)
        val enderecoBytes = ByteArray(4) { i -> bytes[inicio + 4 + i] }
        return StunBindingResult(enderecoMapeado = enderecoBytes.paraIpv4Texto(), portaMapeada = porta)
    }

    private fun ByteArray.paraIpv4Texto(): String = joinToString(".") { (it.toInt() and 0xFF).toString() }

    private fun writeUShort(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = ((value ushr 8) and 0xFF).toByte()
        buffer[offset + 1] = (value and 0xFF).toByte()
    }

    private fun writeUInt(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = ((value ushr 24) and 0xFF).toByte()
        buffer[offset + 1] = ((value ushr 16) and 0xFF).toByte()
        buffer[offset + 2] = ((value ushr 8) and 0xFF).toByte()
        buffer[offset + 3] = (value and 0xFF).toByte()
    }

    private fun readUShort(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)

    private fun readUInt(bytes: ByteArray, offset: Int): Int =
        ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
}

data class StunBindingResult(val enderecoMapeado: String, val portaMapeada: Int)
