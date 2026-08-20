package io.signallq.app.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.signallq.app.ads.AdSlot
import io.signallq.app.ads.AdUnitIds
import io.signallq.app.ads.NativeAdContentSignal
import io.signallq.app.feature.devices.DispositivoRede
import io.signallq.app.feature.devices.EstadoScanDispositivos
import io.signallq.app.feature.devices.NamingPrioridade
import io.signallq.app.feature.devices.ResultadoCorrelacaoTopologia
import io.signallq.app.feature.devices.TipoDispositivo
import io.signallq.app.feature.devices.chaveApelido
import io.signallq.app.feature.devices.ehClienteFinal
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.ads.rememberNativeAd
import io.signallq.app.ui.component.LkSectionOverline
import io.signallq.app.ui.component.ads.NativeAdListRow
import io.signallq.app.ui.component.ads.NativeAdSource

// ---------------------------------------------------------------------------
// Lista principal com pull-to-refresh — extraído de DispositivosScreen.kt na
// issue #1663 (épico #1647, Task 2.0.15). Sem mudança de comportamento.
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DispositivosLista(
    c: LkTokens,
    dispositivos: List<DispositivoRede>,
    isLoading: Boolean,
    // GH#1217 item 3 — quando o scan termina em concluidoParcial (dado válido, porém
    // incompleto porque alguma fase falhou), a lista continua sendo mostrada normalmente,
    // mas com um aviso discreto — nunca finge que a varredura foi 100% completa.
    resultadoParcial: Boolean = false,
    erro: String?,
    onRefresh: () -> Unit,
    apelidos: Map<String, String>,
    onSalvarApelido: (mac: String, apelido: String) -> Unit,
    bandasWifi: String? = null,
    adsEnabled: Boolean = false,
    correlacoesTopologia: Map<String, ResultadoCorrelacaoTopologia> = emptyMap(),
) {
    val gateways = remember(dispositivos) { dispositivos.filter { it.fonteNome == "gateway" } }
    val aps =
        remember(dispositivos) { dispositivos.filter { it.fonteNome != "gateway" && it.tipoDispositivo == TipoDispositivo.pontoAcesso } }
    val clientes =
        remember(dispositivos) {
            dispositivos
                .filter { it.ehClienteFinal() }
                .sortedByDescending { it.esteDispositivo }
        }

    var deviceEmSheet by remember { mutableStateOf<DispositivoRede?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val nativeAd by rememberNativeAd(
        adUnitId = AdUnitIds.para(AdSlot.DISPOSITIVOS),
        contentSignal = NativeAdContentSignal.forSlot(AdSlot.DISPOSITIVOS),
        eligible = adsEnabled,
    )

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = LkSpacing.xl),
        ) {
            // Barra de progresso fina
            if (isLoading) {
                item {
                    LinearProgressIndicator(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                        color = c.primary,
                        trackColor = c.bgSecondary,
                    )
                }
            }
            if (resultadoParcial && !isLoading) {
                item {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = c.warning,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text(
                            "Resultado parcial — uma etapa da varredura não respondeu",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textSecondary,
                        )
                    }
                }
            }

            // ── Infraestrutura ─────────────────────────────────────────────
            if (gateways.isNotEmpty()) {
                item {
                    SectionHeaderRow(
                        title = "Infraestrutura (${gateways.size})",
                        c = c,
                    )
                }
                items(gateways) { gw ->
                    GatewayItem(
                        dispositivo = gw,
                        c = c,
                        apelido = gw.chaveApelido()?.let { apelidos[it] },
                        bandasWifi = bandasWifi,
                        clientesCount = clientes.size,
                        onTap = { deviceEmSheet = gw },
                    )
                }
            }

            // ── Pontos de acesso / nós mesh ───────────────────────────────
            if (aps.isNotEmpty()) {
                item {
                    SectionHeaderRow(title = "Pontos de acesso (${aps.size})", c = c)
                }
                items(aps) { ap ->
                    ApMeshItem(
                        dispositivo = ap,
                        c = c,
                        apelido = ap.chaveApelido()?.let { apelidos[it] },
                        onTap = { deviceEmSheet = ap },
                    )
                }
            }

            // ── Todos os dispositivos ──────────────────────────────────────
            val topPadding = if (gateways.isNotEmpty() || aps.isNotEmpty()) LkSpacing.sm else LkSpacing.md
            item { Spacer(Modifier.height(topPadding)) }

            if (clientes.isNotEmpty()) {
                val adIndex = clientes.size / 2
                item {
                    SectionHeaderRow(title = "Dispositivos (${clientes.size})", c = c)
                }
                itemsIndexed(clientes) { index, dev ->
                    if (index == adIndex) {
                        NativeAdListRow(
                            nativeAd = nativeAd,
                            source = NativeAdSource.ADMOB,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    DispositivoItem(
                        dispositivo = dev,
                        c = c,
                        apelido = dev.chaveApelido()?.let { apelidos[it] },
                        onTap = { deviceEmSheet = dev },
                    )
                }
            }

            if (clientes.isEmpty()) {
                item {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Apenas o gateway foi encontrado",
                            color = c.textSecondary,
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                }
            }
        }
    }

    // ── Modal de detalhe ──────────────────────────────────────────────────
    deviceEmSheet?.let { dev ->
        ModalBottomSheet(
            onDismissRequest = { deviceEmSheet = null },
            sheetState = sheetState,
            containerColor = c.bgPrimary,
        ) {
            if (dev.tipoDispositivo == TipoDispositivo.pontoAcesso) {
                MeshApSheet(
                    dispositivo = dev,
                    c = c,
                    apelidoAtual = dev.chaveApelido()?.let { apelidos[it] } ?: "",
                    onSalvarApelido = { apelido ->
                        dev.chaveApelido()?.let { chave -> onSalvarApelido(chave, apelido) }
                    },
                )
            } else {
                DeviceDetailSheet(
                    dispositivo = dev,
                    c = c,
                    apelidoAtual = dev.chaveApelido()?.let { apelidos[it] } ?: "",
                    onSalvarApelido = { apelido ->
                        dev.chaveApelido()?.let { chave -> onSalvarApelido(chave, apelido) }
                    },
                    correlacao = correlacoesTopologia[dev.id],
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Linha de gateway (Roteador / Extensor)
// ---------------------------------------------------------------------------

@Composable
private fun GatewayItem(
    dispositivo: DispositivoRede,
    c: LkTokens,
    apelido: String?,
    onTap: () -> Unit,
    // GH#531 — bandas Wi-Fi ("2,4G + 5G") e contagem de clientes detectados;
    // null/0 mantém o subtítulo antigo (só IP) quando não há dado suficiente.
    bandasWifi: String? = null,
    clientesCount: Int = 0,
) {
    val iconColor = c.primary
    val bgColor = c.primary.copy(alpha = 0.12f)
    val ip = dispositivo.ip ?: ""
    val subtituloGateway =
        if (bandasWifi.isNullOrBlank()) {
            ip
        } else {
            listOf(ip, bandasWifi, "$clientesCount clientes").filter { it.isNotBlank() }.joinToString(" · ")
        }

    LkListRow(
        c = c,
        leading = {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(LkRadius.button))
                        .background(bgColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Router,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        title = apelido?.takeIf { it.isNotBlank() } ?: dispositivo.nomeExibicao,
        subtitle = subtituloGateway,
        trailing = {
            BadgePill(label = "Roteador", bg = c.primary.copy(alpha = 0.10f), fg = c.primary)
        },
        onTap = onTap,
    )
}

// ---------------------------------------------------------------------------
// Linha de ponto de acesso / nó mesh
// ---------------------------------------------------------------------------

@Composable
private fun ApMeshItem(
    dispositivo: DispositivoRede,
    c: LkTokens,
    apelido: String?,
    onTap: () -> Unit,
) {
    LkListRow(
        c = c,
        leading = {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(LkRadius.button))
                        .background(c.success.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CellTower,
                    contentDescription = null,
                    tint = c.success,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        title = apelido?.takeIf { it.isNotBlank() } ?: dispositivo.nomeExibicao,
        subtitle = dispositivo.ip ?: "",
        trailing = {
            BadgePill(label = "AP Mesh", bg = c.success.copy(alpha = 0.10f), fg = c.success)
        },
        onTap = onTap,
    )
}

// ---------------------------------------------------------------------------
// Linha de dispositivo cliente
// ---------------------------------------------------------------------------

@Composable
private fun DispositivoItem(
    dispositivo: DispositivoRede,
    c: LkTokens,
    apelido: String?,
    onTap: () -> Unit,
) {
    val iconBg = iconBgColor(dispositivo.tipoDispositivo, c)
    val iconFg = iconFgColor(dispositivo.tipoDispositivo, c)
    val icon = iconForTipo(dispositivo.tipoDispositivo)
    val fabricante = dispositivo.fabricante?.takeIf { it.isNotBlank() }

    // Regra de produto (issue #1663, decisão do Luiz 2026-08-19): dispositivo sem nome
    // resolvido nunca recebe fabricante/tipo inventado — rótulo genérico honesto
    // ("Dispositivo <Fabricante>" quando o fabricante É confirmado via OUI/UPnP/mDNS,
    // "Dispositivo desconhecido" quando não há nenhuma evidência).
    val ehIpPuro = dispositivo.nomeExibicao.matches(Regex("""\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}"""))
    val nomeDisplay =
        apelido?.takeIf { it.isNotBlank() }
            ?: if (ehIpPuro) {
                NamingPrioridade.rotuloFallbackGenerico(fabricante)
            } else {
                dispositivo.nomeExibicao
            }
    val subtitulo =
        when {
            dispositivo.esteDispositivo && !dispositivo.ip.isNullOrBlank() -> "${dispositivo.ip} · Este aparelho"
            fabricante != null && nomeDisplay != fabricante -> fabricante
            !dispositivo.ip.isNullOrBlank() -> dispositivo.ip
            else -> "Fabricante desconhecido"
        }

    LkListRow(
        c = c,
        title = nomeDisplay,
        subtitle = subtitulo,
        leading = {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(LkRadius.input))
                        .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconFg,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dispositivo.ip ?: "",
                    color = c.textTertiary,
                    style = MaterialTheme.typography.labelSmall,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = c.textTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
        },
        onTap = onTap,
    )
}

// ---------------------------------------------------------------------------
// Sem Wi-Fi fallback
// ---------------------------------------------------------------------------

@Composable
internal fun SemWifiFallback(
    c: LkTokens,
    hasDadosMoveis: Boolean,
) {
    // #144: mensagem spec-compliant por estado de conexão
    val titulo = if (hasDadosMoveis) "Dispositivos da rede" else "Sem Wi-Fi"
    val subtitle =
        if (hasDadosMoveis) {
            "Dispositivos da rede só aparecem quando você está conectado a um Wi-Fi."
        } else {
            "Sem conexão de rede. Conecte-se a uma rede Wi-Fi para escanear."
        }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.WifiOff,
                contentDescription = null,
                tint = c.textTertiary,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.headlineSmall,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Estado vazio / scanning
// ---------------------------------------------------------------------------

@Composable
internal fun EmptyStateDispositivos(
    c: LkTokens,
    isLoading: Boolean,
    progresso: Int,
    // GH#1217 item 3 — estado tipado tem prioridade sobre a string de erro legada: cada
    // estado (semWifi/timeout/cancelado/erro) agora tem mensagem própria, não precisa mais
    // interpretar substring de erroMensagem pros casos já cobertos pelo enum.
    estado: EstadoScanDispositivos,
    erro: String?,
    onRefresh: () -> Unit,
) {
    val temErro = !erro.isNullOrBlank() || estado in ESTADOS_COM_MENSAGEM_PROPRIA
    val titulo: String
    val subtitulo: String
    val icone: androidx.compose.ui.graphics.vector.ImageVector
    val iconColor: androidx.compose.ui.graphics.Color

    if (temErro) {
        val (ttl, sbt) = tituloSubtituloParaEstado(estado, erro)
        titulo = ttl
        subtitulo = sbt
        icone = Icons.Outlined.WarningAmber
        iconColor = c.warning
    } else if (isLoading) {
        titulo = "Procurando dispositivos..."
        subtitulo = "Aguarde alguns instantes."
        icone = Icons.Outlined.DevicesOther
        iconColor = c.textTertiary
    } else {
        titulo = "Nenhum dispositivo encontrado"
        subtitulo = "Aguarde alguns segundos e tente novamente."
        icone = Icons.Outlined.DevicesOther
        iconColor = c.textTertiary
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = icone,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.headlineSmall,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            if (isLoading) {
                CircularProgressIndicator(color = c.primary)
            } else {
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary),
                ) {
                    Text("Escanear rede")
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Section header
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeaderRow(
    title: String,
    c: LkTokens,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = LkSpacing.lg, top = 20.dp, end = LkSpacing.lg, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LkSectionOverline(title, modifier = Modifier.weight(1f))
        if (trailing != null) {
            trailing()
        }
    }
}

// GH#1217 item 3 — estados que sempre têm mensagem própria, independente de erroMensagem
// estar preenchido (o scanner não seta mais string pra esses casos, só o enum).
private val ESTADOS_COM_MENSAGEM_PROPRIA =
    setOf(
        EstadoScanDispositivos.semWifi,
        EstadoScanDispositivos.timeout,
        EstadoScanDispositivos.cancelado,
        EstadoScanDispositivos.erro,
    )

/**
 * Título/subtítulo pro estado vazio — prioriza o [EstadoScanDispositivos] tipado; só cai pra
 * interpretação de string ([erro]) no estado genérico `erro`, preservando as mensagens mais
 * específicas (permissão de localização, erro de rede) que só existem como string hoje.
 */
private fun tituloSubtituloParaEstado(
    estado: EstadoScanDispositivos,
    erro: String?,
): Pair<String, String> =
    when (estado) {
        EstadoScanDispositivos.semWifi ->
            "Sem conexão Wi-Fi" to "Conecte-se a uma rede Wi-Fi para escanear dispositivos."
        EstadoScanDispositivos.timeout ->
            "Tempo limite excedido" to "O escaneamento levou muito tempo. Tente novamente."
        EstadoScanDispositivos.cancelado ->
            "Escaneamento cancelado" to "Toque em atualizar para escanear novamente."
        else -> traduzirErroParaPortugues(erro ?: "")
    }

private fun traduzirErroParaPortugues(erro: String): Pair<String, String> =
    when {
        erro.contains("semPermissaoLocalizacao", ignoreCase = true) -> {
            "Permissão de localização não concedida" to "Acesse as configurações do app para conceder acesso à localização."
        }
        erro.contains("erroRede", ignoreCase = true) -> {
            "Erro de conexão de rede" to "Verifique se sua conexão Wi-Fi está estável e tente novamente."
        }
        else -> {
            "Erro ao escanear" to "Não foi possível escanear a rede. Tente novamente."
        }
    }
