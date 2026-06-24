package io.veloo.app.feature.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val TAG = "SignallQWifiScan"
private const val TIMEOUT_SCAN_MS = 10_000L

class ScannerRedesWifi(context: Context) {

    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val mutableSnapshotFlow = MutableStateFlow(
        SnapshotScanWifi(estado = EstadoScanWifi.idle, redes = emptyList(), erroMensagem = null),
    )
    val snapshotFlow: StateFlow<SnapshotScanWifi> = mutableSnapshotFlow.asStateFlow()

    suspend fun escanear() = withContext(Dispatchers.IO) {
        mutableSnapshotFlow.value = SnapshotScanWifi(EstadoScanWifi.scanning, emptyList(), null)
        try {
            val resultados = withTimeoutOrNull(TIMEOUT_SCAN_MS) { realizarScan() }
                ?: wifiManager.scanResultsCompat()
            val redes = resultados
                .sortedByDescending { it.level }
                .map { it.paraRedeVizinha() }
                .filter { it.bssid.isNotEmpty() } // Remove entradas com BSSID inválido (placeholder "00:00:00:00:00:00" filtrado em paraRedeVizinha)
            Log.i(TAG, "scan concluido: ${redes.size} redes banda2=${redes.count { it.frequenciaMhz < 3000 }} banda5=${redes.count { it.frequenciaMhz >= 3000 }}")
            mutableSnapshotFlow.value = SnapshotScanWifi(EstadoScanWifi.concluido, redes, null)
        } catch (t: Throwable) {
            val chave = when (t) {
                is SecurityException -> "semPermissaoLocalizacao"
                else -> "erroScanWifi"
            }
            Log.e(TAG, "erro no scan: $chave — ${t.message}", t)
            mutableSnapshotFlow.value = SnapshotScanWifi(EstadoScanWifi.erro, emptyList(), chave)
        }
    }

    private suspend fun realizarScan(): List<ScanResult> = suspendCancellableCoroutine { cont ->
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                try { appContext.unregisterReceiver(this) } catch (_: Exception) {}
                cont.resume(wifiManager.scanResultsCompat())
            }
        }
        cont.invokeOnCancellation {
            try { appContext.unregisterReceiver(receiver) } catch (_: Exception) {}
        }
        appContext.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        @Suppress("DEPRECATION")
        val iniciou = try { wifiManager.startScan() } catch (_: SecurityException) { false }
        if (!iniciou) {
            try { appContext.unregisterReceiver(receiver) } catch (_: Exception) {}
            cont.resume(wifiManager.scanResultsCompat())
        }
    }

    private fun WifiManager.scanResultsCompat(): List<ScanResult> =
        try { scanResults ?: emptyList() } catch (_: SecurityException) { emptyList() }
}

// Retorna string vazia para MACs inválidos/placeholder. RedeVizinha.bssid é String não-nullable;
// usamos "" como sentinela e filtramos no pipeline em escanear(). Mudar para String? quebraria 8+ callers.
private fun bssidValido(bssid: String?): String {
    if (bssid == null) return ""
    if (bssid == "02:00:00:00:00:00") return "" // Android retorna este MAC quando localização está desativada
    if (bssid == "00:00:00:00:00:00") return "" // MAC nulo/inválido
    if (bssid.all { it == '0' || it == ':' }) return "" // qualquer variante de zero
    return bssid
}

private fun ScanResult.paraRedeVizinha(): RedeVizinha {
    val ssidLimpo = extrairSsid(this)
    val cap = capabilities?.uppercase().orEmpty()
    val seguranca = when {
        cap.contains("WPA3") -> SegurancaWifi.wpa3
        cap.contains("WPA2") || cap.contains("RSN") -> SegurancaWifi.wpa2
        cap.contains("WPA") -> SegurancaWifi.wpa
        cap.contains("WEP") -> SegurancaWifi.wep
        cap.contains("[ESS]") || cap.isBlank() -> SegurancaWifi.aberta
        else -> SegurancaWifi.desconhecida
    }
    val largura = when (channelWidth) {
        ScanResult.CHANNEL_WIDTH_20MHZ -> 20
        ScanResult.CHANNEL_WIDTH_40MHZ -> 40
        ScanResult.CHANNEL_WIDTH_80MHZ -> 80
        ScanResult.CHANNEL_WIDTH_160MHZ -> 160
        ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> 160
        else -> null
    }
    val bssidStr = bssidValido(BSSID)
    val ouiStr = if (bssidStr.length >= 8) bssidStr.replace(":", "").uppercase().take(6) else ""
    return RedeVizinha(
        ssid = ssidLimpo,
        // bssidValido retorna "" para MACs nulos ou placeholder — entradas com "" são filtradas pelo caller em escanear()
        bssid = bssidStr,
        rssiDbm = level,
        frequenciaMhz = frequency,
        seguranca = seguranca,
        larguraCanalMhz = largura,
        oui = ouiStr,
    )
}

@Suppress("DEPRECATION")
private fun extrairSsid(sr: ScanResult): String? {
    val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        sr.wifiSsid?.toString()
    } else {
        sr.SSID
    }
    return raw?.trim()?.removePrefix("\"")?.removeSuffix("\"")?.ifBlank { null }
}
