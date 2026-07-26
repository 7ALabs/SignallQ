package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.NetworkWifi
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.R
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkSurfaceCard

// GH#933 — Fase 4 MD3: hub real de atalhos, substitui o placeholder criado na Fase 1
// (#930). Grade estática, sem chamada de rede própria — cada card só navega para a
// tela/overlay correspondente, que já existe (restyle) ou ainda é stub de fase futura
// (Equipamento de internet → Fase 5/#934; Monitoramento → MonitoramentoSheet.kt, Fase 7/#936).
// "Jogos" (Fase 6/#935) abria o fluxo legado próprio até a issue #1487 fundir tudo no Modo
// gamer (Overlay.ModoGamer, Feature #550/#1476) — onAbrirJogos aqui continua abrindo o
// mesmo overlay fundido, só o destino mudou.
private data class FerramentaItem(
    val icon: ImageVector,
    val titulo: String,
    val descricao: String,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FerramentasScreen(
    onAbrirMenu: () -> Unit,
    onAbrirDispositivos: () -> Unit = {},
    onAbrirEquipamentoInternet: () -> Unit = {},
    onAbrirPing: () -> Unit = {},
    onAbrirDns: () -> Unit = {},
    onAbrirLaudo: () -> Unit = {},
    onAbrirMonitoramento: () -> Unit = {},
    onAbrirJogos: () -> Unit = {},
    onAbrirSinalWifi: () -> Unit = {},
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
            onAbrirSinalWifi,
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
                        descricao = "Status do modem/ONT da operadora",
                        onClick = onAbrirEquipamentoInternet,
                    ),
                )
                add(
                    FerramentaItem(
                        icon = Icons.Outlined.NetworkCheck,
                        titulo = "Ping",
                        descricao = "Teste de latência para um endereço",
                        onClick = onAbrirPing,
                    ),
                )
                add(
                    FerramentaItem(
                        icon = Icons.Outlined.Dns,
                        titulo = "DNS",
                        descricao = "Compare servidores e troque o seu",
                        onClick = onAbrirDns,
                    ),
                )
                add(
                    FerramentaItem(
                        icon = Icons.Outlined.Description,
                        titulo = "Laudo",
                        descricao = "Laudo técnico completo da sua conexão",
                        onClick = onAbrirLaudo,
                    ),
                )
                add(
                    FerramentaItem(
                        icon = Icons.Outlined.MonitorHeart,
                        titulo = "Monitoramento",
                        descricao = "Análise avançada e alertas em segundo plano",
                        onClick = onAbrirMonitoramento,
                    ),
                )
                add(
                    FerramentaItem(
                        icon = Icons.Outlined.SportsEsports,
                        titulo = "Modo Jogos",
                        descricao = "Teste sua conexão para 21 jogos, em qualquer dispositivo",
                        onClick = onAbrirJogos,
                    ),
                )
                // GH#1201 — nova ferramenta "Sinal WiFi": indicador dinâmico de RSSI/PHY/padrão
                // via polling manual (versão contida do Walk Test do SignallQ Pro, ver #1176).
                add(
                    FerramentaItem(
                        icon = Icons.Outlined.NetworkWifi,
                        titulo = "Sinal WiFi",
                        descricao = "Veja o sinal, a velocidade e o padrão Wi-Fi em tempo real",
                        onClick = onAbrirSinalWifi,
                    ),
                )
            }
        }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Build,
                            contentDescription = null,
                            tint = c.textPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text(
                            text = "Ferramentas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.W600,
                            color = c.textPrimary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onAbrirMenu) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.appshell_cd_abrir_menu),
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
            contentPadding = PaddingValues(LkSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            items(itens) { item -> FerramentaListItem(item = item, c = c) }
        }
    }
}

@Composable
private fun FerramentaListItem(
    item: FerramentaItem,
    c: LkTokens,
) {
    LkSurfaceCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = item.onClick)
                .padding(0.dp),
        outlined = false,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(LkRadius.input))
                        .background(c.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = c.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f).padding(start = LkSpacing.md),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                )
                Text(
                    text = item.descricao,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = c.textTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
