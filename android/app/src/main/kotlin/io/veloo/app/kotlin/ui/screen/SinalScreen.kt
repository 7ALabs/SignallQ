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
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.SignalCellularOff
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiFind
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
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.signallq.app.R
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.WifiLinkSnapshot
import io.signallq.app.core.network.contracts.topologia.NivelConfianca
import io.signallq.app.core.network.contracts.topologia.PapelTopologia
import io.signallq.app.core.network.topologia.engine.TopologiaRedeEngine
import io.signallq.app.core.telephony.MovelSimSnapshot
import io.signallq.app.core.telephony.MovelSnapshot
import io.signallq.app.feature.devices.DispositivoRede
import io.signallq.app.feature.devices.chaveApelido
import io.signallq.app.feature.devices.encontrarDispositivoPorBssid
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
import io.signallq.app.ui.BancoOperadoras
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkPillBadge
import io.signallq.app.ui.component.LkSectionOverline
import io.signallq.app.ui.component.LkSheetDivider
import io.signallq.app.ui.component.LkSheetFrame
import io.signallq.app.ui.component.LkSheetInfoRow
import io.signallq.app.ui.component.LkSheetSectionTitle
import io.signallq.app.ui.component.LkStatusDot
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.OfflineBanner
import io.signallq.app.ui.component.OperadoraBadge
import io.signallq.app.ui.component.ProfileAvatarButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// ─── Helpers ──────────────────────────────────────────────────────────────────

private data class TopologiaIconData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val cor: Color,
)

private fun TipoTopologia.toIconData(c: LkTokens): TopologiaIconData? =
    when (this) {
        TipoTopologia.ROTEADOR -> TopologiaIconData(Icons.Outlined.Router, c.primary)
        TipoTopologia.ROTEADOR_MESH -> TopologiaIconData(Icons.Outlined.Hub, c.primary)
        TipoTopologia.NO_MESH -> TopologiaIconData(Icons.Outlined.Hub, c.primary)
        TipoTopologia.REPETIDOR -> TopologiaIconData(Icons.Outlined.CellTower, LkColors.warning)
        TipoTopologia.PONTO_DE_ACESSO -> TopologiaIconData(Icons.Outlined.Lan, LkColors.signallQTextSecondaryOnDark)
        TipoTopologia.DESCONHECIDO -> null
    }

// #980 (Fase 2B) — adapta o resultado do motor unificado (TopologiaRedeEngine, Fase 2A/#979)
// pro shape legado (RedeClassificada/TipoTopologia/ConfiancaTopologia) que esta tela ja consome,
// pra nao precisar reescrever icone/agrupamento/renderizacao — so troca a fonte da classificacao.
// SISTEMA_MESH_PROVAVEL vira NO_MESH (mesmo icone de hoje; a incerteza ja e comunicada pelo aviso
// "* Gateway estimado pelo sinal mais forte" em GrupoRedeTree, calculado independente do motor).
internal fun PapelTopologia.paraTipoTopologiaLegado(): TipoTopologia =
    when (this) {
        PapelTopologia.ROTEADOR -> TipoTopologia.ROTEADOR
        PapelTopologia.NO_MESH -> TipoTopologia.NO_MESH
        PapelTopologia.SISTEMA_MESH_PROVAVEL -> TipoTopologia.NO_MESH
        PapelTopologia.REPETIDOR -> TipoTopologia.REPETIDOR
        PapelTopologia.PONTO_DE_ACESSO -> TipoTopologia.PONTO_DE_ACESSO
        PapelTopologia.DESCONHECIDO -> TipoTopologia.DESCONHECIDO
    }

internal fun NivelConfianca.paraConfiancaTopologiaLegado(): ConfiancaTopologia =
    when (this) {
        NivelConfianca.ALTA -> ConfiancaTopologia.ALTA
        NivelConfianca.MEDIA -> ConfiancaTopologia.MEDIA
        NivelConfianca.BAIXA -> ConfiancaTopologia.BAIXA
    }

// GH#1025 (3c) — tipos de nó que o protótipo trata como "ponto de acesso/mesh": abrem
// MeshApSheet (dado real do DispositivoRede correlacionado) em vez de NetworkDetailSheet.
// ROTEADOR fica de fora de propósito — é o próprio gateway conectado, tratado como "sua conexão"
// (NetworkDetailSheet já é a sheet certa pra ele); DESCONHECIDO também fica de fora (rede vizinha
// comum, sem classificação de topologia).
private val TIPOS_TOPOLOGIA_AP_MESH =
    setOf(
        TipoTopologia.ROTEADOR_MESH,
        TipoTopologia.NO_MESH,
        TipoTopologia.REPETIDOR,
        TipoTopologia.PONTO_DE_ACESSO,
    )

/**
 * Decide qual [DispositivoRede] (se algum) deve abrir MeshApSheet pra um nó [rede] da árvore de
 * topologia da tela Sinal — GH#1025. Só tenta correlacionar quando o nó já foi classificado como
 * AP/mesh ([TIPOS_TOPOLOGIA_AP_MESH]); fora disso devolve null sempre, mesmo que por acaso exista
 * um [DispositivoRede] com MAC batendo (evita abrir a sheet errada pra uma rede vizinha comum).
 *
 * Quando o nó é AP/mesh mas nenhum [DispositivoRede] correlaciona (scan LAN ainda não descobriu
 * esse equipamento, ou MAC não resolvível via ARP) devolve null — o chamador mantém o fallback
 * padrão (NetworkDetailSheet), nunca fabrica um [DispositivoRede] parcial.
 */
internal fun resolverDispositivoParaNoTopologia(
    rede: RedeVizinha,
    tipoTopologia: TipoTopologia?,
    dispositivosRede: List<DispositivoRede>,
): DispositivoRede? {
    if (tipoTopologia !in TIPOS_TOPOLOGIA_AP_MESH) return null
    return encontrarDispositivoPorBssid(dispositivosRede, rede.bssid)
}

private fun classificarComMotorUnificado(
    redes: List<RedeVizinha>,
    connectedBssid: String?,
): List<RedeClassificada> =
    TopologiaRedeEngine
        .classificar(redes = redes, connectedBssid = connectedBssid)
        .map { (rede, classificacao) ->
            RedeClassificada(
                rede = rede,
                tipo = classificacao.papelProvavel.paraTipoTopologiaLegado(),
                confianca = classificacao.confianca.paraConfiancaTopologiaLegado(),
                motivo = "",
            )
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
    c: LkTokens,
): Color =
    when (banda) {
        BandaWifi.ghz5 ->
            when {
                rssiDbm >= -65 -> c.success
                rssiDbm >= -75 -> c.warning
                else -> c.error
            }
        else ->
            when {
                rssiDbm >= -60 -> c.success
                rssiDbm >= -70 -> c.warning
                else -> c.error
            }
    }

