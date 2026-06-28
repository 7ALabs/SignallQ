package io.signallq.app.ui.screen

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.SettingsInputAntenna
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import io.signallq.app.R
import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.core.telephony.MovelSimSnapshot
import io.signallq.app.core.telephony.MovelSnapshot
import io.signallq.app.feature.speedtest.GargaloPrimario
import io.signallq.app.feature.speedtest.ModoSpeedtest
import io.signallq.app.feature.speedtest.ResultadoSpeedtest
import io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest
import io.signallq.app.feature.speedtest.VereditoUso
import io.signallq.app.feature.wifi.RedeVizinha
import io.signallq.app.feature.wifi.SegurancaWifi
import io.signallq.app.ui.ConnectionNodeType
import io.signallq.app.ui.GatewayInfo
import io.signallq.app.ui.HistoryPoint
import io.signallq.app.ui.IspInfo
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.ProfileAvatarButton
import io.signallq.app.ui.component.rememberTopBarAlpha
import kotlin.math.roundToInt

// ─── TopBar subtitle (#180) ───────────────────────────────────────────────────

private fun topBarSubtitulo(
    snapshotRede: SnapshotRede,
    movelSnapshot: MovelSnapshot?,
): AnnotatedString =
    buildAnnotatedString {
        when (snapshotRede.estadoConexao) {
            EstadoConexao.wifi -> {
                val ssid = snapshotRede.wifiLinkSnapshot?.ssid?.ifBlank { null } ?: "Wi-Fi"
                append("Você está conectado em ")
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(ssid)
                pop()
            }
            EstadoConexao.movel -> {
                val operadora = movelSnapshot?.operadora?.ifBlank { null } ?: "Operadora"
                append("Você está conectado com ")
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(operadora)
                pop()
            }
            else -> append("Sem conexão ativa")
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    snapshotRede: SnapshotRede,
    snapshotSpeedtest: SnapshotExecucaoSpeedtest,
    history: List<HistoryPoint>,
    ultimaMedicao: MedicaoEntity?,
    localIp: String?,
    publicIp: String?,
    ispInfo: IspInfo?,
    isIspInfoLoading: Boolean = true,
    gateways: List<GatewayInfo>,
    deviceName: String,
    nomeUsuario: String,
    fotoUriUsuario: String?,
    connectedNetwork: RedeVizinha?,
    movelSnapshot: MovelSnapshot?,
    simsAtivos: List<MovelSimSnapshot>,
    anatelBannerDismissed: Boolean,
    onDismissAnatelBanner: () -> Unit,
    onAbrirDns: () -> Unit,
    onAbrirPing: () -> Unit,
    onIniciarTeste: (ModoSpeedtest) -> Unit,
    onAbrirUltimoResultado: () -> Unit,
    onAbrirHistorico: () -> Unit,
    onAbrirPerfil: () -> Unit,
    onAbrirRedes: () -> Unit,
    onAbrirDiagnostico: () -> Unit,
    snapshotDispositivos: io.signallq.app.feature.devices.SnapshotScanDispositivos? = null,
    onAbrirDispositivos: () -> Unit = {},
) {
    val c = LocalLkTokens.current
    val context = LocalContext.current
    val fotoBitmap =
        remember(fotoUriUsuario) {
            fotoUriUsuario?.let { uriStr ->
                runCatching {
                    context.contentResolver
                        .openInputStream(uriStr.toUri())
                        ?.use { stream -> BitmapFactory.decodeStream(stream)?.asImageBitmap() }
                }.getOrNull()
            }
        }
    val lastResult = snapshotSpeedtest.resultado
    val lastHistoryPoint = remember(history) { history.firstOrNull() }
    val effectiveDl = remember(lastResult, lastHistoryPoint) { lastResult?.downloadMbps ?: lastHistoryPoint?.downloadMbps }
    val effectiveUl = remember(lastResult, lastHistoryPoint) { lastResult?.uploadMbps ?: lastHistoryPoint?.uploadMbps }
    val effectiveTs = remember(lastResult, lastHistoryPoint) { lastResult?.timestampEpochMs ?: lastHistoryPoint?.timestampEpochMs }
    val hasEffectiveResult =
        remember(effectiveDl, effectiveUl, effectiveTs) {
            effectiveDl != null &&
                effectiveUl != null &&
                effectiveTs != null
        }
    val isOnWifi = snapshotRede.estadoConexao == EstadoConexao.wifi
    val ssid = snapshotRede.wifiLinkSnapshot?.ssid
    val linkSpeedMbps = snapshotRede.wifiLinkSnapshot?.linkSpeedMbps

    val lat = remember(lastResult, ultimaMedicao) { lastResult?.latenciaMs ?: ultimaMedicao?.latencyMs }
    val jit = remember(lastResult, ultimaMedicao) { lastResult?.jitterMs ?: ultimaMedicao?.jitterMs }
    val loss = remember(lastResult, ultimaMedicao) { lastResult?.perdaPercentual ?: ultimaMedicao?.perdaPercentual }
    val vereditoGamer: VereditoUso? =
        remember(lat, jit, loss, lastResult) {
            if (lat != null && jit != null && loss != null) {
                lastResult?.diagnosticoQualidade?.vereditoGamer
                    ?: when {
                        lat <= 50 && jit <= 15 && loss <= 0.5 -> VereditoUso.good
                        lat <= 100 && jit <= 30 && loss <= 1.5 -> VereditoUso.acceptable
                        else -> VereditoUso.poor
                    }
            } else {
                null
            }
        }

    var showDeviceSheet by remember { mutableStateOf(false) }
    var showGatewaySheet by remember { mutableStateOf<GatewayInfo?>(null) }
    var showInternetSheet by remember { mutableStateOf(false) }
    var showCellularSheet by remember { mutableStateOf(false) }
    var showMedicaoTipoSheet by remember { mutableStateOf(false) }

    if (showDeviceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDeviceSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.bgSecondary,
        ) {
            DeviceInfoSheet(localIp = localIp, isMobile = !isOnWifi, deviceName = deviceName, connectedNetwork = connectedNetwork, c = c)
        }
    }
    showGatewaySheet?.let { gw ->
        ModalBottomSheet(
            onDismissRequest = { showGatewaySheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.bgSecondary,
        ) {
            GatewayInfoSheet(
                gateway = gw,
                connectedNetwork = if (gw.ip == null && gw.type == ConnectionNodeType.WifiRouter) null else connectedNetwork,
                c = c,
            )
        }
    }
    if (showInternetSheet) {
        ModalBottomSheet(
            onDismissRequest = { showInternetSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.bgSecondary,
        ) {
            InternetInfoSheet(
                publicIp = ispInfo?.ip ?: publicIp,
                ispInfo = ispInfo,
                privateDnsAtivo = snapshotRede.privateDnsAtivo,
                privateDnsHostname = snapshotRede.privateDnsHostname,
                dnsServidores = snapshotRede.dnsServidores,
                c = c,
            )
        }
    }
    if (showCellularSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCellularSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.bgSecondary,
        ) {
            CellularInfoSheet(
                ispInfo = ispInfo,
                publicIp = publicIp,
                movelSnapshot = movelSnapshot,
                wifiAtivo = isOnWifi,
                c = c,
            )
        }
    }
    if (showMedicaoTipoSheet) {
        MedicaoTipoSheet(
            isOnWifi = isOnWifi,
            onDismiss = { showMedicaoTipoSheet = false },
            onIniciarTeste = { modo ->
                showMedicaoTipoSheet = false
                onIniciarTeste(modo)
            },
            c = c,
        )
    }

    val profileBrush = remember { Brush.linearGradient(colors = listOf(LkColors.accent, LkColors.accentBlue)) }
    val listState = rememberLazyListState()
    val topBarAlpha = listState.rememberTopBarAlpha()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.graphicsLayer { alpha = topBarAlpha },
                title = {
                    val estaConectado =
                        snapshotRede.estadoConexao == EstadoConexao.wifi ||
                            snapshotRede.estadoConexao == EstadoConexao.movel
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Home,
                                contentDescription = null,
                                tint = c.textPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(LkSpacing.sm))
                            Text(
                                text = stringResource(R.string.home_titulo),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.W600,
                                color = c.textPrimary,
                            )
                        }
                        if (estaConectado) {
                            Text(
                                text = topBarSubtitulo(snapshotRede, movelSnapshot),
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary,
                            )
                        }
                    }
                },
                navigationIcon = {
                    ProfileAvatarButton(
                        nomeUsuario = nomeUsuario,
                        fotoUri = fotoUriUsuario,
                        onClick = onAbrirPerfil,
                    )
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = c.bgPrimary,
                        titleContentColor = c.textPrimary,
                    ),
            )
        },
        containerColor = c.bgPrimary,
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            contentPadding =
                PaddingValues(
                    start = LkSpacing.lg,
                    end = LkSpacing.lg,
                    top = innerPadding.calculateTopPadding() + LkSpacing.md,
                    bottom = LkSpacing.xxl,
                ),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.lg),
        ) {
            // 1. Caminho da rede
            item {
                NetworkPath(
                    ispName = ispInfo?.isp,
                    gateways = gateways,
                    localIp = localIp,
                    publicIp = publicIp,
                    isIspInfoLoading = isIspInfoLoading,
                    deviceName = deviceName,
                    connectedNetwork = connectedNetwork,
                    ispInfo = ispInfo,
                    snapshotRede = snapshotRede,
                    movelSnapshot = movelSnapshot,
                    c = c,
                    onDeviceTap = { showDeviceSheet = true },
                    onGatewayTap = { showGatewaySheet = it },
                    onInternetTap = { showInternetSheet = true },
                )
            }

            // CGNAT banner
            if (isCgNat(ispInfo?.ip ?: publicIp)) {
                item {
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .border(1.dp, LkColors.warning.copy(alpha = 0.50f), RoundedCornerShape(LkRadius.card)),
                        colors = CardDefaults.cardColors(containerColor = LkColors.warning.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(LkRadius.card),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = LkColors.warning,
                                modifier = Modifier.size(20.dp),
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    stringResource(R.string.home_cgnat_titulo),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    stringResource(R.string.home_cgnat_descricao),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // 2. Medições card
            item {
                CardMedicoes(
                    effectiveTs = effectiveTs,
                    effectiveDl = effectiveDl,
                    effectiveUl = effectiveUl,
                    hasEffectiveResult = hasEffectiveResult,
                    history = history,
                    onAbrirHistorico = onAbrirHistorico,
                    onAbrirUltimoResultado = onAbrirUltimoResultado,
                    onIniciarTeste = { showMedicaoTipoSheet = true },
                    c = c,
                )
            }

            // 3. Mini-cards DNS / Ping / Diagnóstico IA
            item {
                MiniCardsRow(
                    c = c,
                    onAbrirDns = onAbrirDns,
                    onAbrirPing = onAbrirPing,
                    onAbrirDiagnostico = onAbrirDiagnostico,
                )
            }

            // 4a. Wi-Fi SignalCard
            if (snapshotRede.estadoConexao == EstadoConexao.wifi) {
                item {
                    WifiSignalCard(
                        snapshotRede = snapshotRede,
                        connectedNetwork = connectedNetwork,
                        c = c,
                        onTap = onAbrirRedes,
                        quantidadeDispositivos =
                            snapshotDispositivos
                                ?.takeIf { it.estado == io.signallq.app.feature.devices.EstadoScanDispositivos.concluido }
                                ?.dispositivos
                                ?.size
                                ?.takeIf { it > 0 },
                        onTapDispositivos = onAbrirDispositivos,
                    )
                }
            }

            // 4b. Mobile SignalCard
            if (movelSnapshot != null) {
                item {
                    MobileSignalCard(
                        movelSnapshot = movelSnapshot,
                        mobileName = movelSnapshot.operadora,
                        c = c,
                        onTap = { showCellularSheet = true },
                    )
                }
            }

            // 5. SIM chips compactos (abaixo dos SignalCards) — só exibir se dual-SIM
            if (simsAtivos.size >= 2) {
                item {
                    CardMovelDualSim(simsAtivos = simsAtivos, c = c)
                }
            }
        }
    }
}

// ─── CardMedicoes ─────────────────────────────────────────────────────────────

/**
 * Card de medicoes (download/upload/historico) extraido do corpo principal do HomeScreen.
 * Recebe apenas os dados que a secao precisa — nao o estado inteiro da tela.
 */
@Composable
private fun CardMedicoes(
    effectiveTs: Long?,
    effectiveDl: Double?,
    effectiveUl: Double?,
    hasEffectiveResult: Boolean,
    history: List<HistoryPoint>,
    onAbrirHistorico: () -> Unit,
    onAbrirUltimoResultado: () -> Unit,
    onIniciarTeste: () -> Unit,
    c: LkTokens,
) {
    SignallQCard(c) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.xs)) {
                    Text(
                        stringResource(R.string.home_medicoes_titulo),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.W500,
                        color = c.textPrimary,
                    )
                    effectiveTs?.let {
                        Text(
                            "Última: ${formatTimestamp(it)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = c.textTertiary,
                        )
                    }
                }
                if (history.isNotEmpty()) {
                    Text(
                        stringResource(R.string.home_btn_ver_historico),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.W600,
                        color = LkColors.accent,
                        modifier = Modifier.minimumInteractiveComponentSize().clickable { onAbrirHistorico() },
                    )
                }
            }
            Spacer(modifier = Modifier.height(LkSpacing.lg))
            if (hasEffectiveResult && effectiveTs != null && effectiveDl != null && effectiveUl != null) {
                LastResultHero(
                    timestampEpochMs = effectiveTs,
                    downloadMbps = effectiveDl,
                    uploadMbps = effectiveUl,
                    c = c,
                )
                Spacer(modifier = Modifier.height(LkSpacing.lg))
            }
            val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
            val chartHeight = (screenHeightDp * 0.10f).coerceIn(72.dp, 120.dp)
            val hasChartData = remember(history) { history.any { it.downloadMbps != null || it.uploadMbps != null } }
            if (hasChartData) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(LkRadius.card))
                            .clickable { onAbrirUltimoResultado() },
                ) {
                    MiniLineChart(
                        history = history,
                        modifier = Modifier.fillMaxWidth().height(chartHeight),
                        c = c,
                    )
                    Spacer(modifier = Modifier.height(LkSpacing.xs))
                    Text(
                        text = "Ver detalhes →",
                        style = MaterialTheme.typography.labelSmall,
                        color = LkColors.accent,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.home_medicoes_sem_dados),
                            style = MaterialTheme.typography.titleSmall,
                            color = c.textTertiary,
                            textAlign = TextAlign.Center,
                        )
                        TextButton(onClick = onIniciarTeste) {
                            Text(stringResource(R.string.home_btn_fazer_teste_agora))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(LkSpacing.lg))
            Button(
                onClick = onIniciarTeste,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LkRadius.card),
                colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                contentPadding = PaddingValues(vertical = 14.dp),
            ) {
                Text(
                    stringResource(R.string.home_btn_medir_velocidade),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.W600,
                )
            }
        }
    }
}

