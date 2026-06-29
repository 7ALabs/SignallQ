package io.signallq.app.ui.screen

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.SignalCellularOff
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiFind
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.R
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.WifiLinkSnapshot
import io.signallq.app.core.telephony.MovelSimSnapshot
import io.signallq.app.core.telephony.MovelSnapshot
import io.signallq.app.feature.devices.SnapshotScanDispositivos
import io.signallq.app.feature.diagnostico.BandaWifi
import io.signallq.app.feature.diagnostico.CanalStrings
import io.signallq.app.feature.diagnostico.CanalTextGenerator
import io.signallq.app.feature.diagnostico.DadoCanal
import io.signallq.app.feature.diagnostico.NivelCongestionamento
import io.signallq.app.feature.diagnostico.RedeWifiVizinha
import io.signallq.app.feature.diagnostico.SnapshotEspectroCanal
import io.signallq.app.feature.diagnostico.WifiChannelDiagnosticEngine
import io.signallq.app.feature.wifi.ConfiancaTopologia
import io.signallq.app.feature.wifi.EstadoScanWifi
import io.signallq.app.feature.wifi.GrupoRedeWifi
import io.signallq.app.feature.wifi.RedeClassificada
import io.signallq.app.feature.wifi.RedeVizinha
import io.signallq.app.feature.wifi.SegurancaWifi
import io.signallq.app.feature.wifi.SnapshotScanWifi
import io.signallq.app.feature.wifi.TipoTopologia
import io.signallq.app.feature.wifi.TopologiaWifiEngine
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.OfflineBanner
import io.signallq.app.ui.component.ProfileAvatarButton
import io.signallq.app.ui.component.WifiChannelGuide

// ─── Helpers ──────────────────────────────────────────────────────────────────

private data class TopologiaIconData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val cor: Color,
)

private fun TipoTopologia.toIconData(): TopologiaIconData? =
    when (this) {
        TipoTopologia.ROTEADOR -> TopologiaIconData(Icons.Outlined.Router, LkColors.accent)
        TipoTopologia.ROTEADOR_MESH -> TopologiaIconData(Icons.Outlined.Hub, LkColors.accent)
        TipoTopologia.NO_MESH -> TopologiaIconData(Icons.Outlined.Hub, LkColors.accent)
        TipoTopologia.REPETIDOR -> TopologiaIconData(Icons.Outlined.CellTower, LkColors.warning)
        TipoTopologia.PONTO_DE_ACESSO -> TopologiaIconData(Icons.Outlined.Lan, LkColors.signallQTextSecondaryOnDark)
        TipoTopologia.DESCONHECIDO -> null
    }

private fun signalQuality(
    rssiDbm: Int,
    banda: BandaWifi = BandaWifi.desconhecida,
): String =
    when (banda) {
        BandaWifi.ghz5 ->
            when {
                rssiDbm >= -55 -> "Excelente"
                rssiDbm >= -65 -> "Bom"
                rssiDbm >= -75 -> "Regular"
                else -> "Fraco"
            }
        else ->
            when {
                rssiDbm >= -50 -> "Excelente"
                rssiDbm >= -60 -> "Bom"
                rssiDbm >= -70 -> "Regular"
                else -> "Fraco"
            }
    }

private fun signalColor(
    rssiDbm: Int,
    banda: BandaWifi = BandaWifi.desconhecida,
): Color =
    when (banda) {
        BandaWifi.ghz5 ->
            when {
                rssiDbm >= -65 -> LkColors.success
                rssiDbm >= -75 -> LkColors.warning
                else -> LkColors.error
            }
        else ->
            when {
                rssiDbm >= -60 -> LkColors.success
                rssiDbm >= -70 -> LkColors.warning
                else -> LkColors.error
            }
    }

private fun congestionColor(nivel: NivelCongestionamento): Color =
    when (nivel) {
        NivelCongestionamento.livre -> LkColors.success
        NivelCongestionamento.moderado -> LkColors.warning
        NivelCongestionamento.congestionado -> LkColors.error
    }

private fun securityLabel(s: SegurancaWifi): String =
    when (s) {
        SegurancaWifi.aberta -> "Aberta"
        SegurancaWifi.wep -> "WEP"
        SegurancaWifi.wpa -> "WPA"
        SegurancaWifi.wpa2 -> "WPA2"
        SegurancaWifi.wpa3 -> "WPA3"
        SegurancaWifi.desconhecida -> "Desconhecida"
    }

// ─── ConexaoTipo ──────────────────────────────────────────────────────────────

private enum class ConexaoTipo { WIFI, MOBILE, CABO, DESCONHECIDO }

private fun EstadoConexao.toConexaoTipo(): ConexaoTipo =
    when (this) {
        EstadoConexao.wifi -> ConexaoTipo.WIFI
        EstadoConexao.movel -> ConexaoTipo.MOBILE
        EstadoConexao.ethernet -> ConexaoTipo.CABO
        else -> ConexaoTipo.DESCONHECIDO
    }

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SinalScreen(
    snapshotWifi: SnapshotScanWifi,
    connectedNetwork: RedeVizinha?,
    estadoConexao: EstadoConexao,
    conectado: Boolean = true,
    movelSnapshot: MovelSnapshot? = null,
    simsAtivos: List<MovelSimSnapshot> = emptyList(),
    localIp: String? = null,
    temPermissaoTelefonia: Boolean = false,
    onSolicitarPermissaoTelefonia: () -> Unit = {},
    temPermissaoLocalizacao: Boolean = true,
    localizacaoBloqueadaPermanentemente: Boolean = false,
    onSolicitarPermissaoLocalizacao: () -> Unit = {},
    onRefresh: () -> Unit,
    onVoltar: () -> Unit,
    nomeUsuario: String = "",
    fotoUri: String? = null,
    onAbrirPerfil: () -> Unit = {},
    wifiLinkSnapshot: WifiLinkSnapshot? = null,
    snapshotDispositivos: SnapshotScanDispositivos? = null,
    apelidos: Map<String, String> = emptyMap(),
    onSalvarApelido: (mac: String, apelido: String) -> Unit = { _, _ -> },
) {
    val c = LocalLkTokens.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val conexaoTipo = estadoConexao.toConexaoTipo()

    var showLocalizacaoSheet by remember { mutableStateOf(false) }
    var localizacaoSheetDismissed by remember { mutableStateOf(false) }
    var showTelefoniaSheet by remember { mutableStateOf(false) }
    var telefoniaSheetDismissed by remember { mutableStateOf(false) }

    val locSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val telSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(conexaoTipo, temPermissaoLocalizacao, localizacaoSheetDismissed) {
        if (conexaoTipo == ConexaoTipo.WIFI && !temPermissaoLocalizacao && !localizacaoSheetDismissed) {
            showLocalizacaoSheet = true
        }
    }

    LaunchedEffect(conexaoTipo, temPermissaoTelefonia, telefoniaSheetDismissed) {
        if (conexaoTipo == ConexaoTipo.MOBILE && !temPermissaoTelefonia && !telefoniaSheetDismissed) {
            showTelefoniaSheet = true
        }
    }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CellTower,
                            contentDescription = "Sinal",
                            tint = c.textPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text("Sinal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W600, color = c.textPrimary)
                    }
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
        // Auto-selecionar tab Móvel quando não estiver em Wi-Fi
        LaunchedEffect(conexaoTipo) {
            if (conexaoTipo != ConexaoTipo.WIFI) {
                selectedTab = 2
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!conectado) OfflineBanner()
            if (conexaoTipo == ConexaoTipo.WIFI && !temPermissaoLocalizacao && !localizacaoSheetDismissed) {
                LocPermissaoBanner(onClick = { showLocalizacaoSheet = true })
            }

            SinalTopTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                snapshotWifi = snapshotWifi,
                connectedNetwork = connectedNetwork,
                c = c,
            )
            when (selectedTab) {
                0 -> {
                    if (conexaoTipo == ConexaoTipo.WIFI) {
                        RedesTab(
                            snapshotWifi = snapshotWifi,
                            connectedNetwork = connectedNetwork,
                            onRefresh = onRefresh,
                            wifiLinkSnapshot = wifiLinkSnapshot,
                        )
                    } else {
                        WifiEmptyState()
                    }
                }
                1 -> {
                    if (conexaoTipo == ConexaoTipo.WIFI) {
                        CanalTab(
                            redes = snapshotWifi.redes,
                            connectedNetwork = connectedNetwork,
                            estado = snapshotWifi.estado,
                            erroMensagem = snapshotWifi.erroMensagem,
                            onRefresh = onRefresh,
                            wifiLinkSnapshot = wifiLinkSnapshot,
                        )
                    } else {
                        WifiEmptyState()
                    }
                }
                2 -> {
                    MovelTab(
                        movelSnapshot = movelSnapshot,
                        simsAtivos = simsAtivos,
                        temPermissaoTelefonia = temPermissaoTelefonia,
                        onSolicitarPermissaoTelefonia = onSolicitarPermissaoTelefonia,
                        tokens = c,
                    )
                }
                else -> {
                    if (conexaoTipo == ConexaoTipo.WIFI) {
                        val snap = snapshotDispositivos
                        if (snap != null) {
                            DispositivosTabContent(
                                snapshotDevices = snap,
                                onRefresh = onRefresh,
                                apelidos = apelidos,
                                onSalvarApelido = onSalvarApelido,
                                c = c,
                            )
                        }
                    } else {
                        WifiEmptyState()
                    }
                }
            }
        }
    }

    if (showLocalizacaoSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showLocalizacaoSheet = false
                localizacaoSheetDismissed = true
            },
            sheetState = locSheetState,
        ) {
            PermissaoLocalizacaoContextoSheet(
                bloqueadaPermanentemente = localizacaoBloqueadaPermanentemente,
                onConceder = {
                    showLocalizacaoSheet = false
                    onSolicitarPermissaoLocalizacao()
                },
                onAgoraNao = {
                    showLocalizacaoSheet = false
                    localizacaoSheetDismissed = true
                },
            )
        }
    }

    if (showTelefoniaSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showTelefoniaSheet = false
                telefoniaSheetDismissed = true
            },
            sheetState = telSheetState,
        ) {
            PermissaoTelefoniaContextoSheet(
                onConceder = {
                    showTelefoniaSheet = false
                    onSolicitarPermissaoTelefonia()
                },
                onAgoraNao = {
                    showTelefoniaSheet = false
                    telefoniaSheetDismissed = true
                },
            )
        }
    }
}

