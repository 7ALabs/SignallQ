package io.signallq.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.feature.dns.EstadoBenchmarkDns
import io.signallq.app.feature.dns.ResultadoBenchmarkDns
import io.signallq.app.feature.dns.SnapshotBenchmarkDns
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkInlineBulletText
import io.signallq.app.ui.component.LkNumberedStep
import io.signallq.app.ui.component.LkPillBadge
import io.signallq.app.ui.component.LkSheetDivider
import io.signallq.app.ui.component.LkSheetSectionTitle
import io.signallq.app.ui.component.LkSurfaceCard
import kotlin.math.roundToInt

// ─── Entry point ──────────────────────────────────────────────────────────────

// GH#933 — Fase 4 MD3: migrou de ModalBottomSheet (antigo DnsSheetContent) para tela
// cheia roteada (Overlay.Dns em AppShell). Lógica de benchmark/orientação de troca de
// DNS preservada — só o container visual mudou de sheet para Scaffold+TopAppBar.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsScreen(
    snapshotDns: SnapshotBenchmarkDns,
    dnsResolverIp: String?,
    snapshotRede: SnapshotRede,
    onIniciarBenchmark: () -> Unit,
    onVoltar: () -> Unit,
) {
    val c = LocalLkTokens.current
    val currentDnsName = resolveDnsName(dnsResolverIp)
    var showGuia by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "DNS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (showGuia) showGuia = false else onVoltar() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Voltar",
                            tint = c.textPrimary,
                        )
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
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
        ) {
            if (showGuia) {
                DnsGuideView(c = c, onVoltar = { showGuia = false })
            } else {
                DnsMainContent(
                    snapshotDns = snapshotDns,
                    dnsResolverIp = dnsResolverIp,
                    currentDnsName = currentDnsName,
                    snapshotRede = snapshotRede,
                    c = c,
                    onAbrirGuia = { showGuia = true },
                    onIniciarBenchmark = onIniciarBenchmark,
                )
            }
        }
    }
}

// ─── Conteúdo principal ───────────────────────────────────────────────────────

@Composable
private fun DnsMainContent(
    snapshotDns: SnapshotBenchmarkDns,
    dnsResolverIp: String?,
    currentDnsName: String,
    snapshotRede: SnapshotRede,
    c: LkTokens,
    onAbrirGuia: () -> Unit,
    onIniciarBenchmark: () -> Unit,
) {
    val isLoading = snapshotDns.estado == EstadoBenchmarkDns.executando

    Text("Comparativo de DNS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.W700, color = c.textPrimary)
    Spacer(Modifier.height(4.dp))
    Text(
        "DNS afeta a abertura de sites, não a velocidade da sua conexão.",
        style = MaterialTheme.typography.bodyMedium,
        color = c.textSecondary,
    )
    Spacer(Modifier.height(16.dp))

    // ── Bloco 1 — DNS atual ───────────────────────────────────────────────────
    DnsBloco1Atual(
        snapshotRede = snapshotRede,
        snapshotDns = snapshotDns,
        currentDnsName = currentDnsName,
        c = c,
    )

    Spacer(Modifier.height(16.dp))
    Text("Latência via DoH · menor é melhor", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
    Spacer(Modifier.height(12.dp))

    // ── Bloco 2 — Benchmark ───────────────────────────────────────────────────
    when {
        snapshotDns.estado == EstadoBenchmarkDns.idle -> {
            Button(
                onClick = onIniciarBenchmark,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Comparar servidores DNS")
            }
        }
        snapshotDns.estado == EstadoBenchmarkDns.erro -> {
            val mensagemErro =
                if (!snapshotRede.conectado || snapshotDns.erroMensagem == "semRede") {
                    "Sem conexão para comparar DNS."
                } else {
                    "Não consegui comparar DNS nesta conexão. Tente novamente quando a rede estabilizar."
                }
            Text(text = mensagemErro, style = MaterialTheme.typography.bodyMedium, color = LkColors.error)
        }
        isLoading && snapshotDns.resultados.isEmpty() -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = c.primary)
                Spacer(Modifier.width(10.dp))
                Text("Medindo servidores...", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            }
        }
        else -> {
            val recomendadoNome =
                snapshotDns.resultados
                    .filter { it.tempoMs != null }
                    .minByOrNull { it.tempoMs!! }
                    ?.nomeProvedor

            LkSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    snapshotDns.resultados.forEachIndexed { index, server ->
                        DnsRowSheet(
                            result = server,
                            isCurrent = server.nomeProvedor == currentDnsName,
                            isRecomendado = server.nomeProvedor == recomendadoNome,
                            c = c,
                        )
                        if (index < snapshotDns.resultados.lastIndex) {
                            HorizontalDivider(color = c.border, thickness = 1.dp)
                        }
                    }
                }
            }

            if (isLoading) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = c.primary.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Medindo...", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                }
            }

            // ── Bloco 3 — Recomendação ────────────────────────────────────────
            val melhor = snapshotDns.resultados.filter { it.tempoMs != null }.minByOrNull { it.tempoMs!! }
            if (melhor != null && snapshotDns.estado != EstadoBenchmarkDns.executando) {
                Spacer(Modifier.height(16.dp))
                DnsBloco3Recomendacao(melhor = melhor, c = c)
            }
        }
    }

    Spacer(Modifier.height(20.dp))

    // ── Bloco 4 — Guia colapsável ─────────────────────────────────────────────
    DnsBloco4Guia(c = c, onAbrirGuiaCompleto = onAbrirGuia)
}

