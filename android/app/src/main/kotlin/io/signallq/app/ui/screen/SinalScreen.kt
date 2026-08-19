package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiFind
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import io.signallq.app.R
import io.signallq.app.core.diagnostico.NivelCongestionamento
import io.signallq.app.core.diagnostico.RedeWifiVizinha
import io.signallq.app.core.diagnostico.WifiChannelDiagnosticEngine
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.WifiLinkSnapshot
import io.signallq.app.core.network.wifi.SnapshotScanWifi
import io.signallq.app.core.telephony.MovelSimSnapshot
import io.signallq.app.core.telephony.MovelSnapshot
import io.signallq.app.feature.devices.DispositivoRede
import io.signallq.app.feature.wifi.RedeVizinha
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.OperadoraSource
import io.signallq.app.ui.ResolvedOperadoraIdentity
import io.signallq.app.ui.component.OfflineBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
    onAbrirMenu: () -> Unit = {},
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
    // GH#970 — resolucao de identidade de operadora (nivel 1, catalogo local, sincrono).
    // Sem I/O, sem corrotina — mesmo comportamento de sempre pras ~12 operadoras principais.
    // Mesmo padrao ja usado em HomeScreen/DiagnosticoGuiadoScreen — injetado a partir da
    // MainActivity (OperadoraDirectoryResolver via Hilt), AppShell so repassa.
    resolveOperadoraIdentidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity? =
        { _, _ -> null },
    // GH#970 — cadeia completa (local -> diretorio remoto do worker signallq-diagnostic ->
    // fallback generico), so chamada quando o nivel 1 acima nao encontrou.
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
                    Text("Sinal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W600, color = c.textPrimary)
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
                        resolveOperadoraIdentidadeLocal = resolveOperadoraIdentidadeLocal,
                        resolveOperadoraIdentidadeRemota = resolveOperadoraIdentidadeRemota,
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
                                larguraCanalMhz = it.larguraCanalMhz,
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
                                tint = c.warning,
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