// ─── SinalTopTabRow ───────────────────────────────────────────────────────────

/**
 * TabRow da tela Sinal com badge de congestionamento no canal Wi-Fi.
 * Extraido do corpo principal do SinalScreen para permitir skip de recomposicao
 * quando apenas o conteudo das tabs muda (e nao os labels/badges).
 */
@Composable
private fun SinalTopTabRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    snapshotWifi: SnapshotScanWifi,
    connectedNetwork: RedeVizinha?,
    c: LkTokens,
) {
    val canalCongestionado =
        remember(snapshotWifi.redes, connectedNetwork) {
            if (connectedNetwork == null) return@remember false
            val bandaConectada = connectedNetwork.banda ?: return@remember false
            val redesBanda = snapshotWifi.redes.filter { it.banda == bandaConectada }
            val espectro =
                WifiChannelDiagnosticEngine.computarEspectro(
                    redes =
                        redesBanda.map {
                            RedeWifiVizinha(
                                canal = it.canal,
                                rssiDbm = it.rssiDbm,
                                frequenciaMhz = it.frequenciaMhz,
                                ssid = it.ssid,
                                bssid = it.bssid,
                            )
                        },
                    canalAtual = connectedNetwork.canal,
                    banda = bandaConectada,
                    seuSSID = connectedNetwork.ssid,
                )
            espectro.dadosPorCanal
                .firstOrNull { it.ehCanalAtual }
                ?.nivel == NivelCongestionamento.congestionado
        }

    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = c.bgPrimary,
        contentColor = LkColors.accent,
    ) {
        listOf("Wi-Fi", "Canal", "Móvel", "Dispositivos").forEachIndexed { index, label ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    if (index == 1 && canalCongestionado) {
                        BadgedBox(badge = { Badge() }) {
                            Text(
                                label,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.W500,
                                color = if (selectedTab == index) LkColors.accent else c.textSecondary,
                            )
                        }
                    } else {
                        Text(
                            label,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.W500,
                            color = if (selectedTab == index) LkColors.accent else c.textSecondary,
                        )
                    }
                },
            )
        }
    }
}

// ─── Tab Móvel ────────────────────────────────────────────────────────────────

@Composable
private fun MovelTab(
    movelSnapshot: MovelSnapshot?,
    simsAtivos: List<MovelSimSnapshot>,
    temPermissaoTelefonia: Boolean,
    onSolicitarPermissaoTelefonia: () -> Unit,
    tokens: LkTokens,
) {
    val c = tokens
    if (!temPermissaoTelefonia) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStatePermissaoTelefonia(
                onSolicitarPermissao = onSolicitarPermissaoTelefonia,
                tokens = c,
            )
        }
        return
    }
    if (movelSnapshot == null && simsAtivos.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateMobile(c)
        }
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(LkSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        if (simsAtivos.isNotEmpty()) {
            ChipsAtivosSection(simsAtivos = simsAtivos, tokens = c)
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LkColors.accent.copy(alpha = 0.07f))
                    .padding(horizontal = LkSpacing.md, vertical = LkSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            Icon(
                Icons.Outlined.Wifi,
                contentDescription = null,
                tint = LkColors.accent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                "Testes pelo chip usam dados do seu plano. Prefira o Wi-Fi quando possível.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
    }
}

@Composable
private fun ChipsAtivosSection(
    simsAtivos: List<MovelSimSnapshot>,
    tokens: LkTokens,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Text(
            "CHIPS ATIVOS",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.W700,
            color = tokens.textTertiary,
            modifier = Modifier.padding(bottom = LkSpacing.xs),
        )
        simsAtivos.forEach { sim ->
            SimCard(sim = sim, tokens = tokens)
        }
    }
}

@Composable
private fun SimCard(
    sim: MovelSimSnapshot,
    tokens: LkTokens,
) {
    val forcaSinal =
        sim.rsrpDbm?.let { rsrp ->
            when {
                rsrp > -85 -> "Forte"
                rsrp > -100 -> "Médio"
                else -> "Fraco"
            }
        }
    val corForca =
        sim.rsrpDbm?.let { rsrp ->
            when {
                rsrp > -85 -> LkColors.success
                rsrp > -100 -> LkColors.warning
                else -> LkColors.error
            }
        }
    val qualidade =
        sim.rsrpDbm?.let { rsrp ->
            when {
                rsrp > -80 -> "Excelente"
                rsrp > -90 -> "Bom"
                rsrp > -100 -> "Regular"
                else -> "Ruim"
            }
        }
    val corQualidade =
        sim.rsrpDbm?.let { rsrp ->
            when {
                rsrp > -80 -> LkColors.success
                rsrp > -90 -> LkColors.accentBlue
                rsrp > -100 -> LkColors.warning
                else -> LkColors.error
            }
        }
    val descricao =
        when {
            sim.isDefaultData && forcaSinal == "Forte" -> "Chamadas e dados estão usando este chip."
            sim.isDefaultData && forcaSinal == "Médio" -> "Chamadas e dados estão usando este chip. Sinal razoável."
            sim.isDefaultData -> "Chamadas e dados estão usando este chip. Sinal fraco neste local."
            forcaSinal == "Fraco" -> "Sinal fraco neste local. Chamadas podem cair ou ficar sem sinal."
            else -> null
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(tokens.bgCard)
                .border(1.dp, tokens.border, RoundedCornerShape(16.dp))
                .padding(LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        // Header: SIM icon + SIM N + EM USO badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.SimCard,
                contentDescription = null,
                tint = LkColors.accent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(LkSpacing.xs))
            Text(
                "SIM ${sim.simIndex}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.W600,
                color = tokens.textSecondary,
            )
            Spacer(Modifier.weight(1f))
            if (sim.isDefaultData) {
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LkColors.success.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        "EM USO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.W700,
                        color = LkColors.success,
                    )
                }
            }
        }

        // Carrier name
        Text(
            sim.operadora ?: "Operadora",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.W700,
            color = tokens.textPrimary,
        )

        // Network type subtitle
        Text(
            "Rede ${sim.tecnologiaRede ?: "móvel"}",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textSecondary,
        )

        // Signal and Quality metrics
        if (forcaSinal != null && corForca != null && qualidade != null && corQualidade != null) {
            Spacer(Modifier.height(LkSpacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.xl),
            ) {
                Column {
                    Text(
                        "SINAL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.W600,
                        color = tokens.textTertiary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        forcaSinal,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.W700,
                        color = corForca,
                    )
                }
                Column {
                    Text(
                        "QUALIDADE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.W600,
                        color = tokens.textTertiary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        qualidade,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.W700,
                        color = corQualidade,
                    )
                }
            }
        }

        // Contextual description
        if (descricao != null) {
            Spacer(Modifier.height(LkSpacing.xs))
            Text(
                descricao,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textSecondary,
                lineHeight = 18.sp,
            )
        }

        // Roaming warning
        if (sim.emRoaming) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
            ) {
                Icon(
                    Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = LkColors.warning,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    "Roaming internacional",
                    style = MaterialTheme.typography.bodySmall,
                    color = LkColors.warning,
                )
            }
        }
    }
}

// ─── Wi-Fi empty state (quando não está em Wi-Fi) ─────────────────────────────

