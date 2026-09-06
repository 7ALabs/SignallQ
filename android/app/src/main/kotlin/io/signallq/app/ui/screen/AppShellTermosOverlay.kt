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
 * Overlay "Termos de uso" do Perfil (GH#1358) — extraído do corpo de [AppShell] pela
 * issue #1695 (épico #1647), como uma das entradas de exemplo do [AppShellOverlayRegistry].
 * Reusa `TermosDeUsoScreen`, o mesmo composable já usado pelo Onboarding
 * (`OnboardingOverlay.TERMOS`), sem duplicar conteúdo legal.
 */
@Composable
internal fun AppShellTermosOverlay(overlayStack: MutableList<AppShellOverlay>) {
    AnimatedVisibility(
        visible = AppShellOverlay.Termos in overlayStack,
        modifier = Modifier.zIndex(rememberOverlayZIndex(AppShellOverlay.Termos, overlayStack)),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        TermosDeUsoScreen(onVoltar = { overlayStack.remove(AppShellOverlay.Termos) })
    }
}
