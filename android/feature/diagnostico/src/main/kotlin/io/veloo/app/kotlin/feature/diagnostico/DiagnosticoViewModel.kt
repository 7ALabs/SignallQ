package io.signallq.app.feature.diagnostico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.signallq.app.core.network.FeatureFlagProvider
import io.signallq.app.feature.diagnostico.ai.AiDiagnosisRepository
import io.signallq.app.feature.diagnostico.ai.AiDiagnosisState
import io.signallq.app.feature.diagnostico.ai.AiFallbackFactory
import io.signallq.app.feature.diagnostico.ai.DiagChatAutor
import io.signallq.app.feature.diagnostico.ai.DiagChatEntry
import io.signallq.app.feature.diagnostico.ai.DiagnosisAiContextFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsavel pelo diagnostico de rede e chat inline de diagnostico.
 *
 * Extraido do MainViewModel (1602L) — etapa C do refactor de ViewModels por feature.
 *
 * Responsabilidades:
 * - Execucao de diagnostico local via [DiagnosticOrchestrator]
 * - Chat inline de diagnostico (perguntas simples ao AI sobre o resultado)
 * - Expoe [snapshotDiagnostico] para a UI
 *
 * Nota: o Chat Diagnostico IA completo (sessoes, historico Room, drawer) continua em
 * [ChatDiagnosticoIaViewModel] — responsabilidade diferente, mais complexa.
 * Este ViewModel cuida apenas do chat "inline" que aparece diretamente na DiagnosticoScreen.
 */
