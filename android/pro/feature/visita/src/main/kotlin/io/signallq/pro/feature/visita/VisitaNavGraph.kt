package io.signallq.pro.feature.visita

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument

private const val ARG_VISITA_ID = "visitaId"

object VisitaRoutes {
    const val GRAFO = "visita"
    const val PAINEL = "visita/painel"
    const val MENU = "visita/menu"
    const val NOVA_VISITA = "visita/nova"
    const val VISITA_RAPIDA = "visita/rapida"
    const val CHECKLIST = "visita/checklist/{$ARG_VISITA_ID}"
    const val ATENDIMENTO = "visita/atendimento/{$ARG_VISITA_ID}"

    fun checklist(visitaId: String) = "visita/checklist/$visitaId"

    fun atendimento(visitaId: String) = "visita/atendimento/$visitaId"
}

/**
 * Grafo do nucleo do Pro (Grupo 2, telas 2.1-2.5 e 2.13-2.14). Callbacks externos
 * (ambientes, ferramentas, clientes, perfil) sao fornecidos por quem compoe (`:pro:app`),
 * respeitando a regra de ouro "feature nao depende de feature" (#1161).
 */
fun NavGraphBuilder.visitaGraph(
    navController: NavHostController,
    onNovoCliente: () -> Unit,
    onAbrirClientes: () -> Unit,
    onAbrirFerramentas: () -> Unit,
    onAbrirCobranca: () -> Unit,
    onAbrirPerfil: () -> Unit,
    onNavegarParaAmbientes: (visitaId: String) -> Unit,
    onNavegarParaConclusao: (visitaId: String) -> Unit,
) {
    navigation(startDestination = VisitaRoutes.PAINEL, route = VisitaRoutes.GRAFO) {
        composable(VisitaRoutes.PAINEL) {
            PainelScreen(
                onRetomarAtendimento = { visitaId ->
                    navController.navigate(VisitaRoutes.atendimento(visitaId))
                },
                onNovoAtendimento = { navController.navigate(VisitaRoutes.NOVA_VISITA) },
                onNovoCliente = onNovoCliente,
                onFerramentas = onAbrirFerramentas,
                onCobrar = onAbrirCobranca,
            )
        }
        composable(VisitaRoutes.MENU) {
            MenuScreen(
                onPainel = { navController.navigate(VisitaRoutes.PAINEL) },
                onClientes = onAbrirClientes,
                onFerramentas = onAbrirFerramentas,
                onPerfil = onAbrirPerfil,
            )
        }
        composable(VisitaRoutes.NOVA_VISITA) {
            NovaVisitaScreen(
                onVisitaCriada = { visitaId ->
                    navController.navigate(VisitaRoutes.checklist(visitaId)) {
                        popUpTo(VisitaRoutes.PAINEL)
                    }
                },
                onNovoCliente = onNovoCliente,
            )
        }
        composable(VisitaRoutes.VISITA_RAPIDA) {
            VisitaRapidaScreen(
                onVisitaCriada = { visitaId ->
                    onNavegarParaAmbientes(visitaId)
                },
                onNovoCliente = onNovoCliente,
            )
        }
        composable(
            VisitaRoutes.CHECKLIST,
            arguments = listOf(navArgument(ARG_VISITA_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val visitaId = checkNotNull(backStackEntry.arguments?.getString(ARG_VISITA_ID))
            ChecklistTipoVisitaScreen(
                onContinuar = { onNavegarParaAmbientes(visitaId) },
            )
        }
        composable(
            VisitaRoutes.ATENDIMENTO,
            arguments = listOf(navArgument(ARG_VISITA_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val visitaId = checkNotNull(backStackEntry.arguments?.getString(ARG_VISITA_ID))
            AtendimentoScreen(
                onContinuarChecklist = { navController.navigate(VisitaRoutes.checklist(visitaId)) },
                onContinuarAmbientes = { onNavegarParaAmbientes(visitaId) },
                onContinuarConclusao = { onNavegarParaConclusao(visitaId) },
            )
        }
    }
}
