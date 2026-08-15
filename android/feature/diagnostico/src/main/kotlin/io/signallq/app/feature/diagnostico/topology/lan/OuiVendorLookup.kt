package io.signallq.app.feature.diagnostico.topology.lan

import android.content.Context

class OuiVendorLookup(private val openStream: () -> java.io.InputStream?) {

    companion object {
        fun fromAssets(context: Context): OuiVendorLookup =
            OuiVendorLookup { context.assets.open("oui.txt") }
    }

    // Lazy — carregado uma vez; seguro para múltiplos lookups.
    private val table: Map<String, String> by lazy { loadTable() }

    fun lookup(mac: String): String? {
        val oui = mac.uppercase().replace(":", "").replace("-", "").take(6)
        return table[oui]
    }

    private fun loadTable(): Map<String, String> = try {
        openStream()?.bufferedReader()?.useLines { lines ->
            lines.associate { line ->
                val tab = line.indexOf('\t')
                if (tab < 0) "" to "" else line.substring(0, tab).uppercase() to line.substring(tab + 1).trim()
            }.filterKeys { it.length == 6 }
        } ?: emptyMap()
    } catch (_: Exception) { emptyMap() }
}