private fun congestionColor(
    nivel: NivelCongestionamento,
    c: LkTokens,
): Color =
    when (nivel) {
        NivelCongestionamento.livre -> c.success
        NivelCongestionamento.moderado -> c.warning
        NivelCongestionamento.congestionado -> c.error
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

// ─── Auto-refresh (#893) ──────────────────────────────────────────────────────

private const val SINAL_AUTO_REFRESH_INTERVAL_MS = 30_000L

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
    // GH#1025 — dado do scan LAN (mesmo carregado em Dispositivos/5a), usado só pra correlacionar
    // um nó da árvore de topologia classificado como AP/mesh com o DispositivoRede real e abrir
    // MeshApSheet em vez de NetworkDetailSheet. Não introduz lista de dispositivos-cliente nesta
    // tela (3d segue fora de escopo, ver issue).
    dispositivosRede: List<DispositivoRede> = emptyList(),
    apelidos: Map<String, String> = emptyMap(),
    onSalvarApelido: (mac: String, apelido: String) -> Unit = { _, _ -> },
    // Seam de teste (#893) — producao nunca passa isso, so os testes de auto-refresh
    // usam um intervalo curto pra nao esperar 30s reais por teste.
    autoRefreshIntervalMs: Long = SINAL_AUTO_REFRESH_INTERVAL_MS,
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

    LaunchedEffect(selectedTab, temPermissaoLocalizacao, localizacaoSheetDismissed) {
        if (selectedTab in 0..1 && !temPermissaoLocalizacao && !localizacaoSheetDismissed) {
            showLocalizacaoSheet = true
        }
    }

    LaunchedEffect(selectedTab, temPermissaoTelefonia, telefoniaSheetDismissed) {
        if (selectedTab == 2 && !temPermissaoTelefonia && !telefoniaSheetDismissed) {
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

        // Auto-refresh (#893): reescaneia periodicamente enquanto a aba Wi-Fi ou Canal
        // estiver visivel e a tela em foreground. `repeatOnLifecycle` cancela o loop
        // sozinho quando o app vai pra background e retoma quando volta — sem isso o
        // scan continuaria rodando com a tela fora de foco. Sair da aba Sinal (troca de
        // tab no AppShell) tambem cancela, pois o LaunchedEffect sai de composicao.
        val autoRefreshAtivo = conexaoTipo == ConexaoTipo.WIFI && (selectedTab == 0 || selectedTab == 1)
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(autoRefreshAtivo, lifecycleOwner, autoRefreshIntervalMs) {
            if (!autoRefreshAtivo) return@LaunchedEffect
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    onRefresh()
                    delay(autoRefreshIntervalMs)
                }
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
                            dispositivosRede = dispositivosRede,
                            apelidos = apelidos,
                            onSalvarApelido = onSalvarApelido,
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
                else -> {
                    MovelTab(
                        movelSnapshot = movelSnapshot,
                        simsAtivos = simsAtivos,
                        temPermissaoTelefonia = temPermissaoTelefonia,
                        onSolicitarPermissaoTelefonia = onSolicitarPermissaoTelefonia,
                        tokens = c,
                    )
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
            val bandaConectada = connectedNetwork.banda
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
        contentColor = c.primary,
        divider = { HorizontalDivider(color = c.outlineVariant, thickness = 1.dp) },
        // GH#1080 (P0): faltava `modifier = Modifier.tabIndicatorOffset(...)` -- sem ele o
        // TabRow mede o indicador com Constraints.fixed(larguraTotal, alturaTotal) da propria
        // TabRow (nao so a altura de 3dp pedida), entao o indicador cobre a faixa inteira com
        // cor solida por cima dos 3 labels (Wi-Fi/Canal/Movel), que continuam compostos --
        // so ficam visualmente cobertos. Padrao correto ja existia em DnsScreen.kt:657-668.
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                height = 3.dp,
                color = c.primary,
            )
        },
    ) {
        listOf("Wi-Fi", "Canal", "Móvel").forEachIndexed { index, label ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    if (index == 1 && canalCongestionado) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (selectedTab == index) FontWeight.W600 else FontWeight.W500,
                                color = if (selectedTab == index) c.primary else c.textSecondary,
                            )
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = "Canal congestionado",
                                tint = LkColors.warning,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    } else {
                        Text(
                            label,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            softWrap = false,
                            fontWeight = if (selectedTab == index) FontWeight.W600 else FontWeight.W500,
                            color = if (selectedTab == index) c.primary else c.textSecondary,
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
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        if (simsAtivos.isNotEmpty()) {
            ChipsAtivosSection(
                simsAtivos = simsAtivos,
                movelSnapshot = movelSnapshot,
                tokens = c,
            )
        } else if (movelSnapshot != null) {
            MobileSnapshotCard(
                snapshot = movelSnapshot,
                tokens = c,
            )
        }
    }
}

@Composable
private fun ChipsAtivosSection(
    simsAtivos: List<MovelSimSnapshot>,
    movelSnapshot: MovelSnapshot?,
    tokens: LkTokens,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.lg),
    ) {
        simsAtivos.forEachIndexed { index, sim ->
            SimCard(
                sim = sim,
                summarySnapshot = movelSnapshot,
                cardLabel = "Chip ${index + 1}",
                tokens = tokens,
            )
        }
    }
}