// ─── SignallQCard ────────────────────────────────────────────────────────────────

@Composable
private fun SignallQCard(
    c: LkTokens,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LkRadius.card),
        color = c.bgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, c.border),
    ) {
        Box(modifier = Modifier.padding(LkSpacing.lg)) {
            content()
        }
    }
}

// ─── #82 AnatelBanner ─────────────────────────────────────────────────────────

@Composable
internal fun AnatelBanner(
    onDismiss: () -> Unit,
    c: LkTokens,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, LkColors.warning.copy(alpha = 0.50f), RoundedCornerShape(LkRadius.card)),
        colors = CardDefaults.cardColors(containerColor = LkColors.warning.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(LkRadius.card),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.CellTower,
                contentDescription = null,
                tint = LkColors.warning,
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    stringResource(R.string.home_banner_anatel_titulo),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    stringResource(R.string.home_banner_anatel_descricao),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.material3.IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Dispensar",
                    tint = c.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ─── #82 BufferbloatCard ───────────────────────────────────────────────────────

@Composable
internal fun BufferbloatCard(
    bufferbloatMs: Double?,
    c: LkTokens,
) {
    if (bufferbloatMs == null) return
    val (badgeLabel, badgeColor, badgeBg) =
        when {
            bufferbloatMs <= 5.0 ->
                Triple(
                    stringResource(R.string.home_atraso_extra_otimo),
                    LkColors.success,
                    LkColors.success.copy(alpha = 0.12f),
                )
            bufferbloatMs <= 30.0 ->
                Triple(
                    stringResource(R.string.home_atraso_extra_aceitavel),
                    LkColors.warning,
                    LkColors.warning.copy(alpha = 0.12f),
                )
            else ->
                Triple(
                    stringResource(R.string.home_atraso_extra_alto),
                    LkColors.error,
                    LkColors.error.copy(alpha = 0.12f),
                )
        }
    SignallQCard(c) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            Icon(
                imageVector = Icons.Outlined.Wifi,
                contentDescription = null,
                tint = LkColors.accent,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_atraso_extra_titulo),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                )
                Text(
                    stringResource(R.string.home_atraso_extra_subtexto),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textTertiary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(badgeBg)
                            .padding(horizontal = LkSpacing.sm, vertical = LkSpacing.xs),
                ) {
                    Text(badgeLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.W600, color = badgeColor)
                }
                Text(
                    "${bufferbloatMs.toInt()} ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                )
            }
        }
    }
}

// ─── #82 MiniCardsRow ─────────────────────────────────────────────────────────

@Composable
private fun MiniCardsRow(
    c: LkTokens,
    onAbrirDns: () -> Unit,
    onAbrirPing: () -> Unit,
    onAbrirDiagnostico: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        MiniCard(
            icon = Icons.Outlined.Language,
            label = stringResource(R.string.home_minicard_dns),
            onClick = onAbrirDns,
            modifier = Modifier.weight(1f),
            c = c,
        )
        MiniCard(
            icon = Icons.Outlined.Wifi,
            label = stringResource(R.string.home_minicard_ping),
            onClick = onAbrirPing,
            modifier = Modifier.weight(1f),
            c = c,
        )
        MiniCard(
            icon = Icons.Outlined.Insights,
            label = stringResource(R.string.home_minicard_diagnostico),
            onClick = onAbrirDiagnostico,
            modifier = Modifier.weight(1f),
            c = c,
        )
    }
}

