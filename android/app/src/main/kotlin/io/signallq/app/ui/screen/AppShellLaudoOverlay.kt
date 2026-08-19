package io.signallq.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.feature.diagnostico.SnapshotDiagnostico

/**
 * Overlay do Laudo (relatório de diagnóstico, [LaudoScreen]) — extraído de `AppShell.kt` pela
 * issue #1659 (épico #1647), aplicando ao overlay que ainda faltava o mesmo padrão já usado em
 * `AppShellResultadoVelocidadeOverlay.kt` (#1714) e `AppShellDetalhesTecnicosOverlay.kt` (#1695).
 * Extração mecânica: mesmo comportamento, sem reordenação de conteúdo, CTA ou rótulo novo.
 */
@Stable
internal data class AppShellLaudoEntry(
    val snapshotDiagnostico: SnapshotDiagnostico,
    val ultimaMedicao: MedicaoEntity?,
    val nomeUsuario: String,
    val operadora: String,
    val ssid: String?,
    val ipLocal: String?,
    val ipPublico: String?,
    val velocidadeContratadaMbps: Int?,
    val conectado: Boolean,
    val onVoltar: () -> Unit,
)

@Composable
internal fun AppShellLaudoOverlay(
    overlayStack: MutableList<AppShellOverlay>,
    entry: AppShellLaudoEntry,
) {
    AnimatedVisibility(
        visible = AppShellOverlay.Laudo in overlayStack,
        modifier =
            Modifier
                .zIndex(rememberOverlayZIndex(AppShellOverlay.Laudo, overlayStack))
                .testTag(TAG_OVERLAY_LAUDO),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        LaudoScreen(
            snapshotDiagnostico = entry.snapshotDiagnostico,
            ultimaMedicao = entry.ultimaMedicao,
            nomeUsuario = entry.nomeUsuario,
            operadora = entry.operadora,
            ssid = entry.ssid,
            ipLocal = entry.ipLocal,
            ipPublico = entry.ipPublico,
            onVoltar = entry.onVoltar,
            velocidadeContratadaMbps = entry.velocidadeContratadaMbps,
            conectado = entry.conectado,
        )
    }
}

internal const val TAG_OVERLAY_LAUDO = "appshell_overlay_laudo"