@Composable
private fun WifiEmptyState() {
    val c = LocalLkTokens.current
    Box(
        Modifier
            .fillMaxSize()
            .padding(LkSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Wifi, null, tint = c.textTertiary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(LkSpacing.lg))
            Text(
                "Você está usando a internet do chip",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                "O Wi-Fi está desligado ou desconectado. Conecte-se a uma rede Wi-Fi para ver os detalhes.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

// ─── Tab Redes ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RedesTab(
    snapshotWifi: SnapshotScanWifi,
    connectedNetwork: RedeVizinha?,
    onRefresh: () -> Unit,
    wifiLinkSnapshot: WifiLinkSnapshot? = null,
) {
    val c = LocalLkTokens.current
    var selectedBanda by remember { mutableStateOf("Todos") }
    var selectedNetwork by remember { mutableStateOf<RedeVizinha?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isRefreshing = snapshotWifi.estado == EstadoScanWifi.scanning

    val filteredRedes =
        remember(snapshotWifi.redes, selectedBanda) {
            if (selectedBanda == "Todos") {
                snapshotWifi.redes
            } else {
                snapshotWifi.redes.filter { it.banda == selectedBanda }
            }
        }
    val showConnected =
        remember(selectedBanda, connectedNetwork) {
            connectedNetwork != null && (selectedBanda == "Todos" || connectedNetwork.banda == selectedBanda)
        }

    // Classificação de topologia para todas as redes visíveis
    val topologiaPorBssid =
        remember(snapshotWifi.redes, connectedNetwork) {
            runCatching {
                val classificadas =
                    TopologiaWifiEngine.classificar(
                        redes = snapshotWifi.redes,
                        connectedBssid = connectedNetwork?.bssid,
                    )
                classificadas.associate { it.rede.bssid to it.tipo }
            }.getOrElse { emptyMap() }
        }

    // Nós da mesma rede: conectado na frente + mesmo SSID ordenado por sinal
    val grupoNos =
        remember(filteredRedes, connectedNetwork) {
            if (connectedNetwork == null) return@remember emptyList()
            buildList {
                add(connectedNetwork)
                addAll(
                    filteredRedes
                        .filter { it.bssid != connectedNetwork.bssid && it.ssid != null && it.ssid == connectedNetwork.ssid }
                        .sortedByDescending { it.rssiDbm },
                )
            }
        }

    // Redes de outros SSIDs (exclui nós da mesma rede) — classificadas e agrupadas por SSID
    val otherClassificadas =
        remember(filteredRedes, connectedNetwork, filteredRedes.size) {
            val connSsid = connectedNetwork?.ssid
            val filtered =
                filteredRedes
                    .filter { it.bssid != connectedNetwork?.bssid }
                    .filter { rede -> connSsid == null || rede.ssid == null || rede.ssid != connSsid }

            // Classificar via TopologiaWifiEngine; fallback gracioso para lista vazia
            val classificadas =
                runCatching {
                    TopologiaWifiEngine.classificar(
                        redes = filtered,
                        connectedBssid = connectedNetwork?.bssid,
                    )
                }.getOrElse {
                    filtered.map { rede ->
                        RedeClassificada(rede = rede, tipo = TipoTopologia.DESCONHECIDO, confianca = ConfiancaTopologia.BAIXA, motivo = "")
                    }
                }

            // Agrupar por SSID (SSIDs nulos vão para "[Ocultas]")
            classificadas
                .groupBy { it.rede.ssid ?: "[Ocultas]" }
                .map { (ssid, redes) ->
                    GrupoRedeWifi(
                        ssid = ssid,
                        redes = redes.sortedByDescending { it.rede.rssiDbm },
                    )
                }.sortedByDescending { grupo -> grupo.redes.maxOfOrNull { it.rede.rssiDbm } ?: Int.MIN_VALUE }
        }

    // Estado para expandir/colapsar SSIDs com múltiplos BSSIDs
    var expandedSsids by remember { mutableStateOf(setOf<String>()) }
    var mostrarTodasRedes by remember { mutableStateOf(false) }
    val redesExibidas = if (mostrarTodasRedes) otherClassificadas else otherClassificadas.take(5)

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = LkSpacing.xl),
        ) {
            if (snapshotWifi.estado == EstadoScanWifi.erro) {
                item {
                    val msg = snapshotWifi.erroMensagem
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(LkColors.error.copy(alpha = 0.1f))
                            .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.WifiFind, null, tint = LkColors.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(LkSpacing.sm))
                        Text(
                            msg ?: "Erro ao escanear redes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Normal,
                            color = LkColors.error,
                        )
                    }
                }
            }

            item {
                BandFilterRow(
                    selected = selectedBanda,
                    bands = listOf("Todos", "2.4GHz", "5GHz", "6GHz"),
                    onSelect = { selectedBanda = it },
                    modifier = Modifier.padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
                )
            }

            if (showConnected && connectedNetwork != null) {
                item {
                    SectionLabel(
                        stringResource(R.string.sinal_sua_conexao),
                        modifier = Modifier.padding(horizontal = LkSpacing.lg),
                    )
                    Spacer(Modifier.height(LkSpacing.sm))
                    Box(
                        modifier =
                            Modifier
                                .padding(horizontal = LkSpacing.lg)
                                .clip(RoundedCornerShape(12.dp))
                                .background(LkColors.success.copy(alpha = 0.12f)),
                    ) {
                        GrupoRedeTree(
                            ssid = connectedNetwork.ssid ?: "Rede oculta",
                            nos = grupoNos,
                            connectedBssid = connectedNetwork.bssid,
                            onNoClick = { selectedNetwork = it },
                            wifiLinkSnapshot = wifiLinkSnapshot,
                            topologiaPorBssid = topologiaPorBssid,
                        )
                    }
                    Spacer(Modifier.height(LkSpacing.lg))
                }
            } else if (connectedNetwork != null && selectedBanda != "Todos") {
                item {
                    SectionLabel(
                        stringResource(R.string.sinal_sua_conexao),
                        modifier = Modifier.padding(horizontal = LkSpacing.lg),
                    )
                    Spacer(Modifier.height(LkSpacing.sm))
                    Row(
                        modifier =
                            Modifier
                                .padding(horizontal = LkSpacing.lg)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(c.bgSecondary)
                                .border(1.dp, c.border, RoundedCornerShape(12.dp))
                                .padding(LkSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                    ) {
                        Icon(
                            Icons.Outlined.Wifi,
                            contentDescription = null,
                            tint = c.textTertiary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Conectado em ${connectedNetwork.banda ?: "outra banda"} (${connectedNetwork.ssid ?: "rede"})",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary,
                        )
                    }
                    Spacer(Modifier.height(LkSpacing.lg))
                }
            }

            if (otherClassificadas.isNotEmpty()) {
                item {
                    SectionLabel(
                        if (showConnected ||
                            (connectedNetwork != null && selectedBanda != "Todos")
                        ) {
                            "OUTRAS REDES"
                        } else {
                            "REDES DISPONÍVEIS"
                        },
                        modifier = Modifier.padding(horizontal = LkSpacing.lg),
                    )
                    Spacer(Modifier.height(LkSpacing.sm))
                }
                items(redesExibidas, key = { it.ssid }) { grupo ->
                    OtherNetworkGroupItem(
                        grupo = grupo,
                        isExpanded = expandedSsids.contains(grupo.ssid),
                        onToggleExpanded = {
                            expandedSsids =
                                if (expandedSsids.contains(grupo.ssid)) {
                                    expandedSsids - grupo.ssid
                                } else {
                                    expandedSsids + grupo.ssid
                                }
                        },
                        onNetworkClick = { rede -> selectedNetwork = rede },
                        topologiaPorBssid = topologiaPorBssid,
                        modifier = Modifier.padding(horizontal = LkSpacing.lg),
                    )
                    HorizontalDivider(color = c.border, modifier = Modifier.padding(horizontal = LkSpacing.lg))
                }
                if (otherClassificadas.size > 5 && !mostrarTodasRedes) {
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { mostrarTodasRedes = true }
                                    .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "Mostrar Mais (${otherClassificadas.size - 5})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.W600,
                                color = LkColors.accent,
                            )
                        }
                    }
                }
            } else if (!isRefreshing) {
                if (snapshotWifi.redes.isEmpty()) {
                    item { EmptyStateWifi() }
                } else if (selectedBanda != "Todos" && filteredRedes.isEmpty()) {
                    val redesOutrasFaixas = snapshotWifi.redes.filter { it.banda != selectedBanda }
                    if (redesOutrasFaixas.isNotEmpty()) {
                        val porFaixa = redesOutrasFaixas.groupBy { it.banda }
                        item {
                            EmptyStateBandaVazia(
                                bandaSelecionada = selectedBanda,
                                redesPorFaixa = porFaixa,
                                onTrocarFaixa = { selectedBanda = it },
                            )
                        }
                    } else {
                        item { EmptyStateWifi() }
                    }
                }
            }
        }
    }

    val net = selectedNetwork
    if (net != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedNetwork = null },
            sheetState = sheetState,
            containerColor = c.bgCard,
            dragHandle = {},
        ) {
            NetworkDetailSheet(rede = net)
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.W600,
        color = c.textTertiary,
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun BandFilterRow(
    selected: String,
    bands: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        bands.forEach { band ->
            val active = selected == band
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) LkColors.accent.copy(alpha = 0.12f) else c.bgSecondary)
                        .minimumInteractiveComponentSize()
                        .clickable { onSelect(band) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    band,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (active) FontWeight.W600 else FontWeight.W500,
                    color = if (active) LkColors.accent else c.textSecondary,
                )
            }
        }
    }
}

// ─── Grupo Rede Tree ──────────────────────────────────────────────────────────

