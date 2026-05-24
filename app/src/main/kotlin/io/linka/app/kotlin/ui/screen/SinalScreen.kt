package io.linka.app.kotlin.ui.screen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.linka.app.kotlin.R
import io.linka.app.kotlin.core.network.EstadoConexao
import io.linka.app.kotlin.core.network.WifiLinkSnapshot
import io.linka.app.kotlin.core.telephony.MovelSnapshot
import io.linka.app.kotlin.feature.diagnostico.BandaWifi
import io.linka.app.kotlin.feature.diagnostico.CanalStrings
import io.linka.app.kotlin.feature.diagnostico.CanalTextGenerator
import io.linka.app.kotlin.feature.diagnostico.DadoCanal
import io.linka.app.kotlin.feature.diagnostico.NivelCongestionamento
import io.linka.app.kotlin.feature.diagnostico.RedeWifiVizinha
import io.linka.app.kotlin.feature.diagnostico.SnapshotEspectroCanal
import io.linka.app.kotlin.feature.diagnostico.WifiChannelDiagnosticEngine
import io.linka.app.kotlin.feature.wifi.ConfiancaTopologia
import io.linka.app.kotlin.feature.wifi.EstadoScanWifi
import io.linka.app.kotlin.feature.wifi.GrupoRedeWifi
import io.linka.app.kotlin.feature.wifi.RedeClassificada
import io.linka.app.kotlin.feature.wifi.RedeVizinha
import io.linka.app.kotlin.feature.wifi.SegurancaWifi
import io.linka.app.kotlin.feature.wifi.SnapshotScanWifi
import io.linka.app.kotlin.feature.wifi.TipoTopologia
import io.linka.app.kotlin.feature.wifi.TopologiaWifiEngine
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkRadius
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LkTokens
import io.linka.app.kotlin.ui.LocalLkTokens
import io.linka.app.kotlin.ui.component.OfflineBanner
import io.linka.app.kotlin.ui.component.ProfileAvatarButton
import io.linka.app.kotlin.ui.component.WifiChannelGuide

// ─── Helpers ──────────────────────────────────────────────────────────────────

private data class TopologiaIconData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val cor: Color,
)