@Composable
private fun SimCard(
    sim: MovelSimSnapshot,
    summarySnapshot: MovelSnapshot?,
    cardLabel: String,
    tokens: LkTokens,
) {
    val operadora = sim.operadora ?: summarySnapshot?.operadora ?: "Operadora"
    val operadoraLocal = remember(operadora) { BancoOperadoras.resolverMovel(operadora) }
    val resumoRede = buildMobileSummary(sim = sim, summarySnapshot = summarySnapshot)
    val qualidade = mobileSignalQuality(sim.rsrpDbm, sim.radioDesligado, tokens)
    val tipoConexao = mobileConnectionType(sim = sim, summarySnapshot = summarySnapshot, c = tokens)
    val experiencia = mobileExpectedExperience(sim = sim, summarySnapshot = summarySnapshot, c = tokens)
    val suporteUrl = operadoraLocal?.site
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        LkSurfaceCard(modifier = Modifier.fillMaxWidth(), outlined = true) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
                    ) {
                        Text(
                            text = cardLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.W600,
                            color = tokens.textSecondary,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
                    ) {
                        Text(
                            text = operadora,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.W600,
                            color = tokens.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (sim.isDefaultData) {
                            MobileStatusBadge(
                                label = "EM USO",
                                color = LkColors.success,
                            )
                        }
                    }
                    Text(
                        text = resumoRede,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textSecondary,
                    )
                }
                operadoraLocal?.let {
                    OperadoraBadge(
                        operadora = it,
                        size = 48.dp,
                    )
                } ?: PlaceholderOperadoraBadge(tokens = tokens)
            }
        }

        MobileDetailCard(
            icon = Icons.Outlined.SignalCellularAlt,
            title = "Qualidade do sinal",
            body = qualidade.description,
            badge = qualidade.label,
            accent = qualidade.color,
            tokens = tokens,
        )
        MobileDetailCard(
            icon = Icons.Outlined.CellTower,
            title = "Tipo de conexão",
            body = tipoConexao.description,
            badge = tipoConexao.label,
            accent = tipoConexao.color,
            tokens = tokens,
        )
        MobileDetailCard(
            icon = Icons.Outlined.CheckCircle,
            title = "Experiência esperada",
            body = experiencia.description,
            badge = experiencia.label,
            accent = experiencia.color,
            tokens = tokens,
        )

        OutlinedButton(
            onClick = {
                suporteUrl?.let { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = suporteUrl != null,
            shape = RoundedCornerShape(LkRadius.button),
        ) {
            Text(
                text = "Falar com a $operadora",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class MobileInsight(
    val label: String,
    val description: String,
    val color: Color,
)

private fun buildMobileSummary(
    sim: MovelSimSnapshot,
    summarySnapshot: MovelSnapshot?,
): String {
    if (sim.radioDesligado) return "Modo avião ativo · rádio celular desligado"
    val rsrp = sim.rsrpDbm ?: summarySnapshot?.rsrpDbm
    val tecnologia = sim.tecnologiaRede ?: summarySnapshot?.tecnologia ?: "Rede móvel"
    return buildString {
        if (rsrp != null) append("RSRP $rsrp dBm")
        if (rsrp != null) append(" · ")
        append(tecnologia)
    }
}

private fun mobileSignalQuality(
    rsrpDbm: Int?,
    radioDesligado: Boolean,
    c: LkTokens,
): MobileInsight =
    when {
        radioDesligado ->
            MobileInsight(
                label = "Sem sinal",
                description = "Rádio celular desligado. Ative a rede móvel para medir chamadas e dados.",
                color = c.warning,
            )
        rsrpDbm == null ->
            MobileInsight(
                label = "Indisponível",
                description = "O Android não expôs a intensidade deste chip agora.",
                color = LkColors.accentBlue,
            )
        rsrpDbm >= -90 ->
            MobileInsight(
                label = "Bom",
                description = "Bom - chamadas e vídeos tendem a ocorrer sem cortes.",
                color = c.success,
            )
        rsrpDbm >= -105 ->
            MobileInsight(
                label = "Regular",
                description = "Regular - pode oscilar em chamadas e vídeo em movimento.",
                color = c.warning,
            )
        else ->
            MobileInsight(
                label = "Ruim",
                description = "Ruim - maior chance de falhas em chamadas, uploads e streaming.",
                color = c.error,
            )
    }

private fun mobileConnectionType(
    sim: MovelSimSnapshot,
    summarySnapshot: MovelSnapshot?,
    c: LkTokens,
): MobileInsight {
    val tecnologia = sim.tecnologiaRede ?: summarySnapshot?.tecnologia
    return when {
        sim.radioDesligado ->
            MobileInsight(
                label = "Off",
                description = "Rádio desligado. Não há tecnologia ativa neste momento.",
                color = c.warning,
            )
        tecnologia?.contains("5G", ignoreCase = true) == true ->
            MobileInsight(
                label = "5G",
                description = "5G NR - tecnologia mais rápida disponível para este chip.",
                color = c.primary,
            )
        tecnologia?.contains("4G", ignoreCase = true) == true ->
            MobileInsight(
                label = "4G",
                description = "4G LTE - bom equilíbrio entre cobertura e velocidade.",
                color = c.warning,
            )
        tecnologia?.contains("3G", ignoreCase = true) == true ->
            MobileInsight(
                label = "3G",
                description = "3G - tecnologia legada com menor capacidade para vídeo e uploads.",
                color = c.warning,
            )
        tecnologia?.contains("2G", ignoreCase = true) == true ->
            MobileInsight(
                label = "2G",
                description = "2G - suficiente para voz e mensagens básicas, mas limitada para internet.",
                color = c.error,
            )
        else ->
            MobileInsight(
                label = "Móvel",
                description = "Tecnologia não identificada pelo Android neste momento.",
                color = LkColors.accentBlue,
            )
    }
}

private fun mobileExpectedExperience(
    sim: MovelSimSnapshot,
    summarySnapshot: MovelSnapshot?,
    c: LkTokens,
): MobileInsight {
    val tecnologia = sim.tecnologiaRede ?: summarySnapshot?.tecnologia
    val rsrp = sim.rsrpDbm ?: summarySnapshot?.rsrpDbm
    return when {
        sim.radioDesligado ->
            MobileInsight(
                label = "Indisponível",
                description = "Sem rede ativa para estimar experiência de uso.",
                color = c.warning,
            )
        tecnologia?.contains("5G", ignoreCase = true) == true && (rsrp == null || rsrp >= -95) ->
            MobileInsight(
                label = "Ótima",
                description = "Ótima para streaming, jogos e videochamadas com boa estabilidade.",
                color = c.success,
            )
        tecnologia?.contains("4G", ignoreCase = true) == true && (rsrp == null || rsrp >= -100) ->
            MobileInsight(
                label = "Boa",
                description = "Boa para navegação, mensagens e vídeo em qualidade padrão.",
                color = c.success,
            )
        tecnologia?.contains("3G", ignoreCase = true) == true || (rsrp != null && rsrp < -105) ->
            MobileInsight(
                label = "Limitada",
                description = "Melhor para mensagens e navegação leve; chamadas e vídeo podem oscilar.",
                color = c.warning,
            )
        else ->
            MobileInsight(
                label = "Regular",
                description = "Uso cotidiano possível, mas com chance de lentidão em horários de pico.",
                color = c.warning,
            )
    }
}

private fun buildSnapshotMobileSummary(snapshot: MovelSnapshot): String =
    buildString {
        snapshot.rsrpDbm?.let {
            append("RSRP $it dBm")
            append(" · ")
        }
        append(snapshot.tecnologia ?: "Rede móvel")
    }

private fun snapshotSignalQuality(
    snapshot: MovelSnapshot,
    c: LkTokens,
): MobileInsight =
    run {
        val rsrp = snapshot.rsrpDbm
        when {
            snapshot.radioDesligado ->
                MobileInsight(
                    label = "Sem sinal",
                    description = "Rádio celular desligado. Ative a rede móvel para medir chamadas e dados.",
                    color = c.warning,
                )
            rsrp == null ->
                MobileInsight(
                    label = "Sem leitura",
                    description = "O Android não forneceu leitura precisa do sinal neste momento.",
                    color = LkColors.accentBlue,
                )
            rsrp >= -90 ->
                MobileInsight(
                    label = "Bom",
                    description = "Bom — chamadas e vídeos tendem a ficar estáveis.",
                    color = c.success,
                )
            rsrp >= -105 ->
                MobileInsight(
                    label = "Regular",
                    description = "Regular — pode haver variação em chamadas, uploads e streaming.",
                    color = c.warning,
                )
            else ->
                MobileInsight(
                    label = "Ruim",
                    description = "Ruim — maior chance de falhas em chamadas, uploads e streaming.",
                    color = c.error,
                )
        }
    }

private fun snapshotConnectionType(
    snapshot: MovelSnapshot,
    c: LkTokens,
): MobileInsight {
    val tecnologia = snapshot.tecnologia
    return when {
        snapshot.radioDesligado ->
            MobileInsight(
                label = "Off",
                description = "Rádio desligado. Não há tecnologia ativa neste momento.",
                color = c.warning,
            )
        tecnologia?.contains("5G", ignoreCase = true) == true ->
            MobileInsight(
                label = "5G",
                description = "5G NR — a tecnologia mais rápida disponível.",
                color = c.primary,
            )
        tecnologia?.contains("4G", ignoreCase = true) == true ->
            MobileInsight(
                label = "4G",
                description = "4G LTE — bom equilíbrio entre cobertura e velocidade.",
                color = c.warning,
            )
        tecnologia?.contains("3G", ignoreCase = true) == true ->
            MobileInsight(
                label = "3G",
                description = "3G — tecnologia legada com menor capacidade para vídeo e uploads.",
                color = c.warning,
            )
        else ->
            MobileInsight(
                label = "Móvel",
                description = "Tecnologia não identificada pelo Android neste momento.",
                color = LkColors.accentBlue,
            )
    }
}

private fun snapshotExpectedExperience(
    snapshot: MovelSnapshot,
    c: LkTokens,
): MobileInsight {
    val tecnologia = snapshot.tecnologia
    val rsrp = snapshot.rsrpDbm
    return when {
        snapshot.radioDesligado ->
            MobileInsight(
                label = "Indisponível",
                description = "Sem rede ativa para estimar experiência de uso.",
                color = c.warning,
            )
        tecnologia?.contains("5G", ignoreCase = true) == true && (rsrp == null || rsrp >= -95) ->
            MobileInsight(
                label = "Ótima",
                description = "Ótima para streaming, jogos e videochamadas com boa estabilidade.",
                color = c.success,
            )
        tecnologia?.contains("4G", ignoreCase = true) == true && (rsrp == null || rsrp >= -100) ->
            MobileInsight(
                label = "Boa",
                description = "Boa para navegação, mensagens e vídeo em qualidade padrão.",
                color = c.success,
            )
        tecnologia?.contains("3G", ignoreCase = true) == true || (rsrp != null && rsrp < -105) ->
            MobileInsight(
                label = "Limitada",
                description = "Melhor para mensagens e navegação leve; chamadas e vídeo podem oscilar.",
                color = c.warning,
            )
        else ->
            MobileInsight(
                label = "Regular",
                description = "Uso cotidiano possível, mas com chance de lentidão em horários de pico.",
                color = c.warning,
            )
    }
}

@Composable
private fun MobileDetailCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    badge: String,
    accent: Color,
    tokens: LkTokens,
) {
    LkSurfaceCard(modifier = Modifier.fillMaxWidth(), outlined = true) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = tokens.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.textSecondary,
                )
            }
            MobileStatusBadge(
                label = badge,
                color = accent,
            )
        }
    }
}