@Composable
private fun GrupoRedeTree(
    ssid: String,
    nos: List<RedeVizinha>,
    connectedBssid: String?,
    onNoClick: (RedeVizinha) -> Unit,
    modifier: Modifier = Modifier,
    wifiLinkSnapshot: WifiLinkSnapshot? = null,
    topologiaPorBssid: Map<String, TipoTopologia> = emptyMap(),
) {
    val c = LocalLkTokens.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .padding(LkSpacing.md),
    ) {
        // Raiz: SSID da rede
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = LkSpacing.sm, vertical = LkSpacing.sm),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(LkColors.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Wifi, null, tint = LkColors.accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(LkSpacing.md))
            Column {
                val temConectado = nos.any { it.bssid == connectedBssid }
                if (temConectado) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
                    ) {
                        Text(
                            ssid,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.W600,
                            color = c.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LkColors.success.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "Conectado",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.W600,
                                color = LkColors.success,
                            )
                        }
                    }
                } else {
                    Text(
                        ssid,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val count = nos.size
                Text(
                    "$count nó${if (count != 1) "s" else ""} detectado${if (count != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textTertiary,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Nós em árvore
        nos.forEachIndexed { index, no ->
            val isConnected = no.bssid == connectedBssid
            NoTreeItem(
                rede = no,
                label =
                    when {
                        isConnected -> "Conectado agora"
                        index == 0 -> "Gateway"
                        else -> "Nó #$index"
                    },
                isConnected = isConnected,
                isLast = index == nos.size - 1,
                onClick = { onNoClick(no) },
                wifiLinkSnapshot = if (isConnected) wifiLinkSnapshot else null,
                tipoTopologia = topologiaPorBssid[no.bssid],
            )
        }

        // Aviso de estimativa quando há mais de um nó
        if (nos.size > 1) {
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                "* Gateway estimado pelo sinal mais forte",
                fontSize = 10.sp,
                color = c.textTertiary,
                modifier = Modifier.padding(horizontal = LkSpacing.sm),
            )
        }
    }
}

