package io.signallq.pro.feature.medicaodiagnostico

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument

private const val ARG_AMBIENTE_ID = "ambienteId"

object MedicaoDiagnosticoRoutes {
    const val GRAFO = "medicao-diagnostico"
    const val MEDICAO = "medicao-diagnostico/medicao/{$ARG_AMBIENTE_ID}"
    const val DIAGNOSTICO_MEDINDO = "medicao-diagnostico/diagnostico-medindo/{$ARG_AMBIENTE_ID}"
    const val DIAGNOSTICO_RESULTADO = "medicao-diagnostico/diagnostico-resultado/{$ARG_AMBIENTE_ID}"
    const val EVIDENCIAS = "medicao-diagnostico/evidencias/{$ARG_AMBIENTE_ID}"

    fun medicao(ambienteId: String) = "medicao-diagnostico/medicao/$ambienteId"

    private fun argumento() = navArgument(ARG_AMBIENTE_ID) { type = NavType.StringType }

    fun argumentos() = listOf(argumento())
}

/**
 * Grafo das telas 2.10-2.12 e 2.15-2.16 (medicao/diagnostico/evidencias por ambiente).
 * [onConcluirAmbiente] volta pra lista de ambientes -- fornecida por quem compoe
 * (`:pro:app`), sem depender de `:pro:feature:ambiente` diretamente (#1161).
 */
fun NavGraphBuilder.medicaoDiagnosticoGraph(
    navController: NavHostController,
    onConcluirAmbiente: () -> Unit,
) {
    navigation(startDestination = MedicaoDiagnosticoRoutes.MEDICAO, route = MedicaoDiagnosticoRoutes.GRAFO) {
        composable(MedicaoDiagnosticoRoutes.MEDICAO, arguments = MedicaoDiagnosticoRoutes.argumentos()) { backStackEntry ->
            val ambienteId = checkNotNull(backStackEntry.arguments?.getString(ARG_AMBIENTE_ID))
            MedicaoAmbienteScreen(
                onMedicaoConcluida = {
                    navController.navigate(
                        MedicaoDiagnosticoRoutes.DIAGNOSTICO_MEDINDO.replace("{$ARG_AMBIENTE_ID}", ambienteId),
                    )
                },
            )
        }
        composable(
            MedicaoDiagnosticoRoutes.DIAGNOSTICO_MEDINDO,
            arguments = MedicaoDiagnosticoRoutes.argumentos(),
        ) { backStackEntry ->
            val ambienteId = checkNotNull(backStackEntry.arguments?.getString(ARG_AMBIENTE_ID))
            DiagnosticoMedindoScreen(
                onDiagnosticoConcluido = {
                    navController.navigate(
                        MedicaoDiagnosticoRoutes.DIAGNOSTICO_RESULTADO.replace("{$ARG_AMBIENTE_ID}", ambienteId),
                    ) {
                        popUpTo(MedicaoDiagnosticoRoutes.DIAGNOSTICO_MEDINDO.replace("{$ARG_AMBIENTE_ID}", ambienteId)) {
                            inclusive = true
                        }
                    }
                },
            )
        }
        composable(
            MedicaoDiagnosticoRoutes.DIAGNOSTICO_RESULTADO,
            arguments = MedicaoDiagnosticoRoutes.argumentos(),
        ) { backStackEntry ->
            val ambienteId = checkNotNull(backStackEntry.arguments?.getString(ARG_AMBIENTE_ID))
            DiagnosticoResultadoScreen(
                onConcluir = {
                    navController.navigate(
                        MedicaoDiagnosticoRoutes.EVIDENCIAS.replace("{$ARG_AMBIENTE_ID}", ambienteId),
                    )
                },
            )
        }
        composable(MedicaoDiagnosticoRoutes.EVIDENCIAS, arguments = MedicaoDiagnosticoRoutes.argumentos()) {
            EvidenciasScreen(onContinuar = onConcluirAmbiente)
        }
    }
}
