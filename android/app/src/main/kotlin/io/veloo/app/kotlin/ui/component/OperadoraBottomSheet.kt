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
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import io.signallq.app.ui.OperadoraSource
import io.signallq.app.ui.ResolvedOperadoraContact
import io.signallq.app.ui.ResolvedOperadoraIdentity
import io.signallq.app.ui.whatsappUrl

private val idsMajores = listOf("vivo_fibra", "claro_net", "tim_live", "oi_fibra")

// Verde oficial da marca WhatsApp — intencionalmente fora da paleta semantica SignallQ,
// mesmo criterio de "cor de marca de terceiro" usado nos badges de operadora.
private val whatsappGreen = Color(0xFF25D366)

/**
 * Bottom sheet "Falar com a operadora" — GH#970. A secao "Sua operadora" agora usa a
 * cadeia local -> diretorio remoto (worker `signallq-diagnostic`) -> fallback generico
 * ([io.signallq.app.ui.OperadoraDirectoryResolver]), via [resolveOperadoraIdentidadeLocal]/
 * [resolveOperadoraIdentidadeRemota] (identidade) e [resolveOperadoraContatoLocal]/
 * [resolveOperadoraContatoRemoto] (contato). A lista "Outras operadoras" continua 100%
 * local (as ~12 principais, [BancoOperadoras.lista]) — sem mudanca de comportamento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperadoraBottomSheet(
    connectionType: String?,
    ispNome: String?,
    operadoraMovel: String?,
    onDismiss: () -> Unit,
    resolveOperadoraIdentidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity? =
        { _, _ -> null },
    resolveOperadoraContatoLocal: (String?, Boolean) -> ResolvedOperadoraContact? =
        { _, _ -> null },
    resolveOperadoraIdentidadeRemota: suspend (String?, Boolean) -> ResolvedOperadoraIdentity =
        { nome, _ ->
            ResolvedOperadoraIdentity(
                displayName = nome ?: "Operadora",
                monograma = nome?.firstOrNull()?.uppercase() ?: "?",
                corMarca = null,
                logoRes = null,
                logoUrl = null,
                source = OperadoraSource.FALLBACK,
            )
        },
    resolveOperadoraContatoRemoto: suspend (String?, Boolean) -> ResolvedOperadoraContact =
        { nome, _ ->
            ResolvedOperadoraContact(
                displayName = nome ?: "Operadora",
                sacPhone = null,
                whatsapp = null,
                site = null,
                source = OperadoraSource.FALLBACK,
            )
        },
) {
    val c = LocalLkTokens.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val viaMovel = connectionType?.equals("movel", ignoreCase = true) == true
    val nomeParaResolver = if (viaMovel) operadoraMovel else ispNome

    // So pra excluir da lista "Outras operadoras" (sempre local) quando o match tambem
    // for local — operadora resolvida via diretorio remoto nunca esta em BancoOperadoras.lista.
    val operadoraDetectadaLocal = BancoOperadoras.resolver(nomeParaResolver)

    val identidadeDetectada =
        rememberResolvedOperadoraIdentity(
            ispNomeBruto = nomeParaResolver,
            viaMovel = viaMovel,
            resolveLocal = resolveOperadoraIdentidadeLocal,
            resolveRemoteOrFallback = resolveOperadoraIdentidadeRemota,
        )
    val contatoDetectado =
        rememberResolvedOperadoraContact(
            ispNomeBruto = nomeParaResolver,
            viaMovel = viaMovel,
            resolveLocal = resolveOperadoraContatoLocal,
            resolveRemoteOrFallback = resolveOperadoraContatoRemoto,
        )
    // FALLBACK = nem local nem diretorio remoto encontraram nada — trata como "nao detectada"
    // (mesmo comportamento de antes, so que agora so decide depois de tentar o remoto tambem).
    val operadoraDetectada =
        contatoDetectado?.takeIf { it.source != OperadoraSource.FALLBACK && it.hasAnyContact }

    val subtituloConexao =
        when {
            operadoraDetectada != null && viaMovel ->
                "Detectamos sua operadora pela rede móvel. Atendimento oficial."
            operadoraDetectada != null ->
                "Detectamos sua operadora pela rede fixa. Atendimento oficial."
            else ->
                "Não foi possível identificar sua operadora automaticamente. Escolha abaixo para ver os canais de atendimento."
        }

    val legendaDetectada = if (viaMovel) "SIM ativo · plano móvel" else "rede fixa"

    val outrasOperadoras =
        BancoOperadoras.lista.filter { it.id != operadoraDetectadaLocal?.id }

    val outrasNacionais = outrasOperadoras.filter { it.id in idsMajores }
    val outrasRegionais = outrasOperadoras.filter { it.id !in idsMajores }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = c.surfaceContainerLow,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
        ) {
            Text(
                text = "Falar com a operadora",
                style = MaterialTheme.typography.headlineSmall,
                color = c.textPrimary,
            )

            Spacer(Modifier.height(LkSpacing.sm))

            Text(
                text = subtituloConexao,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )

            Spacer(Modifier.height(LkSpacing.xl))

            if (operadoraDetectada != null && identidadeDetectada != null) {
                // Seção: operadora detectada (local ou via diretorio remoto)
                Overline(texto = "Sua operadora", color = c.textTertiary)
                Spacer(Modifier.height(LkSpacing.md))
                OperadoraDetectadaSection(
                    identidade = identidadeDetectada,
                    contato = operadoraDetectada,
                    legenda = legendaDetectada,
                    onDismiss = onDismiss,
                )
                Spacer(Modifier.height(LkSpacing.lg))

                // Seção: outras operadoras (só quando há detectada)
                if (outrasOperadoras.isNotEmpty()) {
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
    identidade: ResolvedOperadoraIdentity,
    contato: ResolvedOperadoraContact,
    legenda: String,
    onDismiss: () -> Unit,
) {
    val c = LocalLkTokens.current
    val context = LocalContext.current
    val ehLocal = contato.source == OperadoraSource.LOCAL

    Column(modifier = Modifier.fillMaxWidth()) {
        // Identificacao
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(c.surfaceContainer, RoundedCornerShape(LkRadius.card))
                    .padding(LkSpacing.base),
        ) {
            OperadoraBadge(identidade = identidade, size = 40.dp)
            Spacer(Modifier.width(LkSpacing.md))
            Column {
                Text(
                    text = contato.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = c.textPrimary,
                )
                Text(
                    text = legenda,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(LkSpacing.md))

        // WhatsApp primario
        val waUrl = contato.whatsappUrl()
        if (waUrl != null) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                    context.startActivity(intent)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LkRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = whatsappGreen),
            ) {
                Text(
                    text = "Falar no WhatsApp",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Spacer(Modifier.height(LkSpacing.sm))
        }

        // Ligar + App (App so quando ha grupo conhecido — hoje so pra OperadoraSource.LOCAL,
        // o diretorio remoto GH#965 nao tem esse dado, nunca inventamos)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            if (contato.sacPhone != null) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contato.sacPhone}"))
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
                        text = if (ehLocal) "Ligar *${contato.sacPhone}" else "Ligar ${contato.sacPhone}",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.textPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            if (contato.grupo != null) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${contato.grupo}"))
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
                        text = "App ${contato.grupo}",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.textPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun OutraOperadoraRow(operadora: ContatoOperadora) {
    val c = LocalLkTokens.current
    val context = LocalContext.current

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(c.surfaceContainer, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.base),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OperadoraBadge(operadora = operadora, size = 36.dp)

        Spacer(Modifier.width(LkSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = operadora.nome,
                style = MaterialTheme.typography.titleSmall,
                color = c.textPrimary,
            )
            if (operadora.whatsapp != null) {
                Text(
                    text = "WhatsApp · ligar *${operadora.sac}",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            } else {
                Text(
                    text = "ligar *${operadora.sac}",
                    style = MaterialTheme.typography.bodySmall,
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