@Composable
private fun MobileStatusBadge(
    label: String,
    color: Color,
) {
    LkPillBadge(
        text = label,
        containerColor = color.copy(alpha = 0.12f),
        contentColor = color,
    )
}

@Composable
private fun PlaceholderOperadoraBadge(tokens: LkTokens) {
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(LkRadius.input))
                .background(tokens.surfaceContainerHigh)
                .border(1.dp, tokens.outlineVariant, RoundedCornerShape(LkRadius.input)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "logo",
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textTertiary,
        )
    }
}

@Composable
private fun MobileSnapshotCard(
    snapshot: MovelSnapshot,
    tokens: LkTokens,
) {
    val operadora = snapshot.operadora ?: "Operadora"
    val qualidade = snapshotSignalQuality(snapshot, tokens)
    val tipoConexao = snapshotConnectionType(snapshot, tokens)
    val experiencia = snapshotExpectedExperience(snapshot, tokens)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        LkSurfaceCard(modifier = Modifier.fillMaxWidth(), outlined = true) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
                ) {
                    Text(
                        text = "Chip 1",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.W600,
                        color = tokens.textSecondary,
                    )
                    Text(
                        text = operadora,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.W600,
                        color = tokens.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildSnapshotMobileSummary(snapshot),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.textSecondary,
                    )
                }
                PlaceholderOperadoraBadge(tokens = tokens)
            }
        }
        MobileDetailCard(
            icon = Icons.Outlined.SignalCellularAlt,
            title = "Qualidade do sinal",
            body = qualidade.description,
            badge = qualidade.label,
            accent = qualidade.color,
            tokens = tokens,
        )
        MobileDetailCard(
            icon = Icons.Outlined.CellTower,
            title = "Tipo de conexão",
            body = tipoConexao.description,
            badge = tipoConexao.label,
            accent = tipoConexao.color,
            tokens = tokens,
        )
        MobileDetailCard(
            icon = Icons.Outlined.CheckCircle,
            title = "Experiência esperada",
            body = experiencia.description,
            badge = experiencia.label,
            accent = experiencia.color,
            tokens = tokens,
        )
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
    dispositivosRede: List<DispositivoRede> = emptyList(),
    apelidos: Map<String, String> = emptyMap(),
    onSalvarApelido: (mac: String, apelido: String) -> Unit = { _, _ -> },
) {
    val c = LocalLkTokens.current
    var selectedBanda by remember { mutableStateOf("Todos") }
    var selectedNetwork by remember { mutableStateOf<RedeVizinha?>(null) }
    // GH#1025 (3c) — quando o nó tocado correlaciona com um DispositivoRede real, abre
    // MeshApSheet em vez de NetworkDetailSheet (ver resolverDispositivoParaNoTopologia).
    var selectedDispositivoMesh by remember { mutableStateOf<DispositivoRede?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val meshSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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

    // Classificação de topologia para todas as redes visíveis — motor unificado (Fase 2A/#979,
    // Fase 2B/#980): ve OUI e banda, resolve o conflito Intelbras por contexto.
    val topologiaPorBssid =
        remember(snapshotWifi.redes, connectedNetwork) {
            runCatching {
                val classificadas =
                    classificarComMotorUnificado(
                        redes = snapshotWifi.redes,
                        connectedBssid = connectedNetwork?.bssid,
                    )
                classificadas.associate { it.rede.bssid to it.tipo }
            }.getOrElse { emptyMap() }
        }

    // GH#1025 (3c) — clique num nó da árvore (conectado ou "outras redes"): abre MeshApSheet
    // quando o nó correlaciona com um DispositivoRede real do scan LAN, senão mantém
    // NetworkDetailSheet (comportamento de sempre). O nó conectado nunca abre MeshApSheet, mesmo
    // quando o motor classifica ele como NO_MESH/SISTEMA_MESH_PROVAVEL sem confirmação de rota —
    // "sua conexão" continua sendo a sua conexão, não um equipamento pra inspecionar.
    val onNoOuRedeClick: (RedeVizinha) -> Unit = { rede ->
        val ehRedeConectada = rede.bssid == connectedNetwork?.bssid
        val dispositivo =
            if (ehRedeConectada) {
                null
            } else {
                resolverDispositivoParaNoTopologia(
                    rede = rede,
                    tipoTopologia = topologiaPorBssid[rede.bssid],
                    dispositivosRede = dispositivosRede,
                )
            }
        if (dispositivo != null) {
            selectedDispositivoMesh = dispositivo
        } else {
            selectedNetwork = rede
        }
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

    // Espectro do canal da rede conectada — mesma logica/engine do Tab Canal, usada
    // aqui so para o alerta de congestionamento + recomendacao no card/sheet da rede.
    val espectroConectado =
        remember(snapshotWifi.redes, connectedNetwork) {
            val canal = connectedNetwork?.canal ?: return@remember null
            val redesMesmaBanda = snapshotWifi.redes.filter { it.banda == connectedNetwork.banda }
            WifiChannelDiagnosticEngine.computarEspectro(
                redes =
                    redesMesmaBanda.map {
                        RedeWifiVizinha(
                            canal = it.canal,
                            rssiDbm = it.rssiDbm,
                            frequenciaMhz = it.frequenciaMhz,
                            ssid = it.ssid,
                            bssid = it.bssid,
                        )
                    },
                canalAtual = canal,
                banda = connectedNetwork.banda,
                seuSSID = connectedNetwork.ssid,
            )
        }
    val dadoCanalConectado =
        remember(espectroConectado, connectedNetwork) {
            espectroConectado?.dadosPorCanal?.find { it.canal == connectedNetwork?.canal }
        }
    val canalConectadoCongestionado = dadoCanalConectado?.nivel == NivelCongestionamento.congestionado
    val canalRecomendadoConectado = espectroConectado?.canalRecomendado
    val dadoCanalRecomendadoConectado =
        remember(espectroConectado, canalRecomendadoConectado) {
            espectroConectado?.dadosPorCanal?.find { it.canal == canalRecomendadoConectado }
        }

    // Redes de outros SSIDs (exclui nós da mesma rede) — classificadas e agrupadas por SSID
    val otherClassificadas =
        remember(filteredRedes, connectedNetwork, filteredRedes.size) {
            val connSsid = connectedNetwork?.ssid
            val filtered =
                filteredRedes
                    .filter { it.bssid != connectedNetwork?.bssid }
                    .filter { rede -> connSsid == null || rede.ssid == null || rede.ssid != connSsid }

            // Classificar via motor unificado (TopologiaRedeEngine, Fase 2A/#979); fallback
            // gracioso para lista vazia
            val classificadas =
                runCatching {
                    classificarComMotorUnificado(
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
                            .background(c.error.copy(alpha = 0.1f))
                            .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.WifiFind, null, tint = c.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(LkSpacing.sm))
                        Text(
                            msg ?: "Erro ao escanear redes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Normal,
                            color = c.error,
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
                                .clip(RoundedCornerShape(LkRadius.card))
                                .background(LkColors.success.copy(alpha = 0.12f))
                                .border(
                                    1.dp,
                                    LkColors.success.copy(alpha = 0.24f),
                                    RoundedCornerShape(LkRadius.card),
                                ),
                    ) {
                        GrupoRedeTree(
                            ssid = connectedNetwork.ssid ?: "Rede oculta",
                            nos = grupoNos,
                            connectedBssid = connectedNetwork.bssid,
                            onNoClick = onNoOuRedeClick,
                            wifiLinkSnapshot = wifiLinkSnapshot,
                            topologiaPorBssid = topologiaPorBssid,
                            canalConectadoCongestionado = canalConectadoCongestionado,
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
                                .clip(RoundedCornerShape(LkRadius.input))
                                .background(c.bgSecondary)
                                .border(1.dp, c.border, RoundedCornerShape(LkRadius.input))
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
                            "Conectado em ${connectedNetwork.banda} (${connectedNetwork.ssid})",
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
                        onNetworkClick = onNoOuRedeClick,
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
                                color = c.primary,
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
        val ehRedeConectada = net.bssid == connectedNetwork?.bssid
        ModalBottomSheet(
            onDismissRequest = { selectedNetwork = null },
            sheetState = sheetState,
            containerColor = c.bgCard,
            dragHandle = {},
        ) {
            NetworkDetailSheet(
                rede = net,
                canalCongestionado = ehRedeConectada && canalConectadoCongestionado,
                canalRecomendado = if (ehRedeConectada) canalRecomendadoConectado else null,
                nivelCanalRecomendado = if (ehRedeConectada) dadoCanalRecomendadoConectado?.nivel else null,
            )
        }
    }

    // GH#1025 (3c) — sheet de detalhe de dispositivo AP/mesh, dado real correlacionado do
    // scan LAN (ver onNoOuRedeClick/resolverDispositivoParaNoTopologia).
    val dispositivoMesh = selectedDispositivoMesh
    if (dispositivoMesh != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedDispositivoMesh = null },
            sheetState = meshSheetState,
            containerColor = c.bgCard,
            dragHandle = {},
        ) {
            MeshApSheet(
                dispositivo = dispositivoMesh,
                c = c,
                apelidoAtual = dispositivoMesh.chaveApelido()?.let { apelidos[it] } ?: "",
                onSalvarApelido = { apelido ->
                    dispositivoMesh.chaveApelido()?.let { chave -> onSalvarApelido(chave, apelido) }
                },
            )
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    LkSectionOverline(text = text, modifier = modifier)
}

@Composable
private fun BandFilterRow(
    selected: String,
    bands: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    counts: Map<String, Int>? = null,
) {
    val c = LocalLkTokens.current
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(LkRadius.pill))
                .background(c.surfaceContainer)
                .padding(LkSpacing.xs)
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        bands.forEach { band ->
            val active = selected == band
            val label = counts?.get(band)?.let { "$band ($it)" } ?: band
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(LkRadius.pill))
                        .background(if (active) c.secondaryContainer else Color.Transparent)
                        .minimumInteractiveComponentSize()
                        .clickable { onSelect(band) }
                        .padding(horizontal = LkSpacing.base, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (active) FontWeight.W600 else FontWeight.W500,
                    color = if (active) c.onSecondaryContainer else c.textSecondary,
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
    canalConectadoCongestionado: Boolean = false,
) {
    val c = LocalLkTokens.current
    // Roteador dual-band único: mesmo OUI e cada banda aparecendo uma só vez
    val ehDualBandUnico =
        nos.size > 1 &&
            nos.map { it.oui.uppercase() }.toSet().size <= 1 &&
            nos.map { it.banda }.let { it.size == it.toSet().size }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card)),
    ) {
        LkSurfaceCard(modifier = Modifier.fillMaxWidth(), outlined = true) {
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
                            .background(c.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Wifi, null, tint = c.primary, modifier = Modifier.size(18.dp))
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
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            LkPillBadge(
                                text = "Conectado",
                                containerColor = LkColors.success.copy(alpha = 0.15f),
                                contentColor = LkColors.success,
                            )
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
                    Text(
                        if (ehDualBandUnico) {
                            "Roteador dual-band"
                        } else {
                            val count = nos.size
                            "$count nó${if (count != 1) "s" else ""} detectado${if (count != 1) "s" else ""}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textTertiary,
                    )
                }
            }

            Spacer(Modifier.height(LkSpacing.xs))

            // Nós em árvore
            nos.forEachIndexed { index, no ->
                val isConnected = no.bssid == connectedBssid
                NoTreeItem(
                    rede = no,
                    label =
                        when {
                            isConnected -> "Conectado agora"
                            ehDualBandUnico -> "Mesma rede · ${no.banda}"
                            index == 0 -> "Gateway"
                            else -> "Nó #$index"
                        },
                    isConnected = isConnected,
                    isLast = index == nos.size - 1,
                    onClick = { onNoClick(no) },
                    wifiLinkSnapshot = if (isConnected) wifiLinkSnapshot else null,
                    tipoTopologia = topologiaPorBssid[no.bssid],
                    canalCongestionado = isConnected && canalConectadoCongestionado,
                )
            }

            // Aviso de estimativa quando há mais de um nó
            if (nos.size > 1 && !ehDualBandUnico) {
                Spacer(Modifier.height(LkSpacing.sm))
                Text(
                    "* Gateway estimado pelo sinal mais forte",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                    modifier = Modifier.padding(horizontal = LkSpacing.sm),
                )
            }
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
    canalCongestionado: Boolean = false,
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
                    .background(if (isConnected) c.primary.copy(alpha = 0.12f) else Color.Transparent)
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
                tint = if (isConnected) c.primary else c.textSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(LkSpacing.sm))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.W600,
                        color = if (isConnected) c.primary else c.textPrimary,
                    )
                    if (isConnected) {
                        Spacer(Modifier.width(LkSpacing.sm))
                        LkPillBadge(
                            text = "Conectado",
                            containerColor = c.success.copy(alpha = 0.2f),
                            contentColor = c.success,
                        )
                    }
                    if (canalCongestionado) {
                        Spacer(Modifier.width(LkSpacing.xs))
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = "Canal congestionado",
                            tint = c.warning,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    val topologiaIcon = tipoTopologia?.toIconData(c)
                    if (topologiaIcon != null) {
                        Spacer(Modifier.width(LkSpacing.xs))
                        Icon(
                            imageVector = topologiaIcon.icon,
                            contentDescription = null,
                            tint = if (isConnected) c.primary else topologiaIcon.cor,
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
                        color = signalColor(rede.rssiDbm, bandaVizinha, c),
                    )
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
            SignalBars(
                rssiDbm = rede.rssiDbm,
                banda = bandaVizinha,
                overrideColor = if (isConnected) null else c.warning,
            )
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
                        val topologiaIcon = redeClass.tipo.toIconData(c)
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
                    val bandaSingle =
                        when {
                            rede.frequenciaMhz < 3000 -> BandaWifi.ghz24
                            else -> BandaWifi.ghz5
                        }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(rede.banda, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                        Text("  ·  ", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                        Text(
                            signalQuality(rede.rssiDbm, bandaSingle),
                            style = MaterialTheme.typography.bodySmall,
                            color = signalColor(rede.rssiDbm, bandaSingle, c),
                        )
                    }
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

            // Expandido: resumo (não lista cada nó técnico -- ver # de pontos de acesso
            // de uma rede de terceiros nao ajuda o usuario a decidir nada, so gera ruido)
            if (isExpanded) {
                val melhorRede = grupo.redes.maxByOrNull { it.rede.rssiDbm }?.rede
                if (melhorRede != null) {
                    val bandaMelhor =
                        when {
                            melhorRede.frequenciaMhz < 3000 -> BandaWifi.ghz24
                            else -> BandaWifi.ghz5
                        }
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(start = LkSpacing.lg, top = LkSpacing.sm, bottom = LkSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.DeviceHub, null, tint = c.textTertiary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(LkSpacing.sm))
                        Text(
                            "Rede com ${grupo.redes.size} pontos de acesso · sinal mais forte: " +
                                signalQuality(melhorRede.rssiDbm, bandaMelhor),
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary,
                        )
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
                val topologiaIcon = tipoTopologia?.toIconData(c)
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
    overrideColor: Color? = null,
) {
    val bars =
        when {
            rssiDbm >= -50 -> 4
            rssiDbm >= -60 -> 3
            rssiDbm >= -70 -> 2
            else -> 1
        }
    val c = LocalLkTokens.current
    val color = overrideColor ?: signalColor(rssiDbm, banda, c)
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
                        .clip(RoundedCornerShape(LkRadius.pill))
                        .background(c.primary.copy(alpha = 0.12f))
                        .minimumInteractiveComponentSize()
                        .clickable { onTrocarFaixa(targetBanda) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.sinal_trocar_faixa),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = c.primary,
                )
            }
        }
    }
}

// ─── Network detail sheet ──────────────────────────────────────────────────────

@Composable
private fun NetworkDetailSheet(
    rede: RedeVizinha,
    canalCongestionado: Boolean = false,
    canalRecomendado: Int? = null,
    nivelCanalRecomendado: NivelCongestionamento? = null,
) {
    val c = LocalLkTokens.current
    val ssid = rede.ssid
    val largura = rede.larguraCanalMhz
    val channel = rede.canal

    val bandaDetail =
        when {
            rede.frequenciaMhz < 3000 -> BandaWifi.ghz24
            else -> BandaWifi.ghz5
        }

    LkSheetFrame {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (rede.seguranca == SegurancaWifi.aberta) Icons.Filled.LockOpen else Icons.Filled.Lock,
                contentDescription = null,
                tint = c.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(LkSpacing.sm))
            Text(
                ssid ?: "Rede oculta",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.W700,
                color = c.textPrimary,
            )
        }
        Spacer(Modifier.height(LkSpacing.lg))

        LkSheetInfoRow(
            label = "Sinal",
            value = "${rede.rssiDbm} dBm — ${signalQuality(rede.rssiDbm, bandaDetail)}",
            valueColor = signalColor(rede.rssiDbm, bandaDetail, c),
        )
        LkSheetDivider()
        LkSheetInfoRow(label = "Banda", value = rede.banda)
        channel?.let {
            LkSheetDivider()
            LkSheetInfoRow(label = "Canal", value = it.toString())
        }
        largura?.let {
            LkSheetDivider()
            LkSheetInfoRow(label = "Largura", value = "$it MHz")
        }
        LkSheetDivider()
        LkSheetInfoRow(label = "Segurança", value = securityLabel(rede.seguranca))
        LkSheetDivider()
        LkSheetInfoRow(label = "BSSID", value = rede.bssid)

        Text(
            "Identificador técnico do roteador. Use apenas se você quiser comparar esta rede com o painel do equipamento.",
            style = MaterialTheme.typography.labelMedium,
            color = c.textSecondary,
            modifier = Modifier.padding(top = LkSpacing.xs),
        )

        if (canalCongestionado) {
            Spacer(Modifier.height(LkSpacing.lg))
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LkRadius.card))
                        .background(c.warningContainer)
                        .padding(LkSpacing.lg),
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = c.onWarningContainer,
                        modifier = Modifier.size(20.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Canal congestionado",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.W600,
                            color = c.onWarningContainer,
                        )
                        Text(
                            "Várias redes vizinhas dividem o canal ${rede.canal}. Isso pode aumentar disputa e instabilidade.",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.onWarningContainer.copy(alpha = 0.88f),
                        )
                    }
                }
            }

            if (canalRecomendado != null &&
                canalRecomendado != rede.canal &&
                nivelCanalRecomendado != NivelCongestionamento.congestionado
            ) {
                Spacer(Modifier.height(LkSpacing.md))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(LkRadius.card))
                            .background(c.primary.copy(alpha = 0.12f))
                            .padding(LkSpacing.lg),
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            tint = c.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Troque de canal",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.W600,
                                color = c.primary,
                            )
                            Text(
                                "Mude para o canal $canalRecomendado no roteador. Ele está mais livre agora.",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary,
                            )
                        }
                    }
                }
            }
        }
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
                        canalAtualLivreComAlternativa = { canalAtual, banda ->
                            context.getString(R.string.canal_atual_livre_com_alternativa, canalAtual, banda)
                        },
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

    if (estado == EstadoScanWifi.idle && redes.isEmpty()) {
        CanalIdleState(onRefresh = onRefresh)
        return
    }

    // Erro so ocupa a tela inteira quando nao ha dado anterior pra mostrar — com
    // cache valido, o erro vira so um aviso inline (#893: nao apagar ultimo dado).
    if (estado == EstadoScanWifi.erro && redes.isEmpty()) {
        CanalErroState(erroMensagem = erroMensagem, onRefresh = onRefresh)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LkSpacing.xl),
    ) {
        if (estado == EstadoScanWifi.erro) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(c.error.copy(alpha = 0.1f))
                        .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.WifiFind, null, tint = c.error, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(
                        "Não foi possível atualizar agora. Mostrando o último dado válido.",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Normal,
                        color = c.error,
                    )
                }
            }
        }

        if (bandasDisponiveis.isNotEmpty()) {
            item {
                BandFilterRow(
                    selected = selectedBanda,
                    bands = bandasDisponiveis,
                    onSelect = { selectedBanda = it },
                    counts = bandaCounts + ("Todos" to redes.size),
                    modifier = Modifier.padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
                )
            }
        }

        item {
            Row(
                modifier =
                    Modifier
                        .padding(horizontal = LkSpacing.lg)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LkRadius.card))
                        .background(c.surfaceContainer)
                        .border(1.dp, c.outlineVariant, RoundedCornerShape(LkRadius.card))
                        .padding(LkSpacing.lg),
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = c.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    textoExplicativo,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
            }
            Spacer(Modifier.height(LkSpacing.lg))
        }

        item {
            Column(Modifier.padding(horizontal = LkSpacing.lg)) {
                SectionLabel(
                    if (selectedBanda == "Todos") {
                        "Intensidade por canal"
                    } else {
                        "Intensidade por canal · $selectedBanda"
                    },
                )
                Spacer(Modifier.height(LkSpacing.sm))
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(LkRadius.card))
                            .background(c.surfaceContainer)
                            .border(1.dp, c.outlineVariant, RoundedCornerShape(LkRadius.card))
                            .padding(LkSpacing.md),
                ) {
                    SpectrumChart(
                        espectro = espectro,
                        redesRaw = redesBanda,
                        seuSSID = connectedNetwork?.ssid,
                    )
                }
                Spacer(Modifier.height(LkSpacing.lg))
            }
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
        val dadoCanalAtualParaCard = espectro.dadosPorCanal.find { it.canal == canalAtualParaCard }
        val canalAtualJaLivre = dadoCanalAtualParaCard?.nivel == NivelCongestionamento.livre

        // #1088 — bloco unico de aviso de canal: antes, o banner de congestionamento (canal
        // atual) e o card "Troque de canal" (recomendacao) podiam aparecer juntos na tela,
        // duplicando a mesma orientacao. Agora sao mutuamente exclusivos.
        if (dadoCanalAtualParaCard != null && dadoCanalAtualParaCard.nivel == NivelCongestionamento.congestionado) {
            item {
                CanalCongestionadoBanner(
                    dadoCanal = dadoCanalAtualParaCard,
                    canalRecomendado =
                        canalRecParaCard?.takeIf {
                            it != canalAtualParaCard && nivelCanalRec != NivelCongestionamento.congestionado
                        },
                )
                Spacer(Modifier.height(LkSpacing.lg))
            }
        } else if (canalAtualParaCard != null &&
            (canalRecParaCard == null || canalRecParaCard == canalAtualParaCard || canalAtualJaLivre)
        ) {
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
        } else if (canalAtualParaCard != null &&
            canalRecParaCard != null &&
            canalRecParaCard != canalAtualParaCard &&
            !canalAtualJaLivre &&
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
                    if (selectedBanda == "Todos") "Ocupação dos canais" else "Ocupação dos canais · $selectedBanda",
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
private fun CanalCongestionadoBanner(
    dadoCanal: DadoCanal,
    canalRecomendado: Int? = null,
) {
    val c = LocalLkTokens.current
    // #1088 — aviso unico: quando ha um canal recomendado melhor, a orientacao de troca vai
    // aqui mesmo, em vez de duplicar num CanalRecomendadoCard separado logo abaixo.
    val descricao =
        buildString {
            append("${dadoCanal.countTerceiros} redes vizinhas dividem o canal ${dadoCanal.canal}.")
            if (canalRecomendado != null) {
                append(" Troque para o canal $canalRecomendado, que está mais livre agora.")
            } else {
                append(" Considere ativar o modo automático no roteador.")
            }
        }
    Row(
        modifier =
            Modifier
                .padding(horizontal = LkSpacing.lg)
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.warningContainer.copy(alpha = 0.6f))
                .padding(LkSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Icon(
            imageVector = Icons.Outlined.Lightbulb,
            contentDescription = null,
            tint = c.onWarningContainer,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Canal congestionado",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = c.onWarningContainer,
            )
            Text(
                descricao,
                style = MaterialTheme.typography.bodySmall,
                color = c.onWarningContainer,
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
                tint = c.error,
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
    val accentColor = c.primary
    val gridColor = c.border.copy(alpha = 0.35f)
    val textTertiary = c.textTertiary
    val textMeasurer = rememberTextMeasurer()
    val chartLabelStyle = MaterialTheme.typography.labelSmall.copy(color = textTertiary)
    val spectrumColors =
        listOf(
            c.secondary,
            c.success,
            c.warning,
            c.primary,
            c.error,
            c.secondaryContainer,
            c.successContainer,
            c.warningContainer,
        )

    val redesParaDesenhar =
        remember(redesRaw, seuSSID, accentColor, spectrumColors) {
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
                        cor = if (isSua) accentColor else spectrumColors[idx % spectrumColors.size],
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

        val chartAreaHeight = 130.dp
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

            listOf(-30 to "-30", -50 to "-50", -70 to "-70").forEach { (dBm, label) ->
                val frac = 1f - ((dBm + 90f) / 70f)
                val y = chartH * frac
                drawLine(gridColor, Offset(leftPx, y), Offset(size.width, y), strokeWidth = 0.5.dp.toPx())
                val textLayout = textMeasurer.measure(label, chartLabelStyle)
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
                        chartLabelStyle.copy(color = xLabelColor, fontWeight = xLabelWeight),
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
        Spacer(Modifier.width(LkSpacing.xs))
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
    Box(
        modifier =
            Modifier
                .padding(horizontal = LkSpacing.lg)
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(LkColors.warning.copy(alpha = 0.10f))
                .padding(LkSpacing.lg),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(LkColors.warning.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
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
                Spacer(Modifier.height(LkSpacing.xs))
                Text(
                    "Seu aparelho está em 2,4 GHz. Seu roteador também tem 5 GHz, que é mais rápido e menos congestionado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
                Spacer(Modifier.height(LkSpacing.xs))
                Text(
                    "Para mudar, acesse as configurações do roteador.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textTertiary,
                )
            }
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
                .clip(RoundedCornerShape(LkRadius.card)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LkSurfaceCard(modifier = Modifier.fillMaxWidth(), outlined = false) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(c.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Wifi,
                        contentDescription = null,
                        tint = c.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(LkSpacing.md))
                Column {
                    Text(
                        "Troque de canal",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.W600,
                        color = c.primary,
                    )
                    Spacer(Modifier.height(LkSpacing.xs))
                    Text(
                        descricao,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                }
            }
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
    val corStatus = congestionColor(dado.nivel, c)
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
            color = if (isConnected) c.primary else c.textPrimary,
            modifier = Modifier.widthIn(min = 60.dp),
        )
        if (isConnected) {
            InlineBadge("SEU CANAL", c.primary)
        } else if (dado.ehCanalRecomendado) {
            InlineBadge("RECOMENDADO", c.primary)
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
    LkPillBadge(
        text = label,
        containerColor = color.copy(alpha = 0.12f),
        contentColor = color,
    )
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
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(c.bgSecondary),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(4.dp))
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
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.W600,
            color = tokens.textPrimary,
        )
        Text(
            "Seu aparelho está sem permissão para ler\nas informações de rede móvel.",
            style = MaterialTheme.typography.bodyMedium,
            color = tokens.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(LkSpacing.xs))
        OutlinedButton(onClick = onSolicitarPermissao) {
            Text(
                "Permitir leitura do chip",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
            )
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
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.W600,
            color = tokens.textPrimary,
        )
        Text(
            "Seu aparelho está sem chip de celular ou sem\npermissão para ler as informações de rede móvel.",
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
    val corCongestionamento = congestionColor(dado.nivel, c)
    val isCurrentChannel = dado.ehCanalAtual
    val isRecommended = dado.ehCanalRecomendado

    LkSheetFrame(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Canal ${dado.canal}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.W700,
                color = c.textPrimary,
            )
            Spacer(Modifier.width(LkSpacing.sm))
            if (isCurrentChannel) {
                LkPillBadge(
                    text = "Seu canal",
                    containerColor = LkColors.success.copy(alpha = 0.14f),
                    contentColor = LkColors.success,
                )
            }
            if (isRecommended) {
                LkPillBadge(
                    text = "Recomendado",
                    containerColor = c.primary.copy(alpha = 0.14f),
                    contentColor = c.primary,
                )
            }
        }
        Spacer(Modifier.height(LkSpacing.lg))

        LkSheetSectionTitle(title = "Status")
        Spacer(Modifier.height(LkSpacing.md))
        if (dado.countProprios > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LkStatusDot(color = c.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Você (${dado.countProprios} nó${if (dado.countProprios != 1) "s" else ""})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = c.textPrimary,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        if (dado.countTerceiros > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LkStatusDot(color = LkColors.warning)
                Spacer(Modifier.width(8.dp))
                Text(
                    "${dado.countTerceiros} rede${if (dado.countTerceiros != 1) "s" else ""} de terceiros",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = c.textPrimary,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        if (dado.countProprios == 0 && dado.countTerceiros == 0) {
            Text(
                "Nenhuma rede neste canal",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(LkSpacing.lg))
        LkSheetDivider()
        Spacer(Modifier.height(LkSpacing.lg))

        LkSheetSectionTitle(title = "Análise")
        Spacer(Modifier.height(LkSpacing.md))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isCurrentChannel) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = LkColors.success,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Você está usando este canal",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = c.textPrimary,
                    )
                }
            } else if (isRecommended) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = LkColors.success,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "Recomendado para migração",
                            style = MaterialTheme.typography.bodyMedium,
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
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = LkColors.success,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Canal livre — não há competição",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal,
                            color = c.textPrimary,
                        )
                    }
                }
                NivelCongestionamento.moderado -> {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = LkColors.warning,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Moderado — ${dado.countTerceiros} rede${if (dado.countTerceiros != 1) "s" else ""} compartilhando",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal,
                            color = c.textPrimary,
                        )
                    }
                }
                NivelCongestionamento.congestionado -> {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = corCongestionamento,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Congestionado — ${dado.countTerceiros} redes em competição",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Normal,
                            color = c.textPrimary,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(LkSpacing.lg))
        LkSheetDivider()
        Spacer(Modifier.height(LkSpacing.lg))

        LkSheetSectionTitle(title = "Detalhes Técnicos")
        Spacer(Modifier.height(LkSpacing.md))
        LkSheetInfoRow(label = "Banda", value = espectro.banda)
        LkSheetDivider()
        LkSheetInfoRow(label = "Sinal Máximo", value = "${dado.maxRssiDbm ?: "—"} dBm")
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
                .background(c.primary.copy(alpha = 0.08f))
                .clickable(onClick = onClick)
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm)
                .minimumInteractiveComponentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.WifiFind,
            contentDescription = null,
            tint = c.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            "Permissão de localização necessária para escanear redes",
            style = MaterialTheme.typography.bodySmall,
            color = c.primary,
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
                .background(tokens.primary.copy(alpha = 0.08f))
                .clickable(onClick = onClick)
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm)
                .minimumInteractiveComponentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.SignalCellularAlt,
            contentDescription = null,
            tint = tokens.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            "Toque para ver qualidade do sinal móvel",
            style = MaterialTheme.typography.bodySmall,
            color = tokens.primary,
            modifier = Modifier.weight(1f),
        )
    }
}
