package io.signallq.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Dialog de consentimento LGPD exibido no primeiro uso do app.
 *
 * Nao e cancellable: o usuario deve fazer uma escolha explicita.
 * Aceitar habilita Firebase Analytics e envio de telemetria anonima de diagnostico.
 * Recusar mantem o app funcional, sem coleta de dados.
 */
@Composable
fun LgpdConsentDialog(
    onAceitar: () -> Unit,
    onRecusar: () -> Unit,
) {
    Dialog(
        onDismissRequest = { /* nao dismissivel — escolha obrigatoria */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "Dados e privacidade",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Para melhorar o app, o SignallQ coleta dados anonimos sobre o uso de funcionalidades e resultados de diagnostico de rede.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "O que e coletado:",
                    style = MaterialTheme.typography.labelLarge,
                )

                Spacer(modifier = Modifier.height(4.dp))

                val itens = listOf(
                    "Eventos de uso de funcionalidades (Firebase Analytics)",
                    "Resultados anonimos de diagnostico de rede (latencia, perda de pacotes, score)",
                    "Modelo do dispositivo e versao do Android",
                    "Versao do app e canal de distribuicao",
                )
                itens.forEach { item ->
                    Text(
                        text = "· $item",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Nenhum dado pessoal identificavel (nome, localizacao, contatos, IMEI) e coletado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Voce pode alterar esta preferencia a qualquer momento em Ajustes > Privacidade.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onRecusar,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Recusar")
                    }

                    Button(
                        onClick = onAceitar,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Aceitar")
                    }
                }
            }
        }
    }
}