@HiltViewModel
class DiagnosticoViewModel
    @Inject
    constructor(
        val diagnosticOrchestrator: DiagnosticOrchestrator,
        private val aiRepository: AiDiagnosisRepository,
        private val featureFlags: FeatureFlagProvider,
    ) : ViewModel() {
        companion object {
            private const val MAX_PERGUNTAS_USUARIO = 5
        }

        /** Snapshot do diagnostico local — observado pela DiagnosticoScreen. */
        val snapshotDiagnostico: StateFlow<SnapshotDiagnostico> = diagnosticOrchestrator.snapshotFlow

        // ── DiagChat inline ────────────────────────────────────────────────────────
        private val _diagChatHistorico = MutableStateFlow<List<DiagChatEntry>>(emptyList())
        val diagChatHistorico: StateFlow<List<DiagChatEntry>> = _diagChatHistorico

        private val _diagChatCarregando = MutableStateFlow(false)
        val diagChatCarregando: StateFlow<Boolean> = _diagChatCarregando

        private var diagAiContext: io.signallq.app.feature.diagnostico.ai.DiagnosisAiContext? = null

        /**
         * Inicia diagnostico local.
         * Requer que [internetInput] e [wifiInput] sejam populados pelo chamador
         * (MainViewModel ou Activity) que tem acesso ao MonitorRede e speedtest.
         */
        fun executarDiagnostico(
            internetInput: InternetDiagnosticInput?,
            wifiInput: WifiDiagnosticInput?,
            fibraInput: FibraDiagnosticInput? = null,
        ) {
            viewModelScope.launch {
                diagnosticOrchestrator.executar(internetInput, wifiInput, fibraInput)
            }
        }

        /** Limpa o historico do chat inline e reseta o contexto AI. */
        fun limparDiagChat() {
            _diagChatHistorico.value = emptyList()
            _diagChatCarregando.value = false
            diagAiContext = null
        }

        /**
         * Envia uma pergunta ao AI sobre o diagnostico atual.
         *
         * Limitado a [MAX_PERGUNTAS_USUARIO] perguntas por sessao de diagnostico.
         * Usa streaming SSE quando disponivel, com fallback para resposta completa.
         */
        fun enviarPerguntaDiagnostico(pergunta: String) {
            if (!featureFlags.isAiDiagnosisEnabled()) {
                _diagChatHistorico.value = _diagChatHistorico.value +
                    DiagChatEntry(
                        autor = DiagChatAutor.Ia,
                        texto = "Analise por IA temporariamente desabilitada",
                        isErro = false,
                    )
                return
            }
            val historicoAtual = _diagChatHistorico.value
            val perguntasUsuario = historicoAtual.count { it.autor == DiagChatAutor.Usuario }
            if (perguntasUsuario >= MAX_PERGUNTAS_USUARIO) return
            if (_diagChatCarregando.value) return

            viewModelScope.launch {
                _diagChatHistorico.value = historicoAtual +
                    DiagChatEntry(autor = DiagChatAutor.Usuario, texto = pergunta)
                _diagChatCarregando.value = true

                val ctx = diagAiContext ?: run {
                    val snap = diagnosticOrchestrator.snapshotFlow.value
                    val relatorio = snap.relatorio ?: run {
                        _diagChatCarregando.value = false
                        return@launch
                    }
                    val connectionType = snap.input?.connectionType ?: ConnectionType.desconhecido
                    DiagnosisAiContextFactory
                        .from(relatorio, snap.input, connectionType)
                        .also { diagAiContext = it }
                }

                val historicoContexto =
                    historicoAtual.takeLast(6).joinToString("\n") { entry ->
                        if (entry.autor == DiagChatAutor.Usuario) {
                            "Usuario: ${entry.texto.take(200)}"
                        } else {
                            "IA: ${entry.texto.take(300)}"
                        }
                    }
                val feedbackComHistorico =
                    if (historicoContexto.isNotBlank()) {
                        "Historico:\n$historicoContexto\n\nPergunta atual: $pergunta"
                    } else {
                        pergunta
                    }
                val ctxComPergunta = ctx.copy(feedbackUsuario = feedbackComHistorico.take(1000))
                val snapAtual = diagnosticOrchestrator.snapshotFlow.value
                val relatorio = snapAtual.relatorio

                val tsEntradaIa = System.currentTimeMillis()
                val entradaIa = DiagChatEntry(
                    autor = DiagChatAutor.Ia,
                    texto = "",
                    nomeModelo = "SignallQ IA",
                    isParcial = true,
                    timestamp = tsEntradaIa,
                )
                _diagChatHistorico.value = _diagChatHistorico.value + entradaIa

                var textoAcumulado = ""
                var primeiroChunk = true

                try {
                    aiRepository.explainDiagnosisStream(ctxComPergunta).collect { token ->
                        textoAcumulado += token
                        if (primeiroChunk) {
                            _diagChatCarregando.value = false
                            primeiroChunk = false
                        }
                        _diagChatHistorico.value = _diagChatHistorico.value.dropLast(1) +
                            entradaIa.copy(texto = textoAcumulado, isParcial = true)
                    }
                    if (textoAcumulado.isNotBlank()) {
                        _diagChatHistorico.value = _diagChatHistorico.value.dropLast(1) +
                            entradaIa.copy(texto = textoAcumulado, isParcial = false)
                    } else {
                        _diagChatHistorico.value = _diagChatHistorico.value.dropLast(1)
                        val resultado = if (relatorio != null) {
                            aiRepository.explainDiagnosis(ctxComPergunta) {
                                AiFallbackFactory.fromLocal(relatorio)
                            }
                        } else {
                            AiDiagnosisState.error("sem_relatorio")
                        }
                        val (textoResposta, nomeModelo, isErro) = extrairTextoDeResultado(resultado)
                        _diagChatHistorico.value = _diagChatHistorico.value +
                            DiagChatEntry(
                                autor = DiagChatAutor.Ia,
                                texto = textoResposta,
                                nomeModelo = nomeModelo,
                                isErro = isErro,
                            )
                    }
                } catch (e: Exception) {
                    _diagChatHistorico.value = _diagChatHistorico.value.dropLast(1)
                    try {
                        val resultado = if (relatorio != null) {
                            aiRepository.explainDiagnosis(ctxComPergunta) {
                                AiFallbackFactory.fromLocal(relatorio)
                            }
                        } else {
                            AiDiagnosisState.error("sem_relatorio")
                        }
                        val (textoResposta, nomeModelo, isErro) = extrairTextoDeResultado(resultado)
                        _diagChatHistorico.value = _diagChatHistorico.value +
                            DiagChatEntry(
                                autor = DiagChatAutor.Ia,
                                texto = textoResposta,
                                nomeModelo = nomeModelo,
                                isErro = isErro,
                            )
                    } catch (_: Exception) {
                        _diagChatHistorico.value = _diagChatHistorico.value +
                            DiagChatEntry(autor = DiagChatAutor.Ia, texto = "", isErro = true)
                    }
                } finally {
                    _diagChatCarregando.value = false
                }
            }
        }

        private fun extrairTextoDeResultado(
            resultado: AiDiagnosisState,
        ): Triple<String, String?, Boolean> =
            when (resultado) {
                is AiDiagnosisState.success ->
                    Triple(
                        resultado.result.textoLaudo.ifBlank { resultado.result.resumo },
                        resultado.result.modeloIa.nomeExibicao.ifBlank { "SignallQ IA" },
                        false,
                    )
                else -> Triple("", null, true)
            }
    }
