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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TopAppBar
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
import io.signallq.app.ads.AdSlot
import io.signallq.app.ads.AdUnitIds
import io.signallq.app.ads.NativeAdContentSignals
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.core.network.contracts.localdevice.TipoConexaoFisica
import io.signallq.app.core.network.contracts.topologia.PapelTopologia
import io.signallq.app.feature.devices.DispositivoRede
import io.signallq.app.feature.devices.EstadoScanDispositivos
import io.signallq.app.feature.devices.NamingPrioridade
import io.signallq.app.feature.devices.ResultadoCorrelacaoTopologia
import io.signallq.app.feature.devices.SnapshotScanDispositivos
import io.signallq.app.feature.devices.TipoDispositivo
import io.signallq.app.feature.devices.chaveApelido
import io.signallq.app.feature.devices.ehClienteFinal
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.ads.rememberNativeAd
import io.signallq.app.ui.component.LkInfoCallout
import io.signallq.app.ui.component.LkPillBadge
import io.signallq.app.ui.component.LkSectionOverline
import io.signallq.app.ui.component.LkStatusDot
import io.signallq.app.ui.component.OfflineBanner
import io.signallq.app.ui.component.SheetDragHandle
import io.signallq.app.ui.component.ads.NativeAdListRow
import io.signallq.app.ui.component.ads.NativeAdSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispositivosScreen(
    snapshotDevices: SnapshotScanDispositivos,
    snapshotRede: SnapshotRede,
    onRefresh: () -> Unit,
    apelidos: Map<String, String>,
    onSalvarApelido: (mac: String, apelido: String) -> Unit,
    onVoltar: (() -> Unit)? = null,
    // GH#531 — resumo "2,4G + 5G" das bandas Wi-Fi do gateway conectado, exibido
    // no subtítulo do GatewayItem na seção INFRAESTRUTURA. Null quando sem dado.
    bandasWifi: String? = null,
    /** Toggle remoto (Firebase Remote Config) + gate de consentimento UMP -- issue #555.
     *  Default `false`: nunca mostra anuncio sem sinal explicito de que pode. */
    adsEnabled: Boolean = false,
    /** #983 (Fase 4) — correlacao best-effort topologia/gateway, chaveada por id do dispositivo
     *  (ver MainViewModel.correlacoesTopologia). Mapa vazio (default) preserva o comportamento
     *  anterior a Fase 4 — nenhuma secao nova aparece no detalhe do dispositivo. */
    correlacoesTopologia: Map<String, ResultadoCorrelacaoTopologia> = emptyMap(),
) {
    val c = LocalLkTokens.current

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            // GH#1079: migrado de Column/Row cru para TopAppBar real do M3 -- o layout
            // manual nao aplicava inset de status bar/notch (`.statusBarsPadding()`),
            // diferente das outras 14 telas do app que ja usam TopAppBar/
            // CenterAlignedTopAppBar reais.
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(c.bgPrimary),
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "Dispositivos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.W600,
                            color = c.textPrimary,
                        )
                    },
                    navigationIcon = {
                        if (onVoltar != null) {
                            IconButton(onClick = onVoltar) {
                                Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar", tint = c.textPrimary)
                            }
                        }
                    },
                    actions = {
                        val escaneando = snapshotDevices.estado == EstadoScanDispositivos.varrendo
                        IconButton(onClick = onRefresh, enabled = !escaneando) {
                            if (escaneando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = c.primary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Escanear rede",
                                    tint = c.textPrimary,
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bgPrimary),
                )
                HorizontalDivider(color = c.outlineVariant, thickness = 1.dp)
            }
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
                        bandasWifi = bandasWifi,
                        adsEnabled = adsEnabled,
                        correlacoesTopologia = correlacoesTopologia,
                    )
                }
            } // Box
        } // Column
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
    bandasWifi: String? = null,
    adsEnabled: Boolean = false,
    correlacoesTopologia: Map<String, ResultadoCorrelacaoTopologia> = emptyMap(),
) {
    val gateways = remember(dispositivos) { dispositivos.filter { it.fonteNome == "gateway" } }
    val aps =
        remember(dispositivos) { dispositivos.filter { it.fonteNome != "gateway" && it.tipoDispositivo == TipoDispositivo.pontoAcesso } }
    val clientes =
        remember(dispositivos) {
            dispositivos
                .filter { it.ehClienteFinal() }
                .sortedByDescending { it.esteDispositivo }
        }

    var deviceEmSheet by remember { mutableStateOf<DispositivoRede?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val nativeAd by rememberNativeAd(
        adUnitId = AdUnitIds.para(AdSlot.DISPOSITIVOS),
        contentSignal = NativeAdContentSignals.forSlot(AdSlot.DISPOSITIVOS),
        eligible = adsEnabled,
    )

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
                        color = c.primary,
                        trackColor = c.bgSecondary,
                    )
                }
            }

            // ── Infraestrutura ─────────────────────────────────────────────
            if (gateways.isNotEmpty()) {
                item {
                    SectionHeaderRow(
                        title = "Infraestrutura (${gateways.size})",
                        c = c,
                    )
                }
                items(gateways) { gw ->
                    GatewayItem(
                        dispositivo = gw,
                        c = c,
                        apelido = gw.chaveApelido()?.let { apelidos[it] },
                        bandasWifi = bandasWifi,
                        clientesCount = clientes.size,
                        onTap = { deviceEmSheet = gw },
                    )
                }
            }

            // ── Pontos de acesso / nós mesh ───────────────────────────────
            if (aps.isNotEmpty()) {
                item {
                    SectionHeaderRow(title = "Pontos de acesso (${aps.size})", c = c)
                }
                items(aps) { ap ->
                    ApMeshItem(
                        dispositivo = ap,
                        c = c,
                        apelido = ap.chaveApelido()?.let { apelidos[it] },
                        onTap = { deviceEmSheet = ap },
                    )
                }
            }

            // ── Todos os dispositivos ──────────────────────────────────────
            val topPadding = if (gateways.isNotEmpty() || aps.isNotEmpty()) LkSpacing.sm else LkSpacing.md
            item { Spacer(Modifier.height(topPadding)) }

            if (clientes.isNotEmpty()) {
                val adIndex = clientes.size / 2
                item {
                    SectionHeaderRow(title = "Dispositivos (${clientes.size})", c = c)
                }
                itemsIndexed(clientes) { index, dev ->
                    if (index == adIndex) {
                        NativeAdListRow(
                            nativeAd = nativeAd,
                            source = NativeAdSource.ADMOB,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    DispositivoItem(
                        dispositivo = dev,
                        c = c,
                        apelido = dev.chaveApelido()?.let { apelidos[it] },
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
                    apelidoAtual = dev.chaveApelido()?.let { apelidos[it] } ?: "",
                    onSalvarApelido = { apelido ->
                        dev.chaveApelido()?.let { chave -> onSalvarApelido(chave, apelido) }
                    },
                )
            } else {
                DeviceDetailSheet(
                    dispositivo = dev,
                    c = c,
                    apelidoAtual = dev.chaveApelido()?.let { apelidos[it] } ?: "",
                    onSalvarApelido = { apelido ->
                        dev.chaveApelido()?.let { chave -> onSalvarApelido(chave, apelido) }
                    },
                    correlacao = correlacoesTopologia[dev.id],
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
    // GH#531 — bandas Wi-Fi ("2,4G + 5G") e contagem de clientes detectados;
    // null/0 mantém o subtítulo antigo (só IP) quando não há dado suficiente.
    bandasWifi: String? = null,
    clientesCount: Int = 0,
) {
    val iconColor = c.primary
    val bgColor = c.primary.copy(alpha = 0.12f)
    val ip = dispositivo.ip ?: ""
    val subtituloGateway =
        if (bandasWifi.isNullOrBlank()) {
            ip
        } else {
            listOf(ip, bandasWifi, "$clientesCount clientes").filter { it.isNotBlank() }.joinToString(" · ")
        }

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
        subtitle = subtituloGateway,
        trailing = {
            BadgePill(label = "Roteador", bg = c.primary.copy(alpha = 0.10f), fg = c.primary)
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
                        .background(c.success.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CellTower,
                    contentDescription = null,
                    tint = c.success,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        title = apelido?.takeIf { it.isNotBlank() } ?: dispositivo.nomeExibicao,
        subtitle = dispositivo.ip ?: "",
        trailing = {
            BadgePill(label = "AP Mesh", bg = c.success.copy(alpha = 0.10f), fg = c.success)
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
    val fabricante = dispositivo.fabricante?.takeIf { it.isNotBlank() }

    val nomeExibicao = apelido?.takeIf { it.isNotBlank() } ?: dispositivo.nomeExibicao
    val ehIpPuro = dispositivo.nomeExibicao.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""))
    val nomeDisplay =
        apelido?.takeIf { it.isNotBlank() }
            ?: if (ehIpPuro) {
                NamingPrioridade.rotuloFallbackGenerico(fabricante)
            } else {
                dispositivo.nomeExibicao
            }
    val subtitulo =
        when {
            dispositivo.esteDispositivo && !dispositivo.ip.isNullOrBlank() -> "${dispositivo.ip} · Este aparelho"
            fabricante != null && nomeDisplay != fabricante -> fabricante
            !dispositivo.ip.isNullOrBlank() -> dispositivo.ip
            else -> "Fabricante desconhecido"
        }

    LkListRow(
        c = c,
        title = nomeDisplay,
        subtitle = subtitulo,
        leading = {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(LkRadius.input))
                        .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconFg,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
        },
        onTap = onTap,
    )
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
    /** #983 (Fase 4) — correlacao best-effort com a topologia Wi-Fi/gateway pra este
     *  dispositivo especifico. Null quando nao ha correlacao (comportamento pre-Fase 4). */
    correlacao: ResultadoCorrelacaoTopologia? = null,
) {
    val iconBg = iconBgColor(dispositivo.tipoDispositivo, c)
    val iconFg = iconFgColor(dispositivo.tipoDispositivo, c)
    val icon = iconForTipo(dispositivo.tipoDispositivo)
    val mac = dispositivo.mac
    // #853 — a secao APELIDO usa a chave com fallback ip+nome (chaveApelido), nao so o MAC
    // cru, senao ela some sempre que o Android nao consegue resolver o MAC via ARP.
    val chaveApelido = dispositivo.chaveApelido()
    val fabricante = dispositivo.fabricante
    var apelidoInput by remember { mutableStateOf(apelidoAtual) }

    val fieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.primary,
            unfocusedBorderColor = c.border,
            focusedLabelColor = c.primary,
            unfocusedLabelColor = c.textSecondary,
            cursorColor = c.primary,
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
                modifier = Modifier.padding(horizontal = LkSpacing.lg),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(c.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = c.primary,
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
                        LkStatusDot(color = c.success)
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text(
                            text = "Online",
                            style = MaterialTheme.typography.labelMedium,
                            color = c.success,
                        )
                    }
                }
            }
            Spacer(Modifier.height(LkSpacing.lg))
            HorizontalDivider(color = c.border)
        }

        // Seção APELIDO (#853 — chave com fallback ip+nome quando não há MAC resolvível)
        if (chaveApelido != null) {
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
                        shape = RoundedCornerShape(LkRadius.input),
                    )
                    Spacer(Modifier.height(LkSpacing.sm))
                    Button(
                        onClick = { onSalvarApelido(apelidoInput.trim()) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = c.primary),
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
                Text(
                    tipoLabel(dispositivo.tipoDispositivo),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.textSecondary,
                )
            })
        }
        // #983 (Fase 4) — so aparece quando ha correlacao confirmada (ClientSnapshot exato ou
        // MAC==BSSID exato); correlacao fraca (so OUI) nunca chega aqui como papel/conexao,
        // so como evidencia auxiliar (nao exibida, ver correlacionarDispositivoComTopologia).
        correlacao?.tipoConexaoFisicaConfirmada?.let { tipoConexao ->
            item {
                LkListRow(c = c, title = "Conexão física", trailing = {
                    Text(
                        tipoConexaoFisicaLabel(tipoConexao),
                        style = MaterialTheme.typography.titleSmall,
                        color = c.textSecondary,
                    )
                })
            }
        }
        correlacao?.papelTopologiaHerdado?.let { papel ->
            item {
                LkListRow(c = c, title = "Papel na rede", trailing = {
                    Text(
                        papelTopologiaLabel(papel),
                        style = MaterialTheme.typography.titleSmall,
                        color = c.textSecondary,
                    )
                })
            }
        }
        item {
            LkListRow(c = c, title = "Descoberto via", showDivider = false, trailing = {
                Text(
                    fonteNomeLabel(dispositivo.fonteNome),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.primary,
                )
            })
        }
    }
}

