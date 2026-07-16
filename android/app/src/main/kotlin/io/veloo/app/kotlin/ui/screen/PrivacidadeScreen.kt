package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkSheetDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacidadeScreen(
    onVoltar: () -> Unit,
    onAbrirGerenciarDados: () -> Unit = {},
) {
    val c = LocalLkTokens.current

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Privacidade",
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
                    .padding(padding),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Hero: shield icon + title + description
            item {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LkSpacing.lg)
                            .padding(top = LkSpacing.md, bottom = LkSpacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(LkColors.success.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = LkColors.success,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.height(LkSpacing.md))
                    Text(
                        text = "Tudo é processado localmente",
                        style = MaterialTheme.typography.headlineSmall,
                        color = c.textPrimary,
                    )
                    Spacer(Modifier.height(LkSpacing.sm))
                    Text(
                        text =
                            "O SignallQ roda inteiramente no seu aparelho. Resultados são salvos localmente. " +
                                "Nada vai para servidores externos sem você acionar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                        modifier = Modifier.padding(horizontal = LkSpacing.md),
                    )
                }
            }

            // Section: Dados que coletamos
            item {
                PrivacidadeSection(
                    titulo = "Dados que coletamos",
                    descricao = "Speedtest, scans Wi-Fi e diagnósticos. Tudo fica no Room (SQLite local).",
                    c = c,
                )
            }

            // Section: Permissões usadas
            item {
                PrivacidadeSection(
                    titulo = "Permissões usadas",
                    descricao = "Localização (para listar redes Wi-Fi), Telefonia (4G/5G), notificações (alertas).",
                    c = c,
                )
            }

            // Section: Compartilhamento opcional
            item {
                PrivacidadeSection(
                    titulo = "Compartilhamento opcional",
                    descricao = "Apenas se você acionar \"Compartilhar resultado\" ou \"Diagnóstico IA\".",
                    c = c,
                )
            }

            item { Spacer(Modifier.height(LkSpacing.lg)) }

            // Destino único para limpar histórico, apagar dados ou resetar o app --
            // antes eram dois botões diretos aqui, sem confirmação (ver critique P0).
            item {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onAbrirGerenciarDados)
                            .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = LkColors.accent,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gerenciar dados e privacidade",
                            style = MaterialTheme.typography.titleSmall,
                            color = c.textPrimary,
                        )
                        Text(
                            text = "Limpar histórico, apagar dados locais ou resetar o app",
                            style = MaterialTheme.typography.bodySmall,
                            // GH#937: textTertiary sobre branco ~2.5:1 (fail AA). textSecondary ~4.8:1.
                            color = c.textSecondary,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = c.textTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            item {
                LkSheetDivider(modifier = Modifier.padding(horizontal = LkSpacing.lg))
            }

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
private fun PrivacidadeSection(
    titulo: String,
    descricao: String,
    c: LkTokens,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleSmall,
            color = c.textPrimary,
        )
        Spacer(Modifier.height(LkSpacing.xs))
        Text(
            text = descricao,
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
        )
    }
}
