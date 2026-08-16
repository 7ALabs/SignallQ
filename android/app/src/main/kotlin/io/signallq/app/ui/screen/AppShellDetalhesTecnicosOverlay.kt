package io.signallq.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import io.signallq.app.core.network.contracts.localdevice.LocalNetworkDeviceSnapshot
import io.signallq.app.feature.speedtest.ResultadoSpeedtest

/**
 * Overlay "Detalhes da conexão" (Feature #550, issue #1475), aberto a partir do
 * ResultadoVelocidadeScreen — extraído do corpo de [AppShell] pela issue #1695 (épico #1647),
 * entrada de exemplo do [AppShellOverlayRegistry].
 *
 * Só fica visível com um [resultadoSpeedtest] não nulo — igual à condição original em
 * `AppShell.kt` (overlay aberto sem resultado ainda carregado não deve aparecer vazio).
 */
@Composable
internal fun AppShellDetalhesTecnicosOverlay(
    overlayStack: MutableList<AppShellOverlay>,
    resultadoSpeedtest: ResultadoSpeedtest?,
    localizacaoServidor: String?,
    localDevice: LocalNetworkDeviceSnapshot?,
) {
    AnimatedVisibility(
        visible = AppShellOverlay.DetalhesTecnicos in overlayStack && resultadoSpeedtest != null,
        modifier = Modifier.zIndex(rememberOverlayZIndex(AppShellOverlay.DetalhesTecnicos, overlayStack)),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        resultadoSpeedtest?.let { resultado ->
            DetalhesTecnicosScreen(
                resultado = resultado,
                localizacaoServidor = localizacaoServidor,
                localDevice = localDevice,
                onVoltar = { overlayStack.remove(AppShellOverlay.DetalhesTecnicos) },
            )
        }
    }
}
