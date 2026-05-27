package io.linka.app.kotlin.ui.screen

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.sp
import io.linka.app.kotlin.core.network.SnapshotRede
import io.linka.app.kotlin.feature.dns.EstadoBenchmarkDns
import io.linka.app.kotlin.feature.dns.ResultadoBenchmarkDns
import io.linka.app.kotlin.feature.dns.SnapshotBenchmarkDns
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LkTokens
import kotlin.math.roundToInt

// ─── Entry point ──────────────────────────────────────────────────────────────

@Composable
fun DnsSheetContent(
    snapshotDns: SnapshotBenchmarkDns,
    dnsResolverIp: String?,
    snapshotRede: SnapshotRede,
    c: LkTokens,
) {
    val currentDnsName = resolveDnsName(dnsResolverIp)
    var showGuia by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
    ) {
        // drag handle
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
        Spacer(Modifier.height(20.dp))

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
            )
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
) {
    val isLoading = snapshotDns.estado == EstadoBenchmarkDns.executando

    Text("Comparativo de DNS", fontSize = 18.sp, fontWeight = FontWeight.W700, color = c.textPrimary)
    Spacer(Modifier.height(4.dp))
    Text(
        "DNS afeta a abertura de sites, não a velocidade da sua conexão.",
        fontSize = 12.sp,
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
    Text("Latência via DoH · menor é melhor", fontSize = 12.sp, color = c.textSecondary)
    Spacer(Modifier.height(12.dp))

    // ── Bloco 2 — Benchmark ───────────────────────────────────────────────────
    when {
        snapshotDns.estado == EstadoBenchmarkDns.erro -> {
            Text(
                text = "Não consegui comparar DNS nesta conexão. Tente novamente quando a rede estabilizar.",
                fontSize = 13.sp,
                color = LkColors.error,
            )
        }
        isLoading && snapshotDns.resultados.isEmpty() -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = LkColors.accent)
                Spacer(Modifier.width(10.dp))
                Text("Medindo servidores...", fontSize = 13.sp, color = c.textSecondary)
            }
        }
        else -> {
            val recomendadoNome =
                snapshotDns.resultados
                    .filter { it.tempoMs != null }
                    .minByOrNull { it.tempoMs!! }
                    ?.nomeProvedor

            HorizontalDivider(color = c.border, thickness = 1.dp)

            // LazyColumn com altura máxima para não crescer indefinidamente
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(snapshotDns.resultados) { server ->
                    DnsRowSheet(
                        result = server,
                        isCurrent = server.nomeProvedor == currentDnsName,
                        isRecomendado = server.nomeProvedor == recomendadoNome,
                        c = c,
                    )
                    HorizontalDivider(color = c.border, thickness = 1.dp)
                }
            }

            if (isLoading) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = LkColors.accent.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Medindo...", fontSize = 11.sp, color = c.textTertiary)
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

    // Loading quando ainda não há servidores DNS coletados pelo MonitorRede
    val isLoading = snapshotRede.dnsServidores.isEmpty()

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.bgPrimary)
                .padding(12.dp),
    ) {
        if (isLoading) {
            DnsSkeletonBloco1(c = c)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Seu DNS atual", fontSize = 11.sp, fontWeight = FontWeight.W500, color = c.textTertiary)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentDnsName, fontSize = 14.sp, fontWeight = FontWeight.W600, color = c.textPrimary)
                        if (dnsIp != null) {
                            Text(dnsIp, fontSize = 11.sp, color = c.textSecondary)
                        }
                    }

                    // latência do DNS atual nos resultados do benchmark
                    val latenciaAtual =
                        snapshotDns.resultados
                            .firstOrNull { it.nomeProvedor == currentDnsName }
                            ?.tempoMs
                    if (latenciaAtual != null) {
                        Text(
                            "${latenciaAtual.roundToInt()} ms",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.W500,
                            color = c.textSecondary,
                        )
                    }
                }

                if (snapshotRede.privateDnsAtivo) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(LkColors.success.copy(alpha = 0.12f))
                                    .padding(horizontal = LkSpacing.sm, vertical = 3.dp),
                        ) {
                            Text(
                                "DNS Privado ativo",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.W600,
                                color = LkColors.success,
                            )
                        }
                        val hostname = snapshotRede.privateDnsHostname
                        if (!hostname.isNullOrBlank()) {
                            Text(hostname, fontSize = 11.sp, color = c.textTertiary)
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
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LkColors.success.copy(alpha = 0.08f))
                .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Resultado do teste",
                fontSize = 11.sp,
                fontWeight = FontWeight.W500,
                color = LkColors.success,
            )
            val tempo = melhor.tempoMs?.roundToInt()
            Text(
                "Neste teste, ${melhor.nomeProvedor} respondeu mais rápido." +
                    if (tempo != null) " ($tempo ms)" else "",
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Text(
                "Isso não troca o DNS automaticamente. Para alterar, você precisa configurar no Android ou no roteador.",
                fontSize = 12.sp,
                color = c.textSecondary,
            )
        }
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
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expandido = !expandido }
                    .padding(vertical = LkSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Quando vale a pena trocar DNS?",
                fontSize = 13.sp,
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W500,
                    color = c.textSecondary,
                )
                DnsGuiaBullet("Sites demoram para abrir mesmo com boa velocidade de download", c)
                DnsGuiaBullet("O DNS atual apresentou latência alta neste comparativo", c)
                DnsGuiaBullet("Você quer filtro de anúncios ou rastreadores (ex.: AdGuard, Quad9)", c)
                DnsGuiaBullet("Está em rede que bloqueia domínios sem motivo aparente", c)

                Spacer(Modifier.height(4.dp))

                // Quando não faz diferença
                Text(
                    "Quando não faz diferença:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.W500,
                    color = c.textSecondary,
                )
                DnsGuiaBullet("A lentidão é no download/upload — isso é velocidade de conexão, não DNS", c)
                DnsGuiaBullet("Todos os DNS testados tiveram latência similar (< 10 ms de diferença)", c)
                DnsGuiaBullet("O site que você acessa usa IP fixo em cache local", c)

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
            Text("Como alterar meu DNS", fontSize = 13.sp, color = LkColors.accent, fontWeight = FontWeight.W500)
        }
    }
}

