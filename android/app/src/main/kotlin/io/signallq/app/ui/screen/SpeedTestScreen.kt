package io.signallq.app.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.signallq.app.R
import io.signallq.app.ads.AdSlot
import io.signallq.app.ads.AdUnitIds
import io.signallq.app.ads.NativeAdContentSignal
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.core.telephony.MovelSnapshot
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.ModoSpeedtest
import io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest
import io.signallq.app.ui.IspInfo
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.ads.NativeAdEligibility
import io.signallq.app.ui.ads.NativeAdLoadState
import io.signallq.app.ui.ads.rememberNativeAdState
import io.signallq.app.ui.component.LkSectionOverline
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.ads.NativeAdRow
import io.signallq.app.ui.component.ads.NativeAdSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(
    snapshotSpeedtest: SnapshotExecucaoSpeedtest,
    snapshotRede: SnapshotRede,
    ispInfo: IspInfo?,
    localizacaoServidor: String?,
    onIniciarTeste: () -> Unit,
    onCancelarTeste: () -> Unit,
    onAbrirDnsBenchmark: () -> Unit,
    onAbrirPing: () -> Unit = {},
    onVerResultado: () -> Unit = {},
    onAbrirHistorico: () -> Unit = {},
    onAbrirAjustes: () -> Unit = {},
    speedtestPendenteModoMovel: ModoSpeedtest? = null,
    onConfirmarSpeedtestMovel: () -> Unit = {},
    onCancelarSpeedtestMovel: () -> Unit = {},
    onAbrirMenu: () -> Unit = {},
    planoInternet: String = "",
    movelSnapshot: MovelSnapshot? = null,
    /** Toggle remoto (Firebase Remote Config) + gate de consentimento UMP -- issue #555.
     *  Default `false`: nunca mostra anuncio sem sinal explicito de que pode. */
    adsEnabled: Boolean = false,
) {
    val c = LocalLkTokens.current

    // NAV-E: BackHandler com confirmação durante execução do teste
    val estaExecutando = snapshotSpeedtest.estado == EstadoExecucaoSpeedtest.executando
    var mostrarDialogCancelar by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = estaExecutando) {
        mostrarDialogCancelar = true
    }
    if (mostrarDialogCancelar) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { mostrarDialogCancelar = false },
            title = { Text("Interromper o teste?") },
            text = { Text("O teste em andamento será interrompido e o resultado descartado.") },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        mostrarDialogCancelar = false
                        onCancelarTeste()
                    },
                    colors =
                        androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = c.error,
                            contentColor = c.onError,
                        ),
                ) { Text("Interromper") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogCancelar = false }) {
                    Text("Continuar testando")
                }
            },
        )
    }

    // Dialog de confirmação de uso de dados móveis — fonte de verdade no ViewModel (Task 4).
    // GH#1737 — o modo triplo foi removido e o modo agora é automático por tipo de rede
    // (móvel → fast). deveSolicitarConfirmacaoRedeMovel já isenta o modo fast desse gate
    // (ver MainViewModel), então na prática só o modo complete chega a exibir este dialog —
    // ex.: Wi-Fi tarifado (hotspot) marcado como rede medida.
    if (speedtestPendenteModoMovel != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onCancelarSpeedtestMovel,
            title = { Text("Usar dados móveis para teste?") },
            text = { Text("Este teste vai usar aproximadamente 25 MB. Você poderá repetir em Wi-Fi depois.") },
            confirmButton = {
                androidx.compose.material3.Button(onClick = onConfirmarSpeedtestMovel) {
                    Text("Testar")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelarSpeedtestMovel) {
                    Text("Cancelar")
                }
            },
        )
    }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Velocidade",
                        style = MaterialTheme.typography.titleLarge,
                        color = c.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onAbrirMenu) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription =
                                stringResource(
                                    R.string.ajustes_cd_editar_perfil,
                                ),
                            tint = c.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        val temResultado = snapshotSpeedtest.resultado != null
        val estadoIdle =
            snapshotSpeedtest.estado == EstadoExecucaoSpeedtest.idle ||
                snapshotSpeedtest.estado == EstadoExecucaoSpeedtest.concluido

        ConteudoSpeedTest(
            padding = padding,
            snapshotSpeedtest = snapshotSpeedtest,
            snapshotRede = snapshotRede,
            movelSnapshot = movelSnapshot,
            localizacaoServidor = localizacaoServidor,
            onIniciarTeste = onIniciarTeste,
            onVerResultado = onVerResultado,
            onAbrirHistorico = onAbrirHistorico,
            mostrarDialogCancelar = { mostrarDialogCancelar = true },
            temResultado = temResultado,
            estadoIdle = estadoIdle,
            adsEnabled = adsEnabled,
            c = c,
        )
    }
}

