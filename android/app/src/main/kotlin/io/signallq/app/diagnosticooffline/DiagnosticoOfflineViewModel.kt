package io.signallq.app.diagnosticooffline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Etapas do fluxo de diagnóstico offline guiado (issue #1811, Task 2), CTA opt-in dentro de
 * `SignallQOfflineBanner` — mesma ordem e semântica de sondagem sequencial usada por
 * [io.signallq.app.core.network.connectivity.ConnectivityDiagnosisEngine]: cada etapa só faz
 * sentido testar se a anterior confirmou sucesso.
 *
 * Nomeação deliberadamente mais curta que [io.signallq.app.core.network.contracts.connectivity.ConnectivityProbeStep]
 * (que tem 12 valores, incluindo etapas de coleta de contexto sem sondagem própria, ex.
 * `WIFI_TRANSPORT`/`LINK_PROPERTIES`) — esta camada de apresentação só precisa das 4 etapas que
 * o usuário de fato acompanha, na mesma ordem: gateway → DNS → rota externa → hostname/captive
 * portal. O wiring real com o engine (Task 4) mapeia cada valor abaixo para o subconjunto
 * correspondente de `ConnectivityProbeStep`.
 */
enum class EtapaDiagnosticoOffline {
    GATEWAY,
    DNS,
    ROTA_EXTERNA,
    HOSTNAME_CAPTIVE_PORTAL,
    ;

    companion object {
        /** Ordem de execução do fluxo — gateway → DNS → rota externa → hostname/captive portal. */
        val ORDEM: List<EtapaDiagnosticoOffline> = listOf(GATEWAY, DNS, ROTA_EXTERNA, HOSTNAME_CAPTIVE_PORTAL)
    }
}

/** Resultado definitivo (não transitório) de uma etapa já executada — compõe o histórico exibido na UI. */
sealed interface ResultadoEtapaDiagnosticoOffline {
    val etapa: EtapaDiagnosticoOffline

    data class Sucesso(
        override val etapa: EtapaDiagnosticoOffline,
        val detalhe: String? = null,
    ) : ResultadoEtapaDiagnosticoOffline

    data class Falha(
        override val etapa: EtapaDiagnosticoOffline,
        val motivo: String? = null,
    ) : ResultadoEtapaDiagnosticoOffline
}

/**
 * Contrato de estado do fluxo de diagnóstico offline guiado (issue #1811, Task 2).
 *
 * Desacoplado de `AppShellMedicaoGuiada` — não depende dele, não reaproveita seu holder nem sua
 * lógica de transição. Também não depende ainda do `ConnectivityDiagnosisEngine` real (Task 4);
 * [DiagnosticoOfflineViewModel] recebe as sondagens por etapa como função injetada, então o
 * wiring futuro troca a implementação sem precisar redesenhar este contrato.
 */
sealed interface DiagnosticoOfflineEstado {
    /** Nenhuma etapa foi iniciada — estado inicial antes do tap no CTA do banner offline. */
    data object Idle : DiagnosticoOfflineEstado

    /** Etapa [etapa] está sondando agora. [historico] cobre só etapas já concluídas, em ordem. */
    data class TestandoEtapa(
        val etapa: EtapaDiagnosticoOffline,
        val historico: List<ResultadoEtapaDiagnosticoOffline>,
    ) : DiagnosticoOfflineEstado

    /** Etapa [etapa] confirmou sucesso — transitório, seguido por `TestandoEtapa` da próxima ou `DiagnosticoConcluido`. */
    data class EtapaOk(
        val etapa: EtapaDiagnosticoOffline,
        val historico: List<ResultadoEtapaDiagnosticoOffline>,
    ) : DiagnosticoOfflineEstado

    /** Etapa [etapa] falhou — transitório, seguido por `DiagnosticoConcluido` (fluxo não segue para a próxima etapa após falha). */
    data class EtapaFalhou(
        val etapa: EtapaDiagnosticoOffline,
        val motivo: String?,
        val historico: List<ResultadoEtapaDiagnosticoOffline>,
    ) : DiagnosticoOfflineEstado

    /** Usuário pediu para repetir a partir de [etapa] (normalmente a que falhou) — mantém o histórico anterior a ela. */
    data class RetryEmAndamento(
        val etapa: EtapaDiagnosticoOffline,
        val historico: List<ResultadoEtapaDiagnosticoOffline>,
    ) : DiagnosticoOfflineEstado

    /**
     * Fluxo terminou — por sucesso em todas as etapas ou por parar na primeira falha.
     * [etapaComFalha] é `null` quando as 4 etapas confirmaram sucesso.
     */
    data class DiagnosticoConcluido(
        val historico: List<ResultadoEtapaDiagnosticoOffline>,
        val etapaComFalha: EtapaDiagnosticoOffline?,
    ) : DiagnosticoOfflineEstado
}

/**
 * Sonda uma etapa do fluxo. Task 2 é entregue sem implementação real — o chamador (teste, ou o
 * futuro wiring da Task 3/4) fornece a implementação. Task 4 substitui por uma que delega ao
 * [io.signallq.app.core.network.connectivity.ConnectivityDiagnosisEngine] real, sem mudar a
 * assinatura nem o contrato de estado acima.
 */
fun interface ExecutorEtapaDiagnosticoOffline {
    suspend fun executar(etapa: EtapaDiagnosticoOffline): ResultadoEtapaDiagnosticoOffline
}

/**
 * State holder do fluxo de diagnóstico offline guiado (issue #1811, Task 2).
 *
 * `ViewModel` puro (sem `@HiltViewModel`) por enquanto: nenhuma tela Compose consome este
 * holder ainda (Task 3 conecta o CTA do `SignallQOfflineBanner`; a UI dos estados visuais só
 * nasce depois de `/design-check` validar componentes reais — ver decisão registrada no PR).
 * Adicionar `@HiltViewModel` + `@Inject constructor` quando houver um consumidor real é
 * mudança local, não redesenho do contrato de estado.
 */
class DiagnosticoOfflineViewModel(
    private val executorEtapa: ExecutorEtapaDiagnosticoOffline,
) : ViewModel() {
    private val _estado = MutableStateFlow<DiagnosticoOfflineEstado>(DiagnosticoOfflineEstado.Idle)
    val estado: StateFlow<DiagnosticoOfflineEstado> = _estado.asStateFlow()

    /** Dispara o fluxo do zero — chamada pelo tap no CTA "Diagnosticar problema" (Task 3). */
    fun iniciar() {
        viewModelScope.launch {
            executarDesde(etapa = EtapaDiagnosticoOffline.ORDEM.first(), historicoAnterior = emptyList())
        }
    }

    /**
     * Repete o fluxo a partir de [etapa] (por padrão, a que falhou por último) preservando o
     * histórico das etapas anteriores a ela — não reexecuta o que já confirmou sucesso.
     */
    fun retry(etapa: EtapaDiagnosticoOffline? = null) {
        val estadoAtual = _estado.value
        val etapaParaRetry =
            etapa ?: (estadoAtual as? DiagnosticoOfflineEstado.DiagnosticoConcluido)?.etapaComFalha
                ?: (estadoAtual as? DiagnosticoOfflineEstado.EtapaFalhou)?.etapa
                ?: return
        val historicoAnterior =
            when (estadoAtual) {
                is DiagnosticoOfflineEstado.DiagnosticoConcluido -> estadoAtual.historico.filter { it.etapa != etapaParaRetry }
                is DiagnosticoOfflineEstado.EtapaFalhou -> estadoAtual.historico
                else -> emptyList()
            }
        _estado.value = DiagnosticoOfflineEstado.RetryEmAndamento(etapaParaRetry, historicoAnterior)
        viewModelScope.launch {
            executarDesde(etapa = etapaParaRetry, historicoAnterior = historicoAnterior)
        }
    }

    private suspend fun executarDesde(
        etapa: EtapaDiagnosticoOffline,
        historicoAnterior: List<ResultadoEtapaDiagnosticoOffline>,
    ) {
        var historico = historicoAnterior
        val indiceInicial = EtapaDiagnosticoOffline.ORDEM.indexOf(etapa)

        for (indice in indiceInicial until EtapaDiagnosticoOffline.ORDEM.size) {
            val etapaAtual = EtapaDiagnosticoOffline.ORDEM[indice]
            _estado.value = DiagnosticoOfflineEstado.TestandoEtapa(etapaAtual, historico)

            val resultado = executorEtapa.executar(etapaAtual)
            historico = historico + resultado

            when (resultado) {
                is ResultadoEtapaDiagnosticoOffline.Sucesso -> {
                    _estado.value = DiagnosticoOfflineEstado.EtapaOk(etapaAtual, historico)
                }
                is ResultadoEtapaDiagnosticoOffline.Falha -> {
                    _estado.value = DiagnosticoOfflineEstado.EtapaFalhou(etapaAtual, resultado.motivo, historico)
                    _estado.value = DiagnosticoOfflineEstado.DiagnosticoConcluido(historico, etapaComFalha = etapaAtual)
                    return
                }
            }
        }

        _estado.value = DiagnosticoOfflineEstado.DiagnosticoConcluido(historico, etapaComFalha = null)
    }
}
