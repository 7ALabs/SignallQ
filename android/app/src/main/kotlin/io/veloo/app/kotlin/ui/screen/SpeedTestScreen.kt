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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.R
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.core.telephony.MovelSnapshot
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.FaseSpeedtest
import io.signallq.app.feature.speedtest.ModoSpeedtest
import io.signallq.app.feature.speedtest.ResultadoRodadaTriplo
import io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest
import io.signallq.app.ui.IspInfo
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.ProfileAvatarButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(
    snapshotSpeedtest: SnapshotExecucaoSpeedtest,
    snapshotRede: SnapshotRede,
    ispInfo: IspInfo?,
    localizacaoServidor: String?,
    modoSelecionado: ModoSpeedtest,
    onModoSelecionado: (ModoSpeedtest) -> Unit,
    onIniciarTeste: () -> Unit,
    onCancelarTeste: () -> Unit,
    onAbrirDnsBenchmark: () -> Unit,
    onAbrirPing: () -> Unit = {},
    onVerResultado: () -> Unit = {},
    onAbrirHistorico: () -> Unit = {},
    onAbrirAjustes: () -> Unit = {},
    nomeUsuario: String = "",
    fotoUri: String? = null,
    speedtestPendenteModoMovel: ModoSpeedtest? = null,
    onConfirmarSpeedtestMovel: () -> Unit = {},
    onCancelarSpeedtestMovel: () -> Unit = {},
    onAbrirPerfil: () -> Unit = {},
    planoInternet: String = "",
    movelSnapshot: MovelSnapshot? = null,
) {
    val c = LocalLkTokens.current
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

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
                            containerColor = LkColors.error,
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

    // Dialog de confirmação de uso de dados móveis — fonte de verdade no ViewModel (Task 4)
    if (speedtestPendenteModoMovel != null) {
        val titulo =
            when (speedtestPendenteModoMovel) {
                ModoSpeedtest.triplo -> "Usar dados móveis para teste triplo?"
                ModoSpeedtest.complete -> "Usar dados móveis para teste?"
                else -> "Usar dados móveis para teste?"
            }
        val mensagem =
            when (speedtestPendenteModoMovel) {
                ModoSpeedtest.triplo -> "Este teste vai usar aproximadamente 30 MB em 3 medições. Você poderá repetir em Wi-Fi depois."
                ModoSpeedtest.complete -> "Este teste vai usar aproximadamente 25 MB. Você poderá repetir em Wi-Fi depois."
                else -> "Este teste vai usar aproximadamente 25 MB. Você poderá repetir em Wi-Fi depois."
            }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = onCancelarSpeedtestMovel,
            title = { Text(titulo) },
            text = { Text(mensagem) },
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
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ExpandMore,
                                contentDescription = null,
                                tint = c.textPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "Velocidade",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.W600,
                                color = c.textPrimary,
                            )
                        }
                        if (planoInternet.isNotEmpty()) {
                            Text(
                                text = "Plano contratado: $planoInternet",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary,
                            )
                        }
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
                scrollBehavior = scrollBehavior,
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
            modoSelecionado = modoSelecionado,
            onModoSelecionado = onModoSelecionado,
            onIniciarTeste = onIniciarTeste,
            onVerResultado = onVerResultado,
            onAbrirHistorico = onAbrirHistorico,
            mostrarDialogCancelar = { mostrarDialogCancelar = true },
            temResultado = temResultado,
            estadoIdle = estadoIdle,
            c = c,
        )
    }
}

