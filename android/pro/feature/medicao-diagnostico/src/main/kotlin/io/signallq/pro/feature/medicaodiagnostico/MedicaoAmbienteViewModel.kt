package io.signallq.pro.feature.medicaodiagnostico

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.ExecutorSpeedtest
import io.signallq.app.feature.speedtest.FeatureSpeedtestModulo
import io.signallq.app.feature.speedtest.ModoSpeedtest
import io.signallq.pro.core.database.medicao.MedicaoProRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MedicaoAmbienteEstado { OCIOSO, MEDINDO, SUCESSO, ERRO }

data class MedicaoAmbienteUiState(
    val estado: MedicaoAmbienteEstado = MedicaoAmbienteEstado.OCIOSO,
    val progressoPercentual: Int = 0,
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val latenciaMs: Double = 0.0,
    val jitterMs: Double = 0.0,
    val perdaPercentual: Double = 0.0,
    val medicaoIdSalva: String? = null,
    val mensagemErro: String? = null,
)

/**
 * Tela 2.10 -- reaproveita o motor de speedtest ja existente ([ExecutorSpeedtest]/
 * `ExecutorSpeedtestCloudflare`, `:featureSpeedtest`) via [FeatureSpeedtestModulo] (factory
 * simples, sem acoplamento a Hilt do consumidor -- confirmado reuso limpo, #1161). Medicao
 * invalida (erro/contaminada) NUNCA e salva como resultado valido.
 */
@HiltViewModel
class MedicaoAmbienteViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val medicaoProRepository: MedicaoProRepository,
    ) : ViewModel() {
        val ambienteId: String = checkNotNull(savedStateHandle["ambienteId"])

        private val executorSpeedtest: ExecutorSpeedtest = FeatureSpeedtestModulo.criarExecutorSpeedtest()

        private val _uiState = MutableStateFlow(MedicaoAmbienteUiState())
        val uiState: StateFlow<MedicaoAmbienteUiState> = _uiState

        fun iniciarMedicao() {
            _uiState.update { MedicaoAmbienteUiState(estado = MedicaoAmbienteEstado.MEDINDO) }
            executorSpeedtest.snapshotFlow
                .onEach { snapshot ->
                    _uiState.update { it.copy(progressoPercentual = snapshot.progressoPercentual) }
                }.launchIn(viewModelScope)
            viewModelScope.launch {
                executorSpeedtest.executar(modo = ModoSpeedtest.fast)
                val snapshot = executorSpeedtest.snapshotFlow.value
                val resultado = snapshot.resultado
                if (snapshot.estado != EstadoExecucaoSpeedtest.concluido || resultado == null || resultado.contaminado) {
                    _uiState.update {
                        it.copy(
                            estado = MedicaoAmbienteEstado.ERRO,
                            mensagemErro = snapshot.erroMensagem ?: "Nao foi possivel concluir a medicao. Tente novamente.",
                        )
                    }
                    return@launch
                }
                val medicaoId =
                    medicaoProRepository.salvarMedicaoValida(
                        ambienteId = ambienteId,
                        modo = ModoSpeedtest.fast.name,
                        downloadMbps = resultado.downloadMbps,
                        uploadMbps = resultado.uploadMbps,
                        latenciaMs = resultado.latenciaMs,
                        jitterMs = resultado.jitterMs,
                        perdaPercentual = resultado.perdaPercentual,
                    )
                _uiState.update {
                    it.copy(
                        estado = MedicaoAmbienteEstado.SUCESSO,
                        progressoPercentual = 100,
                        downloadMbps = resultado.downloadMbps,
                        uploadMbps = resultado.uploadMbps,
                        latenciaMs = resultado.latenciaMs,
                        jitterMs = resultado.jitterMs,
                        perdaPercentual = resultado.perdaPercentual,
                        medicaoIdSalva = medicaoId,
                    )
                }
            }
        }
    }
