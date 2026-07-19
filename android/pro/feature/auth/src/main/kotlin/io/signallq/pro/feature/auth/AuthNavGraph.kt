package io.signallq.pro.feature.auth

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation

object AuthRoutes {
    const val GRAFO = "auth"
    const val CARREGAMENTO = "auth/carregamento"
    const val APRESENTACAO = "auth/apresentacao"
    const val CADASTRO_PROFISSIONAL = "auth/cadastro"
    const val PERMISSOES = "auth/permissoes"
    const val PERMISSAO_BLOQUEADA = "auth/permissao-bloqueada"
}

/**
 * Grafo de navegacao do Grupo 1 (trimmed) -- carregamento, apresentacao, cadastro do
 * profissional e permissoes. [onAutenticado] e chamado quando perfil + permissoes essenciais
 * estao prontos; quem compoe (`:pro:app`) decide o destino real (painel), respeitando a
 * regra de ouro "feature nao depende de feature" (#1161).
 */
fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    onAutenticado: () -> Unit,
) {
    navigation(startDestination = AuthRoutes.CARREGAMENTO, route = AuthRoutes.GRAFO) {
        composable(AuthRoutes.CARREGAMENTO) {
            CarregamentoScreen(
                onDestinoDecidido = { destino ->
                    when (destino) {
                        DestinoPosCarregamento.APRESENTACAO ->
                            navController.navigate(AuthRoutes.APRESENTACAO) {
                                popUpTo(AuthRoutes.CARREGAMENTO) { inclusive = true }
                            }
                        DestinoPosCarregamento.PERMISSOES ->
                            navController.navigate(AuthRoutes.PERMISSOES) {
                                popUpTo(AuthRoutes.CARREGAMENTO) { inclusive = true }
                            }
                        DestinoPosCarregamento.PRONTO -> onAutenticado()
                    }
                },
            )
        }
        composable(AuthRoutes.APRESENTACAO) {
            ApresentacaoScreen(
                onContinuar = { navController.navigate(AuthRoutes.CADASTRO_PROFISSIONAL) },
            )
        }
        composable(AuthRoutes.CADASTRO_PROFISSIONAL) {
            CadastroProfissionalScreen(
                onConcluido = {
                    navController.navigate(AuthRoutes.PERMISSOES) {
                        popUpTo(AuthRoutes.APRESENTACAO) { inclusive = true }
                    }
                },
            )
        }
        composable(AuthRoutes.PERMISSOES) {
            PermissoesScreen(
                onContinuar = onAutenticado,
                onPermissaoBloqueada = { navController.navigate(AuthRoutes.PERMISSAO_BLOQUEADA) },
            )
        }
        composable(AuthRoutes.PERMISSAO_BLOQUEADA) {
            PermissaoBloqueadaScreen()
        }
    }
}