/**
 * GH#1785 — mesmo mapeamento de [NativeAdEligibility] usado em `eligibilidadeAnuncioResultado`
 * (ResultadoVelocidadeScreen.kt): a tela só recebe o flag `adsEnabled` de fora, sem sinal de
 * consentimento UMP nem de conectividade separados (mesma limitação que `rememberNativeAd()`,
 * o wrapper antigo, já tinha).
 */
internal fun eligibilidadeAnuncioVelocidade(adsEnabled: Boolean): NativeAdEligibility =
    NativeAdEligibility(
        slot = AdSlot.VELOCIDADE,
        flagEnabled = adsEnabled,
        canRequestAds = adsEnabled,
        online = true,
    )

@Composable
private fun ConteudoSpeedTest(
    padding: androidx.compose.foundation.layout.PaddingValues,
    snapshotSpeedtest: SnapshotExecucaoSpeedtest,
    snapshotRede: SnapshotRede,
    movelSnapshot: MovelSnapshot?,
    localizacaoServidor: String?,
    onIniciarTeste: () -> Unit,
    onVerResultado: () -> Unit,
    onAbrirHistorico: () -> Unit,
    mostrarDialogCancelar: () -> Unit,
    temResultado: Boolean,
    estadoIdle: Boolean,
    adsEnabled: Boolean,
    c: LkTokens,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.base),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(LkSpacing.xl))

        if (estadoIdle) {
            Text(
                text = "Medir a conexão",
                style = MaterialTheme.typography.headlineSmall,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                text = "A medição é uma evidência do diagnóstico — não apenas um número.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(LkSpacing.lg))
        }

        BlocoCirculoSpeedTest(
            snapshotSpeedtest = snapshotSpeedtest,
            snapshotRede = snapshotRede,
            movelSnapshot = movelSnapshot,
            localizacaoServidor = localizacaoServidor,
            onIniciarTeste = onIniciarTeste,
            onVerResultado = onVerResultado,
            mostrarDialogCancelar = mostrarDialogCancelar,
            estadoIdle = estadoIdle,
            c = c,
        )

        if (estadoIdle) {
            Spacer(Modifier.height(LkSpacing.lg))
            LkSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = c.warning,
                    )
                    Text(
                        text = "Feche downloads e mantenha este aparelho no mesmo lugar durante a medição.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                    )
                }
            }
        }

        if (temResultado) {
            Spacer(Modifier.height(LkSpacing.lg))
            val resultado = snapshotSpeedtest.resultado!!
            val timestampRelativo =
                remember(resultado.timestampEpochMs) {
                    val diffMin = ((System.currentTimeMillis() - resultado.timestampEpochMs) / 60_000).toInt()
                    when {
                        diffMin < 1 -> "agora"
                        diffMin < 60 -> "há $diffMin min"
                        diffMin < 1440 -> "há ${diffMin / 60}h"
                        else -> "há ${diffMin / 1440}d"
                    }
                }
            LastResultCard(
                c = c,
                downloadMbps = resultado.downloadMbps,
                uploadMbps = resultado.uploadMbps,
                latencyMs = resultado.latenciaMs,
                relativeTimestamp = timestampRelativo,
                onClick = onAbrirHistorico,
            )
        }

        if (estadoIdle) {
            Spacer(Modifier.height(LkSpacing.md))
            val nativeAdState by rememberNativeAdState(
                adUnitId = AdUnitIds.para(AdSlot.VELOCIDADE),
                contentSignal = NativeAdContentSignal.forSlot(AdSlot.VELOCIDADE),
                eligibility = eligibilidadeAnuncioVelocidade(adsEnabled),
            )
            val nativeAd = (nativeAdState as? NativeAdLoadState.Fill)?.ad
            NativeAdRow(
                nativeAd = nativeAd,
                source = NativeAdSource.ADMOB,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(LkSpacing.xxl))
    }
}

