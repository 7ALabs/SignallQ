package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.signallq.app.R
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkPillBadge
import io.signallq.app.ui.component.LkSectionOverline
import io.signallq.app.ui.component.LkSurfaceCard

// GH#933 — Fase 4 MD3: hub real de atalhos, substitui o placeholder criado na Fase 1
// (#930). Grade estática, sem chamada de rede própria — cada card só navega para a
// tela/overlay correspondente, que já existe (restyle) ou ainda é stub de fase futura
// (Equipamento de internet → Fase 5/#934; Monitoramento → MonitoramentoSheet.kt, Fase 7/#936).
// "Jogos" (Fase 6/#935) abria o fluxo legado próprio até a issue #1487 fundir tudo no Modo
// gamer (Overlay.ModoGamer, Feature #550/#1476) — onAbrirJogos aqui continua abrindo o
// mesmo overlay fundido, só o destino mudou.
private data class FerramentaItem(
    val tipo: TipoFerramenta,
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
    /** Issue #1503 (Camada B) — só não-nulo quando a navegação até aqui veio do card
     *  contextual do diagnóstico guiado ([DiagnosticoGuiadoScreen]); nunca estático. */
    ferramentaRecomendada: TipoFerramenta? = null,
    /** Issue #1503 — quando não-nulo, a tela é empilhada como overlay (via
     *  `Overlay.Ferramentas`, entrada vinda do card contextual) e mostra seta de voltar
     *  em vez do hambúrguer da tab fixa. `null` preserva o comportamento original de tab. */
    onVoltar: (() -> Unit)? = null,
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
                        tipo = TipoFerramenta.DISPOSITIVOS,
                        icon = Icons.Outlined.Devices,
                        titulo = "Dispositivos",
                        descricao = "Quem está na sua rede",
                        onClick = onAbrirDispositivos,
                    ),
                )
                add(
                    FerramentaItem(
                        tipo = TipoFerramenta.EQUIPAMENTO_INTERNET,
                        icon = Icons.Outlined.Router,
                        titulo = "Equipamento de internet",
                        descricao = "Status do modem/ONT da operadora",
                        onClick = onAbrirEquipamentoInternet,
                    ),
                )
                add(
                    FerramentaItem(
                        tipo = TipoFerramenta.PING,
                        icon = Icons.Outlined.NetworkCheck,
                        titulo = "Ping",
                        descricao = "Teste de latência para um endereço",
                        onClick = onAbrirPing,
                    ),
                )
                add(
                    FerramentaItem(
                        tipo = TipoFerramenta.DNS,
                        icon = Icons.Outlined.Dns,
                        titulo = "DNS",
                        descricao = "Compare servidores e troque o seu",
                        onClick = onAbrirDns,
                    ),
                )
                add(
                    FerramentaItem(
                        tipo = TipoFerramenta.LAUDO,
                        icon = Icons.Outlined.Description,
                        titulo = "Laudo",
                        descricao = "Laudo técnico completo da sua conexão",
                        onClick = onAbrirLaudo,
                    ),
                )
                add(
                    FerramentaItem(
                        tipo = TipoFerramenta.MONITORAMENTO,
                        icon = Icons.Outlined.MonitorHeart,
                        titulo = "Monitoramento",
                        descricao = "Análise avançada e alertas em segundo plano",
                        onClick = onAbrirMonitoramento,
                    ),
                )
                add(
                    FerramentaItem(
                        tipo = TipoFerramenta.MODO_JOGOS,
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
                        tipo = TipoFerramenta.SINAL_WIFI,
                        icon = Icons.Outlined.NetworkWifi,
                        titulo = "Sinal WiFi",
                        descricao = "Veja o sinal, a velocidade e o padrão Wi-Fi em tempo real",
                        onClick = onAbrirSinalWifi,
                    ),
                )
            }
        }

    // Issue #1503 (Camada B) — hub sai de lista flat pra 2 seções, curadoria em
    // CatalogoFerramentas (puro, testado em TipoFerramentaTest) — aqui só junta com
    // ícone/texto/onClick de cada item.
    val porTipo = remember(itens) { itens.associateBy { it.tipo } }
    val maisUsadas = remember(porTipo) { CatalogoFerramentas.maisUsadas.mapNotNull { porTipo[it] } }
    val restante = remember(porTipo) { CatalogoFerramentas.restante.mapNotNull { porTipo[it] } }

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
                    IconButton(onClick = onVoltar ?: onAbrirMenu) {
                        if (onVoltar != null) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = c.textPrimary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = stringResource(R.string.appshell_cd_abrir_menu),
                                tint = c.textPrimary,
                            )
                        }
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
            item { LkSectionOverline(text = "Mais usadas") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                ) {
                    maisUsadas.forEach { item ->
                        FerramentaDestacadaCard(
                            item = item,
                            destacada = item.tipo == ferramentaRecomendada,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            c = c,
                        )
                    }
                }
            }
            item { LkSectionOverline(text = "Todas as ferramentas") }
            items(restante) { item ->
                FerramentaListItem(item = item, destacada = item.tipo == ferramentaRecomendada, c = c)
            }
        }
    }
}

@Composable
private fun FerramentaDestacadaCard(
    item: FerramentaItem,
    destacada: Boolean,
    c: LkTokens,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        LkSurfaceCard(
            modifier = Modifier.fillMaxWidth().fillMaxHeight().clickable(onClick = item.onClick),
            outlined = false,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(vertical = LkSpacing.md, horizontal = LkSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(LkRadius.input))
                            .background(c.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = c.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    text = item.titulo,
                    style = MaterialTheme.typography.labelMedium.copy(hyphens = Hyphens.Auto),
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (destacada) {
            // Card estreito (3 por linha) — versão curta do badge, texto completo fica
            // no item compacto de FerramentaListItem, que tem largura de sobra.
            LkPillBadge(
                text = "recomendado",
                containerColor = c.primary,
                contentColor = c.onPrimary,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            )
        }
    }
}

@Composable
private fun FerramentaListItem(
    item: FerramentaItem,
    destacada: Boolean,
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs)) {
                    Text(
                        text = item.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                    )
                    // Issue #1503 (Camada B) — só aparece quando a navegação até aqui veio
                    // do card contextual do diagnóstico guiado, nunca estático.
                    if (destacada) {
                        LkPillBadge(
                            text = "recomendado pra você",
                            containerColor = c.primary,
                            contentColor = c.onPrimary,
                        )
                    }
                }
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
