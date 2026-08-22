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
 * Overlay "Novidades" do Perfil (GH#1358) — extraído do corpo de [AppShell]
 * pela issue #1695 (épico #1647), entrada de exemplo do [AppShellOverlayRegistry].
 */
@Composable
internal fun AppShellNovidadesOverlay(
    overlayStack: MutableList<AppShellOverlay>,
    appVersion: String,
) {
    AnimatedVisibility(
        visible = AppShellOverlay.Novidades in overlayStack,
        modifier = Modifier.zIndex(rememberOverlayZIndex(AppShellOverlay.Novidades, overlayStack)),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        NovidadesScreen(
            appVersion = appVersion,
            onVoltar = { overlayStack.remove(AppShellOverlay.Novidades) },
        )
    }
}
