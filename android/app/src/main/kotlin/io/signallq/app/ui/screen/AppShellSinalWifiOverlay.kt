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
 * Overlay "Sinal WiFi" (GH#1201, indicador dinâmico de RSSI/PHY/padrão) do hub Ferramentas —
 * extraído do corpo de [AppShell] pela issue #1695 (épico #1647), entrada de exemplo do
 * [AppShellOverlayRegistry].
 */
@Composable
internal fun AppShellSinalWifiOverlay(
    overlayStack: MutableList<AppShellOverlay>,
    temPermissaoLocalizacao: Boolean,
    localizacaoBloqueadaPermanentemente: Boolean,
    onSolicitarPermissaoLocalizacao: () -> Unit,
) {
    AnimatedVisibility(
        visible = AppShellOverlay.SinalWifi in overlayStack,
        modifier = Modifier.zIndex(rememberOverlayZIndex(AppShellOverlay.SinalWifi, overlayStack)),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        SinalWifiScreen(
            temPermissaoLocalizacao = temPermissaoLocalizacao,
            localizacaoBloqueadaPermanentemente = localizacaoBloqueadaPermanentemente,
            onSolicitarPermissaoLocalizacao = onSolicitarPermissaoLocalizacao,
            onVoltar = { overlayStack.remove(AppShellOverlay.SinalWifi) },
        )
    }
}
