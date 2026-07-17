package io.signallq.app.ui.component

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PhoneAndroid
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.R
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.ResolvedOperadoraContact
import io.signallq.app.ui.ResolvedOperadoraIdentity
import io.signallq.app.ui.whatsappUrl

/**
 * Card de contato da operadora — GH#965/#970. [identidade] alimenta so o badge visual
 * (logo bundled/remota/monograma, via [OperadoraBadge]); [contato] alimenta os canais
 * (ligar/WhatsApp/site/app). Os dois vem do mesmo nome bruto resolvido pela mesma cadeia
 * local -> remoto -> fallback ([io.signallq.app.ui.OperadoraDirectoryResolver]), so que
 * calculados separadamente porque identidade e contato tem forma diferente no diretorio
 * remoto — ver kdoc de [ResolvedOperadoraContact] e [ResolvedOperadoraIdentity].
 *
 * Botao "Abrir app na Play Store" so aparece quando [ResolvedOperadoraContact.grupo] esta
 * preenchido — hoje so acontece para [OperadoraSource.LOCAL] (as ~12 operadoras
 * catalogadas), nunca inventado para operadora resolvida via diretorio remoto.
 */
@Composable
fun OperadoraContactCard(
    identidade: ResolvedOperadoraIdentity?,
    contato: ResolvedOperadoraContact?,
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
        if (contato != null && contato.hasAnyContact) {
            // Estado: operadora reconhecida (local ou via diretorio remoto)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (identidade != null) {
                    OperadoraBadge(identidade = identidade, size = 32.dp)
                    Spacer(Modifier.width(LkSpacing.sm))
                }
                Text(
                    text = stringResource(R.string.operadora_falar_com, contato.displayName),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                )
            }
            Spacer(Modifier.height(LkSpacing.xs))
            Text(
                text = stringResource(R.string.operadora_contact_mencione),
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(LkSpacing.md))

            if (contato.sacPhone != null) {
                Button(
                    onClick = {
                        val intent =
                            Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${contato.sacPhone}")
                            }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LkRadius.button),
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(LkSpacing.xs))
                    Text(
                        text = stringResource(R.string.operadora_ligar_agora, contato.sacPhone),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W600,
                    )
                }
            }

            if (contato.whatsapp != null) {
                Spacer(Modifier.height(LkSpacing.sm))
                OutlinedButton(
                    onClick = {
                        val intent =
                            Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(contato.whatsappUrl())
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
                        text = stringResource(R.string.operadora_contact_whatsapp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W600,
                        color = LkColors.success,
                    )
                }
            }

            if (contato.site != null || contato.grupo != null) {
                Spacer(Modifier.height(LkSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                ) {
                    if (contato.site != null) {
                        OutlinedButton(
                            onClick = {
                                val intent =
                                    Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(contato.site)
                                    }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(LkRadius.button),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = c.textPrimary,
                            )
                            Spacer(Modifier.width(LkSpacing.xs))
                            Text(
                                text = stringResource(R.string.operadora_contact_site),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.W600,
                                color = c.textPrimary,
                            )
                        }
                    }
                    if (contato.grupo != null) {
                        OutlinedButton(
                            onClick = {
                                val intent =
                                    Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("market://search?q=${contato.grupo}")
                                    }
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
                                text = stringResource(R.string.operadora_contact_playstore),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.W600,
                                color = c.textPrimary,
                            )
                        }
                    }
                }
            }
        } else {
            // Estado: fallback — operadora nao reconhecida (nem local, nem diretorio remoto)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = c.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(LkSpacing.sm))
                Text(
                    text = stringResource(R.string.operadora_contact_nao_lista),
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
                    text = stringResource(R.string.operadora_contact_buscar_anatel),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W600,
                    color = c.primary,
                )
            }
        }
    }
}