@Composable
private fun MiniCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    c: LkTokens,
) {
    Surface(
        modifier =
            modifier
                .clip(RoundedCornerShape(LkRadius.card))
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(LkRadius.card),
        color = c.bgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, c.border),
    ) {
        Column(
            modifier = Modifier.padding(vertical = LkSpacing.md, horizontal = LkSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LkColors.accent,
                modifier = Modifier.size(24.dp),
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─── Network path ─────────────────────────────────────────────────────────────

@Composable
private fun NetworkPath(
    ispName: String?,
    gateways: List<GatewayInfo>,
    localIp: String?,
    publicIp: String?,
    isIspInfoLoading: Boolean,
    deviceName: String,
    connectedNetwork: RedeVizinha?,
    ispInfo: IspInfo?,
    snapshotRede: SnapshotRede,
    movelSnapshot: MovelSnapshot?,
    c: LkTokens,
    onDeviceTap: () -> Unit,
    onGatewayTap: (GatewayInfo) -> Unit,
    onInternetTap: () -> Unit,
) {
    val estadoConexao = snapshotRede.estadoConexao
    val isConectado = estadoConexao == EstadoConexao.wifi || estadoConexao == EstadoConexao.movel || estadoConexao == EstadoConexao.ethernet
    val hasLocalError = localIp == null && !isConectado
    val loadingLocal = localIp == null && isConectado

    // hasInternetError = transporte ativo + IP local presente + conectividade validada ausente.
    // Usa snapshotRede.conectado (NET_CAPABILITY_VALIDATED) em vez de "publicIp == null",
    // porque publicIp nulo pode significar "ainda carregando" — não "sem internet".
    // Mostrar erro apenas quando o SO confirma que não há internet (captive portal, etc.).
    val hasInternetError = isConectado && localIp != null && !snapshotRede.conectado

    // loadingInternet = conectado + validado + fetch de IP/ISP ainda em andamento.
    // Usa isIspInfoLoading (UiState.Loading) em vez de null-check — UiState.Error
    // também produz null após unwrap, mas não significa "ainda carregando".
    val loadingInternet =
        isConectado &&
            snapshotRede.conectado &&
            localIp != null &&
            isIspInfoLoading

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            PathNode(
                icon = Icons.Outlined.Smartphone,
                iconColor = LkColors.accent,
                label = deviceName,
                subLabel =
                    when {
                        localIp != null -> localIp
                        isConectado -> stringResource(R.string.home_network_buscando)
                        else -> stringResource(R.string.home_network_desconectado)
                    },
                subLabelColor = if (hasLocalError) LkColors.error else null,
                c = c,
                onTap = onDeviceTap,
            )
        }
        gateways.forEachIndexed { i, gw ->
            PathConnector(
                c = c,
                active = if (i == 0) !hasLocalError && !loadingLocal else true,
                hasError = i == 0 && hasLocalError,
                loading = i == 0 && loadingLocal,
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val (icon, _) = nodeDisplay(gw.type)
                val isMobileNode = gw.type == ConnectionNodeType.Mobile
                // #221: nó móvel exibe operadora como título e tecnologia como subtítulo
                val nodeLabel =
                    if (isMobileNode) {
                        movelSnapshot?.operadora?.ifBlank { null }
                            ?: gw.name.takeIf { it != "Antena movel" && it != "Antena móvel" }
                            ?: "Rede móvel"
                    } else {
                        gw.name
                    }
                val nodeSubLabel =
                    if (isMobileNode && gw.ip == null) {
                        val tec = movelSnapshot?.tecnologia?.ifBlank { null }
                        if (tec != null) "Rede móvel · $tec" else "Rede móvel"
                    } else {
                        gw.ip ?: "—"
                    }
                PathNode(
                    icon = icon,
                    iconColor = c.textSecondary,
                    label = nodeLabel,
                    subLabel = nodeSubLabel,
                    c = c,
                    onTap = { onGatewayTap(gw) },
                )
            }
        }
        PathConnector(
            c = c,
            active = !hasInternetError && !loadingInternet,
            hasError = hasInternetError,
            loading = loadingInternet,
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            val isMobileConnection = snapshotRede.estadoConexao == EstadoConexao.movel
            val internetLabel =
                if (isMobileConnection) {
                    "Internet"
                } else {
                    ispInfo?.isp?.takeIf { it.isNotEmpty() }
                        ?: ispName?.takeIf { it.isNotEmpty() }
                        ?: "Internet"
                }
            val internetSubLabel =
                ispInfo?.ip ?: publicIp
                    ?: when {
                        hasInternetError -> stringResource(R.string.home_network_sem_conexao)
                        isIspInfoLoading -> stringResource(R.string.home_network_conectando)
                        // #220: "—" substituído por texto útil quando IP público não está disponível
                        else -> "IP indisponível"
                    }
            val internetSubColor: Color? =
                when {
                    hasInternetError -> LkColors.error
                    (ispInfo?.ip ?: publicIp) != null -> LkColors.success
                    else -> null
                }
            PathNode(
                icon = Icons.Outlined.Language,
                iconColor = if (hasInternetError) c.textTertiary else LkColors.success,
                label = internetLabel,
                subLabel = internetSubLabel,
                subLabelColor = internetSubColor,
                c = c,
                onTap = onInternetTap,
            )
        }
    }
}

private fun nodeDisplay(type: ConnectionNodeType): Pair<ImageVector, String> =
    when (type) {
        ConnectionNodeType.WifiRouter -> Icons.Outlined.Router to "Roteador"
        ConnectionNodeType.WifiMesh -> Icons.Outlined.Hub to "Mesh"
        ConnectionNodeType.WifiExtender -> Icons.Outlined.SettingsInputAntenna to "Extensor"
        ConnectionNodeType.Mobile -> Icons.Outlined.CellTower to "Antena móvel"
        ConnectionNodeType.Unknown -> Icons.Outlined.DeviceHub to "Rede"
    }

private fun labelRedeMovel(
    movelSnapshot: MovelSnapshot?,
    fallback: String = "—",
): String {
    val operadora =
        movelSnapshot
            ?.operadora
            ?.trim()
            ?.split(" ")
            ?.joinToString(" ") { word ->
                if (word.equals("BR", ignoreCase = true)) {
                    ""
                } else {
                    word.lowercase().replaceFirstChar { it.uppercaseChar() }
                }
            }?.trim()
            ?.ifBlank { null }
    val tecnologia = movelSnapshot?.tecnologia?.ifBlank { null }

    return when {
        operadora != null && tecnologia != null -> "$operadora · $tecnologia"
        operadora != null -> operadora
        tecnologia != null -> tecnologia
        else -> fallback
    }
}

@Composable
private fun PathNode(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    subLabel: String,
    subLabelColor: Color? = null,
    subLabel2: String? = null,
    c: LkTokens,
    onTap: (() -> Unit)? = null,
) {
    Column(
        modifier =
            if (onTap != null) {
                Modifier
                    .clip(RoundedCornerShape(LkRadius.card))
                    .clickable { onTap() }
                    .padding(horizontal = 6.dp)
            } else {
                Modifier.padding(horizontal = 6.dp)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.height(LkSpacing.sm))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.W600,
            color = c.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            subLabel,
            style = MaterialTheme.typography.labelSmall,
            color = subLabelColor ?: c.textTertiary,
            fontWeight = if (subLabelColor != null) FontWeight.W500 else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subLabel2 != null) {
            Text(
                subLabel2,
                style = MaterialTheme.typography.labelSmall,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PathConnector(
    c: LkTokens,
    active: Boolean = true,
    hasError: Boolean = false,
    loading: Boolean = false,
) {
    val animatedAlpha by if (loading) {
        val transition = rememberInfiniteTransition(label = "connector_loading")
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
            label = "alpha",
        )
    } else {
        remember { mutableStateOf(1f) }
    }
    val lineColor =
        when {
            hasError -> LkColors.error.copy(alpha = 0.3f)
            loading -> LkColors.accent.copy(alpha = animatedAlpha)
            active -> LkColors.success
            else -> c.border
        }
    val dashed = !active || hasError || loading
    val semanticModifier =
        when {
            hasError -> Modifier.semantics { contentDescription = "Sem conexão" }
            loading -> Modifier.semantics { contentDescription = "Conectando" }
            else -> Modifier.semantics(mergeDescendants = true) { contentDescription = "" }
        }
    Box(
        modifier =
            Modifier
                .size(width = 36.dp, height = 56.dp)
                .then(semanticModifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
            if (dashed) {
                var x = 0f
                val dashW = 4.dp.toPx()
                val space = 3.dp.toPx()
                while (x < size.width) {
                    drawLine(
                        color = lineColor,
                        start = Offset(x, 0f),
                        end = Offset(minOf(x + dashW, size.width), 0f),
                        strokeWidth = 2.dp.toPx(),
                    )
                    x += dashW + space
                }
            } else {
                drawLine(lineColor, Offset.Zero, Offset(size.width, 0f), 2.dp.toPx())
            }
        }
        if (hasError) {
            Box(
                modifier = Modifier.size(18.dp).clip(CircleShape).background(c.bgPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = null,
                    tint = LkColors.error,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

// ─── Last result hero ─────────────────────────────────────────────────────────

@Composable
private fun LastResultHero(
    timestampEpochMs: Long,
    downloadMbps: Double,
    uploadMbps: Double,
    c: LkTokens,
) {
    Column {
        HorizontalDivider(color = c.border, thickness = 1.dp)
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                text = formatTimestamp(timestampEpochMs),
                style = MaterialTheme.typography.labelMedium,
                color = c.textTertiary,
                modifier = Modifier.align(Alignment.End),
            )
            Spacer(modifier = Modifier.height(LkSpacing.sm))
            Row(modifier = Modifier.fillMaxWidth()) {
                HeroSpeed(
                    arrow = "↓",
                    value = downloadMbps,
                    label = "Download",
                    color = LkColors.accent,
                    labelColor = c.textTertiary,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(16.dp))
                HeroSpeed(
                    arrow = "↑",
                    value = uploadMbps,
                    label = "Upload",
                    color = LkColors.success,
                    labelColor = c.textTertiary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        HorizontalDivider(color = c.border, thickness = 1.dp)
    }
}

@Composable
private fun HeroSpeed(
    arrow: String,
    value: Double,
    label: String,
    color: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
) {
    val formatted = if (value >= 100) value.toLong().toString() else "%.1f".format(value)
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                arrow,
                fontSize = 15.sp,
                fontWeight = FontWeight.W600,
                color = color,
                lineHeight = 27.sp,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                formatted,
                style = MaterialTheme.typography.displayLarge,
                color = color,
                lineHeight = 30.sp,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "Mbps",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W500,
                color = color.copy(alpha = 0.75f),
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Spacer(modifier = Modifier.height(LkSpacing.sm))
        Text(label, style = MaterialTheme.typography.labelMedium, color = labelColor)
    }
}

private fun formatTimestamp(epochMs: Long): String {
    val diff = System.currentTimeMillis() - epochMs
    val cal =
        java.util.Calendar
            .getInstance()
            .also { it.timeInMillis = epochMs }
    return when {
        diff < 60_000L -> "Agora"
        diff < 3_600_000L -> "Há ${diff / 60_000}min"
        diff < 86_400_000L ->
            "%02d:%02d".format(
                cal.get(java.util.Calendar.HOUR_OF_DAY),
                cal.get(java.util.Calendar.MINUTE),
            )
        diff < 2 * 86_400_000L ->
            "Ontem, %02d:%02d".format(
                cal.get(java.util.Calendar.HOUR_OF_DAY),
                cal.get(java.util.Calendar.MINUTE),
            )
        else -> "${cal.get(java.util.Calendar.DAY_OF_MONTH)}/${cal.get(java.util.Calendar.MONTH) + 1}"
    }
}

// ─── Mini line chart ──────────────────────────────────────────────────────────

@Composable
private fun MiniLineChart(
    history: List<HistoryPoint>,
    modifier: Modifier,
    c: LkTokens,
) {
    val accent = LkColors.accent
    val success = LkColors.success
    Canvas(modifier = modifier) {
        val dlPairs = history.mapIndexedNotNull { i, p -> p.downloadMbps?.let { i.toFloat() to it.toFloat() } }
        val ulPairs = history.mapIndexedNotNull { i, p -> p.uploadMbps?.let { i.toFloat() to it.toFloat() } }
        val allVals = dlPairs.map { it.second } + ulPairs.map { it.second }
        if (allVals.isEmpty()) return@Canvas
        val maxY = allVals.max() * 1.2f
        val n = (history.size - 1).coerceAtLeast(1).toFloat()

        fun toX(i: Float) = i / n * size.width

        fun toY(v: Float) = size.height - (v / maxY * size.height)

        fun smoothPath(pts: List<Pair<Float, Float>>): Path {
            val path = Path()
            if (pts.isEmpty()) return path
            path.moveTo(toX(pts[0].first), toY(pts[0].second))
            for (k in 1 until pts.size) {
                val prev = pts[k - 1]
                val curr = pts[k]
                val cpx = (toX(prev.first) + toX(curr.first)) / 2f
                path.cubicTo(cpx, toY(prev.second), cpx, toY(curr.second), toX(curr.first), toY(curr.second))
            }
            return path
        }

        if (dlPairs.isNotEmpty()) {
            val linePath = smoothPath(dlPairs)
            val fillPath =
                Path().also {
                    it.addPath(linePath)
                    it.lineTo(toX(dlPairs.last().first), size.height)
                    it.lineTo(toX(dlPairs.first().first), size.height)
                    it.close()
                }
            drawPath(fillPath, accent.copy(alpha = 0.1f))
            drawPath(linePath, accent, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        if (ulPairs.isNotEmpty()) {
            val linePath = smoothPath(ulPairs)
            val fillPath =
                Path().also {
                    it.addPath(linePath)
                    it.lineTo(toX(ulPairs.last().first), size.height)
                    it.lineTo(toX(ulPairs.first().first), size.height)
                    it.close()
                }
            drawPath(fillPath, success.copy(alpha = 0.07f))
            drawPath(linePath, success, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

// ─── Signal quality row ───────────────────────────────────────────────────────

@Composable
private fun SignalQualityRow(
    snapshotRede: SnapshotRede,
    connectedNetwork: RedeVizinha?,
    mobileName: String?,
    c: LkTokens,
    onTap: () -> Unit,
) {
    val isWifi = snapshotRede.estadoConexao == EstadoConexao.wifi
    val isMobile = snapshotRede.estadoConexao == EstadoConexao.movel
    val wifiRssi = if (isWifi) (connectedNetwork?.rssiDbm ?: snapshotRede.wifiLinkSnapshot?.rssiDbm) else null
    val wifiPct = wifiRssi?.let { ((it + 90) / 50.0).coerceIn(0.0, 1.0) * 100 }?.roundToInt()

    if (wifiPct == null && !isMobile) return

    val wifiColor =
        when {
            wifiPct == null -> LkColors.success
            wifiPct >= 70 -> LkColors.success
            wifiPct >= 40 -> LkColors.warning
            else -> LkColors.error
        }

    Column(modifier = Modifier.clickable { onTap() }) {
        HorizontalDivider(color = c.border, thickness = 1.dp)
        if (wifiPct != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = LkSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Wifi, contentDescription = null, tint = wifiColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(LkSpacing.sm))
                LinearProgressIndicator(
                    progress = { wifiPct / 100f },
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(999.dp)),
                    color = wifiColor,
                    trackColor = c.border,
                )
                Spacer(modifier = Modifier.width(LkSpacing.sm))
                Text("$wifiPct%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W600, color = wifiColor)
            }
        }
        if (isMobile) {
            Row(
                modifier =
                    Modifier.fillMaxWidth().padding(
                        top = if (wifiPct != null) 0.dp else 12.dp,
                        bottom = 12.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.SignalCellularAlt, contentDescription = null, tint = LkColors.success, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = mobileName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.home_network_rede_movel),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.textSecondary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.home_network_conectado),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = LkColors.success,
                )
            }
        }
        Text(stringResource(R.string.home_network_forca_sinal), style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
    }
}

// ─── Wifi signal helpers ─────────────────────────────────────────────────────

@Composable
private fun wifiSignalColor(rssiDbm: Int): Color =
    when {
        rssiDbm >= -55 -> LkColors.success
        rssiDbm >= -70 -> LkColors.success
        rssiDbm >= -80 -> LkColors.warning
        else -> LkColors.error
    }

private fun wifiSignalQuality(rssiDbm: Int): String =
    when {
        rssiDbm >= -55 -> "Excelente"
        rssiDbm >= -70 -> "Forte"
        rssiDbm >= -80 -> "Regular"
        else -> "Fraco"
    }

private fun mobileSignalQuality(rsrpDbm: Int?): String =
    when {
        rsrpDbm == null -> "Conectado"
        rsrpDbm > -80 -> "Excelente"
        rsrpDbm > -90 -> "Bom"
        rsrpDbm > -100 -> "Regular"
        else -> "Fraco"
    }

private fun mobileSignalColor(rsrpDbm: Int?): Color =
    when {
        rsrpDbm == null -> LkColors.accent
        rsrpDbm > -80 -> LkColors.success
        rsrpDbm > -100 -> LkColors.warning
        else -> LkColors.error
    }

// ─── Wi-Fi signal card ───────────────────────────────────────────────────────

@Composable
private fun WifiSignalCard(
    snapshotRede: SnapshotRede,
    connectedNetwork: RedeVizinha?,
    c: LkTokens,
    onTap: () -> Unit,
    quantidadeDispositivos: Int? = null,
    onTapDispositivos: () -> Unit = {},
) {
    val wifiRssi = connectedNetwork?.rssiDbm ?: snapshotRede.wifiLinkSnapshot?.rssiDbm
    val wifiPct = wifiRssi?.let { ((it + 90) / 50.0).coerceIn(0.0, 1.0) * 100 }?.roundToInt()
    val ssidResolvido = connectedNetwork?.ssid ?: snapshotRede.wifiLinkSnapshot?.ssid
    val ssid = ssidResolvido ?: "Wi-Fi"
    val freqMhz = connectedNetwork?.frequenciaMhz ?: snapshotRede.wifiLinkSnapshot?.frequenciaMhz
    val wifiLinkSpeed = snapshotRede.wifiLinkSnapshot?.linkSpeedMbps
    val canal = freqMhz?.let { freqToChannel(it) }?.takeIf { it > 0 }
    val localizacaoDesligada = ssidResolvido == null && !snapshotRede.locationAtivado

    val iconColor =
        if (localizacaoDesligada) {
            c.textTertiary
        } else if (wifiRssi != null) {
            wifiSignalColor(wifiRssi)
        } else {
            LkColors.accent
        }

    SignallQCard(c) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Wifi,
                        contentDescription = if (wifiPct != null) "Wi-Fi $ssid, sinal $wifiPct%" else "Wi-Fi",
                        tint = iconColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(LkSpacing.md))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (localizacaoDesligada) {
                        val context = LocalContext.current
                        Text(
                            stringResource(R.string.home_network_nome_indisponivel),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.W600,
                            color = c.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            stringResource(R.string.home_network_habilitar_localizacao),
                            style = MaterialTheme.typography.bodySmall,
                            color = LkColors.accent,
                            fontWeight = FontWeight.W500,
                            modifier =
                                Modifier.clickable {
                                    context.startActivity(
                                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                },
                        )
                        Text(
                            stringResource(R.string.home_network_localizacao_necessaria),
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textTertiary,
                        )
                    } else {
                        val bandaLabel =
                            when {
                                freqMhz != null && freqMhz >= 5900 -> "6 GHZ"
                                freqMhz != null && freqMhz >= 3000 -> "5 GHZ"
                                freqMhz != null -> "2.4 GHZ"
                                else -> null
                            }
                        if (bandaLabel != null) {
                            Text(
                                "WI-FI · $bandaLabel",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.W600,
                                color = c.textTertiary,
                                letterSpacing = 0.3.sp,
                            )
                        }
                        Text(
                            ssid,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.W600,
                            color = c.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val linha2 =
                            buildList {
                                wifiRssi?.let { add("RSSI $it dBm") }
                                canal?.let { add("Canal $it") }
                                wifiLinkSpeed?.let { add("$it Mbps") }
                            }.joinToString(" · ")
                        if (linha2.isNotEmpty()) {
                            Text(
                                linha2,
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (wifiRssi != null && wifiPct != null) {
                    MiniSignalBars(pct = wifiPct, color = iconColor)
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(
                        wifiSignalQuality(wifiRssi),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.W600,
                        color = iconColor,
                    )
                }
            }
            if (!localizacaoDesligada && connectedNetwork != null) {
                Spacer(Modifier.height(LkSpacing.sm))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                ) {
                    ChipSegurancaWifi(seguranca = connectedNetwork.seguranca)
                    if (quantidadeDispositivos != null) {
                        val labelDispositivos =
                            if (quantidadeDispositivos == 1) {
                                "1 dispositivo na rede"
                            } else {
                                "$quantidadeDispositivos dispositivos na rede"
                            }
                        AssistChip(
                            onClick = onTapDispositivos,
                            label = { Text(labelDispositivos, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(Icons.Outlined.DeviceHub, contentDescription = null, modifier = Modifier.size(14.dp))
                            },
                            colors =
                                AssistChipDefaults.assistChipColors(
                                    containerColor = LkColors.accent.copy(alpha = 0.08f),
                                    labelColor = LkColors.accent,
                                    leadingIconContentColor = LkColors.accent,
                                ),
                            border =
                                AssistChipDefaults.assistChipBorder(
                                    borderColor = LkColors.accent.copy(alpha = 0.25f),
                                    enabled = true,
                                ),
                        )
                    }
                }
            }
        }
    }
}

// ─── Mobile signal card ──────────────────────────────────────────────────────

@Composable
private fun MobileSignalCard(
    movelSnapshot: MovelSnapshot,
    mobileName: String?,
    c: LkTokens,
    onTap: () -> Unit,
) {
    val tec = movelSnapshot.tecnologia?.ifBlank { null }
    val tecLabel = tec?.uppercase() ?: "LTE"
    val rsrp = movelSnapshot.rsrpDbm
    val mobileColor = mobileSignalColor(rsrp)

    SignallQCard(c) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onTap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(mobileColor.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.SignalCellularAlt,
                    contentDescription = "Rede móvel, ${mobileName ?: "dados móveis"}",
                    tint = mobileColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(LkSpacing.md))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "REDE MÓVEL · $tecLabel",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.W600,
                    color = c.textTertiary,
                    letterSpacing = 0.3.sp,
                )
                Text(
                    mobileName?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.home_network_rede_movel),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val infoItems =
                    buildList {
                        rsrp?.let { add("RSRP $it dBm") }
                        movelSnapshot.operadora?.takeIf { it.isNotBlank() }?.let { add(it) }
                        tec?.let { add(it) }
                    }.joinToString(" · ")
                if (infoItems.isNotEmpty()) {
                    Text(
                        infoItems,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (rsrp != null) {
                val mobilePct = ((rsrp + 140) / 96.0).coerceIn(0.0, 1.0) * 100
                MiniSignalBars(pct = mobilePct.roundToInt(), color = mobileColor)
                Spacer(Modifier.width(LkSpacing.sm))
            }
            Text(
                mobileSignalQuality(rsrp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = mobileColor,
            )
        }
    }
}

@Composable
internal fun MiniSignalBars(
    pct: Int,
    color: Color,
) {
    val active =
        run {
            val v = (pct / 25.0).coerceIn(0.0, 4.0)
            if (v > 0.0 && v < 1.0) 1 else v.toInt()
        }
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        listOf(4.dp, 6.dp, 8.dp, 10.dp).forEachIndexed { i, h ->
            Box(
                Modifier
                    .width(3.dp)
                    .height(h)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (i < active) color else color.copy(alpha = 0.2f)),
            )
        }
    }
}

// ─── #179 Task B — Chip de segurança Wi-Fi ───────────────────────────────────

@Composable
private fun ChipSegurancaWifi(seguranca: SegurancaWifi) {
    val isOpen = seguranca == SegurancaWifi.aberta
    val isWpa3 = seguranca == SegurancaWifi.wpa3
    val isDesconhecida = seguranca == SegurancaWifi.desconhecida

    val (label, chipContainerColor, chipLabelColor, chipIcon) =
        when {
            isWpa3 ->
                Quadruple(
                    "WPA3",
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                    Icons.Outlined.Lock,
                )
            isOpen ->
                Quadruple(
                    "Aberta (sem senha)",
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.onErrorContainer,
                    Icons.Outlined.LockOpen,
                )
            seguranca == SegurancaWifi.wep ->
                Quadruple(
                    "WEP (inseguro)",
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    MaterialTheme.colorScheme.onErrorContainer,
                    Icons.Outlined.Lock,
                )
            isDesconhecida ->
                Quadruple(
                    "Segurança desconhecida",
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    Icons.Outlined.Lock,
                )
            else ->
                Quadruple(
                    wifiSecurityLabel(seguranca),
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                    Icons.Outlined.Lock,
                )
        }

    AssistChip(
        onClick = {},
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = chipLabelColor,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = chipIcon,
                contentDescription = null,
                tint = chipLabelColor,
                modifier = Modifier.size(14.dp),
            )
        },
        colors =
            AssistChipDefaults.assistChipColors(
                containerColor = chipContainerColor,
                labelColor = chipLabelColor,
                leadingIconContentColor = chipLabelColor,
            ),
        border = null,
    )
}

// Quadruple helper local — Kotlin nao tem built-in
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

// ─── #179 Task C — CardMovelDualSim ──────────────────────────────────────────

/**
 * Card de rede movel para dispositivos com um ou mais SIMs ativos.
 *
 * - simsAtivos.isEmpty(): nao renderiza nada.
 * - simsAtivos.size == 1: um card sem badge de SIM.
 * - simsAtivos.size >= 2: um card por SIM com badge "SIM 1", "SIM 2".
 */
@Composable
internal fun CardMovelDualSim(
    simsAtivos: List<MovelSimSnapshot>,
    c: LkTokens,
) {
    if (simsAtivos.isEmpty()) return

    Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
        simsAtivos.forEach { sim ->
            SimChipCompact(sim = sim, showSlot = simsAtivos.size >= 2, c = c, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SimChipCompact(
    sim: MovelSimSnapshot,
    showSlot: Boolean,
    c: LkTokens,
    modifier: Modifier = Modifier,
) {
    val rsrpDbm = sim.rsrpDbm
    val rsrpColor = mobileSignalColor(rsrpDbm)
    val rsrpPct = rsrpDbm?.let { ((it + 140) / 96.0).coerceIn(0.0, 1.0) * 100 }?.roundToInt() ?: 0
    val isActive = sim.tecnologiaRede != null

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(LkRadius.card),
        color = if (isActive) LkColors.accent.copy(alpha = 0.06f) else c.bgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) LkColors.accent.copy(alpha = 0.20f) else c.border),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Smartphone,
                    contentDescription = null,
                    tint = if (isActive) LkColors.accent else c.textTertiary,
                    modifier = Modifier.size(14.dp),
                )
                if (showSlot) {
                    Text(
                        "SIM ${sim.simIndex}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.W700,
                        color = if (isActive) LkColors.accent else c.textTertiary,
                    )
                }
                if (isActive) {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(LkColors.accent.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(
                            "ATIVO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.W700,
                            color = LkColors.accent,
                            fontSize = 9.sp,
                        )
                    }
                }
                if (sim.emRoaming) {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(
                            "R",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.W700,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 9.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                sim.operadora?.ifBlank { null } ?: "Operadora",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val techText =
                    buildList {
                        sim.tecnologiaRede?.let { add(it) }
                        rsrpDbm?.let { add("$it dBm") }
                    }.joinToString(" · ").ifEmpty { "Sem dados" }
                Text(
                    techText,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                    fontSize = 10.sp,
                )
                MiniSignalBars(pct = rsrpPct, color = rsrpColor)
            }
        }
    }
}

// ─── Qualidade shortcut row ───────────────────────────────────────────────────

@Composable
private fun QualidadeShortcutRow(
    history: List<HistoryPoint>,
    c: LkTokens,
    onClick: () -> Unit,
) {
    val subtexto = stringResource(R.string.home_shortcut_diagnostico_descricao)
    val subtextoColor = c.textSecondary

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .clickable(onClick = onClick)
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LkColors.accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Insights, contentDescription = null, tint = LkColors.accent, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(LkSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.home_shortcut_diagnostico_titulo),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtexto, style = MaterialTheme.typography.labelMedium, color = subtextoColor)
        }
        Spacer(modifier = Modifier.width(LkSpacing.sm))
        Icon(Icons.AutoMirrored.Outlined.ArrowForwardIos, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(18.dp))
    }
}

// ─── Experiência de uso ───────────────────────────────────────────────────────

@Composable
private fun ExperienciaDeUsoSection(
    downloadMbps: Double?,
    lat: Double?,
    jit: Double?,
    vereditoGamer: VereditoUso?,
    c: LkTokens,
) {
    // Cada entry: (icon, label, minDownload, maxLatencia?, maxJitter?)
    data class UseCase(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val label: String,
        val minDownload: Double,
        val maxLatencia: Double? = null,
        val maxJitter: Double? = null,
    )
    val useCases =
        listOf(
            UseCase(Icons.Outlined.SportsEsports, stringResource(R.string.home_uso_jogos), 5.0),
            UseCase(Icons.Outlined.Tv, stringResource(R.string.home_uso_streaming), 3.0),
            UseCase(Icons.Outlined.Laptop, stringResource(R.string.home_uso_home_office), 2.0, maxLatencia = 150.0, maxJitter = 30.0),
            UseCase(Icons.Outlined.Videocam, stringResource(R.string.home_uso_chamadas), 1.0, maxLatencia = 100.0, maxJitter = 20.0),
        )
    Column {
        Text(
            stringResource(R.string.home_secao_experiencia),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.W700,
            color = c.textTertiary,
            letterSpacing = 0.8.sp,
        )
        Spacer(modifier = Modifier.height(LkSpacing.sm))
        HorizontalDivider(color = c.border, thickness = 1.dp)
        useCases.forEach { useCase ->
            val isGaming = useCase.label == "Jogos"
            val badgeLabel: String
            val badgeColor: Color
            val badgeBg: Color
            if (isGaming) {
                badgeLabel =
                    when (vereditoGamer) {
                        null -> "–"
                        VereditoUso.good -> stringResource(R.string.home_status_ok)
                        VereditoUso.acceptable -> stringResource(R.string.home_status_regular)
                        VereditoUso.poor -> stringResource(R.string.home_status_ruim)
                    }
                badgeColor =
                    when (vereditoGamer) {
                        null -> c.textTertiary
                        VereditoUso.good -> LkColors.success
                        VereditoUso.acceptable -> LkColors.warning
                        VereditoUso.poor -> LkColors.error
                    }
                badgeBg =
                    when (vereditoGamer) {
                        null -> c.bgSecondary
                        VereditoUso.good -> LkColors.success.copy(alpha = 0.12f)
                        VereditoUso.acceptable -> LkColors.warning.copy(alpha = 0.12f)
                        VereditoUso.poor -> LkColors.error.copy(alpha = 0.12f)
                    }
            } else {
                val downloadOk = downloadMbps == null || downloadMbps >= useCase.minDownload
                val latOk = useCase.maxLatencia == null || lat == null || lat <= useCase.maxLatencia
                val jitOk = useCase.maxJitter == null || jit == null || jit <= useCase.maxJitter
                val isInstavel = downloadMbps != null && downloadOk && (!latOk || !jitOk)
                badgeLabel =
                    when {
                        downloadMbps == null -> "–"
                        isInstavel -> stringResource(R.string.home_status_instavel)
                        downloadOk -> stringResource(R.string.home_status_ok)
                        else -> stringResource(R.string.home_status_lento)
                    }
                badgeColor =
                    when {
                        downloadMbps == null -> c.textTertiary
                        isInstavel -> LkColors.warning
                        downloadOk -> LkColors.success
                        else -> LkColors.error
                    }
                badgeBg =
                    when {
                        downloadMbps == null -> c.bgSecondary
                        isInstavel -> LkColors.warning.copy(alpha = 0.12f)
                        downloadOk -> LkColors.success.copy(alpha = 0.12f)
                        else -> LkColors.error.copy(alpha = 0.12f)
                    }
            }
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = LkSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(useCase.icon, contentDescription = null, tint = LkColors.accent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(LkSpacing.md))
                    Text(
                        useCase.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier =
                            Modifier
                                .clip(
                                    RoundedCornerShape(999.dp),
                                ).background(badgeBg)
                                .padding(horizontal = LkSpacing.sm, vertical = LkSpacing.xs),
                    ) {
                        Text(badgeLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.W600, color = badgeColor)
                    }
                }
                HorizontalDivider(color = c.border, thickness = 1.dp)
            }
        }
    }
}

// ─── WiFi factors ─────────────────────────────────────────────────────────────

private enum class WifiQuality { Excelente, Bom, Razoavel, Ruim }

@Composable
private fun WifiFactorsSection(
    network: RedeVizinha,
    c: LkTokens,
) {
    val overall =
        when {
            network.rssiDbm >= -55 -> WifiQuality.Excelente
            network.rssiDbm >= -65 -> WifiQuality.Bom
            network.rssiDbm >= -75 -> WifiQuality.Razoavel
            else -> WifiQuality.Ruim
        }
    val spectrumOk = network.frequenciaMhz > 5000
    val radioOk = network.rssiDbm >= -65
    val channel = freqToChannel(network.frequenciaMhz)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.home_wifi_fatores_titulo), fontSize = 15.sp, fontWeight = FontWeight.W600, color = c.textPrimary)
            WifiQualityBadge(quality = overall, c = c)
        }
        Spacer(modifier = Modifier.height(LkSpacing.sm))
        HorizontalDivider(color = c.border, thickness = 1.dp)
        val spectrumDetails =
            buildList {
                add("Banda de Wi-Fi ${freqDisplay(network.frequenciaMhz)}")
                network.larguraCanalMhz?.takeIf { it >= 80 }?.let { add("Largura do canal $it MHz") }
            }
        FactorRow(
            title = stringResource(R.string.home_wifi_fator_espectro),
            status = if (spectrumOk) WifiQuality.Excelente else WifiQuality.Razoavel,
            details = spectrumDetails,
            c = c,
        )
        HorizontalDivider(color = c.border, thickness = 1.dp)
        FactorRow(
            title = stringResource(R.string.home_wifi_fator_radio),
            status = if (radioOk) WifiQuality.Excelente else WifiQuality.Razoavel,
            details = listOf("Sinal ${network.rssiDbm} dBm"),
            c = c,
        )
        HorizontalDivider(color = c.border, thickness = 1.dp)
        FactorRow(
            title = stringResource(R.string.home_wifi_fator_canal),
            status = WifiQuality.Bom,
            details = if (channel > 0) listOf("Canal $channel") else emptyList(),
            c = c,
        )
        HorizontalDivider(color = c.border, thickness = 1.dp)
    }
}

