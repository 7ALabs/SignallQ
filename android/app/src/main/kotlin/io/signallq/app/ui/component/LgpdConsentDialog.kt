package io.signallq.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens

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
    val c = LocalLkTokens.current
    Dialog(
        onDismissRequest = { /* nao dismissivel — escolha obrigatoria */ },
        properties =
            DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
    ) {
        Surface(
            shape = RoundedCornerShape(LkRadius.dialog),
            color = c.surface,
            tonalElevation = LkSpacing.xs,
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(LkSpacing.xl)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(LkSpacing.xxxl)
                            .clip(RoundedCornerShape(LkRadius.pill))
                            .background(c.successContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        tint = c.onSuccessContainer,
                        modifier = Modifier.size(LkSpacing.xl),
                    )
                }
                Text(
                    text = "Ajude a melhorar o SignallQ",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                Text(
                    text = "Com sua permissão, usamos dados anônimos para entender falhas e melhorar a experiência. Isso não inclui senhas, conteúdo acessado ou dados pessoais da sua rede.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant,
                )

                Text(
                    text = "O que e coletado:",
                    style = MaterialTheme.typography.labelLarge,
                )

                val itens =
                    listOf(
                        "Eventos de uso de funcionalidades (Firebase Analytics)",
                        "Resultados anonimos de diagnostico de rede (latencia, perda de pacotes, score)",
                        "Modelo do dispositivo e versao do Android",
                        "Versao do app e canal de distribuicao",
                    )
                itens.forEach { item ->
                    Text(
                        text = "· $item",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant,
                        modifier = Modifier.padding(start = LkSpacing.sm),
                    )
                }

                Text(
                    text = "Nenhum dado pessoal identificavel (nome, localizacao, contatos, IMEI) e coletado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                )

                Text(
                    text = "Voce pode alterar esta preferencia a qualquer momento em Ajustes > Privacidade.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(LkSpacing.sm))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                ) {
                    OutlinedButton(
                        onClick = onRecusar,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Continuar sem compartilhar")
                    }

                    Button(
                        onClick = onAceitar,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Permitir dados de uso")
                    }
                }
            }
        }
    }
}