@Composable
private fun DnsGuiaBullet(
    texto: String,
    c: LkTokens,
) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier =
                Modifier
                    .padding(top = 5.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(c.textTertiary),
        )
        Text(texto, fontSize = 12.sp, color = c.textSecondary, modifier = Modifier.weight(1f))
    }
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
        modifier = Modifier.fillMaxWidth().padding(vertical = LkSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    result.nomeProvedor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W600,
                    color = if (isRecomendado) LkColors.success else c.textPrimary,
                )
                if (isCurrent) {
                    Spacer(Modifier.width(LkSpacing.sm))
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(LkColors.accent.copy(alpha = 0.12f))
                                .padding(horizontal = LkSpacing.sm, vertical = 4.dp),
                    ) {
                        Text("atual", fontSize = 10.sp, fontWeight = FontWeight.W600, color = LkColors.accent)
                    }
                }
                if (isRecomendado) {
                    Spacer(Modifier.width(LkSpacing.sm))
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(LkColors.success.copy(alpha = 0.12f))
                                .padding(horizontal = LkSpacing.sm, vertical = 4.dp),
                    ) {
                        Text("mais rápido", fontSize = 10.sp, fontWeight = FontWeight.W600, color = LkColors.success)
                    }
                }
            }
        }
        val tempoMs = result.tempoMs
        if (result.erroMensagem != null && tempoMs == null) {
            Text("Falhou", fontSize = 12.sp, color = LkColors.error)
        } else if (tempoMs != null) {
            Text("${tempoMs.roundToInt()} ms", fontSize = 13.sp, fontWeight = FontWeight.W500, color = c.textSecondary)
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
            "B" -> LkColors.accent
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
        Text(grade, fontSize = 12.sp, fontWeight = FontWeight.W700, color = color)
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
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = c.textSecondary,
            )
            Spacer(Modifier.width(4.dp))
            Text("Voltar ao comparativo", fontSize = 12.sp, color = c.textSecondary)
        }
        Spacer(Modifier.height(12.dp))
        Text("Como alterar meu DNS", fontSize = 16.sp, fontWeight = FontWeight.W600, color = c.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text("Escolha onde prefere alterar:", fontSize = 12.sp, color = c.textSecondary)
        Spacer(Modifier.height(16.dp))

        val tabs = listOf("Dispositivo", "Roteador")
        TabRow(
            selectedTabIndex = tabSelecionada,
            containerColor = c.bgPrimary,
            contentColor = LkColors.accent,
            divider = { HorizontalDivider(color = c.border, thickness = 1.dp) },
        ) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = tabSelecionada == idx,
                    onClick = { tabSelecionada = idx },
                    text = {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = if (tabSelecionada == idx) FontWeight.W600 else FontWeight.W400,
                            color = if (tabSelecionada == idx) LkColors.accent else c.textSecondary,
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        if (tabSelecionada == 0) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Android · DNS Privado", fontSize = 13.sp, fontWeight = FontWeight.W600, color = c.textPrimary)
                DnsGuideStep(1, "Abra as Configurações do sistema", c)
                DnsGuideStep(2, "Vá em Rede e internet → DNS privado", c)
                DnsGuideStep(3, "Selecione \"Nome do host do DNS privado\"", c)
                DnsGuideStep(4, "Digite o hostname do servidor DNS desejado", c)
                DnsGuideStep(5, "Toque em Salvar", c)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Esta configuração afeta apenas este dispositivo.",
                    fontSize = 11.sp,
                    color = c.textTertiary,
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Configurações do Roteador", fontSize = 13.sp, fontWeight = FontWeight.W600, color = c.textPrimary)
                DnsGuideStep(1, "Acesse o painel admin do roteador (geralmente 192.168.0.1 ou 192.168.1.1)", c)
                DnsGuideStep(2, "Faça login com as credenciais (veja na etiqueta do roteador)", c)
                DnsGuideStep(3, "Localize as configurações de Rede ou WAN", c)
                DnsGuideStep(4, "Encontre o campo DNS primário e DNS secundário", c)
                DnsGuideStep(5, "Insira os endereços do servidor DNS desejado", c)
                DnsGuideStep(6, "Salve e aguarde o roteador reiniciar", c)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Esta configuração afeta todos os dispositivos conectados à rede.",
                    fontSize = 11.sp,
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
    c: LkTokens,
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(LkColors.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("$numero", fontSize = 11.sp, fontWeight = FontWeight.W600, color = LkColors.accent)
        }
        Spacer(Modifier.width(LkSpacing.sm))
        Text(texto, fontSize = 13.sp, color = c.textSecondary, modifier = Modifier.weight(1f))
    }
}

// ─── Utilitário ───────────────────────────────────────────────────────────────

internal fun resolveDnsName(dnsIp: String?): String =
    when (dnsIp) {
        "1.1.1.1", "1.0.0.1" -> "Cloudflare"
        "8.8.8.8", "8.8.4.4" -> "Google DNS"
        "9.9.9.9", "149.112.112.112" -> "Quad9"
        "208.67.222.222", "208.67.220.220" -> "OpenDNS"
        "94.140.14.14", "94.140.15.15" -> "AdGuard"
        else -> "DNS do Provedor"
    }
