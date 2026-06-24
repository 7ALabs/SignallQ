package io.veloo.app.feature.devices

/**
 * Parser de XML de descrição de dispositivo UPnP/DLNA.
 *
 * Extrai campos relevantes do device description XML retornado pela URL do header
 * LOCATION nas respostas SSDP M-SEARCH. Não usa DOM/SAX para evitar dependência de
 * parser XML completo — usa regex simples que toleram XML malformado de firmwares
 * embarcados (capitalização variável, namespaces, atributos extras em tags).
 *
 * Exposto como objeto singleton para facilitar testes unitários sem Context.
 */
internal object XmlDescricaoUpnpParser {

    /** Dados extraídos do XML de descrição UPnP. */
    data class Descricao(
        val friendlyName: String,
        val manufacturer: String,
        val modelName: String,
    )

    /**
     * Parseia o XML e retorna [Descricao] ou null se o XML não contiver friendlyName válido.
     *
     * Case-insensitive para tolerar `<FriendlyName>` vs `<friendlyName>`.
     * Suporta atributos nas tags (ex: `<friendlyName lang="pt">Sala</friendlyName>`).
     */
    fun parsear(xml: String): Descricao? {
        if (xml.isBlank()) return null

        val friendlyName = extrairTag(xml, "friendlyName")
        if (friendlyName.isBlank()) return null

        return Descricao(
            friendlyName = friendlyName,
            manufacturer = extrairTag(xml, "manufacturer"),
            modelName = extrairTag(xml, "modelName"),
        )
    }

    /**
     * Extrai o conteúdo textual da primeira ocorrência de [tag] no [xml].
     * Suporta atributos na tag de abertura. Case-insensitive.
     */
    private fun extrairTag(xml: String, tag: String): String {
        val regex = Regex("<$tag[^>]*>([^<]*)</$tag>", RegexOption.IGNORE_CASE)
        return regex.find(xml)?.groupValues?.getOrNull(1)?.trim() ?: ""
    }
}
