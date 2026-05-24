package io.linka.app.kotlin.ui.screen

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.ShareLocation
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LkTokens
import io.linka.app.kotlin.ui.LocalLkTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacidadeScreen(onVoltar: () -> Unit) {
    val c = LocalLkTokens.current
    val context = LocalContext.current

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Privacidade e dados",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = c.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(c.bgPrimary),
        ) {
            // ─── O que coletamos ───────────────────────────────────────────
            item {
                SectionHeaderPriv(titulo = "O que coletamos", c = c)
            }
            item {
                PrivItemRow(
                    c = c,
                    icon = Icons.Outlined.Speed,
                    label = "Velocidade de download e upload",
                    desc = "Medida durante os testes que você inicia",
                )
            }
            item {
                PrivItemRow(
                    c = c,
                    icon = Icons.Outlined.Timeline,
                    label = "Latência e jitter",
                    desc = "Tempo de resposta da sua conexão",
                )
            }
            item {
                PrivItemRow(
                    c = c,
                    icon = Icons.Outlined.Wifi,
                    label = "Tipo de rede",
                    desc = "Wi-Fi ou dados móveis (sem SSID)",
                )
            }
            item {
                PrivItemRow(
                    c = c,
                    icon = Icons.Outlined.Language,
                    label = "Qualidade de DNS",
                    desc = "Tempo de resposta do servidor de nomes",
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            // ─── O que NÃO coletamos ───────────────────────────────────────
            item {
                SectionHeaderPriv(titulo = "O que NÃO coletamos", c = c)
            }
            item {
                PrivItemRow(
                    c = c,
                    icon = Icons.Outlined.Block,
                    label = "Conteúdo de navegação",
                    desc = "Não vemos sites visitados, mensagens ou arquivos",
                )
            }
            item {
                PrivItemRow(
                    c = c,
                    icon = Icons.Outlined.LocationOff,
                    label = "Localização precisa",
                    desc = "Não usamos GPS nem endereço físico",
                )
            }
            item {
                PrivItemRow(
                    c = c,
                    icon = Icons.Outlined.PersonOff,
                    label = "Identidade do usuário",
                    desc = "Nenhum cadastro, e-mail ou CPF necessário",
                )
            }
            item {
                PrivItemRow(
                    c = c,
                    icon = Icons.Outlined.ShareLocation,
                    label = "Dados de terceiros",
                    desc = "Não compartilhamos dados com nenhuma empresa",
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            // ─── Retenção e controle ───────────────────────────────────────
            item {
                SectionHeaderPriv(titulo = "Retenção e controle", c = c)
            }
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LkSpacing.lg)
                            .background(color = c.bgSecondary, shape = RoundedCornerShape(12.dp))
                            .border(width = 1.dp, color = c.border, shape = RoundedCornerShape(12.dp))
                            .padding(LkSpacing.lg),
                ) {
                    Text(
                        text = "Todos os dados ficam armazenados localmente no seu dispositivo.\n\nVocê pode apagar tudo a qualquer momento em Configurações → Gerenciar dados locais.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

            // ─── Solicitação de exclusão ───────────────────────────────────
            item {
                SectionHeaderPriv(titulo = "Solicitação de exclusão", c = c)
            }
            item {
                OutlinedButton(
                    onClick = {
                        val intent =
                            Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:privacidade@linka.app".toUri()
                                putExtra(Intent.EXTRA_SUBJECT, "Solicitação de exclusão de dados - Linka")
                            }
                        context.startActivity(intent)
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LkSpacing.lg),
                    border = BorderStroke(width = 1.dp, color = LkColors.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LkColors.error),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = LkColors.error,
                    )
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(
                        text = "Solicitar exclusão de dados",
                        color = LkColors.error,
                    )
                }
            }
            item {
                Text(
                    text = "Enviamos confirmação de exclusão por e-mail em até 5 dias úteis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textTertiary,
                    modifier = Modifier.padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm),
                )
            }

            // ─── Footer ───────────────────────────────────────────────────
            item {
                Spacer(
                    Modifier
                        .navigationBarsPadding()
                        .height(24.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeaderPriv(
    titulo: String,
    c: LkTokens,
) {
    Text(
        text = titulo.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.W600,
        color = c.textTertiary,
        letterSpacing = 0.8.sp,
        modifier =
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(top = 8.dp),
    )
}

@Composable
private fun PrivItemRow(
    c: LkTokens,
    icon: ImageVector,
    label: String,
    desc: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LkColors.accent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(LkSpacing.md))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
                color = c.textPrimary,
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
    }
}