@Composable
private fun NoTreeItem(
    rede: RedeVizinha,
    label: String,
    isConnected: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    wifiLinkSnapshot: WifiLinkSnapshot? = null,
    tipoTopologia: TipoTopologia? = null,
) {
    val c = LocalLkTokens.current
    val lineColor = c.border

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Conector da árvore: linha vertical + ramificação horizontal
        Canvas(modifier = Modifier.width(28.dp).fillMaxHeight()) {
            val midX = 14.dp.toPx()
            val midY = size.height / 2f
            val strokeW = 1.5.dp.toPx()
            drawLine(lineColor, Offset(midX, 0f), Offset(midX, if (isLast) midY else size.height), strokeWidth = strokeW)
            drawLine(lineColor, Offset(midX, midY), Offset(size.width, midY), strokeWidth = strokeW)
        }

        // Card do nó
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(top = 4.dp, bottom = 4.dp, end = LkSpacing.sm)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isConnected) LkColors.accent.copy(alpha = 0.12f) else Color.Transparent)
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onClick)
                    .padding(horizontal = LkSpacing.sm, vertical = LkSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val bandaVizinha =
                when {
                    rede.frequenciaMhz < 3000 -> BandaWifi.ghz24
                    else -> BandaWifi.ghz5
                }
            Icon(
                imageVector = if (isConnected) Icons.Outlined.Router else Icons.Outlined.Wifi,
                contentDescription = null,
                tint = if (isConnected) LkColors.accent else c.textSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(LkSpacing.sm))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.W600,
                        color = if (isConnected) LkColors.accent else c.textPrimary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "· ${rede.bssid.takeLast(8)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isConnected) {
                        Spacer(Modifier.width(LkSpacing.sm))
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LkColors.success.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text("✓ Conectado", fontSize = 12.sp, fontWeight = FontWeight.W600, color = LkColors.success)
                        }
                    }
                    val topologiaIcon = tipoTopologia?.toIconData()
                    if (topologiaIcon != null) {
                        Spacer(Modifier.width(LkSpacing.xs))
                        Icon(
                            imageVector = topologiaIcon.icon,
                            contentDescription = null,
                            tint = if (isConnected) LkColors.accent else topologiaIcon.cor,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(rede.banda, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                    Text("  ·  ", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                    Text(
                        signalQuality(rede.rssiDbm, bandaVizinha),
                        style = MaterialTheme.typography.bodySmall,
                        color = signalColor(rede.rssiDbm, bandaVizinha),
                    )
                }
                // #177 — segunda linha: banda, RSSI, canal
                val canalNo = rede.canal
                val metaPartsNo =
                    buildList {
                        add("Banda: ${rede.banda}")
                        add("RSSI: ${rede.rssiDbm} dBm")
                        if (canalNo != null) add("Canal: $canalNo")
                    }
                Text(
                    metaPartsNo.joinToString("  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (wifiLinkSnapshot != null) {
                    val parts =
                        listOfNotNull(
                            wifiLinkSnapshot.padraoWifi?.takeIf { it.isNotBlank() },
                            wifiLinkSnapshot.linkSpeedMbps?.let { "$it Mbps" },
                        )
                    val infoText = parts.joinToString(" · ")
                    if (infoText.isNotEmpty()) {
                        Text(infoText, style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                    }
                }
            }
            Spacer(Modifier.width(LkSpacing.sm))
            SignalBars(rssiDbm = rede.rssiDbm, banda = bandaVizinha)
        }
    }
}

@Composable
private fun OtherNetworkGroupItem(
    grupo: GrupoRedeWifi,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onNetworkClick: (RedeVizinha) -> Unit,
    topologiaPorBssid: Map<String, TipoTopologia>,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val isSingleBssid = grupo.redes.size == 1
    val isOculta = grupo.ssid == "[Ocultas]"

    Column(modifier = modifier.fillMaxWidth()) {
        if (isSingleBssid) {
            // Single BSSID: sem chevron, click abre detalhe direto
            val redeClass = grupo.redes[0]
            val rede = redeClass.rede
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clickable { onNetworkClick(rede) }
                        .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Wifi, null, tint = c.textSecondary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(LkSpacing.md))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (isOculta) "Rede oculta" else (rede.ssid ?: "Rede oculta"),
                            fontWeight = FontWeight.W500,
                            color = c.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        val topologiaIcon = redeClass.tipo.toIconData()
                        if (topologiaIcon != null) {
                            Spacer(Modifier.width(LkSpacing.xs))
                            Icon(
                                imageVector = topologiaIcon.icon,
                                contentDescription = null,
                                tint = topologiaIcon.cor,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    // #177 — segunda linha: banda, RSSI, canal
                    val canalSingle = rede.canal
                    val metaPartsSingle =
                        buildList {
                            add("Banda: ${rede.banda}")
                            add("RSSI: ${rede.rssiDbm} dBm")
                            if (canalSingle != null) add("Canal: $canalSingle")
                        }
                    Text(
                        metaPartsSingle.joinToString("  "),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(LkSpacing.sm))
                Icon(
                    imageVector = if (rede.seguranca == SegurancaWifi.aberta) Icons.Filled.LockOpen else Icons.Filled.Lock,
                    contentDescription = if (rede.seguranca == SegurancaWifi.aberta) stringResource(R.string.cd_rede_aberta) else stringResource(R.string.cd_rede_protegida),
                    tint = c.textTertiary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(LkSpacing.sm))
                val banda =
                    when {
                        rede.frequenciaMhz < 3000 -> BandaWifi.ghz24
                        else -> BandaWifi.ghz5
                    }
                SignalBars(rssiDbm = rede.rssiDbm, banda = banda)
            }
        } else {
            // Multi-BSSID: cabeçalho com chevron expansível
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .semantics {
                            val nomeGrupo = if (isOculta) "Redes ocultas" else grupo.ssid
                            contentDescription =
                                if (isExpanded) "Recolher redes do grupo $nomeGrupo" else "Expandir redes do grupo $nomeGrupo"
                        }.clickable { onToggleExpanded() }
                        .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Wifi, null, tint = c.textSecondary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(LkSpacing.md))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (isOculta) "Redes ocultas" else grupo.ssid,
                            fontWeight = FontWeight.W500,
                            color = c.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text(
                            "· ${grupo.redes.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textTertiary,
                        )
                    }
                    val bestSignal =
                        grupo.redes
                            .maxByOrNull { it.rede.rssiDbm }
                            ?.rede
                            ?.rssiDbm ?: 0
                    val banda =
                        when {
                            grupo.redes.any { it.rede.frequenciaMhz < 3000 } -> "2.4GHz"
                            else -> "5GHz"
                        }
                    Text(banda, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                }
                Spacer(Modifier.width(LkSpacing.sm))
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = c.textSecondary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(LkSpacing.sm))
                val bestBanda =
                    when {
                        grupo.redes.any { it.rede.frequenciaMhz < 3000 } -> BandaWifi.ghz24
                        else -> BandaWifi.ghz5
                    }
                SignalBars(rssiDbm = grupo.redes.maxOfOrNull { it.rede.rssiDbm } ?: 0, banda = bestBanda)
            }

            // Expandido: lista de nós
            if (isExpanded) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(start = LkSpacing.lg, top = LkSpacing.sm, bottom = LkSpacing.sm),
                ) {
                    grupo.redes.forEach { redeClass ->
                        val rede = redeClass.rede
                        val banda =
                            when {
                                rede.frequenciaMhz < 3000 -> BandaWifi.ghz24
                                else -> BandaWifi.ghz5
                            }
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable { onNetworkClick(rede) }
                                    .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val topologiaIcon = redeClass.tipo.toIconData()
                            if (topologiaIcon != null) {
                                Icon(
                                    imageVector = topologiaIcon.icon,
                                    contentDescription = null,
                                    tint = topologiaIcon.cor,
                                    modifier = Modifier.size(16.dp),
                                )
                            } else {
                                Spacer(Modifier.width(16.dp))
                            }
                            Spacer(Modifier.width(LkSpacing.md))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        rede.bssid.takeLast(8),
                                        fontWeight = FontWeight.W500,
                                        color = c.textPrimary,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Text(
                                    signalQuality(rede.rssiDbm, banda),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = signalColor(rede.rssiDbm, banda),
                                )
                                // #177 — segunda linha: banda, RSSI, canal
                                val canalMulti = rede.canal
                                val metaPartsMulti =
                                    buildList {
                                        add("Banda: ${rede.banda}")
                                        add("RSSI: ${rede.rssiDbm} dBm")
                                        if (canalMulti != null) add("Canal: $canalMulti")
                                    }
                                Text(
                                    metaPartsMulti.joinToString("  "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.textTertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Spacer(Modifier.width(LkSpacing.sm))
                            SignalBars(rssiDbm = rede.rssiDbm, banda = banda)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkListItem(
    rede: RedeVizinha,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tipoTopologia: TipoTopologia? = null,
) {
    val c = LocalLkTokens.current
    val ssid = rede.ssid
    val isOpen = rede.seguranca == SegurancaWifi.aberta
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Wifi, null, tint = c.textSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(LkSpacing.md))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    ssid ?: "Rede oculta",
                    fontWeight = FontWeight.W500,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                val topologiaIcon = tipoTopologia?.toIconData()
                if (topologiaIcon != null) {
                    Spacer(Modifier.width(LkSpacing.xs))
                    Icon(
                        imageVector = topologiaIcon.icon,
                        contentDescription = null,
                        tint = topologiaIcon.cor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(rede.banda, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
        Spacer(Modifier.width(LkSpacing.sm))
        Icon(
            imageVector = if (isOpen) Icons.Filled.LockOpen else Icons.Filled.Lock,
            contentDescription = if (isOpen) stringResource(R.string.cd_rede_aberta) else stringResource(R.string.cd_rede_protegida),
            tint = c.textTertiary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(LkSpacing.sm))
        val bandaNetworkListItem =
            when {
                rede.frequenciaMhz < 3000 -> BandaWifi.ghz24
                else -> BandaWifi.ghz5
            }
        SignalBars(rssiDbm = rede.rssiDbm, banda = bandaNetworkListItem)
    }
}

@Composable
private fun SignalBars(
    rssiDbm: Int,
    banda: BandaWifi = BandaWifi.desconhecida,
) {
    val bars =
        when {
            rssiDbm >= -50 -> 4
            rssiDbm >= -60 -> 3
            rssiDbm >= -70 -> 2
            else -> 1
        }
    val color = signalColor(rssiDbm, banda)
    val c = LocalLkTokens.current
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        for (i in 1..4) {
            Box(
                Modifier
                    .width(4.dp)
                    .height((4 + i * 3).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (i <= bars) color else c.border),
            )
        }
    }
}

@Composable
private fun EmptyStateWifi() {
    val c = LocalLkTokens.current
    Box(Modifier.fillMaxWidth().padding(vertical = 80.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.WifiFind, null, tint = c.textTertiary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(LkSpacing.md))
            Text("Nenhuma rede encontrada", color = c.textSecondary, fontWeight = FontWeight.W500)
            Text("Puxe para atualizar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Normal, color = c.textTertiary)
        }
    }
}

@Composable
private fun EmptyStateBandaVazia(
    bandaSelecionada: String,
    redesPorFaixa: Map<String, List<RedeVizinha>>,
    onTrocarFaixa: (String) -> Unit,
) {
    val c = LocalLkTokens.current
    val totalOutras = redesPorFaixa.values.sumOf { it.size }
    val targetBanda = if (redesPorFaixa.size == 1) redesPorFaixa.keys.first() else "Todos"
    val subtitulo =
        if (redesPorFaixa.size == 1) {
            stringResource(R.string.sinal_redes_em_faixa, totalOutras, redesPorFaixa.keys.first())
        } else {
            stringResource(R.string.sinal_redes_em_outras_faixas, totalOutras)
        }

    Box(Modifier.fillMaxWidth().padding(vertical = 80.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.WifiFind, null, tint = c.textTertiary, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(LkSpacing.md))
            Text(
                stringResource(R.string.sinal_sem_redes_nesta_faixa, bandaSelecionada),
                color = c.textSecondary,
                fontWeight = FontWeight.W500,
            )
            Spacer(Modifier.height(LkSpacing.xs))
            Text(
                subtitulo,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Normal,
                color = c.textTertiary,
            )
            Spacer(Modifier.height(LkSpacing.md))
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(LkColors.accent.copy(alpha = 0.12f))
                        .minimumInteractiveComponentSize()
                        .clickable { onTrocarFaixa(targetBanda) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.sinal_trocar_faixa),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = LkColors.accent,
                )
            }
        }
    }
}

// ─── Network detail sheet ──────────────────────────────────────────────────────

@Composable
private fun NetworkDetailSheet(rede: RedeVizinha) {
    val c = LocalLkTokens.current
    val ssid = rede.ssid
    val largura = rede.larguraCanalMhz
    val channel = rede.canal

    Column(Modifier.fillMaxWidth().padding(horizontal = LkSpacing.lg, vertical = LkSpacing.lg)) {
        Box(
            Modifier
                .width(40.dp)
                .height(4.dp)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(2.dp))
                .background(c.border),
        )
        Spacer(Modifier.height(LkSpacing.lg))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (rede.seguranca == SegurancaWifi.aberta) Icons.Filled.LockOpen else Icons.Filled.Lock,
                contentDescription = null,
                tint = LkColors.accent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(LkSpacing.sm))
            Text(ssid ?: "Rede oculta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = c.textPrimary)
        }
        Spacer(Modifier.height(LkSpacing.xl))

        val bandaDetail =
            when {
                rede.frequenciaMhz < 3000 -> BandaWifi.ghz24
                else -> BandaWifi.ghz5
            }
        DetailRow(
            "Sinal",
            "${rede.rssiDbm} dBm — ${signalQuality(rede.rssiDbm, bandaDetail)}",
            valueColor = signalColor(rede.rssiDbm, bandaDetail),
        )
        HorizontalDivider(color = c.border, modifier = Modifier.padding(vertical = LkSpacing.sm))
        DetailRow("Banda", rede.banda)
        if (channel != null) {
            HorizontalDivider(color = c.border, modifier = Modifier.padding(vertical = LkSpacing.sm))
            DetailRow("Canal", channel.toString())
        }
        if (largura != null) {
            HorizontalDivider(color = c.border, modifier = Modifier.padding(vertical = LkSpacing.sm))
            DetailRow("Largura", "$largura MHz")
        }
        HorizontalDivider(color = c.border, modifier = Modifier.padding(vertical = LkSpacing.sm))
        DetailRow("Segurança", securityLabel(rede.seguranca))
        HorizontalDivider(color = c.border, modifier = Modifier.padding(vertical = LkSpacing.sm))
        DetailRow("BSSID", rede.bssid)
        Spacer(Modifier.height(LkSpacing.xxl))
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color? = null,
) {
    val c = LocalLkTokens.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        Text(
            value,
            color = valueColor ?: c.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.W500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─── Tab Canal ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CanalTab(
    redes: List<RedeVizinha>,
    connectedNetwork: RedeVizinha?,
    estado: EstadoScanWifi = EstadoScanWifi.concluido,
    erroMensagem: String? = null,
    onRefresh: () -> Unit = {},
    wifiLinkSnapshot: WifiLinkSnapshot? = null,
) {
    val c = LocalLkTokens.current
    val bandasDisponiveis = listOf("Todos", "2.4GHz", "5GHz", "6GHz")
    var selectedBanda by remember { mutableStateOf(connectedNetwork?.banda ?: "Todos") }
    var selectedCanal by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val bandaCounts =
        remember(redes) {
            mapOf(
                "2.4GHz" to redes.count { it.banda == "2.4GHz" },
                "5GHz" to redes.count { it.banda == "5GHz" },
                "6GHz" to redes.count { it.banda == "6GHz" },
            )
        }
    val redesBanda =
        remember(redes, selectedBanda) {
            if (selectedBanda == "Todos") redes else redes.filter { it.banda == selectedBanda }
        }
    val canalAtual = remember(connectedNetwork) { connectedNetwork?.canal }
    val espectro =
        remember(redesBanda, canalAtual, selectedBanda, connectedNetwork) {
            WifiChannelDiagnosticEngine.computarEspectro(
                redes =
                    redesBanda.map {
                        RedeWifiVizinha(
                            canal = it.canal,
                            rssiDbm = it.rssiDbm,
                            frequenciaMhz = it.frequenciaMhz,
                            ssid = it.ssid,
                            bssid = it.bssid,
                        )
                    },
                canalAtual = canalAtual,
                banda = selectedBanda,
                seuSSID = connectedNetwork?.ssid,
            )
        }
    val canalOrdenados =
        remember(espectro) {
            val dados = espectro.dadosPorCanal
            val recomendado = dados.filter { it.ehCanalRecomendado }
            val atual = dados.filter { it.ehCanalAtual && !it.ehCanalRecomendado }
            val resto =
                dados
                    .filter { !it.ehCanalAtual && !it.ehCanalRecomendado }
                    .sortedWith(compareBy<DadoCanal> { it.nivel.ordinal }.thenBy { it.count })
            recomendado + atual + resto
        }
    val context = LocalContext.current
    val textoExplicativo =
        remember(espectro) {
            CanalTextGenerator.gerarTexto(
                snapshot = espectro,
                strings =
                    CanalStrings(
                        bandaCongestionada = { banda -> context.getString(R.string.canal_banda_congestionada, banda) },
                        bandaQuaseVazia = { banda -> context.getString(R.string.canal_faixa_quase_vazia, banda) },
                        canalAtualCongestionado = {
                                canalAtual,
                                canalRec,
                            ->
                            context.getString(R.string.canal_atual_congestionado, canalAtual, canalRec)
                        },
                        canalRecomendadoLivre = { canal, banda -> context.getString(R.string.canal_recomendado_livre, canal, banda) },
                        canalRecomendadoModerado = { canal, banda -> context.getString(R.string.canal_recomendado_moderado, canal, banda) },
                        semDados = { context.getString(R.string.canal_sem_dados) },
                    ),
            )
        }

    // ── Band steering detection ───────────────────────────────────────────────
    val mostrarAlertaBandSteering =
        remember(wifiLinkSnapshot, connectedNetwork, redes) {
            val freqMhz = wifiLinkSnapshot?.frequenciaMhz
            if (freqMhz == null || freqMhz >= 3000) return@remember false
            // Conectado em 2.4 GHz — verificar se existe nó do mesmo SSID em 5 GHz
            val ssidAtual = connectedNetwork?.ssid ?: wifiLinkSnapshot.ssid
            ssidAtual != null &&
                redes.any { rede ->
                    rede.ssid == ssidAtual && rede.frequenciaMhz >= 5000
                }
        }

    if (estado == EstadoScanWifi.idle) {
        CanalIdleState(onRefresh = onRefresh)
        return
    }

    if (estado == EstadoScanWifi.erro) {
        CanalErroState(erroMensagem = erroMensagem, onRefresh = onRefresh)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LkSpacing.xl),
    ) {
        if (bandasDisponiveis.isNotEmpty()) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                ) {
                    bandasDisponiveis.forEach { banda ->
                        val n = if (banda == "Todos") redes.size else (bandaCounts[banda] ?: 0)
                        val active = selectedBanda == banda
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(if (active) LkColors.accent.copy(alpha = 0.12f) else c.bgSecondary)
                                    .clickable { selectedBanda = banda }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$banda ($n)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (active) FontWeight.W600 else FontWeight.W500,
                                color = if (active) LkColors.accent else c.textSecondary,
                            )
                        }
                    }
                }
            }
        }

        val canalAtualInfo = connectedNetwork?.canal
        val dadoCanalAtual = if (canalAtualInfo != null) espectro.dadosPorCanal.find { it.canal == canalAtualInfo } else null
        if (dadoCanalAtual?.nivel == NivelCongestionamento.congestionado) {
            item {
                CanalCongestionadoBanner(dadoCanal = dadoCanalAtual)
                Spacer(Modifier.height(LkSpacing.md))
            }
        }

        item {
            Column(Modifier.padding(horizontal = LkSpacing.lg)) {
                SectionLabel(if (selectedBanda == "Todos") "ESPECTRO" else "ESPECTRO $selectedBanda")
                Spacer(Modifier.height(LkSpacing.sm))
                SpectrumChart(
                    espectro = espectro,
                    redesRaw = redesBanda,
                    seuSSID = connectedNetwork?.ssid,
                )
                Spacer(Modifier.height(LkSpacing.lg))
            }
        }

        item {
            Text(
                textoExplicativo,
                modifier = Modifier.padding(horizontal = LkSpacing.lg),
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(LkSpacing.lg))
        }

        if (mostrarAlertaBandSteering) {
            item {
                BandSteeringCard()
                Spacer(Modifier.height(LkSpacing.lg))
            }
        }

        val canalAtualParaCard = espectro.canalAtual
        val canalRecParaCard = espectro.canalRecomendado
        val dadoCanalRec = espectro.dadosPorCanal.find { it.canal == canalRecParaCard }
        val nivelCanalRec = dadoCanalRec?.nivel
        if (canalAtualParaCard != null && (canalRecParaCard == null || canalRecParaCard == canalAtualParaCard)) {
            item {
                Row(
                    modifier =
                        Modifier
                            .padding(horizontal = LkSpacing.lg)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(LkRadius.card))
                            .background(LkColors.success.copy(alpha = 0.08f))
                            .padding(LkSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = LkColors.success,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(
                        "Você está no canal ideal — não é necessário mudar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LkColors.success,
                    )
                }
                Spacer(Modifier.height(LkSpacing.lg))
            }
        }
        if (canalAtualParaCard != null &&
            canalRecParaCard != null &&
            canalRecParaCard != canalAtualParaCard &&
            nivelCanalRec != NivelCongestionamento.congestionado
        ) {
            item {
                CanalRecomendadoCard(
                    canalAtual = canalAtualParaCard,
                    canalRecomendado = canalRecParaCard,
                    banda = espectro.banda,
                    nivelRecomendado = nivelCanalRec ?: NivelCongestionamento.livre,
                )
                Spacer(Modifier.height(LkSpacing.lg))
            }
        }

        if (canalOrdenados.isNotEmpty()) {
            item {
                SectionLabel(
                    "USO POR CANAL",
                    modifier = Modifier.padding(horizontal = LkSpacing.lg),
                )
                Spacer(Modifier.height(LkSpacing.sm))
            }
            items(canalOrdenados, key = { it.canal }) { dado ->
                ChannelItem(
                    dado = dado,
                    isConnected = dado.ehCanalAtual,
                    onClick = { selectedCanal = dado.canal },
                )
                HorizontalDivider(color = c.border, modifier = Modifier.padding(horizontal = LkSpacing.lg))
            }
        } else {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = LkSpacing.xl), contentAlignment = Alignment.Center) {
                    Text("Nenhum canal nesta faixa", color = c.textSecondary)
                }
            }
        }
    }

    val ch = selectedCanal
    if (ch != null) {
        val dadoCanal = espectro.dadosPorCanal.find { it.canal == ch }
        if (dadoCanal != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedCanal = null },
                sheetState = sheetState,
                containerColor = c.bgCard,
                dragHandle = {},
            ) {
                ChannelDetailSheet(dado = dadoCanal, connectedNetwork = connectedNetwork, espectro = espectro)
            }
        }
    }
}

