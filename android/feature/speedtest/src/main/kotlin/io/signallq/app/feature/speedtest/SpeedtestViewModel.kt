package io.signallq.app.feature.speedtest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.signallq.app.core.datastore.PreferenciasAppRepository
import io.signallq.app.core.network.AnalyticsHelper
import io.signallq.app.core.network.AnalyticsTracker
import io.signallq.app.core.network.MonitorRede
import io.signallq.app.core.network.NetworkCapabilitiesProvider
import io.signallq.app.core.telephony.MonitorTelephony
import io.signallq.app.feature.speedtest.connectivity.ConnectivityDiagnosisMensagem
import io.signallq.app.feature.speedtest.connectivity.ConnectivityDiagnosisPresenter
import io.signallq.app.feature.speedtest.connectivity.ConnectivityDiagnosisRepository
import io.signallq.app.feature.speedtest.connectivity.indicaAusenciaDeInternetParaBloquearSpeedtest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
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
        /** Funil principal de engajamento (SIG-155) — speedtest_iniciado/speedtest_concluido. */
        private val analyticsHelper: AnalyticsHelper,
        /** GH#784 — funil do teste de velocidade no admin-worker (abriu/iniciou/completou/
         *  compartilhou). Evento distinto de [analyticsHelper] (que só vai pro Firebase);
         *  aqui reaproveita feature_used (mesmo schema de "wifi"/"historico"/etc), com
         *  feature_id "speedtest_iniciado"/"speedtest_completou" — sem endpoint novo no worker. */
        private val analyticsTracker: AnalyticsTracker,
        /** GH#1512 — diagnostico local de conectividade (Wi-Fi conectado sem internet). */
        private val connectivityDiagnosisRepository: ConnectivityDiagnosisRepository,
    ) : ViewModel() {
        private companion object {
            const val LOG_TAG = "SpeedtestViewModel"
        }

        /** Modo de speedtest aguardando confirmacao do usuario em rede medida. null = sem pendencia. */
        private val _speedtestPendenteModoMovel = MutableStateFlow<ModoSpeedtest?>(null)
        val speedtestPendenteModoMovel: StateFlow<ModoSpeedtest?> = _speedtestPendenteModoMovel

        /** GH#1512 — conclusao do diagnostico local quando o Speedtest e interrompido por
         *  Wi-Fi sem internet (nao um erro generico de execucao). null = sem pendencia;
         *  UI limpa apos exibir (ver [limparDiagnosticoConectividade]). */
        private val _diagnosticoConectividade = MutableStateFlow<ConnectivityDiagnosisMensagem?>(null)
        val diagnosticoConectividade: StateFlow<ConnectivityDiagnosisMensagem?> = _diagnosticoConectividade

        fun limparDiagnosticoConectividade() {
            _diagnosticoConectividade.value = null
        }

        /** Callback disparado apos execucao bem-sucedida (ou falha) de um speedtest. */
        var onSpeedtestConcluido: (() -> Unit)? = null

        /**
         * GH#1221 RF-09 — trava de concorrencia no dominio, independente do estado visual
         * do botao. O executor ja rejeita silenciosamente uma segunda execucao concorrente
         * ([ExecutorSpeedtestCloudflare.emExecucao]), mas sem essa trava aqui o SEGUNDO clique
         * ainda dispararia sua propria corrotina ate o fim (acumulando MB consumidos em
         * dobro e chamando [onSpeedtestConcluido] duas vezes para uma unica medicao real).
         */
        private val execucaoEmAndamento = AtomicBoolean(false)

        /**
         * Inicia ou enfileira um speedtest.
         *
         * Em rede medida com modo pesado (complete/triplo) e sem permissao previa,
         * suspende e armazena o modo para exibir dialogo de confirmacao.
         * Caso contrario, executa imediatamente.
         */
        fun reiniciarSuite(modo: ModoSpeedtest) {
            if (!execucaoEmAndamento.compareAndSet(false, true)) {
                Timber.w("$LOG_TAG: reiniciarSuite ignorado — ja ha execucao em andamento")
                return
            }
            viewModelScope.launch {
                if (modo != ModoSpeedtest.fast && networkCapabilitiesProvider.isMeteredNetwork()) {
                    val permiteHeavy = preferenciasAppRepository.speedtestPermiteHeavyMovel.first()
                    if (!permiteHeavy) {
                        _speedtestPendenteModoMovel.value = modo
                        execucaoEmAndamento.set(false)
                        return@launch
                    }
                }
                // GH#1512 — verificado ANTES do try/finally de baixo: se o diagnóstico
                // interromper, não queremos acionar acumularMbConsumidos/onSpeedtestConcluido
                // (nenhum teste real rodou) nem duplicar a lógica de erro do speedtest.
                if (interromperPorWifiSemInternet()) {
                    execucaoEmAndamento.set(false)
                    return@launch
                }
                try {
                    executarSpeedtest(modo)
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    Timber.e(t, "$LOG_TAG: erro ao executar speedtest modo=${modo.name}")
                } finally {
                    acumularMbConsumidos(modo)
                    execucaoEmAndamento.set(false)
                    onSpeedtestConcluido?.invoke()
                }
            }
        }

        /** Confirma execucao do speedtest pendente (usuario aceitou usar dados moveis). */
        fun confirmarSpeedtestEmMovel() {
            val modo = _speedtestPendenteModoMovel.value ?: return
            if (!execucaoEmAndamento.compareAndSet(false, true)) {
                Timber.w("$LOG_TAG: confirmarSpeedtestEmMovel ignorado — ja ha execucao em andamento")
                return
            }
            _speedtestPendenteModoMovel.value = null
            viewModelScope.launch {
                // GH#1512 (3a revisao, correcao) -- reintroduzido: o dialog que leva ate
                // aqui e sobre rede MEDIDA (`metered`), nao sobre transporte celular --
                // `networkCapabilitiesProvider.isMeteredNetwork()` e true para qualquer
                // rede medida, Wi-Fi tarifado incluso (hotspot, rede marcada como
                // limitada). Remover o gate daqui permitia rodar um Speedtest completo
                // sobre um Wi-Fi tarifado e sem internet, sem nenhum aviso. O gate ja e
                // auto-limitado -- devolve false de cara quando nao ha rede Wi-Fi nenhuma
                // (dados moveis puros) -- entao mante-lo aqui nao interfere no caso de
                // dados moveis de verdade.
                if (interromperPorWifiSemInternet()) {
                    execucaoEmAndamento.set(false)
                    return@launch
                }
                try {
                    executarSpeedtest(modo)
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    Timber.e(t, "$LOG_TAG: erro ao confirmar speedtest em rede movel modo=${modo.name}")
                } finally {
                    acumularMbConsumidos(modo)
                    execucaoEmAndamento.set(false)
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
            analyticsHelper.registrarSpeedtestIniciado(
                modo = modo.name,
                tipoConexao = mapTipoConexaoParaAnalytics(connectionType),
            )
            // GH#784 — etapa "iniciou" do funil (Uso do App, admin-worker).
            analyticsTracker.registrarFeatureUsada("speedtest_iniciado")
            val inicioMs = System.currentTimeMillis()
            executorSpeedtest.executar(
                modo = modo,
                connectionType = connectionType,
                connectionTypeProvider = { monitorRede.snapshotFlow.value.estadoConexao.name },
                tecnologiaProvider = { monitorTelefony.snapshotFlow.value?.tecnologia },
                // GH#1221 RF-01 — resolve o perfil de pool (metered/movel) no momento do
                // teste, nao no valor congelado na criacao do singleton via AppModule.
                isMobileProvider = { networkCapabilitiesProvider.isMeteredNetwork() },
            )
            Timber.i("$LOG_TAG: finalizado modo=${modo.name}")
            registrarSpeedtestConcluidoSeDisponivel(modo, System.currentTimeMillis() - inicioMs)
        }

        /**
         * GH#1512 — decide localmente rodando o diagnóstico ativo de verdade sempre que há
         * uma rede Wi-Fi presente ([ConnectivityDiagnosisRepository.existeRedeWifiAtiva],
         * checagem direta via `ConnectivityManager`, nunca via [MonitorRede]/rede default —
         * achado de revisão: `SnapshotRede.conectado`/`estadoConexao` podem ficar
         * "otimistas" por até ~600ms após instabilidade, ou seguir dados móveis quando o
         * Android promove a rede móvel a default, mascarando exatamente o cenário que esta
         * issue pede para detectar). Sem rede Wi-Fi nenhuma, não intervém aqui — o fluxo/
         * diálogo já existente (`ForaDoWifiDialog`) cobre rede móvel pura.
         */
        private suspend fun interromperPorWifiSemInternet(): Boolean {
            // GH#1512 (achado de revisao) -- existeRedeWifiAtiva() e diagnosticar() ficam
            // no MESMO try/catch: uma excecao inesperada em qualquer uma delas (binder
            // IPC do ConnectivityManager, I/O de persistencia, etc.) nunca pode propagar
            // como crash nem travar execucaoEmAndamento -- so nao bloqueia o teste
            // (equivalente a "sem evidencia suficiente", igual a INCONCLUSIVE logo
            // abaixo), deixando o Speedtest seguir seu proprio caminho normal de erro.
            val diagnostico =
                try {
                    if (!connectivityDiagnosisRepository.existeRedeWifiAtiva()) return false
                    connectivityDiagnosisRepository.diagnosticar()
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    Timber.e(t, "$LOG_TAG: diagnostico de conectividade falhou inesperadamente -- nao bloqueia o speedtest")
                    return false
                }

            // GH#1512 (3a revisao) -- politica extraida para ConnectivityBlockingPolicy.kt
            // (testavel isoladamente, sem duplicar entre MainViewModel/SpeedtestViewModel --
            // achado de revisao R-6). Ver KDoc la para o raciocinio completo.
            if (!diagnostico.indicaAusenciaDeInternetParaBloquearSpeedtest()) return false

            _diagnosticoConectividade.value = ConnectivityDiagnosisPresenter.apresentar(diagnostico)
            Timber.i("$LOG_TAG: speedtest interrompido -- wifi sem internet, status=${diagnostico.status}")
            return true
        }

        /**
         * Dispara speedtest_concluido (SIG-155) a partir do resultado que acabou de
         * ficar disponivel em [ExecutorSpeedtest.snapshotFlow]. No-op se, por qualquer
         * motivo, o snapshot nao estiver em estado concluido com resultado (ex.: erro).
         */
        private fun registrarSpeedtestConcluidoSeDisponivel(
            modo: ModoSpeedtest,
            duracaoMs: Long,
        ) {
            val snapshot = executorSpeedtest.snapshotFlow.value
            if (snapshot.estado != EstadoExecucaoSpeedtest.concluido) return
            val resultado = snapshot.resultado ?: return
            // GH#784 — etapa "completou" do funil (Uso do App, admin-worker). So dispara
            // quando ha resultado real disponivel (mesmo guard do bloco acima), nunca em
            // erro/cancelamento.
            analyticsTracker.registrarFeatureUsada("speedtest_completou")
            analyticsHelper.registrarSpeedtestConcluido(
                modo = modo.name,
                tipoConexaoInicio = resultado.connectionTypeStart?.let(::mapTipoConexaoParaAnalytics) ?: "desconhecido",
                tipoConexaoFim = resultado.connectionTypeEnd?.let(::mapTipoConexaoParaAnalytics),
                downloadMbps = resultado.downloadMbps,
                uploadMbps = resultado.uploadMbps,
                latenciaMs = resultado.latenciaMs,
                jitterMs = resultado.jitterMs,
                perdaPct = resultado.perdaPercentual,
                bufferbloatMs = resultado.bufferbloatMs,
                severidadeBufferbloat = mapSeveridadeBufferbloatParaAnalytics(resultado.severidadeBufferbloat),
                stabilityScore = resultado.stabilityScore,
                contaminado = resultado.contaminado,
                duracaoMs = duracaoMs,
            )
        }

        // EstadoConexao.name usa "movel"; o schema de analytics usa "mobile".
        private fun mapTipoConexaoParaAnalytics(raw: String): String = if (raw == "movel") "mobile" else raw

        private fun mapSeveridadeBufferbloatParaAnalytics(severidade: SeveridadeBufferbloat): String =
            when (severidade) {
                SeveridadeBufferbloat.none -> "nenhum"
                SeveridadeBufferbloat.mild -> "leve"
                SeveridadeBufferbloat.moderate -> "moderado"
                SeveridadeBufferbloat.severe -> "severo"
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
