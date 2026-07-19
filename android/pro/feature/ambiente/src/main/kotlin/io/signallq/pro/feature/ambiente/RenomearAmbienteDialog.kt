package io.signallq.pro.feature.ambiente

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.signallq.pro.core.designsystem.ProButton
import io.signallq.pro.core.designsystem.ProButtonVariant
import io.signallq.pro.core.designsystem.ProTextField
import io.signallq.pro.core.designsystem.corSurfaceOverlay

/** Tela 2.7 -- renomear ambiente. */
@Composable
fun RenomearAmbienteDialog(
    nomeAtual: String,
    onConfirmar: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var nome by remember { mutableStateOf(nomeAtual) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = corSurfaceOverlay(),
        title = { Text("Renomear ambiente") },
        text = {
            ProTextField(valor = nome, onValorChange = { nome = it })
        },
        confirmButton = {
            ProButton(
                texto = "Salvar",
                onClick = { if (nome.isNotBlank()) onConfirmar(nome.trim()) },
                variant = ProButtonVariant.TEXTO,
            )
        },
        dismissButton = {
            ProButton(texto = "Cancelar", onClick = onDismiss, variant = ProButtonVariant.TEXTO)
        },
    )
}
