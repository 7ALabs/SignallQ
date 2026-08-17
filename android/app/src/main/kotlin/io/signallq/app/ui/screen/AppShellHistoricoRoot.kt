package io.signallq.app.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.feature.history.ResumoHistorico
import io.signallq.app.ui.FiltroConexaoHistorico

// Raiz "Histórico" (tab 3) do AppShell — issue #1698, épico #1647.
//
// Segunda das duas raízes migradas para AppShellRootRegistry. Diferente de
// AppShellFerramentasRoot (que trouxe regra de negócio junto), esta é o caso simples e por isso o
// melhor exemplo do padrão: sete parâmetros soltos de `AppShell.kt` viraram um grupo
// (AppShellHistoricoState) construído pela `MainActivity`, exatamente como já acontecia com
// AppShellSpeedtestState e AppShellWifiState.
//
// O efeito é o que a issue pede: uma fatia futura que mexa em filtro, resumo ou lista do Histórico
// edita ESTE arquivo e o data class, não o arquivo central.

/**
 * Agrupa o que a tela de Histórico consome do shell. Substitui os parâmetros soltos
 * `historicoFiltrado`, `resumoHistorico`, `filtroConexaoHistorico`,
 * `onFiltroConexaoHistoricoChange`, `filtroOperadoraHistorico`, `onFiltroOperadoraHistoricoChange`
 * e `operadorasDisponiveisHistorico` de `AppShell`.
 *
 * Não confundir com o parâmetro `historico: List<MedicaoEntity>` que continua em `AppShell`: aquele
 * é a lista bruta usada por `resolverPrimeiraHistoria` (Home e Laudo, GH#1223/#1265), não a lista
 * filtrada que esta tela exibe. São consumidores diferentes do mesmo dado de origem.
 */
@Stable
data class AppShellHistoricoState(
    val historicoFiltrado: List<MedicaoEntity> = emptyList(),
    val resumoHistorico: ResumoHistorico? = null,
    val filtroConexao: FiltroConexaoHistorico = FiltroConexaoHistorico.TODOS,
    val onFiltroConexaoChange: (FiltroConexaoHistorico) -> Unit = {},
    val filtroOperadora: String? = null,
    val onFiltroOperadoraChange: (String?) -> Unit = {},
    val operadorasDisponiveis: List<String> = emptyList(),
)

/**
 * A raiz em si. [adsEnabled] e os dois callbacks de navegação continuam vindo do shell porque são
 * decisão dele, não do Histórico: o gate de anúncio combina flag remota com consentimento UMP
 * (issue #555) e a navegação para outra raiz é do `AppShellNavigator`.
 */
@Composable
internal fun AppShellHistoricoRoot(
    state: AppShellHistoricoState,
    adsEnabled: Boolean,
    onAbrirMenu: () -> Unit,
    onIniciarTeste: () -> Unit,
) {
    HistoricoScreen(
        historico = state.historicoFiltrado,
        resumoHistorico = state.resumoHistorico,
        onAbrirMenu = onAbrirMenu,
        onIniciarTeste = onIniciarTeste,
        filtroConexao = state.filtroConexao,
        onFiltroConexaoChange = state.onFiltroConexaoChange,
        filtroOperadora = state.filtroOperadora,
        onFiltroOperadoraChange = state.onFiltroOperadoraChange,
        operadorasDisponiveis = state.operadorasDisponiveis,
        adsEnabled = adsEnabled,
    )
}
