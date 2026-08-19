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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.signallq.app.R
import io.signallq.app.core.diagnostico.BandaWifi
import io.signallq.app.core.diagnostico.DadoCanal
import io.signallq.app.core.diagnostico.NivelCongestionamento
import io.signallq.app.core.diagnostico.RedeWifiVizinha
import io.signallq.app.core.diagnostico.SnapshotEspectroCanal
import io.signallq.app.core.diagnostico.WifiChannelDiagnosticEngine
import io.signallq.app.core.network.WifiLinkSnapshot
import io.signallq.app.core.network.wifi.EstadoScanWifi
import io.signallq.app.feature.diagnostico.CanalStrings
import io.signallq.app.feature.diagnostico.CanalTextGenerator
import io.signallq.app.feature.wifi.RedeVizinha
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkPillBadge
import io.signallq.app.ui.component.LkSheetDivider
import io.signallq.app.ui.component.LkSheetFrame
import io.signallq.app.ui.component.LkSheetInfoRow
import io.signallq.app.ui.component.LkSheetSectionTitle
import io.signallq.app.ui.component.LkStatusDot
import io.signallq.app.ui.component.LkSurfaceCard

// ─── Tab Canal ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CanalTab(
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
    // GH#1207 item 6 — sem isso, o filtro ficava preso na banda de quando a tela abriu: se a
    // conexão trocar de 2,4 GHz pra 5 GHz (roaming/troca manual) com a tela aberta, o filtro,
    // o gráfico e a recomendação continuavam na banda antiga. So reage a mudança de banda
    // conectada quando o usuário não escolheu manualmente uma banda diferente.
    var bandaEscolhidaManualmente by remember { mutableStateOf(false) }
    LaunchedEffect(connectedNetwork?.banda) {
        if (!bandaEscolhidaManualmente) {
            selectedBanda = connectedNetwork?.banda ?: "Todos"
        }
    }
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
                            larguraCanalMhz = it.larguraCanalMhz,
                        )
                    },
                canalAtual = canalAtual,
                banda = selectedBanda,
                seuSSID = connectedNetwork?.ssid,
                // GH#1207 item 2 — banda da rede conectada, distinta do filtro "Todos"
                // selecionado pelo usuario; sem isso, `ehCanalAtual` no modo Todos nao sabia em
                // qual banda o canal atual realmente esta.
                bandaConectada = connectedNetwork?.banda,
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
                    onSelect = {
                        selectedBanda = it
                        bandaEscolhidaManualmente = true
                    },
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
                            .background(c.success.copy(alpha = 0.08f))
                            .padding(LkSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = c.success,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(
                        // GH#1207 item 5 — "canal ideal" afirma mais do que um unico scan
                        // sustenta; troca por texto proporcional a evidencia real (nenhum
                        // ganho relevante encontrado agora, nao "ideal" para sempre).
                        "O canal atual apresentou baixa interferência no scan realizado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.success,
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
            // GH#1207 item 2 — chave composta banda+canal: no modo "Todos" o mesmo número de
            // canal pode existir em bandas diferentes (ex.: 149 em 5GHz e em 6GHz), o que antes
            // colidia como chave duplicada no LazyColumn.
            items(canalOrdenados, key = { "${it.banda}_${it.canal}" }) { dado ->
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

            // #1131 (bug 1) — canalToX posiciona pelo VALOR numerico do canal, nao pelo indice.
            // Com muitos canais proximos (ex.: 36,40,44,48 em 5GHz) os rotulos colam uns nos
            // outros virando uma sequencia ilegivel. Desenha em ordem crescente e so escreve
            // o proximo rotulo quando ele nao invade o espaco do ultimo rotulo desenhado —
            // reduz a quantidade de numeros exibidos sem afetar as curvas do espectro.
            val labelMinGapPx = LkSpacing.xs.toPx()
            var lastLabelRight = Float.NEGATIVE_INFINITY
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
                val labelLeft = x - xLayout.size.width / 2f
                if (labelLeft >= lastLabelRight + labelMinGapPx) {
                    drawText(
                        xLayout,
                        topLeft = Offset(labelLeft, chartH + (xAxisH - xLayout.size.height) / 2),
                    )
                    lastLabelRight = labelLeft + xLayout.size.width
                }
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
                .background(c.warning.copy(alpha = 0.10f))
                .padding(LkSpacing.lg),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(c.warning.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = c.warning,
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
                    // GH#1207 item 7 — a checagem so confirma "mesmo SSID em frequencia mais
                    // alta", nao que o BSSID pertence ao mesmo equipamento/mesh, nem que a
                    // outra banda esta de fato menos congestionada. Copy nao afirma band
                    // steering nem "seu roteador" como fato.
                    "Seu aparelho está em 2,4 GHz. Foi encontrada outra rede com o mesmo nome em " +
                        "uma frequência mais alta, que costuma ser mais rápida e menos congestionada.",
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
    // GH#1207 item 4 — a barra usava `count / 8`, independente do nível classificado (podia
    // mostrar barra cheia num canal marcado como livre). Agora usa a mesma fração de score
    // espectral (fracaoInterferencia) que decide `nivel` e a recomendação.
    val fracaoUso = dado.fracaoInterferencia.toFloat().coerceIn(0f, 1f)

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
                    containerColor = c.success.copy(alpha = 0.14f),
                    contentColor = c.success,
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
                LkStatusDot(color = c.warning)
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
                        tint = c.success,
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
                        tint = c.success,
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
                            tint = c.success,
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
                            tint = c.warning,
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

        LkSheetSectionTitle(title = "Detalhes técnicos")
        Spacer(Modifier.height(LkSpacing.md))
        LkSheetInfoRow(label = "Banda", value = espectro.banda)
        LkSheetDivider()
        val maxRssi = dado.maxRssiDbm
        val bandaMaxRssi =
            when {
                espectro.banda.contains("5") -> BandaWifi.ghz5
                espectro.banda.contains("2.4") -> BandaWifi.ghz24
                else -> BandaWifi.desconhecida
            }
        LkSheetInfoRow(
            label = "Sinal máximo",
            value =
                if (maxRssi != null) {
                    "$maxRssi dBm · ${signalQuality(maxRssi, bandaMaxRssi)}"
                } else {
                    "— dBm"
                },
        )
    }
}