private fun TipoTopologia.toIconData(): TopologiaIconData? =
    when (this) {
        TipoTopologia.ROTEADOR -> TopologiaIconData(Icons.Outlined.Router, Color(0xFF9CA3AF)) // cinza neutro
        TipoTopologia.ROTEADOR_MESH -> TopologiaIconData(Icons.Outlined.Hub, LkColors.accent)
        TipoTopologia.NO_MESH -> TopologiaIconData(Icons.Outlined.Hub, LkColors.accent)
        TipoTopologia.REPETIDOR -> TopologiaIconData(Icons.Outlined.CellTower, LkColors.warning)
        TipoTopologia.PONTO_DE_ACESSO -> TopologiaIconData(Icons.Outlined.Lan, Color(0xFF9CA3AF)) // cinza neutro
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
    localIp: String? = null,
    temPermissaoTelefonia: Boolean = false,
    onSolicitarPermissaoTelefonia: () -> Unit = {},
    temPermissaoLocalizacao: Boolean = true,
    onSolicitarPermissaoLocalizacao: () -> Unit = {},
    onRefresh: () -> Unit,
    onVoltar: () -> Unit,
    nomeUsuario: String = "",
    fotoUri: String? = null,
    onAbrirPerfil: () -> Unit = {},
    wifiLinkSnapshot: WifiLinkSnapshot? = null,
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
            val tituloTopBar =
                when (conexaoTipo) {
                    ConexaoTipo.MOBILE -> "Sinal Móvel"
                    ConexaoTipo.WIFI -> "Redes Wi-Fi"
                    else -> "Sinal"
                }
            val iconeTopBar =
                when (conexaoTipo) {
                    ConexaoTipo.MOBILE -> Icons.Outlined.SignalCellularAlt
                    else -> Icons.Outlined.Wifi
                }
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = iconeTopBar,
                            contentDescription =
                                if (conexaoTipo ==
                                    ConexaoTipo.MOBILE
                                ) {
                                    "Sinal móvel"
                                } else {
                                    "Sinal Wi-Fi"
                                },
                            tint = c.textPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text(tituloTopBar, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W600, color = c.textPrimary)
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
        if (conexaoTipo != ConexaoTipo.WIFI) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                if (!conectado) OfflineBanner()
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = LkSpacing.xl),
                    ) {
                        when (conexaoTipo) {
                            ConexaoTipo.MOBILE -> {
                                if (movelSnapshot != null) {
                                    MobileSignalCard(
                                        snapshot = movelSnapshot,
                                        localIp = localIp,
                                        temPermissao = temPermissaoTelefonia,
                                        onSolicitarPermissao = onSolicitarPermissaoTelefonia,
                                        tokens = c,
                                    )
                                } else if (!temPermissaoTelefonia && !telefoniaSheetDismissed) {
                                    MovelSemPermissaoBanner(
                                        onClick = { showTelefoniaSheet = true },
                                        tokens = c,
                                    )
                                } else if (!temPermissaoTelefonia) {
                                    EmptyStatePermissaoTelefonia(
                                        onSolicitarPermissao = onSolicitarPermissaoTelefonia,
                                        tokens = c,
                                    )
                                } else {
                                    EmptyStateMobile(c)
                                }
                            }
                            ConexaoTipo.CABO -> EmptyStateCabo(localIp, c)
                            else -> EmptyStateDesconhecido(c)
                        }
                    }
                }
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!conectado) OfflineBanner()
            if (!temPermissaoLocalizacao && !localizacaoSheetDismissed) {
                LocPermissaoBanner(onClick = { showLocalizacaoSheet = true })
            }

            // Calcular congestionamento do canal atual para badge na tab Canal
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
                listOf("Redes", "Canal").forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
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
            when (selectedTab) {
                0 ->
                    RedesTab(
                        snapshotWifi = snapshotWifi,
                        connectedNetwork = connectedNetwork,
                        onRefresh = onRefresh,
                        wifiLinkSnapshot = wifiLinkSnapshot,
                    )
                else ->
                    CanalTab(
                        redes = snapshotWifi.redes,
                        connectedNetwork = connectedNetwork,
                        estado = snapshotWifi.estado,
                        erroMensagem = snapshotWifi.erroMensagem,
                        onRefresh = onRefresh,
                        wifiLinkSnapshot = wifiLinkSnapshot,
                    )
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
            val classificadas =
                TopologiaWifiEngine.classificar(
                    redes = snapshotWifi.redes,
                    connectedBssid = connectedNetwork?.bssid,
                )
            classificadas.associate { it.rede.bssid to it.tipo }
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

            // Classificar cada rede com TopologiaWifiEngine
            val classificadas =
                filtered.map { rede ->
                    RedeClassificada(
                        rede = rede,
                        tipo = TipoTopologia.DESCONHECIDO, // TODO: integrar com TopologiaWifiEngine se necessário
                        confianca = ConfiancaTopologia.BAIXA,
                        motivo = "",
                    )
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
                    SectionLabel("MINHA REDE", modifier = Modifier.padding(horizontal = LkSpacing.lg))
                    Spacer(Modifier.height(LkSpacing.sm))
                    GrupoRedeTree(
                        ssid = connectedNetwork.ssid ?: "Rede oculta",
                        nos = grupoNos,
                        connectedBssid = connectedNetwork.bssid,
                        onNoClick = { selectedNetwork = it },
                        modifier = Modifier.padding(horizontal = LkSpacing.lg),
                        wifiLinkSnapshot = wifiLinkSnapshot,
                        topologiaPorBssid = topologiaPorBssid,
                    )
                    Spacer(Modifier.height(LkSpacing.lg))
                }
            }

            if (otherClassificadas.isNotEmpty()) {
                item {
                    SectionLabel(
                        if (showConnected) "OUTRAS REDES" else "REDES DISPONÍVEIS",
                        modifier = Modifier.padding(horizontal = LkSpacing.lg),
                    )
                    Spacer(Modifier.height(LkSpacing.sm))
                }
                items(otherClassificadas, key = { it.ssid }) { grupo ->
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
    Text(text, modifier = modifier, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.W600, color = c.textTertiary, letterSpacing = 0.8.sp)
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
                imageVector = Icons.Outlined.Wifi,
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
                    Text(signalQuality(rede.rssiDbm, bandaVizinha), style = MaterialTheme.typography.bodySmall, color = signalColor(rede.rssiDbm, bandaVizinha))
                }
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
                    Text(rede.banda, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                }
                Spacer(Modifier.width(LkSpacing.sm))
                Icon(
                    imageVector = if (rede.seguranca == SegurancaWifi.aberta) Icons.Filled.LockOpen else Icons.Filled.Lock,
                    contentDescription = null,
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
                        .clickable { onToggleExpanded() }
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
            contentDescription = null,
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
        DetailRow("Sinal", "${rede.rssiDbm} dBm — ${signalQuality(rede.rssiDbm, bandaDetail)}", valueColor = signalColor(rede.rssiDbm, bandaDetail))
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
    val bandasDisponiveis = listOf("2.4GHz", "5GHz", "6GHz")
    var selectedBanda by remember { mutableStateOf(connectedNetwork?.banda ?: "2.4GHz") }
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
    val redesBanda = remember(redes, selectedBanda) { redes.filter { it.banda == selectedBanda } }
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
                        canalAtualCongestionado = { canalAtual, canalRec -> context.getString(R.string.canal_atual_congestionado, canalAtual, canalRec) },
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
                        val n = bandaCounts[banda] ?: 0
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
        val bandaAtualInfo = connectedNetwork?.banda
        val dadoCanalAtual = if (canalAtualInfo != null) espectro.dadosPorCanal.find { it.canal == canalAtualInfo } else null
        if (canalAtualInfo != null) {
            item {
                Row(
                    modifier =
                        Modifier
                            .padding(horizontal = LkSpacing.lg)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(LkRadius.card))
                            .background(c.bgCard)
                            .padding(LkSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Canal atual",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textTertiary,
                        )
                        Text(
                            "Canal $canalAtualInfo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.W700,
                            color = c.textPrimary,
                        )
                        if (bandaAtualInfo != null) {
                            Text(
                                bandaAtualInfo,
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary,
                            )
                        }
                    }
                    if (dadoCanalAtual != null) {
                        val chipColor = congestionColor(dadoCanalAtual.nivel)
                        val chipLabel =
                            when (dadoCanalAtual.nivel) {
                                NivelCongestionamento.livre -> "Livre"
                                NivelCongestionamento.moderado -> "Moderado"
                                NivelCongestionamento.congestionado -> "Congestionado"
                            }
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(chipColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                chipLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.W600,
                                color = chipColor,
                                maxLines = 1,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(LkSpacing.md))
            }
        }

        item {
            Column(Modifier.padding(horizontal = LkSpacing.lg)) {
                SectionLabel("ESPECTRO $selectedBanda")
                Spacer(Modifier.height(LkSpacing.sm))
                SpectrumChart(espectro = espectro)
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
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = LkSpacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("CANAL", style = MaterialTheme.typography.labelMedium, color = c.textTertiary, fontWeight = FontWeight.W600, letterSpacing = 0.8.sp)
                    Text(
                        "REDES / SINAL",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.textTertiary,
                        fontWeight = FontWeight.W600,
                        letterSpacing = 0.8.sp,
                    )
                }
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

// ─── Spectrum chart ───────────────────────────────────────────────────────────

@Composable
private fun SpectrumChart(espectro: SnapshotEspectroCanal) {
    val c = LocalLkTokens.current
    val dados = espectro.dadosPorCanal

    val accentColor = LkColors.accent
    val successColor = LkColors.success
    val warningColor = LkColors.warning
    val errorColor = LkColors.error
    val gridColor = c.border.copy(alpha = 0.35f)
    val textTertiary = c.textTertiary

    val textMeasurer = rememberTextMeasurer()

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LkRadius.card))
            .background(c.bgSecondary)
            .padding(horizontal = LkSpacing.md, vertical = LkSpacing.md),
    ) {
        // Legenda
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            LegendaItem(label = "Livre", cor = successColor, c = c)
            LegendaItem(label = "Moderado", cor = warningColor, c = c)
            LegendaItem(label = "Congestionado", cor = errorColor, c = c)
        }

        Spacer(Modifier.height(LkSpacing.sm))

        if (dados.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                Text("Sem dados", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Normal, color = c.textTertiary)
            }
            return@Column
        }

        // Canvas principal: barras + gridlines + labels Y e X
        val barAreaHeight = 130.dp
        val xAxisHeight = 20.dp
        val yAxisWidth = 30.dp

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(barAreaHeight + xAxisHeight),
        ) {
            val leftPx = yAxisWidth.toPx()
            val barAreaH = barAreaHeight.toPx()
            val xAxisH = xAxisHeight.toPx()
            val chartW = size.width - leftPx

            val labelStyle = TextStyle(fontSize = 9.sp, color = textTertiary)

            // Gridlines e labels Y (-30, -50, -70 dBm)
            listOf(-30 to "-30", -50 to "-50", -70 to "-70").forEach { (dBm, label) ->
                val frac = 1f - ((dBm + 90f) / 70f)
                val y = barAreaH * frac

                drawLine(gridColor, Offset(leftPx, y), Offset(size.width, y), strokeWidth = 0.5.dp.toPx())

                val textLayout = textMeasurer.measure(label, labelStyle)
                drawText(textLayout, topLeft = Offset(0f, y - textLayout.size.height / 2f))
            }

            // Barras
            val n = dados.size
            val gap = if (n > 1) 2.dp.toPx() else 0f
            val barW = ((chartW - gap * (n - 1)) / n).coerceAtLeast(4f)

            dados.forEachIndexed { idx, dado ->
                val barX = leftPx + idx * (barW + gap)

                val barColor =
                    when {
                        dado.ehCanalAtual -> accentColor
                        else -> congestionColor(dado.nivel)
                    }

                val rssiDbm = dado.maxRssiDbm
                if (rssiDbm != null) {
                    val fraction = ((rssiDbm + 90).coerceIn(0, 70)) / 70f
                    val barH = (barAreaH * fraction).coerceAtLeast(4.dp.toPx())
                    val barTop = barAreaH - barH

                    drawRect(barColor.copy(alpha = 0.85f), topLeft = Offset(barX, barTop), size = Size(barW, barH))

                    // Borda verde no canal recomendado (quando não é o atual)
                    if (dado.ehCanalRecomendado && !dado.ehCanalAtual) {
                        drawRect(
                            successColor,
                            topLeft = Offset(barX - 1f, barTop - 1f),
                            size = Size(barW + 2f, barH + 2f),
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                    }

                    // Contagem de redes acima da barra
                    if (dado.count > 0) {
                        val countLayout =
                            textMeasurer.measure(
                                "${dado.count}",
                                TextStyle(fontSize = 8.sp, color = barColor, fontWeight = FontWeight.W600),
                            )
                        val countX = barX + barW / 2 - countLayout.size.width / 2
                        val countY = barTop - countLayout.size.height - 2.dp.toPx()
                        if (countY >= 0) drawText(countLayout, topLeft = Offset(countX, countY))
                    }
                } else {
                    drawRect(
                        c.textTertiary.copy(alpha = 0.2f),
                        topLeft = Offset(barX, barAreaH - 2.dp.toPx()),
                        size = Size(barW, 2.dp.toPx()),
                    )
                }

                // Label do canal no eixo X
                val xLabelColor = if (dado.ehCanalAtual) accentColor else textTertiary
                val xLabelWeight = if (dado.ehCanalAtual) FontWeight.Bold else FontWeight.Normal
                val xLayout =
                    textMeasurer.measure(
                        "${dado.canal}",
                        TextStyle(fontSize = 9.sp, color = xLabelColor, fontWeight = xLabelWeight),
                    )
                drawText(
                    xLayout,
                    topLeft =
                        Offset(
                            barX + barW / 2 - xLayout.size.width / 2,
                            barAreaH + (xAxisH - xLayout.size.height) / 2,
                        ),
                )
            }
        }
    }
}

