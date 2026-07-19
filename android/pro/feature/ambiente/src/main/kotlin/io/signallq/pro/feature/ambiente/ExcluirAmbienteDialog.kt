package io.signallq.pro.feature.ambiente

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.signallq.pro.core.designsystem.ProButton
import io.signallq.pro.core.designsystem.ProButtonVariant
import io.signallq.pro.core.designsystem.corSurfaceOverlay

/**
 * Tela 2.9 -- confirmar exclusão. A recusa por medição associada é mostrada como erro
 * na própria [AmbientesScreen] (StateCard/erro), não neste diálogo de confirmação --
 * o bloqueio só é conhecido depois de tentar excluir (handoff Fase 2, #1161).
 */
@Composable
fun ExcluirAmbienteDialog(
    nomeAmbiente: String,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = corSurfaceOverlay(),
        title = { Text("Excluir ambiente") },
        text = { Text("Tem certeza que deseja excluir \"$nomeAmbiente\"?") },
        confirmButton = {
            ProButton(texto = "Excluir", onClick = onConfirmar, variant = ProButtonVariant.DESTRUTIVO)
        },
        dismissButton = {
            ProButton(texto = "Cancelar", onClick = onDismiss, variant = ProButtonVariant.TEXTO)
        },
    )
}