// ─── Bloco 1 — DNS atual com skeleton ─────────────────────────────────────────

@Composable
private fun DnsBloco1Atual(
    snapshotRede: SnapshotRede,
    snapshotDns: SnapshotBenchmarkDns,
    currentDnsName: String,
    c: LkTokens,
) {
    val dnsIp = snapshotRede.dnsServidores.firstOrNull()

    // Loading quando ainda não há servidores DNS coletados pelo MonitorRede.
    // #378: offline nunca coleta dnsServidores — sem essa checagem o skeleton ficava
    // girando para sempre em vez de refletir a ausência de conexão.
    val isLoading = snapshotRede.dnsServidores.isEmpty() && snapshotRede.conectado
    val semDadosOffline = snapshotRede.dnsServidores.isEmpty() && !snapshotRede.conectado

    LkSurfaceCard(modifier = Modifier.fillMaxWidth()) {
        if (semDadosOffline) {
            Text(
                "Sem conexão para comparar DNS.",
                style = MaterialTheme.typography.bodyMedium,
                color = LkColors.error,
            )
        } else if (isLoading) {
            DnsSkeletonBloco1(c = c)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Seu DNS atual", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.W500, color = c.textTertiary)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentDnsName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W600, color = c.textPrimary)
                        if (dnsIp != null) {
                            Text(dnsIp, style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                        }
                    }

                    // Roteador local: latência local (sub-ms) não é comparável com DNS externos.
                    // DNS público: exibe latência medida no benchmark.
                    if (currentDnsName != "Roteador da rede") {
                        val latenciaAtual =
                            snapshotDns.resultados
                                .firstOrNull { it.nomeProvedor == currentDnsName }
                                ?.tempoMs
                        if (latenciaAtual != null) {
                            Text(
                                "${latenciaAtual.roundToInt()} ms",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.W500,
                                color = c.textSecondary,
                            )
                        }
                    }
                }

                // Aviso contextual quando o DNS ativo é o roteador local
                if (currentDnsName == "Roteador da rede") {
                    Text(
                        "O roteador repassa as consultas ao DNS real do provedor. A latência local não é comparável com DNS públicos externos.",
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textTertiary,
                    )
                }

                if (snapshotRede.privateDnsAtivo) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        LkPillBadge(
                            text = "DNS Privado ativo",
                            containerColor = LkColors.success.copy(alpha = 0.12f),
                            contentColor = LkColors.success,
                        )
                        val hostname = snapshotRede.privateDnsHostname
                        if (!hostname.isNullOrBlank()) {
                            Text(hostname, style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                        }
                    }
                }
            }
        }
    }
}

// ─── Skeleton shimmer ─────────────────────────────────────────────────────────

@Composable
private fun DnsSkeletonBloco1(c: LkTokens) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 300f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "shimmer_x",
    )

    val shimmerBrush =
        Brush.linearGradient(
            colors =
                listOf(
                    c.border.copy(alpha = 0.5f),
                    Color.White.copy(alpha = 0.15f),
                    c.border.copy(alpha = 0.5f),
                ),
            start = Offset(translateX, 0f),
            end = Offset(translateX + 300f, 0f),
        )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier =
                Modifier
                    .width(80.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush),
        )
        Box(
            modifier =
                Modifier
                    .width(140.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush),
        )
        Box(
            modifier =
                Modifier
                    .width(100.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush),
        )
    }
}

