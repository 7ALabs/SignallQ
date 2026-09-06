package io.signallq.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.feature.dns.SnapshotBenchmarkDns

/**
 * Overlay "DNS" (GH#933, Fase 4 — migrou de `ModalBottomSheet` para tela cheia roteada, lógica
 * de benchmark preservada em `DnsScreen.kt`) — extraído do corpo de [AppShell] pela issue #1695
 * (épico #1647), entrada do [AppShellOverlayRegistry].
 */
@Composable
internal fun AppShellDnsOverlay(
    overlayStack: MutableList<AppShellOverlay>,
    snapshotDns: SnapshotBenchmarkDns,
    dnsResolverIp: String?,
    snapshotRede: SnapshotRede,
    onIniciarBenchmark: () -> Unit,
) {
    AnimatedVisibility(
        visible = AppShellOverlay.Dns in overlayStack,
        modifier = Modifier.zIndex(rememberOverlayZIndex(AppShellOverlay.Dns, overlayStack)),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        DnsScreen(
            snapshotDns = snapshotDns,
            dnsResolverIp = dnsResolverIp,
            snapshotRede = snapshotRede,
            onIniciarBenchmark = onIniciarBenchmark,
            onVoltar = { overlayStack.remove(AppShellOverlay.Dns) },
        )
    }
}