// ─── Canal: banner de congestionamento ───────────────────────────────────────

@Composable
private fun CanalCongestionadoBanner(dadoCanal: DadoCanal) {
    val c = LocalLkTokens.current
    Row(
        modifier =
            Modifier
                .padding(horizontal = LkSpacing.lg)
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .border(1.dp, LkColors.warning.copy(alpha = 0.3f), RoundedCornerShape(LkRadius.card))
                .background(LkColors.warning.copy(alpha = 0.08f))
                .padding(LkSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            tint = LkColors.warning,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Canal congestionado",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = LkColors.warning,
            )
            Text(
                "${dadoCanal.countTerceiros} redes vizinhas dividem o canal ${dadoCanal.canal}.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
    }
}

// ─── Canal: estados especiais ─────────────────────────────────────────────────

@Composable
private fun CanalIdleState(onRefresh: () -> Unit) {
    val c = LocalLkTokens.current
    Box(
        modifier = Modifier.fillMaxSize().padding(LkSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = c.textTertiary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(LkSpacing.md))
            Text(
                stringResource(R.string.canal_idle_titulo),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                stringResource(R.string.canal_idle_descricao),
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(LkSpacing.lg))
            Button(onClick = onRefresh) {
                Text(stringResource(R.string.canal_idle_titulo))
            }
        }
    }
}

@Composable
private fun CanalErroState(
    erroMensagem: String?,
    onRefresh: () -> Unit,
) {
    val c = LocalLkTokens.current
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize().padding(LkSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.WifiFind,
                contentDescription = null,
                tint = LkColors.error,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(LkSpacing.md))
            when (erroMensagem) {
                "semPermissaoLocalizacao" -> {
                    Text(
                        stringResource(R.string.canal_erro_permissao_mensagem),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(LkSpacing.lg))
                    FilledTonalButton(onClick = {
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        context.startActivity(intent)
                    }) {
                        Text(stringResource(R.string.canal_erro_permissao_botao))
                    }
                }
                "erroScanWifi" -> {
                    Text(
                        stringResource(R.string.canal_erro_scan_mensagem),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(LkSpacing.lg))
                    Button(onClick = onRefresh) {
                        Text(stringResource(R.string.canal_erro_scan_botao))
                    }
                }
                else -> {
                    Text(
                        erroMensagem ?: stringResource(R.string.canal_erro_scan_mensagem),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(LkSpacing.lg))
                    Button(onClick = onRefresh) {
                        Text(stringResource(R.string.canal_erro_scan_botao))
                    }
                }
            }
        }
    }
}

