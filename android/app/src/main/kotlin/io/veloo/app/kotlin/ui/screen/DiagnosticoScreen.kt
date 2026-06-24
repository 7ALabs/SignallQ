package io.veloo.app.ui.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.NetworkWifi
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.veloo.app.R
import io.veloo.app.feature.diagnostico.ConnectionType
import io.veloo.app.feature.diagnostico.DiagSignalSelection
import io.veloo.app.feature.diagnostico.EstadoDiagnostico
import io.veloo.app.feature.diagnostico.SnapshotDiagnostico
import io.veloo.app.feature.diagnostico.ai.AiDiagnosisRepository
import io.veloo.app.feature.diagnostico.ai.AiDiagnosisResult
import io.veloo.app.feature.diagnostico.ai.AiDiagnosisState
import io.veloo.app.feature.diagnostico.ai.AiFallbackFactory
import io.veloo.app.feature.diagnostico.ai.ClassificacaoTecnica
import io.veloo.app.feature.diagnostico.ai.DiagChatEntry
import io.veloo.app.feature.diagnostico.ai.DiagnosisAiContextFactory
import io.veloo.app.feature.diagnostico.pulse.SignallQState
import io.veloo.app.ui.LkColors
import io.veloo.app.ui.LkRadius
import io.veloo.app.ui.LkSpacing
import io.veloo.app.ui.LkTokens
import io.veloo.app.ui.LocalLkTokens
import io.veloo.app.ui.component.DiagActionFooter
import io.veloo.app.ui.component.DiagImpactCard
import io.veloo.app.ui.component.DiagMetricsGrid
import io.veloo.app.ui.component.DiagRecommendationCard
import io.veloo.app.ui.component.DiagRootCauseCard
import io.veloo.app.ui.component.DiagVerdictHeroCard
import io.veloo.app.ui.component.ImpactItem
import io.veloo.app.ui.component.MetricItem
import io.veloo.app.ui.component.MetricStatus
import io.veloo.app.ui.component.OnDevicePill
import io.veloo.app.ui.component.SignalToggleCard
import io.veloo.app.ui.component.SignallQSymbol
import io.veloo.app.ui.state.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class DiagnosticoFase { Engines, Ia }

sealed interface DiagnosticoUiData {
    data class Carregando(
        val fase: DiagnosticoFase,
    ) : DiagnosticoUiData

