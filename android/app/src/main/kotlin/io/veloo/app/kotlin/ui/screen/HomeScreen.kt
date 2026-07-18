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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.outlined.AirplanemodeActive
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
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
import io.signallq.app.core.network.contracts.gateway.GatewayConnectionResultado
import io.signallq.app.core.network.contracts.gateway.GatewayConnectionService
import io.signallq.app.core.network.contracts.topologia.NivelConfianca
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
import io.signallq.app.ui.OperadoraSource
import io.signallq.app.ui.ResolvedOperadoraIdentity
import io.signallq.app.ui.component.LkSheetDivider
import io.signallq.app.ui.component.LkSheetFrame
import io.signallq.app.ui.component.LkSheetInfoRow
import io.signallq.app.ui.component.LkSheetSectionTitle
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.OperadoraBadge
import io.signallq.app.ui.component.ProfileAvatarButton
import io.signallq.app.ui.component.SheetDragHandle
import io.signallq.app.ui.component.rememberResolvedOperadoraIdentity
import java.util.concurrent.TimeUnit
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

/**
 * "5G SA"/"5G NSA" → "5G" para texto voltado ao usuário leigo — NSA/SA é jargão de operadora,
 * não informação acionável (GH#515, ver `movelSnapshot.tecnologia`/[MovelSnapshot]).
 */
