package io.signallq.app.feature.fibra

/**
 * GH#1213 item 2 (Nokia G-1425G-B) — antes o [NokiaModemClient] aceitava qualquer string
 * como host e tentava se conectar direto, sem nenhuma validação. `modemHost` é uma
 * preferência persistida sem nenhum ponto de escrita hoje (sempre cai no fallback do
 * gateway detectado via scan de rede — ver `MainViewModel.host`), mas a ausência de
 * validação é uma dívida real: qualquer escrita futura nessa preferência (ou em outro
 * ponto de entrada) mandaria credenciais reais pra qualquer host, sem checagem.
 *
 * Validação por **string literal de IP**, nunca resolvendo hostname via DNS: resolver um
 * hostname só pra validar (e depois abrir conexão separadamente) introduziria uma janela de
 * DNS rebinding e uma chamada de rede desnecessária dentro de uma função de validação. Um
 * driver de equipamento LAN sempre recebe IP, não hostname — exigir IP literal é a
 * restrição correta aqui, não uma limitação.
 */
internal object ValidadorHostEquipamento {
    fun ehIpPrivadoValido(host: String): Boolean {
        if (host.isBlank()) return false
        return ehIpv4Privado(host) || ehIpv6PrivadoOuLocal(host)
    }

    private fun ehIpv4Privado(host: String): Boolean {
        val partes = host.split(".")
        if (partes.size != 4) return false
        val octetos = partes.mapNotNull { it.toIntOrNull() }
        if (octetos.size != 4 || octetos.any { it !in 0..255 }) return false
        val (a, b) = octetos
        return when {
            a == 10 -> true
            a == 172 && b in 16..31 -> true
            a == 192 && b == 168 -> true
            a == 169 && b == 254 -> true // link-local
            a == 127 -> true // loopback
            else -> false
        }
    }

    private fun ehIpv6PrivadoOuLocal(host: String): Boolean {
        if (!host.contains(':')) return false
        val normalizado = host.lowercase().removePrefix("[").removeSuffix("]")
        return when {
            normalizado == "::1" -> true // loopback
            normalizado.startsWith("fe80:") -> true // link-local
            normalizado.startsWith("fc") || normalizado.startsWith("fd") -> true // ULA fc00::/7
            else -> false
        }
    }
}
