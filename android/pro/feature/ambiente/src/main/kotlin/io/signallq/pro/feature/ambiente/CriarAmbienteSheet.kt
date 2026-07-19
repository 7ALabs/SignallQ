package io.signallq.pro.feature.ambiente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.signallq.pro.core.designsystem.ProButton
import io.signallq.pro.core.designsystem.ProTextField
import io.signallq.pro.core.designsystem.corSurfaceOverlay

/** Tela 2.6 -- criar ambiente. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CriarAmbienteSheet(
    onConfirmar: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var nome by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = corSurfaceOverlay()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Novo ambiente")
            ProTextField(
                valor = nome,
                onValorChange = { nome = it },
                rotulo = "Nome (ex.: Sala, Quarto 1)",
                modifier = Modifier.fillMaxWidth(),
            )
            ProButton(
                texto = "Criar",
                onClick = { if (nome.isNotBlank()) onConfirmar(nome.trim()) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
