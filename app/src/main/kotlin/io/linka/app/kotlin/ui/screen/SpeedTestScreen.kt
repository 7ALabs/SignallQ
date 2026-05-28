package io.linka.app.kotlin.ui.screen

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.Wifi1Bar
import androidx.compose.material.icons.outlined.Wifi2Bar
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.linka.app.kotlin.FeatureFlags
import io.linka.app.kotlin.core.network.EstadoConexao
import io.linka.app.kotlin.core.network.SnapshotRede
import io.linka.app.kotlin.feature.speedtest.EstadoExecucaoSpeedtest
import io.linka.app.kotlin.feature.speedtest.FaseSpeedtest
import io.linka.app.kotlin.feature.speedtest.ModoSpeedtest
import io.linka.app.kotlin.feature.speedtest.ResultadoRodadaTriplo
import io.linka.app.kotlin.feature.speedtest.SeveridadeBufferbloat
import io.linka.app.kotlin.feature.speedtest.SnapshotExecucaoSpeedtest
import io.linka.app.kotlin.ui.IspInfo
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkRadius
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LkTokens
import io.linka.app.kotlin.ui.LocalLkTokens
import io.linka.app.kotlin.ui.component.ProfileAvatarButton

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
    onAbrirDiagnostico: () -> Unit,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.Speed, contentDescription = null, tint = c.textPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text(
                            text = "Central de testes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.W600,
                            color = c.textPrimary,
                        )
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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LkSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(LkSpacing.xxl))

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
                TextButton(onClick = { mostrarDialogCancelar = true }) {
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
                Spacer(Modifier.height(LkSpacing.xl))
            }

            ModeSelector(modoSelecionado = modoSelecionado, onSelect = onModoSelecionado)

            Spacer(Modifier.height(LkSpacing.md))

            Text(
                text =
                    when (modoSelecionado) {
                        ModoSpeedtest.fast -> "Download e upload · cerca de 15s"
                        ModoSpeedtest.complete -> "Download, upload, bufferbloat e DNS · cerca de 60s"
                        ModoSpeedtest.triplo -> "3 medições com intervalo de 10s · média calculada ao final"
                    },
                style = MaterialTheme.typography.titleSmall,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = LkSpacing.xl),
            )

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

            Spacer(Modifier.height(LkSpacing.xl))

            val resultado = snapshotSpeedtest.resultado
            if (resultado != null) {
                LastResultCard(
                    c = c,
                    downloadMbps = resultado.downloadMbps,
                    uploadMbps = resultado.uploadMbps,
                    label = if (modoSelecionado == ModoSpeedtest.triplo) "Média das 3 medições" else "Último resultado",
                    onClick = onAbrirHistorico,
                )
                if (modoSelecionado == ModoSpeedtest.triplo && snapshotSpeedtest.rodadasTriplo.isNotEmpty()) {
                    Spacer(Modifier.height(LkSpacing.sm))
                    CardRodadasTriplo(c = c, rodadas = snapshotSpeedtest.rodadasTriplo)
                }
                Spacer(Modifier.height(LkSpacing.lg))
                CardContextoUso(
                    c = c,
                    downloadMbps = resultado.downloadMbps,
                    uploadMbps = resultado.uploadMbps,
                    latenciaMs = resultado.latenciaMs,
                    jitterMs = resultado.jitterMs,
                )
                Spacer(Modifier.height(LkSpacing.lg))
                if (planoInternet.isBlank()) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = LkSpacing.sm)
                                .clip(RoundedCornerShape(LkRadius.card))
                                .background(c.bgSecondary)
                                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                                .padding(LkSpacing.md),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = LkColors.accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(LkSpacing.sm))
                            Text(
                                text = "Configure sua velocidade contratada para comparar com a ANATEL.",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary,
                            )
                        }
                        TextButton(
                            onClick = onAbrirAjustes,
                            contentPadding =
                                androidx.compose.foundation.layout
                                    .PaddingValues(0.dp),
                        ) {
                            Text(
                                text = "Configurar agora",
                                style = MaterialTheme.typography.labelMedium,
                                color = LkColors.accent,
                            )
                        }
                    }
                }
                CardRqualAnatel(
                    c = c,
                    planoInternet = planoInternet,
                    downloadMbps = resultado.downloadMbps,
                    estadoConexao = snapshotRede.estadoConexao,
                )
                Spacer(Modifier.height(LkSpacing.lg))
                CardBufferbloat(
                    c = c,
                    severidade = resultado.severidadeBufferbloat,
                )
                Spacer(Modifier.height(LkSpacing.lg))
            }

            ExploreToolsRow(
                c = c,
                onAbrirDnsBenchmark = onAbrirDnsBenchmark,
                onAbrirDiagnostico = onAbrirDiagnostico,
                onAbrirPing = onAbrirPing,
            )

            Spacer(Modifier.height(LkSpacing.lg))

            StatusCard(
                c = c,
                snapshotRede = snapshotRede,
                ispInfo = ispInfo,
                localizacaoServidor = localizacaoServidor,
            )

            Spacer(Modifier.height(LkSpacing.xxl))
        }
    }
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
                color = LkColors.linkaTextOnDark,
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
            Text(text = "Iniciar teste", style = MaterialTheme.typography.titleLarge, color = LkColors.success, fontWeight = FontWeight.W600)
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
            Text(text = "Tentar novamente", style = MaterialTheme.typography.titleLarge, color = LkColors.error, fontWeight = FontWeight.W600)
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
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(LkColors.accent.copy(alpha = 0.1f))
                .padding(2.dp)
                .semantics { contentDescription = "Modo do teste" },
    ) {
        modoOpcoes.forEach { (label, modo) ->
            val selected = modoSelecionado == modo
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) LkColors.accent else Color.Transparent)
                        .clickable { onSelect(modo) }
                        .padding(vertical = LkSpacing.lg)
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
                    color = if (selected) LkColors.linkaTextOnDark else LkColors.accent,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardRqualAnatel(
    c: LkTokens,
    planoInternet: String,
    downloadMbps: Double?,
    estadoConexao: EstadoConexao,
) {
    val velocidadeContratadaMbps =
        planoInternet
            .filter { it.isDigit() }
            .toIntOrNull()
            ?.let { if (planoInternet.contains("gbps", ignoreCase = true)) it * 1000 else it }

    // Nao renderiza se contrato nao informado ou invalido
    if (velocidadeContratadaMbps == null || velocidadeContratadaMbps <= 0) return

    // Nao renderiza se conexao nao e Wi-Fi nem Ethernet
    if (estadoConexao != EstadoConexao.wifi && estadoConexao != EstadoConexao.ethernet) return

    // Nao renderiza se nao ha medicao de download
    if (downloadMbps == null) return

    val percentual = ((downloadMbps / velocidadeContratadaMbps) * 100).toInt()

    val passaMinimoGarantido = percentual >= 40
    val passaVelocidadeNormal = percentual >= 80

    // Estado do card
    val (badgeTexto, badgeCor, textoConclusao) =
        when {
            passaVelocidadeNormal ->
                Triple(
                    "Aprovado",
                    LkColors.success,
                    "Sua internet está dentro do esperado pelo contrato.",
                )
            passaMinimoGarantido ->
                Triple(
                    "Parcial",
                    LkColors.warning,
                    "Velocidade acima do mínimo, mas abaixo do normal. Pode ser variação pontual — faça mais testes para confirmar.",
                )
            else ->
                Triple(
                    "Abaixo do mínimo",
                    LkColors.error,
                    "Se isso se repetir, você tem direito de reclamar com sua operadora.",
                )
        }

    val badgeTextColor = if (passaMinimoGarantido && !passaVelocidadeNormal) c.textPrimary else badgeCor

    var showTooltip by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.lg),
    ) {
        // Cabecalho: titulo + botao info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "Velocidade vs. contrato",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { showTooltip = true },
                modifier = Modifier.size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Mais informacoes sobre a regra ANATEL",
                    tint = c.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.height(LkSpacing.md))

        // Badge de veredicto
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(badgeCor.copy(alpha = 0.12f))
                    .padding(horizontal = LkSpacing.md, vertical = LkSpacing.xs),
        ) {
            Text(
                text = badgeTexto,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.W600,
                color = badgeTextColor,
            )
        }

        Spacer(Modifier.height(LkSpacing.md))

        // Criterio 1: Minimo garantido (40%)
        AnatelCriterioRow(
            c = c,
            label = "Mínimo garantido (40%)",
            passou = passaMinimoGarantido,
        )

        Spacer(Modifier.height(LkSpacing.sm))

        // Criterio 2: Velocidade normal (80%)
        AnatelCriterioRow(
            c = c,
            label = "Velocidade normal (80%)",
            passou = passaVelocidadeNormal,
        )

        Spacer(Modifier.height(LkSpacing.md))

        // Texto de conclusao
        Text(
            text = textoConclusao,
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
        )

        if (!passaMinimoGarantido) {
            Spacer(Modifier.height(LkSpacing.xs))
            Text(
                text = "Abaixo de 40%: você tem direito a solicitar rescisão sem multa (ANATEL Ato 7869/2022).",
                style = MaterialTheme.typography.bodySmall,
                color = LkColors.error,
            )
        }

        Spacer(Modifier.height(LkSpacing.sm))

        HorizontalDivider(color = c.border, thickness = 0.5.dp)

        Spacer(Modifier.height(LkSpacing.sm))

        // Rodape
        Text(
            text = "Ato 7869/2022 · ANATEL",
            style = MaterialTheme.typography.labelSmall,
            color = c.textTertiary,
        )
    }

    // Tooltip bottom sheet
    if (showTooltip) {
        ModalBottomSheet(
            onDismissRequest = { showTooltip = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = c.bgCard,
            dragHandle = {},
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LkSpacing.lg)
                        .padding(top = LkSpacing.xl, bottom = LkSpacing.xxl),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(c.border, RoundedCornerShape(999.dp)),
                    )
                }
                Spacer(Modifier.height(LkSpacing.lg))
                Text(
                    text = "Velocidade vs. contrato",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                )
                Spacer(Modifier.height(LkSpacing.sm))
                Text(
                    text =
                        "A ANATEL define dois limites de velocidade que sua operadora é obrigada a cumprir.\n\n" +
                            "O mínimo garantido é 40% da velocidade que você contratou — em qualquer momento do dia. " +
                            "Este teste mede exatamente isso.\n\n" +
                            "O limite de velocidade normal é 80% da velocidade contratada. Esse cálculo usa uma média " +
                            "de vários testes ao longo do tempo — não é possível confirmar esse critério com uma única medição.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
                Spacer(Modifier.height(LkSpacing.md))
                Text(
                    text = "Ato 7869/2022 · ANATEL",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun AnatelCriterioRow(
    c: LkTokens,
    label: String,
    passou: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (passou) {
                            LkColors.success.copy(alpha = 0.12f)
                        } else {
                            LkColors.error.copy(alpha = 0.12f)
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (passou) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = if (passou) "Aprovado" else "Reprovado",
                tint = if (passou) LkColors.success else LkColors.error,
                modifier = Modifier.size(12.dp),
            )
        }
        Spacer(Modifier.width(LkSpacing.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = c.textPrimary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardContextoUso(
    c: LkTokens,
    downloadMbps: Double,
    uploadMbps: Double,
    latenciaMs: Double,
    jitterMs: Double,
) {
    val aprovadoVideochamada = downloadMbps >= 10.0 && uploadMbps >= 3.0 && latenciaMs <= 80.0 && jitterMs <= 30.0
    val aprovadoStreaming = downloadMbps >= 25.0
    val aprovadoJogos = latenciaMs <= 50.0 && jitterMs <= 20.0
    val aprovadoHomeOffice = downloadMbps >= 5.0 && uploadMbps >= 5.0

    data class UsoItem(
        val nome: String,
        val aprovado: Boolean,
        val descritor: String,
    )
    val usos =
        listOf(
            UsoItem("Videochamada", aprovadoVideochamada, "Pode ter travamentos ou queda de qualidade"),
            UsoItem("Streaming HD", aprovadoStreaming, "Buffering provável em qualidade alta"),
            UsoItem("Jogos online", aprovadoJogos, "Latência ou jitter podem causar lag"),
            UsoItem("Home-office", aprovadoHomeOffice, "Upload insuficiente para ferramentas colaborativas"),
        )

    var showTooltip by remember { mutableStateOf(false) }
    if (showTooltip) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTooltip = false },
            confirmButton = {
                TextButton(onClick = { showTooltip = false }) { Text("Entendi") }
            },
            text = {
                Text(
                    "Estimativa baseada em velocidade, latência e jitter medidos. Pode variar conforme uso simultâneo de outros dispositivos.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )
    }

    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LkRadius.card),
        colors =
            androidx.compose.material3.CardDefaults
                .cardColors(containerColor = c.bgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, c.border),
    ) {
        Column(modifier = Modifier.padding(LkSpacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "O que você consegue fazer",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { showTooltip = true },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = c.textTertiary,
                    )
                }
            }
            Spacer(Modifier.height(LkSpacing.sm))
            usos.forEachIndexed { index, uso ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (uso.aprovado) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                        contentDescription = if (uso.aprovado) "Aprovado" else "Não aprovado",
                        tint = if (uso.aprovado) LkColors.success else c.textTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            uso.nome,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.W500,
                            color = c.textPrimary,
                        )
                        if (uso.aprovado) {
                            Text(
                                text = "Velocidade adequada para este uso",
                                style = MaterialTheme.typography.labelSmall,
                                color = LkColors.success,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        } else {
                            Text(
                                uso.descritor,
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary,
                            )
                        }
                    }
                }
                if (index < usos.lastIndex) {
                    HorizontalDivider(thickness = 0.5.dp, color = c.border)
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = c.border,
            )
            Text(
                text = "Jitter é a variação da latência — valores altos causam lag em jogos e travamentos em videochamadas.",
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardBufferbloat(
    c: LkTokens,
    severidade: SeveridadeBufferbloat,
) {
    if (severidade == SeveridadeBufferbloat.none) {
        androidx.compose.material3.Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.card),
            colors =
                androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = LkColors.success.copy(alpha = 0.08f),
                ),
            border = androidx.compose.foundation.BorderStroke(1.dp, LkColors.success.copy(alpha = 0.20f)),
        ) {
            Row(
                modifier = Modifier.padding(LkSpacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = LkColors.success,
                    modifier = Modifier.size(24.dp),
                )
                Column {
                    Text(
                        text = "Sem bufferbloat detectado",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Sua rede não apresenta atraso extra sob carga. Jogos e videochamadas devem funcionar bem.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                }
            }
        }
        return
    }

    val badgeLabel =
        when (severidade) {
            SeveridadeBufferbloat.mild -> "Leve"
            SeveridadeBufferbloat.moderate -> "Moderado"
            SeveridadeBufferbloat.severe -> "Severo"
            else -> return
        }
    val badgeBg =
        when (severidade) {
            SeveridadeBufferbloat.mild -> c.bgSecondary
            SeveridadeBufferbloat.moderate -> LkColors.warning.copy(alpha = 0.12f)
            else -> LkColors.error.copy(alpha = 0.12f)
        }
    val badgeTextColor =
        when (severidade) {
            SeveridadeBufferbloat.mild -> c.textSecondary
            SeveridadeBufferbloat.moderate -> c.textPrimary
            else -> LkColors.error
        }
    val icone =
        when (severidade) {
            SeveridadeBufferbloat.mild -> Icons.Outlined.Info
            SeveridadeBufferbloat.moderate -> Icons.Filled.WarningAmber
            else -> Icons.Filled.Error
        }
    val iconeTint =
        when (severidade) {
            SeveridadeBufferbloat.mild -> c.textSecondary
            SeveridadeBufferbloat.moderate -> LkColors.warning
            else -> LkColors.error
        }
    val texto =
        when (severidade) {
            SeveridadeBufferbloat.mild -> "Pequeno atraso detectado sob carga. Em uso normal, provavelmente imperceptível."
            SeveridadeBufferbloat.moderate -> "Em uso intenso — muitos downloads ou chamadas simultâneas — pode sentir travamentos e aumento de latência."
            else -> "Atraso alto mesmo com boa velocidade. Chamadas de vídeo e jogos online serão afetados durante qualquer uso da rede."
        }

    var showTooltip by remember { mutableStateOf(false) }
    if (showTooltip) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTooltip = false },
            confirmButton = {
                TextButton(onClick = { showTooltip = false }) { Text("Entendi") }
            },
            text = {
                Text(
                    "O bufferbloat acontece quando o roteador acumula pacotes em fila durante uso intenso, causando atraso extra nas comunicações em tempo real. Um bom roteador gerencia essa fila automaticamente (chamado de QoS ou Smart Queue Management).",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )
    }

    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LkRadius.card),
        colors =
            androidx.compose.material3.CardDefaults
                .cardColors(containerColor = c.bgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, c.border),
    ) {
        Column(modifier = Modifier.padding(LkSpacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Atraso extra na conexão",
                    style = MaterialTheme.typography.titleSmall,
                    color = c.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { showTooltip = true },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = c.textTertiary,
                    )
                }
            }
            Spacer(Modifier.height(LkSpacing.sm))
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icone, contentDescription = null, tint = iconeTint, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        badgeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.W600,
                        color = badgeTextColor,
                    )
                }
            }
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                texto,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textPrimary,
                lineHeight = 22.sp,
            )
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
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
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
                        text = "↓ ${"%.0f".format(rodada.downloadMbps)} · ↑ ${"%.0f".format(rodada.uploadMbps)} Mbps · ${rodada.latenciaMs.toInt()} ms",
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
    label: String = "Último resultado",
    onClick: () -> Unit = {},
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .clickable(onClick = onClick)
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = c.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "↓ ${"%.0f".format(downloadMbps)} · ↑ ${"%.0f".format(uploadMbps)} Mbps",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.W600,
            color = LkColors.accent,
        )
        Spacer(Modifier.width(LkSpacing.xs))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = c.textTertiary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreToolsRow(
    c: LkTokens,
    onAbrirDnsBenchmark: () -> Unit,
    onAbrirDiagnostico: () -> Unit,
    onAbrirPing: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = LkSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            FerramentaCard(
                c = c,
                icon = Icons.Outlined.Speed,
                titulo = "DNS Benchmark",
                descricao = "Mede servidores DNS",
                onClick = onAbrirDnsBenchmark,
                modificador = Modifier.weight(1f),
            )
            FerramentaCard(
                c = c,
                icon = Icons.Outlined.NetworkCheck,
                titulo = "Ping / Latência",
                descricao = "Testa o tempo de resposta",
                onClick = onAbrirPing,
                modificador = Modifier.weight(1f),
            )
        }
        if (FeatureFlags.DIAGNOSTICO_CHAT) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = LkSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            ) {
                FerramentaCard(
                    c = c,
                    icon = Icons.Outlined.Psychology,
                    titulo = "Diagnóstico",
                    descricao = "Análise inteligente",
                    onClick = onAbrirDiagnostico,
                    modificador = Modifier.weight(1f),
                )
                Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FerramentaCard(
    c: LkTokens,
    icon: ImageVector,
    titulo: String,
    descricao: String,
    onClick: () -> Unit,
    badge: String? = null,
    habilitado: Boolean = true,
    modificador: Modifier = Modifier,
) {
    val cardModifier =
        if (habilitado) {
            modificador
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .clickable { onClick() }
                .padding(LkSpacing.md)
        } else {
            modificador
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard.copy(alpha = 0.5f))
                .border(1.dp, c.border.copy(alpha = 0.5f), RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.md)
                .graphicsLayer { alpha = 0.5f }
        }

    Column(
        modifier = cardModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = c.textPrimary,
            modifier =
                Modifier
                    .size(32.dp)
                    .padding(bottom = LkSpacing.sm),
        )
        Text(
            text = titulo,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.W600,
            color = c.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = descricao,
            style = MaterialTheme.typography.labelSmall,
            color = c.textTertiary,
            textAlign = TextAlign.Center,
        )
        if (badge != null) {
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val toolsEmBreve =
    listOf(
        "Ping / Latência" to "Testa o ping para servidores externos",
    )

@Composable
private fun ExploreToolsSheet(
    c: LkTokens,
    onAbrirDnsBenchmark: () -> Unit,
    onAbrirDiagnostico: () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 32.dp)) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = LkSpacing.md, bottom = LkSpacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(c.border, RoundedCornerShape(999.dp)),
            )
        }

        Text(
            text = "Ferramentas",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = c.textPrimary,
            modifier = Modifier.padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm),
        )

        // DNS Benchmark — ativo
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onAbrirDnsBenchmark() }
                    .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "DNS Benchmark", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W600, color = c.textPrimary)
                Text(text = "Mede a velocidade dos servidores DNS", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = c.textTertiary,
                modifier = Modifier.size(14.dp),
            )
        }

        HorizontalDivider(
            color = c.border,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = LkSpacing.lg),
        )

        // Diagnostico Inteligente — ativo
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { onAbrirDiagnostico() }
                    .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Diagnóstico Inteligente", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W600, color = c.textPrimary)
                Text(text = "Diagnóstico inteligente da conexão", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = c.textTertiary,
                modifier = Modifier.size(14.dp),
            )
        }

        toolsEmBreve.forEachIndexed { index, (title, desc) ->
            HorizontalDivider(
                color = c.border,
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = LkSpacing.lg),
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.W600, color = c.textPrimary)
                    Text(text = desc, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                }
                Text(text = "Em breve", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
            }
        }
    }
}