// ─── Bloco 3 — Recomendação ───────────────────────────────────────────────────

@Composable
private fun DnsBloco3Recomendacao(
    melhor: ResultadoBenchmarkDns,
    c: LkTokens,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(LkColors.success.copy(alpha = 0.10f))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "Resultado do teste",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.W500,
            color = LkColors.success,
        )
        val tempo = melhor.tempoMs?.roundToInt()
        Text(
            "Neste teste, ${melhor.nomeProvedor} respondeu mais rápido." +
                if (tempo != null) " ($tempo ms)" else "",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.W600,
            color = c.textPrimary,
        )
        Text(
            "Isso não troca o DNS automaticamente. Para alterar, você precisa configurar no Android ou no roteador.",
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
        )
    }
}

// ─── Bloco 4 — Guia colapsável ────────────────────────────────────────────────

@Composable
private fun DnsBloco4Guia(
    c: LkTokens,
    onAbrirGuiaCompleto: () -> Unit,
) {
    var expandido by remember { mutableStateOf(false) }

    Column {
        // cabeçalho clicável
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.input))
                    .clickable { expandido = !expandido }
                    .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Quando vale a pena trocar DNS?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expandido) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expandido) "Recolher" else "Expandir",
                tint = c.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }

        AnimatedVisibility(
            visible = expandido,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier.padding(bottom = LkSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Quando vale a pena
                Text(
                    "Vale a pena trocar quando:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.W500,
                    color = c.textSecondary,
                )
                DnsGuiaBullet("Sites demoram para abrir mesmo com boa velocidade de download")
                DnsGuiaBullet("O DNS atual apresentou latência alta neste comparativo")
                DnsGuiaBullet("Você quer filtro de anúncios ou rastreadores (ex.: AdGuard, Quad9)")
                DnsGuiaBullet("Está em rede que bloqueia domínios sem motivo aparente")

                Spacer(Modifier.height(4.dp))

                // Quando não faz diferença
                Text(
                    "Quando não faz diferença:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.W500,
                    color = c.textSecondary,
                )
                DnsGuiaBullet("A lentidão é no download/upload — isso é velocidade de conexão, não DNS")
                DnsGuiaBullet("Todos os DNS testados tiveram latência similar (< 10 ms de diferença)")
                DnsGuiaBullet("O site que você acessa usa IP fixo em cache local")

                Spacer(Modifier.height(8.dp))
            }
        }

        HorizontalDivider(color = c.border, thickness = 1.dp)
        Spacer(Modifier.height(LkSpacing.sm))

        // link para o guia completo de como alterar
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAbrirGuiaCompleto() }
                    .padding(vertical = LkSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Como alterar meu DNS",
                style = MaterialTheme.typography.labelLarge,
                color = c.primary,
                fontWeight = FontWeight.W500,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = c.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DnsGuiaBullet(
    texto: String,
) {
    LkInlineBulletText(text = texto)
}

// ─── Row de benchmark ─────────────────────────────────────────────────────────

@Composable
private fun DnsRowSheet(
    result: ResultadoBenchmarkDns,
    isCurrent: Boolean,
    isRecomendado: Boolean,
    c: LkTokens,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    result.nomeProvedor,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = if (isRecomendado) LkColors.success else c.textPrimary,
                )
                if (isCurrent) {
                    Spacer(Modifier.width(LkSpacing.sm))
                    LkPillBadge("atual", c.primary.copy(alpha = 0.12f), c.primary)
                }
                if (isRecomendado) {
                    Spacer(Modifier.width(LkSpacing.sm))
                    LkPillBadge("mais rápido", LkColors.success.copy(alpha = 0.12f), LkColors.success)
                }
            }
        }
        val tempoMs = result.tempoMs
        if (result.erroMensagem != null && tempoMs == null) {
            Text("Falhou", style = MaterialTheme.typography.bodySmall, color = LkColors.error)
        } else if (tempoMs != null) {
            Text("${tempoMs.roundToInt()} ms", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W600, color = c.textPrimary)
            Spacer(Modifier.width(8.dp))
            result.gradeRapidez?.let { DnsGradeBadge(grade = it, c = c) }
        }
    }
}

// ─── Grade badge ──────────────────────────────────────────────────────────────