@Composable
private fun LegendaItem(
    label: String,
    cor: Color,
    c: LkTokens,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(cor))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.textSecondary)
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
            Text("Canal recomendado · $banda", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W600, color = c.textPrimary)
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
    val corFundo =
        when {
            isConnected -> LkColors.accent.copy(alpha = 0.08f)
            else -> Color.Transparent
        }
    val corCongestionamento = congestionColor(dado.nivel)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(corFundo)
            .then(
                if (isConnected) {
                    Modifier.border(
                        width = 2.dp,
                        color = LkColors.accent.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                    )
                } else {
                    Modifier
                },
            ).minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .padding(horizontal = LkSpacing.lg, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Canal ${dado.canal}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W600,
                    color = if (isConnected) LkColors.accent else c.textPrimary,
                )
                if (isConnected) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(LkColors.success.copy(alpha = 0.14f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("Você está aqui", fontSize = 10.sp, fontWeight = FontWeight.W600, color = LkColors.success)
                    }
                }
            }
            if (dado.countProprios > 0 || dado.countTerceiros > 0) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (dado.countProprios > 0) {
                        Text(
                            "Seus: ${dado.countProprios}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.W500,
                            color = LkColors.accent,
                        )
                    }
                    if (dado.countProprios > 0 && dado.countTerceiros > 0) {
                        Text("·", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                    }
                    if (dado.countTerceiros > 0) {
                        Text(
                            "Outros: ${dado.countTerceiros}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.W500,
                            color = corCongestionamento,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(LkSpacing.md))
        Column(horizontalAlignment = Alignment.End) {
            if (dado.maxRssiDbm != null) {
                Text("${dado.maxRssiDbm} dBm", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W600, color = c.textPrimary)
            }
            Text(
                when (dado.nivel) {
                    NivelCongestionamento.livre -> "Livre"
                    NivelCongestionamento.moderado -> "Moderado"
                    NivelCongestionamento.congestionado -> "Congestionado"
                },
                style = MaterialTheme.typography.bodySmall,
                color = corCongestionamento,
            )
        }
    }
}

