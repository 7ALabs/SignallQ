package io.signallq.app.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiFind
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.signallq.app.R
import io.signallq.app.core.diagnostico.BandaWifi
import io.signallq.app.core.diagnostico.NivelCongestionamento
import io.signallq.app.core.diagnostico.RedeWifiVizinha
import io.signallq.app.core.diagnostico.WifiChannelDiagnosticEngine
import io.signallq.app.core.network.WifiLinkSnapshot
import io.signallq.app.core.network.wifi.EstadoScanWifi
import io.signallq.app.core.network.wifi.SnapshotScanWifi
import io.signallq.app.feature.devices.DispositivoRede
import io.signallq.app.feature.devices.chaveApelido
import io.signallq.app.feature.wifi.ConfiancaTopologia
import io.signallq.app.feature.wifi.GrupoRedeWifi
import io.signallq.app.feature.wifi.RedeClassificada
import io.signallq.app.feature.wifi.RedeVizinha
import io.signallq.app.feature.wifi.SegurancaWifi
import io.signallq.app.feature.wifi.TipoTopologia
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkPillBadge
import io.signallq.app.ui.component.LkSheetDivider
import io.signallq.app.ui.component.LkSheetFrame
import io.signallq.app.ui.component.LkSheetInfoRow
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.SignalBars
import io.signallq.app.ui.component.signalColor