@Composable
private fun WifiQualityBadge(
    quality: WifiQuality,
    c: LkTokens,
) {
    val color =
        when (quality) {
            WifiQuality.Excelente -> LkColors.success
            WifiQuality.Bom -> LkColors.success.copy(alpha = 0.65f)
            WifiQuality.Razoavel -> LkColors.warning
            WifiQuality.Ruim -> LkColors.error
        }
    val label =
        when (quality) {
            WifiQuality.Excelente -> "Excelente"
            WifiQuality.Bom -> "Bom"
            WifiQuality.Razoavel -> "Razoável"
            WifiQuality.Ruim -> "Ruim"
        }
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = LkSpacing.md, vertical = LkSpacing.sm),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.W600, color = color)
    }
}

@Composable
private fun FactorRow(
    title: String,
    status: WifiQuality,
    details: List<String>,
    c: LkTokens,
) {
    val statusColor =
        when (status) {
            WifiQuality.Excelente -> LkColors.success
            WifiQuality.Bom -> LkColors.success.copy(alpha = 0.65f)
            WifiQuality.Razoavel -> LkColors.warning
            WifiQuality.Ruim -> LkColors.error
        }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = LkSpacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
                Modifier
                    .padding(top = LkSpacing.sm)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor),
        )
        Spacer(modifier = Modifier.width(LkSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W500, color = c.textPrimary)
            Spacer(modifier = Modifier.height(LkSpacing.xs))
            details.forEach { detail ->
                Text(detail, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
        }
    }
}