// ─── Empty states: conexão não-Wi-Fi ─────────────────────────────────────────

@Composable
private fun MobileSignalCard(
    snapshot: MovelSnapshot,
    localIp: String?,
    temPermissao: Boolean,
    onSolicitarPermissao: () -> Unit,
    tokens: LkTokens,
) {
    val rsrp = snapshot.rsrpDbm

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        // ── Seção 1 — Header ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    snapshot.operadora ?: "Rede móvel",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.W700,
                    color = tokens.textPrimary,
                )
                if (localIp != null) {
                    Text(
                        "IP: $localIp",
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textTertiary,
                    )
                }
            }
            if (snapshot.tecnologia != null) {
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(LkColors.accent.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        snapshot.tecnologia ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.W700,
                        color = LkColors.accent,
                    )
                }
            }
        }

        // ── Permissão ausente ou sem RSRP ─────────────────────────────────────
        if (!temPermissao || rsrp == null) {
            EmptyStatePermissaoTelefonia(onSolicitarPermissao, tokens)
            return@Column
        }

        // ── Lógica de qualidade ────────────────────────────────────────────────
        val (qualidadeLabel, qualidadeCor) =
            when {
                rsrp >= -80 -> "Excelente" to LkColors.success
                rsrp >= -90 -> "Bom" to LkColors.success
                rsrp >= -100 -> "Regular" to LkColors.warning
                else -> "Fraco" to LkColors.error
            }

        // ── Seção 2 — Gauge semicircular ──────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(200.dp, 110.dp)) {
                val strokeWidth = 16.dp.toPx()
                val halfStroke = strokeWidth / 2f
                val arcRect =
                    androidx.compose.ui.geometry.Rect(
                        left = halfStroke,
                        top = halfStroke,
                        right = size.width - halfStroke,
                        bottom = size.height * 2 - halfStroke,
                    )
                // Fundo do arco
                drawArc(
                    color = tokens.border.copy(alpha = 0.3f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = arcRect.topLeft,
                    size = arcRect.size,
                    style =
                        androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        ),
                )
                // Frente do arco — progresso
                val progresso = ((rsrp - (-110f)) / ((-70f) - (-110f))).coerceIn(0f, 1f)
                val sweepProgresso = progresso * 180f
                if (sweepProgresso > 0f) {
                    drawArc(
                        color = qualidadeCor,
                        startAngle = 180f,
                        sweepAngle = sweepProgresso,
                        useCenter = false,
                        topLeft = arcRect.topLeft,
                        size = arcRect.size,
                        style =
                            androidx.compose.ui.graphics.drawscope.Stroke(
                                width = strokeWidth,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            ),
                    )
                }
            }
            // Labels sobrepostos abaixo do centro
            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    qualidadeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W700,
                    color = qualidadeCor,
                )
                Text(
                    "$rsrp dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textTertiary,
                )
            }
        }

        // ── Seção 3 — Força e Estabilidade ────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Força do sinal",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textTertiary,
                )
                Text(
                    qualidadeLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.W700,
                    color = qualidadeCor,
                )
            }
            Box(
                modifier =
                    Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(tokens.border),
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Estabilidade",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textTertiary,
                )
                val rsrq = snapshot.rsrqDb
                val sinr = snapshot.sinrDb
                val rsrqBom = rsrq != null && rsrq >= -10
                val rsrqRuim = rsrq != null && rsrq < -15
                val sinrBom = sinr != null && sinr >= 10
                val sinrRuim = sinr != null && sinr < 0
                val (estLabel, estCor) =
                    when {
                        rsrq == null && sinr == null -> "—" to tokens.textTertiary
                        rsrqRuim || sinrRuim -> "Instável" to LkColors.error
                        (rsrqBom || sinrBom) && !rsrqRuim && !sinrRuim -> "Estável" to LkColors.success
                        else -> "Moderada" to LkColors.warning
                    }
                Text(
                    estLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.W700,
                    color = estCor,
                )
            }
        }

        // ── Seção 4 — Card de diagnóstico ─────────────────────────────────────
        val (diagIcone, diagCor, diagCausa, diagAcao) =
            when {
                rsrp >= -80 ->
                    DiagData(
                        Icons.Outlined.CheckCircle,
                        LkColors.success,
                        "Sinal ótimo",
                        "Você está próximo de uma torre. Ideal para streaming e videochamadas.",
                    )
                rsrp >= -90 ->
                    DiagData(
                        Icons.Outlined.CheckCircle,
                        LkColors.success,
                        "Sinal bom",
                        "Conexão estável para a maioria das atividades.",
                    )
                rsrp >= -100 ->
                    DiagData(
                        Icons.Outlined.Warning,
                        LkColors.warning,
                        "Sinal moderado — pode lentidão em picos",
                        "Tente ir para área aberta ou próximo de uma janela.",
                    )
                else ->
                    DiagData(
                        Icons.Outlined.SignalCellularOff,
                        LkColors.error,
                        "Sinal fraco — cobertura limitada",
                        "Mova-se para um local com mais espaço aberto ou ative o Wi-Fi Calling.",
                    )
            }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(tokens.bgSecondary)
                    .padding(LkSpacing.lg),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = diagIcone,
                contentDescription = null,
                tint = diagCor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(LkSpacing.sm))
            Column {
                Text(
                    diagCausa,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.W700,
                    color = tokens.textPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    diagAcao,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textSecondary,
                )
            }
        }
    }
}