    data class Resultado(
        val result: AiDiagnosisResult,
        val isFallback: Boolean,
    ) : DiagnosticoUiData
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticoScreen(
    snapshotDiagnostico: SnapshotDiagnostico,
    @Suppress("UNUSED_PARAMETER") onAbrirRedes: () -> Unit,
    onIniciarDiagnostico: () -> Unit,
    analiseSolicitada: Boolean,
    onAnaliseSolicitadaChange: (Boolean) -> Unit,
    aiState: AiDiagnosisState,
    onAiStateChange: (AiDiagnosisState) -> Unit,
    onVoltar: () -> Unit,
    onCompartilhar: () -> Unit = {},
    onRefazer: () -> Unit = {},
    onFalarOperadora: () -> Unit = {},
    onAbrirChat: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") chatHistorico: List<DiagChatEntry> = emptyList(),
    @Suppress("UNUSED_PARAMETER") chatCarregando: Boolean = false,
    @Suppress("UNUSED_PARAMETER") onEnviarChat: (String) -> Unit = {},
    /** AiDiagnosisRepository injetada via AppShell — instancia unica do grafo Hilt.
     *  Nao instancie AiDiagnosisRepository dentro de Composables (regra de negocio fora da UI). */
    aiRepository: AiDiagnosisRepository,
) {
    val c = LocalLkTokens.current
    val scope = rememberCoroutineScope()

    var signalSelection by remember { mutableStateOf(DiagSignalSelection()) }
    var loadingStartTime by remember { mutableStateOf<Long?>(null) }
    var mostrarMensagemConectando by remember(loadingStartTime) {
        mutableStateOf(false)
    }

    LaunchedEffect(aiState) {
        if (aiState is AiDiagnosisState.loading) {
            loadingStartTime = System.currentTimeMillis()
        } else {
            loadingStartTime = null
            mostrarMensagemConectando = false
        }
    }

    if (loadingStartTime != null) {
        LaunchedEffect(loadingStartTime) {
            delay(10_000L)
            mostrarMensagemConectando = true
        }
    }

    LaunchedEffect(snapshotDiagnostico.estado, analiseSolicitada) {
        if (!analiseSolicitada) return@LaunchedEffect
        if (aiState is AiDiagnosisState.loading || aiState is AiDiagnosisState.success) return@LaunchedEffect
        val relatorio = snapshotDiagnostico.relatorio ?: return@LaunchedEffect
        if (snapshotDiagnostico.estado != EstadoDiagnostico.concluido) return@LaunchedEffect

        onAiStateChange(AiDiagnosisState.loading)
        val connectionType = snapshotDiagnostico.input?.connectionType ?: ConnectionType.desconhecido
        val ctx = DiagnosisAiContextFactory.from(relatorio, snapshotDiagnostico.input, connectionType)
        onAiStateChange(aiRepository.explainDiagnosis(ctx) { AiFallbackFactory.fromLocal(relatorio) })
    }

    Scaffold(
        containerColor = c.bgPrimary,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            val uiStateForBar = resolveUiState(snapshotDiagnostico, aiState, analiseSolicitada)
            val showShare =
                uiStateForBar is UiState.Success &&
                    (uiStateForBar as UiState.Success).data is DiagnosticoUiData.Resultado
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Diagnóstico IA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = c.textPrimary,
                        )
                    }
                },
                actions = {
                    if (showShare) {
                        IconButton(onClick = onCompartilhar) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Compartilhar",
                                tint = c.textPrimary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        val uiState = resolveUiState(snapshotDiagnostico, aiState, analiseSolicitada)
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            when (uiState) {
                UiState.Empty -> {
                    DiagSetupContent(
                        c = c,
                        selection = signalSelection,
                        onToggle = { area ->
                            signalSelection =
                                when (area) {
                                    "velocidade" -> signalSelection.copy(velocidade = !signalSelection.velocidade)
                                    "wifi" -> signalSelection.copy(wifiSinal = !signalSelection.wifiSinal)
                                    "latencia" -> signalSelection.copy(latencia = !signalSelection.latencia)
                                    "fibra" -> signalSelection.copy(fibra = !signalSelection.fibra)
                                    "dns" -> signalSelection.copy(dns = !signalSelection.dns)
                                    else -> signalSelection
                                }
                        },
                        onDiagnosticar = {
                            onAnaliseSolicitadaChange(true)
                            onAiStateChange(AiDiagnosisState.idle)
                            onIniciarDiagnostico()
                        },
                    )
                }

                UiState.Loading -> {
                    // não utilizado — loading com fase usa Success<Carregando>
                }

                is UiState.Success -> {
                    when (val data = uiState.data) {
                        is DiagnosticoUiData.Carregando ->
                            DiagAnalyzingContent(
                                c = c,
                                selection = signalSelection,
                                isAiPhase = data.fase == DiagnosticoFase.Ia,
                                mostrarMensagemConectando = mostrarMensagemConectando && data.fase == DiagnosticoFase.Ia,
                            )

                        is DiagnosticoUiData.Resultado ->
                            DiagResultContent(
                                c = c,
                                result = data.result,
                                onCompartilhar = onCompartilhar,
                                onRefazer = {
                                    onAnaliseSolicitadaChange(false)
                                    onAiStateChange(AiDiagnosisState.idle)
                                    onRefazer()
                                },
                                onFalarOperadora = onFalarOperadora,
                                onAbrirChat = onAbrirChat,
                            )
                    }
                }

                is UiState.Error -> {
                    val isAiError = uiState.message != "Não foi possível diagnosticar a conexão."
                    if (isAiError) {
                        val codigoAmigavel =
                            when {
                                uiState.message.contains("timeout", ignoreCase = true) -> "ERR_TIMEOUT"
                                uiState.message.contains("503") || uiState.message.contains("504") -> "ERR_SERVIDOR_INDISPONIVEL"
                                uiState.message.contains("sem_relatorio") -> "ERR_SEM_DADOS"
                                else -> uiState.message.take(40)
                            }
                        var mostrarDetalhes by remember { mutableStateOf(false) }
                        AlertDialog(
                            onDismissRequest = { onAiStateChange(AiDiagnosisState.idle) },
                            title = { Text("IA temporariamente indisponível") },
                            text = {
                                Column {
                                    Text("A IA não respondeu agora. O diagnóstico local continua funcionando.")
                                    if (mostrarDetalhes) {
                                        Spacer(Modifier.height(LkSpacing.sm))
                                        Text(
                                            "Código: $codigoAmigavel",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = c.textTertiary,
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val relatorio = snapshotDiagnostico.relatorio ?: return@launch
                                            onAiStateChange(AiDiagnosisState.loading)
                                            val connectionType = snapshotDiagnostico.input?.connectionType ?: ConnectionType.desconhecido
                                            val ctx = DiagnosisAiContextFactory.from(relatorio, snapshotDiagnostico.input, connectionType)
                                            onAiStateChange(aiRepository.explainDiagnosis(ctx) { AiFallbackFactory.fromLocal(relatorio) })
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                                ) { Text("Tentar novamente") }
                            },
                            dismissButton = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    TextButton(onClick = {
                                        onAnaliseSolicitadaChange(false)
                                        onAiStateChange(AiDiagnosisState.idle)
                                    }) { Text("Sair") }
                                    TextButton(onClick = { mostrarDetalhes = !mostrarDetalhes }) {
                                        Text(
                                            if (mostrarDetalhes) "Ocultar detalhes" else "Ver detalhes",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = c.textTertiary,
                                        )
                                    }
                                }
                            },
                        )
                    } else {
                        DiagnosticoErroContent(
                            c = c,
                            onTentar = {
                                onAnaliseSolicitadaChange(false)
                                onAiStateChange(AiDiagnosisState.idle)
                            },
                        )
                    }
                }
            }
        }
    }
}

