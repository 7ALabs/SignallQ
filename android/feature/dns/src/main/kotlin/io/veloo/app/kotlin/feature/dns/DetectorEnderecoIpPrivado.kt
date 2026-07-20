package io.signallq.app.feature.dns

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * GH#1212 item 10 — antes existiam DUAS implementações desse detector (uma privada em
 * [BenchmarkDnsDoh], outra em `DnsScreen.kt`, com comentário manual pedindo sincronia entre
 * elas), e nenhuma das duas cobria IPv6: um DNS ativo exposto só como link-local/ULA/loopback
 * IPv6 era classificado incorretamente como "DNS do provedor" em vez de "Roteador da rede".
 *
 * Usa [InetAddress] (via [java.net.Inet4Address]/[java.net.Inet6Address]) em vez de parser
 * manual de string — cobre IPv4 RFC-1918/link-local/loopback e IPv6 loopback (`::1`),
 * link-local (`fe80::/10`) e ULA (`fc00::/7`) sem depender de regex frágil.
 */
object DetectorEnderecoIpPrivado {
    fun ehPrivadoOuLocal(ip: String): Boolean {
        val endereco = runCatching { InetAddress.getByName(ip) }.getOrNull() ?: return ehPrivadoIpv4Fallback(ip)
        return when (endereco) {
            is Inet4Address ->
                endereco.isSiteLocalAddress || endereco.isLoopbackAddress || endereco.isLinkLocalAddress
            is Inet6Address ->
                endereco.isLoopbackAddress || endereco.isLinkLocalAddress || isUniqueLocalIpv6(endereco)
            else -> false
        }
    }

    // fc00::/7 (ULA) não tem um `isX` dedicado em InetAddress — checa o primeiro byte.
    private fun isUniqueLocalIpv6(endereco: Inet6Address): Boolean {
        val primeiroByte = endereco.address.getOrNull(0)?.toInt() ?: return false
        return (primeiroByte and 0xFE) == 0xFC
    }

    // Fallback só para o caso raríssimo de InetAddress.getByName falhar num literal IPv4
    // simples (ambiente de teste sem resolver, por ex.) — mantém o comportamento anterior.
    private fun ehPrivadoIpv4Fallback(ip: String): Boolean {
        val partes = ip.split(".").mapNotNull { it.toIntOrNull() }
        if (partes.size != 4) return false
        val (a, b) = partes
        return when {
            a == 10 -> true
            a == 172 && b in 16..31 -> true
            a == 192 && b == 168 -> true
            a == 169 && b == 254 -> true
            a == 127 -> true
            else -> false
        }
    }
}
