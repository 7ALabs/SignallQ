package io.signallq.pro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.signallq.pro.ui.screen.PainelPlaceholderScreen

// Esqueleto do MVP0 (issue #1157) — uma unica rota placeholder. As rotas reais (auth,
// atendimento, ambiente, medicao, laudo...) chegam na Fase 2 em modulos :pro:feature:* proprios,
// nunca direto aqui (ver docs_ai/plataforma/13_..._v1.md §5).
private const val ROTA_PAINEL = "painel"

@Composable
fun ProNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = ROTA_PAINEL) {
        composable(ROTA_PAINEL) {
            PainelPlaceholderScreen()
        }
    }
}