// ─── State resolution ─────────────────────────────────────────────────────────

private fun resolveUiState(
    snap: SnapshotDiagnostico,
    ai: AiDiagnosisState,
    solicitada: Boolean,
): UiState<DiagnosticoUiData> {
    if (!solicitada) return UiState.Empty
    if (snap.estado == EstadoDiagnostico.erro) return UiState.Error("Não foi possível diagnosticar a conexão.")
    if (ai is AiDiagnosisState.success) {
        return UiState.Success(DiagnosticoUiData.Resultado(ai.result, isFallback = false))
    }
    if (ai is AiDiagnosisState.fallback || ai is AiDiagnosisState.error) {
        val code = if (ai is AiDiagnosisState.error) ai.code else "ERR_SERVIDOR_INDISPONIVEL"
        return UiState.Error(code)
    }
    if (ai is AiDiagnosisState.timeout) {
        return UiState.Error("timeout")
    }
    if (ai is AiDiagnosisState.loading) {
        return UiState.Success(DiagnosticoUiData.Carregando(DiagnosticoFase.Ia))
    }
    return UiState.Success(DiagnosticoUiData.Carregando(DiagnosticoFase.Engines))
}

// ─── DiagSetup ────────────────────────────────────────────────────────────────

@Composable
private fun DiagSetupContent(
    c: LkTokens,
    selection: DiagSignalSelection,
    onToggle: (String) -> Unit,
    onDiagnosticar: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Intro
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = LkColors.accent,
                    modifier =
                        Modifier
                            .size(30.dp)
                            .padding(top = 2.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "A IA lê os sinais da sua conexão e entrega um diagnóstico pronto.",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = c.textPrimary,
                        lineHeight = 20.sp,
                    )
                    Text(
                        text = "Sem conversa: você escolhe o que medir, ela interpreta e aponta a causa.",
                        fontSize = 12.sp,
                        color = c.textSecondary,
                        lineHeight = 17.sp,
                    )
                }
            }

            // Overline
            Text(
                text = "O QUE ANALISAR",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = c.textTertiary,
                letterSpacing = 0.5.sp,
            )

            // Signal cards
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SignalToggleCard(
                    icon = Icons.Outlined.BarChart,
                    title = "Velocidade",
                    subtitle = "Download, upload e estabilidade",
                    enabled = selection.velocidade,
                    onToggle = { onToggle("velocidade") },
                    modifier = Modifier.fillMaxWidth(),
                )
                SignalToggleCard(
                    icon = Icons.Outlined.Wifi,
                    title = "Wi-Fi & Sinal",
                    subtitle = "Potência, canal e congestionamento",
                    enabled = selection.wifiSinal,
                    onToggle = { onToggle("wifi") },
                    modifier = Modifier.fillMaxWidth(),
                )
                SignalToggleCard(
                    icon = Icons.Outlined.SwapVert,
                    title = "Latência & Bufferbloat",
                    subtitle = "Atraso ocioso e sob carga",
                    enabled = selection.latencia,
                    onToggle = { onToggle("latencia") },
                    modifier = Modifier.fillMaxWidth(),
                )
                SignalToggleCard(
                    icon = Icons.Outlined.CellTower,
                    title = "Modem / Fibra (GPON)",
                    subtitle = "Potência óptica e status PPP",
                    enabled = selection.fibra,
                    onToggle = { onToggle("fibra") },
                    modifier = Modifier.fillMaxWidth(),
                )
                SignalToggleCard(
                    icon = Icons.Outlined.Language,
                    title = "DNS",
                    subtitle = "Tempo de resolução de nomes",
                    enabled = selection.dns,
                    onToggle = { onToggle("dns") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // Footer fixo
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(c.bgPrimary)
                    .border(width = 1.dp, color = c.border, shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp))
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onDiagnosticar,
                enabled = selection.anySelected,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                shape = RoundedCornerShape(LkRadius.button),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = LkColors.accent,
                        disabledContainerColor = LkColors.accent.copy(alpha = 0.4f),
                    ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Diagnosticar conexão",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            OnDevicePill(dark = false)
        }
    }
}