private fun freqToChannel(mhz: Int): Int =
    when {
        mhz in 2412..2484 -> (mhz - 2412) / 5 + 1
        mhz in 5170..5825 -> (mhz - 5000) / 5
        mhz >= 5945 -> (mhz - 5950) / 5 + 1
        else -> 0
    }

private fun freqDisplay(mhz: Int): String =
    when {
        mhz < 3000 -> "2.4 GHz"
        mhz < 6000 -> "5 GHz"
        else -> "6 GHz"
    }

// ─── Bottom sheets ────────────────────────────────────────────────────────────

private fun wifiSecurityLabel(s: SegurancaWifi): String =
    when (s) {
        SegurancaWifi.wpa3 -> "WPA3"
        SegurancaWifi.wpa2 -> "WPA2"
        SegurancaWifi.wpa -> "WPA"
        SegurancaWifi.wep -> "WEP (inseguro)"
        SegurancaWifi.aberta -> "Aberta (sem senha)"
        SegurancaWifi.desconhecida -> "Desconhecida"
    }

@Composable
private fun DeviceInfoSheet(
    localIp: String?,
    isMobile: Boolean,
    deviceName: String,
    connectedNetwork: RedeVizinha?,
    c: LkTokens,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.xl)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
    ) {
        SheetDragHandle(c)
        Spacer(modifier = Modifier.height(LkSpacing.xl))
        Text(
            stringResource(R.string.home_sheet_meu_dispositivo),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.W700,
            color = c.textPrimary,
        )
        Spacer(modifier = Modifier.height(LkSpacing.lg))
        SheetInfoRow("Modelo", deviceName, c)
        SheetInfoRow("Sistema", "Android", c)
        SheetInfoRow("IP Local", localIp?.takeIf { it.isNotEmpty() } ?: "N/A", c)
        SheetInfoRow("Tipo de conexão", if (isMobile) "Dados móveis" else "Wi-Fi", c)
        if (!isMobile && connectedNetwork != null) {
            connectedNetwork.canal?.let { SheetInfoRow("Canal Wi-Fi", "$it", c) }
            SheetInfoRow("Segurança", wifiSecurityLabel(connectedNetwork.seguranca), c)
        }
    }
}

