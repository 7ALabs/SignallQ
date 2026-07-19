package io.signallq.pro.feature.medicaodiagnostico

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.signallq.app.core.network.contracts.wifi.channel.Band
import io.signallq.app.core.network.contracts.wifi.channel.freqToChannel
import io.signallq.app.core.permissions.EstadoPermissao
import io.signallq.app.core.permissions.GerenciadorPermissoesRedeAndroid
import io.signallq.pro.core.database.walktest.WalkTestProRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val INTERVALO_AMOSTRAGEM_MS = 1500L
private const val JANELA_HISTORICO_LEITURAS = 20 // ~30s a 1.5s por leitura
private const val VARIACAO_ESTAVEL_MAXIMA_DBM = 6
private const val RSSI_SEM_LEITURA_SENTINELA = -127

enum class QualidadeRssi { EXCELENTE, BOA, REGULAR, FRACA }

data class LeituraRssi(
    val timestampMs: Long,
    val rssiDbm: Int,
)

data class WalkTestUiState(
    val permissaoConcedida: Boolean = true,
    val rssiAtual: Int? = null,
    val qualidade: QualidadeRssi? = null,
    val estavel: Boolean = true,
    val ssid: String? = null,
    val canalBanda: String? = null,
    val linkSpeedMbps: Int? = null,
    val historico: List<LeituraRssi> = emptyList(),
    val rssiMinSessao: Int? = null,
    val rssiMaxSessao: Int? = null,
    val tempoSessaoFormatado: String = "00:00",
    val deltaPontoCandidatoDbm: Int? = null,
    val roamingNaSessao: Boolean = false,
    val pontosSalvosNaSessao: Int = 0,
)

/**
 * Tela 2.11 -- Walk Test: amostra RSSI real via [WifiManager] em polling periódico (o
 * [io.signallq.app.core.network.MonitorRede] do `:coreNetwork` é orientado a evento de
 * `NetworkCallback`, não dispara em variação pura de RSSI dentro da mesma rede conectada --
 * não serve para "caminhar e ver o gráfico mudar"; decisão registrada na issue #1176).
 */
@HiltViewModel
class WalkTestViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        @ApplicationContext private val context: Context,
        private val walkTestProRepository: WalkTestProRepository,
    ) : ViewModel() {
        val ambienteId: String = checkNotNull(savedStateHandle["ambienteId"])

        private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

        private val _uiState = MutableStateFlow(WalkTestUiState())
        val uiState: StateFlow<WalkTestUiState> = _uiState

        private val inicioSessaoMs = System.currentTimeMillis()
        private var sessaoIniciada = false
        private var ultimoBssid: String? = null

        fun iniciarSessao() {
            if (sessaoIniciada) return
            sessaoIniciada = true

            val permissaoConcedida =
                GerenciadorPermissoesRedeAndroid(context).avaliar().localizacaoFina == EstadoPermissao.concedida
            _uiState.update { it.copy(permissaoConcedida = permissaoConcedida) }
            if (!permissaoConcedida) return

            viewModelScope.launch {
                while (isActive) {
                    amostrar()
                    delay(INTERVALO_AMOSTRAGEM_MS)
                }
            }
        }

        @SuppressLint("MissingPermission")
        private fun amostrar() {
            val info = wifiManager.connectionInfo ?: return
            val rssi = info.rssi
            // RSSI 0/-127 é o valor sentinela do Android para "sem leitura" -- descarta.
            if (rssi == 0 || rssi <= RSSI_SEM_LEITURA_SENTINELA) return

            val agora = System.currentTimeMillis()
            val novaLeitura = LeituraRssi(timestampMs = agora, rssiDbm = rssi)

            // Roaming real: BSSID mudou desde a leitura anterior (troca de AP dentro do mesmo
            // SSID) -- primeira leitura da sessão só fixa a referência, não conta como roaming.
            val bssidAtual = info.bssid
            val houveRoaming = ultimoBssid != null && bssidAtual != null && bssidAtual != ultimoBssid
            ultimoBssid = bssidAtual ?: ultimoBssid

            _uiState.update { estadoAtual ->
                val historico = (estadoAtual.historico + novaLeitura).takeLast(JANELA_HISTORICO_LEITURAS)
                val minSessao = minOf(rssi, estadoAtual.rssiMinSessao ?: rssi)
                val maxSessao = maxOf(rssi, estadoAtual.rssiMaxSessao ?: rssi)
                val variacaoJanela = (historico.maxOf { it.rssiDbm } - historico.minOf { it.rssiDbm })

                estadoAtual.copy(
                    rssiAtual = rssi,
                    qualidade = classificarRssi(rssi),
                    estavel = variacaoJanela <= VARIACAO_ESTAVEL_MAXIMA_DBM,
                    ssid = normalizarSsid(info.ssid),
                    canalBanda = formatarCanalBanda(info.frequency),
                    linkSpeedMbps = info.linkSpeed,
                    historico = historico,
                    rssiMinSessao = minSessao,
                    rssiMaxSessao = maxSessao,
                    tempoSessaoFormatado = formatarDuracaoSessao(agora - inicioSessaoMs),
                    deltaPontoCandidatoDbm = calcularDeltaPontoCandidato(rssi, minSessao),
                    roamingNaSessao = estadoAtual.roamingNaSessao || houveRoaming,
                )
            }
        }

        fun marcarPontoCandidato() = salvarPonto(candidato = true)

        fun salvarMedicao() = salvarPonto(candidato = false)

        private fun salvarPonto(candidato: Boolean) {
            val rssi = _uiState.value.rssiAtual ?: return
            viewModelScope.launch {
                walkTestProRepository.salvarPonto(ambienteId = ambienteId, rssiDbm = rssi, candidato = candidato)
                _uiState.update { it.copy(pontosSalvosNaSessao = it.pontosSalvosNaSessao + 1) }
            }
        }

        private fun normalizarSsid(ssidBruto: String?): String? {
            val semAspas = ssidBruto?.trim('"')
            return semAspas?.takeIf { it.isNotBlank() && it != WifiManager.UNKNOWN_SSID }
        }

        private fun formatarCanalBanda(frequenciaMhz: Int): String? {
            val (banda, canal) = freqToChannel(frequenciaMhz) ?: return null
            val rotuloBanda =
                when (banda) {
                    Band.GHZ_24 -> "2,4 GHz"
                    Band.GHZ_5 -> "5 GHz"
                    Band.GHZ_6 -> "6 GHz"
                }
            return "Canal $canal • $rotuloBanda"
        }
    }
