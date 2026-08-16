package io.signallq.app.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex

/**
 * Overlay "Ping" (bottom sheet de latência) — extraído do corpo de [AppShell] pela issue #1695
 * (épico #1647), entrada de exemplo do [AppShellOverlayRegistry].
 *
 * Único overlay do registro sem `AnimatedVisibility`: `PingScreen` já é uma `ModalBottomSheet`
 * com sua própria animação de entrada/saída — envolvê-la também em `AnimatedVisibility`
 * duplicaria a transição. Mantido igual ao comportamento original em `AppShell.kt`.
 */
@Composable
internal fun AppShellPingOverlay(overlayStack: MutableList<AppShellOverlay>) {
    if (AppShellOverlay.Ping in overlayStack) {
        Box(modifier = Modifier.zIndex(rememberOverlayZIndex(AppShellOverlay.Ping, overlayStack))) {
            PingScreen(onDismiss = { overlayStack.remove(AppShellOverlay.Ping) })
        }
    }
}
