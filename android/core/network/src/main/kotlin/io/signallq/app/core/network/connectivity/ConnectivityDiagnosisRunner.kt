package io.signallq.app.core.network.connectivity

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.signallq.app.core.network.contracts.connectivity.ConnectivityDiagnosis

/**
 * Fonte de diagnóstico de conectividade -- abstrai [ConnectivityDiagnosisRunner] (a única
 * implementação real, dependente de Android) para permitir fakes triviais em quem consome
 * o diagnóstico (ex.: [io.signallq.app.feature.speedtest.connectivity.ConnectivityDiagnosisRepositoryImpl])
 * sem precisar de Robolectric/Context real em teste JVM puro.
 */
interface ConnectivityDiagnosisSource {
    /**
     * Checagem síncrona e direta (via [ConnectivityManager.getAllNetworks], nunca
     * `activeNetwork`/`MonitorRede`) de que existe uma rede Wi-Fi neste exato momento --
     * independente de qual rede o Android escolheu como *default* do processo. Usada como
     * gate barato antes de [diagnosticar] (que sempre faz sondagem ativa real, nunca confia
     * isoladamente em `NET_CAPABILITY_VALIDATED` -- GH#1512, regra obrigatória).
     */
    fun existeRedeWifiAtiva(): Boolean

    suspend fun diagnosticar(): ConnectivityDiagnosis
}

/**
 * Ponto de entrada Android do diagnóstico local de conectividade (GH#1512).
 *
 * Responsável por capturar explicitamente a `Network` do Wi-Fi sob análise — nunca a
 * rede default do processo, que pode ser dados móveis mesmo com Wi-Fi conectado — e
 * montar o [ConnectivityDiagnosisContext] a partir de [NetworkCapabilities]/
 * [android.net.LinkProperties] dessa rede específica antes de delegar a um
 * [ConnectivityDiagnosisEngine] cujas sondagens ficam amarradas a essa mesma rede.
 *
 * Diferente de [io.signallq.app.core.network.MonitorRedeAndroid] (que segue a rede
 * default via `registerDefaultNetworkCallback`, correto para "qual rede está ativa
 * agora"), este runner precisa da `Network` do Wi-Fi mesmo quando ele não é a rede
 * default -- é exatamente o cenário reportado na #1512 (Wi-Fi conectado sem internet,
 * Android pode já ter promovido dados móveis a rede default).
 */
class ConnectivityDiagnosisRunner(
    context: Context,
    private val criarEngine: (ConnectivityProbeBinding) -> ConnectivityDiagnosisEngine = { binding ->
        ConnectivityDiagnosisEngine(
            gatewayProbe = GatewayReachabilityProbe(binding),
            dnsProbe = DnsReachabilityProbe(binding),
            externalIpProbe = ExternalIpReachabilityProbe(binding),
            hostnameProbe = HostnameReachabilityProbe(binding),
        )
    },
) : ConnectivityDiagnosisSource {
    private val applicationContext = context.applicationContext
    private val connectivityManager =
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @SuppressLint("MissingPermission")
    override fun existeRedeWifiAtiva(): Boolean = capturarRedeWifi() != null

    @SuppressLint("MissingPermission")
    override suspend fun diagnosticar(): ConnectivityDiagnosis {
        val redeWifi = capturarRedeWifi()
        val mobileFallbackAvailable = existeRedeMovelComInternet()

        if (redeWifi == null) {
            // Sem Network Wi-Fi capturada: nao ha rede para amarrar sondagem nenhuma --
            // o motor recebe wifiConnected=false e conclui WIFI_DISCONNECTED sem tentar
            // nenhuma sondagem pelo caminho padrao do sistema.
            val engineSemRede = criarEngine(NenhumaRedeDisponivelBinding)
            return engineSemRede.diagnosticar(
                ConnectivityDiagnosisContext(
                    wifiConnected = false,
                    localAddressAvailable = false,
                    gatewayIp = null,
                    dnsServers = emptyList(),
                    androidInternetCapability = false,
                    androidValidated = false,
                    androidCaptivePortalCapability = false,
                    mobileFallbackAvailable = mobileFallbackAvailable,
                ),
            )
        }

        val capabilities = connectivityManager.getNetworkCapabilities(redeWifi)
        val linkProperties = connectivityManager.getLinkProperties(redeWifi)
        val gatewayIp = linkProperties?.routes
            ?.firstOrNull { it.isDefaultRoute && it.gateway != null }
            ?.gateway?.hostAddress?.takeIf { it.isNotBlank() }
        val dnsServers = linkProperties?.dnsServers
            ?.mapNotNull { it.hostAddress?.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val localAddressAvailable = linkProperties?.linkAddresses?.isNotEmpty() == true

        val engine = criarEngine(AndroidNetworkProbeBinding(redeWifi))
        return engine.diagnosticar(
            ConnectivityDiagnosisContext(
                wifiConnected = true,
                localAddressAvailable = localAddressAvailable,
                gatewayIp = gatewayIp,
                dnsServers = dnsServers,
                androidInternetCapability = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true,
                androidValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
                androidCaptivePortalCapability = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true,
                mobileFallbackAvailable = mobileFallbackAvailable,
            ),
        )
    }

    /** Rede Wi-Fi atual entre todas as redes conhecidas pelo sistema -- deliberadamente
     *  não usa `activeNetwork` (rede default, pode já não ser o Wi-Fi). */
    @SuppressLint("MissingPermission")
    private fun capturarRedeWifi(): Network? {
        return try {
            connectivityManager.allNetworks.firstOrNull { rede ->
                connectivityManager.getNetworkCapabilities(rede)
                    ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            }
        } catch (_: SecurityException) {
            null
        }
    }

    /** Presença de dados móveis como alternativa -- nunca usada para substituir o
     *  resultado do Wi-Fi sob análise (GH#1512, regra obrigatória). */
    @SuppressLint("MissingPermission")
    private fun existeRedeMovelComInternet(): Boolean {
        return try {
            connectivityManager.allNetworks.any { rede ->
                val caps = connectivityManager.getNetworkCapabilities(rede)
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        } catch (_: SecurityException) {
            false
        }
    }
}

/** Binding inerte usado apenas quando nenhuma rede Wi-Fi foi capturada -- o contexto
 *  correspondente marca `wifiConnected=false`, então o motor nunca chama nenhum método
 *  desta implementação (retorna cedo antes de sondar qualquer coisa). */
private object NenhumaRedeDisponivelBinding : ConnectivityProbeBinding {
    override fun bindSocket(socket: java.net.Socket) {
        error("NenhumaRedeDisponivelBinding nao deve ser usado -- wifiConnected=false interrompe o motor antes de sondar")
    }

    override fun resolveHost(hostname: String): Array<java.net.InetAddress> {
        error("NenhumaRedeDisponivelBinding nao deve ser usado -- wifiConnected=false interrompe o motor antes de sondar")
    }

    override fun openConnection(url: java.net.URL): java.net.URLConnection {
        error("NenhumaRedeDisponivelBinding nao deve ser usado -- wifiConnected=false interrompe o motor antes de sondar")
    }
}
