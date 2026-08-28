package io.signallq.app.diagnosticooffline

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

/**
 * Ponto de entrada do wiring real (issue #1811, Task 4/4) para quem for consumir
 * [DiagnosticoOfflineViewModel] — conecta o ViewModel ao [DiagnosticoOfflineExecutorReal], que
 * por sua vez chama [io.signallq.app.core.network.connectivity.ConnectivityDiagnosisEngine]
 * probe a probe (incluindo o `DohFallbackProbe` da Task 1).
 *
 * Não há `@HiltViewModel` aqui de propósito: nenhuma tela Compose consome este ViewModel
 * ainda — a Task 2 registrou essa mesma decisão (sem componente visual novo sem referência
 * real do design system) e ela continua válida nesta task; o CTA da Task 3
 * (`SignallQOfflineBanner`) hoje abre um diálogo placeholder porque ainda não existe uma tela
 * real para navegar. Esta fábrica é o que essa tela futura vai chamar quando existir — trocar
 * `@HiltViewModel` por este `ViewModelProvider.Factory` (ou vice-versa) é mudança local, não
 * redesenho do wiring.
 *
 * ## Ressalva 1 da revisão do Caio na PR #1814 — dispatcher implícito por trás do `yield()`
 *
 * `DiagnosticoOfflineViewModel.executarDesde()` depende de `yield()` para que `EtapaOk`/
 * `EtapaFalhou` (estados transitórios, `StateFlow` conflado) sejam observáveis por um coletor
 * antes de serem sobrescritos. Caio validou que isso funciona porque produtor
 * (`viewModelScope` = `Dispatchers.Main.immediate`) e consumidor real (`collectAsStateWithLifecycle`
 * em Compose, também Main) compartilham o mesmo dispatcher single-threaded — e avisou que é um
 * acordo implícito, não uma garantia: qualquer coletor fora da Main (`flowOn(Default)`, um
 * segundo coletor concorrente, `UnconfinedTestDispatcher`) reabre a janela de perda.
 *
 * Decisão desta task: **manter `yield()` como está, sem reforço adicional.** Motivo: esta PR
 * não introduz nenhum coletor de UI — `DiagnosticoOfflineExecutorReal`/esta fábrica só trocam a
 * implementação do seam (`ExecutorEtapaDiagnosticoOffline`) por uma que faz I/O real; nenhuma
 * tela ainda observa `estado`. O consumo real (`collectAsStateWithLifecycle` na Main) só nasce
 * quando existir uma tela Compose de verdade para o CTA navegar, e nesse ponto o pressuposto do
 * Caio ("mesmo dispatcher single-threaded") continua valendo pela própria natureza do Compose.
 * Se aquela tela futura precisar do estado transitório GARANTIDO (ex.: cada etapa acende com
 * tempo mínimo visível, não só "o que sobrou depois do próximo yield"), a estrutura certa é
 * substituir o `StateFlow` conflado por um canal de eventos (`SharedFlow` com
 * `BufferOverflow.SUSPEND`) — mudança de contrato que cabe àquela task, não a esta, porque reabre
 * o `DiagnosticoOfflineEstado` aprovado na #1814.
 */
class DiagnosticoOfflineViewModelFactory(
    private val appContext: Context,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras,
    ): T {
        require(modelClass.isAssignableFrom(DiagnosticoOfflineViewModel::class.java)) {
            "DiagnosticoOfflineViewModelFactory só cria DiagnosticoOfflineViewModel, recebeu $modelClass"
        }
        @Suppress("UNCHECKED_CAST")
        return DiagnosticoOfflineViewModel(
            executorEtapa = DiagnosticoOfflineExecutorReal(appContext.applicationContext),
        ) as T
    }
}
