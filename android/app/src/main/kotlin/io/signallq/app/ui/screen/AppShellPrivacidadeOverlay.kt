package io.signallq.app.ui.screen

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import io.signallq.app.ads.ConsentManager

/**
 * Overlay "Privacidade" do menu lateral e do Perfil — extraído do corpo de [AppShell] pela
 * issue #1695 (épico #1647), entrada de exemplo do [AppShellOverlayRegistry].
 *
 * [onAbrirGerenciarDados] fecha este overlay e abre a `DadosLocaisSheet` hoisted no AppShell
 * (permanece fora — a sheet não usa a pilha de overlays, é `showGerenciarDadosSheet` local).
 *
 * GH#1703 — é aqui que a `Activity` é resolvida para a entrada de opções de privacidade da UMP.
 * Fica neste arquivo, e não em `PrivacidadeScreen`, por dois motivos: a tela continua testável
 * sem `Activity` real (recebe só um `Boolean` e um callback), e o acoplamento com o SDK de ads
 * não desce para a camada de UI pura.
 */
@Composable
internal fun AppShellPrivacidadeOverlay(
    overlayStack: MutableList<AppShellOverlay>,
    onAbrirGerenciarDados: () -> Unit,
) {
    // `LocalActivity` (activity-compose 1.10+, projeto em 1.13.0) já resolve a Activity
    // hospedeira desembrulhando `ContextWrapper` — `LocalContext.current` NÃO é garantidamente uma
    // Activity. Um helper próprio foi escrito e descartado na revisão da PR #1709: eram 101 linhas
    // reimplementando o que a plataforma entrega.
    val activity = LocalActivity.current
    // Recalculado a cada abertura do overlay: o status da UMP muda depois que o usuário responde
    // o formulário inicial, e a tela pode ser aberta antes disso na mesma sessão.
    var precisaOpcoesAnuncios by remember { mutableStateOf(false) }
    var erroOpcoes by remember { mutableStateOf<String?>(null) }
    val privacidadeAberta = AppShellOverlay.Privacidade in overlayStack
    LaunchedEffect(privacidadeAberta, activity) {
        precisaOpcoesAnuncios =
            activity != null &&
            ConsentManager.precisaOferecerOpcoesPrivacidade(activity)
    }

    AnimatedVisibility(
        visible = privacidadeAberta,
        modifier = Modifier.zIndex(rememberOverlayZIndex(AppShellOverlay.Privacidade, overlayStack)),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        PrivacidadeScreen(
            onVoltar = { overlayStack.remove(AppShellOverlay.Privacidade) },
            onAbrirGerenciarDados = {
                overlayStack.remove(AppShellOverlay.Privacidade)
                onAbrirGerenciarDados()
            },
            mostrarOpcoesAnuncios = precisaOpcoesAnuncios,
            erroOpcoesAnuncios = erroOpcoes,
            onErroOpcoesAnunciosExibido = { erroOpcoes = null },
            onAbrirOpcoesAnuncios = {
                // Não fecha o overlay: o formulário da UMP abre por cima e o usuário volta para
                // a tela de Privacidade onde estava.
                activity?.let { act ->
                    ConsentManager.mostrarOpcoesPrivacidade(act) { erro ->
                        // Ressalva R2 da revisão da PR #1709: sem isto, falhar ao carregar o
                        // formulário (sem rede, por exemplo) deixava o toque sem NENHUM retorno
                        // visível — só um log. Numa região GDPR, "não consigo abrir minhas opções
                        // de privacidade e o app não me diz nada" vira reclamação de política.
                        if (erro != null) {
                            erroOpcoes = "Não foi possível abrir agora. Verifique sua conexão e tente de novo."
                        }
                    }
                }
            },
        )
    }
}