/**
 * Retorna os últimos 3 octetos do BSSID em formato legível, ou null quando:
 * - a string é nula
 * - não tem exatamente 6 pares separados por ':'
 * - é o BSSID fake de privacidade Android ("02:00:00:00:00:00")
 */
internal fun bssidCurto(bssid: String?): String? {
    if (bssid.isNullOrBlank()) return null
    val partes = bssid.split(":")
    if (partes.size != 6 || partes.any { it.length != 2 }) return null
    if (bssid == "02:00:00:00:00:00") return null
    return partes.takeLast(3).joinToString(":")
}

@Composable
private fun GatewayInfoSheet(
    gateway: GatewayInfo,
    connectedNetwork: RedeVizinha?,
    c: LkTokens,
) {
    val typeLabel =
        when (gateway.type) {
            ConnectionNodeType.WifiRouter -> "Roteador Wi-Fi"
            ConnectionNodeType.WifiMesh -> "Rede Mesh"
            ConnectionNodeType.WifiExtender -> "Repetidor Wi-Fi"
            ConnectionNodeType.Mobile -> "Antena móvel"
            ConnectionNodeType.Unknown -> "Não identificado"
        }
    val isMeshOrExtensor =
        gateway.type == ConnectionNodeType.WifiMesh ||
            gateway.type == ConnectionNodeType.WifiExtender
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.xl)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
    ) {
        SheetDragHandle(c)
        Spacer(modifier = Modifier.height(LkSpacing.xl))
        Text(gateway.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.W700, color = c.textPrimary)
        Spacer(modifier = Modifier.height(LkSpacing.lg))
        SheetInfoRow("Tipo detectado", typeLabel, c)
        SheetInfoRow("IP do roteador", gateway.ip ?: "Não detectado", c)
        connectedNetwork?.let { net ->
            net.ssid?.let { SheetInfoRow("SSID", it, c) }
            if (isMeshOrExtensor) {
                bssidCurto(net.bssid)?.let { SheetInfoRow("Nó atual", it, c) }
            }
            SheetInfoRow("Sinal", "${net.rssiDbm} dBm", c)
            SheetInfoRow("Banda", freqDisplay(net.frequenciaMhz), c)
        }
        connectedNetwork?.canal?.let { SheetInfoRow("Canal", "$it", c) }
        connectedNetwork?.larguraCanalMhz?.let { SheetInfoRow("Largura de canal", "$it MHz", c) }
    }
}

private fun isCgNat(ip: String?): Boolean {
    val parts = ip?.split(".")?.mapNotNull { it.toIntOrNull() } ?: return false
    return parts.size == 4 && parts[0] == 100 && parts[1] in 64..127
}

@Composable
private fun InternetInfoSheet(
    publicIp: String?,
    ispInfo: IspInfo?,
    privateDnsAtivo: Boolean,
    privateDnsHostname: String?,
    dnsServidores: List<String>,
    c: LkTokens,
) {
    val countryRegion = listOfNotNull(ispInfo?.country, ispInfo?.region).joinToString(" / ")
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.xl)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
    ) {
        SheetDragHandle(c)
        Spacer(modifier = Modifier.height(LkSpacing.xl))
        Text(
            stringResource(R.string.home_sheet_internet),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.W700,
            color = c.textPrimary,
        )
        Spacer(modifier = Modifier.height(LkSpacing.lg))
        SheetInfoRow("IP Público", ispInfo?.ip ?: publicIp ?: "N/A", c)
        if (isCgNat(ispInfo?.ip ?: publicIp)) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.home_sheet_ip_compartilhado),
                style = MaterialTheme.typography.labelSmall,
                color = LkColors.warning,
                modifier = Modifier.padding(bottom = LkSpacing.md),
            )
        }
        ispInfo?.isp?.takeIf { it.isNotEmpty() }?.let { SheetInfoRow("Provedor", it, c) }
        ispInfo?.asn?.takeIf { it.isNotEmpty() }?.let { SheetInfoRow("ASN", it, c) }
        if (countryRegion.isNotEmpty()) SheetInfoRow("País / Região", countryRegion, c)
        val dnsPrivadoValor =
            if (privateDnsAtivo) {
                privateDnsHostname?.takeIf { it.isNotEmpty() } ?: "Ativo"
            } else {
                "Padrão do provedor"
            }
        SheetInfoRow("DNS Privado", dnsPrivadoValor, c, valueColor = if (privateDnsAtivo) LkColors.success else null)
        val servidoresDnsStr = dnsServidores.take(2).joinToString(" / ").takeIf { it.isNotEmpty() }
        servidoresDnsStr?.let { SheetInfoRow("Servidores DNS", it, c) }
    }
}

@Composable
private fun SignalQualitySheet(
    isWifi: Boolean,
    pct: Int,
    connectedNetwork: RedeVizinha?,
    linkSpeedMbps: Int?,
    gateways: List<GatewayInfo>,
    localIp: String?,
    publicIp: String?,
    ispInfo: IspInfo?,
    c: LkTokens,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp).navigationBarsPadding(),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SheetDragHandle(c)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (isWifi) "Detalhes da rede Wi-Fi" else "Detalhes da rede móvel",
            fontSize = 16.sp,
            fontWeight = FontWeight.W600,
            color = c.textPrimary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(20.dp))
        if (isWifi && connectedNetwork != null) {
            connectedNetwork.ssid?.let { SheetInfoRow("Nome da rede", it, c) }
            SheetInfoRow("Qualidade do sinal", "$pct%", c)
            SheetInfoRow("Força do sinal", "${connectedNetwork.rssiDbm} dBm", c)
            linkSpeedMbps?.let { SheetInfoRow("Velocidade de conexão", "$it Mbps", c) }
            SheetInfoRow("Frequência", "${connectedNetwork.frequenciaMhz} MHz", c)
            SheetInfoRow("Banda", freqDisplay(connectedNetwork.frequenciaMhz), c)
            val ch = freqToChannel(connectedNetwork.frequenciaMhz)
            if (ch > 0) SheetInfoRow("Canal", "$ch", c)
            connectedNetwork.larguraCanalMhz?.let { SheetInfoRow("Largura de canal", "$it MHz", c) }
            SheetInfoRow("Segurança", wifiSecurityLabel(connectedNetwork.seguranca), c)
            val gw = gateways.firstOrNull()
            gw?.ip?.let { SheetInfoRow("IP do roteador", it, c) }
            localIp?.let { SheetInfoRow("IP do dispositivo", it, c) }
        }
    }
}

// ─── Qualidade placeholder sheet ──────────────────────────────────────────────

@Composable
private fun QualidadePlaceholderSheet(c: LkTokens) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.xl)
                .padding(bottom = 48.dp)
                .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SheetDragHandle(c)
        Spacer(Modifier.height(LkSpacing.xxl))
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(LkColors.accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Insights,
                contentDescription = null,
                tint = LkColors.accent,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.home_qualidade_titulo),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = c.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(LkSpacing.sm))
        Text(
            text = stringResource(R.string.home_qualidade_em_breve),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.W600,
            color = LkColors.accent,
        )
        Spacer(Modifier.height(LkSpacing.lg))
        Text(
            text = stringResource(R.string.home_qualidade_descricao),
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
    }
}

// ─── Cellular info sheet ──────────────────────────────────────────────────────