private data class DiagData(
    val icone: androidx.compose.ui.graphics.vector.ImageVector,
    val cor: Color,
    val causa: String,
    val acao: String,
)

@Composable
private fun EmptyStatePermissaoTelefonia(
    onSolicitarPermissao: () -> Unit,
    tokens: LkTokens,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Icon(
            imageVector = Icons.Outlined.SignalCellularAlt,
            contentDescription = null,
            tint = tokens.textTertiary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            "Conectado via rede móvel",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.W600,
            color = tokens.textPrimary,
        )
        Text(
            "Para ver informações de sinal (RSRP, RSRQ),\nconceda permissão de leitura telefônica.",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(LkSpacing.xs))
        FilledTonalButton(onClick = onSolicitarPermissao) {
            Text("Conceder permissão")
        }
    }
}

@Composable
private fun EmptyStateMobile(tokens: LkTokens) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.SignalCellularAlt, null, tint = tokens.textTertiary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(LkSpacing.md))
        Text("Conectado via rede móvel", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.W600, color = tokens.textPrimary)
        Spacer(Modifier.height(LkSpacing.sm))
        Text(
            "Análise de canais Wi-Fi não disponível\nna rede móvel.",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
        Text("Conectado via cabo (Ethernet)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.W600, color = tokens.textPrimary)
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
            Text("Canal ${dado.canal}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = c.textPrimary)
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
            Text("Nenhuma rede neste canal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Normal, color = c.textSecondary)
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
