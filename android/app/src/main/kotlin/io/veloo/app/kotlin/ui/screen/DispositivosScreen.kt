package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.feature.devices.DispositivoRede
import io.signallq.app.feature.devices.EstadoScanDispositivos
import io.signallq.app.feature.devices.NamingPrioridade
import io.signallq.app.feature.devices.SnapshotScanDispositivos
import io.signallq.app.feature.devices.TipoDispositivo
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.OfflineBanner
import io.signallq.app.ui.component.SheetDragHandle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispositivosScreen(
    snapshotDevices: SnapshotScanDispositivos,
    snapshotRede: SnapshotRede,
    onRefresh: () -> Unit,
    apelidos: Map<String, String>,
    onSalvarApelido: (mac: String, apelido: String) -> Unit,
    onVoltar: (() -> Unit)? = null,
) {
    val c = LocalLkTokens.current

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Devices,
                            contentDescription = null,
                            tint = c.textPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text("Dispositivos na rede", style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                    }
                },
                navigationIcon = {
                    if (onVoltar != null) {
                        IconButton(onClick = onVoltar) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar", tint = c.textPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            if (!snapshotRede.conectado) {
                OfflineBanner()
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f),
            ) {
                if (snapshotRede.estadoConexao != EstadoConexao.wifi) {
                    SemWifiFallback(c = c, hasDadosMoveis = snapshotRede.estadoConexao == EstadoConexao.movel)
                    return@Box
                }

                val dispositivos = snapshotDevices.dispositivos
                val isLoading = snapshotDevices.estado == EstadoScanDispositivos.varrendo
                val erro = snapshotDevices.erroMensagem

                if (dispositivos.isEmpty()) {
                    EmptyStateDispositivos(
                        c = c,
                        isLoading = isLoading,
                        progresso = snapshotDevices.progressoPercentual,
                        erro = erro,
                        onRefresh = onRefresh,
                    )
                } else {
                    DispositivosLista(
                        c = c,
                        dispositivos = dispositivos,
                        isLoading = isLoading,
                        erro = erro,
                        onRefresh = onRefresh,
                        apelidos = apelidos,
                        onSalvarApelido = onSalvarApelido,
                    )
                }
            } // Box
        } // Column
    }
}

// ---------------------------------------------------------------------------
// Tab content: usada em SinalScreen para aba Dispositivos
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispositivosTabContent(
    snapshotDevices: SnapshotScanDispositivos,
    onRefresh: () -> Unit,
    apelidos: Map<String, String>,
    onSalvarApelido: (mac: String, apelido: String) -> Unit,
    c: LkTokens,
) {
    val dispositivos = snapshotDevices.dispositivos
    val isLoading = snapshotDevices.estado == EstadoScanDispositivos.varrendo
    val erro = snapshotDevices.erroMensagem

    if (dispositivos.isEmpty()) {
        EmptyStateDispositivos(
            c = c,
            isLoading = isLoading,
            progresso = snapshotDevices.progressoPercentual,
            erro = erro,
            onRefresh = onRefresh,
        )
    } else {
        DispositivosLista(
            c = c,
            dispositivos = dispositivos,
            isLoading = isLoading,
            erro = erro,
            onRefresh = onRefresh,
            apelidos = apelidos,
            onSalvarApelido = onSalvarApelido,
        )
    }
}