@Composable
private fun CellularInfoSheet(
    ispInfo: IspInfo?,
    publicIp: String?,
    movelSnapshot: MovelSnapshot?,
    wifiAtivo: Boolean = false,
    c: LkTokens,
) {
    val operadora =
        movelSnapshot?.operadora?.takeIf { it.isNotBlank() }
            ?: ispInfo?.isp?.takeIf { it.isNotEmpty() }
    // IP público via HTTP usa a rede ativa do SO. Android prefere Wi-Fi sobre Móvel,
    // portanto quando Wi-Fi está ativo o IP retornado é da Wi-Fi, não da móvel.
    val ip = if (!wifiAtivo) ispInfo?.ip ?: publicIp else null
    val tec = movelSnapshot?.tecnologia?.ifBlank { null }
    val banda = movelSnapshot?.bandaMovel?.ifBlank { null }
    val rsrp = movelSnapshot?.rsrpDbm
    val sinr = movelSnapshot?.sinrDb
    val mcc = movelSnapshot?.mcc
    val mnc = movelSnapshot?.mnc

    val qualityLabel = rsrp?.let { "${mobileSignalQuality(it)} ($it dBm)" }
    val qualityColor = rsrp?.let { mobileSignalColor(it) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.xl)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
    ) {
        SheetDragHandle(c)
        Spacer(Modifier.height(LkSpacing.xl))

        Text(
            "Rede móvel",
            fontSize = 20.sp,
            fontWeight = FontWeight.W700,
            color = c.textPrimary,
        )
        Text(
            "Detalhes da conexão móvel ativa",
            fontSize = 13.sp,
            color = c.textTertiary,
        )

        Spacer(Modifier.height(LkSpacing.lg))

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(c.bgCard)
                    .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                    .padding(LkSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(LkColors.accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.SignalCellularAlt,
                    contentDescription = null,
                    tint = LkColors.accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(LkSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    operadora ?: "Rede móvel",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                )
                val heroSub =
                    listOfNotNull(tec, banda?.let { "banda $it" })
                        .joinToString(" · ")
                        .takeIf { it.isNotEmpty() }
                if (heroSub != null) {
                    Text(heroSub, fontSize = 12.sp, color = c.textSecondary)
                }
            }
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(LkColors.success.copy(alpha = 0.10f))
                        .padding(horizontal = LkSpacing.sm, vertical = 2.dp),
            ) {
                Text(
                    "Conectado",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W700,
                    color = LkColors.success,
                )
            }
        }

        Spacer(Modifier.height(LkSpacing.lg))

        tec?.let { SheetInfoRow("Tecnologia", it, c) }
        operadora?.let { SheetInfoRow("Operadora", it, c) }
        ip?.let { SheetInfoRow("IP público", it, c) }
        qualityLabel?.let { SheetInfoRow("Qualidade do sinal", it, c, valueColor = qualityColor) }
        rsrp?.let {
            val asu = it + 140
            SheetInfoRow("ASU", "$asu", c)
        }
        sinr?.let { s ->
            val sinrColor =
                when {
                    s > 10 -> LkColors.success
                    s > 0 -> LkColors.warning
                    else -> LkColors.error
                }
            SheetInfoRow("SINR", "$s dB", c, valueColor = sinrColor)
        }
        SheetInfoRow(
            "Roaming",
            if (movelSnapshot?.roaming == true) "Sim" else "Não",
            c,
            valueColor = if (movelSnapshot?.roaming == true) LkColors.warning else null,
        )
        if (mcc != null && mnc != null) {
            SheetInfoRow("MCC / MNC", "$mcc / $mnc", c)
        }

        Spacer(Modifier.height(LkSpacing.md))
        Text(
            "Teste de velocidade pode consumir uma parcela significativa do seu plano de dados.",
            fontSize = 11.sp,
            color = c.textTertiary,
            lineHeight = 15.sp,
        )

        if (ip == null && operadora == null && movelSnapshot == null) {
            Spacer(Modifier.height(LkSpacing.lg))
            Text(
                text = stringResource(R.string.home_sheet_movel_indisponivel),
                style = MaterialTheme.typography.titleSmall,
                color = c.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            )
        }
    }
}

// ─── Sheet helpers ────────────────────────────────────────────────────────────

@Composable
private fun SheetDragHandle(c: LkTokens) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(c.border),
        )
    }
}

@Composable
private fun SheetInfoRow(
    key: String,
    value: String,
    c: LkTokens,
    valueColor: Color? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = LkSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, modifier = Modifier.padding(end = LkSpacing.lg))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.W600,
            color = valueColor ?: c.textPrimary,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

// ─── Gamer shortcut card ──────────────────────────────────────────────────────

@Composable
internal fun GamerShortcutCard(
    c: LkTokens,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .clickable(onClick = onClick)
                .padding(LkSpacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LkColors.success.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.SportsEsports,
                    contentDescription = null,
                    tint = LkColors.success,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(LkSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_shortcut_gaming_titulo),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                )
                Text(
                    stringResource(R.string.home_shortcut_gaming_descricao),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = c.textTertiary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

// ─── GamerSheet ───────────────────────────────────────────────────────────────

@Composable
internal fun GamerSheet(
    resultado: ResultadoSpeedtest?,
    ultimaMedicao: MedicaoEntity?,
    c: LkTokens,
    onIrParaTeste: () -> Unit,
) {
    val lat = resultado?.latenciaMs ?: ultimaMedicao?.latencyMs
    val jit = resultado?.jitterMs ?: ultimaMedicao?.jitterMs
    val loss = resultado?.perdaPercentual ?: ultimaMedicao?.perdaPercentual
    val dl = resultado?.downloadMbps ?: ultimaMedicao?.downloadMbps
    val ul = resultado?.uploadMbps ?: ultimaMedicao?.uploadMbps
    val temDados = lat != null && jit != null && loss != null && dl != null && ul != null
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
    ) {
        SheetDragHandle(c)
        Spacer(Modifier.height(20.dp))

        // Header
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(LkRadius.button))
                        .background(LkColors.success.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.SportsEsports, null, tint = LkColors.success, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    stringResource(R.string.home_shortcut_gaming_titulo),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.W700,
                    color = c.textPrimary,
                )
                Text(
                    stringResource(R.string.home_shortcut_gaming_descricao),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = c.border)

        if (!temDados) {
            // Empty state
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Outlined.SportsEsports, null, tint = c.textTertiary, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.home_gamer_sem_medicao), style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.home_gamer_sem_medicao_descricao),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.textSecondary,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onIrParaTeste,
                    colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.home_gamer_btn_iniciar_teste)) }
            }
        } else {
            val latV = checkNotNull(lat) { "lat deve ser non-null quando temDados=true" }
            val jitV = checkNotNull(jit) { "jit deve ser non-null quando temDados=true" }
            val lossV = checkNotNull(loss) { "loss deve ser non-null quando temDados=true" }
            val dlV = checkNotNull(dl) { "dl deve ser non-null quando temDados=true" }
            val ulV = checkNotNull(ul) { "ul deve ser non-null quando temDados=true" }

            val vereditoGamer =
                resultado?.diagnosticoQualidade?.vereditoGamer
                    ?: when {
                        latV <= 50 && jitV <= 15 && lossV <= 0.5 -> VereditoUso.good
                        latV <= 100 && jitV <= 30 && lossV <= 1.5 -> VereditoUso.acceptable
                        else -> VereditoUso.poor
                    }
            val gargalo =
                resultado?.diagnosticoQualidade?.gargaloPrimario
                    ?: ultimaMedicao?.gargaloPrimario?.let { s ->
                        GargaloPrimario.entries.firstOrNull { it.name == s } ?: GargaloPrimario.none
                    }
                    ?: GargaloPrimario.none

            // Verdict card
            Spacer(Modifier.height(LkSpacing.md))
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                GamerVeredictCard(veredito = vereditoGamer, c = c)
            }
            Spacer(Modifier.height(LkSpacing.lg))

            // Metrics section
            Text(
                stringResource(R.string.home_gamer_secao_metricas),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.W700,
                color = c.textTertiary,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 20.dp, bottom = 4.dp),
            )
            GamerMetricRow(stringResource(R.string.home_gamer_metrica_latencia), "${latV.roundToInt()} ms", vereditoFromLatency(latV), c)
            HorizontalDivider(color = c.border.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
            GamerMetricRow(stringResource(R.string.home_gamer_metrica_oscilacao), "${jitV.roundToInt()} ms", vereditoFromJitter(jitV), c)
            HorizontalDivider(color = c.border.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
            GamerMetricRow(stringResource(R.string.home_gamer_metrica_perda), "%.1f%%".format(lossV), vereditoFromLoss(lossV), c)
            HorizontalDivider(color = c.border.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
            GamerMetricRow(
                stringResource(R.string.home_gamer_metrica_download),
                "%.0f Mbps".format(dlV),
                if (dlV >=
                    10
                ) {
                    VereditoUso.good
                } else if (dlV >= 5) {
                    VereditoUso.acceptable
                } else {
                    VereditoUso.poor
                },
                c,
            )
            HorizontalDivider(color = c.border.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
            GamerMetricRow(
                stringResource(R.string.home_gamer_metrica_upload),
                "%.0f Mbps".format(ulV),
                if (ulV >= 3) {
                    VereditoUso.good
                } else if (ulV >= 1) {
                    VereditoUso.acceptable
                } else {
                    VereditoUso.poor
                },
                c,
            )

            Spacer(Modifier.height(LkSpacing.lg))
            HorizontalDivider(color = c.border)

            // Games table
            Text(
                stringResource(R.string.home_gamer_secao_jogos),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.W700,
                color = c.textTertiary,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 20.dp, top = LkSpacing.md, bottom = 4.dp),
            )
            val jogos =
                listOf(
                    Triple("VALORANT", Icons.Outlined.Adjust, avaliarJogo(latV, jitV, lossV, 50, 80, 12, 25, 0.3, 0.8)),
                    Triple("CS2", Icons.Outlined.Shield, avaliarJogo(latV, jitV, lossV, 50, 80, 12, 25, 0.3, 0.8)),
                    Triple("CoD Warzone", Icons.Outlined.GpsFixed, avaliarJogo(latV, jitV, lossV, 70, 100, 20, 35, 0.5, 1.0)),
                    Triple("Fortnite", Icons.Outlined.Construction, avaliarJogo(latV, jitV, lossV, 70, 100, 20, 35, 0.5, 1.0)),
                    Triple("Free Fire", Icons.Outlined.LocalFireDepartment, avaliarJogo(latV, jitV, lossV, 90, 130, 25, 40, 0.8, 1.5)),
                    Triple("Minecraft", Icons.Outlined.Public, avaliarJogo(latV, jitV, lossV, 120, 180, 35, 60, 1.0, 2.0)),
                )
            jogos.forEachIndexed { i, (nome, icon, veredito) ->
                GamerJogoRow(nome = nome, icon = icon, veredito = veredito, c = c)
                if (i < jogos.lastIndex) {
                    HorizontalDivider(
                        color = c.border.copy(alpha = 0.5f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }

            // Gargalo note
            if (gargalo != GargaloPrimario.none) {
                Spacer(Modifier.height(LkSpacing.md))
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    GargaloGamerNote(gargalo = gargalo, c = c)
                }
            }
            Spacer(Modifier.height(LkSpacing.md))
        }
    }
}

@Composable
private fun GamerVeredictCard(
    veredito: VereditoUso,
    c: LkTokens,
) {
    val (label, desc, cor) =
        when (veredito) {
            VereditoUso.good ->
                Triple(
                    stringResource(R.string.home_gamer_veredito_otimo_label),
                    stringResource(R.string.home_gamer_veredito_otimo_desc),
                    LkColors.success,
                )
            VereditoUso.acceptable ->
                Triple(
                    stringResource(R.string.home_gamer_veredito_bom_label),
                    stringResource(R.string.home_gamer_veredito_bom_desc),
                    LkColors.warning,
                )
            VereditoUso.poor ->
                Triple(
                    stringResource(R.string.home_gamer_veredito_ruim_label),
                    stringResource(R.string.home_gamer_veredito_ruim_desc),
                    LkColors.error,
                )
        }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.button))
                .background(cor.copy(alpha = 0.10f))
                .border(1.dp, cor.copy(alpha = 0.30f), RoundedCornerShape(LkRadius.button))
                .padding(LkSpacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(cor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.SportsEsports, null, tint = cor, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(LkSpacing.md))
            Column {
                Text(label, fontSize = 20.sp, fontWeight = FontWeight.W800, color = cor)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
        }
    }
}

