package io.signallq.app.core.network.connectivity

import android.net.Network
import java.net.InetAddress
import java.net.Socket
import java.net.URL
import java.net.URLConnection

/**
 * Implementação real de [ConnectivityProbeBinding] — amarra toda sondagem à [network]
 * capturada explicitamente para o Wi-Fi sob análise (GH#1512), nunca ao caminho padrão
 * do sistema (que poderia migrar para dados móveis).
 *
 * `Network.bindSocket`/`Network.getAllByName`/`Network.openConnection` são as APIs Android
 * dedicadas a isso (todas disponíveis desde API 23 — minSdk deste app é 24).
 */
class AndroidNetworkProbeBinding(
    private val network: Network,
) : ConnectivityProbeBinding {
    override fun bindSocket(socket: Socket) {
        network.bindSocket(socket)
    }

    override fun resolveHost(hostname: String): Array<InetAddress> = network.getAllByName(hostname)

    override fun openConnection(url: URL): URLConnection = network.openConnection(url)
}