// ─── Spectrum chart (Gaussian curves) ────────────────────────────────────────

private val SPECTRUM_COLORS =
    listOf(
        Color(0xFF4FC3F7),
        Color(0xFFAED581),
        Color(0xFFFFB74D),
        Color(0xFFBA68C8),
        Color(0xFFFF8A65),
        Color(0xFF4DB6AC),
        Color(0xFFE57373),
        Color(0xFFF06292),
    )

private data class RedeParaEspectro(
    val ssid: String,
    val canal: Int,
    val rssiDbm: Int,
    val cor: Color,
    val isSua: Boolean,
)

@Composable
private fun SpectrumChart(
    espectro: SnapshotEspectroCanal,
    redesRaw: List<RedeVizinha> = emptyList(),
    seuSSID: String? = null,
) {
    val c = LocalLkTokens.current
    val dados = espectro.dadosPorCanal
    val accentColor = LkColors.accent
    val gridColor = c.border.copy(alpha = 0.35f)
    val textTertiary = c.textTertiary
    val textMeasurer = rememberTextMeasurer()

    val redesParaDesenhar =
        remember(redesRaw, seuSSID) {
            redesRaw
                .filter { it.canal != null }
                .sortedByDescending { it.rssiDbm }
                .take(20)
                .mapIndexed { idx, rede ->
                    val isSua = seuSSID != null && rede.ssid == seuSSID
                    RedeParaEspectro(
                        ssid = rede.ssid ?: "Oculta",
                        canal = rede.canal!!,
                        rssiDbm = rede.rssiDbm,
                        cor = if (isSua) accentColor else SPECTRUM_COLORS[idx % SPECTRUM_COLORS.size],
                        isSua = isSua,
                    )
                }
        }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LkRadius.card))
            .background(c.bgSecondary)
            .padding(horizontal = LkSpacing.md, vertical = LkSpacing.md),
    ) {
        if (redesParaDesenhar.isEmpty() && dados.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Sem redes visíveis",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Normal,
                    color = c.textTertiary,
                )
            }
            return@Column
        }

        val chartAreaHeight = 140.dp
        val xAxisHeight = 20.dp
        val yAxisWidth = 30.dp

        val canais = remember(dados) { dados.map { it.canal }.sorted() }
        val canalMin = canais.firstOrNull() ?: 1
        val canalMax = canais.lastOrNull() ?: 13

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(chartAreaHeight + xAxisHeight),
        ) {
            val leftPx = yAxisWidth.toPx()
            val chartH = chartAreaHeight.toPx()
            val xAxisH = xAxisHeight.toPx()
            val chartW = size.width - leftPx

            val labelStyle = TextStyle(fontSize = 9.sp, color = textTertiary)

            listOf(-30 to "-30", -50 to "-50", -70 to "-70").forEach { (dBm, label) ->
                val frac = 1f - ((dBm + 90f) / 70f)
                val y = chartH * frac
                drawLine(gridColor, Offset(leftPx, y), Offset(size.width, y), strokeWidth = 0.5.dp.toPx())
                val textLayout = textMeasurer.measure(label, labelStyle)
                drawText(textLayout, topLeft = Offset(0f, y - textLayout.size.height / 2f))
            }

            val range = (canalMax - canalMin).coerceAtLeast(1).toFloat()

            fun canalToX(canal: Float): Float = leftPx + ((canal - canalMin + 1f) / (range + 2f)) * chartW

            val is24Ghz = canalMin <= 14
            val halfWidthChannels = if (is24Ghz) 2.5f else 4f
            val sigma = halfWidthChannels / 2.355f

            redesParaDesenhar.reversed().forEach { rede ->
                val centerX = canalToX(rede.canal.toFloat())
                val heightFraction = ((rede.rssiDbm + 90).coerceIn(0, 70)) / 70f

                val path = Path()
                val steps = 60
                val xSpread = halfWidthChannels * 2f
                val startCanal = rede.canal - xSpread
                val endCanal = rede.canal + xSpread
                val step = (endCanal - startCanal) / steps

                for (i in 0..steps) {
                    val canalPos = startCanal + i * step
                    val x = canalToX(canalPos)
                    val dist = (canalPos - rede.canal) / sigma
                    val gauss = kotlin.math.exp(-0.5 * dist * dist).toFloat()
                    val y = chartH - (chartH * heightFraction * gauss)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(path, color = rede.cor, style = Stroke(width = 2.dp.toPx()))

                val fillPath = Path()
                fillPath.addPath(path)
                fillPath.lineTo(canalToX(endCanal), chartH)
                fillPath.lineTo(canalToX(startCanal), chartH)
                fillPath.close()
                drawPath(fillPath, color = rede.cor.copy(alpha = 0.15f), style = Fill)
            }

            canais.forEach { canal ->
                val x = canalToX(canal.toFloat())
                val isAtual = canal == espectro.canalAtual
                val xLabelColor = if (isAtual) accentColor else textTertiary
                val xLabelWeight = if (isAtual) FontWeight.Bold else FontWeight.Normal
                val xLayout =
                    textMeasurer.measure(
                        "$canal",
                        TextStyle(fontSize = 9.sp, color = xLabelColor, fontWeight = xLabelWeight),
                    )
                drawText(
                    xLayout,
                    topLeft = Offset(x - xLayout.size.width / 2, chartH + (xAxisH - xLayout.size.height) / 2),
                )
            }
        }

        Spacer(Modifier.height(LkSpacing.sm))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            redesParaDesenhar.take(8).forEach { rede ->
                LegendaRedeItem(ssid = rede.ssid, cor = rede.cor, c = c)
            }
        }
    }
}

@Composable
private fun LegendaRedeItem(
    ssid: String,
    cor: Color,
    c: LkTokens,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(cor))
        Spacer(Modifier.width(4.dp))
        Text(
            ssid,
            style = MaterialTheme.typography.labelSmall,
            color = c.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ─── Band steering card ───────────────────────────────────────────────────────

@Composable
private fun BandSteeringCard() {
    val c = LocalLkTokens.current
    Row(
        modifier =
            Modifier
                .padding(horizontal = LkSpacing.lg)
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .border(1.dp, LkColors.warning.copy(alpha = 0.3f), RoundedCornerShape(LkRadius.card))
                .background(LkColors.warning.copy(alpha = 0.08f))
                .padding(LkSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(LkColors.warning.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Wifi,
                contentDescription = null,
                tint = LkColors.warning,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(LkSpacing.md))
        Column {
            Text(
                "Você pode estar mais rápido",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Seu aparelho está em 2,4 GHz. Seu roteador também tem 5 GHz, que é mais rápido e menos congestionado.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Para mudar, acesse as configurações do roteador.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary,
            )
        }
    }
}

// ─── Canal recomendado card ───────────────────────────────────────────────────

@Composable
private fun CanalRecomendadoCard(
    canalAtual: Int,
    canalRecomendado: Int,
    banda: String,
    nivelRecomendado: NivelCongestionamento = NivelCongestionamento.livre,
) {
    val c = LocalLkTokens.current
    val descricao =
        when (nivelRecomendado) {
            NivelCongestionamento.livre -> "Seu canal é o $canalAtual. Melhor mudar para o $canalRecomendado, que está livre agora."
            NivelCongestionamento.moderado -> "Seu canal é o $canalAtual. O canal $canalRecomendado tem menos interferência no momento."
            NivelCongestionamento.congestionado -> "Seu canal é o $canalAtual e está congestionado. Considere ativar o modo automático no roteador."
        }
    Row(
        modifier =
            Modifier
                .padding(horizontal = LkSpacing.lg)
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(LkColors.accent.copy(alpha = 0.08f))
                .padding(LkSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
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
                imageVector = Icons.Outlined.Wifi,
                contentDescription = null,
                tint = LkColors.accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(LkSpacing.md))
        Column {
            Text(
                "Troque de canal",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W600,
                color = LkColors.accent,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                descricao,
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
    }
}

// ─── Recommendation card ──────────────────────────────────────────────────────

@Composable
private fun RecommendationCard(
    channel: Int,
    reason: String,
    banda: String,
) {
    val c = LocalLkTokens.current
    Row(
        Modifier
            .padding(horizontal = LkSpacing.lg)
            .fillMaxWidth()
            .clip(RoundedCornerShape(LkRadius.card))
            .background(LkColors.success.copy(alpha = 0.1f))
            .padding(LkSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(LkColors.success.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(channel.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LkColors.success)
        }
        Spacer(Modifier.width(LkSpacing.md))
        Column {
            Text(
                "Canal recomendado · $banda",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Text(reason, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
    }
}

// ─── Channel item ──────────────────────────────────────────────────────────

@Composable
private fun ChannelItem(
    dado: DadoCanal,
    isConnected: Boolean,
    onClick: () -> Unit,
) {
    val c = LocalLkTokens.current
    val corStatus = congestionColor(dado.nivel)
    val labelStatus =
        when (dado.nivel) {
            NivelCongestionamento.livre -> "Livre"
            NivelCongestionamento.moderado -> "Moderado"
            NivelCongestionamento.congestionado -> "Congestionado"
        }
    val fracaoUso = (dado.count / 8f).coerceIn(0f, 1f)

    Row(
        Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = LkSpacing.lg, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Text(
            "Canal ${dado.canal}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isConnected) FontWeight.W700 else FontWeight.W500,
            color = if (isConnected) LkColors.accent else c.textPrimary,
            modifier = Modifier.widthIn(min = 60.dp),
        )
        if (isConnected) {
            InlineBadge("SEU CANAL", LkColors.accent)
        } else if (dado.ehCanalRecomendado) {
            InlineBadge("RECOMENDADO", LkColors.accent)
        }
        LinearProgressBar(
            fraction = fracaoUso,
            color = corStatus,
            modifier = Modifier.weight(1f),
        )
        Text(
            labelStatus,
            style = MaterialTheme.typography.labelSmall,
            color = corStatus,
            fontWeight = FontWeight.W600,
        )
    }
}

@Composable
private fun InlineBadge(
    label: String,
    color: Color,
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.W700, color = color)
    }
}

@Composable
private fun LinearProgressBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Box(
        modifier =
            modifier
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(c.bgSecondary),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
        )
    }
}

// ─── Empty states: conexão não-Wi-Fi ─────────────────────────────────────────

@Composable
private fun EmptyStatePermissaoTelefonia(
    onSolicitarPermissao: () -> Unit,
    tokens: LkTokens,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(LkColors.warning.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.SignalCellularAlt,
                contentDescription = null,
                tint = LkColors.warning,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(LkSpacing.xs))
        Text(
            "Permissão necessária",
            fontSize = 17.sp,
            fontWeight = FontWeight.W600,
            color = tokens.textPrimary,
        )
        Text(
            "Seu aparelho está sem permissão para ler\nas informações de rede móvel.",
            fontSize = 13.sp,
            color = tokens.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(LkSpacing.xs))
        OutlinedButton(onClick = onSolicitarPermissao) {
            Text("Permitir leitura do chip", fontSize = 12.sp, fontWeight = FontWeight.W600)
        }
    }
}

@Composable
private fun EmptyStateMobile(tokens: LkTokens) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(LkColors.warning.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.SignalCellularOff,
                contentDescription = null,
                tint = LkColors.warning,
                modifier = Modifier.size(36.dp),
            )
        }
        Spacer(Modifier.height(LkSpacing.xs))
        Text(
            "Sem chip detectado",
            fontSize = 17.sp,
            fontWeight = FontWeight.W600,
            color = tokens.textPrimary,
        )
        Text(
            "Seu aparelho está sem chip de celular ou sem\npermissão para ler as informações de rede móvel.",
            fontSize = 13.sp,
            color = tokens.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 19.sp,
        )
    }
}