@Composable
private fun GamerMetricRow(
    label: String,
    value: String,
    veredito: VereditoUso,
    c: LkTokens,
) {
    val cor =
        when (veredito) {
            VereditoUso.good -> LkColors.success
            VereditoUso.acceptable -> LkColors.warning
            VereditoUso.poor -> LkColors.error
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = LkSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W600, color = c.textSecondary)
        Spacer(Modifier.width(LkSpacing.sm))
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(cor.copy(alpha = 0.12f))
                    .padding(horizontal = LkSpacing.sm, vertical = LkSpacing.xs),
        ) {
            Text(
                when (veredito) {
                    VereditoUso.good -> stringResource(R.string.home_status_ok)
                    VereditoUso.acceptable -> stringResource(R.string.home_status_regular)
                    else -> stringResource(R.string.home_status_ruim)
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.W600,
                color = cor,
            )
        }
    }
}

@Composable
private fun GamerJogoRow(
    nome: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    veredito: VereditoUso,
    c: LkTokens,
) {
    val cor =
        when (veredito) {
            VereditoUso.good -> LkColors.success
            VereditoUso.acceptable -> LkColors.warning
            VereditoUso.poor -> LkColors.error
        }
    val label =
        when (veredito) {
            VereditoUso.good -> stringResource(R.string.home_gamer_status_otimo)
            VereditoUso.acceptable -> stringResource(R.string.home_gamer_status_jogavel)
            else -> stringResource(R.string.home_status_ruim)
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = LkSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(LkSpacing.sm))
        Text(nome, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary, modifier = Modifier.weight(1f))
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(cor.copy(alpha = 0.12f))
                    .padding(horizontal = LkSpacing.sm, vertical = LkSpacing.xs),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.W600, color = cor)
        }
    }
}

@Composable
private fun GargaloGamerNote(
    gargalo: GargaloPrimario,
    c: LkTokens,
) {
    val texto =
        when (gargalo) {
            GargaloPrimario.latency -> stringResource(R.string.home_gamer_gargalo_latencia)
            GargaloPrimario.packetLoss -> stringResource(R.string.home_gamer_gargalo_perda)
            GargaloPrimario.bufferbloat -> stringResource(R.string.home_gamer_gargalo_bufferbloat)
            GargaloPrimario.upload -> stringResource(R.string.home_gamer_gargalo_upload)
            GargaloPrimario.none -> return
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(LkColors.warning.copy(alpha = 0.08f))
                .border(1.dp, LkColors.warning.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                .padding(LkSpacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Outlined.Insights, null, tint = LkColors.warning, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(LkSpacing.sm))
        Text(texto, style = MaterialTheme.typography.bodySmall, color = c.textPrimary, lineHeight = 16.sp)
    }
}

private fun avaliarJogo(
    latenciaMs: Double,
    jitterMs: Double,
    lossPercent: Double,
    latOtimo: Int,
    latBom: Int,
    jitOtimo: Int,
    jitBom: Int,
    lossOtimo: Double,
    lossBom: Double,
): VereditoUso =
    when {
        latenciaMs <= latOtimo && jitterMs <= jitOtimo && lossPercent <= lossOtimo -> VereditoUso.good
        latenciaMs <= latBom && jitterMs <= jitBom && lossPercent <= lossBom -> VereditoUso.acceptable
        else -> VereditoUso.poor
    }

private fun vereditoFromLatency(ms: Double): VereditoUso =
    if (ms <= 50) {
        VereditoUso.good
    } else if (ms <= 100) {
        VereditoUso.acceptable
    } else {
        VereditoUso.poor
    }

private fun vereditoFromJitter(ms: Double): VereditoUso =
    if (ms <= 15) {
        VereditoUso.good
    } else if (ms <= 30) {
        VereditoUso.acceptable
    } else {
        VereditoUso.poor
    }

private fun vereditoFromLoss(pct: Double): VereditoUso =
    if (pct <= 0.5) {
        VereditoUso.good
    } else if (pct <= 1.5) {
        VereditoUso.acceptable
    } else {
        VereditoUso.poor
    }

// ─── MedicaoTipoSheet ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicaoTipoSheet(
    isOnWifi: Boolean,
    onDismiss: () -> Unit,
    onIniciarTeste: (ModoSpeedtest) -> Unit,
    c: LkTokens,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.bgSecondary,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LkSpacing.lg)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            Text(
                "Tipo de medição",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W700,
                color = c.textPrimary,
            )
            Text(
                "Escolha como quer medir sua conexão",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
            Spacer(modifier = Modifier.height(LkSpacing.sm))

            MedicaoOpcaoItem(
                icon = Icons.Outlined.Speed,
                titulo = "Rápido",
                descricao = "Somente download · ~30 seg",
                badge = null,
                disponivel = true,
                c = c,
                onClick = { onIniciarTeste(ModoSpeedtest.fast) },
            )

            MedicaoOpcaoItem(
                icon = Icons.Outlined.Adjust,
                titulo = "Completo",
                descricao = "Download e upload · ~90 seg",
                badge = "Recomendado",
                badgeColor = LkColors.accent,
                disponivel = true,
                c = c,
                onClick = { onIniciarTeste(ModoSpeedtest.complete) },
            )

            MedicaoOpcaoItem(
                icon = Icons.Outlined.Refresh,
                titulo = "Triplo",
                descricao = "Média de 3 testes consecutivos · ~3 min",
                badge = if (!isOnWifi) "Só Wi-Fi" else null,
                badgeColor = c.textTertiary,
                disponivel = isOnWifi,
                c = c,
                onClick = { onIniciarTeste(ModoSpeedtest.triplo) },
            )

            Spacer(modifier = Modifier.height(LkSpacing.xs))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancelar", color = c.textSecondary)
            }
        }
    }
}

@Composable
private fun MedicaoOpcaoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    descricao: String,
    badge: String?,
    badgeColor: Color = LkColors.accent,
    disponivel: Boolean,
    c: LkTokens,
    onClick: () -> Unit,
) {
    val textColor = if (disponivel) c.textPrimary else c.textTertiary
    val subTextColor = if (disponivel) c.textSecondary else c.textTertiary
    val iconColor = if (disponivel) LkColors.accent else c.textTertiary

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .then(
                    if (disponivel) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    },
                ).padding(LkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                titulo,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W600,
                color = textColor,
            )
            Text(
                descricao,
                style = MaterialTheme.typography.bodySmall,
                color = subTextColor,
            )
        }
        if (badge != null) {
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(badgeColor.copy(alpha = 0.12f))
                        .padding(horizontal = LkSpacing.sm, vertical = LkSpacing.xs),
            ) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.W600,
                    color = badgeColor,
                )
            }
        }
    }
}

// ─── ConnectionContextCard (bloco 1 adaptativo) ───────────────────────────────

@Composable
private fun ConnectionContextCard(
    snapshotRede: SnapshotRede,
    movelSnapshot: MovelSnapshot?,
    onMedirVelocidade: () -> Unit,
    onVerHistorico: () -> Unit,
    context: Context,
) {
    val c = LocalLkTokens.current

    when (snapshotRede.estadoConexao) {
        EstadoConexao.wifi -> {
            val rssiDbm = snapshotRede.wifiLinkSnapshot?.rssiDbm
            val wifiPct = rssiDbm?.let { ((it + 90) / 50.0).coerceIn(0.0, 1.0) * 100 }?.roundToInt()
            val ssid =
                snapshotRede.wifiLinkSnapshot
                    ?.ssid
                    ?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.home_context_wifi_ssid_desconhecido)
            val qualidadeColor =
                when {
                    wifiPct == null -> LkColors.accent
                    wifiPct >= 70 -> LkColors.success
                    wifiPct >= 40 -> LkColors.warning
                    else -> LkColors.error
                }

            SignallQCard(c) {
                Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LkColors.accent.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Wifi,
                                contentDescription = null,
                                tint = LkColors.accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Column {
                            Text(
                                stringResource(R.string.home_context_wifi_titulo),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.W600,
                                color = c.textPrimary,
                            )
                            Text(
                                ssid,
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (wifiPct != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                        ) {
                            LinearProgressIndicator(
                                progress = { wifiPct / 100f },
                                modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(999.dp)),
                                color = qualidadeColor,
                                trackColor = c.border,
                            )
                            Text(
                                "$wifiPct%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.W600,
                                color = qualidadeColor,
                            )
                        }
                    }
                    Button(
                        onClick = onMedirVelocidade,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(LkRadius.card),
                        colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        Text(
                            stringResource(R.string.home_btn_medir_velocidade),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.W600,
                        )
                    }
                }
            }
        }

        EstadoConexao.movel -> {
            val operadora =
                movelSnapshot
                    ?.operadora
                    ?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.home_context_movel_operadora_desconhecida)
            val tecnologia = movelSnapshot?.tecnologia

            SignallQCard(c) {
                Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(LkColors.accent.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SignalCellularAlt,
                                contentDescription = null,
                                tint = LkColors.accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Column {
                            Text(
                                stringResource(R.string.home_context_movel_titulo),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.W600,
                                color = c.textPrimary,
                            )
                            Text(
                                buildString {
                                    append(operadora)
                                    if (tecnologia != null) append(" · $tecnologia")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Button(
                        onClick = onMedirVelocidade,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(LkRadius.card),
                        colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                        contentPadding = PaddingValues(vertical = 12.dp),
                    ) {
                        Text(
                            stringResource(R.string.home_btn_medir_velocidade),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.W600,
                        )
                    }
                }
            }
        }

        else -> {
            // Offline / desconhecido — comportamento original do OfflineCard
            var registrado by remember { mutableStateOf(false) }

            DisposableEffect(Unit) {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val cb =
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            if (registrado) onMedirVelocidade()
                        }
                    }
                cm.registerNetworkCallback(NetworkRequest.Builder().build(), cb)
                onDispose { cm.unregisterNetworkCallback(cb) }
            }

            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, LkColors.warning.copy(alpha = 0.50f), RoundedCornerShape(LkRadius.card)),
                colors = CardDefaults.cardColors(containerColor = LkColors.warning.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(LkRadius.card),
            ) {
                Column(
                    modifier = Modifier.padding(LkSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WifiOff,
                            contentDescription = null,
                            tint = LkColors.warning,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            stringResource(R.string.home_offline_titulo),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.W600,
                            color = c.textPrimary,
                        )
                    }
                    Text(
                        stringResource(R.string.home_offline_descricao),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                    OutlinedButton(
                        onClick = { registrado = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !registrado,
                    ) {
                        Text(
                            if (registrado) {
                                stringResource(R.string.home_offline_btn_aguardar)
                            } else {
                                stringResource(R.string.home_offline_btn_testar_quando_voltar)
                            },
                        )
                    }
                    OutlinedButton(
                        onClick = onVerHistorico,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.home_offline_btn_ver_historico))
                    }
                }
            }
        }
    }
}
