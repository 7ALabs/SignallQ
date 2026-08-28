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
