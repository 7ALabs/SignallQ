package io.linka.app.kotlin.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.linka.app.kotlin.ui.ContatoOperadora
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkRadius
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LocalLkTokens

@Composable
fun OperadoraContactCard(
    operadora: ContatoOperadora?,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val context = LocalContext.current

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgSecondary)
                .padding(LkSpacing.lg),
    ) {
        if (operadora != null) {
            // Estado: operadora reconhecida
            Text(
                text = "Falar com ${operadora.nome}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(LkSpacing.xs))
            Text(
                text = "Mencione os dados deste diagnóstico ao ligar.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(LkSpacing.md))

            Button(
                onClick = {
                    val intent =
                        Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${operadora.sac}")
                        }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LkRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Call,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(LkSpacing.xs))
                Text(
                    text = "Ligar agora · ${operadora.sac}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W600,
                )
            }

            if (operadora.whatsapp != null) {
                Spacer(Modifier.height(LkSpacing.sm))
                OutlinedButton(
                    onClick = {
                        val intent =
                            Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://wa.me/55${operadora.whatsapp}")
                            }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LkRadius.button),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = LkColors.success,
                    )
                    Spacer(Modifier.width(LkSpacing.xs))
                    Text(
                        text = "WhatsApp",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W600,
                        color = LkColors.success,
                    )
                }
            }
        } else {
            // Estado: fallback — operadora não reconhecida
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = c.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(LkSpacing.sm))
                Text(
                    text = "Sua operadora não está na nossa lista",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                )
            }
            Spacer(Modifier.height(LkSpacing.md))
            OutlinedButton(
                onClick = {
                    val intent =
                        Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://www.anatel.gov.br/consumidor/acessar-central")
                        }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                Text(
                    text = "Buscar no site da Anatel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W600,
                    color = LkColors.accent,
                )
            }
        }
    }
}
