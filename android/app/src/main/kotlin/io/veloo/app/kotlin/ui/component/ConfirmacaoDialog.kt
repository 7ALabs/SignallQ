package io.signallq.app.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun ConfirmacaoDialog(
    titulo: String,
    mensagem: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    textoBotaoConfirmar: String = "Confirmar",
    textoBotaoCancelar: String = "Cancelar",
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo, fontWeight = FontWeight.W600) },
        text = { Text(mensagem, fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text(textoBotaoConfirmar) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text(textoBotaoCancelar) }
        },
    )
}