@Composable
private fun ConteudoSpeedTest(
    padding: androidx.compose.foundation.layout.PaddingValues,
    snapshotSpeedtest: SnapshotExecucaoSpeedtest,
    snapshotRede: SnapshotRede,
    movelSnapshot: MovelSnapshot?,
    localizacaoServidor: String?,
    modoSelecionado: ModoSpeedtest,
    onModoSelecionado: (ModoSpeedtest) -> Unit,
    onIniciarTeste: () -> Unit,
    onVerResultado: () -> Unit,
    onAbrirHistorico: () -> Unit,
    mostrarDialogCancelar: () -> Unit,
    temResultado: Boolean,
    estadoIdle: Boolean,
    c: LkTokens,
) {
    if (!temResultado) {
        // Sem resultado: centraliza verticalmente no espaço disponível, sem vazio inferior
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = LkSpacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BlocoCirculoSpeedTest(
                    snapshotSpeedtest = snapshotSpeedtest,
                    snapshotRede = snapshotRede,
                    movelSnapshot = movelSnapshot,
                    localizacaoServidor = localizacaoServidor,
                    modoSelecionado = modoSelecionado,
                    onModoSelecionado = onModoSelecionado,
                    onIniciarTeste = onIniciarTeste,
                    onVerResultado = onVerResultado,
                    mostrarDialogCancelar = mostrarDialogCancelar,
                    estadoIdle = estadoIdle,
                    c = c,
                )
            }
        }
    } else {
        // Com resultado: scroll para acomodar o card
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LkSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(LkSpacing.lg))

            BlocoCirculoSpeedTest(
                snapshotSpeedtest = snapshotSpeedtest,
                snapshotRede = snapshotRede,
                movelSnapshot = movelSnapshot,
                localizacaoServidor = localizacaoServidor,
                modoSelecionado = modoSelecionado,
                onModoSelecionado = onModoSelecionado,
                onIniciarTeste = onIniciarTeste,
                onVerResultado = onVerResultado,
                mostrarDialogCancelar = mostrarDialogCancelar,
                estadoIdle = estadoIdle,
                c = c,
            )

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
                label = if (modoSelecionado == ModoSpeedtest.triplo) "Média das 3 medições" else "Último resultado",
                onClick = onAbrirHistorico,
            )
            if (modoSelecionado == ModoSpeedtest.triplo && snapshotSpeedtest.rodadasTriplo.isNotEmpty()) {
                Spacer(Modifier.height(LkSpacing.sm))
                CardRodadasTriplo(c = c, rodadas = snapshotSpeedtest.rodadasTriplo)
            }
            Spacer(Modifier.height(LkSpacing.xxl))
        }
    }
}

