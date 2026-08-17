package io.signallq.app.ui.screen

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import io.signallq.app.ads.ConsentManager
import io.signallq.app.ui.encontrarActivity

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
    val activity = LocalContext.current.encontrarActivity()
    // Recalculado a cada abertura do overlay: o status da UMP muda depois que o usuário responde
    // o formulário inicial, e a tela pode ser aberta antes disso na mesma sessão.
    var precisaOpcoesAnuncios by remember { mutableStateOf(false) }
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
            onAbrirOpcoesAnuncios = {
                // Não fecha o overlay: o formulário da UMP abre por cima e o usuário volta para
                // a tela de Privacidade onde estava.
                activity?.let { ConsentManager.mostrarOpcoesPrivacidade(it) }
            },
        )
    }
}
