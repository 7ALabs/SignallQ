package io.signallq.app.feature.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val TIMEOUT_SCAN_MS = 10_000L

class ScannerRedesWifi(context: Context) {

    private val appContext = context.applicationContext
    private val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val mutableSnapshotFlow = MutableStateFlow(
        SnapshotScanWifi(estado = EstadoScanWifi.idle, redes = emptyList(), erroMensagem = null),
    )
    val snapshotFlow: StateFlow<SnapshotScanWifi> = mutableSnapshotFlow.asStateFlow()

    /**
     * Escaneia redes Wi-Fi vizinhas. Ignora silenciosamente chamadas concorrentes
     * (guard abaixo) — chamador do auto-refresh (#893) e pull-to-refresh/retry manual
     * podem colidir, e um scan duplicado só desperdiça o orçamento apertado de
     * scans do Android (throttle: 4/2min em foreground, ver skill regras-android).
     *
     * Erro ou novo scan em andamento NUNCA apagam a última lista de redes válida —
     * só o [EstadoScanWifi] muda. Sem isso, cada refresh automático (#893) fazia a
     * tela piscar vazia por alguns instantes e um erro temporário derrubava os
     * dados já exibidos.
     */
    suspend fun escanear() = withContext(Dispatchers.IO) {
        if (mutableSnapshotFlow.value.estado == EstadoScanWifi.scanning) return@withContext
        val redesAnteriores = mutableSnapshotFlow.value.redes
        mutableSnapshotFlow.value = SnapshotScanWifi(EstadoScanWifi.scanning, redesAnteriores, null)
        try {
            val resultados = withTimeoutOrNull(TIMEOUT_SCAN_MS) { realizarScan() }
                ?: wifiManager.scanResultsCompat()
            val redes = resultados
                .sortedByDescending { it.level }
                .map { it.paraRedeVizinha() }
                .filter { it.bssid.isNotEmpty() } // Remove entradas com BSSID inválido (placeholder "00:00:00:00:00:00" filtrado em paraRedeVizinha)
            Timber.i("scan concluido: ${redes.size} redes banda2=${redes.count { it.frequenciaMhz < 3000 }} banda5=${redes.count { it.frequenciaMhz >= 3000 }}")
            mutableSnapshotFlow.value = SnapshotScanWifi(EstadoScanWifi.concluido, redes, null)
        } catch (t: Throwable) {
            val chave = when (t) {
                is SecurityException -> "semPermissaoLocalizacao"
                else -> "erroScanWifi"
            }
            Timber.e(t, "erro no scan: $chave — ${t.message}")
            mutableSnapshotFlow.value = SnapshotScanWifi(EstadoScanWifi.erro, redesAnteriores, chave)
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
