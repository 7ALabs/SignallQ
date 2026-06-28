package io.signallq.app.feature.speedtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.signallq.app.core.datastore.PreferenciasAppRepository
import io.signallq.app.core.network.MonitorRede
import io.signallq.app.core.network.NetworkCapabilitiesProvider
import io.signallq.app.core.telephony.MonitorTelephony
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel responsavel pela execucao de speedtests, guarda de rede medida
 * e acumulacao de MB consumidos em rede movel.
 *
 * Extraido do MainViewModel (1602L) — etapa B do refactor de ViewModels por feature.
 *
 * Responsabilidades:
 * - Execucao de speedtest (fast/complete/triplo)
 * - Guard de rede medida (dialogo de confirmacao em rede movel)
 * - Acumulacao mensal de MB consumidos em rede movel
 * - Cancelamento de teste em andamento
 *
 * O callback [onSpeedtestConcluido] e invocado apos cada execucao — permite que o
 * MainViewModel (ou quem orquestra) dispare rotinas nao-speedtest (scan de dispositivos,
 * diagnostico etc.) sem criar dependencia direta entre feature modules.
 */
@HiltViewModel
class SpeedtestViewModel
    @Inject
    constructor(
        val executorSpeedtest: ExecutorSpeedtest,
        private val preferenciasAppRepository: PreferenciasAppRepository,
        private val monitorRede: MonitorRede,
        private val networkCapabilitiesProvider: NetworkCapabilitiesProvider,
        private val monitorTelefony: MonitorTelephony,
    ) : ViewModel() {
        private companion object {
            const val LOG_TAG = "SpeedtestViewModel"
        }

        /** Modo de speedtest aguardando confirmacao do usuario em rede medida. null = sem pendencia. */
        private val _speedtestPendenteModoMovel = MutableStateFlow<ModoSpeedtest?>(null)
        val speedtestPendenteModoMovel: StateFlow<ModoSpeedtest?> = _speedtestPendenteModoMovel

        /** Callback disparado apos execucao bem-sucedida (ou falha) de um speedtest. */
        var onSpeedtestConcluido: (() -> Unit)? = null

        /**
         * Inicia ou enfileira um speedtest.
         *
         * Em rede medida com modo pesado (complete/triplo) e sem permissao previa,
         * suspende e armazena o modo para exibir dialogo de confirmacao.
         * Caso contrario, executa imediatamente.
         */
        fun reiniciarSuite(modo: ModoSpeedtest) {
            viewModelScope.launch {
                if (modo != ModoSpeedtest.fast && networkCapabilitiesProvider.isMeteredNetwork()) {
                    val permiteHeavy = preferenciasAppRepository.speedtestPermiteHeavyMovel.first()
                    if (!permiteHeavy) {
                        _speedtestPendenteModoMovel.value = modo
                        return@launch
                    }
                }
                try {
                    executarSpeedtest(modo)
                } finally {
                    acumularMbConsumidos(modo)
                    onSpeedtestConcluido?.invoke()
                }
            }
        }

        /** Confirma execucao do speedtest pendente (usuario aceitou usar dados moveis). */
        fun confirmarSpeedtestEmMovel() {
            val modo = _speedtestPendenteModoMovel.value ?: return
            _speedtestPendenteModoMovel.value = null
            viewModelScope.launch {
                try {
                    executarSpeedtest(modo)
                } finally {
                    acumularMbConsumidos(modo)
                    onSpeedtestConcluido?.invoke()
                }
            }
        }

        /** Cancela o speedtest pendente (usuario recusou usar dados moveis). */
        fun cancelarSpeedtestMovel() {
            _speedtestPendenteModoMovel.value = null
        }

        /** Define a preferencia de permitir testes pesados em rede medida. */
        fun setSpeedtestPermiteHeavyMovel(valor: Boolean) {
            viewModelScope.launch { preferenciasAppRepository.setSpeedtestPermiteHeavyMovel(valor) }
        }

        private suspend fun executarSpeedtest(modo: ModoSpeedtest) {
            val connectionType = monitorRede.snapshotFlow.value.estadoConexao.name
            Timber.i("$LOG_TAG: iniciando modo=${modo.name} connectionType=$connectionType")
            executorSpeedtest.executar(
                modo = modo,
                connectionType = connectionType,
                connectionTypeProvider = { monitorRede.snapshotFlow.value.estadoConexao.name },
                tecnologiaProvider = { monitorTelefony.snapshotFlow.value?.tecnologia },
            )
            Timber.i("$LOG_TAG: finalizado modo=${modo.name}")
        }

        /**
         * Acumula MB estimados consumidos no mes corrente.
         * Reset automatico quando o mes muda em relacao ao valor salvo.
         * Estimativas: fast=10 MB, complete=25 MB, triplo=30 MB.
         */
        private fun acumularMbConsumidos(modo: ModoSpeedtest) {
            val mbEstimado =
                when (modo) {
                    ModoSpeedtest.fast -> 10L
                    ModoSpeedtest.complete -> 25L
                    ModoSpeedtest.triplo -> 30L
                }
            val cal = java.util.Calendar.getInstance()
            val mesAtual = "%04d-%02d".format(
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
            )
            viewModelScope.launch {
                withContext(kotlinx.coroutines.NonCancellable) {
                    val mesReferencia = preferenciasAppRepository.speedtestMesReferencia.first()
                    val mbAcumulados =
                        if (mesReferencia == mesAtual) {
                            preferenciasAppRepository.speedtestMbConsumidosMes.first()
                        } else {
                            preferenciasAppRepository.setSpeedtestMesReferencia(mesAtual)
                            0L
                        }
                    preferenciasAppRepository.setSpeedtestMbConsumidosMes(mbAcumulados + mbEstimado)
                }
            }
        }
    }