// ─── DiagAnalyzing ────────────────────────────────────────────────────────────

private enum class AnalysisStepStatus { Wait, Run, Done }

private data class AnalysisStep(
    val label: String,
    val status: AnalysisStepStatus,
)

@Composable
private fun DiagAnalyzingContent(
    c: LkTokens,
    selection: DiagSignalSelection,
    isAiPhase: Boolean,
    mostrarMensagemConectando: Boolean = false,
) {
    val steps =
        buildList {
            if (selection.velocidade) add("Velocidade medida")
            if (selection.wifiSinal) add("Wi-Fi e canais lidos")
            if (selection.latencia) add("Latência sob carga")
            if (selection.fibra) add("Modem / fibra")
            if (selection.dns) add("DNS resolvido")
            add("IA analisando")
        }

    var completedCount by remember(isAiPhase) {
        mutableIntStateOf(if (isAiPhase) steps.size - 1 else 0)
    }

    LaunchedEffect(isAiPhase) {
        if (!isAiPhase) {
            val signalStepCount = steps.size - 1
            signalStepCount.let { count ->
                for (i in 0 until count) {
                    delay(if (i == 0) 600L else 900L)
                    completedCount = i + 1
                }
            }
        }
    }

    val progress = if (steps.isEmpty()) 0f else completedCount.toFloat() / steps.size

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SignallQSymbol(
                state = SignallQState.Analyzing,
                size = 96.dp,
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = if (mostrarMensagemConectando) "Conectando ao AI..." else "Analisando sua conexão",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text =
                    if (mostrarMensagemConectando) {
                        "O servidor está processando os sinais da sua rede. Isso pode levar alguns segundos."
                    } else {
                        "A IA está cruzando os sinais para encontrar o que está limitando você."
                    },
                fontSize = 12.5.sp,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )

            Spacer(Modifier.height(20.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = LkColors.accent,
                trackColor = c.bgSecondary,
            )

            Spacer(Modifier.height(24.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                steps.forEachIndexed { index, label ->
                    val stepStatus =
                        when {
                            index < completedCount -> AnalysisStepStatus.Done
                            index == completedCount -> AnalysisStepStatus.Run
                            else -> AnalysisStepStatus.Wait
                        }
                    AnalysisStepRow(c = c, label = label, status = stepStatus)
                }
            }
        }

        OnDevicePill(
            dark = false,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun AnalysisStepRow(
    c: LkTokens,
    label: String,
    status: AnalysisStepStatus,
) {
    val pulse by androidx.compose.animation.core.rememberInfiniteTransition(label = "step-pulse").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
        label = "pulse-alpha",
    )

    val statusDesc =
        when (status) {
            AnalysisStepStatus.Done -> "concluído"
            AnalysisStepStatus.Run -> "em andamento"
            AnalysisStepStatus.Wait -> "pendente"
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    contentDescription = "$label — $statusDesc"
                    stateDescription = statusDesc
                },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        when (status) {
            AnalysisStepStatus.Done ->
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = LkColors.success,
                    modifier = Modifier.size(20.dp),
                )
            AnalysisStepStatus.Run ->
                CircularProgressIndicator(
                    color = LkColors.accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            AnalysisStepStatus.Wait ->
                Icon(
                    imageVector = Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = c.textTertiary,
                    modifier = Modifier.size(20.dp),
                )
        }
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (status == AnalysisStepStatus.Run) FontWeight.W500 else FontWeight.W400,
            color =
                when (status) {
                    AnalysisStepStatus.Done -> c.textSecondary
                    AnalysisStepStatus.Run -> c.textPrimary.copy(alpha = pulse)
                    AnalysisStepStatus.Wait -> c.textTertiary
                },
        )
    }
}