// ─── Tab Redes ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RedesTab(
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
    val classificacaoPorBssid =
        remember(snapshotWifi.redes, connectedNetwork) {
            runCatching {
                classificarComMotorUnificado(
                    redes = snapshotWifi.redes,
                    connectedBssid = connectedNetwork?.bssid,
                ).associateBy { it.rede.bssid }
            }.getOrElse { emptyMap() }
        }
    val topologiaPorBssid = remember(classificacaoPorBssid) { classificacaoPorBssid.mapValues { it.value.tipo } }
    // GH#1209 item 8 — confiança por BSSID, usada pro rótulo "Gateway" só afirmar o papel de
    // roteador quando o motor tem confiança suficiente (nunca por "sinal mais forte").
    val confiancaPorBssid = remember(classificacaoPorBssid) { classificacaoPorBssid.mapValues { it.value.confianca } }

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

    // Nós da mesma rede: conectado na frente + mesmo SSID ordenado por sinal.
    // GH#1209 item 1 (bug principal) — mesmo SSID sozinho não comprova mesma infraestrutura
    // (rede de vizinho com nome igual, SSID público/padrão, hotspot falso). Antes desta
    // correção, qualquer BSSID com o mesmo SSID do conectado entrava aqui direto. Agora só
    // entra quando o motor de topologia unificado (TopologiaRedeEngine) já classificou o BSSID
    // com alguma evidência (OUI/banda/SSID) — DESCONHECIDO (sem evidência) fica em "outras
    // redes", mesmo com SSID idêntico.
    val grupoNos =
        remember(filteredRedes, connectedNetwork, topologiaPorBssid) {
            if (connectedNetwork == null) return@remember emptyList()
            buildList {
                add(connectedNetwork)
                addAll(
                    filteredRedes
                        .filter { it.bssid != connectedNetwork.bssid }
                        .filter { rede ->
                            rede.ssid != null &&
                                rede.ssid == connectedNetwork.ssid &&
                                ehMembroDaEstruturaPropria(topologiaPorBssid[rede.bssid])
                        }.sortedByDescending { it.rssiDbm },
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
                            larguraCanalMhz = it.larguraCanalMhz,
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

    // Redes de outros SSIDs/BSSIDs não correlacionados — classificadas e agrupadas.
    // GH#1209 — antes excluía qualquer BSSID de mesmo SSID daqui (mesmo sem correlação de
    // topologia), fazendo uma rede de vizinho homônima sumir da tela inteira. Agora só exclui
    // quem de fato entrou no grupo "sua conexão" (mesmo critério de [grupoNos]).
    val otherClassificadas =
        remember(filteredRedes, connectedNetwork, filteredRedes.size, topologiaPorBssid) {
            val connSsid = connectedNetwork?.ssid
            val filtered =
                filteredRedes
                    .filter { it.bssid != connectedNetwork?.bssid }
                    .filter { rede ->
                        val mesmoSsidDoConectado = connSsid != null && rede.ssid == connSsid
                        !(mesmoSsidDoConectado && ehMembroDaEstruturaPropria(topologiaPorBssid[rede.bssid]))
                    }

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

            // GH#1209 item 4 — redes ocultas (SSID nulo) diferentes não são a mesma rede só por
            // não terem nome; agrupar todas como "[Ocultas]" fabricava uma rede falsa com vários
            // APs. Cada BSSID oculto vira seu próprio grupo, salvo correlação futura do motor.
            classificadas
                .groupBy { it.rede.ssid ?: "[Oculta] ${it.rede.bssid}" }
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
                                .background(c.success.copy(alpha = 0.12f)),
                    ) {
                        GrupoRedeTree(
                            ssid = connectedNetwork.ssid ?: "Rede oculta",
                            nos = grupoNos,
                            connectedBssid = connectedNetwork.bssid,
                            onNoClick = onNoOuRedeClick,
                            wifiLinkSnapshot = wifiLinkSnapshot,
                            topologiaPorBssid = topologiaPorBssid,
                            confiancaPorBssid = confiancaPorBssid,
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
    confiancaPorBssid: Map<String, ConfiancaTopologia> = emptyMap(),
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
        LkSurfaceCard(modifier = Modifier.fillMaxWidth()) {
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
                                containerColor = c.success.copy(alpha = 0.15f),
                                contentColor = c.success,
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

            // Nós em árvore.
            // GH#1209 item 8 — o rótulo "Gateway" só é usado quando o motor de topologia
            // unificado classificou o nó como ROTEADOR com confiança ALTA (evidência real de
            // OUI/banda/SSID) — nunca inferido pela posição na lista ou pelo sinal mais forte,
            // que é tecnicamente inválido (em mesh, o nó de sinal mais forte pode ser um
            // satélite, não o gateway).
            nos.forEachIndexed { index, no ->
                val isConnected = no.bssid == connectedBssid
                val tipoDoNo = topologiaPorBssid[no.bssid]
                val ehRoteadorConfirmado = tipoDoNo == TipoTopologia.ROTEADOR && confiancaPorBssid[no.bssid] == ConfiancaTopologia.ALTA
                NoTreeItem(
                    rede = no,
                    label =
                        when {
                            isConnected -> "Conectado agora"
                            ehDualBandUnico -> "Mesma rede · ${no.banda}"
                            ehRoteadorConfirmado -> "Roteador"
                            else -> "Nó #$index"
                        },
                    isConnected = isConnected,
                    isLast = index == nos.size - 1,
                    onClick = { onNoClick(no) },
                    wifiLinkSnapshot = if (isConnected) wifiLinkSnapshot else null,
                    tipoTopologia = tipoDoNo,
                    canalCongestionado = isConnected && canalConectadoCongestionado,
                )
            }

            // GH#1209 item 8 — só mostra o aviso de incerteza quando existe de fato ambiguidade
            // (algum nó com confiança abaixo de ALTA); quando o motor confirma os papéis com
            // confiança alta, não há necessidade de alertar sobre estimativa.
            val temNoAmbiguo = nos.any { no -> confiancaPorBssid[no.bssid] != ConfiancaTopologia.ALTA }
            if (nos.size > 1 && !ehDualBandUnico && temNoAmbiguo) {
                Spacer(Modifier.height(LkSpacing.sm))
                Text(
                    "* Estrutura estimada por fabricante/sinal — sem confirmação de rota de rede",
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
            val bandaVizinha = rede.paraBandaWifi()
            // GH#1209 item 5 — pro nó conectado, prioriza o RSSI ao vivo (WifiInfo/
            // WifiLinkSnapshot) sobre o valor do ScanResult, que pode estar atrasado. Nunca
            // mistura RSSI de outro BSSID — só usa o snapshot quando ESTE nó é o conectado
            // (o chamador já garante isso, passando wifiLinkSnapshot=null pra nós não conectados).
            val rssiEfetivo = if (isConnected) (wifiLinkSnapshot?.rssiDbm ?: rede.rssiDbm) else rede.rssiDbm
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
                        signalQuality(rssiEfetivo, bandaVizinha),
                        style = MaterialTheme.typography.bodySmall,
                        color = signalColor(rssiEfetivo, bandaVizinha, c),
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
            // GH#1209 item 9 — antes forçava cor de warning (âmbar) pra qualquer nó não
            // conectado, mesmo com sinal excelente. A cor da barra agora sempre reflete a
            // qualidade real do sinal (SignalBars já calcula isso sozinho); "conectado" é
            // comunicado só pelo badge/ícone/destaque de fundo, nunca pela cor da barra.
            SignalBars(
                rssiDbm = rssiEfetivo,
                banda = bandaVizinha,
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
    // GH#1209 item 4 — cada rede oculta agora tem sua própria chave "[Oculta] <bssid>"
    // (ver otherClassificadas), em vez do bucket único "[Ocultas]" de antes.
    val isOculta = grupo.ssid.startsWith("[Oculta]")

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
                    val bandaSingle = rede.paraBandaWifi()
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
                    imageVector = if (rede.seguranca == SegurancaWifi.aberta) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
                    contentDescription = if (rede.seguranca == SegurancaWifi.aberta) stringResource(R.string.cd_rede_aberta) else stringResource(R.string.cd_rede_protegida),
                    tint = c.textTertiary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(LkSpacing.sm))
                SignalBars(rssiDbm = rede.rssiDbm, banda = rede.paraBandaWifi())
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
                    // GH#1209 item 7 — antes mostrava só a banda de UM BSSID do grupo (2,4GHz
                    // se existisse, senão qualquer outra), mesmo quando o grupo tinha várias
                    // bandas presentes (ex.: roteador dual/tri-band). Agora combina todas.
                    val banda = grupo.redes.map { it.rede }.bandaCombinadaLabel()
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
                val melhorRedeGrupo = grupo.redes.maxByOrNull { it.rede.rssiDbm }?.rede
                SignalBars(
                    rssiDbm = grupo.redes.maxOfOrNull { it.rede.rssiDbm } ?: 0,
                    banda = melhorRedeGrupo?.paraBandaWifi() ?: BandaWifi.desconhecida,
                )
            }

            // Expandido: resumo (não lista cada nó técnico -- ver # de pontos de acesso
            // de uma rede de terceiros nao ajuda o usuario a decidir nada, so gera ruido)
            if (isExpanded) {
                val melhorRede = grupo.redes.maxByOrNull { it.rede.rssiDbm }?.rede
                if (melhorRede != null) {
                    val bandaMelhor = melhorRede.paraBandaWifi()
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

    val bandaDetail = rede.paraBandaWifi()

    LkSheetFrame {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (rede.seguranca == SegurancaWifi.aberta) Icons.Outlined.LockOpen else Icons.Outlined.Lock,
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
            value = "${rede.rssiDbm} dBm · ${signalQuality(rede.rssiDbm, bandaDetail)}",
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
