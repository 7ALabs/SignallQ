package io.signallq.pro.feature.medicaodiagnostico

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.signallq.app.core.diagnostico.ConnectionType
import io.signallq.app.core.diagnostico.DiagnosticArea
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticRunner
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.InternetDiagnosticInput
import io.signallq.pro.core.database.diagnostico.AchadoParaSalvar
import io.signallq.pro.core.database.diagnostico.DiagnosticoProRepository
import io.signallq.pro.core.database.medicao.MedicaoProRepository
import io.signallq.pro.core.designsystem.RecommendationPriority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AchadoUiState(
    val titulo: String,
    val mensagem: String,
    val recomendacao: String?,
    val prioridade: RecommendationPriority,
)

enum class DiagnosticoAmbienteEstado { MEDINDO, SUCESSO, ERRO }

data class DiagnosticoAmbienteUiState(
    val estado: DiagnosticoAmbienteEstado = DiagnosticoAmbienteEstado.MEDINDO,
    val veredito: String = "",
    val scoreConexao: Int = 0,
    val achados: List<AchadoUiState> = emptyList(),
    val mensagemErro: String? = null,
)

/**
 * Telas 2.15-2.16 -- diagnostico usando o motor puro [DiagnosticRunner] (`:core:diagnostico`,
 * ja extraido na Fase 1). Analisa a ultima medicao valida do ambiente -- nao dispara novo
 * speedtest (handoff Fase 2, #1161: "explicar previamente o que sera medido" ja aconteceu
 * na tela 2.10).
 */
@HiltViewModel
class DiagnosticoAmbienteViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val medicaoProRepository: MedicaoProRepository,
        private val diagnosticoProRepository: DiagnosticoProRepository,
    ) : ViewModel() {
        val ambienteId: String = checkNotNull(savedStateHandle["ambienteId"])

        private val _uiState = MutableStateFlow(DiagnosticoAmbienteUiState())
        val uiState: StateFlow<DiagnosticoAmbienteUiState> = _uiState

        init {
            executarDiagnostico()
        }

        fun executarDiagnostico() {
            _uiState.update { DiagnosticoAmbienteUiState(estado = DiagnosticoAmbienteEstado.MEDINDO) }
            viewModelScope.launch {
                val medicao = medicaoProRepository.buscarUltima(ambienteId)
                if (medicao == null) {
                    _uiState.update {
                        it.copy(
                            estado = DiagnosticoAmbienteEstado.ERRO,
                            mensagemErro = "Nenhuma medicao encontrada para este ambiente.",
                        )
                    }
                    return@launch
                }
                val input =
                    DiagnosticInput(
                        connectionType = ConnectionType.wifi,
                        internet =
                            InternetDiagnosticInput(
                                downloadMbps = medicao.downloadMbps,
                                uploadMbps = medicao.uploadMbps,
                                latencyMs = medicao.latenciaMs,
                                jitterMs = medicao.jitterMs,
                                perdaPercentual = medicao.perdaPercentual,
                            ),
                    )
                val report =
                    DiagnosticRunner.run(
                        input = input,
                        enabledAreas = setOf(DiagnosticArea.VELOCIDADE, DiagnosticArea.LATENCIA),
                    )
                val achados =
                    (listOf(report.decisao) + report.achadosSecundarios).map { resultado ->
                        AchadoUiState(
                            titulo = resultado.titulo,
                            mensagem = resultado.mensagemUsuario,
                            recomendacao = resultado.recomendacao,
                            prioridade = mapPrioridade(resultado.status),
                        )
                    }
                diagnosticoProRepository.salvarResultado(
                    ambienteId = ambienteId,
                    medicaoId = medicao.id,
                    veredito = report.veredito,
                    scoreConexao = report.scoreConexao,
                    decisaoTitulo = report.decisao.titulo,
                    decisaoMensagem = report.decisao.mensagemUsuario,
                    achados =
                        achados.mapIndexed { indice, achado ->
                            AchadoParaSalvar(
                                titulo = achado.titulo,
                                mensagem = achado.mensagem,
                                recomendacao = achado.recomendacao,
                                status = report.decisao.status.name,
                                principal = indice == 0,
                            )
                        },
                )
                _uiState.update {
                    it.copy(
                        estado = DiagnosticoAmbienteEstado.SUCESSO,
                        veredito = report.veredito,
                        scoreConexao = report.scoreConexao,
                        achados = achados,
                    )
                }
            }
        }

        private fun mapPrioridade(status: DiagnosticStatus): RecommendationPriority =
            when (status) {
                DiagnosticStatus.critical -> RecommendationPriority.CRITICO
                DiagnosticStatus.attention -> RecommendationPriority.ATENCAO
                else -> RecommendationPriority.INFO
            }
    }