// ─── DiagResult ───────────────────────────────────────────────────────────────

@Composable
private fun DiagResultContent(
    c: LkTokens,
    result: AiDiagnosisResult,
    onCompartilhar: () -> Unit,
    onRefazer: () -> Unit,
    onFalarOperadora: () -> Unit,
    onAbrirChat: () -> Unit,
) {
    var metricsExpanded by remember { mutableStateOf(false) }

    val statusColor = diagStatusToColor(result.status, c)
    val statusLabel = diagStatusToLabel(result.status)
    val confiancaLabel = diagConfiancaLabel(result.problemaPrincipal.confianca)

    val impactItems = buildImpactItems(result)
    val metricItems = buildMetricItems(result)

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = 100.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 1. Herói
            item {
                DiagVerdictHeroCard(
                    titulo = "DIAGNÓSTICO IA",
                    veredito = result.textoLaudo.ifBlank { result.resumo },
                    statusLabel = statusLabel,
                    statusColor = statusColor,
                    confianca = "Confiança $confiancaLabel",
                    modelName = result.modeloIa.nomeExibicao.takeIf { it.isNotBlank() },
                )
            }

            // 2. Causa-raiz
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CAUSA-RAIZ IDENTIFICADA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = c.textTertiary,
                        letterSpacing = 0.5.sp,
                    )
                    DiagRootCauseCard(
                        icon = iconForProblemaTipo(result.problemaPrincipal.tipo),
                        title = result.problemaPrincipal.tipo.replaceFirstChar { it.uppercaseChar() },
                        subtitle = result.problemaPrincipal.descricao,
                    )
                    val secundarias = buildSecondaryRootCauses(result)
                    secundarias.forEach { (icon, title, subtitle) ->
                        DiagRootCauseCard(
                            icon = icon,
                            title = title,
                            subtitle = subtitle,
                        )
                    }
                }
            }

            // 3. Impacto
            if (impactItems.isNotEmpty()) {
                item { DiagImpactCard(items = impactItems) }
            }

            // 4. Recomendações
            if (result.acoesRecomendadas.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "O QUE FAZER · EM ORDEM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = c.textTertiary,
                            letterSpacing = 0.5.sp,
                        )
                        result.acoesRecomendadas.forEachIndexed { idx, acao ->
                            DiagRecommendationCard(
                                index = idx + 1,
                                title = acao.titulo,
                                description = acao.descricao,
                                priority = acao.prioridade.uppercase(),
                                priorityColor = priorityToColor(acao.prioridade),
                            )
                        }
                    }
                }
            }

            // 5. Métricas colapsável
            if (metricItems.isNotEmpty()) {
                item {
                    DiagMetricsGrid(
                        metrics = metricItems,
                        expanded = metricsExpanded,
                        onToggleExpand = { metricsExpanded = !metricsExpanded },
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        DiagActionFooter(
            onShare = onCompartilhar,
            onRefresh = onRefazer,
            onContactIsp = onFalarOperadora,
            onAbrirChat = onAbrirChat,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .background(c.bgPrimary),
        )
    }
}

// ─── Erro ─────────────────────────────────────────────────────────────────────

@Composable
private fun DiagnosticoErroContent(
    c: LkTokens,
    onTentar: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = LkSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = LkColors.error,
                modifier = Modifier.size(48.dp),
            )
            Text(
                stringResource(R.string.diagnostico_erro_titulo),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Text(
                stringResource(R.string.diagnostico_erro_descricao),
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(LkSpacing.sm))
            OutlinedButton(onClick = onTentar) {
                Text(stringResource(R.string.global_btn_tentar_novamente))
            }
        }
    }
}

