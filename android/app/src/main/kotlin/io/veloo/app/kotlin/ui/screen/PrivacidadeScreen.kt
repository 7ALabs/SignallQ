package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacidadeScreen(
    onVoltar: () -> Unit,
    onApagarDadosLocais: () -> Unit = {},
    onResetarApp: () -> Unit = {},
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
                                .background(LkColors.success.copy(alpha = 0.10f)),
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
                        fontSize = 17.sp,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                    )
                    Spacer(Modifier.height(LkSpacing.sm))
                    Text(
                        text =
                            "O SignallQ roda inteiramente no seu aparelho. Resultados são salvos localmente. " +
                                "Nada vai para servidores externos sem você acionar.",
                        fontSize = 13.sp,
                        color = c.textSecondary,
                        lineHeight = 19.sp,
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

            // Destructive actions
            item {
                Column(modifier = Modifier.padding(horizontal = LkSpacing.lg)) {
                    TextButton(onClick = onApagarDadosLocais) {
                        Text(
                            text = "Apagar dados locais",
                            color = LkColors.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W600,
                        )
                    }
                    Text(
                        text = "Apaga histórico, perfil e preferências do app.",
                        fontSize = 12.sp,
                        color = c.textTertiary,
                        modifier = Modifier.padding(start = LkSpacing.md),
                    )
                }
            }

            item { Spacer(Modifier.height(LkSpacing.sm)) }

            item {
                Column(modifier = Modifier.padding(horizontal = LkSpacing.lg)) {
                    TextButton(onClick = onResetarApp) {
                        Text(
                            text = "Resetar app",
                            color = LkColors.error,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.W600,
                        )
                    }
                    Text(
                        text = "Volta ao estado inicial, incluindo onboarding.",
                        fontSize = 12.sp,
                        color = c.textTertiary,
                        modifier = Modifier.padding(start = LkSpacing.md),
                    )
                }
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
            fontSize = 14.sp,
            fontWeight = FontWeight.W600,
            color = c.textPrimary,
        )
        Spacer(Modifier.height(LkSpacing.xs))
        Text(
            text = descricao,
            fontSize = 13.sp,
            color = c.textSecondary,
            lineHeight = 19.sp,
        )
    }
}
