package io.signallq.app.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.R
import io.signallq.app.ui.BancoOperadoras
import io.signallq.app.ui.ContatoOperadora
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens

private val idsMajores = listOf("vivo_fibra", "claro_net", "tim_live", "oi_fibra")

// Verde oficial da marca WhatsApp — intencionalmente fora da paleta semantica SignallQ,
// mesmo criterio de "cor de marca de terceiro" usado nos badges de operadora.
private val whatsappGreen = Color(0xFF25D366)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperadoraBottomSheet(
    connectionType: String?,
    ispNome: String?,
    operadoraMovel: String?,
    onDismiss: () -> Unit,
) {
    val c = LocalLkTokens.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val nomeParaResolver = if (connectionType?.equals("movel", ignoreCase = true) == true) operadoraMovel else ispNome
    val operadoraDetectada = BancoOperadoras.resolver(nomeParaResolver)

    val subtituloConexao =
        when {
            operadoraDetectada != null && connectionType?.equals("movel", ignoreCase = true) == true ->
                "Detectamos sua operadora pela rede móvel. Atendimento oficial."
            operadoraDetectada != null ->
                "Detectamos sua operadora pela rede fixa. Atendimento oficial."
            else ->
                "Não foi possível identificar sua operadora automaticamente. Escolha abaixo para ver os canais de atendimento."
        }

    val legendaDetectada =
        if (connectionType?.equals("movel", ignoreCase = true) == true) "SIM ativo · plano móvel" else "rede fixa"

    val outrasOperadoras =
        BancoOperadoras.lista.filter { it.id != operadoraDetectada?.id }

    val outrasNacionais = outrasOperadoras.filter { it.id in idsMajores }
    val outrasRegionais = outrasOperadoras.filter { it.id !in idsMajores }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.bgPrimary,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LkSpacing.xl)
                    .navigationBarsPadding(),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Falar com a operadora",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = c.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Fechar",
                        tint = c.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(LkSpacing.xs))

            Text(
                text = subtituloConexao,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )

            Spacer(Modifier.height(LkSpacing.lg))
            HorizontalDivider(color = c.border, thickness = 1.dp)
            Spacer(Modifier.height(LkSpacing.lg))

            if (operadoraDetectada != null) {
                // Seção: operadora detectada
                Overline(texto = "Sua operadora", color = c.textTertiary)
                Spacer(Modifier.height(LkSpacing.md))
                OperadoraDetectadaSection(
                    operadora = operadoraDetectada,
                    legenda = legendaDetectada,
                    onDismiss = onDismiss,
                )
                Spacer(Modifier.height(LkSpacing.lg))

                // Seção: outras operadoras (só quando há detectada)
                if (outrasOperadoras.isNotEmpty()) {
                    HorizontalDivider(color = c.border, thickness = 1.dp)
                    Spacer(Modifier.height(LkSpacing.lg))
                    Overline(texto = "Não é a sua? Outras operadoras", color = c.textTertiary)
                    Spacer(Modifier.height(LkSpacing.md))
                    if (outrasNacionais.isNotEmpty()) {
                        outrasNacionais.forEach { op ->
                            OutraOperadoraRow(operadora = op)
                            Spacer(Modifier.height(LkSpacing.sm))
                        }
                    }
                    if (outrasRegionais.isNotEmpty()) {
                        if (outrasNacionais.isNotEmpty()) {
                            Spacer(Modifier.height(LkSpacing.xs))
                            Overline(texto = "Regionais", color = c.textTertiary)
                            Spacer(Modifier.height(LkSpacing.sm))
                        }
                        outrasRegionais.forEach { op ->
                            OutraOperadoraRow(operadora = op)
                            Spacer(Modifier.height(LkSpacing.sm))
                        }
                    }
                    Spacer(Modifier.height(LkSpacing.md))
                }
            } else {
                // Seção: nenhuma detectada — mostrar todas com divisão nacional/regional
                HorizontalDivider(color = c.border, thickness = 1.dp)
                Spacer(Modifier.height(LkSpacing.lg))
                Overline(texto = "Operadoras disponíveis", color = c.textTertiary)
                Spacer(Modifier.height(LkSpacing.md))
                val nacionais = BancoOperadoras.lista.filter { it.id in idsMajores }
                val regionais = BancoOperadoras.lista.filter { it.id !in idsMajores }
                nacionais.forEach { op ->
                    OutraOperadoraRow(operadora = op)
                    Spacer(Modifier.height(LkSpacing.sm))
                }
                if (regionais.isNotEmpty()) {
                    Spacer(Modifier.height(LkSpacing.xs))
                    Overline(texto = "Regionais", color = c.textTertiary)
                    Spacer(Modifier.height(LkSpacing.sm))
                    regionais.forEach { op ->
                        OutraOperadoraRow(operadora = op)
                        Spacer(Modifier.height(LkSpacing.sm))
                    }
                }
                Spacer(Modifier.height(LkSpacing.md))
            }

            HorizontalDivider(color = c.border, thickness = 1.dp)
            Spacer(Modifier.height(LkSpacing.md))

            Text(
                text = "O SignallQ não tem vínculo com as operadoras. Você será levado ao canal oficial de cada uma.",
                style = MaterialTheme.typography.labelMedium,
                color = c.textTertiary,
            )

            Spacer(Modifier.height(LkSpacing.lg))
        }
    }
}

@Composable
private fun OperadoraDetectadaSection(
    operadora: ContatoOperadora,
    legenda: String,
    onDismiss: () -> Unit,
) {
    val c = LocalLkTokens.current
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // Identificacao
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OperadoraBadge(operadora = operadora, size = 40.dp)
            Spacer(Modifier.width(LkSpacing.md))
            Column {
                Text(
                    text = operadora.nome,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = c.textPrimary,
                )
                Text(
                    text = legenda,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(LkSpacing.md))

        // WhatsApp primario
        if (operadora.whatsapp != null) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/55${operadora.whatsapp}"))
                    context.startActivity(intent)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LkRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = whatsappGreen),
            ) {
                Text(
                    text = "Falar no WhatsApp",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Spacer(Modifier.height(LkSpacing.sm))
        }

        // Ligar + App
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${operadora.sac}"))
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Call,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = c.textPrimary,
                )
                Spacer(Modifier.width(LkSpacing.xs))
                Text(
                    text = "Ligar *${operadora.sac}",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textPrimary,
                    fontWeight = FontWeight.Medium,
                )
            }
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${operadora.grupo}"))
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = c.textPrimary,
                )
                Spacer(Modifier.width(LkSpacing.xs))
                Text(
                    text = "App ${operadora.grupo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textPrimary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun OutraOperadoraRow(operadora: ContatoOperadora) {
    val c = LocalLkTokens.current
    val context = LocalContext.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OperadoraBadge(operadora = operadora, size = 36.dp)

        Spacer(Modifier.width(LkSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = operadora.nome,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = c.textPrimary,
            )
            if (operadora.whatsapp != null) {
                Text(
                    text = "WhatsApp · ligar *${operadora.sac}",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textSecondary,
                )
            } else {
                Text(
                    text = "ligar *${operadora.sac}",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textSecondary,
                )
            }
        }

        if (operadora.whatsapp != null) {
            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/55${operadora.whatsapp}"))
                    context.startActivity(intent)
                },
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .background(whatsappGreen, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhoneAndroid,
                        contentDescription = stringResource(R.string.cd_abrir_whatsapp),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
