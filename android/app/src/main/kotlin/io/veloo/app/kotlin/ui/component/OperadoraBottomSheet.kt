package io.veloo.app.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.veloo.app.ui.BancoOperadoras
import io.veloo.app.ui.ContatoOperadora
import io.veloo.app.ui.LkColors
import io.veloo.app.ui.LkRadius
import io.veloo.app.ui.LkSpacing
import io.veloo.app.ui.LocalLkTokens

private val corOperadora: Map<String, Color> =
    mapOf(
        "vivo_fibra" to Color(0xFF660099),
        "claro_net" to Color(0xFFED1C24),
        "tim_live" to Color(0xFF003D8F),
        "oi_fibra" to Color(0xFFFF8C00),
        "nio" to Color(0xFF00B4D8),
        "algar" to Color(0xFF0066CC),
        "unifique" to Color(0xFF00A651),
        "brisanet" to Color(0xFFFF6600),
        "desktop" to Color(0xFF1E3A5F),
        "ligga" to Color(0xFF8BC53F),
        "vero" to Color(0xFF7B2D8E),
        "giga_mais" to Color(0xFF00AEEF),
    )

private val idsMajores = listOf("vivo_fibra", "claro_net", "tim_live", "oi_fibra")

private fun corParaId(id: String): Color = corOperadora[id] ?: LkColors.accent

@Composable
private fun OperadoraLogo(
    operadora: ContatoOperadora,
    size: Int = 40,
) {
    val cor = corParaId(operadora.id)
    if (operadora.logoUrl != null) {
        Box(
            modifier =
                Modifier
                    .size(size.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = operadora.logoUrl,
                contentDescription = operadora.nome,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    } else {
        Box(
            modifier =
                Modifier
                    .size(size.dp)
                    .background(cor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = operadora.nome.first().uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size * 0.45).sp,
            )
        }
    }
}

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
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
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
                fontSize = 14.sp,
                color = c.textSecondary,
                lineHeight = 20.sp,
            )

            Spacer(Modifier.height(LkSpacing.lg))
            HorizontalDivider(color = c.border, thickness = 1.dp)
            Spacer(Modifier.height(LkSpacing.lg))

            if (operadoraDetectada != null) {
                // Seção: operadora detectada
                Text(
                    text = "SUA OPERADORA",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.8.sp,
                    color = c.textTertiary,
                )
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
                    Text(
                        text = "NÃO É A SUA? OUTRAS OPERADORAS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        letterSpacing = 0.8.sp,
                        color = c.textTertiary,
                    )
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
                            Text(
                                text = "REGIONAIS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.6.sp,
                                color = c.textTertiary,
                            )
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
                Text(
                    text = "OPERADORAS DISPONÍVEIS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 0.8.sp,
                    color = c.textTertiary,
                )
                Spacer(Modifier.height(LkSpacing.md))
                val nacionais = BancoOperadoras.lista.filter { it.id in idsMajores }
                val regionais = BancoOperadoras.lista.filter { it.id !in idsMajores }
                nacionais.forEach { op ->
                    OutraOperadoraRow(operadora = op)
                    Spacer(Modifier.height(LkSpacing.sm))
                }
                if (regionais.isNotEmpty()) {
                    Spacer(Modifier.height(LkSpacing.xs))
                    Text(
                        text = "REGIONAIS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.6.sp,
                        color = c.textTertiary,
                    )
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
                fontSize = 12.sp,
                color = c.textTertiary,
                lineHeight = 17.sp,
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
            OperadoraLogo(operadora = operadora, size = 40)
            Spacer(Modifier.width(LkSpacing.md))
            Column {
                Text(
                    text = operadora.nome,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = c.textPrimary,
                )
                Text(
                    text = legenda,
                    fontSize = 13.sp,
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
            ) {
                Text(
                    text = "Falar no WhatsApp",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
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
                    fontSize = 13.sp,
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
                    fontSize = 13.sp,
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
        OperadoraLogo(operadora = operadora, size = 36)

        Spacer(Modifier.width(LkSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = operadora.nome,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = c.textPrimary,
            )
            if (operadora.whatsapp != null) {
                Text(
                    text = "WhatsApp · ligar *${operadora.sac}",
                    fontSize = 12.sp,
                    color = c.textSecondary,
                )
            } else {
                Text(
                    text = "ligar *${operadora.sac}",
                    fontSize = 12.sp,
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
                            .background(Color(0xFF25D366), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhoneAndroid,
                        contentDescription = "WhatsApp",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