@Composable
private fun BlocoCirculoSpeedTest(
    snapshotSpeedtest: SnapshotExecucaoSpeedtest,
    snapshotRede: SnapshotRede,
    movelSnapshot: MovelSnapshot?,
    localizacaoServidor: String?,
    modoSelecionado: ModoSpeedtest,
    onModoSelecionado: (ModoSpeedtest) -> Unit,
    onIniciarTeste: () -> Unit,
    onVerResultado: () -> Unit,
    mostrarDialogCancelar: () -> Unit,
    estadoIdle: Boolean,
    c: LkTokens,
) {
    if (modoSelecionado == ModoSpeedtest.triplo && snapshotSpeedtest.estado == EstadoExecucaoSpeedtest.executando) {
        IndicadorRodadaTriplo(
            rodadaAtual = snapshotSpeedtest.rodadaAtual,
            aguardando = snapshotSpeedtest.aguardandoProximaRodada,
        )
        Spacer(Modifier.height(LkSpacing.md))
    }

    SpeedTestCircle(
        estado = snapshotSpeedtest.estado,
        progresso = snapshotSpeedtest.progressoPercentual,
        fase = snapshotSpeedtest.faseAtual,
        velocidadeMbps = if (snapshotSpeedtest.aguardandoProximaRodada) 0.0 else snapshotSpeedtest.velocidadeAtualMbps,
        onIniciarTeste = onIniciarTeste,
    )

    // Linha de contexto: tipo de conexão + servidor (só no estado idle/concluído)
    if (estadoIdle) {
        Spacer(Modifier.height(LkSpacing.sm))
        LinhaContextoConexao(
            snapshotRede = snapshotRede,
            movelSnapshot = movelSnapshot,
            localizacaoServidor = localizacaoServidor,
            c = c,
        )
    }

    if (!snapshotRede.conectado) {
        Spacer(Modifier.height(LkSpacing.sm))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
        ) {
            Icon(
                imageVector = Icons.Outlined.WifiOff,
                contentDescription = null,
                tint = LkColors.warning,
                modifier = Modifier.size(14.dp),
            )
            Text(
                "Sem conexão — teste indisponível",
                style = MaterialTheme.typography.labelSmall,
                color = LkColors.warning,
            )
        }
    }

    if (snapshotSpeedtest.estado == EstadoExecucaoSpeedtest.executando) {
        Spacer(Modifier.height(LkSpacing.sm))
        TextButton(onClick = mostrarDialogCancelar) {
            Text(
                text = if (modoSelecionado == ModoSpeedtest.triplo) "Cancelar teste" else "Cancelar",
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
                color = LkColors.accent,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    } else {
        Spacer(Modifier.height(LkSpacing.md))
    }

    ModeSelector(modoSelecionado = modoSelecionado, onSelect = onModoSelecionado)

    val erroMsg = snapshotSpeedtest.erroMensagem
    if (snapshotSpeedtest.estado == EstadoExecucaoSpeedtest.erro && erroMsg != null) {
        Spacer(Modifier.height(LkSpacing.md))
        Text(
            text = erroMsg,
            style = MaterialTheme.typography.titleSmall,
            color = LkColors.error,
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
                val tec = movelSnapshot?.tecnologia?.ifBlank { null }?.uppercase()
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
    progresso: Int,
    fase: FaseSpeedtest,
    velocidadeMbps: Double,
    onIniciarTeste: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(230.dp),
    ) {
        when (estado) {
            EstadoExecucaoSpeedtest.idle -> {
                IdleCircle(onIniciarTeste = onIniciarTeste)
            }
            EstadoExecucaoSpeedtest.erro -> {
                ErrorCircle(onTentarNovamente = onIniciarTeste)
            }
            EstadoExecucaoSpeedtest.executando -> {
                ProgressCircle(
                    progresso = progresso,
                    fase = fase,
                    velocidadeMbps = velocidadeMbps,
                )
            }
            EstadoExecucaoSpeedtest.concluido -> {
                ConcluidoCircle(onIniciarTeste = onIniciarTeste)
            }
        }
    }
}

@Composable
private fun IdleCircle(onIniciarTeste: () -> Unit) {
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

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier
                    .size(220.dp)
                    .background(LkColors.accent.copy(alpha = glowAlpha), CircleShape),
        )
        Box(
            modifier =
                Modifier
                    .size(210.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(LkColors.accent)
                    .semantics { contentDescription = "Iniciar teste de velocidade" }
                    .clickable(onClick = onIniciarTeste),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Iniciar",
                color = LkColors.signallQTextOnDark,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ProgressCircle(
    progresso: Int,
    fase: FaseSpeedtest,
    velocidadeMbps: Double,
) {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progresso / 100f },
            modifier = Modifier.size(210.dp),
            color = LkColors.accent,
            strokeWidth = 6.dp,
            trackColor = LkColors.accent.copy(alpha = 0.2f),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            val rotulo =
                when (fase) {
                    FaseSpeedtest.ping -> "PING"
                    FaseSpeedtest.download -> "DOWNLOAD"
                    FaseSpeedtest.upload -> "UPLOAD"
                    else -> ""
                }
            if (rotulo.isNotBlank()) {
                Text(
                    text = rotulo,
                    style = MaterialTheme.typography.labelMedium,
                    color = LkColors.accent.copy(alpha = 0.7f),
                    fontWeight = FontWeight.W600,
                    letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.height(4.dp))
            }
            val mostraVelocidade = fase == FaseSpeedtest.download || fase == FaseSpeedtest.upload
            if (mostraVelocidade && velocidadeMbps > 0.0) {
                Text(
                    text = "%.1f".format(velocidadeMbps),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = LkColors.accent,
                )
                Text(
                    text = "Mbps",
                    style = MaterialTheme.typography.bodySmall,
                    color = LkColors.accent.copy(alpha = 0.7f),
                )
            } else {
                Text(
                    text = "$progresso%",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = LkColors.accent,
                )
                Text(
                    text = if (rotulo.isBlank()) "testando…" else "medindo…",
                    style = MaterialTheme.typography.bodySmall,
                    color = LkColors.accent.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun ConcluidoCircle(onIniciarTeste: () -> Unit) {
    Box(
        modifier =
            Modifier
                .size(210.dp)
                .clip(CircleShape)
                .background(LkColors.success.copy(alpha = 0.1f))
                .border(2.dp, LkColors.success, CircleShape)
                .semantics { contentDescription = "Iniciar novo teste" }
                .clickable(onClick = onIniciarTeste),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = LkColors.success,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Iniciar teste",
                style = MaterialTheme.typography.titleLarge,
                color = LkColors.success,
                fontWeight = FontWeight.W600,
            )
        }
    }
}

@Composable
private fun ErrorCircle(onTentarNovamente: () -> Unit) {
    Box(
        modifier =
            Modifier
                .size(210.dp)
                .clip(CircleShape)
                .background(LkColors.error.copy(alpha = 0.1f))
                .border(2.dp, LkColors.error, CircleShape)
                .semantics { contentDescription = "Erro no teste. Toque para tentar novamente." }
                .clickable(onClick = onTentarNovamente),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = LkColors.error,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tentar novamente",
                style = MaterialTheme.typography.titleLarge,
                color = LkColors.error,
                fontWeight = FontWeight.W600,
            )
        }
    }
}

private val modoOpcoes =
    listOf(
        "Rápido" to ModoSpeedtest.fast,
        "Completo" to ModoSpeedtest.complete,
        "Triplo" to ModoSpeedtest.triplo,
    )

@Composable
private fun ModeSelector(
    modoSelecionado: ModoSpeedtest,
    onSelect: (ModoSpeedtest) -> Unit,
) {
    val c = LocalLkTokens.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(c.bgSecondary)
                .padding(2.dp)
                .semantics { contentDescription = "Modo do teste" },
    ) {
        modoOpcoes.forEach { (label, modo) ->
            val selected = modoSelecionado == modo
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .shadow(elevation = if (selected) 1.dp else 0.dp, shape = RoundedCornerShape(999.dp))
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) c.bgPrimary else Color.Transparent)
                        .clickable { onSelect(modo) }
                        .padding(vertical = LkSpacing.sm)
                        .semantics {
                            role = Role.Tab
                            this.selected = selected
                            contentDescription = "$label${if (selected) ", selecionado" else ""}"
                        },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.W600,
                    color = if (selected) c.textPrimary else c.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun IndicadorRodadaTriplo(
    rodadaAtual: Int,
    aguardando: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { index ->
                val ativo = index < rodadaAtual
                Box(
                    modifier =
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (ativo) {
                                    LkColors.accent
                                } else {
                                    LkColors.accent.copy(alpha = 0.2f)
                                },
                            ),
                )
            }
        }
        Spacer(Modifier.height(LkSpacing.xs))
        Text(
            text = if (aguardando) "Aguardando próxima medição…" else "Medição $rodadaAtual de 3",
            style = MaterialTheme.typography.labelSmall,
            color = LocalLkTokens.current.textSecondary,
        )
    }
}