internal fun tecnologiaSimplificada(tec: String?): String? = tec?.ifBlank { null }?.substringBefore(" ")

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
    // GH#530 — reuso da GatewayConnectionSheet no nó do gateway (roteador) da trilha.
    // Sessão válida (BSSID atual == BSSID salvo com "manter conectado") pula a sheet.
    gatewaySessaoValida: Boolean = false,
    conectarGateway: GatewayConnectionService = GatewayConnectionService { _, _, _ -> GatewayConnectionResultado.Sucesso },
    modemUsername: String = "",
    modemPassword: String = "",
    modemPermanecerConectado: Boolean = false,
    onAbrirGatewayDetalhe: () -> Unit = {},
    onGatewayConectado: (ip: String, usuario: String, senha: String, lembrarSenha: Boolean, manterConectado: Boolean) -> Unit =
        { _, _, _, _, _ -> },
    anatelBannerDismissed: Boolean,
    onDismissAnatelBanner: () -> Unit,
    onIniciarTeste: (ModoSpeedtest) -> Unit,
    onAbrirHistorico: () -> Unit,
    onAbrirPerfil: () -> Unit,
    onAbrirRedes: () -> Unit,
    /** GH#970 — resolucao de identidade de operadora (nivel 1, catalogo local, sincrono).
     *  Sem I/O, sem corrotina — mesmo comportamento de sempre pras ~12 operadoras principais. */
    resolveOperadoraIdentidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity? =
        { _, _ -> null },
    /** GH#970 — cadeia completa (local -> diretorio remoto do worker signallq-diagnostic ->
     *  fallback generico), so chamada quando o nivel 1 acima nao encontrou. */
    resolveOperadoraIdentidadeRemota: suspend (String?, Boolean) -> ResolvedOperadoraIdentity =
        { nome, _ ->
            ResolvedOperadoraIdentity(
                displayName = nome ?: "Operadora",
                monograma = nome?.firstOrNull()?.uppercase() ?: "?",
                corMarca = null,
                logoRes = null,
                logoUrl = null,
                source = OperadoraSource.FALLBACK,
            )
        },
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
    // GH#530 — GatewayConnectionSheet (roteador/GPON) do nó do gateway na trilha. IP separado
    // de "visível" porque um gateway detectado pode legitimamente ter ip == null.
    var showGatewayConnectionSheet by remember { mutableStateOf(false) }
    var gatewayConnectionIp by remember { mutableStateOf<String?>(null) }

    if (showDeviceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDeviceSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.bgSecondary,
        ) {
            DeviceInfoSheet(localIp = localIp, isMobile = !isOnWifi, deviceName = deviceName, c = c)
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
                linkSpeedMbps = linkSpeedMbps,
            )
        }
    }
    if (showGatewayConnectionSheet) {
        GatewayConnectionSheet(
            ipInicial = gatewayConnectionIp,
            usuarioInicial = modemUsername,
            senhaInicial = modemPassword,
            lembrarSenhaInicial = modemUsername.isNotBlank() || modemPassword.isNotBlank(),
            manterConectadoInicial = modemPermanecerConectado,
            onDismissRequest = { showGatewayConnectionSheet = false },
            conectar = conectarGateway,
            onConectado = onGatewayConectado,
        )
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

    val profileBrush = remember { Brush.linearGradient(colors = listOf(c.primary, LkColors.accentBlue)) }
    val listState = rememberLazyListState()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val estaConectado =
                        snapshotRede.estadoConexao == EstadoConexao.wifi ||
                            snapshotRede.estadoConexao == EstadoConexao.movel
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
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
                    resolveOperadoraIdentidadeLocal = resolveOperadoraIdentidadeLocal,
                    resolveOperadoraIdentidadeRemota = resolveOperadoraIdentidadeRemota,
                    onDeviceTap = { showDeviceSheet = true },
                    onGatewayTap = { gw ->
                        when {
                            gw.type == ConnectionNodeType.Mobile -> showCellularSheet = true
                            // GH#530: nó do roteador — sessão válida pula a sheet e vai direto
                            // ao destino provisório; sem sessão abre a GatewayConnectionSheet.
                            // Mesh/extensor/desconhecido seguem no GatewayInfoSheet somente-leitura.
                            gw.type == ConnectionNodeType.WifiRouter -> {
                                if (gatewaySessaoValida) {
                                    onAbrirGatewayDetalhe()
                                } else {
                                    gatewayConnectionIp = gw.ip
                                    showGatewayConnectionSheet = true
                                }
                            }
                            else -> showGatewaySheet = gw
                        }
                    },
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
                    onIniciarTeste = { showMedicaoTipoSheet = true },
                    c = c,
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
                    )
                }
            }

            if (movelSnapshot != null || simsAtivos.isNotEmpty()) {
                item {
                    MobileChipsCard(
                        movelSnapshot = movelSnapshot,
                        simsAtivos = simsAtivos,
                        c = c,
                        onTap = { showCellularSheet = true },
                        resolveOperadoraIdentidadeLocal = resolveOperadoraIdentidadeLocal,
                        resolveOperadoraIdentidadeRemota = resolveOperadoraIdentidadeRemota,
                    )
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
    onIniciarTeste: () -> Unit,
    c: LkTokens,
) {
    SignallQCard(c) {
        // GH: card inteiro nao navegava para o Historico -- onAbrirHistorico chegava como
        // parametro mas nunca era usado. Card clicavel; os botoes internos (onIniciarTeste)
        // consomem o toque antes de propagar, entao continuam funcionando normalmente.
        Column(modifier = Modifier.clickable(onClick = onAbrirHistorico)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.xs)) {
                    Text(
                        text = stringResource(R.string.home_medicoes_titulo).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.W600,
                        color = c.textTertiary,
                    )
                    effectiveTs?.let {
                        Text(
                            formatRelativeTimestamp(it),
                            style = MaterialTheme.typography.labelMedium,
                            color = c.textTertiary,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(LkSpacing.lg))
            if (hasEffectiveResult && effectiveTs != null && effectiveDl != null && effectiveUl != null) {
                LastResultHero(
                    downloadMbps = effectiveDl,
                    uploadMbps = effectiveUl,
                    c = c,
                )
                Spacer(modifier = Modifier.height(LkSpacing.lg))
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
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
                colors = ButtonDefaults.buttonColors(containerColor = c.primary),
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
                    c.success,
                    c.success.copy(alpha = 0.12f),
                )
            bufferbloatMs <= 30.0 ->
                Triple(
                    stringResource(R.string.home_atraso_extra_aceitavel),
                    c.warning,
                    c.warning.copy(alpha = 0.12f),
                )
            else ->
                Triple(
                    stringResource(R.string.home_atraso_extra_alto),
                    c.error,
                    c.error.copy(alpha = 0.12f),
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
                tint = c.primary,
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
                            .clip(RoundedCornerShape(LkRadius.pill))
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
    resolveOperadoraIdentidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity?,
    resolveOperadoraIdentidadeRemota: suspend (String?, Boolean) -> ResolvedOperadoraIdentity,
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

    val isMobileConnection = snapshotRede.estadoConexao == EstadoConexao.movel
    val mobileGateway = gateways.singleOrNull { it.type == ConnectionNodeType.Mobile }

    SignallQCard(c) {
        Column {
            Text(
                text = "CAMINHO DA SUA INTERNET",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.W600,
                color = c.textTertiary,
            )
            Spacer(modifier = Modifier.height(LkSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    PathNode(
                        icon = Icons.Outlined.Smartphone,
                        iconColor = LkColors.success,
                        label = "Seu aparelho",
                        subLabel =
                            when {
                                localIp != null -> stringResource(R.string.home_network_conectado)
                                isConectado -> stringResource(R.string.home_network_buscando)
                                else -> stringResource(R.string.home_network_desconectado)
                            },
                        subLabelColor = if (hasLocalError) LkColors.error else null,
                        c = c,
                        onTap = onDeviceTap,
                    )
                }
                if (isMobileConnection && mobileGateway != null) {
                    PathConnector(
                        c = c,
                        active = !hasLocalError && !loadingLocal && !hasInternetError && !loadingInternet,
                        hasError = hasLocalError || hasInternetError,
                        loading = loadingLocal || loadingInternet,
                    )
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        val nomeOperadora =
                            movelSnapshot?.operadora?.ifBlank { null }
                                ?: ispInfo?.isp?.ifBlank { null }
                                ?: mobileGateway.name.takeIf { it != "Antena movel" && it != "Antena móvel" }
                        val identidadeOperadora =
                            rememberResolvedOperadoraIdentity(
                                ispNomeBruto = nomeOperadora,
                                viaMovel = true,
                                resolveLocal = resolveOperadoraIdentidadeLocal,
                                resolveRemoteOrFallback = resolveOperadoraIdentidadeRemota,
                            )
                        val nodeLabel = nomeOperadora ?: "Operadora"
                        val tec = tecnologiaSimplificada(movelSnapshot?.tecnologia)
                        val statusLabel =
                            when {
                                hasInternetError -> stringResource(R.string.home_network_sem_conexao)
                                loadingInternet -> stringResource(R.string.home_network_conectando)
                                else -> stringResource(R.string.home_network_conectado)
                            }
                        val nodeSubLabel = if (tec != null && !hasInternetError) "$tec · $statusLabel" else statusLabel
                        PathNode(
                            icon = Icons.Outlined.CellTower,
                            iconColor = if (hasInternetError) c.textTertiary else c.textSecondary,
                            label = nodeLabel,
                            subLabel = nodeSubLabel,
                            subLabelColor = if (hasInternetError) LkColors.error else null,
                            c = c,
                            onTap = { onGatewayTap(mobileGateway) },
                            iconContent =
                                identidadeOperadora?.let { identidade ->
                                    { OperadoraBadge(identidade = identidade, size = 34.dp) }
                                },
                        )
                    }
                } else {
                    gateways.forEachIndexed { i, gw ->
                        PathConnector(
                            c = c,
                            active = if (i == 0) !hasLocalError && !loadingLocal else true,
                            hasError = i == 0 && hasLocalError,
                            loading = i == 0 && loadingLocal,
                        )
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            val (icon, _) = nodeDisplay(gw.type)
                            val gatewayConectado = gw.ip != null
                            PathNode(
                                icon = icon,
                                iconColor = if (gatewayConectado) LkColors.success else c.textSecondary,
                                label = "Roteador",
                                subLabel = if (gatewayConectado) stringResource(R.string.home_network_conectado) else "—",
                                subLabelColor = if (gatewayConectado) LkColors.success else null,
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
                        val internetLabel =
                            ispInfo?.isp?.takeIf { it.isNotEmpty() }
                                ?: ispName?.takeIf { it.isNotEmpty() }
                                ?: "Provedor"
                        val identidadeInternet =
                            rememberResolvedOperadoraIdentity(
                                ispNomeBruto = ispInfo?.isp ?: ispName,
                                viaMovel = false,
                                resolveLocal = resolveOperadoraIdentidadeLocal,
                                resolveRemoteOrFallback = resolveOperadoraIdentidadeRemota,
                            )
                        val internetSubLabel =
                            if ((ispInfo?.ip ?: publicIp) != null) {
                                stringResource(R.string.home_network_conectado)
                            } else {
                                when {
                                    hasInternetError -> stringResource(R.string.home_network_sem_conexao)
                                    isIspInfoLoading -> stringResource(R.string.home_network_conectando)
                                    else -> "IP indisponível"
                                }
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
                            iconContent =
                                identidadeInternet?.let { identidade ->
                                    { OperadoraBadge(identidade = identidade, size = 34.dp) }
                                },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(LkSpacing.md))
            Text(
                text =
                    if (!hasLocalError && !hasInternetError && !loadingLocal && !loadingInternet) {
                        "Tudo conectado. Sua conexão chega até o provedor sem falhas."
                    } else if (loadingLocal || loadingInternet) {
                        "Estamos confirmando cada etapa da sua conexão."
                    } else {
                        "Há uma falha em um dos pontos da sua conexão."
                    },
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
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
    iconContent: (@Composable () -> Unit)? = null,
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
        Box(
            modifier =
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f))
                    .border(1.dp, iconColor.copy(alpha = 0.30f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (iconContent != null) {
                iconContent()
            } else {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
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
            loading -> c.primary.copy(alpha = animatedAlpha)
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
                .size(width = 28.dp, height = 56.dp)
                .then(semanticModifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .height(2.dp),
        ) {
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
                drawLine(
                    brush = Brush.horizontalGradient(listOf(LkColors.success, c.primary)),
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 2.dp.toPx(),
                )
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
    downloadMbps: Double,
    uploadMbps: Double,
    c: LkTokens,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.lg),
    ) {
        HeroMetric(
            label = "Download",
            value = downloadMbps,
            color = LkColors.success,
            modifier = Modifier.weight(1f),
        )
        HeroMetric(
            label = "Upload",
            value = uploadMbps,
            color = c.primary,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Faixas absolutas de qualidade de velocidade, sem relacao ao plano contratado do
 * usuario (veredito relativo ao plano fica para outra issue — ver SIG referente ao
 * wiring de velocidadeContratadaDownMbps/UpMbps). Mesmos limiares ja usados em
 * ScoreEvidenceBuilder.velocidade() (:featureDiagnostico), duplicados aqui de proposito
 * para nao acoplar a tela Home ao motor de diagnostico so por causa de um rotulo.
 */
private fun veredictoVelocidade(mbps: Double): Pair<String, Color> =
    when {
        mbps >= 100.0 -> "Excelente" to LkColors.success
        mbps >= 50.0 -> "Bom" to LkColors.success
        mbps >= 25.0 -> "Regular" to LkColors.warning
        mbps >= 10.0 -> "Fraco" to LkColors.error
        else -> "Fraco" to LkColors.error
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
    val (veredicto, veredictoColor) = veredictoVelocidade(value)
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                arrow,
                style = MaterialTheme.typography.titleMedium,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = labelColor)
            Text(
                "  ·  $veredicto",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.W600,
                color = veredictoColor,
            )
        }
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

private fun formatRelativeTimestamp(epochMs: Long): String {
    val diffMs = (System.currentTimeMillis() - epochMs).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    return when {
        minutes < 1 -> "agora mesmo"
        minutes < 60 -> "há $minutes min"
        minutes < 24 * 60 -> "há ${minutes / 60} h"
        else -> "há ${minutes / (24 * 60)} d"
    }
}

private fun formatSpeedValue(value: Double): String = if (value >= 100) value.toLong().toString() else "%.1f".format(value)

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
    val wifiPct = wifiSignalPercent(wifiRssi)

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
                    modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(LkRadius.pill)),
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

/**
 * Higiene 2026-07-16 (varredura Fase 2, tela Início) — a mesma conversão RSSI→percentual
 * estava copiada 3x dentro deste arquivo ([SignalQualityRow], [WifiSignalCard],
 * [ConnectionContextCard]). Fonte única aqui; **não confundir** com o cálculo de
 * `heightFraction` de `SinalScreen.kt` (escala 0..70 pra altura de barra, não percentual de
 * qualidade) — telas diferentes, dominios visuais diferentes, fora do escopo desta varredura.
 */
private fun wifiSignalPercent(rssiDbm: Int?): Int? = rssiDbm?.let { ((it + 90) / 50.0).coerceIn(0.0, 1.0) * 100 }?.roundToInt()

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

private fun mobileSignalColor(
    rsrpDbm: Int?,
    c: LkTokens,
): Color =
    when {
        rsrpDbm == null -> c.primary
        rsrpDbm > -80 -> LkColors.success
        rsrpDbm > -100 -> LkColors.warning
        else -> LkColors.error
    }

/** Mesma conversão RSRP→percentual estava copiada 2x ([MobileSignalCard], [SimChipCompact]). */
private fun mobileSignalPercent(rsrpDbm: Int?): Int? = rsrpDbm?.let { ((it + 140) / 96.0).coerceIn(0.0, 1.0) * 100 }?.roundToInt()

// ─── Wi-Fi signal card ───────────────────────────────────────────────────────

@Composable
private fun WifiSignalCard(
    snapshotRede: SnapshotRede,
    connectedNetwork: RedeVizinha?,
    c: LkTokens,
    onTap: () -> Unit,
) {
    val wifiRssi = connectedNetwork?.rssiDbm ?: snapshotRede.wifiLinkSnapshot?.rssiDbm
    val wifiPct = wifiSignalPercent(wifiRssi)
    val ssidResolvido = connectedNetwork?.ssid ?: snapshotRede.wifiLinkSnapshot?.ssid
    val ssid = ssidResolvido ?: "Wi-Fi"
    val freqMhz = connectedNetwork?.frequenciaMhz ?: snapshotRede.wifiLinkSnapshot?.frequenciaMhz
    val localizacaoDesligada = ssidResolvido == null && !snapshotRede.locationAtivado

    val iconColor =
        if (localizacaoDesligada) {
            c.textTertiary
        } else if (wifiRssi != null) {
            wifiSignalColor(wifiRssi)
        } else {
            c.primary
        }
    val qualityLabel =
        when {
            localizacaoDesligada -> "Sem leitura"
            wifiRssi != null -> wifiSignalQuality(wifiRssi)
            else -> "Conectado"
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
                            color = c.primary,
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
                                freqMhz != null && freqMhz >= 5900 -> "6 GHz"
                                freqMhz != null && freqMhz >= 3000 -> "5 GHz"
                                freqMhz != null -> "2.4 GHz"
                                else -> null
                            }
                        if (bandaLabel != null) {
                            Text(
                                bandaLabel.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.W600,
                                color = c.textTertiary,
                                letterSpacing = 0.3.sp,
                            )
                        }
                        Text(
                            ssid,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.W600,
                            color = c.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    text = qualityLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.W700,
                    color = iconColor,
                )
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
    if (movelSnapshot.radioDesligado) {
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
                            .background(c.textTertiary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.AirplanemodeActive,
                        contentDescription = "Modo avião ativado",
                        tint = c.textTertiary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(LkSpacing.md))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "REDE MÓVEL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.W600,
                        color = c.textTertiary,
                        letterSpacing = 0.3.sp,
                    )
                    Text(
                        "Modo avião ativado",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Rádio desligado",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        return
    }

    val tec = tecnologiaSimplificada(movelSnapshot.tecnologia)
    val tecLabel = tec?.uppercase() ?: "LTE"
    val rsrp = movelSnapshot.rsrpDbm
    val mobileColor = mobileSignalColor(rsrp, c)

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
                if (rsrp != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "RSRP $rsrp dBm",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text("  ·  ", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                        Text(
                            mobileSignalQuality(rsrp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.W600,
                            color = mobileColor,
                        )
                    }
                }
            }
            if (rsrp != null) {
                MiniSignalBars(pct = mobileSignalPercent(rsrp) ?: 0, color = mobileColor)
            }
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
    val rsrpColor = mobileSignalColor(rsrpDbm, c)
    val rsrpPct = mobileSignalPercent(rsrpDbm) ?: 0
    val isActive = sim.tecnologiaRede != null

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(LkRadius.card),
        color = if (isActive) c.primary.copy(alpha = 0.06f) else c.bgCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) c.primary.copy(alpha = 0.20f) else c.border),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Smartphone,
                    contentDescription = null,
                    tint = if (isActive) c.primary else c.textTertiary,
                    modifier = Modifier.size(14.dp),
                )
                if (showSlot) {
                    Text(
                        "SIM ${sim.simIndex}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.W700,
                        color = if (isActive) c.primary else c.textTertiary,
                    )
                }
                if (isActive) {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(LkRadius.pill))
                                .background(c.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                    ) {
                        Text(
                            "ATIVO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.W700,
                            color = c.primary,
                            fontSize = 9.sp,
                        )
                    }
                }
                if (sim.emRoaming) {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(LkRadius.pill))
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
                .background(c.surfaceContainer)
                .clickable(onClick = onClick)
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(c.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Insights, contentDescription = null, tint = c.primary, modifier = Modifier.size(18.dp))
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
                        VereditoUso.good -> c.success
                        VereditoUso.acceptable -> c.warning
                        VereditoUso.poor -> c.error
                    }
                badgeBg =
                    when (vereditoGamer) {
                        null -> c.bgSecondary
                        VereditoUso.good -> c.success.copy(alpha = 0.12f)
                        VereditoUso.acceptable -> c.warning.copy(alpha = 0.12f)
                        VereditoUso.poor -> c.error.copy(alpha = 0.12f)
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
                        isInstavel -> c.warning
                        downloadOk -> c.success
                        else -> c.error
                    }
                badgeBg =
                    when {
                        downloadMbps == null -> c.bgSecondary
                        isInstavel -> c.warning.copy(alpha = 0.12f)
                        downloadOk -> c.success.copy(alpha = 0.12f)
                        else -> c.error.copy(alpha = 0.12f)
                    }
            }
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = LkSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(useCase.icon, contentDescription = null, tint = c.primary, modifier = Modifier.size(20.dp))
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
                                    RoundedCornerShape(LkRadius.pill),
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
            Text(
                stringResource(R.string.home_wifi_fatores_titulo),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
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
                .clip(RoundedCornerShape(LkRadius.pill))
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
    c: LkTokens,
) {
    LkSheetFrame(modifier = Modifier.navigationBarsPadding()) {
        LkSheetSectionTitle(title = stringResource(R.string.home_sheet_meu_dispositivo))
        Spacer(modifier = Modifier.height(LkSpacing.lg))
        LkSheetInfoRow("Modelo", deviceName)
        LkSheetDivider()
        LkSheetInfoRow("Sistema", "Android")
        LkSheetDivider()
        LkSheetInfoRow("IP Local", localIp?.takeIf { it.isNotEmpty() } ?: "Não disponível")
        LkSheetDivider()
        LkSheetInfoRow("Tipo de conexão", if (isMobile) "Dados móveis" else "Wi-Fi")
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
    linkSpeedMbps: Int? = null,
) {
    val typeLabelBase =
        when (gateway.type) {
            ConnectionNodeType.WifiRouter -> "Roteador Wi-Fi"
            ConnectionNodeType.WifiMesh -> "Rede Mesh"
            ConnectionNodeType.WifiExtender -> "Repetidor Wi-Fi"
            ConnectionNodeType.Mobile -> "Antena móvel"
            ConnectionNodeType.Unknown -> "Não identificado"
        }
    // #980 (Fase 2B) — sem confirmação de qual nó é o central (ver TopologiaRedeEngine/#979),
    // a confiança do motor fica MEDIA/BAIXA; a Home nunca afirma o papel nesse caso.
    val typeLabel =
        if (gateway.confianca != null && gateway.confianca != NivelConfianca.ALTA) {
            "$typeLabelBase (provável)"
        } else {
            typeLabelBase
        }
    val isMeshOrExtensor =
        gateway.type == ConnectionNodeType.WifiMesh ||
            gateway.type == ConnectionNodeType.WifiExtender
    LkSheetFrame(modifier = Modifier.navigationBarsPadding()) {
        LkSheetSectionTitle(title = "Roteador da casa")
        Spacer(modifier = Modifier.height(LkSpacing.lg))
        LkSheetInfoRow("Tipo detectado", typeLabel)
        LkSheetDivider()
        LkSheetInfoRow("IP do roteador", gateway.ip ?: "Não detectado")
        LkSheetDivider()
        LkSheetInfoRow("SSID", connectedNetwork?.ssid?.takeIf { it.isNotBlank() } ?: "Não detectado")
        LkSheetDivider()
        LkSheetInfoRow("Sinal", connectedNetwork?.let { "${it.rssiDbm} dBm" } ?: "Não detectado")
        LkSheetDivider()
        LkSheetInfoRow("Banda", connectedNetwork?.let { freqDisplay(it.frequenciaMhz) } ?: "Não detectado")
        LkSheetDivider()
        LkSheetInfoRow(
            "Velocidade do link",
            if (!isMeshOrExtensor) {
                linkSpeedMbps?.let { "$it Mbps" } ?: "Não detectado"
            } else {
                "Não detectado"
            },
        )
        LkSheetDivider()
        LkSheetInfoRow("Canal", connectedNetwork?.canal?.toString() ?: "Não detectado")
        LkSheetDivider()
        LkSheetInfoRow("Largura de canal", connectedNetwork?.larguraCanalMhz?.let { "$it MHz" } ?: "Não detectado")
        LkSheetDivider()
        LkSheetInfoRow("Segurança", connectedNetwork?.let { wifiSecurityLabel(it.seguranca) } ?: "Não detectada")
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
    val publicIpValue = ispInfo?.ip ?: publicIp ?: "Não disponível"
    val provedorValue = ispInfo?.isp?.takeIf { it.isNotEmpty() } ?: "Não disponível"
    val regionValue = countryRegion.ifEmpty { "Não disponível" }
    LkSheetFrame(modifier = Modifier.navigationBarsPadding()) {
        LkSheetSectionTitle(title = stringResource(R.string.home_sheet_internet))
        Spacer(modifier = Modifier.height(LkSpacing.lg))
        LkSheetInfoRow("IP Público", publicIpValue)
        if (isCgNat(ispInfo?.ip ?: publicIp)) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.home_sheet_ip_compartilhado),
                style = MaterialTheme.typography.labelSmall,
                color = LkColors.warning,
                modifier = Modifier.padding(bottom = LkSpacing.md),
            )
        }
        LkSheetDivider()
        LkSheetInfoRow("Provedor", provedorValue)
        LkSheetDivider()
        LkSheetInfoRow("País / Região", regionValue)
        val dnsPrivadoValor =
            if (privateDnsAtivo) {
                privateDnsHostname?.takeIf { it.isNotEmpty() } ?: "Ativo"
            } else {
                "Padrão do provedor"
            }
        LkSheetDivider()
        LkSheetInfoRow("DNS Privado", dnsPrivadoValor, valueColor = if (privateDnsAtivo) LkColors.success else null)
        val servidoresDnsStr = dnsServidores.take(2).joinToString(" / ").ifEmpty { "Não disponível" }
        LkSheetDivider()
        LkSheetInfoRow("Servidores DNS", servidoresDnsStr)
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
            SheetDragHandle()
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            if (isWifi) "Detalhes da rede Wi-Fi" else "Detalhes da rede móvel",
            style = MaterialTheme.typography.titleLarge,
            color = c.textPrimary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(20.dp))
        if (isWifi && connectedNetwork != null) {
            connectedNetwork.ssid?.let { LkSheetInfoRow("Nome da rede", it) }
            LkSheetInfoRow("Qualidade do sinal", "$pct%")
            LkSheetInfoRow("Força do sinal", "${connectedNetwork.rssiDbm} dBm")
            linkSpeedMbps?.let { LkSheetInfoRow("Velocidade de conexão", "$it Mbps") }
            LkSheetInfoRow("Frequência", "${connectedNetwork.frequenciaMhz} MHz")
            LkSheetInfoRow("Banda", freqDisplay(connectedNetwork.frequenciaMhz))
            val ch = freqToChannel(connectedNetwork.frequenciaMhz)
            if (ch > 0) LkSheetInfoRow("Canal", "$ch")
            connectedNetwork.larguraCanalMhz?.let { LkSheetInfoRow("Largura de canal", "$it MHz") }
            LkSheetInfoRow("Segurança", wifiSecurityLabel(connectedNetwork.seguranca))
            val gw = gateways.firstOrNull()
            gw?.ip?.let { LkSheetInfoRow("IP do roteador", it) }
            localIp?.let { LkSheetInfoRow("IP do dispositivo", it) }
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
        SheetDragHandle()
        Spacer(Modifier.height(LkSpacing.xxl))
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(c.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Insights,
                contentDescription = null,
                tint = c.primary,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.home_qualidade_titulo),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = c.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(LkSpacing.sm))
        Text(
            text = stringResource(R.string.home_qualidade_em_breve),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.W600,
            color = c.primary,
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
    val qualityColor = rsrp?.let { mobileSignalColor(it, c) }
    val asuValue = rsrp?.let { "${it + 140}" } ?: "Não disponível"
    val sinrValue = sinr?.let { "$it dB" } ?: "Não disponível"
    val sinrColor =
        sinr?.let { s ->
            when {
                s > 10 -> LkColors.success
                s > 0 -> LkColors.warning
                else -> LkColors.error
            }
        }

    LkSheetFrame(modifier = Modifier.navigationBarsPadding()) {
        LkSheetSectionTitle(
            title = "Rede móvel",
            subtitle = "Detalhes da conexão móvel ativa",
        )

        Spacer(Modifier.height(LkSpacing.lg))

        LkSurfaceCard(outlined = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(c.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.SignalCellularAlt,
                        contentDescription = null,
                        tint = c.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(LkSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        operadora ?: "Rede móvel",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                    )
                    val heroSub =
                        listOfNotNull(tec, banda?.let { "banda $it" })
                            .joinToString(" · ")
                            .takeIf { it.isNotEmpty() }
                    if (heroSub != null) {
                        Text(heroSub, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                    }
                }
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(LkRadius.pill))
                            .background(LkColors.success.copy(alpha = 0.10f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        "Conectado",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.W700,
                        color = LkColors.success,
                    )
                }
            }
        }

        Spacer(Modifier.height(LkSpacing.lg))

        LkSheetInfoRow("Tecnologia", tec ?: "Não disponível")
        LkSheetDivider()
        LkSheetInfoRow("Operadora", operadora ?: "Não disponível")
        LkSheetDivider()
        LkSheetInfoRow("IP público", ip ?: "Não disponível")
        LkSheetDivider()
        LkSheetInfoRow("Qualidade do sinal", qualityLabel ?: "Não disponível", valueColor = qualityColor)
        LkSheetDivider()
        LkSheetInfoRow("ASU", asuValue)
        LkSheetDivider()
        LkSheetInfoRow("SINR", sinrValue, valueColor = sinrColor)
        LkSheetDivider()
        LkSheetInfoRow(
            "Roaming",
            if (movelSnapshot?.roaming == true) "Sim" else "Não",
            valueColor = if (movelSnapshot?.roaming == true) LkColors.warning else null,
        )
        LkSheetDivider()
        LkSheetInfoRow("MCC / MNC", if (mcc != null && mnc != null) "$mcc / $mnc" else "Não disponível")

        Spacer(Modifier.height(LkSpacing.md))
        Text(
            "Teste de velocidade pode consumir uma parcela significativa do seu plano de dados.",
            style = MaterialTheme.typography.labelMedium,
            color = c.textSecondary,
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
        SheetDragHandle()
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
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary),
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
                Text(label, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.W800, color = cor)
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
                    .clip(RoundedCornerShape(LkRadius.pill))
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
                    .clip(RoundedCornerShape(LkRadius.pill))
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
                    .padding(top = LkSpacing.sm)
                    .padding(horizontal = LkSpacing.lg)
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            SheetDragHandle()
            Text(
                "Tipo de medição",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.W700,
                color = c.textPrimary,
            )
            Text(
                "Escolha como quer medir sua conexão",
                style = MaterialTheme.typography.bodySmall,
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
                badgeColor = c.primary,
                disponivel = true,
                c = c,
                onClick = { onIniciarTeste(ModoSpeedtest.complete) },
            )

            MedicaoOpcaoItem(
                icon = Icons.Outlined.Refresh,
                titulo = "Triplo",
                descricao = "Média de 3 testes consecutivos · ~3 min",
                badge = "Só Wi-Fi",
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
    badgeColor: Color = c.primary,
    disponivel: Boolean,
    c: LkTokens,
    onClick: () -> Unit,
) {
    val textColor = if (disponivel) c.textPrimary else c.textTertiary
    val subTextColor = if (disponivel) c.textSecondary else c.textTertiary
    val iconColor = if (disponivel) c.primary else c.textTertiary

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
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
                    .clip(CircleShape)
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
                style = MaterialTheme.typography.titleSmall,
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
                        .clip(RoundedCornerShape(LkRadius.pill))
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
            val wifiPct = wifiSignalPercent(rssiDbm)
            val ssid =
                snapshotRede.wifiLinkSnapshot
                    ?.ssid
                    ?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.home_context_wifi_ssid_desconhecido)
            val qualidadeColor =
                when {
                    wifiPct == null -> c.primary
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
                                    .background(c.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Wifi,
                                contentDescription = null,
                                tint = c.primary,
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
                                modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(LkRadius.pill)),
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
                        colors = ButtonDefaults.buttonColors(containerColor = c.primary),
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
            val tecnologia = tecnologiaSimplificada(movelSnapshot?.tecnologia)

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
                                    .background(c.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.SignalCellularAlt,
                                contentDescription = null,
                                tint = c.primary,
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
                        colors = ButtonDefaults.buttonColors(containerColor = c.primary),
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

@Composable
private fun HeroMetric(
    label: String,
    value: Double,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = LocalLkTokens.current.textSecondary,
        )
        Text(
            text = formatSpeedValue(value),
            style = MaterialTheme.typography.headlineLarge,
            color = color,
        )
        Text(
            text = "Mbps",
            style = MaterialTheme.typography.labelMedium,
            color = LocalLkTokens.current.textTertiary,
        )
    }
}

@Composable
private fun MobileChipsCard(
    movelSnapshot: MovelSnapshot?,
    simsAtivos: List<MovelSimSnapshot>,
    c: LkTokens,
    onTap: () -> Unit,
    resolveOperadoraIdentidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity?,
    resolveOperadoraIdentidadeRemota: suspend (String?, Boolean) -> ResolvedOperadoraIdentity,
) {
    val linhas: List<Pair<String, Pair<String, Color>>> =
        remember(movelSnapshot, simsAtivos) {
            when {
                simsAtivos.isNotEmpty() ->
                    simsAtivos.map { sim ->
                        (sim.operadora?.takeIf { it.isNotBlank() } ?: "SIM ${sim.simIndex}") to
                            simStatusLabel(sim.rsrpDbm, sim.radioDesligado)
                    }
                movelSnapshot != null ->
                    listOf(
                        (movelSnapshot.operadora?.takeIf { it.isNotBlank() } ?: "Rede móvel") to
                            simStatusLabel(movelSnapshot.rsrpDbm, movelSnapshot.radioDesligado),
                    )
                else -> emptyList<Pair<String, Pair<String, Color>>>()
            }
        }

    if (linhas.isEmpty()) return

    SignallQCard(c) {
        Column {
            Text(
                text = "CHIP MÓVEL",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.W600,
                color = c.textTertiary,
            )
            Spacer(Modifier.height(LkSpacing.md))
            Text(
                text = "Operadoras detectadas",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(LkSpacing.md))
            linhas.forEachIndexed { index, (operadora, status) ->
                // #1087 — logo real da operadora (catalogo local, fallback operator_generic
                // via OperadoraBadge/OperadoraLogoCatalog), uma resolucao por linha porque cada
                // SIM pode ser de uma operadora diferente (nao reusa so a "principal").
                val identidadeLinha =
                    rememberResolvedOperadoraIdentity(
                        ispNomeBruto = operadora,
                        viaMovel = true,
                        resolveLocal = resolveOperadoraIdentidadeLocal,
                        resolveRemoteOrFallback = resolveOperadoraIdentidadeRemota,
                    )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onTap)
                            .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (identidadeLinha != null) {
                        OperadoraBadge(identidade = identidadeLinha, size = 40.dp)
                    } else {
                        Box(
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(c.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Smartphone,
                                contentDescription = null,
                                tint = c.textSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(LkSpacing.md))
                    Text(
                        text = operadora,
                        style = MaterialTheme.typography.titleSmall,
                        color = c.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = status.first,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.W700,
                        color = status.second,
                    )
                }
                if (index < linhas.lastIndex) {
                    HorizontalDivider(color = c.border, thickness = 1.dp)
                }
            }
        }
    }
}

private fun simStatusLabel(
    rsrpDbm: Int?,
    radioDesligado: Boolean,
): Pair<String, Color> =
    when {
        radioDesligado -> "Sem sinal" to LkColors.error
        rsrpDbm == null -> "Sem leitura" to LkColors.warning
        rsrpDbm > -85 -> "Bom" to LkColors.success
        rsrpDbm > -100 -> "Regular" to LkColors.warning
        else -> "Ruim" to LkColors.error
    }
