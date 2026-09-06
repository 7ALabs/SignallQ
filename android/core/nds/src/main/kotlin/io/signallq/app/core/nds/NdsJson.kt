package io.signallq.app.core.nds

import org.json.JSONArray
import org.json.JSONObject

// =============================================================================
// Helpers de conversao JSON -> estruturas Kotlin puras.
// =============================================================================
// NdsModuleResult.result/cards viram Map<String, Any?>/List<Map<String, Any?>>
// em vez de guardar org.json.JSONObject/JSONArray direto: JSONObject nao
// implementa equals/hashCode estrutural (so referencia), o que quebraria
// comparacao de data class em teste. Map/List padrao do Kotlin comparam por
// valor e mantem o contrato extensivel para modulos futuros sem depender de
// org.json fora deste arquivo.

internal fun JSONObject.toKotlinMap(): Map<String, Any?> {
    val map = LinkedHashMap<String, Any?>()
    val keysIterator = keys()
    while (keysIterator.hasNext()) {
        val key = keysIterator.next()
        map[key] = convertJsonValue(opt(key))
    }
    return map
}

internal fun JSONArray.toKotlinList(): List<Any?> {
    val list = mutableListOf<Any?>()
    for (i in 0 until length()) {
        list.add(convertJsonValue(opt(i)))
    }
    return list
}

private fun convertJsonValue(value: Any?): Any? =
    when (value) {
        null, JSONObject.NULL -> null
        is JSONObject -> value.toKotlinMap()
        is JSONArray -> value.toKotlinList()
        else -> value
    }

/** `null` quando a chave nao existe ou e JSON null — nunca lanca excecao. */
internal fun JSONObject.optStringOrNull(name: String): String? {
    if (!has(name) || isNull(name)) return null
    return optString(name, "")
}
