package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.ProfileAvatarButton

// GH#933 — Fase 4 MD3: hub real de atalhos, substitui o placeholder criado na Fase 1
// (#930). Grade estática, sem chamada de rede própria — cada card só navega para a
// tela/overlay correspondente, que já existe (restyle) ou ainda é stub de fase futura
// (Equipamento de internet → Fase 5/#934, Jogos → Fase 6/#935; Monitoramento → MonitoramentoSheet.kt, Fase 7/#936).
private data class FerramentaItem(
    val icon: ImageVector,
    val titulo: String,
    val descricao: String,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FerramentasScreen(
    nomeUsuario: String,
    fotoUri: String?,
    onAbrirPerfil: () -> Unit,
    onAbrirDispositivos: () -> Unit = {},
    onAbrirEquipamentoInternet: () -> Unit = {},
    onAbrirPing: () -> Unit = {},
    // Null esconde o card — controlado por FeatureFlags.DNS_SCREEN no AppShell.
    onAbrirDns: (() -> Unit)? = null,
    onAbrirLaudo: () -> Unit = {},
    onAbrirMonitoramento: () -> Unit = {},
    onAbrirJogos: () -> Unit = {},
) {
    val c = LocalLkTokens.current

    val itens =
        remember(
            onAbrirDispositivos,
            onAbrirEquipamentoInternet,
            onAbrirPing,
            onAbrirDns,
            onAbrirLaudo,
            onAbrirMonitoramento,
            onAbrirJogos,
        ) {
            buildList {
                add(
                    FerramentaItem(
                        icon = Icons.Outlined.Devices,
                        titulo = "Dispositivos",
                        descricao = "Quem está na sua rede",
                        onClick = onAbrirDispositivos,
                    ),
                )
                add(
                    FerramentaItem(
                        icon = Icons.Outlined.Router,
                        titulo = "Equipamento de internet",
                        descricao = "ONT, roteador e sinal óptico",
                        onClick = onAbrirEquipamentoInternet,
                    ),
                )
                add(
                    FerramentaItem(
                        icon = Icons.Outlined.NetworkCheck,
                        titulo = "Ping",
                        descricao = "Latência e estabilidade",
                        onClick = onAbrirPing,
                    ),
                )
                if (onAbrirDns != null) {
                    add(
                        FerramentaItem(
                            icon = Icons.Outlined.Dns,
                            titulo = "DNS",
                            descricao = "Comparar servidores DNS",
                            onClick = onAbrirDns,
                        ),
                    )
                }
                add(
                    FerramentaItem(
                        icon = Icons.Outlined.Description,
                        titulo = "Laudo",
                        descricao = "Relatório do último diagnóstico",
                        onClick = onAbrirLaudo,
                    ),
                )
                add(
                    FerramentaItem(
                        icon = Icons.Outlined.MonitorHeart,
                        titulo = "Monitoramento",
                        descricao = "Alertas em segundo plano",
                        onClick = onAbrirMonitoramento,
                    ),
                )
                add(
                    FerramentaItem(
                        icon = Icons.Outlined.SportsEsports,
                        titulo = "Jogos",
                        descricao = "Prontidão da rede para jogar",
                        onClick = onAbrirJogos,
                    ),
                )
            }
        }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Ferramentas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                    )
                },
                navigationIcon = {
                    ProfileAvatarButton(
                        nomeUsuario = nomeUsuario,
                        fotoUri = fotoUri,
                        onClick = onAbrirPerfil,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            contentPadding = PaddingValues(LkSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            items(itens) { item -> FerramentaCard(item = item, c = c) }
        }
    }
}

@Composable
private fun FerramentaCard(
    item: FerramentaItem,
    c: LkTokens,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .clickable(onClick = item.onClick)
                .padding(LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(LkColors.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = LkColors.accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = item.titulo,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.W600,
            color = c.textPrimary,
        )
        Text(
            text = item.descricao,
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
        )
    }
}
