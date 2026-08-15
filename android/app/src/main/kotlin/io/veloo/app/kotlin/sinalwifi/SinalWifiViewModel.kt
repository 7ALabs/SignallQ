package io.signallq.app.sinalwifi

import android.annotation.SuppressLint
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive

private const val INTERVALO_AMOSTRAGEM_SINAL_WIFI_MS = 1500L
private const val RSSI_SEM_LEITURA_SENTINELA = -127
private const val FREQUENCIA_MINIMA_6GHZ_MHZ = 5945

data class SinalWifiUiState(
    val permissaoConcedida: Boolean,
    val rssiAtual: Int? = null,
    val linkSpeedMbps: Int? = null,
    val ssid: String? = null,
    val padraoWifi: String? = null,
    val suportaMuMimo: Boolean? = null,
)

/**
 * ViewModel da tela "Sinal WiFi" (GH#1201) -- hub Ferramentas. Não é `@HiltViewModel`, criado via
 * `remember{}` no Composable, mesmo padrão do [io.signallq.app.modogamer.ModoGamerViewModel]
 * (escopo do overlay, sem necessidade de sobreviver à recomposição do grafo de navegação).
 *
 * Amostra [WifiManager.getConnectionInfo] em polling periódico -- o `MonitorRede`/`NetworkCallback`
 * de `:coreNetwork` é orientado a evento e não dispara de forma confiável em variação pura de RSSI
 * dentro da mesma rede conectada (ver issue #1176).
 */
class SinalWifiViewModel(
    private val wifiManager: WifiManager,
    private val permissaoConcedida: () -> Boolean,
    // Seam de teste -- producao nunca passa isso, so os testes de amostragem usam um
    // intervalo curto pra nao esperar 1.5s reais por teste.
    private val intervaloAmostragemMs: Long = INTERVALO_AMOSTRAGEM_SINAL_WIFI_MS,
) {
    private val mutableUiState = MutableStateFlow(SinalWifiUiState(permissaoConcedida = permissaoConcedida()))
    val uiState: StateFlow<SinalWifiUiState> = mutableUiState.asStateFlow()

    suspend fun iniciarAmostragem() {
        if (!permissaoConcedida()) return
        while (currentCoroutineContext().isActive) {
            amostrar()
            delay(intervaloAmostragemMs)
        }
    }

    @SuppressLint("MissingPermission")
    private fun amostrar() {
        val info = wifiManager.connectionInfo ?: return
        val rssi = info.rssi
        // RSSI 0/-127 é o valor sentinela do Android para "sem leitura" -- descarta.
        if (rssi == 0 || rssi <= RSSI_SEM_LEITURA_SENTINELA) return
        mutableUiState.update { atual ->
            // padraoWifi/suportaMuMimo só são calculados na primeira leitura válida e
            // preservados depois -- o padrão do link não muda enquanto conectado à mesma rede.
            val padrao = atual.padraoWifi ?: calcularPadraoWifi(info)
            atual.copy(
                rssiAtual = rssi,
                linkSpeedMbps = info.linkSpeed,
                ssid = normalizarSsid(info.ssid),
                padraoWifi = padrao,
                suportaMuMimo = atual.suportaMuMimo ?: calcularSuportaMuMimo(padrao),
            )
        }
    }
}

/**
 * Réplica exata do mapeamento de `WifiInfo.wifiStandard` já implementado em
 * `MonitorRedeAndroid.capturarWifiLinkSnapshot` (`:coreNetwork`, ~linhas 211-222) -- não dá pra
 * importar direto porque lá é função privada de outra classe em outro módulo.
 */
private fun calcularPadraoWifi(info: WifiInfo): String? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
    val freq = info.frequency
    @Suppress("NewApi")
    return when (info.wifiStandard) {
        1 -> null // LEGACY (a/b/g) — sem MIMO, não exibir
        4 -> "Wi-Fi 4 (n)"
        5 -> "Wi-Fi 5 (ac)"
        6 -> if (freq >= FREQUENCIA_MINIMA_6GHZ_MHZ) "Wi-Fi 6E (ax)" else "Wi-Fi 6 (ax)"
        7 -> "WiGig (ad)"
        8 -> "Wi-Fi 7 (be)"
        else -> null
    }
}

private fun normalizarSsid(ssid: String?): String? {
    val campo = ssid?.trim().orEmpty()
    if (campo.isBlank()) return null
    if (campo.equals("<unknown ssid>", ignoreCase = true)) return null
    return campo.removePrefix("\"").removeSuffix("\"")
}

fun calcularSuportaMuMimo(padraoWifi: String?): Boolean? =
    when {
        padraoWifi == null -> null
        padraoWifi.startsWith("Wi-Fi 4") -> false // 802.11n: sem MU-MIMO
        padraoWifi.startsWith("Wi-Fi 5") -> true // 802.11ac Wave 2: MU-MIMO downlink
        padraoWifi.startsWith("Wi-Fi 6") -> true // 802.11ax (inclui 6E): MU-MIMO up+downlink, OFDMA
        padraoWifi.startsWith("Wi-Fi 7") -> true // 802.11be
        else -> null
    }
