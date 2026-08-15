package io.signallq.app.ui.relatorio

/**
 * Mascaramento de dados sensíveis pro relatório de diagnóstico — GH#1219 item 11.
 *
 * Privacidade por padrão: os dois callers (`ResultadoPdfGenerator`, `LaudoScreen`) sempre
 * mascaram antes de montar o [RelatorioDiagnosticoSnapshot] — o builder/renderer nunca vê o
 * dado bruto. Não existe nesta entrega um toggle de usuário para desligar a máscara
 * (decisão de design/produto pendente, ver pendências registradas na issue).
 */
object RelatorioPrivacidade {
    /** Mascara o último octeto de um IPv4. Ex.: "192.168.1.100" → "192.168.1.*".
     *  IPv6 e formatos não-IPv4 são retornados sem alteração (heurística simples,
     *  suficiente pro caso comum de rede doméstica). */
    fun mascararIpLocal(ip: String?): String? {
        if (ip.isNullOrBlank()) return null
        val partes = ip.trim().split(".")
        return if (partes.size == 4) "${partes[0]}.${partes[1]}.${partes[2]}.*" else ip.trim()
    }

    /** Mascara os dois últimos octetos de um IPv4 público — mais conservador que o IP
     *  local porque o IP público sozinho já identifica a conexão do usuário. */
    fun mascararIpPublico(ip: String?): String? {
        if (ip.isNullOrBlank()) return null
        val partes = ip.trim().split(".")
        return if (partes.size == 4) "${partes[0]}.${partes[1]}.*.*" else ip.trim()
    }

    /** Mascara o SSID mantendo só os 2 primeiros caracteres — o suficiente pra identificar
     *  "é a minha rede de casa" sem expor o nome completo no PDF compartilhado. */
    fun mascararSsid(ssid: String?): String? {
        if (ssid.isNullOrBlank()) return null
        val limpo = ssid.trim()
        return if (limpo.length <= 2) "${limpo.take(1)}***" else "${limpo.take(2)}${"*".repeat((limpo.length - 2).coerceAtMost(6))}"
    }
}