@Composable
private fun BlocoCirculoSpeedTest(
    snapshotSpeedtest: SnapshotExecucaoSpeedtest,
    snapshotRede: SnapshotRede,
    movelSnapshot: MovelSnapshot?,
    localizacaoServidor: String?,
    onIniciarTeste: () -> Unit,
    onVerResultado: () -> Unit,
    mostrarDialogCancelar: () -> Unit,
    estadoIdle: Boolean,
    c: LkTokens,
) {
    SpeedTestCircle(
        estado = snapshotSpeedtest.estado,
        conectado = snapshotRede.conectado,
        onIniciarTeste = onIniciarTeste,
    )

    // Linha de contexto: tipo de conexão + servidor (só no estado idle/concluído)
    if (!snapshotRede.conectado) {
        Spacer(Modifier.height(LkSpacing.sm))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
        ) {
            Icon(
                imageVector = Icons.Outlined.WifiOff,
                contentDescription = null,
                tint = c.warning,
                modifier = Modifier.size(14.dp),
            )
            Text(
                "Sem conexão — teste indisponível",
                style = MaterialTheme.typography.labelSmall,
                color = c.warning,
            )
        }
    }

    if (snapshotSpeedtest.estado == EstadoExecucaoSpeedtest.executando) {
        Spacer(Modifier.height(LkSpacing.sm))
        TextButton(onClick = mostrarDialogCancelar) {
            Text(
                text = "Cancelar",
                color = c.textTertiary,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        val mbConsumidos = snapshotSpeedtest.bytesConsumidos / 1_000_000.0
        if (snapshotSpeedtest.bytesConsumidos > 0L) {
            Text(
                text = "%.1f MB usados".format(mbConsumidos),
                style = MaterialTheme.typography.labelMedium,
                color = c.textTertiary,
            )
        }
    } else if (snapshotSpeedtest.estado == EstadoExecucaoSpeedtest.concluido && snapshotSpeedtest.resultado != null) {
        Spacer(Modifier.height(LkSpacing.sm))
        TextButton(onClick = onVerResultado) {
            Text(
                text = "Ver resultado",
                color = c.primary,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    } else {
        Spacer(Modifier.height(LkSpacing.md))
    }

    if (estadoIdle) {
        Spacer(Modifier.height(LkSpacing.md))
        LinhaContextoConexao(
            snapshotRede = snapshotRede,
            movelSnapshot = movelSnapshot,
            localizacaoServidor = localizacaoServidor,
            c = c,
        )
    }

    val erroMsg = snapshotSpeedtest.erroMensagem
    if (snapshotSpeedtest.estado == EstadoExecucaoSpeedtest.erro && erroMsg != null) {
        Spacer(Modifier.height(LkSpacing.md))
        Text(
            text = erroMsg,
            style = MaterialTheme.typography.titleSmall,
            color = c.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = LkSpacing.xl),
        )
    }
}

@Composable
private fun LinhaContextoConexao(
    snapshotRede: SnapshotRede,
    movelSnapshot: MovelSnapshot?,
    localizacaoServidor: String?,
    c: LkTokens,
) {
    val tipoConexao =
        when (snapshotRede.estadoConexao) {
            EstadoConexao.wifi -> {
                val freq = snapshotRede.wifiLinkSnapshot?.frequenciaMhz
                val banda =
                    when {
                        freq != null && freq >= 5900 -> "Wi-Fi 6 GHz"
                        freq != null && freq >= 3000 -> "Wi-Fi 5 GHz"
                        freq != null -> "Wi-Fi 2.4 GHz"
                        else -> "Wi-Fi"
                    }
                banda
            }
            EstadoConexao.movel -> {
                val tec = tecnologiaSimplificada(movelSnapshot?.tecnologia)?.uppercase()
                if (tec != null) "Rede móvel · $tec" else "Rede móvel"
            }
            EstadoConexao.ethernet -> "Ethernet"
            else -> null
        }

    val partes = listOfNotNull(tipoConexao, localizacaoServidor?.takeIf { it.isNotBlank() })
    if (partes.isEmpty()) return

    Text(
        text = partes.joinToString(" · "),
        style = MaterialTheme.typography.labelSmall,
        color = c.textTertiary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SpeedTestCircle(
    estado: EstadoExecucaoSpeedtest,
    conectado: Boolean,
    onIniciarTeste: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(180.dp),
    ) {
        when (estado) {
            EstadoExecucaoSpeedtest.idle -> {
                IdleCircle(onIniciarTeste = onIniciarTeste, habilitado = conectado)
            }
            EstadoExecucaoSpeedtest.erro -> {
                ErrorCircle(onTentarNovamente = onIniciarTeste)
            }
            EstadoExecucaoSpeedtest.executando -> {
                // Sem UI própria: o overlay em tela cheia (VelocidadeScreen/GaugeCircular)
                // cobre a tela inteira durante a execução (ver AppShell) -- este branch
                // nunca fica visível. Renderizar aqui de novo so duplicava visual
                // divergente (rotulo "PING" e cor sempre violeta, sem cor por fase).
            }
            EstadoExecucaoSpeedtest.concluido -> {
                ConcluidoCircle(onIniciarTeste = onIniciarTeste)
            }
        }
    }
}

@Composable
private fun IdleCircle(
    onIniciarTeste: () -> Unit,
    habilitado: Boolean = true,
) {
    val c = LocalLkTokens.current
    // #1074 (SPD-019) — sem guarda local, um duplo toque rapido disparava onIniciarTeste()
    // duas vezes antes da recomposicao trocar o estado para `executando` (que desmonta este
    // Composable). O AtomicBoolean do ExecutorSpeedtest ja bloqueia a segunda execucao real,
    // mas o clique fantasma ainda resetava flags e duplicava contagem de MB no ViewModel.
    // `remember` sem chave: uma nova instancia de IdleCircle (e portanto um novo flag "false")
    // so aparece quando o estado volta a idle, que e exatamente o reset que queremos.
    var cliqueDisparado by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "scale",
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.46f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "glow",
    )

    val corBotao = if (habilitado) c.primary else c.primary.copy(alpha = 0.4f)
    val cdBotao = if (habilitado) "Iniciar teste de velocidade" else "Iniciar teste de velocidade, indisponível sem conexão"

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier
                    .size(180.dp)
                    .background(corBotao.copy(alpha = if (habilitado) glowAlpha else glowAlpha * 0.5f), CircleShape),
        )
        Box(
            modifier =
                Modifier
                    .size(164.dp)
                    .scale(if (habilitado) scale else 1f)
                    .clip(CircleShape)
                    .background(corBotao)
                    .semantics { contentDescription = cdBotao }
                    .clickable(enabled = habilitado && !cliqueDisparado) {
                        cliqueDisparado = true
                        onIniciarTeste()
                    },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Iniciar teste",
                color = LkColors.signallQTextOnDark,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun ConcluidoCircle(onIniciarTeste: () -> Unit) {
    val c = LocalLkTokens.current
    Box(
        modifier =
            Modifier
                .size(210.dp)
                .clip(CircleShape)
                .background(c.success.copy(alpha = 0.1f))
                .border(2.dp, c.success, CircleShape)
                .semantics { contentDescription = "Iniciar novo teste" }
                .clickable(onClick = onIniciarTeste),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = c.success,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Iniciar teste",
                style = MaterialTheme.typography.titleLarge,
                color = c.success,
                fontWeight = FontWeight.W600,
            )
        }
    }
}

@Composable
private fun ErrorCircle(onTentarNovamente: () -> Unit) {
    val c = LocalLkTokens.current
    Box(
        modifier =
            Modifier
                .size(210.dp)
                .clip(CircleShape)
                .background(c.error.copy(alpha = 0.1f))
                .border(2.dp, c.error, CircleShape)
                .semantics { contentDescription = "Erro no teste. Toque para tentar novamente." }
                .clickable(onClick = onTentarNovamente),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = c.error,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tentar novamente",
                style = MaterialTheme.typography.titleLarge,
                color = c.error,
                fontWeight = FontWeight.W600,
            )
        }
    }
}

@Composable
private fun LastResultCard(
    c: LkTokens,
    downloadMbps: Double,
    uploadMbps: Double,
    latencyMs: Double = 0.0,
    relativeTimestamp: String = "",
    label: String = "Último resultado",
    onClick: () -> Unit = {},
) {
    LkSurfaceCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LkSectionOverline(text = label)
            if (relativeTimestamp.isNotEmpty()) {
                Text(
                    text = relativeTimestamp,
                    style = MaterialTheme.typography.labelMedium,
                    color = c.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(LkSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.lg),
        ) {
            MetricColumn("Download", "%.0f".format(downloadMbps), "Mbps", c.success, Modifier.weight(1f))
            MetricColumn("Upload", "%.0f".format(uploadMbps), "Mbps", c.primary, Modifier.weight(1f))
            MetricColumn("Latência", "%.0f".format(latencyMs), "ms", c.success, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricColumn(
    label: String,
    value: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = LocalLkTokens.current.textTertiary)
        Spacer(Modifier.height(LkSpacing.xs))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs)) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W700, color = color)
            Text(text = unit, style = MaterialTheme.typography.labelSmall, color = LocalLkTokens.current.textSecondary)
        }
    }
}
