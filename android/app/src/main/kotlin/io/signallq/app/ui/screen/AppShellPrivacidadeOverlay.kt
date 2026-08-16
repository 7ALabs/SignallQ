package io.signallq.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex

/**
 * Overlay "Privacidade" do menu lateral e do Perfil — extraído do corpo de [AppShell] pela
 * issue #1695 (épico #1647), entrada de exemplo do [AppShellOverlayRegistry].
 *
 * [onAbrirGerenciarDados] fecha este overlay e abre a `DadosLocaisSheet` hoisted no AppShell
 * (permanece fora — a sheet não usa a pilha de overlays, é `showGerenciarDadosSheet` local).
 */
@Composable
internal fun AppShellPrivacidadeOverlay(
    overlayStack: MutableList<AppShellOverlay>,
    onAbrirGerenciarDados: () -> Unit,
) {
    AnimatedVisibility(
        visible = AppShellOverlay.Privacidade in overlayStack,
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
        )
    }
}
