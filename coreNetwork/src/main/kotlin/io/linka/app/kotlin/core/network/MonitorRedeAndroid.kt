package io.linka.app.kotlin.core.network

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TransportInfo
import android.net.wifi.WifiInfo
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MonitorRedeAndroid(
    context: Context,
) : MonitorRede {
    private val applicationContext = context.applicationContext
    private val connectivityManager =
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val locationManager =
        applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val mutableSnapshotFlow = MutableStateFlow(calcularSnapshotAtual())
    override val snapshotFlow: StateFlow<SnapshotRede> = mutableSnapshotFlow.asStateFlow()

    private var callbackRegistrado = false

    private val callbackRede = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            atualizarSnapshot()
        }

        override fun onLost(network: Network) {
            atualizarSnapshot()
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
        } catch (_: SecurityException) {
            mutableSnapshotFlow.value = SnapshotRede.desconectado(System.currentTimeMillis())
        }
    }

    override fun encerrar() {
        if (!callbackRegistrado) return

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

        val conectado = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val linkProperties = connectivityManager.getLinkProperties(network)
        val privateDnsHostname = linkProperties?.privateDnsServerName?.trim()?.ifBlank { null }
        val dnsServidores =
            linkProperties
                ?.dnsServers
                ?.mapNotNull { endereco -> endereco.hostAddress?.trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty()
        val locationAtivado = estaLocalizacaoAtivada()
        return SnapshotRede(
            estadoConexao = estadoConexao,
            conectado = conectado,
            timestampEpochMs = agora,
            wifiLinkSnapshot = if (estadoConexao == EstadoConexao.wifi) capturarWifiLinkSnapshot(caps, locationAtivado) else null,
            privateDnsAtivo = privateDnsHostname != null,
            privateDnsHostname = privateDnsHostname,
            dnsServidores = dnsServidores,
            locationAtivado = locationAtivado,
        )
    }

    private fun capturarWifiLinkSnapshot(networkCapabilities: NetworkCapabilities, locationAtivado: Boolean): WifiLinkSnapshot? {
        return try {
            val transportInfo: TransportInfo = networkCapabilities.transportInfo ?: return null
            val wifiInfo = transportInfo as? WifiInfo ?: return null
            val freq = wifiInfo.frequency
            // Quando localização está desativada, o Android retorna 02:00:00:00:00:00 — não é confiável.
            val bssidConfiavel = if (locationAtivado) bssidValido(wifiInfo.bssid) else null
            WifiLinkSnapshot(
                ssid = normalizarSsid(wifiInfo.ssid),
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