@Composable
private fun EmptyStateCabo(
    localIp: String?,
    tokens: LkTokens,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Cable, null, tint = tokens.textTertiary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(LkSpacing.md))
        Text(
            "Conectado via cabo (Ethernet)",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.W600,
            color = tokens.textPrimary,
        )
        Spacer(Modifier.height(LkSpacing.sm))
        Text(
            "Análise Wi-Fi não aplicável para\nconexão cabeada.",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (localIp != null) {
            Spacer(Modifier.height(LkSpacing.sm))
            Text("IP: $localIp", style = MaterialTheme.typography.bodySmall, color = tokens.textTertiary)
        }
    }
}

@Composable
private fun EmptyStateDesconhecido(tokens: LkTokens) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Wifi, null, tint = tokens.textTertiary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(LkSpacing.md))
        Text("Sem conexão de rede", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.W600, color = tokens.textPrimary)
        Spacer(Modifier.height(LkSpacing.sm))
        Text(
            "Conecte-se a uma rede para\nvisualizar informações de sinal.",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

// ─── Channel detail sheet ──────────────────────────────────────────────────────

@Composable
private fun ChannelDetailSheet(
    dado: DadoCanal,
    connectedNetwork: RedeVizinha?,
    espectro: SnapshotEspectroCanal,
) {
    val c = LocalLkTokens.current
    val corCongestionamento = congestionColor(dado.nivel)
    val isCurrentChannel = dado.ehCanalAtual
    val isRecommended = dado.ehCanalRecomendado

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.lg),
    ) {
        Box(
            Modifier
                .width(40.dp)
                .height(4.dp)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(2.dp))
                .background(c.border),
        )
        Spacer(Modifier.height(LkSpacing.lg))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Canal ${dado.canal}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
            )
            Spacer(Modifier.width(LkSpacing.sm))
            if (isCurrentChannel) {
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LkColors.success.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("Seu canal", fontSize = 10.sp, fontWeight = FontWeight.W600, color = LkColors.success)
                }
            }
            if (isRecommended) {
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(LkColors.accent.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("Recomendado", fontSize = 10.sp, fontWeight = FontWeight.W600, color = LkColors.accent)
                }
            }
        }
        Spacer(Modifier.height(LkSpacing.lg))

        Text("Status", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W600, color = c.textPrimary)
        Spacer(Modifier.height(LkSpacing.sm))
        if (dado.countProprios > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(LkColors.accent),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Você (${dado.countProprios} nó${if (dado.countProprios != 1) "s" else ""} seu/seus)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Normal,
                    color = c.textPrimary,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        if (dado.countTerceiros > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(corCongestionamento),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${dado.countTerceiros} rede${if (dado.countTerceiros != 1) "s" else ""} de terceiros",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Normal,
                    color = c.textPrimary,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        if (dado.countProprios == 0 && dado.countTerceiros == 0) {
            Text(
                "Nenhuma rede neste canal",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Normal,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(LkSpacing.lg))
        HorizontalDivider(color = c.border)
        Spacer(Modifier.height(LkSpacing.lg))

        Text("Análise", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W600, color = c.textPrimary)
        Spacer(Modifier.height(LkSpacing.sm))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isCurrentChannel) {
                Row(verticalAlignment = Alignment.Top) {
                    Text("✓", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LkColors.success)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Você está usando este canal",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Normal,
                        color = c.textPrimary,
                    )
                }
            } else if (isRecommended) {
                Row(verticalAlignment = Alignment.Top) {
                    Text("✓", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LkColors.success)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "Recomendado para migração",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.W500,
                            color = c.textPrimary,
                        )
                        Text(
                            espectro.motivoRecomendacao ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary,
                        )
                    }
                }
            }

            when (dado.nivel) {
                NivelCongestionamento.livre -> {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("✓", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LkColors.success)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Canal livre — não há competição",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Normal,
                            color = c.textPrimary,
                        )
                    }
                }
                NivelCongestionamento.moderado -> {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("⚠", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LkColors.warning)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Moderado — ${dado.countTerceiros} rede${if (dado.countTerceiros != 1) "s" else ""} compartilhando",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Normal,
                            color = c.textPrimary,
                        )
                    }
                }
                NivelCongestionamento.congestionado -> {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("✗", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LkColors.error)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Congestionado — ${dado.countTerceiros} redes em competição",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Normal,
                            color = c.textPrimary,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(LkSpacing.lg))
        HorizontalDivider(color = c.border)
        Spacer(Modifier.height(LkSpacing.lg))

        Text("Detalhes Técnicos", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W600, color = c.textPrimary)
        Spacer(Modifier.height(LkSpacing.md))
        DetailRow("Banda", espectro.banda)
        HorizontalDivider(color = c.border, modifier = Modifier.padding(vertical = LkSpacing.sm))
        DetailRow("Sinal Máximo", "${dado.maxRssiDbm ?: "—"} dBm")

        if (isCurrentChannel || isRecommended) {
            Spacer(Modifier.height(LkSpacing.lg))
            HorizontalDivider(color = c.border)
            Spacer(Modifier.height(LkSpacing.lg))
            WifiChannelGuide()
        }

        Spacer(Modifier.height(LkSpacing.xxl))
    }
}

// ─── Banners inline ───────────────────────────────────────────────────────────

@Composable
private fun LocPermissaoBanner(onClick: () -> Unit) {
    val c = LocalLkTokens.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(LkColors.accent.copy(alpha = 0.08f))
                .clickable(onClick = onClick)
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm)
                .minimumInteractiveComponentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.WifiFind,
            contentDescription = null,
            tint = LkColors.accent,
            modifier = Modifier.size(18.dp),
        )
        Text(
            "Permissão de localização necessária para escanear redes",
            style = MaterialTheme.typography.bodySmall,
            color = LkColors.accent,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MovelSemPermissaoBanner(
    onClick: () -> Unit,
    tokens: LkTokens,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(LkColors.accent.copy(alpha = 0.08f))
                .clickable(onClick = onClick)
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm)
                .minimumInteractiveComponentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.SignalCellularAlt,
            contentDescription = null,
            tint = LkColors.accent,
            modifier = Modifier.size(18.dp),
        )
        Text(
            "Toque para ver qualidade do sinal móvel",
            style = MaterialTheme.typography.bodySmall,
            color = LkColors.accent,
            modifier = Modifier.weight(1f),
        )
    }
}
