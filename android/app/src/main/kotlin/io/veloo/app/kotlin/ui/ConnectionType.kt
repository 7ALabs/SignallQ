package io.signallq.app.ui

/**
 * GH#1226 item 10/RF-J — antes a tela Operadoras considerava rede móvel só quando
 * `connectionType == "movel"` (string livre). Valores como `mobile`/`4G`/`5G`/`celular` ou um
 * enum serializado de outra fonte cairiam silenciosamente como rede fixa, usando o nome de ISP
 * errado no lugar da operadora do SIM.
 *
 * O produtor real de hoje (`EstadoConexao.name`, ver `MainViewModel`) só emite
 * "movel"/"wifi"/"ethernet"/"desconectado"/"desconhecido" — então o bug descrito não se
 * manifesta com a fonte atual — mas o tipo formal remove a dependência de um literal de
 * string solto e cobre qualquer variação futura (motor de diagnóstico, PDF, histórico) sem
 * precisar caçar todo `== "movel"` espalhado pelo código.
 */
enum class ConnectionType {
    WIFI,
    ETHERNET,
    MOBILE,
    UNKNOWN,
    ;

    companion object {
        fun parse(raw: String?): ConnectionType {
            val normalizado = raw?.trim()?.lowercase() ?: return UNKNOWN
            return when {
                normalizado == "wifi" -> WIFI
                normalizado == "ethernet" -> ETHERNET
                normalizado == "movel" ||
                    normalizado == "mobile" ||
                    normalizado == "celular" ||
                    normalizado.contains("2g") ||
                    normalizado.contains("3g") ||
                    normalizado.contains("4g") ||
                    normalizado.contains("5g") -> MOBILE
                else -> UNKNOWN
            }
        }
    }
}
