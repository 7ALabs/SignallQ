package io.signallq.app.core.network

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TransportInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

class MonitorRedeAndroid(
    context: Context,
) : MonitorRede {
    private val applicationContext = context.applicationContext
    private val connectivityManager =
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val locationManager =
        applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    // ATENÇÃO — ordem de inicialização crítica:
    // calcularSnapshotAtual() é chamado durante a inicialização de mutableSnapshotFlow.
    // Todos os campos que calcularSnapshotAtual() acessa devem ser declarados ANTES dele,
    // caso contrário serão null no momento da chamada → NullPointerException na inicialização.
    private var callbackRegistrado = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val runnableRetry = Runnable { if (callbackRegistrado) atualizarSnapshot() }

    // Contador de tentativas aguardando NET_CAPABILITY_VALIDATED.
    // Permite fallback após 1 retry: evita deixar o usuário preso como "desconectado"
    // em captive portal, VPN corporativa ou redes de operadora com proxy.
    // AtomicInteger: acesso concorrente entre main thread (Handler) e thread do ConnectivityManager.
    private val tentativasAguardandoValidated = AtomicInteger(0)

    // Inicializado após todas as dependências de calcularSnapshotAtual() acima.
    private val mutableSnapshotFlow = MutableStateFlow(calcularSnapshotAtual())
    override val snapshotFlow: StateFlow<SnapshotRede> = mutableSnapshotFlow.asStateFlow()

    // Runnable de debounce para transição ao estado desconectado.
    // onLost pode disparar antes de onAvailable durante handoff Wi-Fi → Mobile (e vice-versa).
    // Sem debounce, o usuário vê "desconectado" por 1-3 segundos mesmo com internet ativa.
    private val runnableDesconectado = Runnable { atualizarSnapshot() }

    private val callbackRede = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // Cancela debounce de desconexão pendente — a rede voltou antes da janela expirar.
            mainHandler.removeCallbacks(runnableDesconectado)
            atualizarSnapshot()
        }

        override fun onLost(network: Network) {
            // Não emite desconectado imediatamente: aguarda 2000ms para ver se onAvailable
            // chega em seguida (handoff entre redes). Se não chegar, confirma desconexão.
            mainHandler.removeCallbacks(runnableDesconectado)
            mainHandler.postDelayed(runnableDesconectado, 2000)
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            atualizarSnapshot()
        }
    }

    @SuppressLint("MissingPermission")
    override fun iniciar() {
        if (callbackRegistrado) return

        try {
            connectivityManager.registerDefaultNetworkCallback(callbackRede)
            callbackRegistrado = true
            atualizarSnapshot()
            // getNetworkCapabilities() pode retornar null transientemnte logo após o registro;
            // o retry garante o estado correto caso onAvailable ainda não tenha disparado.
            mainHandler.postDelayed(runnableRetry, 600)
        } catch (_: SecurityException) {
            mutableSnapshotFlow.value = SnapshotRede.desconectado(System.currentTimeMillis())
        }
    }

    override fun encerrar() {
        if (!callbackRegistrado) return

        mainHandler.removeCallbacks(runnableRetry)
        mainHandler.removeCallbacks(runnableDesconectado)
        try {
            connectivityManager.unregisterNetworkCallback(callbackRede)
        } catch (_: Exception) {
            // Ignora erros de unregister em estados de corrida.
        } finally {
            callbackRegistrado = false
        }
    }

    private fun atualizarSnapshot() {
        mutableSnapshotFlow.value = calcularSnapshotAtual()
    }

    @SuppressLint("MissingPermission")
    private fun calcularSnapshotAtual(): SnapshotRede {
        val agora = System.currentTimeMillis()
        val network = connectivityManager.activeNetwork ?: return SnapshotRede.desconectado(agora)
        val caps =
            connectivityManager.getNetworkCapabilities(network)
                ?: return SnapshotRede.desconectado(agora)

        val estadoConexao =
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> EstadoConexao.wifi
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> EstadoConexao.movel
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> EstadoConexao.ethernet
                else -> EstadoConexao.desconhecido
            }

        // #123: exige VALIDATED além de INTERNET para considerar conectado.
        // Fallback temporal: se INTERNET está presente mas VALIDATED ainda não,
        // agenda um retry e considera conectado após 1 tentativa — evita travar o
        // usuário como "desconectado" em captive portal, VPN corporativa ou proxy de operadora.
        // estadoConexao (wifi/movel/desconectado) não exige VALIDATED — permanece inalterado.
        val temInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val temValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val conectado = when {
            temInternet && temValidated -> {
                tentativasAguardandoValidated.set(0)
                true
            }
            temInternet && !temValidated -> {
                // VALIDATED ainda não chegou — pode ser Samsung One UI disparando onAvailable cedo,
                // captive portal, VPN ou proxy de operadora.
                if (tentativasAguardandoValidated.get() >= 1) {
                    // Já esperamos 1 ciclo de retry (600 ms) — fallback: considera conectado
                    // para não travar o usuário. onCapabilitiesChanged vai corrigir se mudar.
                    tentativasAguardandoValidated.set(0)
                    true
                } else {
                    tentativasAguardandoValidated.incrementAndGet()
                    // Agenda retry: onCapabilitiesChanged normalmente chega com VALIDATED em seguida.
                    // Se não chegar, o retry dispara e no próximo ciclo o fallback acima assume.
                    mainHandler.postDelayed(runnableRetry, 600)
                    false
                }
            }
            else -> {
                tentativasAguardandoValidated.set(0)
                false
            }
        }
        val linkProperties = connectivityManager.getLinkProperties(network)
        val privateDnsHostname = linkProperties?.privateDnsServerName?.trim()?.ifBlank { null }
        val dnsServidores =
            linkProperties
                ?.dnsServers
                ?.mapNotNull { endereco -> endereco.hostAddress?.trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty()
        val locationAtivado = estaLocalizacaoAtivada()
        // #838 — mesmo NetworkCapabilities ja obtido acima (via callback continuo, nao
        // uma consulta avulsa): fonte confiavel para o gate de confirmacao de dados moveis.
        val metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        return SnapshotRede(
            estadoConexao = estadoConexao,
            conectado = conectado,
            timestampEpochMs = agora,
            wifiLinkSnapshot = if (estadoConexao == EstadoConexao.wifi) capturarWifiLinkSnapshot(caps, locationAtivado) else null,
            privateDnsAtivo = privateDnsHostname != null,
            privateDnsHostname = privateDnsHostname,
            dnsServidores = dnsServidores,
            locationAtivado = locationAtivado,
            metered = metered,
        )
    }

    @SuppressLint("MissingPermission")
    private fun capturarWifiLinkSnapshot(networkCapabilities: NetworkCapabilities, locationAtivado: Boolean): WifiLinkSnapshot? {
        return try {
            val transportInfo: TransportInfo = networkCapabilities.transportInfo ?: return null
            val wifiInfo = transportInfo as? WifiInfo ?: return null
            val freq = wifiInfo.frequency
            var ssid = normalizarSsid(wifiInfo.ssid)
            // Quando localização está desativada, o Android retorna 02:00:00:00:00:00 — não é confiável.
            var bssidConfiavel = if (locationAtivado) bssidValido(wifiInfo.bssid) else null
            // Android 12+: transportInfo retorna SSID/BSSID redigidos sem FLAG_INCLUDE_LOCATION_INFO.
            // Fallback via WifiManager.connectionInfo que ainda funciona com ACCESS_FINE_LOCATION.
            if (ssid == null && locationAtivado) {
                try {
                    @Suppress("DEPRECATION")
                    val legacyInfo = wifiManager.connectionInfo
                    if (legacyInfo != null) {
                        ssid = normalizarSsid(legacyInfo.ssid)
                        if (bssidConfiavel == null) bssidConfiavel = bssidValido(legacyInfo.bssid)
                    }
                } catch (_: SecurityException) { /* sem permissão — ignora */ }
            }
            WifiLinkSnapshot(
                ssid = ssid,
                bssid = bssidConfiavel,
                rssiDbm = wifiInfo.rssi,
                linkSpeedMbps = wifiInfo.linkSpeed,
                frequenciaMhz = freq,
                padraoWifi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    @Suppress("NewApi")
                    when (wifiInfo.wifiStandard) {
                        1 -> null // LEGACY (a/b/g) — sem MIMO, não exibir
                        4 -> "Wi-Fi 4 (n)"
                        5 -> "Wi-Fi 5 (ac)"
                        6 -> if (freq >= 5945) "Wi-Fi 6E (ax)" else "Wi-Fi 6 (ax)"
                        7 -> "WiGig (ad)"
                        8 -> "Wi-Fi 7 (be)"
                        else -> null
                    }
                } else null,
                is5GhzCapable = try {
                    wifiManager.is5GHzBandSupported
                } catch (_: Exception) {
                    null
                },
            )
        } catch (_: SecurityException) {
            null
        }
    }

    private fun estaLocalizacaoAtivada(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }
    }

    private fun bssidValido(bssid: String?): String? {
        if (bssid == null) return null
        if (bssid == "02:00:00:00:00:00") return null // Android retorna este MAC quando localização está desativada
        if (bssid == "00:00:00:00:00:00") return null // MAC nulo/inválido
        if (bssid.all { it == '0' || it == ':' }) return null // qualquer variante de zero
        return bssid
    }

    private fun normalizarSsid(ssid: String?): String? {
        val campo = normalizarCampo(ssid) ?: return null
        if (campo.equals("<unknown ssid>", ignoreCase = true)) return null
        return campo.removePrefix("\"").removeSuffix("\"")
    }

    private fun normalizarCampo(campo: String?): String? {
        val valor = campo?.trim().orEmpty()
        return if (valor.isBlank()) null else valor
    }
}