@Composable
private fun DnsGradeBadge(
    grade: String,
    c: LkTokens,
) {
    val color =
        when (grade) {
            "A" -> LkColors.success
            "B" -> c.primary
            "C" -> LkColors.warning
            else -> LkColors.error
        }
    Box(
        modifier =
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(grade, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.W700, color = color)
    }
}

// ─── Guia completo (como alterar DNS) ────────────────────────────────────────

@Composable
private fun DnsGuideView(
    c: LkTokens,
    onVoltar: () -> Unit,
) {
    var tabSelecionada by remember { mutableIntStateOf(0) }
    Column {
        TextButton(
            onClick = onVoltar,
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = c.textSecondary,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Voltar ao comparativo",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
        }
        Spacer(Modifier.height(12.dp))
        LkSheetSectionTitle(
            title = "Como alterar meu DNS",
            subtitle = "Escolha onde prefere alterar:",
        )
        Spacer(Modifier.height(16.dp))

        val tabs = listOf("Dispositivo", "Roteador")
        TabRow(
            selectedTabIndex = tabSelecionada,
            containerColor = c.bgPrimary,
            contentColor = c.primary,
            divider = { LkSheetDivider() },
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[tabSelecionada]),
                    color = c.primary,
                    height = 2.dp,
                )
            },
        ) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = tabSelecionada == idx,
                    onClick = { tabSelecionada = idx },
                    text = {
                        Text(
                            label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (tabSelecionada == idx) FontWeight.W600 else FontWeight.W400,
                            color = if (tabSelecionada == idx) c.primary else c.textSecondary,
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        if (tabSelecionada == 0) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Android · DNS Privado",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                )
                DnsGuideStep(1, "Abra as Configurações do sistema")
                DnsGuideStep(2, "Vá em Rede e internet → DNS privado")
                DnsGuideStep(3, "Selecione \"Nome do host do DNS privado\"")
                DnsGuideStep(4, "Digite o hostname do servidor DNS desejado")
                DnsGuideStep(5, "Toque em Salvar")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Esta configuração afeta apenas este dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textTertiary,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Configurações do Roteador",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                )
                DnsGuideStep(1, "Acesse o painel admin do roteador (geralmente 192.168.0.1 ou 192.168.1.1)")
                DnsGuideStep(2, "Faça login com as credenciais (veja na etiqueta do roteador)")
                DnsGuideStep(3, "Localize as configurações de Rede ou WAN")
                DnsGuideStep(4, "Encontre o campo DNS primário e DNS secundário")
                DnsGuideStep(5, "Insira os endereços do servidor DNS desejado")
                DnsGuideStep(6, "Salve e aguarde o roteador reiniciar")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Esta configuração afeta todos os dispositivos conectados à rede.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun DnsGuideStep(
    numero: Int,
    texto: String,
) {
    LkNumberedStep(number = numero, text = texto)
}

// ─── Utilitário ───────────────────────────────────────────────────────────────

// Deve estar sincronizado com BenchmarkDnsDoh.mapaIpParaProvedor —
// qualquer IP adicionado lá precisa ser adicionado aqui também.
internal fun resolveDnsName(dnsIp: String?): String {
    if (dnsIp != null && isDnsIpPrivado(dnsIp)) return "Roteador da rede"
    return when (dnsIp) {
        "1.1.1.1", "1.0.0.1" -> "Cloudflare"
        "8.8.8.8", "8.8.4.4" -> "Google DNS"
        "9.9.9.9", "149.112.112.112" -> "Quad9"
        "208.67.222.222", "208.67.220.220" -> "OpenDNS"
        "94.140.14.14", "94.140.15.15" -> "AdGuard"
        "76.76.2.0", "76.76.10.0" -> "Control D"
        "185.228.168.9", "185.228.169.9" -> "CleanBrowsing"
        else -> "DNS do Provedor"
    }
}

// IP RFC-1918, link-local ou loopback — não é um DNS público real.
internal fun isDnsIpPrivado(ip: String): Boolean {
    val partes = ip.split(".").mapNotNull { it.toIntOrNull() }
    if (partes.size != 4) return false
    val (a, b) = partes
    return when {
        a == 10 -> true
        a == 172 && b in 16..31 -> true
        a == 192 && b == 168 -> true
        a == 169 && b == 254 -> true
        a == 127 -> true
        else -> false
    }
}
