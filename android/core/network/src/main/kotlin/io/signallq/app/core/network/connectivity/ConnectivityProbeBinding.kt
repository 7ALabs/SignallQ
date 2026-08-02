package io.signallq.app.core.network.connectivity

import java.net.InetAddress
import java.net.Socket
import java.net.URL
import java.net.URLConnection

/**
 * Fixação de sondagem à rede Wi-Fi sob análise (GH#1512, requisito obrigatório).
 *
 * Todas as sondagens de conectividade recebem uma [ConnectivityProbeBinding] em vez de
 * usar sockets/resolução/HTTP pelo caminho padrão do sistema — que pode migrar
 * silenciosamente para dados móveis quando o Wi-Fi não está validado. A implementação de
 * produção ([AndroidNetworkProbeBinding]) amarra socket, resolução DNS e HttpURLConnection
 * à `android.net.Network` capturada para o Wi-Fi específico.
 */
interface ConnectivityProbeBinding {
    /** Amarra [socket] à rede sob análise antes do `connect()`. */
    fun bindSocket(socket: Socket)

    /** Resolve [hostname] usando os servidores DNS da rede sob análise. */
    fun resolveHost(hostname: String): Array<InetAddress>

    /** Abre uma conexão HTTP(S) amarrada à rede sob análise. */
    fun openConnection(url: URL): URLConnection
}