@Composable
private fun CardRodadasTriplo(
    c: LkTokens,
    rodadas: List<ResultadoRodadaTriplo>,
) {
    var expandido by remember { mutableStateOf(false) }
    val cdMedicoes = if (expandido) stringResource(R.string.cd_recolher_detalhes_medicoes) else stringResource(R.string.cd_expandir_detalhes_medicoes)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .semantics { contentDescription = cdMedicoes }
                .clickable { expandido = !expandido }
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (expandido) "Ocultar detalhes" else "Ver detalhes das 3 medições",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = c.textSecondary,
                modifier = Modifier.rotate(if (expandido) 180f else 0f),
            )
        }
        if (expandido) {
            Spacer(Modifier.height(LkSpacing.sm))
            rodadas.forEachIndexed { index, rodada ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Medição ${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                    Text(
                        text = "↓ ${"%.0f".format(
                            rodada.downloadMbps,
                        )} · ↑ ${"%.0f".format(rodada.uploadMbps)} Mbps · ${rodada.latenciaMs.toInt()} ms",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.W600,
                        color = LkColors.accent,
                    )
                }
            }
            if (rodadas.size == 3) {
                Spacer(Modifier.height(LkSpacing.xs))
                HorizontalDivider(color = c.border)
                Spacer(Modifier.height(LkSpacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Min/Max ↓", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                    Text(
                        "${"%.0f".format(rodadas.minOf { it.downloadMbps })} / ${"%.0f".format(rodadas.maxOf { it.downloadMbps })} Mbps",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.W600,
                        color = c.textSecondary,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Min/Max ↑", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                    Text(
                        "${"%.0f".format(rodadas.minOf { it.uploadMbps })} / ${"%.0f".format(rodadas.maxOf { it.uploadMbps })} Mbps",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.W600,
                        color = c.textSecondary,
                    )
                }
            }
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
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .clickable(onClick = onClick)
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.W600,
                color = c.textTertiary,
                letterSpacing = 0.4.sp,
            )
            if (relativeTimestamp.isNotEmpty()) {
                Text(
                    text = relativeTimestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                )
            }
        }
        Spacer(Modifier.height(LkSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.lg),
        ) {
            MetricColumn("Download", "%.1f".format(downloadMbps), "Mbps", LkColors.success, Modifier.weight(1f))
            MetricColumn("Upload", "%.1f".format(uploadMbps), "Mbps", LkColors.accent, Modifier.weight(1f))
            MetricColumn("Latência", "%.0f".format(latencyMs), "ms", LkColors.success, Modifier.weight(1f))
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
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W700, color = color)
            Text(text = unit, style = MaterialTheme.typography.labelSmall, color = LocalLkTokens.current.textSecondary)
        }
    }
}
