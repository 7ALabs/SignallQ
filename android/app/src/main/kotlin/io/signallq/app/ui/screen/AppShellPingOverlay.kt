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
 * Overlay "Ping" (tela cheia de tempo de resposta) — extraído do corpo de [AppShell] pela
 * issue #1695 (épico #1647), entrada de exemplo do [AppShellOverlayRegistry].
 *
 * issue #1665 migrou [PingScreen] de `ModalBottomSheet` para tela cheia roteada (mesmo padrão
 * de [AppShellDnsOverlay]) — a transição passa a ser a mesma dos demais overlays de tela cheia
 * em vez da animação própria do bottom sheet.
 */
@Composable
internal fun AppShellPingOverlay(overlayStack: MutableList<AppShellOverlay>) {
    AnimatedVisibility(
        visible = AppShellOverlay.Ping in overlayStack,
        modifier = Modifier.zIndex(rememberOverlayZIndex(AppShellOverlay.Ping, overlayStack)),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        PingScreen(onVoltar = { overlayStack.remove(AppShellOverlay.Ping) })
    }
}
