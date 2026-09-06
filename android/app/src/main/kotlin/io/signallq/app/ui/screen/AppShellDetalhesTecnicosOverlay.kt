package io.signallq.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import io.signallq.app.core.network.contracts.localdevice.LocalNetworkDeviceSnapshot
import io.signallq.app.feature.speedtest.ResultadoSpeedtest

/**
 * Overlay "Detalhes técnicos" (Feature #550, issue #1475), aberto a partir do
 * ResultadoVelocidadeScreen — extraído do corpo de [AppShell] pela issue #1695 (épico #1647),
 * entrada de exemplo do [AppShellOverlayRegistry].
 *
 * Só fica visível com um [resultadoSpeedtest] não nulo — igual à condição original em
 * `AppShell.kt` (overlay aberto sem resultado ainda carregado não deve aparecer vazio). O
 * `testTag` no container existe só para o teste de caracterização conseguir distinguir essa
 * guarda de `visible` do `?.let` interno (que por si só já omite o conteúdo quando o resultado é
 * nulo, mas não impede o container `AnimatedVisibility` de compor) — ver
 * `AppShellOverlayRegistryTest`.
 */
@Composable
internal fun AppShellDetalhesTecnicosOverlay(
    overlayStack: MutableList<AppShellOverlay>,
    resultadoSpeedtest: ResultadoSpeedtest?,
    localizacaoServidor: String?,
    localDevice: LocalNetworkDeviceSnapshot?,
    onGerarLaudo: () -> Unit = {},
) {
    AnimatedVisibility(
        visible = AppShellOverlay.DetalhesTecnicos in overlayStack,
        modifier =
            Modifier
                .zIndex(rememberOverlayZIndex(AppShellOverlay.DetalhesTecnicos, overlayStack))
                .testTag("appshell_overlay_detalhes_tecnicos"),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        // GH#1714 — sem resultado, o overlay mostra estado vazio honesto em vez de não compor
        // nada. Antes, o container ficava vazio e o back consumia um `pop()` invisível.
        val fechar = { overlayStack.remove(AppShellOverlay.DetalhesTecnicos) }
        if (resultadoSpeedtest == null) {
            ResultadoIndisponivelScreen(titulo = "Detalhes técnicos", onVoltar = { fechar() })
        } else {
            DetalhesTecnicosScreen(
                resultado = resultadoSpeedtest,
                localizacaoServidor = localizacaoServidor,
                localDevice = localDevice,
                onVoltar = { fechar() },
                onGerarLaudo = onGerarLaudo,
            )
        }
    }
}