// ---------------------------------------------------------------------------
// MeshApSheet — ponto de acesso / nó mesh
// ---------------------------------------------------------------------------

// GH#1025 — exposta (sem `private`) pra ser reaproveitada por SinalScreen.kt (mesmo pacote
// ui.screen), que abre esta sheet quando um nó da árvore de topologia é correlacionado a um
// DispositivoRede do scan LAN. Continua fisicamente aqui (não extraída pra ui/component/) porque
// depende de vários helpers file-private deste arquivo (SheetDragHandle, LkListRow, mascaraMac
// etc.) — extrair exigiria mover esse conjunto inteiro, refactor maior que o necessário pra #1025.
@Composable
fun MeshApSheet(
    dispositivo: DispositivoRede,
    c: LkTokens,
    apelidoAtual: String,
    onSalvarApelido: (String) -> Unit,
) {
    val mac = dispositivo.mac
    // #853 — mesma logica de fallback do DeviceDetailSheet: chave com fallback ip+nome.
    val chaveApelido = dispositivo.chaveApelido()
    val fabricante = dispositivo.fabricante
    var apelidoInput by remember { mutableStateOf(apelidoAtual) }

    val fieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.primary,
            unfocusedBorderColor = c.border,
            focusedLabelColor = c.primary,
            unfocusedLabelColor = c.textSecondary,
            cursorColor = c.primary,
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
                modifier = Modifier.padding(horizontal = LkSpacing.lg),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(LkRadius.input))
                            .background(c.success.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CellTower,
                        contentDescription = null,
                        tint = c.success,
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
                        LkStatusDot(color = c.success)
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text(text = "Online", style = MaterialTheme.typography.labelSmall, color = c.success)
                        Spacer(Modifier.width(LkSpacing.sm))
                        BadgePill(label = "AP Mesh", bg = c.success.copy(0.12f), fg = c.success)
                    }
                }
            }
            Spacer(Modifier.height(LkSpacing.lg))
            HorizontalDivider(color = c.border)
        }

        // Aviso sobre dados disponíveis
        item {
            Spacer(Modifier.height(LkSpacing.md))
            Row(modifier = Modifier.padding(horizontal = LkSpacing.lg)) {
                LkInfoCallout(
                    icon = Icons.Outlined.Info,
                    text =
                        "Sinal, banda e clientes conectados não estão disponíveis via varredura passiva. " +
                            "Para métricas detalhadas, acesse o painel do seu roteador mesh.",
                    iconTint = c.textSecondary,
                )
            }
            Spacer(Modifier.height(LkSpacing.sm))
        }

        // Seção APELIDO (#853 — chave com fallback ip+nome quando não há MAC resolvível)
        if (chaveApelido != null) {
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
                        shape = RoundedCornerShape(LkRadius.input),
                    )
                    Spacer(Modifier.height(LkSpacing.sm))
                    Button(
                        onClick = { onSalvarApelido(apelidoInput.trim()) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = c.primary),
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
        iconColor = c.warning
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
                style = MaterialTheme.typography.headlineSmall,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            if (isLoading) {
                CircularProgressIndicator(color = c.primary)
            } else {
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary),
                ) {
                    Text("Escanear rede")
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
        LkSectionOverline(title, modifier = Modifier.weight(1f))
        if (trailing != null) {
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
    LkSectionOverline(
        text = title,
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
                .background(c.warning.copy(alpha = 0.10f))
                .border(1.dp, c.warning.copy(alpha = 0.35f), RoundedCornerShape(LkRadius.button))
                .padding(LkSpacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = c.warning,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(LkSpacing.sm))
        Text(text = text, style = MaterialTheme.typography.labelSmall, color = c.textPrimary)
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
                Text(
                    text = title,
                    color = c.textPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(LkSpacing.xs))
                    Text(
                        text = subtitle,
                        color = c.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
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
    LkPillBadge(
        text = label,
        containerColor = bg,
        contentColor = fg,
    )
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
        TipoDispositivo.console -> Icons.Outlined.SportsEsports
        TipoDispositivo.desconhecido -> Icons.Outlined.DevicesOther
    }

@Composable
private fun iconBgColor(
    tipo: TipoDispositivo,
    c: LkTokens,
): Color =
    when (tipo) {
        TipoDispositivo.smartphone -> c.primary.copy(alpha = 0.12f)
        TipoDispositivo.computador -> c.success.copy(alpha = 0.12f)
        TipoDispositivo.roteador -> c.primary.copy(alpha = 0.12f)
        TipoDispositivo.pontoAcesso -> c.success.copy(alpha = 0.12f)
        TipoDispositivo.smarthome -> c.warning.copy(alpha = 0.12f)
        else -> c.bgSecondary
    }

@Composable
private fun iconFgColor(
    tipo: TipoDispositivo,
    c: LkTokens,
): Color =
    when (tipo) {
        TipoDispositivo.smartphone -> c.primary
        TipoDispositivo.computador -> c.success
        TipoDispositivo.roteador -> c.primary
        TipoDispositivo.pontoAcesso -> c.success
        TipoDispositivo.smarthome -> c.warning
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
        TipoDispositivo.console -> "Console de jogos"
        TipoDispositivo.desconhecido -> "Desconhecido"
    }

/** #983 (Fase 4) — traduz [TipoConexaoFisica] (confirmado por leitura direta do gateway,
 *  [ResultadoCorrelacaoTopologia.tipoConexaoFisicaConfirmada]) pra rotulo exibido no detalhe
 *  do dispositivo. `internal` pra ser testavel isoladamente (padrao ja usado em
 *  `PapelParaTipoTopologiaLegadoTest`/`PapelParaConnectionNodeTypeTest`). */
internal fun tipoConexaoFisicaLabel(tipo: TipoConexaoFisica): String =
    when (tipo) {
        TipoConexaoFisica.ETHERNET -> "Cabo (Ethernet)"
        TipoConexaoFisica.WIFI -> "Wi-Fi"
        TipoConexaoFisica.DESCONHECIDO -> "Desconhecida"
    }

/** #983 (Fase 4) — traduz [PapelTopologia] herdado por correlacao forte (MAC/ClientSnapshot
 *  exato, [ResultadoCorrelacaoTopologia.papelTopologiaHerdado]) pra rotulo exibido no detalhe
 *  do dispositivo. Nunca chamado com papel vindo de correlacao fraca (so OUI) — ver
 *  [correlacionarDispositivoComTopologia]. */
internal fun papelTopologiaLabel(papel: PapelTopologia): String =
    when (papel) {
        PapelTopologia.ROTEADOR -> "Roteador"
        PapelTopologia.NO_MESH -> "Nó mesh"
        PapelTopologia.REPETIDOR -> "Repetidor"
        PapelTopologia.PONTO_DE_ACESSO -> "Ponto de acesso"
        PapelTopologia.SISTEMA_MESH_PROVAVEL -> "Sistema mesh (provável)"
        PapelTopologia.DESCONHECIDO -> "Desconhecido"
    }

// #854: nunca expor o valor cru de fonteNome na UI (viola "métrica crua sempre
// acompanhada de veredito humano" do design system) — todo valor produzido pelo
// scanner (ver prioridade de fonte em ScannerDispositivosAndroid) precisa de
// tradução aqui. O fallback (`fonte tratada`) so existe pra nao quebrar em caso
// de fonte nova ainda nao mapeada, nunca deve aparecer em uso normal.
private fun fonteNomeLabel(fonte: String) =
    when (fonte) {
        NamingPrioridade.FONTE_NOME_ROUTER_ACTIVE -> "Confirmado pelo roteador"
        "gateway" -> "Roteador (gateway)"
        "mdns" -> "mDNS · Bonjour"
        "mdnsJmDns" -> "mDNS · Bonjour"
        "subnetMdns" -> "mDNS · Bonjour"
        "ssdp" -> "UPnP · SSDP"
        "ssdpXml" -> "UPnP · SSDP"
        "nbns" -> "NetBIOS"
        "arp" -> "ARP (varredura)"
        "subnet" -> "Varredura de rede"
        "tcpProbe" -> "TCP probe"
        else -> "Varredura de rede"
    }

/** Fontes em que o próprio equipamento/dispositivo se identifica ativamente
 *  (protocolo estruturado — UPnP/SSDP, mDNS, leitura ativa do gateway), em vez
 *  de inferência passiva da varredura (ARP/subnet/TCP probe). Usado para
 *  decidir quando mostrar o selo de confiabilidade (ícone) ao lado do nome. */
private val FONTES_CONFIAVEIS =
    setOf(
        NamingPrioridade.FONTE_NOME_ROUTER_ACTIVE,
        "gateway",
        "ssdp",
        "ssdpXml",
        "mdns",
        "mdnsJmDns",
    )

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