// ---------------------------------------------------------------------------
// Lista principal com pull-to-refresh
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DispositivosLista(
    c: LkTokens,
    dispositivos: List<DispositivoRede>,
    isLoading: Boolean,
    erro: String?,
    onRefresh: () -> Unit,
    apelidos: Map<String, String>,
    onSalvarApelido: (mac: String, apelido: String) -> Unit,
) {
    val gateways = remember(dispositivos) { dispositivos.filter { it.fonteNome == "gateway" } }
    val aps =
        remember(dispositivos) { dispositivos.filter { it.fonteNome != "gateway" && it.tipoDispositivo == TipoDispositivo.pontoAcesso } }
    val clientes =
        remember(dispositivos) {
            dispositivos
                .filter { it.fonteNome != "gateway" && it.tipoDispositivo != TipoDispositivo.pontoAcesso }
                .sortedByDescending { it.esteDispositivo }
        }

    var deviceEmSheet by remember { mutableStateOf<DispositivoRede?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = LkSpacing.xl),
        ) {
            // Barra de progresso fina
            if (isLoading) {
                item {
                    LinearProgressIndicator(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                        color = LkColors.accent,
                        trackColor = c.bgSecondary,
                    )
                }
            }

            // ── Infraestrutura ─────────────────────────────────────────────
            if (gateways.isNotEmpty()) {
                item {
                    SectionHeaderRow(
                        title = "INFRAESTRUTURA (${gateways.size})",
                        c = c,
                    )
                }
                items(gateways) { gw ->
                    GatewayItem(
                        dispositivo = gw,
                        c = c,
                        apelido = gw.mac?.let { apelidos[it] },
                        onTap = { deviceEmSheet = gw },
                    )
                }
            }

            // ── Pontos de acesso / nós mesh ───────────────────────────────
            if (aps.isNotEmpty()) {
                item {
                    SectionHeaderRow(title = "PONTOS DE ACESSO (${aps.size})", c = c)
                }
                items(aps) { ap ->
                    ApMeshItem(
                        dispositivo = ap,
                        c = c,
                        apelido = ap.mac?.let { apelidos[it] },
                        onTap = { deviceEmSheet = ap },
                    )
                }
            }

            // ── Todos os dispositivos ──────────────────────────────────────
            val topPadding = if (gateways.isNotEmpty() || aps.isNotEmpty()) LkSpacing.sm else LkSpacing.md
            item { Spacer(Modifier.height(topPadding)) }

            if (clientes.isNotEmpty()) {
                item {
                    SectionHeaderRow(title = "DISPOSITIVOS (${clientes.size})", c = c)
                }
                items(clientes) { dev ->
                    DispositivoItem(
                        dispositivo = dev,
                        c = c,
                        apelido = dev.mac?.let { apelidos[it] },
                        onTap = { deviceEmSheet = dev },
                    )
                }
            }

            if (clientes.isEmpty()) {
                item {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Apenas o gateway foi encontrado",
                            color = c.textSecondary,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }

    // ── Modal de detalhe ──────────────────────────────────────────────────
    deviceEmSheet?.let { dev ->
        ModalBottomSheet(
            onDismissRequest = { deviceEmSheet = null },
            sheetState = sheetState,
            containerColor = c.bgPrimary,
        ) {
            if (dev.tipoDispositivo == TipoDispositivo.pontoAcesso) {
                MeshApSheet(
                    dispositivo = dev,
                    c = c,
                    apelidoAtual = dev.mac?.let { apelidos[it] } ?: "",
                    onSalvarApelido = { apelido ->
                        dev.mac?.let { mac -> onSalvarApelido(mac, apelido) }
                    },
                )
            } else {
                DeviceDetailSheet(
                    dispositivo = dev,
                    c = c,
                    apelidoAtual = dev.mac?.let { apelidos[it] } ?: "",
                    onSalvarApelido = { apelido ->
                        dev.mac?.let { mac -> onSalvarApelido(mac, apelido) }
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Linha de gateway (Roteador / Extensor)
// ---------------------------------------------------------------------------

@Composable
private fun GatewayItem(
    dispositivo: DispositivoRede,
    c: LkTokens,
    apelido: String?,
    onTap: () -> Unit,
) {
    val iconColor = LkColors.accent
    val bgColor = LkColors.accent.copy(alpha = 0.12f)

    LkListRow(
        c = c,
        leading = {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(LkRadius.button))
                        .background(bgColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Router,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        title = apelido?.takeIf { it.isNotBlank() } ?: dispositivo.nomeExibicao,
        subtitle = dispositivo.ip ?: "",
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BadgePill(label = "Roteador", bg = LkColors.accent.copy(alpha = 0.10f), fg = LkColors.accent)
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.Router,
                    contentDescription = null,
                    tint = c.textTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
        onTap = onTap,
    )
}

// ---------------------------------------------------------------------------
// Linha de ponto de acesso / nó mesh
// ---------------------------------------------------------------------------

@Composable
private fun ApMeshItem(
    dispositivo: DispositivoRede,
    c: LkTokens,
    apelido: String?,
    onTap: () -> Unit,
) {
    LkListRow(
        c = c,
        leading = {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(LkRadius.button))
                        .background(LkColors.success.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CellTower,
                    contentDescription = null,
                    tint = LkColors.success,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        title = apelido?.takeIf { it.isNotBlank() } ?: dispositivo.nomeExibicao,
        subtitle = dispositivo.ip ?: "",
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BadgePill(label = "AP Mesh", bg = LkColors.success.copy(alpha = 0.10f), fg = LkColors.success)
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.CellTower,
                    contentDescription = null,
                    tint = c.textTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
        onTap = onTap,
    )
}

// ---------------------------------------------------------------------------
// Linha de dispositivo cliente
// ---------------------------------------------------------------------------

@Composable
private fun DispositivoItem(
    dispositivo: DispositivoRede,
    c: LkTokens,
    apelido: String?,
    onTap: () -> Unit,
) {
    val iconBg = iconBgColor(dispositivo.tipoDispositivo, c)
    val iconFg = iconFgColor(dispositivo.tipoDispositivo, c)
    val icon = iconForTipo(dispositivo.tipoDispositivo)

    val nomeExibicao = apelido?.takeIf { it.isNotBlank() } ?: dispositivo.nomeExibicao
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .semantics {
                    role = Role.Button
                    contentDescription = nomeExibicao
                }.clickable(onClick = onTap)
                .border(
                    width = 0.5.dp,
                    color = c.border.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(0.dp),
                ).padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Ícone
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(LkRadius.button))
                        .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconFg,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            // Nome (apelido tem prioridade se definido) + fabricante
            Column(modifier = Modifier.weight(1f)) {
                val fabricante = dispositivo.fabricante?.takeIf { it.isNotBlank() }
                // Quando nomeExibicao é um IP puro (fallback do scanner), preferir fabricante como título
                val ehIpPuro = dispositivo.nomeExibicao.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""))
                val nomeDisplay =
                    apelido?.takeIf { it.isNotBlank() }
                        ?: if (ehIpPuro) {
                            NamingPrioridade.rotuloFallbackGenerico(fabricante)
                        } else {
                            dispositivo.nomeExibicao
                        }
                Text(
                    text = nomeDisplay,
                    color = c.textPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Exibe fabricante somente se disponível e não for o próprio título
                if (fabricante != null && nomeDisplay != fabricante) {
                    Text(
                        text = fabricante,
                        color = c.textTertiary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (dispositivo.esteDispositivo) {
                    Spacer(Modifier.height(4.dp))
                    BadgePill(
                        label = "Este aparelho",
                        bg = LkColors.accent.copy(alpha = 0.10f),
                        fg = LkColors.accent,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // IP + chevron
            Text(
                text = dispositivo.ip ?: "",
                color = c.textTertiary,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = c.textTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// DeviceDetailSheet — somente leitura (sem personalização por ora)
// ---------------------------------------------------------------------------

@Composable
private fun DeviceDetailSheet(
    dispositivo: DispositivoRede,
    c: LkTokens,
    apelidoAtual: String,
    onSalvarApelido: (String) -> Unit,
) {
    val iconBg = iconBgColor(dispositivo.tipoDispositivo, c)
    val iconFg = iconFgColor(dispositivo.tipoDispositivo, c)
    val icon = iconForTipo(dispositivo.tipoDispositivo)
    val isGateway = dispositivo.fonteNome == "gateway"
    val mac = dispositivo.mac
    val fabricante = dispositivo.fabricante
    var apelidoInput by remember { mutableStateOf(apelidoAtual) }

    val fieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LkColors.accent,
            unfocusedBorderColor = c.border,
            focusedLabelColor = LkColors.accent,
            unfocusedLabelColor = c.textSecondary,
            cursorColor = LkColors.accent,
            focusedTextColor = c.textPrimary,
            unfocusedTextColor = c.textPrimary,
        )

    LazyColumn(
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            SheetDragHandle()
        }

        // Cabeçalho
        item {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconFg,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = dispositivo.nomeExibicao,
                        style = MaterialTheme.typography.headlineSmall,
                        color = c.textPrimary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier =
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(LkColors.success),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Online",
                            style = MaterialTheme.typography.labelSmall,
                            color = LkColors.success,
                        )
                        if (isGateway) {
                            Spacer(Modifier.width(8.dp))
                            BadgePill(label = "Gateway", bg = LkColors.accent.copy(0.12f), fg = LkColors.accent)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = c.border)
        }

        // Seção APELIDO (só se tiver MAC)
        if (mac != null) {
            item {
                SheetSectionHeader(title = "APELIDO", c = c)
            }
            item {
                Column(
                    modifier = Modifier.padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm),
                ) {
                    OutlinedTextField(
                        value = apelidoInput,
                        onValueChange = { apelidoInput = it },
                        label = { Text("Apelido (opcional)") },
                        placeholder = { Text(dispositivo.nomeExibicao, color = c.textTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(8.dp),
                    )
                    Spacer(Modifier.height(LkSpacing.sm))
                    Button(
                        onClick = { onSalvarApelido(apelidoInput.trim()) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                    ) {
                        Text("Salvar apelido")
                    }
                }
            }
            item { HorizontalDivider(color = c.border) }
        }

        // Seção REDE
        item {
            SheetSectionHeader(title = "REDE", c = c)
        }
        item {
            LkListRow(c = c, title = "Endereço IP", trailing = {
                Text(dispositivo.ip ?: "—", style = MaterialTheme.typography.titleSmall, color = c.textSecondary)
            })
        }
        if (mac != null) {
            item {
                LkListRow(c = c, title = "MAC", trailing = {
                    Text(mascaraMac(mac), style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                })
            }
        }
        if (fabricante != null) {
            item {
                LkListRow(c = c, title = "Fabricante", trailing = {
                    Text(fabricante, style = MaterialTheme.typography.titleSmall, color = c.textSecondary)
                })
            }
        }
        item {
            LkListRow(c = c, title = "Tipo", trailing = {
                Text(tipoLabel(dispositivo.tipoDispositivo), fontSize = 13.sp, color = c.textSecondary)
            })
        }
        item {
            LkListRow(c = c, title = "Descoberto via", showDivider = false, trailing = {
                Text(fonteNomeLabel(dispositivo.fonteNome), fontSize = 13.sp, color = c.textSecondary)
            })
        }
    }
}

// ---------------------------------------------------------------------------
// MeshApSheet — ponto de acesso / nó mesh
// ---------------------------------------------------------------------------

@Composable
private fun MeshApSheet(
    dispositivo: DispositivoRede,
    c: LkTokens,
    apelidoAtual: String,
    onSalvarApelido: (String) -> Unit,
) {
    val mac = dispositivo.mac
    val fabricante = dispositivo.fabricante
    var apelidoInput by remember { mutableStateOf(apelidoAtual) }

    val fieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LkColors.accent,
            unfocusedBorderColor = c.border,
            focusedLabelColor = LkColors.accent,
            unfocusedLabelColor = c.textSecondary,
            cursorColor = LkColors.accent,
            focusedTextColor = c.textPrimary,
            unfocusedTextColor = c.textPrimary,
        )

    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            SheetDragHandle()
        }

        // Cabeçalho
        item {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LkColors.success.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CellTower,
                        contentDescription = null,
                        tint = LkColors.success,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = dispositivo.nomeExibicao,
                        style = MaterialTheme.typography.headlineSmall,
                        color = c.textPrimary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier =
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(LkColors.success),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(text = "Online", style = MaterialTheme.typography.labelSmall, color = LkColors.success)
                        Spacer(Modifier.width(8.dp))
                        BadgePill(label = "AP Mesh", bg = LkColors.success.copy(0.12f), fg = LkColors.success)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = c.border)
        }

        // Aviso sobre dados disponíveis
        item {
            Spacer(Modifier.height(LkSpacing.md))
            Box(modifier = Modifier.padding(horizontal = LkSpacing.lg)) {
                AlertBanner(
                    text =
                        "Sinal, banda e clientes conectados não estão disponíveis via varredura passiva. " +
                            "Para métricas detalhadas, acesse o painel do seu roteador mesh.",
                    c = c,
                )
            }
            Spacer(Modifier.height(LkSpacing.sm))
        }

        // Seção APELIDO
        if (mac != null) {
            item { SheetSectionHeader(title = "APELIDO", c = c) }
            item {
                Column(modifier = Modifier.padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm)) {
                    OutlinedTextField(
                        value = apelidoInput,
                        onValueChange = { apelidoInput = it },
                        label = { Text("Apelido (opcional)") },
                        placeholder = { Text(dispositivo.nomeExibicao, color = c.textTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(8.dp),
                    )
                    Spacer(Modifier.height(LkSpacing.sm))
                    Button(
                        onClick = { onSalvarApelido(apelidoInput.trim()) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                    ) { Text("Salvar apelido") }
                }
            }
            item { HorizontalDivider(color = c.border) }
        }

        // Seção REDE
        item { SheetSectionHeader(title = "REDE", c = c) }
        item {
            LkListRow(c = c, title = "Endereço IP", trailing = {
                Text(dispositivo.ip ?: "—", style = MaterialTheme.typography.titleSmall, color = c.textSecondary)
            })
        }
        if (mac != null) {
            item {
                LkListRow(c = c, title = "MAC", trailing = {
                    Text(mascaraMac(mac), style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                })
            }
        }
        if (fabricante != null) {
            item {
                LkListRow(c = c, title = "Fabricante", trailing = {
                    Text(fabricante, style = MaterialTheme.typography.titleSmall, color = c.textSecondary)
                })
            }
        }
        item {
            LkListRow(c = c, title = "Tipo", showDivider = false, trailing = {
                Text("Ponto de Acesso / Mesh", style = MaterialTheme.typography.titleSmall, color = c.textSecondary)
            })
        }
    }
}

// ---------------------------------------------------------------------------
// Sem Wi-Fi fallback
// ---------------------------------------------------------------------------

@Composable
private fun SemWifiFallback(
    c: LkTokens,
    hasDadosMoveis: Boolean,
) {
    // #144: mensagem spec-compliant por estado de conexão
    val titulo = if (hasDadosMoveis) "Dispositivos da rede" else "Sem Wi-Fi"
    val subtitle =
        if (hasDadosMoveis) {
            "Dispositivos da rede só aparecem quando você está conectado a um Wi-Fi."
        } else {
            "Sem conexão de rede. Conecte-se a uma rede Wi-Fi para escanear."
        }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.WifiOff,
                contentDescription = null,
                tint = c.textTertiary,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.headlineSmall,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                lineHeight = 20.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Estado vazio / scanning
// ---------------------------------------------------------------------------

@Composable
private fun EmptyStateDispositivos(
    c: LkTokens,
    isLoading: Boolean,
    progresso: Int,
    erro: String?,
    onRefresh: () -> Unit,
) {
    val temErro = !erro.isNullOrBlank()
    val titulo: String
    val subtitulo: String
    val icone: androidx.compose.ui.graphics.vector.ImageVector
    val iconColor: androidx.compose.ui.graphics.Color

    if (temErro) {
        val (ttl, sbt) = traduzirErroParaPortugues(checkNotNull(erro) { "invariante: temErro=true implica erro não-nulo" })
        titulo = ttl
        subtitulo = sbt
        icone = Icons.Outlined.WarningAmber
        iconColor = LkColors.warning
    } else if (isLoading) {
        titulo = "Procurando dispositivos..."
        subtitulo = "Aguarde alguns instantes."
        icone = Icons.Outlined.DevicesOther
        iconColor = c.textTertiary
    } else {
        titulo = "Nenhum dispositivo encontrado"
        subtitulo = "Aguarde alguns segundos e tente novamente."
        icone = Icons.Outlined.DevicesOther
        iconColor = c.textTertiary
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.titleSmall,
                color = c.textSecondary,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(24.dp))
            if (isLoading) {
                CircularProgressIndicator(color = LkColors.accent)
            } else {
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                ) {
                    Text("Escanear Rede")
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section header
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeaderRow(
    title: String,
    c: LkTokens,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = LkSpacing.lg, top = 20.dp, end = LkSpacing.lg, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = c.textTertiary,
            letterSpacing = 0.8.sp,
            fontWeight = FontWeight.W700,
        )
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            trailing()
        }
    }
}

// ---------------------------------------------------------------------------
// Sheet section header
// ---------------------------------------------------------------------------

@Composable
private fun SheetSectionHeader(
    title: String,
    c: LkTokens,
) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.W700,
        color = c.textTertiary,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = LkSpacing.lg, top = LkSpacing.lg, bottom = 4.dp),
    )
}

// ---------------------------------------------------------------------------
// Alert banner
// ---------------------------------------------------------------------------

@Composable
private fun AlertBanner(
    text: String,
    c: LkTokens,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.button))
                .background(LkColors.warning.copy(alpha = 0.10f))
                .border(1.dp, LkColors.warning.copy(alpha = 0.35f), RoundedCornerShape(LkRadius.button))
                .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = LkColors.warning,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = c.textPrimary, lineHeight = 16.sp)
    }
}

// ---------------------------------------------------------------------------
// LkListRow — linha reutilizável com divisor
// ---------------------------------------------------------------------------

@Composable
private fun LkListRow(
    c: LkTokens,
    title: String = "",
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
    onTap: (() -> Unit)? = null,
) {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (onTap != null) {
                            Modifier
                                .minimumInteractiveComponentSize()
                                .semantics { role = Role.Button }
                                .clickable(onClick = onTap)
                        } else {
                            Modifier
                        },
                    ).padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(LkSpacing.md))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = c.textPrimary, style = MaterialTheme.typography.bodyMedium)
                if (subtitle != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(text = subtitle, color = c.textSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (trailing != null) trailing()
        }
        if (showDivider) {
            HorizontalDivider(color = c.border.copy(alpha = 0.5f), thickness = 0.5.dp)
        }
    }
}

// ---------------------------------------------------------------------------
// Badge pill
// ---------------------------------------------------------------------------

@Composable
private fun BadgePill(
    label: String,
    bg: Color,
    fg: Color,
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(bg)
                .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = fg, fontWeight = FontWeight.W700)
    }
}

// Padding16 helper
@Composable
private fun Padding16(content: @Composable () -> Unit) {
    Box(modifier = Modifier.padding(start = LkSpacing.lg, top = 12.dp, end = LkSpacing.lg)) {
        content()
    }
}

// ---------------------------------------------------------------------------
// Helpers de mapeamento
// ---------------------------------------------------------------------------

private fun iconForTipo(tipo: TipoDispositivo): ImageVector =
    when (tipo) {
        TipoDispositivo.roteador -> Icons.Outlined.Router
        TipoDispositivo.pontoAcesso -> Icons.Outlined.CellTower
        TipoDispositivo.computador -> Icons.Outlined.Laptop
        TipoDispositivo.smartphone -> Icons.Outlined.Smartphone
        TipoDispositivo.smarthome -> Icons.Outlined.Lightbulb
        TipoDispositivo.impressora -> Icons.Outlined.Print
        TipoDispositivo.desconhecido -> Icons.Outlined.DevicesOther
    }

@Composable
private fun iconBgColor(
    tipo: TipoDispositivo,
    c: LkTokens,
): Color =
    when (tipo) {
        TipoDispositivo.smartphone -> LkColors.accent.copy(alpha = 0.12f)
        TipoDispositivo.computador -> LkColors.success.copy(alpha = 0.12f)
        TipoDispositivo.roteador -> LkColors.accent.copy(alpha = 0.12f)
        TipoDispositivo.pontoAcesso -> LkColors.success.copy(alpha = 0.12f)
        TipoDispositivo.smarthome -> LkColors.warning.copy(alpha = 0.12f)
        else -> c.bgSecondary
    }

@Composable
private fun iconFgColor(
    tipo: TipoDispositivo,
    c: LkTokens,
): Color =
    when (tipo) {
        TipoDispositivo.smartphone -> LkColors.accent
        TipoDispositivo.computador -> LkColors.success
        TipoDispositivo.roteador -> LkColors.accent
        TipoDispositivo.pontoAcesso -> LkColors.success
        TipoDispositivo.smarthome -> LkColors.warning
        else -> c.textSecondary
    }

private fun tipoLabel(tipo: TipoDispositivo): String =
    when (tipo) {
        TipoDispositivo.roteador -> "Roteador / Gateway"
        TipoDispositivo.pontoAcesso -> "Ponto de Acesso / Mesh"
        TipoDispositivo.computador -> "Computador"
        TipoDispositivo.smartphone -> "Celular / Tablet"
        TipoDispositivo.smarthome -> "Dispositivo inteligente"
        TipoDispositivo.impressora -> "Impressora"
        TipoDispositivo.desconhecido -> "Desconhecido"
    }

private fun fonteNomeLabel(fonte: String) =
    when (fonte) {
        "gateway" -> "Roteador (gateway)"
        "mdns" -> "mDNS · Bonjour"
        "ssdp" -> "UPnP · SSDP"
        "nbns" -> "NetBIOS"
        "arp" -> "ARP (varredura)"
        "tcpProbe" -> "TCP probe"
        else -> fonte
    }

private fun traduzirErroParaPortugues(erro: String): Pair<String, String> =
    when {
        erro.contains("semPermissaoLocalizacao", ignoreCase = true) -> {
            "Permissão de localização não concedida" to "Acesse as configurações do app para conceder acesso à localização."
        }
        erro.contains("erroRede", ignoreCase = true) -> {
            "Erro de conexão de rede" to "Verifique se sua conexão Wi-Fi está estável e tente novamente."
        }
        erro.contains("semWifi", ignoreCase = true) -> {
            "Sem conexão Wi-Fi" to "Conecte-se a uma rede Wi-Fi para escanear dispositivos."
        }
        erro.contains("timeout", ignoreCase = true) -> {
            "Tempo limite excedido" to "O escanear levou muito tempo. Tente novamente."
        }
        else -> {
            "Erro ao escanear" to "Não foi possível escanear a rede. Tente novamente."
        }
    }

/** Mascara os octetos 3-4 do MAC: ex. "c4:8e:de:ad:1a:2b" → "c4:8e:••:••:1a:2b" */
private fun mascaraMac(mac: String): String {
    val partes = mac.trim().split(":")
    return if (partes.size == 6) {
        "${partes[0]}:${partes[1]}:••:••:${partes[4]}:${partes[5]}"
    } else {
        mac
    }
}