@Composable
private fun StatusCard(
    c: LkTokens,
    snapshotRede: SnapshotRede,
    ispInfo: IspInfo?,
    localizacaoServidor: String?,
) {
    val isConnected = snapshotRede.conectado
    val estadoConexao = snapshotRede.estadoConexao
    val wifiSnapshot = snapshotRede.wifiLinkSnapshot

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .background(c.bgCard),
    ) {
        when {
            estadoConexao == EstadoConexao.wifi && isConnected && wifiSnapshot != null -> {
                StatusRow(
                    c = c,
                    icon = wifiSignalIcon(wifiSnapshot.rssiDbm),
                    label = wifiSnapshot.ssid ?: "Wi-Fi",
                    value = "Conectado",
                    valueColor = LkColors.success,
                )
            }
            estadoConexao == EstadoConexao.movel && isConnected -> {
                StatusRow(
                    c = c,
                    icon = Icons.Outlined.SignalCellularAlt,
                    label = ispInfo?.isp ?: "Operadora",
                    value = "Conectado",
                    valueColor = LkColors.success,
                )
            }
            else -> {
                StatusRow(
                    c = c,
                    icon = Icons.Outlined.WifiOff,
                    label = "Sem conexão",
                    value = "Desconectado",
                    valueColor = LkColors.error,
                )
            }
        }
        HorizontalDivider(
            color = c.border,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = LkSpacing.lg),
        )
        if (!localizacaoServidor.isNullOrBlank()) {
            StatusRow(
                c = c,
                icon = Icons.Outlined.Language,
                label = "Servidor",
                value = localizacaoServidor,
                valueColor = c.textSecondary,
            )
        }
    }
}

private fun wifiSignalIcon(rssiDbm: Int?): ImageVector =
    when {
        rssiDbm == null || rssiDbm >= -60 -> Icons.Outlined.Wifi
        rssiDbm >= -70 -> Icons.Outlined.Wifi2Bar
        else -> Icons.Outlined.Wifi1Bar
    }

@Composable
private fun StatusRow(
    c: LkTokens,
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = c.textSecondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(LkSpacing.md))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = c.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = valueColor,
            fontWeight = FontWeight.W500,
        )
    }
}