// ─── Helpers de mapeamento de dados ──────────────────────────────────────────

private fun diagStatusToColor(
    status: String,
    c: LkTokens,
): Color =
    when (status.lowercase()) {
        "excelente", "bom" -> LkColors.success
        "regular" -> LkColors.warning
        "ruim", "critico" -> LkColors.error
        else -> c.textTertiary
    }

private fun diagStatusToLabel(status: String): String =
    when (status.lowercase()) {
        "excelente" -> "EXCELENTE"
        "bom" -> "BOM"
        "regular" -> "ATENÇÃO"
        "ruim" -> "RUIM"
        "critico" -> "CRÍTICO"
        else -> status.uppercase()
    }

private fun diagConfiancaLabel(confianca: Double): String =
    when {
        confianca >= 0.85 -> "alta"
        confianca >= 0.60 -> "média"
        confianca > 0.0 -> "baixa"
        else -> "indeterminada"
    }

private fun priorityToColor(prioridade: String): Color =
    when (prioridade.lowercase()) {
        "alta" -> LkColors.error
        "media", "média" -> LkColors.warning
        else -> LkColors.success
    }

private fun iconForProblemaTipo(tipo: String): ImageVector =
    when (tipo.lowercase()) {
        "wifi", "canal", "sinal" -> Icons.Outlined.Wifi
        "velocidade", "isp", "operadora" -> Icons.Outlined.Speed
        "latencia", "latência", "bufferbloat" -> Icons.Outlined.SwapVert
        "dns" -> Icons.Outlined.Language
        "fibra", "gpon", "modem" -> Icons.Outlined.CellTower
        else -> Icons.Outlined.NetworkWifi
    }

private data class RootCauseEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
)

private fun buildSecondaryRootCauses(result: AiDiagnosisResult): List<RootCauseEntry> {
    val cl = result.classificacaoTecnica
    val tipoLower = result.problemaPrincipal.tipo.lowercase()
    val candidates = mutableListOf<RootCauseEntry>()

    fun addIfBad(
        avaliacao: String?,
        label: String,
        icon: ImageVector,
        justificativa: String?,
    ) {
        if (avaliacao?.lowercase() in setOf("ruim", "regular") && !justificativa.isNullOrBlank()) {
            candidates.add(RootCauseEntry(icon, label, justificativa))
        }
    }

    if (tipoLower !in setOf("velocidade", "isp", "operadora")) {
        addIfBad(cl.velocidade?.avaliacao, "Velocidade", Icons.Outlined.BarChart, cl.velocidade?.justificativa)
    }
    if (tipoLower !in setOf("wifi", "canal", "sinal")) {
        addIfBad(cl.wifi?.avaliacao, "Wi-Fi", Icons.Outlined.Wifi, cl.wifi?.justificativa)
    }
    if (tipoLower !in setOf("latencia", "latência", "bufferbloat")) {
        addIfBad(cl.estabilidade?.avaliacao, "Latência & Bufferbloat", Icons.Outlined.SwapVert, cl.estabilidade?.justificativa)
    }
    if (tipoLower !in setOf("fibra", "gpon", "modem")) {
        addIfBad(cl.fibra?.avaliacao, "Modem / Fibra", Icons.Outlined.CellTower, cl.fibra?.justificativa)
    }
    if (tipoLower !in setOf("dns")) {
        addIfBad(cl.dns?.avaliacao, "DNS", Icons.Outlined.Language, cl.dns?.justificativa)
    }
    return candidates.take(2)
}

private fun buildImpactItems(result: AiDiagnosisResult): List<ImpactItem> {
    val imp = result.impacto
    return buildList {
        fun addImpact(
            icon: ImageVector,
            label: String,
            raw: String,
        ) {
            if (raw.isBlank()) return
            val (status, color) = impactoToStatusAndColor(raw)
            add(ImpactItem(icon, label, status, color))
        }
        addImpact(Icons.Outlined.Language, "Navegação", imp.navegacao)
        addImpact(Icons.Outlined.BarChart, "Streaming", imp.streaming)
        addImpact(Icons.Outlined.NetworkWifi, "Videochamadas", imp.videochamada)
        addImpact(Icons.Outlined.Speed, "Jogos", imp.jogos)
        addImpact(Icons.Outlined.Refresh, "Trabalho remoto", imp.trabalho)
    }.filter { it.status.isNotBlank() }
}

private fun impactoToStatusAndColor(raw: String): Pair<String, Color> {
    val lower = raw.trim().lowercase()
    return when {
        lower == "ok" -> "OK" to LkColors.success
        lower in setOf("lento", "instavel", "instável", "alta latencia", "alta latência") -> "Atenção" to LkColors.warning
        lower in setOf("indisponivel", "indisponível", "comprometido", "comprometida") -> "Ruim" to LkColors.error
        else -> raw to LkColors.success
    }
}

private fun buildMetricItems(result: AiDiagnosisResult): List<MetricItem> {
    val cl = result.classificacaoTecnica
    return result.evidencias.mapNotNull { ev ->
        val label = ev.label.substringAfter(":").ifBlank { ev.label }
        val valor = ev.valor.ifBlank { return@mapNotNull null }
        val status = inferMetricStatus(ev.label, cl)
        MetricItem(label = label, value = valor, status = status)
    }
}

private fun inferMetricStatus(
    label: String,
    cl: ClassificacaoTecnica,
): MetricStatus {
    val lower = label.lowercase()
    val avaliacao: String? =
        when {
            "download" in lower || "upload" in lower || "velocidade" in lower -> cl.velocidade?.avaliacao
            "wifi" in lower || "rssi" in lower || "sinal" in lower -> cl.wifi?.avaliacao
            "latencia" in lower || "latência" in lower || "jitter" in lower || "bufferbloat" in lower || "perda" in lower -> cl.estabilidade?.avaliacao
            "dns" in lower -> cl.dns?.avaliacao
            "fibra" in lower || "gpon" in lower || "ppp" in lower -> cl.fibra?.avaliacao
            else -> null
        }
    return when (avaliacao?.lowercase()) {
        "boa", "bom" -> MetricStatus.OK
        "regular" -> MetricStatus.WARN
        "ruim" -> MetricStatus.BAD
        else -> MetricStatus.OK
    }
}
